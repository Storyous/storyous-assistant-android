package com.storyous.assistant

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import timber.log.Timber

class IncomingCallSyncService : Service() {

    private val phoneStateListener = object : PhoneStateListener() {

        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            super.onCallStateChanged(state, incomingNumber)

            if (state == TelephonyManager.CALL_STATE_RINGING) {
                Timber.d("Incoming call: $incomingNumber")
                getContactsRepository().onCallReceived(incomingNumber)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        initChannels(this)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_sync_black_24dp)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_info))
            .setContentIntent(pendingIntent)
            .build()
            .also {
                startForeground(NOTIF_ID, it)
            }

        runCatching {
            // TELEPHONY MANAGER class object to register one listener
            getTelephonyService().listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }.exceptionOrNull()?.also {
            Timber.e(it, "Phone Receive Error")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getTelephonyService().listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        stopForeground(true)
    }
}

fun Context.getTelephonyService() = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
