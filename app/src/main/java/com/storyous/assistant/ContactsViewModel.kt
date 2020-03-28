package com.storyous.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val sharedPreferences = application.getSharedPreferences(
        "contactSyncConfig",
        Context.MODE_PRIVATE
    )
    private val configLive = MutableLiveData<Config>()
    val isConfigured = Transformations.map(configLive) { it != null }
    val syncEnabled = MutableLiveData(false)

    init {
        viewModelScope.launch {
            configLive.value = getConfig()
        }
    }

    private fun getConfig(): Config? {
        return sharedPreferences.getString("config", null)
            ?.let { Gson().fromJson(it, Config::class.java) }
    }

    private fun storeConfig(config: Config?) {
        sharedPreferences.edit().putString("config", gson.toJson(config)).apply()
        configLive.value = config
    }

    fun onContactsConfigReceived(configJson: String) {
        viewModelScope.launch {
            val config = gson.fromJson(configJson, Config::class.java)

            storeConfig(config)
            enableSync(config.token != null)
            if (config.token != null) {
                // TODO start receiver to listen incoming calls
            }
        }
    }

    fun enableSync(enabled: Boolean) {
        syncEnabled.value = enabled
    }

    fun deleteConfiguration() {
        enableSync(false)
        storeConfig(null)
    }
}
