package com.storyous.assistant

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val sharedPreferences: SharedPreferences) {

    companion object {
        const val SP_CONFIG_KEY = "config"
        const val SP_SYNC_ENABLED_KEY = "syncEnabled"
    }

    private val gson = Gson()
    val configLive = MutableLiveData<Config>()
    val isConfiguredLive = Transformations.map(configLive) { it != null }
    val syncEnabledLive = MutableLiveData(false)

    suspend fun initFromPersistence() {
        val config = loadConfig()
        val syncEnabled = loadSyncEnabled()

        withContext(Dispatchers.Main) {
            configLive.value = config
            syncEnabledLive.value = syncEnabled
        }
    }

    suspend fun loadAccess(url: String): AccessResponse = withContext(Dispatchers.IO) {
        firestoreApi.loadAccess(url)
    }

    private suspend fun loadConfig(): Config? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(SP_CONFIG_KEY, null)
            ?.let { gson.fromJson(it, Config::class.java) }
    }

    private suspend fun loadSyncEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(SP_SYNC_ENABLED_KEY, false)
    }

    fun parseQRCode(qrCodeValue: String): QRCode {
        return gson.fromJson(qrCodeValue, QRCode::class.java)
    }

    fun storeConfig(config: Config?) {
        sharedPreferences.edit().putString(SP_CONFIG_KEY, gson.toJson(config)).apply()
        configLive.value = config
    }

    fun setSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(SP_SYNC_ENABLED_KEY, enabled).apply()
        syncEnabledLive.value = enabled
    }

    fun onCallReceived(incomingNumber: String) {
        // TODO store incoming call to firestore
    }
}
