package com.bof.mobile.data.remote

import android.util.Log
import com.bof.mobile.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val BASE_URL: String = BuildConfig.API_BASE_URL
    private const val TAG = "NetworkModule"

    fun createApiService(tokenProvider: () -> String?): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider()
            val request = chain.request()
            val requestBuilder = request.newBuilder()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            val requestWithAuth = requestBuilder.build()
            val start = System.currentTimeMillis()
            try {
                val response = chain.proceed(requestWithAuth)
                if (requestWithAuth.url.encodedPath.contains("dashboard")) {
                    Log.d(TAG, "${requestWithAuth.method} ${requestWithAuth.url} -> ${response.code} in ${System.currentTimeMillis() - start}ms")
                }
                response
            } catch (error: Exception) {
                Log.e(TAG, "${requestWithAuth.method} ${requestWithAuth.url} failed in ${System.currentTimeMillis() - start}ms", error)
                throw error
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
