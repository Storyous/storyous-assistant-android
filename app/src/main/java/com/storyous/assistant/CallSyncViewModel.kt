package com.storyous.assistant

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class CallSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsManager = application.getContactsManager()
    private val repository = application.getCallSyncRepository()
    private var configuringJob: Job? = null
    private val errorConfigToast =
        Toast.makeText(getApplication(), R.string.config_error, Toast.LENGTH_SHORT)

    val isConfiguredLive = MutableLiveData(false)
    val isConfigured: Boolean
        get() = isConfiguredLive.value ?: false
    val syncEnabledLive: LiveData<Boolean> = repository.syncEnabledLive

    init {
        contactsManager.setAuthListener { isConfiguredLive.value = it }
    }

    override fun onCleared() {
        super.onCleared()
        contactsManager.removeAuthListener()
    }

    fun onQRCodeReceived(qrCodeValue: String) {
        if (configuringJob?.isActive == true) {
            return
        }

        configuringJob = viewModelScope.launch {
            val qrCode = repository.parseQRCode(qrCodeValue)
            runCatching {
                qrCode.configUrl
                    ?.let { repository.loadAccess(it) }
                    ?.let { contactsManager.authenticate(it.fields.token.stringValue) }
            }.onFailure {
                Timber.e(it, "Failed to configure app by QR")
                if (!errorConfigToast.view.isShown) {
                    errorConfigToast.show()
                }
                enableSync(false)
                repository.storePersonId(null)
            }.onSuccess {
                enableSync(true)
                repository.storePersonId(qrCode.personId)
            }
        }
    }

    fun enableSync(enabled: Boolean) {
        repository.setSyncEnabled(enabled)
    }

    fun deleteConfiguration() {
        enableSync(false)
        repository.storePersonId(null)
        contactsManager.signOut()
    }
}
