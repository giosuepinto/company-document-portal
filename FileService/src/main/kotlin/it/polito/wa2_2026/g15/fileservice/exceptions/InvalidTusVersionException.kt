package it.polito.wa2_2026.g15.fileservice.exceptions

class InvalidTusVersionException(message: String = "Only TUS version 1.0.0 supported.") : RuntimeException(message)