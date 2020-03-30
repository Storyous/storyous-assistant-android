package com.storyous.assistant

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope

class ContactsRepository(private val sharedPreferences: SharedPreferences) {

    private val gson = Gson()
    val configLive = MutableLiveData<Config>()
    val isConfigured = Transformations.map(configLive) { it != null }
    val syncEnabled = MutableLiveData(false)

    suspend fun loadConfig() {
        configLive.value = getConfig()
    }

    suspend fun getConfig(): Config? = coroutineScope {
        sharedPreferences.getString("config", null)
            ?.let { gson.fromJson(it, Config::class.java) }
    }

    fun parseConfig(configJson: String): Config {
        return gson.fromJson(configJson, Config::class.java)
    }

    fun storeConfig(config: Config?) {
        sharedPreferences.edit().putString("config", gson.toJson(config)).apply()
        configLive.value = config
    }
}
