package org.hwu.care.healthub

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// Data classes (SseEvent, ItemStatePayload) remain the same
// (Assuming they are in this file or imported)
data class SseEvent(
    val topic: String?,
    val payload: String?,
    val type: String?
)

data class ItemStatePayload(
    val type: String,
    val value: String,
    val oldType: String? = null,
    val oldValue: String? = null
)

object SseRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    // Public flow for subscribers
    val itemStateChanges = MutableSharedFlow<Pair<String, String>>(
        replay = 1 // Replay the last emitted item for new subscribers
    )

    private var httpClient: OkHttpClient? = null
    private var eventSource: EventSource? = null

    private var currentAuthToken: String? = null
    private var currentIpAddress: String? = null
    private var isListening = false

    // Counter for active listeners (optional, for more advanced management)
    private val activeListenerCount = AtomicInteger(0)


    private fun getClient(): OkHttpClient {
        if (httpClient == null) {
            httpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // Adjusted timeout
                .readTimeout(0, TimeUnit.MINUTES)    // For SSE, read timeout is often set to 0 (no timeout)
                .writeTimeout(60, TimeUnit.SECONDS)  // Adjusted timeout
                // Add logging interceptor if needed for debugging the SSE client itself
                // .addInterceptor(HttpLoggingInterceptor { message -> Log.d("SseClient", message) }.apply { level = HttpLoggingInterceptor.Level.HEADERS })
                .build()
        }
        return httpClient!!
    }

    @Synchronized
    fun startListening(authToken: String, ipAddress: String) {
        activeListenerCount.incrementAndGet()
        Log.d("SseRepository", "startListening called. Active listeners: ${activeListenerCount.get()}")

        if (isListening && currentAuthToken == authToken && currentIpAddress == ipAddress) {
            Log.d("SseRepository", "Already listening with the same active connection and parameters.")
            return
        }

        // If parameters changed or not currently listening effectively, stop existing and restart
        if (isListening) {
            Log.d("SseRepository", "Parameters changed or need to restart. Stopping existing connection.")
            internalStopListening() // Stop without decrementing listener count yet
        }

        currentAuthToken = authToken
        currentIpAddress = ipAddress

        if (authToken.isBlank() || ipAddress.isBlank()) {
            Log.e("SseRepository", "Auth token or IP address is blank. Cannot start listening.")
            // Optionally emit an error state or notify UI
            activeListenerCount.decrementAndGet() // Decrement if we can't even attempt to start
            return
        }

        val sseUrl = "http://$ipAddress:8080/rest/events"
        Log.i("SseRepository", "Attempting to connect to SSE URL: $sseUrl")

        val request = Request.Builder()
            .url(sseUrl)
            .header("Authorization", "Bearer $authToken")
            .header("Accept", "text/event-stream")
            .header("Connection", "Keep-Alive")
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                isListening = true
                Log.i("SseRepository", "SSE connection OPENED successfully to $sseUrl")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                // Log.d("SseRepository", "SSE event received: type=$type, data=$data")
                try {
                    val sseEvent = gson.fromJson(data, SseEvent::class.java)
                    Log.d("SseRepository", "Parsed SSE Event: topic='${sseEvent.topic}', type='${sseEvent.type}', payload='${sseEvent.payload}'")

                    // ▼▼▼ CORRECTED CONDITION ▼▼▼
                    if (sseEvent.topic != null &&
                        (sseEvent.topic.endsWith("/state") || sseEvent.topic.endsWith("/statechanged") || sseEvent.topic.endsWith("/stateupdated"))
                    ) {
                        // sseEvent.topic is now smart-cast to non-null String within this block
                        val topicParts = sseEvent.topic.split('/')
                        if (topicParts.size >= 3 && topicParts[0] == "openhab" && topicParts[1] == "items") {
                            val itemName = topicParts[2]
                            if (sseEvent.payload != null) { // Good practice to check payload too
                                val payloadData = gson.fromJson(sseEvent.payload, ItemStatePayload::class.java)
                                val newState = payloadData.value

                                repositoryScope.launch {
                                    if (itemStateChanges.tryEmit(itemName to newState)) {
                                        Log.v("SseRepository", "Item: $itemName, New State: $newState -- Emitted successfully. (SSE Event Type: ${sseEvent.type})")
                                    } else {
                                        Log.w("SseRepository", "Item: $itemName, New State: $newState -- Emit FAILED (no collectors or buffer full). (SSE Event Type: ${sseEvent.type})")
                                    }
                                }
                            } else {
                                Log.w("SseRepository", "SSE event payload was null for topic: ${sseEvent.topic}")
                            }
                        } else {
                            Log.w("SseRepository", "Received state-like event with unexpected topic format: ${sseEvent.topic}")
                        }
                    } else {
                        if (sseEvent.topic == null) {
                            Log.d("SseRepository", "Ignoring SSE event with NULL topic. Type: ${sseEvent.type}, Data: $data")
                        } else {
                            // This means the topic was not null, but didn't end with any of the specified suffixes
                            Log.v("SseRepository", "Ignoring event with topic: '${sseEvent.topic}' (did not match state suffixes). Type: ${sseEvent.type}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SseRepository", "Failed to parse SSE event or emit update", e)
                }
// ...

            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                isListening = false
                Log.e("SseRepository", "SSE connection FAILED: ${response?.message}", t)
                // Consider some retry logic or notifying clients of the failure
                // If it's an auth error (401), new parameters won't help without new token
                if (response?.code == 401) {
                    Log.e("SseRepository", "SSE Authentication failed. Please check API token.")
                    // Potentially clear currentAuthToken to force re-auth attempt if token changes
                }
                // Attempt to reconnect after a delay, unless it's a permanent error
                // For simplicity, this example doesn't include complex retry logic
                // but you might want it in a production app.
            }

            override fun onClosed(eventSource: EventSource) {
                isListening = false
                Log.i("SseRepository", "SSE connection CLOSED.")
                // This might be called if the server closes the connection or if we call cancel()
            }
        }

        this.eventSource = EventSources.createFactory(getClient()).newEventSource(request, eventSourceListener)
        Log.d("SseRepository", "SSE newEventSource created. Waiting for connection to open...")
    }


    @Synchronized
    private fun internalStopListening() {
        if (eventSource != null) {
            Log.d("SseRepository", "internalStopListening: Closing existing EventSource.")
            eventSource?.cancel()
            eventSource = null
            isListening = false // Explicitly set isListening to false
        }
    }

    @Synchronized
    fun stopExplicitly() { // Call this if a component is definitively done
        val remainingListeners = activeListenerCount.decrementAndGet()
        Log.d("SseRepository", "stopExplicitly called. Remaining listeners: $remainingListeners")
        if (remainingListeners <= 0) {
            Log.i("SseRepository", "No active listeners remaining. Stopping SSE connection.")
            internalStopListening()
            currentAuthToken = null // Clear sensitive info when no one is listening
            currentIpAddress = null
            activeListenerCount.set(0) // Reset to 0 just in case
        }
    }
}
