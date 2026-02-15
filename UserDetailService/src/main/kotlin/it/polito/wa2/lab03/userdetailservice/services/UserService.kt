package it.polito.wa2.lab03.userdetailservice.services

import it.polito.wa2.lab03.userdetailservice.dto.UserDTO
import it.polito.wa2.lab03.userdetailservice.dto.UserRequestDTO
import it.polito.wa2.lab03.userdetailservice.dto.UserUpdateDTO
import it.polito.wa2.lab03.userdetailservice.dto.toDTO
import it.polito.wa2.lab03.userdetailservice.dto.toEntity
import it.polito.wa2.lab03.userdetailservice.entities.Role
import it.polito.wa2.lab03.userdetailservice.entities.User
import it.polito.wa2.lab03.userdetailservice.exceptions.DuplicateEmailException
import it.polito.wa2.lab03.userdetailservice.exceptions.EmailNotFoundException
import it.polito.wa2.lab03.userdetailservice.exceptions.UserNotFoundException
import it.polito.wa2.lab03.userdetailservice.repositories.UserRepository
import it.polito.wa2.lab03.userdetailservice.utils.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Retrieves all users with pagination support.
     */
    @Transactional(readOnly = true)
    fun getAllUsers(pageable: Pageable): Page<UserDTO> {
        logger.debug("Fetching all users with pagination: {}", pageable)
        return userRepository.findAll(pageable).map { it.toDTO() }
    }

    /**
     * Retrieves a specific user by ID.
     */
    @Transactional(readOnly = true)
    fun getUserById(id: UUID): UserDTO {
        logger.debug("Fetching user by ID: {}", id)

        val user = userRepository.findById(id)
            .orElseThrow {
                logger.warn("User lookup failed: ID {} not found", id)
                UserNotFoundException(id)
            }

        return user.toDTO()
    }

    /**
     * Retrieves the authenticated user.
     */
    @Transactional
    fun getMeUser(): UserDTO {
        logger.debug("Fetching me user")

        val currentUserPrincipal = securityUtils.getCurrentPrincipal()
        val id = currentUserPrincipal.id

        val userEntity = userRepository.findById(id).orElse(null)

        val userToReturn = if (userEntity == null) {
            logger.warn("Logged user lookup failed: ID {} not found. Starting JIT provisioning...", id)

            val newUserEntity = User()
            newUserEntity.id = id
            newUserEntity.fullName = currentUserPrincipal.fullName
            newUserEntity.email = currentUserPrincipal.email
            newUserEntity.organization = "Default Organization"
            newUserEntity.role = Role.STAFF // by default
            newUserEntity.department = null
            newUserEntity.storageQuota = null
            newUserEntity.notes = "Provisioned via JIT at first login"

            val savedUser = userRepository.save(newUserEntity)
            savedUser.toDTO()
        } else {
            // Se l'utente esiste, restituisci il DTO
            userEntity.toDTO()
        }

        return userToReturn
    }

    /**
     * Retrieves a user by email.
     */
    @Transactional(readOnly = true)
    fun getUserByEmail(email: String): UserDTO {
        logger.debug("Fetching user by email: {}", email)

        val user = userRepository.findByEmail(email)
            .orElseThrow {
                logger.warn("User lookup failed: Email {} not found", email)
                EmailNotFoundException(email)
            }

        return user.toDTO()
    }

    /**
     * Creates a new user profile.
     * Checks for duplicate emails before saving.
     */
    fun createUser(userRequest: UserRequestDTO): UserDTO {
        logger.info("Request to create new user: {}", userRequest.email)

        if (userRepository.existsByEmail(userRequest.email)) {
            logger.warn("Creation failed: Email {} already in use", userRequest.email)
            throw DuplicateEmailException(userRequest.email)
        }

        if (userRequest.id == null) {
            logger.warn("Creation failed: User ID is required in the payload for this operation.")
            throw IllegalArgumentException("User ID must be provided in the payload for creation.")
        }

        val newUser = userRequest.toEntity()
        val savedUser = userRepository.save(newUser)

        logger.info("User created successfully with ID: {}", savedUser.id)
        return savedUser.toDTO()
    }

    /**
     * Updates an existing user profile.
     * Handles optimistic locking and email uniqueness checks.
     */
    fun updateUser(id: UUID, userRequest: UserUpdateDTO): UserDTO { // <--- CAMBIO QUI: Usa UserUpdateDTO
        logger.info("Request to update user {}", id)

        val userToUpdate = userRepository.findById(id)
            .orElseThrow {
                logger.warn("Update failed: User {} not found", id)
                UserNotFoundException(id)
            }

        // Check if email is being changed to one that already exists
        if (userToUpdate.email != userRequest.email && userRepository.existsByEmail(userRequest.email)) {
            logger.warn("Update failed: Email {} is taken by another user", userRequest.email)
            throw DuplicateEmailException(userRequest.email)
        }

        // Apply updates
        userToUpdate.apply {
            fullName = userRequest.fullName
            email = userRequest.email
            organization = userRequest.organization
            role = userRequest.role
            department = userRequest.department
            storageQuota = userRequest.storageQuota
            notes = userRequest.notes
        }

        val updatedUser = userRepository.save(userToUpdate)
        logger.info("User {} updated successfully", id)

        return updatedUser.toDTO()
    }

    /**
     * Deletes a user profile.
     */
    fun deleteUser(id: UUID) {
        logger.info("Request to delete user {}", id)

        if (!userRepository.existsById(id)) {
            logger.warn("Delete failed: User {} not found", id)
            throw UserNotFoundException(id)
        }

        userRepository.deleteById(id)
        logger.info("User {} deleted successfully", id)
    }

    @Transactional(readOnly = true)
    fun getUserIdsByDepartment(department: String): List<UUID> {
        logger.debug("Fetching user IDs for department: {}", department)
        return userRepository.findAllByDepartment(department).mapNotNull { it.id }
    }

}