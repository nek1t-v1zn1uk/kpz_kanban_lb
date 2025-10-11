package org.example.project.data.dtos

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.project.utils.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class ProjectDto (
    val id: Long? = null,
    var title: String,
    var description: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val ownerId: Long
)