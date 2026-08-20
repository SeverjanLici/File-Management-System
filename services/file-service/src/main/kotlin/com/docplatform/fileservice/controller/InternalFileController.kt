package com.docplatform.fileservice.controller

import com.docplatform.fileservice.service.FileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/internal/files")
class InternalFileController(
    private val fileService: FileService
) {

    @DeleteMapping("/{id}")
    fun deleteFile(@PathVariable id: UUID): ResponseEntity<Void> {
        fileService.deleteFile(id)
        return ResponseEntity.noContent().build()
    }
}