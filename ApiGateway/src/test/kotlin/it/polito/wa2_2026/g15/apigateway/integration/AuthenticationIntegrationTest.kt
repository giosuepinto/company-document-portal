package it.polito.wa2_2026.g15.apigateway.integration

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    /**
     * Test for Step 8.8: Protected routes and redirection to login[cite: 220].
     */
    @Test
    fun `shouldRedirectToLoginWhenUnauthenticated`() {
        // Simulate a browser so that Spring chooses oauth2Login instead of oauth2ResourceServer
        mockMvc.perform(get("/api/files/test")
            .header("Accept", "text/html"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", containsString("/oauth2/authorization/gateway-client")))
    }

    /**
     * Test for Step 8.8: Logout session terminated[cite: 221].
     */

    @Test
    fun `shouldHandleLogoutSuccessfully`() {
        // Spring Security listens POST requests on "/logout"
        mockMvc.perform(post("/logout"))
            .andExpect(status().isFound) // Wait for 302
            //Expect redirection  to logout endpoint
            .andExpect(header().string("Location", "/"))
    }
}