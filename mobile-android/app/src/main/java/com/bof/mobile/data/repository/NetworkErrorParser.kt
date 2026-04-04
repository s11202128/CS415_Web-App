package com.bof.mobile.data.repository

import org.json.JSONObject
import retrofit2.HttpException

internal fun parseHttpError(exception: HttpException, fallback: String): String {
    return try {
        val errorBody = exception.response()?.errorBody()?.string().orEmpty()
        if (errorBody.isBlank()) {
            fallback
        } else {
            val json = JSONObject(errorBody)
            val message = json.optString("message").ifBlank { json.optString("error") }
            if (message.isBlank()) fallback else message
        }
    } catch (_: Exception) {
        fallback
    }
}
