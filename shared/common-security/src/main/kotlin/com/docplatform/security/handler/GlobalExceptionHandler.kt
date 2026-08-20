package com.docplatform.security.handler

import com.docplatform.common.dto.ApiResponse
import com.docplatform.common.dto.ErrorDto
import com.docplatform.common.exception.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Resource not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message)))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Access denied: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message)))
    }

    @ExceptionHandler(SpringAccessDeniedException::class)
    fun handleSpringAccessDenied(ex: SpringAccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Spring access denied: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ErrorDto("ACCESS_DENIED", ex.message ?: "Access denied")))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Validation error: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message, ex.details)))
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Conflict: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message)))
    }

    @ExceptionHandler(UploadException::class)
    fun handleUpload(ex: UploadException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Upload error: ${ex.message}", ex.cause)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message)))
    }

    @ExceptionHandler(StorageException::class)
    fun handleStorage(ex: StorageException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Storage error: ${ex.message}", ex.cause)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message)))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        logger.warn("Validation errors: $errors")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorDto("VALIDATION_ERROR", "Validation failed", errors)))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Malformed request payload: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponse.error(
                    ErrorDto(
                        "INVALID_REQUEST_BODY",
                        "Request body is invalid or missing required fields"
                    )
                )
            )
    }

    @ExceptionHandler(DocumentPlatformException::class)
    fun handleDocumentPlatform(ex: DocumentPlatformException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Platform error: ${ex.message}", ex.cause)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorDto(ex.code, ex.message)))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorDto("INTERNAL_ERROR", "An unexpected error occurred")))
    }
}
