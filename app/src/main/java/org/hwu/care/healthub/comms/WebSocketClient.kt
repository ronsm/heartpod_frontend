package org.hwu.care.healthub.comms

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import org.hwu.care.healthub.AppState
import org.json.JSONObject

private const val TAG = "WebSocketClient"
private const val RECONNECT_DELAY_MS = 2000L

class WebSocketClient : CommsClient {

    private val http = OkHttpClient()
    private var scope: CoroutineScope? = null
    private var webSocket: WebSocket? = null
    private var wsUrl = ""
    private var onStateChanged: ((AppState) -> Unit)? = null
    private var stopped = false

    override fun start(baseUrl: String, onStateChanged: (AppState) -> Unit) {
        wsUrl = baseUrl.trimEnd('/')
        this.onStateChanged = onStateChanged
        stopped = false
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        connect()
        Log.d(TAG, "Started, connecting to $wsUrl")
    }

    private fun connect() {
        val request = Request.Builder().url(wsUrl).build()
        webSocket = http.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Connected")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "state" -> {
                            val pageId = json.getInt("page_id")
                            val data = mutableMapOf<String, String>()
                            json.optJSONObject("data")?.let { obj ->
                                obj.keys().forEach { key -> data[key] = obj.get(key).toString() }
                            }
                            onStateChanged?.invoke(AppState(pageId, data))
                        }
                        "tts" -> {
                            // TODO: implement Temi TTS handling
                            Log.d(TAG, "TTS received: ${json.optString("text")}")
                        }
                        else -> Log.w(TAG, "Unknown message type: ${json.optString("type")}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse message: ${e.message}")
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed: $reason")
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Failure: ${t.message}")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (stopped) return
        scope?.launch {
            delay(RECONNECT_DELAY_MS)
            if (!stopped) connect()
        }
    }

    override fun sendAction(action: String, data: Map<String, String>) {
        val msg = JSONObject().apply {
            put("type", "action")
            put("action", action)
            put("data", JSONObject(data as Map<*, *>))
        }.toString()
        val sent = webSocket?.send(msg) ?: false
        if (!sent) Log.w(TAG, "Cannot send '$action': not connected")
    }

    override fun stop() {
        stopped = true
        webSocket?.close(1000, "Client stopping")
        webSocket = null
        scope?.cancel()
        scope = null
        Log.d(TAG, "Stopped")
    }
}
