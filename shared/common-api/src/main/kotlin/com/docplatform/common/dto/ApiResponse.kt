package com.docplatform.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDto? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun <T> error(error: ErrorDto): ApiResponse<T> = ApiResponse(success = false, error = error)
        fun <T> error(code: String, message: String): ApiResponse<T> =
            ApiResponse(success = false, error = ErrorDto(code, message))
    }
}

data class ErrorDto(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
