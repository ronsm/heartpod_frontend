package org.hwu.care.healthub.data.models

data class Reading(
    val deviceId: String,
    val value: String,
    val unit: String,
    val timestamp: Long
)
