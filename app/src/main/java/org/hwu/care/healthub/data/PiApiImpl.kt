package org.hwu.care.healthub.data

import org.hwu.care.healthub.data.models.Reading
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PiApiService {
    @GET("/readings/{deviceId}/latest")
    suspend fun getLatestReading(@Path("deviceId") deviceId: String): Reading?

    @POST("/control/focus/{deviceId}")
    suspend fun setFocus(@Path("deviceId") deviceId: String)
}

class PiApiImpl : PiApi {
    private val service: PiApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.2.150:8080/") // Pi 2 IP
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(PiApiService::class.java)
    }

    override suspend fun getLatestReading(deviceId: String): Reading? {
        return try {
            service.getLatestReading(deviceId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setFocus(deviceId: String) {
        try {
            service.setFocus(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
