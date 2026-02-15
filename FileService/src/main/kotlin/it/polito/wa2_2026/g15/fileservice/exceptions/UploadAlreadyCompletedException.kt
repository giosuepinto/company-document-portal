package it.polito.wa2_2026.g15.fileservice.exceptions

class UploadAlreadyCompletedException(message: String = "File upload already completed.") : RuntimeException(message)