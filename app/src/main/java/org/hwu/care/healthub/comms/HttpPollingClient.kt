package org.hwu.care.healthub.comms

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.hwu.care.healthub.AppState
import org.json.JSONObject

private const val TAG = "HttpPollingClient"
private const val POLL_INTERVAL_MS = 500L

class HttpPollingClient : CommsClient {

    private val http = OkHttpClient()
    private var scope: CoroutineScope? = null
    private var baseUrl = ""
    private var onStateChanged: ((AppState) -> Unit)? = null

    override fun start(baseUrl: String, onStateChanged: (AppState) -> Unit) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.onStateChanged = onStateChanged
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope?.launch {
            while (isActive) {
                try {
                    pollState()
                } catch (e: Exception) {
                    Log.w(TAG, "Poll failed: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Started polling $baseUrl")
    }

    override fun sendAction(action: String, data: Map<String, String>) {
        scope?.launch {
            try {
                val body = JSONObject().apply {
                    put("action", action)
                    put("data", JSONObject(data as Map<*, *>))
                }.toString()
                val request = Request.Builder()
                    .url("$baseUrl/action")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().close()
                Log.d(TAG, "Action sent: $action")
            } catch (e: Exception) {
                Log.w(TAG, "Action failed: ${e.message}")
            }
        }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        Log.d(TAG, "Stopped")
    }

    private fun pollState() {
        val request = Request.Builder().url("$baseUrl/state").build()
        val body = http.newCall(request).execute().use { it.body?.string() } ?: return
        val json = JSONObject(body)
        val pageId = json.getInt("page_id")
        val data = mutableMapOf<String, String>()
        json.optJSONObject("data")?.let { obj ->
            obj.keys().forEach { key -> data[key] = obj.get(key).toString() }
        }
        onStateChanged?.invoke(AppState(pageId, data))
    }
}
