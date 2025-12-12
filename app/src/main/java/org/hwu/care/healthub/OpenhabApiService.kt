package org.hwu.care.healthub

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OpenhabApiService {
    // This function will fetch a group and all items inside it, recursively
    @GET("rest/items/gHealth?metadata=.*&recursive=true&parents=false") // Your curl endpoint
        suspend fun getHealthGroupDetails( // Renamed for clarity
            @Header("Authorization") authToken: String = "oh.HEALTHUB.Oqf0KVxIF0sWTSxBizEzpWucru2iix7hudwW0EcSuOLfgioXaABhN5L7AwNXEBftMNGUibaMa6AWAkU2DUw"
        ): Response<OpenhabRootResponse> // <-- Changed to the wrapper
}