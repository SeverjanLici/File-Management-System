plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":shared:common-api"))

    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))

    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.spring.security)
    implementation(libs.spring.boot.starter.web)

    testImplementation(libs.bundles.testing)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
