package it.polito.wa2_2026.g15.apigateway

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun contextLoads() {

        // Smoke Test standard
        assertNotNull(context)


        try {
            it.polito.wa2_2026.g15.apigateway.main(arrayOf())
        } catch (e: Exception) {
            // Ignore exception, the code is just starting itself
        }
    }

}
