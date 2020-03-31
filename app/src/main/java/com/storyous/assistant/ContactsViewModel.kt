package com.storyous.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.getContactsRepository()
    private var configuringJob: Job? = null
    val isConfiguredLive: LiveData<Boolean> = repository.isConfiguredLive
    val isConfigured: Boolean
        get() = repository.isConfiguredLive.value ?: false
    val syncEnabledLive: LiveData<Boolean> = repository.syncEnabledLive

    fun onQRCodeReceived(qrCodeValue: String) {
        configuringJob?.cancel()
        configuringJob = viewModelScope.launch {
            val config = runCatching {
                val qrCode = repository.parseQRCode(qrCodeValue)
                qrCode.configUrl
                    ?.let { repository.loadAccess(it) }
                    ?.let {
                        Config(
                            qrCode.personId,
                            it.fields.token.stringValue,
                            it.fields.validUntil.integerValue
                        )
                    }
            }.getOrElse {
                Timber.e(it, "Failed to configure app by QR")
                null
            }

            repository.storeConfig(config)
            enableSync(config != null)
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
