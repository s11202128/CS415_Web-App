package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.LoginRequest
import com.bof.mobile.model.LoginResponse
import com.bof.mobile.model.RegisterRequest
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val apiService: ApiService) {

    private fun extractErrorMessage(e: HttpException): String {
        val raw = e.response()?.errorBody()?.string()?.trim()
        if (!raw.isNullOrBlank()) {
            val marker = "\"error\":\""
            val start = raw.indexOf(marker)
            if (start >= 0) {
                val messageStart = start + marker.length
                val end = raw.indexOf('"', messageStart)
                if (end > messageStart) {
                    return raw.substring(messageStart, end)
                }
            }
            return raw
        }
        return e.message() ?: "Request failed"
    }

    suspend fun login(identifier: String, password: String): ApiResult<LoginResponse> {
        return try {
            val result = apiService.login(LoginRequest(email = identifier, password = password))
            ApiResult.Success(result)
        } catch (e: HttpException) {
            ApiResult.Error(message = extractErrorMessage(e), code = e.code())
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
        accountType: String,
        password: String,
        confirmPassword: String
    ): ApiResult<String> {
        return try {
            val response = apiService.register(
                RegisterRequest(
                    fullName = fullName,
                    mobile = mobile,
                    email = email,
                    accountType = accountType,
                    password = password,
                    confirmPassword = confirmPassword
                )
            )
            ApiResult.Success(response.message)
        } catch (e: HttpException) {
            ApiResult.Error(message = extractErrorMessage(e), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Cannot connect to server. Check network and backend status.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
