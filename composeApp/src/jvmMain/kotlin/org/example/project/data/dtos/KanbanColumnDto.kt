package org.example.project.data.dtos

import kotlinx.serialization.Serializable
import org.example.project.utils.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class KanbanColumnDto (
    val id: Long? = null,
    var title: String,
    var position: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val boardId: Long
)