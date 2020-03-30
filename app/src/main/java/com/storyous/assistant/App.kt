package com.storyous.assistant

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import kotlin.reflect.KClass

class App : Application() {

    val contactsRepository by lazy {
        ContactsRepository(
            getSharedPreferences("contactSyncConfig", Context.MODE_PRIVATE)
        )
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        CoroutineScope(Dispatchers.IO).launch {
            contactsRepository.initFromPersistence()
        }

        contactsRepository.syncEnabledLive.observeForever { enabled ->
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

fun Context.getContactsRepository(): ContactsRepository =
    (this.applicationContext as App).contactsRepository

fun Context.startService(clazz: KClass<out Service>) {
    with(Intent(this, clazz.java)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(this)
        } else {
            startService(this)
        }
    }
}

fun Context.stopService(clazz: KClass<out Service>) {
    stopService(Intent(this, clazz.java))
}
