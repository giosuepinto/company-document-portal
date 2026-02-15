package it.polito.wa2_2026.g15.fileservice

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import it.polito.wa2_2026.g15.fileservice.services.UploadService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("/uploads")
@Tag(name = "TUS Protocol", description = "Endpoints implementing the TUS Resumable Upload Protocol")
class TusController(
    private val uploadService: UploadService
) {
    private val logger = LoggerFactory.getLogger(TusController::class.java)

    // 1 GB limit (must match the OPTIONS configuration)
    private val TUS_MAX_SIZE = 1024L * 1024L * 1024L

    /**
     * STEP 1 (OPTIONS)
     */
    @Operation(summary = "TUS Options", description = "Returns the TUS server capabilities and configuration.")
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204", description = "Configuration returned successfully", headers = [
                Header(name = "Tus-Resumable", description = "Protocol version"),
                Header(name = "Tus-Version", description = "Supported versions"),
                Header(name = "Tus-Extension", description = "Supported extensions"),
                Header(name = "Tus-Max-Size", description = "Maximum allowed file size in bytes")
            ]
        )
    ])
    @RequestMapping(method = [RequestMethod.OPTIONS])
    fun options(): ResponseEntity<Void> {
        logger.info("Received TUS OPTIONS request.")

        val headers = HttpHeaders()
        headers.add("Tus-Resumable", "1.0.0")
        headers.add("Tus-Version", "1.0.0")
        headers.add("Tus-Extension", "creation, termination, checksum")
        headers.add("Tus-Checksum-Algorithm", "sha256")
        headers.add("Tus-Max-Size", (TUS_MAX_SIZE).toString())

        return ResponseEntity(headers, HttpStatus.NO_CONTENT)
    }

    /**
     * STEP 2 (POST)
     */
    @Operation(summary = "Create Upload Session", description = "Initializes a new upload session for a file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Upload session created", headers = [
            Header(name = "Location", description = "The URL of the created upload resource"),
            Header(name = "Tus-Resumable", description = "Protocol version")
        ]),
        ApiResponse(responseCode = "400", description = "Invalid headers, invalid metadata or size is 0"),
        ApiResponse(responseCode = "412", description = "Precondition failed (Invalid Tus-Resumable version)"),
        ApiResponse(responseCode = "413", description = "Payload Too Large (Upload-Length exceeds server limit)")
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["", "/"])
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun createUpload(
        @Parameter(name = "Tus-Resumable", description = "Protocol version (must be 1.0.0)", required = true, `in` = ParameterIn.HEADER, example = "1.0.0")
        @RequestHeader("Tus-Resumable") tusResumable: String,

        @Parameter(name = "Upload-Length", description = "Total size of the file in bytes", required = true, `in` = ParameterIn.HEADER, example = "1024")
        @RequestHeader("Upload-Length") uploadLength: Long,

        @Parameter(name = "Upload-Metadata", description = "Key-value pairs encoded in Base64", required = false, `in` = ParameterIn.HEADER)
        @RequestHeader("Upload-Metadata", required = false) metadata: String?,

        @Parameter(name = "X-User-Id", description = "Dev: Temporary Owner injection", required = false, `in` = ParameterIn.HEADER)
        @RequestHeader("X-User-Id", required = false) userIdHeader: String?
    ): ResponseEntity<Void> {

        logger.info("Received TUS POST request. Length: {}, Metadata: {}", uploadLength, metadata)

        // 1. Validate headers
        uploadService.verifyTusPostHeader(tusResumable, uploadLength, TUS_MAX_SIZE)

        // 2. Handle Ownership
        val ownerId = if (userIdHeader != null) {
            try {
                UUID.fromString(userIdHeader)
            } catch (_: IllegalArgumentException) {
                logger.warn("Invalid X-User-Id provided: {}. Generating random owner.", userIdHeader)
                UUID.randomUUID()
            }
        } else {
            UUID.randomUUID()
        }

        // 3. Create session
        val session = uploadService.createUpload(uploadLength, metadata, ownerId)
        logger.info("Upload session created with ID: {}", session.id)

        // 4. Build Response

        //val locationUrl = ServletUriComponentsBuilder
        //    .fromCurrentRequest()
        //    .path("/{id}")
        //    .buildAndExpand(session.id)
        //    .toUriString()

        val locationUrl = "http://localhost:8080/uploads/${session.id}"

        val headers = HttpHeaders().apply {
            add("Location", locationUrl)
            add("Tus-Resumable", "1.0.0")
        }

        return ResponseEntity(headers, HttpStatus.CREATED)
    }

    /**
     * STEP 3 (HEAD)
     */
    @Operation(summary = "Get Upload Status", description = "Returns the current offset and size of the upload.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Status retrieved", headers = [
            Header(name = "Upload-Offset"), Header(name = "Upload-Length"),
            Header(name = "Tus-Resumable"), Header(name = "Cache-Control")
        ]),
        ApiResponse(responseCode = "404", description = "Upload session not found"),
        ApiResponse(responseCode = "410", description = "Upload Gone (Resource was explicitly deleted or expired)"),
        ApiResponse(responseCode = "412", description = "Precondition Failed (Tus-Resumable mismatch)")
    ])
    @RequestMapping(method = [RequestMethod.HEAD], value = ["/{id}", "/{id}/"])
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun getUploadStatus(
        @Parameter(name = "Tus-Resumable", required = true, `in` = ParameterIn.HEADER)
        @RequestHeader("Tus-Resumable") tusResumable: String,

        @Parameter(description = "UUID of the upload session", required = true)
        @PathVariable id: String
    ): ResponseEntity<Void> {
        logger.info("Received TUS HEAD request for upload ID: {}", id)

        uploadService.verifyTusHeader(tusResumable)

        val uploadId = try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            // Standard Spring handling for bad UUID usually returns 400, handled by framework or generic exception handler
            throw IllegalArgumentException("Invalid UUID format")
        }

        val uploadInfo = uploadService.getUploadStatus(uploadId)

        return ResponseEntity.ok()
            .header("Tus-Resumable", tusResumable)
            .header("Upload-Offset", uploadInfo.totalOffset.toString())
            .header("Upload-Length", uploadInfo.size.toString())
            .header("Cache-Control", "no-store")
            .build()
    }

    /**
     * STEP 4 (PATCH)
     */
    @Operation(summary = "Upload Chunk", description = "Uploads a chunk of the file. The body must be the binary content.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Binary file content chunk",
        content = [Content(mediaType = "application/offset+octet-stream")]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Chunk accepted", headers = [
            Header(name = "Upload-Offset", description = "New total offset"),
            Header(name = "Tus-Resumable", description = "Protocol version")
        ]),
        ApiResponse(responseCode = "400", description = "Bad Request (Checksum mismatch, already completed)"),
        ApiResponse(responseCode = "404", description = "Upload session not found"),
        ApiResponse(responseCode = "409", description = "Conflict (Upload-Offset does not match server offset)"),
        ApiResponse(responseCode = "410", description = "Gone (Upload was deleted)"),
        ApiResponse(responseCode = "412", description = "Precondition Failed (Tus-Resumable mismatch)"),
        ApiResponse(responseCode = "415", description = "Unsupported Media Type (Content-Type must be application/offset+octet-stream)")
    ])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/{id}", "/{id}/"])
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun uploadChunk(
        @Parameter(description = "UUID of the upload session", required = true)
        @PathVariable id: String,
        @Parameter(name = "Tus-Resumable", required = true, `in` = ParameterIn.HEADER)
        @RequestHeader("Tus-Resumable") tusResumable: String,
        @Parameter(name = "Upload-Offset", description = "Current offset at which the chunk starts", required = true, `in` = ParameterIn.HEADER)
        @RequestHeader("Upload-Offset") uploadOffset: Long,
        @Parameter(name = "Content-Type", description = "Must be application/offset+octet-stream", required = true, `in` = ParameterIn.HEADER)
        @RequestHeader("Content-Type") contentType: String,
        @Parameter(name = "Upload-Checksum", description = "Optional checksum (algorithm base64-hash)", required = false, `in` = ParameterIn.HEADER)
        @RequestHeader("Upload-Checksum", required = false) uploadChecksum: String?,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        logger.info("Received TUS PATCH request for ID: {}, Offset: {}", id, uploadOffset)

        uploadService.verifyTusPatchHeader(tusResumable, contentType)

        val uploadId = UUID.fromString(id)
        val updatedSession = uploadService.writeChunk(uploadId, uploadOffset, request.inputStream, uploadChecksum)

        logger.info("Chunk processed for {}. New Offset: {}", id, updatedSession.totalOffset)

        val headers = HttpHeaders().apply {
            add("Tus-Resumable", "1.0.0")
            add("Upload-Offset", updatedSession.totalOffset.toString())
        }
        return ResponseEntity(headers, HttpStatus.NO_CONTENT)
    }

    /**
     * STEP 5 (DELETE)
     */
    @Operation(summary = "Terminate Upload", description = "Terminates an upload session and deletes partial data.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Upload terminated successfully"),
        ApiResponse(responseCode = "404", description = "Upload not found"),
        ApiResponse(responseCode = "410", description = "Gone (Upload already deleted)"),
        ApiResponse(responseCode = "412", description = "Precondition Failed")
    ])
    @RequestMapping(method = [RequestMethod.DELETE], value = ["/{id}", "/{id}/"])
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun deleteUpload(
        @Parameter(name = "Tus-Resumable", required = true, `in` = ParameterIn.HEADER)
        @RequestHeader("Tus-Resumable") tusResumable: String,
        @Parameter(description = "UUID of the upload session", required = true)
        @PathVariable id: String
    ): ResponseEntity<Void> {
        logger.info("Received TUS DELETE request for ID: {}", id)

        uploadService.verifyTusHeader(tusResumable)

        val uploadId = UUID.fromString(id)
        uploadService.terminateUpload(uploadId)

        logger.info("Upload {} terminated successfully.", id)

        return ResponseEntity.noContent().build()
    }
}