plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "it.polito.wa2_2026.g15"
version = "1.3.0"
description = "ApiGateway"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// Gestione centralizzata della versione Spring Cloud
val springCloudVersion = "2024.0.0" // Compatibile con Spring Boot 3.3.x

dependencies {
    // --- CORE GATEWAY & WEB ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-mvc") // Il cuore del Gateway

    // --- SECURITY & OAUTH2 ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client") // Per il login con Keycloak
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") //abilitare la validazione dei Token JWT sul Gateway

    // --- UTILS ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- OPTIONAL (SWAGGER AGGREGATOR) ---
    // Tienilo se vuoi vedere le API docs sul gateway, altrimenti puoi togliere anche questo
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // --- TESTING ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Fondamentale per far funzionare Spring Cloud Gateway senza conflitti
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    useJUnitPlatform()
}