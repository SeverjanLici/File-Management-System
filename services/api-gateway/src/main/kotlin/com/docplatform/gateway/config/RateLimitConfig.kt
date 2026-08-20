package com.docplatform.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class RateLimitConfig {

    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val principal = exchange.request.headers.getFirst("Authorization")
                ?.removePrefix("Bearer ")
                ?.take(32)
                ?: exchange.request.remoteAddress?.address?.hostAddress
                ?: "anonymous"
            Mono.just(principal)
        }
    }
}
