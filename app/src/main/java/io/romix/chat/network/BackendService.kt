package io.romix.chat.network

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BackendService {

    @POST("api/v1/auth/login")
    suspend fun authorize(@Body authRequest: AuthRequest): NetworkResponse<AuthResponse, *>
}