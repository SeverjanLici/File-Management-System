package com.docplatform.aiservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ExchangeStrategies

@Configuration
class WebClientConfig {

    //only used for the Q&A response feature
    @Bean("plainWebClient")
    fun plainWebClient(): WebClient {
        // Plain WebClient without OAuth2 filter for passing through user tokens
        val strategies = ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) // 16 MB
            }
            .build()

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .build()
    }
}
