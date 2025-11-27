package com.example.costmanager.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("CostManagerPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    fun saveApiKey(apiKey: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_GEMINI_API_KEY, apiKey)
            apply()
        }
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }
}
