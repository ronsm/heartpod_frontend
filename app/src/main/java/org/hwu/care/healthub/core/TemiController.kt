package org.hwu.care.healthub.core

interface TemiController {
    fun speak(text: String, onDone: () -> Unit = {})
    fun navigateTo(location: String)
    fun onStart()
    fun onStop()
}
