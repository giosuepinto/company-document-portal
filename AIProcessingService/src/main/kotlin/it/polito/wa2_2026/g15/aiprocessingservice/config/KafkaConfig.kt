package it.polito.wa2_2026.g15.aiprocessingservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.lang.reflect.Type

@Configuration
class KafkaConfig(private val objectMapper: ObjectMapper) {

    @Bean
    fun converter(): JsonMessageConverter {
        return object : JsonMessageConverter(objectMapper) {
            override fun extractAndConvertValue(record: org.apache.kafka.clients.consumer.ConsumerRecord<*, *>, type: Type): Any {
                var value = record.value()

                if (value is String) {
                    // Pulizia del "double encoding" di Debezium
                    // Se la stringa inizia e finisce con " (es: "{\"id\":1}")
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length > 2) {
                        value = value.substring(1, value.length - 1).replace("\\\"", "\"")
                    }
                    return objectMapper.readValue(value, objectMapper.constructType(type))
                }

                return super.extractAndConvertValue(record, type)
            }
        }
    }

    @Bean
    fun errorHandler(template: KafkaTemplate<Any, Any>): DefaultErrorHandler {
        // Riprova 3 volte ogni 2 secondi
        val backOff = FixedBackOff(2000L, 3)
        return DefaultErrorHandler(
            DeadLetterPublishingRecoverer(template),
            backOff
        )
    }
}