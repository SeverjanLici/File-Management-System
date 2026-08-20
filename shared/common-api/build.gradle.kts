plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))

    implementation(libs.bundles.kotlin)
    implementation(libs.spring.boot.starter.validation)
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
}
