package it.polito.wa2_2026.g15.apigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val clientRegistrationRepository: ClientRegistrationRepository
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/index.html", "/*.ico","/assets/**", "/error","/login/**", "/webjars/**", "/oauth2/**", "/*.svg",).permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2Login { }
            .logout { logout ->
                logout.logoutSuccessHandler(oidcLogoutSuccessHandler())
            }
            // --- ENABLE CSRF HERE ---
            .csrf { csrf ->
                csrf
                    // 1. Store the token in a cookie named "XSRF-TOKEN"
                    // 2. setHttpOnly(false) allows React to read it via JavaScript
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

                    // 3. Essential for SPA: Makes the token available as a request attribute immediately
                    .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            }
            .addFilterAfter(CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter::class.java) // <--- AGGIUNGI QUESTO
        // ------------------------

        return http.build()
    }
    private fun oidcLogoutSuccessHandler(): LogoutSuccessHandler {
        val successHandler = OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)
        // Dopo il logout, torna alla home del gateway
        successHandler.setPostLogoutRedirectUri("http://localhost:8080/")
        return successHandler
    }
}