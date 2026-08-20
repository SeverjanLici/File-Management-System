plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":shared:common-api"))
    implementation(project(":shared:common-security"))

    implementation(libs.bundles.kotlin)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.bundles.spring.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.minio)
    implementation("org.apache.pdfbox:pdfbox:2.0.31") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    
    // HTTP Client
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Spring AI for Chat functionality with Ollama
    val springAiVersion = "0.8.1"
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-retry")

    runtimeOnly(libs.postgresql)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}
