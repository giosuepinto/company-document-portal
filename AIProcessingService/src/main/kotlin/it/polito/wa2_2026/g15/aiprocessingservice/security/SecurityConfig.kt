package it.polito.wa2_2026.g15.aiprocessingservice.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Abilita @PreAuthorize
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                // Swagger e Actuator pubblici
                auth.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // ATTENZIONE: Se TUS richiede OPTIONS (preflight) pubblici, permettili esplicitamente
                auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // Tutto il resto richiede autenticazione
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    // Usa il nostro convertitore personalizzato
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .csrf { it.disable() } // Backend stateless, niente CSRF

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): org.springframework.web.cors.CorsConfigurationSource {
        val configuration = org.springframework.web.cors.CorsConfiguration()
        // In produzione, mettere solo l'URL del Gateway. Per il lab, "*" va bene. (forse)
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        // Espone gli header necessari a TUS
        configuration.exposedHeaders = listOf(
            "Tus-Resumable", "Tus-Version", "Tus-Extension", "Tus-Max-Size",
            "Upload-Offset", "Upload-Length", "Location", "Content-Disposition"
        )
        val source = org.springframework.web.cors.UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    // Collega il converter dei ruoli al converter di autenticazione standard
    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(KeycloakRealmRoleConverter())
        return converter
    }
}