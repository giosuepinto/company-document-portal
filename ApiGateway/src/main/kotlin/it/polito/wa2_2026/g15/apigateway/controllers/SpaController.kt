package it.polito.wa2_2026.g15.apigateway.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class SpaController {

    // Cattura tutti i path che non hanno un'estensione (es. /admin, /files)
    // Esclude /api/** che sono gestiti dal Gateway MVC
    @RequestMapping(value = ["/","/{path:[^\\.]*}"])
    fun redirect(): String {
        // Forwarda a index.html (che è nella root di static)
        return "forward:/index.html"
    }
}