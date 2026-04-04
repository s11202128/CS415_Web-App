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
            val message = json.optString("message")
            val directError = json.optString("error")
            val nestedErrorMessage = json.optJSONObject("error")?.optString("message").orEmpty()
            val firstValidationMessage = json
                .optJSONArray("errors")
                ?.optJSONObject(0)
                ?.optString("message")
                .orEmpty()

            val resolved = listOf(message, directError, nestedErrorMessage, firstValidationMessage)
                .firstOrNull { it.isNotBlank() }
                ?: fallback

            resolved
        }
    } catch (_: Exception) {
        fallback
    }
}
