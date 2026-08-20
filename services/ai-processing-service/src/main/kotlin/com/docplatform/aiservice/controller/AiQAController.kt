package com.docplatform.aiservice.controller

import com.docplatform.aiservice.dto.QuestionRequest
import com.docplatform.aiservice.dto.AnswerResponse
import com.docplatform.aiservice.service.AiQAService
import com.docplatform.common.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai")
class AiQAController(
    private val aiQAService: AiQAService
) {
    private val logger = LoggerFactory.getLogger(AiQAController::class.java)

    private fun trace(title: String, message: String) {
        logger.info("""


==================== AI QA CONTROLLER: $title ====================
$message
=================================================================


""".trimIndent())
    }

    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @PostMapping("/files/{fileId}/ask")
    fun askAboutFile(
        @PathVariable fileId: UUID,
        @RequestBody request: QuestionRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<AnswerResponse>> {
        trace("ASK FILE START", "fileId=$fileId\nsubject=${jwt.subject}\nquestion=${request.question}")
        val userId = UUID.fromString(jwt.subject)
        val token = jwt.tokenValue
        val answer = aiQAService.askAboutFile(fileId, request.question, userId, token)
        trace("ASK FILE END", "fileId=$fileId\nanswer=${answer.take(400)}")
        return ResponseEntity.ok(ApiResponse.success(AnswerResponse(answer = answer, sources = listOf(fileId.toString()))))
    }

    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @PostMapping("/ask-global")
    fun askGlobal(
        @RequestBody request: QuestionRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<AnswerResponse>> {
        trace("ASK GLOBAL START", "subject=${jwt.subject}\nquestion=${request.question}")
        val userId = UUID.fromString(jwt.subject)
        val token = jwt.tokenValue
        val answer = aiQAService.askAcrossDocuments(request.question, userId, token)
        trace("ASK GLOBAL END", "subject=${jwt.subject}\nanswer=${answer.take(400)}")
        return ResponseEntity.ok(ApiResponse.success(AnswerResponse(answer = answer)))
    }
}
