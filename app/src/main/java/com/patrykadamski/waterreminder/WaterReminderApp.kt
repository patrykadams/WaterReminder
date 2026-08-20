package com.patrykadamski.waterreminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WaterReminderApp : Application() {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val EVENING_CHANNEL_ID = "evening_summary_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(
            id = CHANNEL_ID,
            name = "Przypomnienia o piciu wody",
            description = "Powiadomienia przypominające o regularnym piciu wody",
            importance = NotificationManager.IMPORTANCE_HIGH
        )
        createNotificationChannel(
            id = EVENING_CHANNEL_ID,
            name = "Wieczorne podsumowanie dnia",
            description = "Codzienne podsumowanie spożycia wody i passy, ok. godzinę przed snem",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun createNotificationChannel(id: String, name: String, description: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(id) != null) return

        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
        }
        notificationManager.createNotificationChannel(channel)
    }
}
