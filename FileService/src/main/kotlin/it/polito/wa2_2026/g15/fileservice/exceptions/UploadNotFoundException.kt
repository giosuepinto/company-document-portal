package it.polito.wa2_2026.g15.fileservice.exceptions

class UploadNotFoundException(message: String = "Upload ID not found.") : RuntimeException(message)