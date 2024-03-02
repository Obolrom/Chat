package io.romix.chat.network

data class AuthResponse(
    val userId: Long?,
    val token: String?,
    val issuedAt: String?,
    val expiredAt: String?,
)
