package io.romix.chat.network

import com.haroldadmin.cnradapter.NetworkResponse
import io.romix.chat.network.response.ChatPageResponse
import io.romix.chat.network.response.PageMessageResponse
import io.romix.chat.network.response.UserListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendService {

    @POST("api/v1/auth/login")
    suspend fun authorize(@Body authRequest: AuthRequest): NetworkResponse<AuthResponse, *>

    @GET("api/v1/messages/private/users/{collocutorId}")
    suspend fun getMessages(
        @Header("Authorization") bearerToken: String,
        @Path("collocutorId") collocutorId: Long,
        @Query("pageSize") pageSize: Int,
    ): NetworkResponse<PageMessageResponse, *>

    @GET("api/v1/messages/rooms/{roomId}")
    suspend fun getRoomMessages(
        @Header("Authorization") bearerToken: String,
        @Path("roomId") roomId: Long,
        @Query("pageSize") pageSize: Int,
    ): NetworkResponse<PageMessageResponse, *>

    @GET("api/v1/users")
    suspend fun getUsers(
        @Header("Authorization") bearerToken: String,
    ): NetworkResponse<UserListResponse, *>

    @GET("api/v1/chats")
    suspend fun getChats(
        @Header("Authorization") bearerToken: String,
    ): NetworkResponse<ChatPageResponse, *>
}