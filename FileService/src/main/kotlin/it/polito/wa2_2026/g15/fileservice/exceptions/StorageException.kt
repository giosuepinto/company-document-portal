package it.polito.wa2_2026.g15.fileservice.exceptions

class StorageException(message: String = "Not able to read/write fromt/into the storage.") : RuntimeException(message)