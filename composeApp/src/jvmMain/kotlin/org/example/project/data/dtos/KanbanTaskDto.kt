package org.example.project.data.dtos

import kotlinx.serialization.Serializable
import org.example.project.utils.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class KanbanTaskDto (
    val id: Long? = null,
    var title: String,
    var description: String? = null,
    var position: Int,
    val priority: KanbanTaskPriorities = KanbanTaskPriorities.MEDIUM,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dueDate: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val columnId: Long
)

enum class KanbanTaskPriorities {
    HIGH,
    MEDIUM,
    LOW,
    OPTIONAL
}