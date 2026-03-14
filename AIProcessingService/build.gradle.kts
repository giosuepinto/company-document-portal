plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "it.polito.wa2_2026.g15"
version = "1.7.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Repositories sono gestiti in settings.gradle.kts (NON TOCCARLI)

dependencyManagement {
    imports {
        // Usiamo M6 che è stabile per tutto il resto
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0-M6")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- SOLUZIONE UNIVERSALE ---
    // Usiamo il driver OpenAI (che esiste sempre) per pilotare Gemini
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    // PER GLI EMBEDDING LOCALI (Risolve il crash)
    implementation("org.springframework.ai:spring-ai-transformers-spring-boot-starter")

    // PgVector (Versione M6 stabile)
    implementation("org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    //Fix for Windows 11: force use of CPU for pytorch
    runtimeOnly("ai.djl.pytorch:pytorch-native-cpu:2.4.0:win-x86_64")

    //Test
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("com.h2database:h2")

    implementation("org.springframework.ai:spring-ai-tika-document-reader:1.0.0-M6") // useful for file reading

    // OpenTelemetry
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.38.0"))
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.3.0-alpha"))
    implementation("io.opentelemetry.instrumentation:opentelemetry-kafka-clients-2.6")
    // OpenTelemetry starter for the integration with Spring Boot
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    // OTLP Explorator (sends data to the Collector)
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Genera le metriche (CPU, RAM, API)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Invia le metriche generate da Actuator al Collector in formato OTLP
    implementation("io.micrometer:micrometer-registry-otlp")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
