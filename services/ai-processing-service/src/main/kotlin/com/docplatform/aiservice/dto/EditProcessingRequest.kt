package com.docplatform.aiservice.dto

data class EditProcessingRequest(
    val extractedSummary: String?,
    val classifiedCategory: String?,
    val generatedTags: List<String>?
)