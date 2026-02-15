package it.polito.wa2_2026.g15.apigateway.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SpaControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    /**
     * Test per SpaController.
     * Verifica che una rotta "logica" (senza estensione file) venga
     * inoltrata (forward) correttamente a index.html.
     */
    @Test
    @WithMockUser(username = "testUser") // Simuliamo un utente loggato per superare la Security
    fun `shouldForwardToIndexHtmlForSpaRoutes`() {
        // Proviamo una rotta tipica da frontend, es. "/dashboard"
        // La regex nel controller è: "/{path:[^\\.]*}"
        val spaRoute = "/dashboard"

        mockMvc.perform(get(spaRoute))
            // Verifichiamo che la risposta sia un successo (200 OK)
            // Nota: Se index.html non esiste fisicamente nel contesto di test,
            // potresti ricevere 404. In tal caso rimuovi .isOk() e tieni solo forwardedUrl.
            .andExpect(status().isOk())
            // Questo è il cuore del test: verifica che Spring abbia fatto il forward interno
            .andExpect(forwardedUrl("/index.html"))
    }

    /**
     * (Opzionale per robustezza)
     * Verifica che un file con estensione (es. style.css) NON venga catturato
     * da questo controller.
     */
    @Test
    @WithMockUser
    fun `shouldNotForwardStaticAssets`() {
        // Una rotta con un punto non dovrebbe matchare la regex del controller
        // Quindi Spring cercherà la risorsa statica. Se non c'è, darà 404,
        // ma l'importante è che NON faccia forward a index.html
        mockMvc.perform(get("/style.css"))
            .andExpect(forwardedUrl(null)) // Non deve esserci forward
    }
}