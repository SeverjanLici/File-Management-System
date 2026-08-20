package com.docplatform.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:5173", "http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Upload-Length",
                "Upload-Offset",
                "Upload-Metadata",
                "Tus-Resumable",
                "X-Requested-With"
            )
            exposedHeaders = listOf(
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
            allowCredentials = true
            maxAge = 3600
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)

        return CorsWebFilter(source)
    }
}
