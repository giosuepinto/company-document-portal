package it.polito.wa2_2026.g15.aiprocessingservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

        val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        )
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
        return authorizedClientManager
    }

    @Bean
    fun aiServiceClient(authorizedClientManager: OAuth2AuthorizedClientManager): RestClient {
        val oauth2Interceptor = OAuth2ClientHttpRequestInterceptor(authorizedClientManager)
        oauth2Interceptor.setClientRegistrationIdResolver { "keycloak" }

        return RestClient.builder()
            .requestInterceptor(oauth2Interceptor)
            .build()
    }

}