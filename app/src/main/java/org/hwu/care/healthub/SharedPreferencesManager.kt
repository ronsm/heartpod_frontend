package org.hwu.care.healthub // Make sure this matches your project's package name

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    companion object {
        // Name of the SharedPreferences file
        private const val PREFS_NAME = "HealthubAppPreferences"

        // Keys for the values stored in SharedPreferences
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_OPENHAB_IP = "openhab_ip_address"
        private const val KEY_SETUP_COMPLETED = "setup_completed" // Optional: to track if initial setup was done
    }

    init {
        // Initialize SharedPreferences instance
        // It's good practice to use applicationContext to avoid context leaks if this manager
        // might live longer than a specific Activity/Fragment context.
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the API token to SharedPreferences.
     * @param token The API token string to save.
     */
    fun saveToken(token: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_API_TOKEN, token)
        editor.apply() // Use apply() for asynchronous saving
    }

    /**
     * Retrieves the API token from SharedPreferences.
     * @return The saved API token, or null if not found.
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_API_TOKEN, null)
    }

    /**
     * Saves the OpenHAB IP address to SharedPreferences.
     * @param ipAddress The OpenHAB IP address string to save.
     */
    fun saveOpenhabIp(ipAddress: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_OPENHAB_IP, ipAddress)
        editor.apply()
    }

    /**
     * Retrieves the OpenHAB IP address from SharedPreferences.
     * @return The saved OpenHAB IP address, or null if not found.
     */
    fun getOpenhabIp(): String? {
        return sharedPreferences.getString(KEY_OPENHAB_IP, null)
    }

    /**
     * Saves the setup completion status.
     * @param isCompleted Boolean indicating if the initial setup is done.
     */
    fun saveSetupCompleted(isCompleted: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_SETUP_COMPLETED, isCompleted)
        editor.apply()
    }

    /**
     * Checks if the initial setup has been marked as completed.
     * @return True if setup is completed, false otherwise. Defaults to false.
     */
    fun isSetupCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    /**
     * Clears all data from this SharedPreferences file.
     * Useful for logout or resetting app settings.
     */
    fun clearAllPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
