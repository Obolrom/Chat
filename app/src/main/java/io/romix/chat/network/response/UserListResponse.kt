package io.romix.chat.network.response

data class UserListResponse(
    val users: List<UserResponse>,
) {
    data class UserResponse(
        val id: Long,
        val username: String,
        val photoUrl: String?,
    )
}
