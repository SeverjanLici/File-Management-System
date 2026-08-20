package com.docplatform.fileservice.config

import me.desair.tus.server.TusFileUploadService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class TusConfig(
    @Value("\${tus.storage-path}")
    private val storagePath: String,

    @Value("\${tus.upload-uri}")
    private val uploadUri: String
) {

    @Bean
    fun tusFileUploadService(): TusFileUploadService {
        val storageDir = File(storagePath)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return TusFileUploadService()
            .withStoragePath(storagePath)
            .withUploadUri(uploadUri)
            .withMaxUploadSize(1024L * 1024L * 1024L) // 1GB max
            .withDownloadFeature()
    }
}
