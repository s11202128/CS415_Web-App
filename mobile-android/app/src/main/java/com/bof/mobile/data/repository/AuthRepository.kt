package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.LoginRequest
import com.bof.mobile.model.LoginResponse
import com.bof.mobile.model.RegisterRequest
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val apiService: ApiService) {

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return try {
            val result = apiService.login(LoginRequest(email = email, password = password))
            ApiResult.Success(result)
        } catch (e: HttpException) {
            ApiResult.Error(message = "Login failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Cannot connect to server. Check network and backend status.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun register(
        fullName: String,
        mobile: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ApiResult<String> {
        return try {
            val response = apiService.register(
                RegisterRequest(
                    fullName = fullName,
                    mobile = mobile,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword
                )
            )
            ApiResult.Success(response.message)
        } catch (e: HttpException) {
            ApiResult.Error(message = "Registration failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Cannot connect to server. Check network and backend status.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
