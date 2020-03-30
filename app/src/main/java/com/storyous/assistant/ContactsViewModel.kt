package com.storyous.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.getContactsRepository()
    val isConfiguredLive: LiveData<Boolean> = repository.isConfiguredLive
    val isConfigured: Boolean
        get() = repository.isConfiguredLive.value ?: false
    val syncEnabledLive: LiveData<Boolean> = repository.syncEnabledLive

    fun onContactsConfigReceived(configJson: String) {
        viewModelScope.launch {
            runCatching {
                val config = repository.parseConfig(configJson)
                repository.storeConfig(config)
                enableSync(config.token != null)
            }.exceptionOrNull().also {
                Timber.e(it, "Failed to receive config")
            }
        }
    }

    fun enableSync(enabled: Boolean) {
        repository.setSyncEnabled(enabled)
    }

    fun deleteConfiguration() {
        enableSync(false)
        repository.storeConfig(null)
    }
}
