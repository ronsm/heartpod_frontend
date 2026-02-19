package org.hwu.care.healthub.comms

import org.hwu.care.healthub.AppState

/**
 * Transport-agnostic interface for backend communication.
 * Current implementation: HTTP polling (HttpPollingClient).
 * Swap for WebSocket or anything else by implementing this interface.
 */
interface CommsClient {
    fun start(baseUrl: String, onStateChanged: (AppState) -> Unit)
    fun sendAction(action: String, data: Map<String, String> = emptyMap())
    fun stop()
}
