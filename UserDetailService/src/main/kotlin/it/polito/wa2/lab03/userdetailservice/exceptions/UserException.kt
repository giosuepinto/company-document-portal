package it.polito.wa2.lab03.userdetailservice.exceptions

import java.util.UUID

class UserNotFoundException(id: UUID) : RuntimeException("User with ID $id not found")
class DuplicateEmailException(email: String) : RuntimeException("User with email $email already exists")
class EmailNotFoundException(email: String) : RuntimeException("User with email $email does not exist")