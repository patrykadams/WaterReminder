package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Pokaż powiadomienie
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "water_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Czas na wodę! 💧")
            .setContentText("Pamiętaj o nawodnieniu.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)

        // 2. --- AUTO-RESCHEDULE (PĘTLA) ---
        // Pobierz ustawiony czas z pamięci
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("alert_interval", 60) // Domyślnie 60 min

        scheduleNextAlarm(context, intervalMinutes)
    }

    private fun scheduleNextAlarm(context: Context, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, ReminderReceiver::class.java)
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Oblicz czas: Teraz + X minut
        val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000)

        try {
            // Ustawiamy dokładny alarm (wymaga uprawnień w nowszych Androidach, ale dla testów zadziała)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, nextPendingIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}