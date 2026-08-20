plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

extra["springCloudVersion"] = "2023.0.0"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}"))

    implementation(libs.bundles.kotlin)
    implementation(libs.spring.cloud.starter.gateway)
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.bundles.observability)

    testImplementation(libs.bundles.testing)
}
