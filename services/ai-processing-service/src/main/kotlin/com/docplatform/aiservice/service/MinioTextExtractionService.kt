package com.docplatform.aiservice.service

import com.docplatform.common.event.FileUploadedEvent
import io.minio.GetObjectArgs
import io.minio.MinioClient
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Service
class MinioTextExtractionService(
    @Value("\${minio.endpoint:http://localhost:9000}")
    endpoint: String,
    @Value("\${minio.access-key:minioadmin}")
    accessKey: String,
    @Value("\${minio.secret-key:minioadmin123}")
    secretKey: String,
    @Value("\${minio.bucket-name:documents}")
    private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val maxExtractedChars = 12_000
    private val maxPlainTextBytes = 1_000_000
    private val maxPdfPages = 12

    private val minioClient: MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()

    fun extractText(event: FileUploadedEvent): String {
        val objectName = extractObjectName(event.storagePath)
        val contentType = event.contentType.substringBefore(';').trim().lowercase()

        return try {
            minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            ).use { stream ->
                when {
                    isPdfLike(contentType, objectName) -> extractPdfText(stream)
                    isTextLike(contentType) || isTextExtension(objectName) -> extractPlainText(stream)
                    else -> ""
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract text from MinIO object: {}", objectName, e)
            throw IllegalStateException("Failed to extract text from uploaded file", e)
        }
    }

    private fun extractObjectName(storagePath: String): String {
        // storagePath is typically "<bucket>/<objectName>"
        val prefix = "$bucketName/"
        return if (storagePath.startsWith(prefix)) {
            storagePath.removePrefix(prefix)
        } else {
            storagePath.substringAfter('/', storagePath)
        }
    }

    private fun isTextLike(contentType: String): Boolean {
        return contentType.startsWith("text/") ||
            contentType.contains("json") ||
            contentType.contains("xml") ||
            contentType.contains("csv")
    }

    private fun isTextExtension(objectName: String): Boolean {
        val lower = objectName.lowercase()
        return lower.endsWith(".txt") ||
            lower.endsWith(".md") ||
            lower.endsWith(".csv") ||
            lower.endsWith(".json") ||
            lower.endsWith(".xml") ||
            lower.endsWith(".log")
    }

    private fun isPdfLike(contentType: String, objectName: String): Boolean {
        return contentType == "application/pdf" || objectName.lowercase().endsWith(".pdf")
    }

    private fun extractPdfText(stream: InputStream): String {
        return PDDocument.load(stream).use { document ->
            if (document.numberOfPages <= 0) return ""

            val text = PDFTextStripper().apply {
                startPage = 1
                endPage = minOf(document.numberOfPages, maxPdfPages)
            }.getText(document)
                .replace(Regex("\\s+"), " ")
                .trim()
            text.take(maxExtractedChars)
        }
    }

    private fun extractPlainText(stream: InputStream): String {
        val bytes = stream.readNBytes(maxPlainTextBytes)
        if (bytes.isEmpty()) return ""

        return String(bytes, StandardCharsets.UTF_8)
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxExtractedChars)
    }
}

