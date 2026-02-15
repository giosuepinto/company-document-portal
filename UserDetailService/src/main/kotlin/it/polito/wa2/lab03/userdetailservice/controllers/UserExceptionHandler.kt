package it.polito.wa2.lab03.userdetailservice.controllers

import it.polito.wa2.lab03.userdetailservice.exceptions.DuplicateEmailException
import it.polito.wa2.lab03.userdetailservice.exceptions.EmailNotFoundException
import it.polito.wa2.lab03.userdetailservice.exceptions.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class UserExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(UserExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ProblemDetail {
        log.warn("User Error: Resource Not Found - {}", e.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "User not found")
    }

    @ExceptionHandler(EmailNotFoundException::class)
    fun handleEmailNotFound(e: EmailNotFoundException): ProblemDetail {
        log.warn("User Error: Email Not Found - {}", e.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Email not found")
    }

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(e: DuplicateEmailException): ProblemDetail {
        log.warn("User Error: Conflict - {}", e.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Email already exists")
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ProblemDetail {
        log.error("User Service Critical Error: {}", e.message, e)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred")
    }

    @ExceptionHandler(AuthorizationDeniedException::class, AccessDeniedException::class)
    fun handleSpringSecurityAccessDenied(e: Exception): ProblemDetail {
        // Il log di sicurezza viene lanciato qui
        logger.warn("Security Audit: Access Denied by PreAuthorize - {}", e)

        // L'errore viene esplicitamente mappato a 403 Forbidden
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Access Denied: You do not have the required role for this resource."
        )
    }
}