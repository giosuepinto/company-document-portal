package it.polito.wa2.lab03.userdetailservice

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserDetailServiceApplicationTests {

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun contextLoads() {
        // Smoke Test standard
        assertNotNull(context)


        try {
            main(arrayOf())
        } catch (e: Exception) {
            // Ignore exception, the code is just starting itself
        }
    }

}
