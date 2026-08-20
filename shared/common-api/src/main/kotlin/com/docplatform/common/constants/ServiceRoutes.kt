package com.docplatform.common.constants

object ServiceRoutes {
    const val API_PREFIX = "/api/v1"

    object UserService {
        const val BASE = "/users"
        const val ME = "/me"
        const val DEPARTMENTS = "/departments"
        const val ROLES = "/roles"
    }

    object DocumentService {
        const val BASE = "/documents"
        const val SHARES = "/shares"
        const val ACCESS_CHECK = "/internal/access-check"
    }

    object FileService {
        const val UPLOAD = "/upload"
        const val FILES = "/files"
        const val PRESIGNED = "/presigned"
    }
}

object HttpHeaders {
    const val UPLOAD_LENGTH = "Upload-Length"
    const val UPLOAD_OFFSET = "Upload-Offset"
    const val UPLOAD_METADATA = "Upload-Metadata"
    const val TUS_RESUMABLE = "Tus-Resumable"
    const val TUS_VERSION = "1.0.0"
    const val LOCATION = "Location"
}

object ContentTypes {
    const val APPLICATION_OFFSET_OCTET_STREAM = "application/offset+octet-stream"
}
