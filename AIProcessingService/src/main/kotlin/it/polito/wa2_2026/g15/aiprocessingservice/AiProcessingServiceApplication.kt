package it.polito.wa2_2026.g15.aiprocessingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AiProcessingServiceApplication

fun main(args: Array<String>) {
    runApplication<AiProcessingServiceApplication>(*args)
}
