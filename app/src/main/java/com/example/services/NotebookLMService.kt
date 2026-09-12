package com.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// Models for MCP RAG / NotebookLM Proxy
@Serializable
data class RagQueryRequest(
    val query: String,
    val contextId: String = "default-brain"
)

@Serializable
data class RagQueryResponse(
    val answer: String,
    val citations: List<String> = emptyList()
)

interface NotebookLMApi {
    // This connects to your Pop!_OS MCP server endpoint (NotebookLLM / Gemma proxy)
    @POST("v1/mcp/rag/query")
    suspend fun queryBrain(@Body request: RagQueryRequest): RagQueryResponse
}

class NotebookLMService(private val baseUrl: String = "http://100.91.238.121:8080/") {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        
    private val api = retrofit.create(NotebookLMApi::class.java)

    /**
     * Connects to the RAG backend to retrieve travel assistant / diagnostic data
     */
    suspend fun askBrain(prompt: String): Result<RagQueryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.queryBrain(RagQueryRequest(query = prompt))
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
