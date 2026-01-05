package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar // Ważny import do obliczania czasu

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)

        // 1. Pokaż powiadomienie (tylko jeśli jest teraz "dzień", ale dla pewności pokazujemy zawsze,
        // bo może to być ostatnie przypomnienie przed snem)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val addWaterIntent = Intent(context, AddWaterReceiver::class.java)
        val addWaterPendingIntent = PendingIntent.getBroadcast(
            context, 1, addWaterIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "water_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Czas na wodę! 💧")
            .setContentText("Napij się, aby zrealizować cel.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_input_add, "+$quickAddAmount ml", addWaterPendingIntent)
            .build()

        notificationManager.notify(1, notification)

        // 2. Zaplanuj następny (z uwzględnieniem nocy)
        scheduleNextAlarm(context)
    }

    private fun scheduleNextAlarm(context: Context) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("alert_interval", 60)
        val wakeUpHour = prefs.getInt("wake_up_hour", 8)
        val sleepHour = prefs.getInt("sleep_hour", 22)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, ReminderReceiver::class.java)
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Obliczamy teoretyczny czas następnego alarmu (Teraz + Interwał)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, intervalMinutes)

        val nextAlarmHour = calendar.get(Calendar.HOUR_OF_DAY)

        // LOGIKA NOCNA:
        // Jeśli następny alarm wypada PO godzinie spania LUB PRZED godziną wstania...
        if (nextAlarmHour >= sleepHour || nextAlarmHour < wakeUpHour) {
            // ...to przesuń go na jutro rano (na godzinę wakeUpHour)

            // Jeśli jest już po północy (np. 1:00), to ustawiamy na dzisiaj 8:00.
            // Jeśli jest przed północą (np. 23:00), to ustawiamy na jutro 8:00.
            if (nextAlarmHour >= sleepHour) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Dodaj jeden dzień
            }

            calendar.set(Calendar.HOUR_OF_DAY, wakeUpHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        }
        // W przeciwnym razie (dzień) zostawiamy czas taki, jaki wyszedł z obliczeń (Teraz + Interwał)

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, nextPendingIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}