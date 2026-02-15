package it.polito.wa2.lab03.userdetailservice.integration

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.lab03.userdetailservice.dto.UserDTO
import it.polito.wa2.lab03.userdetailservice.dto.UserRequestDTO
import it.polito.wa2.lab03.userdetailservice.entities.Role
import it.polito.wa2.lab03.userdetailservice.services.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var userService: UserService

    private val userId = UUID.randomUUID()
    private val userEmail = "mario.rossi@polito.it"

    // Helper to create a valid response DTO
    private fun createMockUserDTO(role: Role = Role.STAFF) = UserDTO(
        id = userId,
        fullName = "Mario Rossi",
        email = userEmail,
        organization = "Polito",
        role = role,
        department = "IT",
        storageQuota = 1000L,
        notes = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    // --- HUMAN USER ACCESS TEST (/me) ---

    @Test
    fun `getMeUser should allow Staff Role`() {
        val mockUser = createMockUserDTO(Role.STAFF)
        `when`(userService.getMeUser()).thenReturn(mockUser)

        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_staff"))

        mockMvc.perform(get("/api/v1/users/me").with(jwtToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(userEmail))
            .andExpect(jsonPath("$.role").value("STAFF"))
    }

    @Test
    fun `getMeUser should reject Anonymous`() {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }

    // --- ADMIN TEST (CRUD) ---

    @Test
    fun `getAllUsers should allow Admin`() {
        val page = PageImpl(listOf(createMockUserDTO()))

        // Use a concrete object matching the controller default (@PageableDefault(size = 20))
        // This avoids using any(), which caused NullPointerException with Kotlin
        val pageable = PageRequest.of(0, 20)
        `when`(userService.getAllUsers(pageable)).thenReturn(page)

        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_admin"))

        mockMvc.perform(get("/api/v1/users").with(jwtToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].email").value(userEmail))
    }

    @Test
    fun `getAllUsers should forbid Staff`() {
        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_staff"))

        mockMvc.perform(get("/api/v1/users").with(jwtToken))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `createUser should allow Admin`() {
        val requestDTO = UserRequestDTO(
            id = UUID.randomUUID(),
            fullName = "New User",
            email = "new@test.com",
            organization = "Polito",
            role = Role.STAFF
        )

        // Here we use the concrete requestDTO object.
        // Since it is a data class, equals will work correctly when the controller reconstructs the object from JSON.
        `when`(userService.createUser(requestDTO)).thenReturn(
            createMockUserDTO().copy(email = "new@test.com")
        )

        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_admin"))

        mockMvc.perform(post("/api/v1/users")
            .with(jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isCreated)
    }

    // --- MACHINE-TO-MACHINE TEST (Service Account) ---

    @Test
    fun `getUserById should allow Service Account`() {
        val mockUser = createMockUserDTO()
        `when`(userService.getUserById(userId)).thenReturn(mockUser)

        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_service-account"))

        mockMvc.perform(get("/api/v1/users/$userId").with(jwtToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId.toString()))
    }

    @Test
    fun `getUserIdsByDepartment should allow Service Account`() {
        val department = "IT"
        `when`(userService.getUserIdsByDepartment(department)).thenReturn(listOf(userId))

        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_service-account"))

        mockMvc.perform(get("/api/v1/users/ids")
            .param("department", department)
            .with(jwtToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value(userId.toString()))
    }

    @Test
    fun `getUserIdsByDepartment should forbid Normal User`() {
        val jwtToken = jwt().authorities(SimpleGrantedAuthority("ROLE_staff"))

        mockMvc.perform(get("/api/v1/users/ids")
            .param("department", "IT")
            .with(jwtToken))
            .andExpect(status().isForbidden)
    }
}