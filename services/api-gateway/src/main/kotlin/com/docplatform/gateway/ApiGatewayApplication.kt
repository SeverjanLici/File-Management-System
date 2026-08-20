package com.docplatform.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {

    Hooks.enableAutomaticContextPropagation()

    runApplication<ApiGatewayApplication>(*args)
}
