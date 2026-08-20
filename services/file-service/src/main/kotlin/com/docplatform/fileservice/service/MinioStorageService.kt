package com.docplatform.fileservice.service

import com.docplatform.common.exception.StorageException
import io.minio.*
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class MinioStorageService(
    private val minioClient: MinioClient,

    @Value("\${minio.bucket-name}")
    private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(MinioStorageService::class.java)

    fun uploadFile(
        objectName: String,
        inputStream: InputStream,
        size: Long,
        contentType: String
    ): String {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            )
            logger.info("Uploaded file to MinIO: $objectName")
            return "$bucketName/$objectName"
        } catch (e: Exception) {
            logger.error("Failed to upload file to MinIO: ${e.message}", e)
            throw StorageException("Failed to upload file to storage", e)
        }
    }

    fun downloadFile(objectName: String): InputStream {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
        } catch (e: Exception) {
            logger.error("Failed to download file from MinIO: ${e.message}", e)
            throw StorageException("Failed to download file from storage", e)
        }
    }

    fun getPresignedUrl(objectName: String, expirationMinutes: Int = 60, downloadFilename: String? = null): Pair<String, Instant> {
        try {
            val builder = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .method(Method.GET)
                .expiry(expirationMinutes, TimeUnit.MINUTES)

            if (downloadFilename != null) {
                builder.extraQueryParams(mapOf("response-content-disposition" to "attachment; filename=\"$downloadFilename\""))
            }

            val url = minioClient.getPresignedObjectUrl(builder.build())
            val expiresAt = Instant.now().plusSeconds(expirationMinutes * 60L)
            return Pair(url, expiresAt)
        } catch (e: Exception) {
            logger.error("Failed to generate presigned URL: ${e.message}", e)
            throw StorageException("Failed to generate download URL", e)
        }
    }

    fun deleteFile(objectName: String) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            logger.info("Deleted file from MinIO: $objectName")
        } catch (e: Exception) {
            logger.error("Failed to delete file from MinIO: ${e.message}", e)
            throw StorageException("Failed to delete file from storage", e)
        }
    }

    fun fileExists(objectName: String): Boolean {
        return try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getFileInfo(objectName: String): StatObjectResponse? {
        return try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
        } catch (e: Exception) {
            null
        }
    }
}
