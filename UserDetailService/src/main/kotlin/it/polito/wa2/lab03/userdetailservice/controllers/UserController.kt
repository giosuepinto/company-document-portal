package it.polito.wa2.lab03.userdetailservice.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import it.polito.wa2.lab03.userdetailservice.dto.UserDTO
import it.polito.wa2.lab03.userdetailservice.dto.UserRequestDTO
import it.polito.wa2.lab03.userdetailservice.dto.UserUpdateDTO
import it.polito.wa2.lab03.userdetailservice.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Endpoints for managing user business data and profiles.")
class UserController(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @Operation(summary = "List all users", description = "Retrieves a paginated list of registered users.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "List retrieved successfully"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @GetMapping
    @PreAuthorize("hasAnyRole('admin')") // RBAC
    fun getAllUsers(
        @Parameter(description = "Pagination parameters (page, size, sort)", hidden = true)
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<UserDTO> {
        logger.info("REST: Request to list users with pagination: {}", pageable)
        return userService.getAllUsers(pageable)
    }

    @Operation(summary = "Get (me) user details", description = "Retrieves detailed information about the authenticated user.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "400", description = "Invalid UUID format")
    ])
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')") // RBAC
    fun getMeUser(
    ): UserDTO {

        logger.info("REST: Request to get logged user")
        return userService.getMeUser()
    }

    @Operation(summary = "Get user details", description = "Retrieves detailed information about a specific user by ID.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "400", description = "Invalid UUID format")
    ])
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'service-account')") // RBAC
    fun getUserById(
        @Parameter(description = "UUID of the user to retrieve")
        @PathVariable id: UUID
    ): UserDTO {
        logger.info("REST: Request to get user {}", id)
        return userService.getUserById(id)
    }

    @Operation(summary = "Get user by email", description = "Retrieves user details searching by email address.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff')") // RBAC
    fun getUserByEmail(
        @Parameter(description = "The email address to search for")
        @PathVariable email: String
    ): UserDTO {
        logger.info("REST: Request to find user by email: {}", email)
        return userService.getUserByEmail(email)
    }

    @Operation(summary = "Register new user", description = "Creates a new user profile.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid input data"),
        ApiResponse(responseCode = "409", description = "Conflict: Email already exists")
    ])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin')") // RBAC
    fun createUser(@Valid @RequestBody userRequest: UserRequestDTO): UserDTO {
        logger.info("REST: Request to create user: {}", userRequest.email)
        return userService.createUser(userRequest)
    }

    @Operation(summary = "Update user profile", description = "Updates an existing user's information.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User updated successfully"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "409", description = "Email conflict")
    ])
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin')")
    fun updateUser(
        @Parameter(description = "UUID of the user to update")
        @PathVariable id: UUID,
        @Valid @RequestBody userRequest: UserUpdateDTO
    ): UserDTO {
        logger.info("REST: Request to update user {}", id)
        return userService.updateUser(id, userRequest)
    }

    @Operation(summary = "Delete user", description = "Removes a user from the system.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "User deleted successfully"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin')")
    fun deleteUser(
        @Parameter(description = "UUID of the user to delete")
        @PathVariable id: UUID
    ) {
        logger.info("REST: Request to delete user {}", id)
        userService.deleteUser(id)
    }


    @Operation(summary = "Get user IDs by department", description = "Returns a list of User UUIDs belonging to a specific department. Used for M2M aggregation.")
    @GetMapping("/ids")
    @PreAuthorize("hasAnyRole('admin', 'service-account')") // Fondamentale: service-account deve poter chiamare
    fun getUserIdsByDepartment(
        @RequestParam department: String
    ): List<UUID> {
        logger.info("REST: Request user IDs for department: {}", department)
        return userService.getUserIdsByDepartment(department)
    }
}