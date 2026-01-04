package com.patrykadamski.waterreminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kliknięcie w powiadomienie otworzy aplikację
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "water_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Domyślna ikonka
            .setContentTitle("Czas na wodę! 💧")
            .setContentText("Napij się szklankę wody, aby zrealizować cel.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ważne!
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Dźwięk + Wibracja
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Zniknij po kliknięciu
            .build()

        notificationManager.notify(1, notification)
    }
}