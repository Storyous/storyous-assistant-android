package com.storyous.assistant

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallSyncRepository(private val sharedPreferences: SharedPreferences) {

    companion object {
        const val SP_PERSON_ID_KEY = "personId"
        const val SP_SYNC_ENABLED_KEY = "syncEnabled"
    }

    private val gson = Gson()
    val syncEnabledLive = MutableLiveData(false)
    var personId: String? = null

    suspend fun initFromPersistence() {
        val syncEnabled = isSyncEnabled()
        personId = getPersonId()

        withContext(Dispatchers.Main) {
            syncEnabledLive.value = syncEnabled
        }
    }

    suspend fun loadAccess(url: String): AccessResponse = withContext(Dispatchers.IO) {
        firestoreApi.loadAccess(url)
    }

    private suspend fun getPersonId(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(SP_PERSON_ID_KEY, null)
    }

    private suspend fun isSyncEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(SP_SYNC_ENABLED_KEY, false)
    }

    fun parseQRCode(qrCodeValue: String): QRCode {
        return gson.fromJson(qrCodeValue, QRCode::class.java)
    }

    fun storePersonId(personId: String?) {
        sharedPreferences.edit().putString(SP_PERSON_ID_KEY, personId).apply()
        this.personId = personId
    }

    fun setSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(SP_SYNC_ENABLED_KEY, enabled).apply()
        syncEnabledLive.value = enabled
    }

    fun onCallReceived(incomingNumber: String) {
        // TODO store incoming call to firestore
    }
}
