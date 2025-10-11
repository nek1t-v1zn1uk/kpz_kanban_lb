package org.example.project.data.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserDto (
    val id: Long? = null,
    var username: String? = null,
    var email: String,
    var passwordHash: String,
)