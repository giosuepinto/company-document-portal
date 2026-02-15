package it.polito.wa2_2026.g15.fileservice.controllers

import it.polito.wa2_2026.g15.fileservice.exceptions.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class FileExceptionHandler {

    private val logger = LoggerFactory.getLogger(FileExceptionHandler::class.java)

    /**
     * Handles 404 - File Not Found.
     * Triggered when a requested persistent file or version does not exist.
     */
    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(e: FileNotFoundException): ProblemDetail {
        val detail = e.message ?: "File not found"
        logger.warn("File API Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail)
    }

    /**
     * Handles 404 - Target User Not Found.
     * Triggered when trying to share a file with a user that does not exist in UserDetailService.
     */
    @ExceptionHandler(TargetUserNotFoundException::class)
    fun handleTargetUserNotFound(e: TargetUserNotFoundException): ProblemDetail {
        val detail = e.message ?: "Target user not found"
        logger.warn("File API Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail)
    }

    /**
     * Handles 409 - File Already Shared.
     * Triggered to enforce idempotency when a share already exists.
     */
    @ExceptionHandler(FileAlreadySharedException::class)
    fun handleFileAlreadyShared(e: FileAlreadySharedException): ProblemDetail {
        val detail = e.message ?: "Share already exists"
        logger.warn("File API Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail)
    }

    /**
     * Handles 400 - Illegal Argument.
     * Generic handler for invalid UUIDs or bad request parameters.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ProblemDetail {
        val detail = e.message ?: "Invalid argument"
        logger.warn("File API Error: Bad Request - {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
    }

    @ExceptionHandler(PermissionDeniedException::class)
    fun handlePermissionDenied(e: PermissionDeniedException): ProblemDetail {
        // Logghiamo l'evento di sicurezza
        logger.warn("Security Audit: Access Denied - {}", e.message)

        // Restituiamo un ProblemDetail standard con status 403
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.message ?: "Access denied")
    }
}