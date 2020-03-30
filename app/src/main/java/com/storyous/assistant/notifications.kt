package com.storyous.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build

const val NOTIF_ID = 1
const val NOTIF_CHANNEL_ID = "Channel_Id"
private const val NOTIF_CHANNEL_NAME = "Self Checkout channel"
private const val NOTIF_CHANNEL_DESC = "Self Checkout channel"

fun initChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }

    val notificationManager = context.getNotificationService()
    val channel = NotificationChannel(
        NOTIF_CHANNEL_ID,
        NOTIF_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
    )
    channel.description = NOTIF_CHANNEL_DESC
    notificationManager.createNotificationChannel(channel)
}

fun Context.getNotificationService() =
    getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
