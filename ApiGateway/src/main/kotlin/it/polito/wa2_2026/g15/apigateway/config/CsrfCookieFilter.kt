package it.polito.wa2_2026.g15.apigateway.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // Forza il caricamento del token CSRF per assicurare che il cookie venga inviato al browser
        val csrfToken = request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
        csrfToken?.token
        filterChain.doFilter(request, response)
    }
}