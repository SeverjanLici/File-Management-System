package com.docplatform.aiservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.kafka.annotation.EnableKafka

@EnableAsync
@EnableKafka
@SpringBootApplication
class AiProcessingServiceApplication

fun main(args: Array<String>) {
    runApplication<AiProcessingServiceApplication>(*args)
}

