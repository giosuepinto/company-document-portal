package it.polito.wa2_2026.g15.fileservice.services

import java.io.InputStream

interface StorageService {

    fun putObject(bucket: String, objectName: String, stream: InputStream, size: Long, contentType: String)
    fun getObject(bucket: String, objectName: String): InputStream
    fun deleteObject(bucket: String, objectName: String)
    fun composeObject(bucket: String, targetObject: String, sourceObjects: List<String>): String
    fun moveObject(bucket: String, source: String, target: String)


}