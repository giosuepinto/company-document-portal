package it.polito.wa2_2026.g15.fileservice.services

import io.minio.*
import it.polito.wa2_2026.g15.fileservice.exceptions.StorageException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class MinioStorageService(
    @Value("\${minio.url}") url: String,
    @Value("\${minio.access-key}") accessKey: String,
    @Value("\${minio.secret-key}") secretKey: String
) : StorageService {

    private val logger = LoggerFactory.getLogger(MinioStorageService::class.java)

    private val client: MinioClient = MinioClient.builder()
        .endpoint(url)
        .credentials(accessKey, secretKey)
        .build()

    private fun ensureBucketExists(bucket: String) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                logger.info("Bucket '{}' created.", bucket)
            }
        } catch (e: Exception) {
            logger.error("Failed to check/create bucket {}", bucket, e)
            throw StorageException("Storage initialization failed")
        }
    }

    override fun putObject(bucket: String, objectName: String, stream: InputStream, size: Long, contentType: String) {
        try {
            ensureBucketExists(bucket)
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build()
            )
            logger.debug("Uploaded object: {}", objectName)
        } catch (ex: Exception) {
            logger.error("MinIO Put Error: {}", objectName, ex)
            throw StorageException("Failed to upload file")
        }
    }

    override fun getObject(bucket: String, objectName: String): InputStream {
        return try {
            client.getObject(GetObjectArgs.builder().bucket(bucket).`object`(objectName).build())
        } catch (ex: Exception) {
            logger.error("MinIO Get Error: {}", objectName, ex)
            throw StorageException("Failed to retrieve file")
        }
    }

    override fun deleteObject(bucket: String, objectName: String) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(objectName).build())
            logger.debug("Deleted object: {}", objectName)
        } catch (ex: Exception) {
            logger.warn("MinIO Delete Warning: {}", objectName, ex)
        }
    }

    override fun composeObject(bucket: String, targetObject: String, sourceObjects: List<String>): String {
        try {
            ensureBucketExists(bucket)
            val sources = sourceObjects.map { ComposeSource.builder().bucket(bucket).`object`(it).build() }

            val response = client.composeObject(
                ComposeObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(targetObject)
                    .sources(sources)
                    .build()
            )
            return response.etag()
        } catch (ex: Exception) {
            logger.error("MinIO Compose Error: {}", targetObject, ex)
            throw StorageException("Failed to merge chunks")
        }
    }

    override fun moveObject(bucket: String, source: String, target: String) {
        try {
            ensureBucketExists(bucket)

            // 1. Copy
            client.copyObject(
                CopyObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(target)
                    .source(CopySource.builder().bucket(bucket).`object`(source).build())
                    .build()
            )

            // 2. Delete original
            deleteObject(bucket, source)
            logger.info("Moved object {} -> {}", source, target)
        } catch (ex: Exception) {
            logger.error("MinIO Move Error: {} -> {}", source, target, ex)
            throw StorageException("Failed to move file in storage")
        }
    }
}