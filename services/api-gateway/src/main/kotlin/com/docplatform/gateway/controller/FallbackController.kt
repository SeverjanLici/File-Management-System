package com.docplatform.gateway.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/fallback")
class FallbackController {

    @RequestMapping("/user-service")
    fun userServiceFallback(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                    mapOf(
                        "success" to false,
                        "error" to mapOf(
                            "code" to "SERVICE_UNAVAILABLE",
                            "message" to "User service is currently unavailable. Please try again later."
                        )
                    )
                )
        )
    }

    @RequestMapping("/document-service")
    fun documentServiceFallback(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                    mapOf(
                        "success" to false,
                        "error" to mapOf(
                            "code" to "SERVICE_UNAVAILABLE",
                            "message" to "Document service is currently unavailable. Please try again later."
                        )
                    )
                )
        )
    }

    @RequestMapping("/file-service")
    fun fileServiceFallback(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                    mapOf(
                        "success" to false,
                        "error" to mapOf(
                            "code" to "SERVICE_UNAVAILABLE",
                            "message" to "File service is currently unavailable. Please try again later."
                        )
                    )
                )
        )
    }
}
