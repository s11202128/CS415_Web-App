package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.DashboardResponse
import com.bof.mobile.model.ApiResult
import retrofit2.HttpException
import java.io.IOException

class DashboardRepository(private val apiService: ApiService) {
    suspend fun getDashboard(customerId: Int): ApiResult<DashboardResponse> {
        return try {
            ApiResult.Success(apiService.getDashboard(customerId))
        } catch (e: HttpException) {
            ApiResult.Error(message = "Failed to load dashboard: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
