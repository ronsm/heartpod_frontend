package org.hwu.care.healthub.data

import android.util.Log
import org.hwu.care.healthub.data.models.Reading
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// OpenHAB REST API response model
data class OpenHabItemResponse(
    val name: String,
    val state: String,
    val type: String
)

interface PiApiService {
    @GET("/rest/items/{itemName}")
    suspend fun getItem(
        @Path("itemName") itemName: String,
        @Header("Authorization") authToken: String
    ): OpenHabItemResponse
}

class PiApiImpl : PiApi {
    private val service: PiApiService
    private val authToken = "Bearer oh.NHSTHT.QViW3MMVzsp56R8PNt3maoKrv9Z7iP7LNRymiPG25bYqlOXgV0BgggwQ8ZCbBbBdPTy6WxbBW0u0BBqCkiG9w"

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.2.150:8080/") // Pi 2 IP
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(PiApiService::class.java)
    }

    override suspend fun getLatestReading(deviceId: String): Reading? {
        return try {
            // Map device IDs to OpenHAB item names
            val itemBaseName = when (deviceId.lowercase()) {
                "oximeter", "pulse_oximeter", "pulseoximeter" -> "Oximeter"
                "bp", "blood_pressure", "omron" -> "Omron"
                "hr", "heart_rate", "polar" -> "Polar"
                "thermometer", "thermometer_station", "beurer" -> "Thermometer"
                else -> deviceId.capitalize()
            }

            // Thermometer Handling
            if (itemBaseName == "Thermometer") {
                val tempItem = service.getItem("${itemBaseName}_Temperature", authToken)
                val lastUseItem = try { service.getItem("${itemBaseName}_LastUse", authToken) } catch(e:Exception) { null }
                
                // Freshness check
                if (lastUseItem != null && lastUseItem.state != "NULL") {
                     try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val lastUseTime = sdf.parse(lastUseItem.state.replace("Z", ""))
                        val now = java.util.Date()
                        val ageSeconds = (now.time - lastUseTime.time) / 1000
                        
                        // 60s for temperature is generous but safe
                        if (ageSeconds > 60) {
                            Log.w("PiApiImpl", "Temp Data is stale (${ageSeconds}s old), returning null")
                            return null
                        }
                    } catch (e: Exception) {
                        Log.e("PiApiImpl", "Failed to parse LastUse timestamp", e)
                    }
                }
                
                return if (tempItem.state != "NULL") {
                    Reading(deviceId, "${tempItem.state}°C", "°C", System.currentTimeMillis())
                } else null
            }

            // Default Oximeter/BP Handling (Existing Logic)
            // Fetch SpO2 and Pulse from OpenHAB
            val spo2Item = service.getItem("${itemBaseName}_SpO2", authToken)
            val pulseItem = try {
                service.getItem("${itemBaseName}_Pulse", authToken)
            } catch (e: Exception) {
                null
            }
            
             // Check if data is fresh (updated in last 30 seconds)
            val lastUseItem = try {
                service.getItem("${itemBaseName}_LastUse", authToken)
            } catch (e: Exception) {
                null
            }
            
            // If no recent update, return null (no fresh reading)
            if (lastUseItem != null && lastUseItem.state != "NULL") {
                try {
                    // Parse ISO 8601 timestamp using SimpleDateFormat (Android 6 compatible)
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val lastUseTime = sdf.parse(lastUseItem.state.replace("Z", ""))
                    val now = java.util.Date()
                    val ageSeconds = (now.time - lastUseTime.time) / 1000
                    
                    if (ageSeconds > 30) {
                        Log.w("PiApiImpl", "Data is stale (${ageSeconds}s old), returning null")
                        return null
                    }
                } catch (e: Exception) {
                    Log.e("PiApiImpl", "Failed to parse LastUse timestamp: ${lastUseItem.state}", e)
                }
            }
            
            val readingText = buildString {
                append("SpO2: ${spo2Item.state}%")
                if (pulseItem != null) {
                    val pulseValue = pulseItem.state.split(" ").firstOrNull() ?: pulseItem.state
                    append(", Pulse: $pulseValue bpm")
                }
            }

            // Convert to Reading model
            Reading(
                deviceId = deviceId,
                value = readingText,
                unit = "",
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun setFocus(deviceId: String) {
        // OpenHAB doesn't need explicit focus - BLE service auto-scans
        // This is a no-op for now
    }
    
    override suspend fun getItemState(itemName: String): String {
        return try {
            val item = service.getItem(itemName, authToken)
            item.state
        } catch (e: Exception) {
            Log.e("PiApiImpl", "Failed to get item state for $itemName", e)
            "NULL"
        }
    }
}
