package com.example.data.config

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import org.json.JSONObject

data class ApiSettings(
    val geminiApiKey: String,
    val openaiApiKey: String,
    val baseUrl: String,
    val modelName: String,
    val provider: String // "gemini" or "openai"
)

class ApiConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wander_ai_api_settings", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: ApiConfigManager? = null

        fun getInstance(context: Context): ApiConfigManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ApiConfigManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model_name"
        private const val KEY_PROVIDER = "provider"

        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
    }

    fun getSettings(): ApiSettings {
        val defaultGemini = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        val savedGemini = prefs.getString(KEY_GEMINI, "") ?: ""
        val effectiveGemini = if (savedGemini.isNotBlank()) savedGemini else defaultGemini

        return ApiSettings(
            geminiApiKey = effectiveGemini,
            openaiApiKey = prefs.getString(KEY_OPENAI, "") ?: "",
            baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            modelName = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            provider = prefs.getString(KEY_PROVIDER, "gemini") ?: "gemini"
        )
    }

    fun saveSettings(settings: ApiSettings) {
        prefs.edit()
            .putString(KEY_GEMINI, settings.geminiApiKey)
            .putString(KEY_OPENAI, settings.openaiApiKey)
            .putString(KEY_BASE_URL, settings.baseUrl)
            .putString(KEY_MODEL, settings.modelName)
            .putString(KEY_PROVIDER, settings.provider)
            .apply()
    }

    fun exportSettingsJson(): String {
        val s = getSettings()
        val json = JSONObject()
        json.put("gemini_api_key", s.geminiApiKey)
        json.put("openai_api_key", s.openaiApiKey)
        json.put("base_url", s.baseUrl)
        json.put("model", s.modelName)
        json.put("provider", s.provider)
        return json.toString(2)
    }

    fun exportShellCommands(): String {
        val s = getSettings()
        return buildString {
            appendLine("# WanderAI Shell Export Config")
            appendLine("export GEMINI_API_KEY=\"${s.geminiApiKey}\"")
            appendLine("export OPENAI_API_KEY=\"${s.openaiApiKey}\"")
            appendLine("export OPENAI_BASE_URL=\"${s.baseUrl}\"")
            appendLine("export MODEL=\"${s.modelName}\"")
            appendLine("export PROVIDER=\"${s.provider}\"")
        }
    }

    fun importSettingsJson(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            val current = getSettings()
            val gemini = json.optString("gemini_api_key", current.geminiApiKey)
            val openai = json.optString("openai_api_key", current.openaiApiKey)
            val base = json.optString("base_url", current.baseUrl)
            val model = json.optString("model", current.modelName)
            val prov = json.optString("provider", current.provider)
            saveSettings(ApiSettings(gemini, openai, base, model, prov))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resetToDefaults() {
        val defaultGemini = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        saveSettings(
            ApiSettings(
                geminiApiKey = defaultGemini,
                openaiApiKey = "",
                baseUrl = DEFAULT_BASE_URL,
                modelName = DEFAULT_MODEL,
                provider = "gemini"
            )
        )
    }
}
