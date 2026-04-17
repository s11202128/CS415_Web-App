package com.bof.mobile.data.local

import android.content.Context

class LoginSuggestionsStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSuggestions(): List<String> {
        val raw = preferences.getString(KEY_IDENTIFIERS, "") ?: ""
        return raw
            .split(DELIMITER)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun saveIdentifier(identifier: String): List<String> {
        val normalized = identifier.trim()
        if (normalized.isBlank()) return getSuggestions()

        val merged = (listOf(normalized) + getSuggestions())
            .distinct()
            .take(MAX_SUGGESTIONS)

        preferences.edit().putString(KEY_IDENTIFIERS, merged.joinToString(DELIMITER)).apply()
        return merged
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_IDENTIFIERS = "saved_login_identifiers"
        private const val DELIMITER = "|"
        private const val MAX_SUGGESTIONS = 12
    }
}