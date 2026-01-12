package org.hwu.care.healthub.data

import org.hwu.care.healthub.data.models.Reading

interface PiApi {
    suspend fun getLatestReading(deviceId: String): Reading?
    suspend fun setFocus(deviceId: String)
    suspend fun getItemState(itemName: String): String
}
