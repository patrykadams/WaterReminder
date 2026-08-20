package com.patrykadamski.waterreminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WaterReminderApp : Application() {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Przypomnienia o piciu wody",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Powiadomienia przypominające o regularnym piciu wody"
        }
        notificationManager.createNotificationChannel(channel)
    }
}
