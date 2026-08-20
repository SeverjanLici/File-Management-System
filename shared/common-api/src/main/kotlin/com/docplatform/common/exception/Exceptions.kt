package com.docplatform.common.exception

import java.util.UUID

open class DocumentPlatformException(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class ResourceNotFoundException(
    resourceType: String,
    identifier: Any
) : DocumentPlatformException(
    code = "RESOURCE_NOT_FOUND",
    message = "$resourceType not found: $identifier"
)

class AccessDeniedException(
    message: String = "Access denied"
) : DocumentPlatformException(
    code = "ACCESS_DENIED",
    message = message
)

class ValidationException(
    message: String,
    val details: Map<String, Any>? = null
) : DocumentPlatformException(
    code = "VALIDATION_ERROR",
    message = message
)

class ConflictException(
    message: String
) : DocumentPlatformException(
    code = "CONFLICT",
    message = message
)

class UploadException(
    message: String,
    cause: Throwable? = null
) : DocumentPlatformException(
    code = "UPLOAD_ERROR",
    message = message,
    cause = cause
)

class StorageException(
    message: String,
    cause: Throwable? = null
) : DocumentPlatformException(
    code = "STORAGE_ERROR",
    message = message,
    cause = cause
)
