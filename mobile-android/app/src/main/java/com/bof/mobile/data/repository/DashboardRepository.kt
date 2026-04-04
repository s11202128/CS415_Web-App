package com.bof.mobile.data.repository

import android.util.Log
import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.DashboardResponse
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

class DashboardRepository(private val apiService: ApiService) {
    private val tag = "DashboardRepository"

    suspend fun getDashboard(customerId: Int?): ApiResult<DashboardResponse> {
        val startedAt = System.currentTimeMillis()
        Log.d(tag, "getDashboard start customerId=$customerId")
        return try {
            val response = withTimeout(15_000L) {
                apiService.getDashboard(customerId)
            }
            Log.d(tag, "getDashboard success in ${System.currentTimeMillis() - startedAt}ms accounts=${response.accounts.size}")
            ApiResult.Success(response)
        } catch (e: HttpException) {
            Log.e(tag, "getDashboard http error code=${e.code()} after ${System.currentTimeMillis() - startedAt}ms", e)
            ApiResult.Error(message = parseHttpError(e, "Failed to load dashboard"), code = e.code())
        } catch (e: TimeoutCancellationException) {
            Log.e(tag, "getDashboard timed out after ${System.currentTimeMillis() - startedAt}ms", e)
            ApiResult.Error(message = "Dashboard request timed out. Please try again.")
        } catch (e: IOException) {
            Log.e(tag, "getDashboard network error after ${System.currentTimeMillis() - startedAt}ms", e)
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            Log.e(tag, "getDashboard unexpected error after ${System.currentTimeMillis() - startedAt}ms", e)
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
