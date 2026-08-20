package com.docplatform.fileservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/v1/upload/**")
            .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://localhost")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
            .allowedHeaders(
                "Authorization",
                "Content-Type",
                "Upload-Length",
                "Upload-Offset",
                "Upload-Metadata",
                "Tus-Resumable",
                "X-Requested-With"
            )
            .exposedHeaders(
                "Location",
                "Upload-Offset",
                "Upload-Length",
                "Tus-Resumable",
                "Tus-Version",
                "Tus-Extension",
                "Tus-Max-Size",
                "X-Upload-Complete",
                "X-File-Id"
            )
            .allowCredentials(true)
            .maxAge(3600)

        registry.addMapping("/api/v1/files/**")
            .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://localhost")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(true)
            .maxAge(3600)
    }
}

