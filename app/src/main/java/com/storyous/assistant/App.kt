package com.storyous.assistant

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.firebase.FirebaseApp
import com.storyous.contacts.ContactsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import kotlin.reflect.KClass

class App : Application() {

    val contactsManager by lazy { ContactsManager() }
    val callSyncRepository by lazy {
        CallSyncRepository(
            getSharedPreferences("contactSyncConfig", Context.MODE_PRIVATE)
        )
    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        CoroutineScope(Dispatchers.IO).launch {
            callSyncRepository.initFromPersistence()
        }

        callSyncRepository.syncEnabledLive.observeForever { enabled ->
            runCatching {
                if (enabled) {
                    startService(IncomingCallSyncService::class)
                } else {
                    stopService(IncomingCallSyncService::class)
                }
            }
        }
    }
}

fun Context.getCallSyncRepository(): CallSyncRepository =
    (this.applicationContext as App).callSyncRepository

fun Context.getContactsManager(): ContactsManager =
    (this.applicationContext as App).contactsManager

fun Context.startService(clazz: KClass<out Service>) {
    val intent = Intent(this, clazz.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

fun Context.stopService(clazz: KClass<out Service>) {
    stopService(Intent(this, clazz.java))
}
