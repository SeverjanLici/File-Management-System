package com.docplatform.aiservice.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

data class FileMetadataDTO(
    val id: String,
    val filename: String,
    val summary: String?,
    val tags: List<String>?,
    val sensitivity: String?,
    val mimeType: String?
)

@Service
class AiQAService(
    private val chatClient: ChatClient,
    @Qualifier("plainWebClient") private val webClient: WebClient,
    @Value("\${services.document-service.url:http://localhost:8082}")
    private val documentServiceBaseUrl: String
) {
    private val logger = LoggerFactory.getLogger(AiQAService::class.java)

    private fun trace(title: String, message: String) {
        logger.info("""


#################### AI QA SERVICE: $title ####################
$message
###############################################################


""".trimIndent())
    }

    fun askAboutFile(fileId: UUID, question: String, userId: UUID, token: String): String {
        trace("ASK ABOUT FILE START", "fileId=$fileId\nuserId=$userId\nquestion=$question")

        return try {
            trace("FETCH FILE METADATA", "fileId=$fileId")
            val fileMetadata = getFileMetadata(fileId, token)
            trace("FILE METADATA RESULT", "fileId=$fileId\nmetadata=${fileMetadata?.filename ?: "null"}")

            trace("DOWNLOAD FILE CONTENT", "fileId=$fileId")
            val fileContent = downloadFileContent(fileId, token)
            trace("FILE CONTENT RESULT", "fileId=$fileId\ncontentLength=${fileContent.length}")

            trace("BUILD FILE CONTEXT", "fileId=$fileId")
            val context = buildFileContext(fileMetadata, fileContent)
            trace("FILE CONTEXT BUILT", "fileId=$fileId\ncontextLength=${context.length}")

            val promptMessage = """
                You are answering a question about ONE document.
                Use the document summary as the primary source of truth.
                If the summary contains the answer, prefer it over the raw content excerpt.
                Do not claim the document has no summary if one is provided below.
                If the document seems to be a PDF/TXT or similar file, mention that only if relevant.
                
                Document: ${fileMetadata?.filename ?: "Unknown"}
                File type: ${fileMetadata?.mimeType ?: "Unknown"}
                Summary: ${fileMetadata?.summary ?: "No summary available"}
                Tags: ${fileMetadata?.tags?.joinToString(", ") ?: "None"}
                
                Content excerpt:
                ${context.take(2000)}
                
                User question: $question
                
                Please provide a clear and concise answer based on the document content.
            """.trimIndent()

            trace("CALLING MODEL FOR FILE", "fileId=$fileId\npromptLength=${promptMessage.length}")

            val response = chatClient.call(Prompt(promptMessage))
            trace("MODEL RESPONSE FOR FILE", "fileId=$fileId\nresponseLength=${response.result?.output?.content?.length ?: 0}")
            response.result?.output?.content ?: "I couldn't generate an answer. Please try rephrasing your question."

        } catch (e: Exception) {
            logger.error("""


!!!!!!!!!!!!!!!!!!!! AI QA SERVICE FILE ERROR !!!!!!!!!!!!!!!!!!!!
fileId=$fileId
userId=$userId
question=$question
error=${e.javaClass.simpleName}: ${e.message}
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


""".trimIndent(), e)
            "Sorry, I encountered an error while processing your question. Error: ${e.message}"
        }
    }

    fun askAcrossDocuments(question: String, userId: UUID, token: String): String {
        return try {
            trace("ASK GLOBAL START", "userId=$userId\nquestion=$question")
            trace("FETCH DOCUMENTS", "userId=$userId")
            val filesMetadata = getAllUserFilesMetadata(userId, token)
            trace("DOCUMENTS FETCHED", "userId=$userId\ndocuments=${filesMetadata.size}")

            if (filesMetadata.isEmpty()) {
                trace("NO DOCUMENTS FOUND", "userId=$userId")
                return "You don't have any documents yet. Please upload some files first."
            }

            handleSimpleInventoryQuestion(question, filesMetadata)?.let { directAnswer ->
                trace("DIRECT INVENTORY ANSWER", "userId=$userId\nanswer=$directAnswer")
                return directAnswer
            }

            trace("BUILD GLOBAL CONTEXT", "userId=$userId")
            val context = filesMetadata.joinToString("\n\n") { file ->
                """
                Document: ${file.filename}
                File type: ${file.mimeType ?: "Unknown"}
                Summary: ${file.summary ?: "No summary"}
                Tags: ${file.tags?.joinToString(", ") ?: "None"}
                """.trimIndent()
            }
            trace("GLOBAL CONTEXT BUILT", "userId=$userId\ncontextLength=${context.length}")

            val promptMessage = """
                You are answering a question across the user's documents.
                The document summaries below are important and should be trusted.
                If the user asks about a file type, count, or whether a document exists, use the document list directly.
                If a document summary answers the question, use that summary first.
                Be concise and factual. Do not invent missing details.
                
                Documents:
                $context
                
                User question: $question
                
                Please provide a clear answer, and if relevant, mention which document(s) contain the information.
            """.trimIndent()

            trace("CALLING MODEL FOR GLOBAL QUESTION", "userId=$userId\npromptLength=${promptMessage.length}")

            val response = chatClient.call(Prompt(promptMessage))
            trace("MODEL RESPONSE FOR GLOBAL QUESTION", "userId=$userId\nresponseLength=${response.result?.output?.content?.length ?: 0}")
            response.result?.output?.content ?: "I couldn't generate an answer. Please try rephrasing your question."

        } catch (e: Exception) {
            logger.error("""


!!!!!!!!!!!!!!!!!!!! AI QA SERVICE GLOBAL ERROR !!!!!!!!!!!!!!!!!!!!
userId=$userId
question=$question
error=${e.javaClass.simpleName}: ${e.message}
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


""".trimIndent(), e)
            "Sorry, I encountered an error while processing your question. Error: ${e.message}"
        }
    }

    private fun getFileMetadata(fileId: UUID, token: String): FileMetadataDTO? {
        return try {
            trace("HTTP FILE METADATA REQUEST", "fileId=$fileId\nurl=$documentServiceBaseUrl/api/v1/documents/$fileId")
            webClient.get()
                .uri("$documentServiceBaseUrl/api/v1/documents/$fileId")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .bodyToMono(DocumentResponse::class.java)
                .map { response ->
                    val doc = response.data
                    FileMetadataDTO(
                        id = fileId.toString(),
                        filename = doc?.fileName ?: doc?.name ?: "Unknown",
                        summary = doc?.summary,
                        tags = doc?.tags,
                        sensitivity = doc?.category,
                        mimeType = doc?.mimeType
                    )
                }
                .block()
        } catch (e: Exception) {
            logger.warn("""


-------------------- FAILED FILE METADATA REQUEST --------------------
fileId=$fileId
error=${e.javaClass.simpleName}: ${e.message}
---------------------------------------------------------------------


""".trimIndent(), e)
            null
        }
    }

    private fun downloadFileContent(fileId: UUID, token: String): String {
        return try {
            trace("HTTP FILE DOWNLOAD REQUEST", "fileId=$fileId\nurl=$documentServiceBaseUrl/api/v1/files/$fileId/download")
            val bytes = webClient.get()
                .uri("$documentServiceBaseUrl/api/v1/files/$fileId/download")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()

            if (bytes != null) {
                String(bytes, Charsets.UTF_8).take(3000)
            } else {
                ""
            }
        } catch (e: Exception) {
            logger.warn("""


-------------------- FAILED FILE DOWNLOAD REQUEST --------------------
fileId=$fileId
error=${e.javaClass.simpleName}: ${e.message}
---------------------------------------------------------------------


""".trimIndent(), e)
            ""
        }
    }

    private fun getAllUserFilesMetadata(userId: UUID, token: String): List<FileMetadataDTO> {
        return try {
            val url = "$documentServiceBaseUrl/api/v1/documents/my"
            trace("HTTP DOCUMENT LIST REQUEST", "userId=$userId\nurl=$url\naccessTokenPrefix=${token.take(20)}...")

            try {
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                    trace("JWT PAYLOAD", "userId=$userId\npayload=$payload")
                }
            } catch (e: Exception) {
                logger.warn("""


-------------------- JWT DEBUG DECODE FAILED --------------------
userId=$userId
error=${e.javaClass.simpleName}: ${e.message}
---------------------------------------------------------------


""".trimIndent())
            }

            val documents = webClient.get()
                .uri("$url?page=0&size=1000")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .onStatus({ it.isError }) { response ->
                    response.bodyToMono(String::class.java).map { body ->
                        logger.error("Error response from DocumentService: status=${response.statusCode()}, body=$body")
                        RuntimeException("DocumentService returned error: ${response.statusCode()}")
                    }
                }
                .bodyToMono(DocumentListResponse::class.java)
                .doOnNext { response ->
                    trace("DOCUMENT LIST RESPONSE", "userId=$userId\ntotal=${response.data?.content?.size ?: 0}")
                }
                .block()

            trace("DOCUMENT LIST COMPLETE", "userId=$userId\ncount=${documents?.data?.content?.size ?: 0}")

            documents?.data?.content?.mapNotNull { doc ->
                trace("DOCUMENT ITEM", "userId=$userId\nid=${doc.id}\nname=${doc.name}")
                FileMetadataDTO(
                    id = doc.id ?: return@mapNotNull null,
                    filename = doc.fileName ?: doc.name ?: "Unknown",
                    summary = doc.summary,
                    tags = doc.tags,
                    sensitivity = doc.category,
                    mimeType = doc.mimeType
                )
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error("""


!!!!!!!!!!!!!!!!!!!! DOCUMENT LIST FETCH FAILED !!!!!!!!!!!!!!!!!!!!
userId=$userId
baseUrl=$documentServiceBaseUrl
error=${e.javaClass.simpleName}: ${e.message}
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


""".trimIndent(), e)
            emptyList()
        }
    }

    private fun buildFileContext(metadata: FileMetadataDTO?, content: String): String {
        val sb = StringBuilder()
        metadata?.let {
            sb.append("Filename: ${it.filename}\n")
            it.mimeType?.let { mimeType -> sb.append("File type: $mimeType\n") }
            it.summary?.let { summary -> sb.append("Summary: $summary\n") }
            it.tags?.let { tags -> sb.append("Tags: ${tags.joinToString(", ")}\n") }
        }
        sb.append("\nContent:\n$content")
        trace("CONTEXT BUILT", "metadata=${metadata?.filename ?: "null"}\nlength=${sb.length}")
        return sb.toString()
    }

    private fun handleSimpleInventoryQuestion(question: String, files: List<FileMetadataDTO>): String? {
        val normalized = question.lowercase()
        val asksAboutPdf = normalized.contains("pdf")
        val asksAboutTxt = normalized.contains("txt") || normalized.contains("text file") || normalized.contains("text files")
        val asksAboutFileType = asksAboutPdf || asksAboutTxt || normalized.contains("file type") || normalized.contains("file types")
        val asksForCount = normalized.contains("how many") || normalized.startsWith("any ") || normalized.contains("any ") || normalized.contains("do i have") || normalized.contains("list") || normalized.contains("show")

        if (!asksAboutFileType || !asksForCount) {
            return null
        }

        val pdfFiles = files.filter { it.isPdf() }
        val txtFiles = files.filter { it.isTxt() }
        val matchedFiles = when {
            asksAboutPdf -> pdfFiles
            asksAboutTxt -> txtFiles
            else -> files
        }

        val targetLabel = when {
            asksAboutPdf -> "PDF"
            asksAboutTxt -> "TXT/text"
            else -> "files"
        }

        if (matchedFiles.isEmpty()) {
            return "No $targetLabel files were found in your documents."
        }

        val names = matchedFiles.joinToString(", ") { it.filename }
        val count = matchedFiles.size
        return when {
            normalized.contains("how many") -> "You have $count $targetLabel files: $names."
            normalized.contains("any ") || normalized.startsWith("any ") || normalized.contains("do i have") -> "Yes — you have $count $targetLabel files: $names."
            normalized.contains("list") || normalized.contains("show") -> "Here are your $count $targetLabel files: $names."
            else -> "You have $count $targetLabel files: $names."
        }
    }

    private fun FileMetadataDTO.isPdf(): Boolean {
        val file = filename.lowercase()
        val mime = mimeType?.lowercase() ?: ""
        return file.endsWith(".pdf") || mime.contains("pdf")
    }

    private fun FileMetadataDTO.isTxt(): Boolean {
        val file = filename.lowercase()
        val mime = mimeType?.lowercase() ?: ""
        return file.endsWith(".txt") || mime.contains("text/plain") || mime.contains("text/")
    }
}

// DTOs matching DocumentService response
data class DocumentListResponse(
    val success: Boolean,
    val data: PagedData?
)

data class PagedData(
    val content: List<DocumentSummaryItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class DocumentSummaryItem(
    val id: String?,
    val name: String?,
    val summary: String?,
    val tags: List<String>?,
    val category: String?,
    val fileName: String?,
    val mimeType: String?
)

data class DocumentResponse(
    val success: Boolean,
    val data: DocumentDetailItem?
)

data class DocumentDetailItem(
    val id: String?,
    val name: String?,
    val summary: String?,
    val tags: List<String>?,
    val category: String?,
    val fileName: String?,
    val mimeType: String?
)
