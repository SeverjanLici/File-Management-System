package com.docplatform.aiservice.dto

data class QuestionRequest(
    val question: String
)

data class AnswerResponse(
    val answer: String,
    val sources: List<String>? = null
)
