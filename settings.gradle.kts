rootProject.name = "document-management-platform"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Shared modules
include(":shared:common-api")
include(":shared:common-security")

// Services
include(":services:api-gateway")
include(":services:user-service")
include(":services:document-service")
include(":services:file-service")
include(":services:ai-processing-service")

// Configure project paths
project(":shared:common-api").projectDir = file("shared/common-api")
project(":shared:common-security").projectDir = file("shared/common-security")
project(":services:api-gateway").projectDir = file("services/api-gateway")
project(":services:user-service").projectDir = file("services/user-service")
project(":services:document-service").projectDir = file("services/document-service")
project(":services:file-service").projectDir = file("services/file-service")
project(":services:ai-processing-service").projectDir = file("services/ai-processing-service")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}
