package com.example.services

import android.content.Context
import com.example.data.config.ApiConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Diagnostic Service Engine — dynamic backend proxy for custom endpoint testing.
 */
class AgyPrimeBridge(private val context: Context? = null) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun executeSovereignPrompt(
        prompt: String,
        targetNode: String = "prime"
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Dynamically resolved from runtime settings or local environment (never hardcoded)
                val config = context?.let { ApiConfigManager.getInstance(it).getSettings() }
                val targetBase = config?.baseUrl ?: "http://127.0.0.1:20128"
                val url = "$targetBase/v1/chat/completions"
                val model = if (targetNode == "spark") "spark-x2.5-4b" else "free-many"

                val jsonPayload = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.4)
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val builder = Request.Builder().url(url).post(body)
                
                val currentApiKey = when (config?.provider) {
                    "openai" -> config.openaiApiKey
                    "gemini" -> config.geminiApiKey
                    else -> ""
                }

                if (currentApiKey.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer $currentApiKey")
                }

                val response = client.newCall(builder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val root = JSONObject(responseBody)
                    val choices = root.optJSONArray("choices")
                    val message = choices?.optJSONObject(0)?.optJSONObject("message")
                    val content = message?.optString("content") ?: ""
                    val reasoning = message?.optString("reasoning_content") ?: ""

                    val output = if (reasoning.isNotBlank()) {
                        "🧠 $reasoning\n\n$content"
                    } else {
                        content
                    }
                    Result.success(output.ifEmpty { "Diagnostic OK (empty return)" })
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
