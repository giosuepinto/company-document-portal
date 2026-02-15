plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "it.polito.wa2_2026.g15"
version = "1.2.1"
description = "TusUploaderService"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy.force(
        "com.squareup.okio:okio:3.14.9",
        "com.squareup.okhttp3:okhttp:4.11.0"
    )
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core:11.15.0") //Flyway
    implementation("org.flywaydb:flyway-database-postgresql:11.15.0")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-docker-compose") //Docker
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.minio:minio:8.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0") // to build the project without conflicts (due to Kotlin version)
    implementation("com.squareup.okio:okio:3.2.0") // to build the project without conflicts (due to Kotlin version)
    implementation("org.apache.commons:commons-lang3:3.18.0") // for vulnerability issues with minio 8.6.0

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:minio")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    //Testing Security
    testImplementation("org.springframework.security:spring-security-test")

    // security
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}
