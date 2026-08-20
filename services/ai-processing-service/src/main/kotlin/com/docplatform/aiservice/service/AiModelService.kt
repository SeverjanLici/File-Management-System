package com.docplatform.aiservice.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class AiModelService(
    webClientBuilder: WebClient.Builder,
    @Value("\${ai.ollama.base-url:http://localhost:11434}")
    private val ollamaBaseUrl: String,
    @Value("\${ai.ollama.model:llama3.2:3b}")
    private val ollamaModel: String,
    @Value("\${ai.ollama.enabled:true}")
    private val ollamaEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val ollamaClient = webClientBuilder.baseUrl(ollamaBaseUrl).build()

    private val categories = listOf(
        "Financial Report",
        "Contract",
        "Invoice",
        "Policy Document",
        "Internal Memo",
        "Project Plan",
        "Technical Specification",
        "Employee Handbook",
        "Marketing Material",
        "Compliance Document"
    )

    fun generateSummary(text: String): String {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return "No text was available for summarization."
        logger.info("Generating summary for text of length: {}", normalizedText.length)

        val prompt = """
            Describe what this file is about using 1 to 2 complete sentences.
            Focus on the actual content and key topic.
            Do not use bullet points or markdown.
            Return only plain text summary.

            DOCUMENT:
            ${normalizedText.take(4000)}
        """.trimIndent()

        return normalizeSummary(callOllama(prompt)).ifBlank { fallbackSummary(normalizedText) }
    }

    fun classifyDocument(text: String): String {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return "Internal Memo"
        logger.info("Classifying document")

        val prompt = """
            Choose exactly one category from this list:
            ${categories.joinToString(", ")}.
            Return only the category text.

            DOCUMENT:
            ${normalizedText.take(3000)}
        """.trimIndent()

        return normalizeCategoryResult(callOllama(prompt))
    }

    fun extractTags(text: String): Set<String> {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return setOf("document")

        val prompt = """
            Extract up to 5 short tags for this document.
            Return only comma-separated tags, lowercase, no explanations.

            DOCUMENT:
            ${normalizedText.take(3000)}
        """.trimIndent()



        val parsed = callOllama(prompt)
            .replace("\n", ",")
            .split(",")
            .map { normalizeTagToken(it) }
            .filter { it.isNotBlank() && !isInstructionalTag(it) }
            .distinct()
            .take(5)

        return if (parsed.isEmpty()) setOf("document") else parsed.toSet()
    }

    private fun normalizeTagToken(token: String): String {
        return token.trim().lowercase().replace("\"", "").replace("'", "")
            .replace(Regex("^[-*\\d.)\\s]+"), "").replace(Regex("^(tags?|keywords?)\\s*:\\s*"), "")
            .replace(Regex("^lowercase\\s*:\\s*"), "").trim().trim(':', ';', '.', '-')
    }

    private fun isInstructionalTag(tag: String): Boolean {
        if (tag.length !in 2..40) return true
        if (tag.contains("here are") || tag.contains("extracted tags") || tag.contains("comma-separated") || tag.contains("format") || tag == "lowercase") return true
        return !tag.any { it.isLetter() }
    }

    private fun callOllama(prompt: String): String {
        if (!ollamaEnabled) {
            logger.warn("Ollama is disabled; using fallback output")
            return ""
        }

        return try {
            val response = ollamaClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(OllamaGenerateRequest(model = ollamaModel, prompt = prompt, stream = false))
                .retrieve()
                .bodyToMono(OllamaGenerateResponse::class.java)
                .block()

            response?.response.orEmpty().trim()
        } catch (ex: Exception) {
            logger.warn("Ollama request failed", ex)
            ""
        }
    }

    private fun normalizeSummary(summary: String) = summary.replace(Regex("\\s+"), " ").trim().trim('"', '\'', '-', '–')

    private fun normalizeCategoryResult(result: String): String {
        val cleaned = result.replace(Regex("\\s+"), " ").trim()
        categories.firstOrNull { it.equals(cleaned, ignoreCase = true) }?.let { return it }
        categories.firstOrNull { cleaned.contains(it, ignoreCase = true) }?.let { return it }
        return inferCategoryFromText(cleaned)
    }

    private fun inferCategoryFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("invoice") -> "Invoice"
            lower.contains("contract") || lower.contains("agreement") -> "Contract"
            lower.contains("policy") -> "Policy Document"
            lower.contains("plan") -> "Project Plan"
            lower.contains("specification") || lower.contains("technical") -> "Technical Specification"
            lower.contains("handbook") -> "Employee Handbook"
            lower.contains("marketing") -> "Marketing Material"
            lower.contains("compliance") -> "Compliance Document"
            lower.contains("financial") || lower.contains("report") -> "Financial Report"
            else -> "Internal Memo"
        }
    }

    private fun fallbackSummary(text: String): String {
        val firstSentence = text.replace(Regex("\\s+"), " ").trim().substringBefore(".").trim()
        return if (firstSentence.isBlank()) "This document contains uploaded content that needs manual review."
        else if (firstSentence.endsWith(".")) firstSentence else "$firstSentence."
    }
}

data class OllamaGenerateRequest(val model: String, val prompt: String, val stream: Boolean = false)
data class OllamaGenerateResponse(val response: String? = null)