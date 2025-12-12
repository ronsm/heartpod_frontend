package org.hwu.care.healthub.nlu

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hwu.care.healthub.core.State
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * LangGraph Agent API Interface
 */
interface LangGraphApi {
    @POST("/agent/interpret")
    suspend fun interpret(@Body request: InterpretRequest): InterpretResponse
    
    @GET("/health")
    suspend fun health(): HealthResponse
}

/**
 * Request model for LLM interpretation
 */
data class InterpretRequest(
    val user_text: String,
    val current_state: String,
    val session_id: String,
    val context: Map<String, Any> = emptyMap()
)

/**
 * Response model from LLM agent
 */
data class InterpretResponse(
    val intent: String,
    val next_state: String?,
    val speech: String,
    val action: String?,
    val confidence: Float
)

/**
 * Health check response
 */
data class HealthResponse(
    val status: String
)

/**
 * LangGraph Client - Communicates with cloud LLM agent
 */
class LangGraphClient(baseUrl: String = "https://your-agent.onrender.com") {
    
    companion object {
        private const val TAG = "LangGraphClient"
    }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(LangGraphApi::class.java)
    
    /**
     * Interpret user input using LLM
     * @param text User's speech or text
     * @param currentState Current FSM state
     * @param sessionId Unique session identifier
     * @return LLM interpretation result
     */
    suspend fun interpret(
        text: String,
        currentState: State,
        sessionId: String,
        context: Map<String, Any> = emptyMap()
    ): InterpretResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val request = InterpretRequest(
                    user_text = text,
                    current_state = currentState.javaClass.simpleName,
                    session_id = sessionId,
                    context = context
                )
                
                Log.d(TAG, "Sending to LLM: $request")
                
                val response = api.interpret(request)
                
                Log.d(TAG, "LLM response: $response")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "Failed to interpret with LLM", e)
                null
            }
        }
    }
    
    /**
     * Check if LLM agent is available
     */
    suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.health()
                response.status == "healthy"
            } catch (e: Exception) {
                Log.w(TAG, "LLM agent not available", e)
                false
            }
        }
    }
}
