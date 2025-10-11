package org.example.project.data.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ProjectMemberDto (
    val id: Long? = null,
    val role: ProjectMemberRole = ProjectMemberRole.MEMBER,
    val projectId: Long,
    val userId: Long,
)
enum class ProjectMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}