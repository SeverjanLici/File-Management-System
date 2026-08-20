package com.docplatform.aiservice.config

import org.springframework.ai.chat.ChatClient
import org.springframework.ai.ollama.OllamaChatClient
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value

@Configuration
class ChatClientConfig {

    @Bean
    fun chatClient(
        ollamaApi: OllamaApi,
        @Value("\${spring.ai.ollama.model:llama3.2:3b}") model: String,
    ): ChatClient {
        return OllamaChatClient(ollamaApi).withModel(model)
    }
}
