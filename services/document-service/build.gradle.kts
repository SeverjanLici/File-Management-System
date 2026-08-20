plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":shared:common-api"))
    implementation(project(":shared:common-security"))

    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))

    implementation(libs.bundles.kotlin)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.bundles.spring.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.bundles.observability)

    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly(libs.postgresql)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}
