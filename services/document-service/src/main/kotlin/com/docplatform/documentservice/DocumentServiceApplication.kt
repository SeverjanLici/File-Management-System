package com.docplatform.documentservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.docplatform.documentservice", "com.docplatform.security"])
class DocumentServiceApplication

fun main(args: Array<String>) {
    runApplication<DocumentServiceApplication>(*args)
}
