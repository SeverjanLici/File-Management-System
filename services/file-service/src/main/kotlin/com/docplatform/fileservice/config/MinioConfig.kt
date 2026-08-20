package com.docplatform.fileservice.config

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class MinioConfig(
    @Value("\${minio.endpoint}")
    private val endpoint: String,

    @Value("\${minio.access-key}")
    private val accessKey: String,

    @Value("\${minio.secret-key}")
    private val secretKey: String,

    @Value("\${minio.bucket-name}")
    private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(MinioConfig::class.java)

    @Bean
    fun minioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    @Bean
    fun initMinioBucket(minioClient: MinioClient): Boolean {
        var attempt = 0
        val maxAttempts = 6
        val sleepMillis = 2000L

        while (attempt < maxAttempts) {
            try {
                val bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
                )

                if (!bucketExists) {
                    minioClient.makeBucket(
                        MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
                    )
                    logger.info("Created MinIO bucket: $bucketName")
                } else {
                    logger.info("MinIO bucket already exists: $bucketName")
                }

                // success - break out
                return true
            } catch (e: Exception) {
                attempt++
                logger.warn("Attempt $attempt/$maxAttempts: Failed to initialize MinIO bucket: ${e.message}")
                if (attempt >= maxAttempts) {
                    logger.error("Failed to initialize MinIO bucket after $maxAttempts attempts: ${e.message}")
                    return false
                }
                try {
                    Thread.sleep(sleepMillis)
                } catch (_: InterruptedException) {
                }
            }
        }
        return false
    }
}
