package org.hwu.care.healthub

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

// Imports from your DeviceModels.kt
import org.hwu.care.healthub.HealthDevice
import org.hwu.care.healthub.OpenhabEquipmentItem
import org.hwu.care.healthub.OpenhabItem
import org.hwu.care.healthub.OpenhabRootResponse
// SharedPreferencesManager should also be imported if in a different file/package
// import org.hwu.care.healthub.SharedPreferencesManager
// OpenhabApiService should be imported
// import org.hwu.care.healthub.OpenhabApiService


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val instanceId = UUID.randomUUID().toString()

    private val _devices = MutableLiveData<List<HealthDevice>>()
    val devices: LiveData<List<HealthDevice>> = _devices

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val sharedPreferencesManager = SharedPreferencesManager(application.applicationContext)
    private var apiToken: String = ""
    private var openhabIpAddress: String = "192.168.2.150" // Updated to Pi 2

    private val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            Log.d("OkHttp_MainViewModel", message) // More specific tag
        }
    }).apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val apiService: OpenhabApiService by lazy {
        if (openhabIpAddress.isBlank()) {
            Log.e("MainViewModel_Retrofit", "OpenHAB IP Address is BLANK. Retrofit will likely fail. Instance: $instanceId")
        }
        val baseUrl = "http://$openhabIpAddress:8080/"
        Log.d("MainViewModel_Retrofit", "Retrofit base URL: $baseUrl (Instance: $instanceId)")
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenhabApiService::class.java)
    }

    init {
        Log.i("MainViewModel_Instance", "ViewModel instance CREATED. ID: $instanceId")
        loadConfiguration()
        if (apiToken.isBlank() || openhabIpAddress.isBlank() || openhabIpAddress == "0.0.0.0") {
            Log.w("MainViewModel_Init", "API Token or OpenHAB IP is not valid. Data fetching and SSE might not work. IP: $openhabIpAddress, Token Blank: ${apiToken.isBlank()} (Instance: $instanceId)")
            _error.postValue("Configuration missing or invalid: API Token or OpenHAB IP not set correctly.")
        }
        // Initial fetch can be triggered from Activity/Fragment after ViewModel is ready
        // and configuration is known to be loaded.
    }

    private fun loadConfiguration() {
        // LOCAL OPENHAB (on HEALTHUB)
        apiToken = sharedPreferencesManager.getToken() ?: "oh.NHSTHT.QViW3MMVzsp56R8PNt3maoKrv9Z7iP7LNRymiPG25bYqlOXgV0BgggwQ8ZCbBbBdPTy6WxbBW0u0BBqCkiG9w"
        openhabIpAddress = sharedPreferencesManager.getOpenhabIp() ?: "192.168.2.150"

        //apiToken = sharedPreferencesManager.getToken() ?: "oh.MCP.tLQBOZFwd3UAnubBDKJZXoo7PV1tRXCm9fbLDoeRGJrJA1yIUCAyxCfcxdaixCuzrVg4c0khi1hef6XVu3yXQ"
        //openhabIpAddress = sharedPreferencesManager.getOpenhabIp() ?: "openhabian"

        Log.d("MainViewModel_Config", "Loaded Config - IP: $openhabIpAddress, Token IsNotBlank: ${apiToken.isNotBlank()} (Instance: $instanceId)")
    }

    fun logInstanceId(activityName: String) {
        Log.i("MainViewModel_Instance", "ViewModel accessed from $activityName. ID: $instanceId")
    }

    fun fetchHealthDevices() {
        // Reload config in case it changed in settings and ViewModel is reused
        loadConfiguration()

        if (apiToken.isBlank()) {
            Log.e("MainViewModel_Fetch", "Cannot fetch devices: API Token is blank. (Instance: $instanceId)")
            _error.postValue("API Token is not configured.")
            _isLoading.postValue(false)
            return
        }
        if (openhabIpAddress.isBlank() || openhabIpAddress == "0.0.0.0") {
            Log.e("MainViewModel_Fetch", "Cannot fetch devices: OpenHAB IP Address is blank or invalid ('$openhabIpAddress'). (Instance: $instanceId)")
            _error.postValue("OpenHAB IP Address is not configured or is invalid.")
            _isLoading.postValue(false)
            return
        }

        Log.d("MainViewModel_Fetch", "fetchHealthDevices() called. (Instance: $instanceId)")
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            Log.d("MainViewModel_Fetch", "Coroutine started for fetching devices. (Instance: $instanceId)")
            try {
                val bearerAuthToken = "Bearer $apiToken"
                val service = apiService // Ensures lazy init with current IP
                val response = service.getHealthGroupDetails(authToken = bearerAuthToken)

                if (response.isSuccessful && response.body() != null) {
                    val rootResponse = response.body()!!
                    val equipmentItems = rootResponse.members
                    Log.i("MainViewModel_Fetch", "Successfully fetched ${equipmentItems.size} equipment groups. (Instance: $instanceId)")
                    _devices.postValue(processEquipmentItems(equipmentItems))
                    startLiveUpdates()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("MainViewModel_Fetch", "API Error: ${response.code()} - ${response.message()} - Body: $errorBody. (Instance: $instanceId)")
                    _error.postValue("API Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel_Fetch", "Network/Parsing Exception: ${e.message}. (Instance: $instanceId)", e)
                _error.postValue("Failure: ${e.message}")
            } finally {
                Log.d("MainViewModel_Fetch", "Fetching finished, isLoading set to false. (Instance: $instanceId)")
                _isLoading.postValue(false)
            }
        }
    }

    private fun startLiveUpdates() {
        if (apiToken.isBlank() || openhabIpAddress.isBlank() || openhabIpAddress == "0.0.0.0") {
            Log.w("MainViewModel_SSE", "Cannot start SSE: API Token or OpenHAB IP is invalid. IP: $openhabIpAddress (Instance: $instanceId)")
            return
        }
        Log.d("MainViewModel_SSE", "startLiveUpdates() called. (Instance: $instanceId)")
        SseRepository.startListening(apiToken, openhabIpAddress)
        Log.d("MainViewModel_SSE", "SSE listening initiated via SseRepository. (Instance: $instanceId)")

        viewModelScope.launch {
            SseRepository.itemStateChanges.collectLatest { (itemName, newState) ->
                Log.d("MainViewModel_SSE", "Collected from SseRepository Flow: Item='${itemName}', New State='${newState}'. (Instance: $instanceId)")
                updateDeviceState(itemName, newState)
            }
        }
    }

    private fun updateDeviceState(itemName: String, newState: String) {
        Log.d("MainViewModel_Update", "updateDeviceState CALLED for Item: '${itemName}', New State: '${newState}'. (Instance: $instanceId)")

        val currentDeviceList = _devices.value
        if (currentDeviceList == null) {
            Log.w("MainViewModel_Update", "currentDevices is NULL. Cannot update. (Instance: $instanceId)")
            return
        }
        Log.v("MainViewModel_Update", "Current device list size: ${currentDeviceList.size}. (Instance: $instanceId)")

        var anyDeviceUpdated = false
        val updatedDeviceList = currentDeviceList.map { device ->
            var deviceModified = false
            var updatedDevice = device

            // Check if the update is for one of the device's direct properties (OpenhabItem list)
            val propertyIndex = device.properties.indexOfFirst { it.name == itemName }
            if (propertyIndex != -1) {
                val oldProperty = device.properties[propertyIndex]
                Log.i("MainViewModel_Update", "MATCH for PROPERTY: Device='${device.name}', Property='${oldProperty.name}', OldState='${oldProperty.state}', NewState='$newState'. (Instance: $instanceId)")
                val updatedProperties = device.properties.toMutableList()
                updatedProperties[propertyIndex] = oldProperty.copy(state = newState)
                updatedDevice = device.copy(properties = updatedProperties)
                deviceModified = true
                anyDeviceUpdated = true
            } else if (device.name + "_Status" == itemName) {
                // Check if the update is for the device's main status
                Log.i("MainViewModel_Update", "MATCH for STATUS: Device='${device.name}', Item(Status)='$itemName', OldStatus='${device.status}', NewState='$newState'. (Instance: $instanceId)")
                updatedDevice = device.copy(status = newState)
                deviceModified = true
                anyDeviceUpdated = true
            }
            // Add other specific top-level fields of HealthDevice if they are also OpenHAB items not in 'properties'
            else if (device.name + "_LastUse" == itemName) {
                Log.i("MainViewModel_Update", "MATCH for LastUse: Device='${device.name}', Item(LastUse)='$itemName', OldValue='${device.lastUseTime}', NewState='$newState'. (Instance: $instanceId)")
                updatedDevice = device.copy(lastUseTime = newState) // Ensure HealthDevice has lastUseTime
                deviceModified = true
                anyDeviceUpdated = true
            } else if (device.name + "_UseDue" == itemName) {
                Log.i("MainViewModel_Update", "MATCH for UseDue: Device='${device.name}', Item(UseDue)='$itemName', OldValue='${device.useDue}', NewState='$newState'. (Instance: $instanceId)")
                updatedDevice = device.copy(useDue = newState) // Ensure HealthDevice has useDue
                deviceModified = true
                anyDeviceUpdated = true
            }


            if (deviceModified) {
                Log.d("MainViewModel_Update", "Device '${device.name}' was modified. (Instance: $instanceId)")
            }
            updatedDevice
        }

        if (anyDeviceUpdated) {
            Log.i("MainViewModel_Update", "POSTING to LiveData. Overall, updates were made for item '${itemName}'. (Instance: $instanceId)")
            _devices.postValue(updatedDeviceList)
        } else {
            Log.v("MainViewModel_Update", "NO POST to LiveData: No device or property matched item '${itemName}'. (Instance: $instanceId)")
        }
    }

    private fun processEquipmentItems(equipmentItems: List<OpenhabEquipmentItem>): List<HealthDevice> {
        Log.d("MainViewModel_Process", "processEquipmentItems called with ${equipmentItems.size} groups. (Instance: $instanceId)")
        val processedDevices = mutableListOf<HealthDevice>()
        val openhabBaseUrl = "http://$openhabIpAddress:8080"

        for (equipment in equipmentItems) {
            val baseName = equipment.name // e.g., "Oximeter"

            // Find specific items by convention (e.g., DeviceName_Status)
            val statusItem = equipment.members.find { it.name == "${baseName}_Status" }
            val pictureUrlItem = equipment.members.find { it.name == "${baseName}_PictureURL" }
            val instructionsUrlItem = equipment.members.find { it.name == "${baseName}_InstructionsURL" }
            val lastUseItem = equipment.members.find { it.name == "${baseName}_LastUse" }
            val useDueItem = equipment.members.find { it.name == "${baseName}_UseDue" }

            var fullPictureUrl = ""
            pictureUrlItem?.state?.let { path ->
                if (path.isNotBlank()) {
                    fullPictureUrl = if (path.startsWith("http://") || path.startsWith("https://")) path else "$openhabBaseUrl$path"
                    fullPictureUrl = fullPictureUrl.replace("\"", "") // Clean quotes
                }
            }
            Log.v("MainViewModel_Process", "Device: ${equipment.name}, Raw Pic State: '${pictureUrlItem?.state}', Processed URL: '$fullPictureUrl'. (Instance: $instanceId)")


            // All other members are considered dynamic properties (List<OpenhabItem>)
            // Exclude the ones we've already handled and any sub-groups.
            val dynamicProperties = equipment.members.filterNot { item ->
                item.name == statusItem?.name ||
                        item.name == pictureUrlItem?.name ||
                        item.name == instructionsUrlItem?.name ||
                        item.name == lastUseItem?.name ||
                        item.name == useDueItem?.name ||
                        item.type == "Group" // Assuming "Group" type items are not end-point data
            }

            val device = HealthDevice(
                name = baseName,
                label = equipment.label ?: baseName,
                status = statusItem?.state ?: "OFFLINE", // Default to OFFLINE
                useDue = useDueItem?.state ?: "NO",       // Default based on your model
                lastUseTime = lastUseItem?.state ?: "Unknown", // Default
                pictureUrl = fullPictureUrl,
                instructionsUrl = instructionsUrlItem?.state?.let { path -> if (path.startsWith("http")) path else "$openhabBaseUrl$path" }?.replace("\"", "") ?: "",
                properties = dynamicProperties // Directly assign the filtered OpenhabItem list
            )
            processedDevices.add(device)
            Log.v("MainViewModel_Process", "Processed device: ${device.label} with ${dynamicProperties.size} dynamic properties. (Instance: $instanceId)")
        }
        Log.i("MainViewModel_Process", "Finished processing. Total devices: ${processedDevices.size}. (Instance: $instanceId)")
        return processedDevices
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("MainViewModel_Lifecycle", "onCleared called. Stopping SSE listening. (Instance: $instanceId)")
        SseRepository.stopExplicitly()
    }
}

