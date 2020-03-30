package com.storyous.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactsRepository(
        application.getSharedPreferences("contactSyncConfig", Context.MODE_PRIVATE)
    )
    val isConfigured: LiveData<Boolean> = repository.isConfigured
    val syncEnabled: LiveData<Boolean> = repository.syncEnabled

    init {
        viewModelScope.launch {
            repository.loadConfig()
        }
    }

    fun onContactsConfigReceived(configJson: String) {
        viewModelScope.launch {
            val config = repository.parseConfig(configJson)

            repository.storeConfig(config)
            enableSync(config.token != null)
            if (config.token != null) {
                // TODO start receiver to listen incoming calls
            }
        }
    }

    fun enableSync(enabled: Boolean) {
        repository.syncEnabled.value = enabled
    }

    fun deleteConfiguration() {
        enableSync(false)
        repository.storeConfig(null)
    }
}
