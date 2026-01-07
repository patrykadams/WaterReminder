package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)

        // --- NOWOŚĆ: SPRAWDZAMY CZY CEL JUŻ OSIĄGNIĘTY ---
        // Jeśli tak, nie wysyłamy powiadomienia i nie planujemy następnego na dziś.
        val dailyGoal = prefs.getInt("daily_goal", 2000)

        // Musimy pobrać aktualny stan wody z bazy w tle
        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val entry = dao.getTodayWater(todayDate)
            val currentAmount = entry?.amount ?: 0

            // JEŚLI CEL ZREALIZOWANY -> KOŃCZYMY PRACĘ NA DZIŚ
            if (currentAmount >= dailyGoal) {
                return@launch // Nie wysyłaj powiadomienia, nie planuj następnego
            }

            // JEŚLI NIE, WYSYŁAMY POWIADOMIENIE (Kod z poprzedniej odpowiedzi)
            // ... (tutaj cała reszta kodu z budzeniem ekranu, tekstami itd.)
            // Ze względu na limit znaków, upewnij się, że wkleisz tu kod z poprzedniego kroku,
            // ale opakowany w ten warunek if (currentAmount < dailyGoal)

            // --- TUTAJ WKLEJ KOD WYSYŁANIA I PLANOWANIA ---
            launchNotification(context, prefs, quickAddAmount)
        }
    }

    // Wyciągnąłem logikę do osobnej funkcji dla czytelności wewnątrz korutyny
    private fun launchNotification(context: Context, prefs: android.content.SharedPreferences, quickAddAmount: Int) {
        val gender = prefs.getString("user_gender", "M") ?: "M"
        val missedCount = prefs.getInt("missed_reminders_count", 0)
        val (title, text) = getMotivationText(missedCount, gender)

        prefs.edit().putInt("missed_reminders_count", missedCount + 1).apply()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "WaterReminder:WakeUpScreen")
            wakeLock.acquire(3000)
        }

        val notificationId = System.currentTimeMillis().toInt()
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, notificationId, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val addWaterIntent = Intent(context, AddWaterReceiver::class.java)
        val addWaterPendingIntent = PendingIntent.getBroadcast(context, notificationId, addWaterIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val remoteInput = RemoteInput.Builder("key_custom_amount").setLabel("Ile wypiłeś?").build()
        val customWaterIntent = Intent(context, AddCustomWaterReceiver::class.java)
        val customWaterPendingIntent = PendingIntent.getBroadcast(context, notificationId, customWaterIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val customAction = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_edit, "Inna ilość", customWaterPendingIntent).addRemoteInput(remoteInput).build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "water_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_input_add, "+$quickAddAmount ml", addWaterPendingIntent)
            .addAction(customAction)
            .setGroup("WATER_REMINDER_GROUP")
            .build()

        notificationManager.notify(notificationId, notification)

        scheduleNextAlarm(context)
    }

    // (Metody getMotivationText i scheduleNextAlarm bez zmian - skopiuj z poprzedniej odpowiedzi)
    private fun getMotivationText(missedCount: Int, gender: String): Pair<String, String> {
        val isFemale = gender == "K"
        val facts = listOf("Głowa nie będzie boleć. 💆‍♀️", "Darmowa energia w 3.. 2.. 1.. ⚡", "Cera Ci podziękuje. ✨", "Nerki lubią to.")
        val randomFact = facts.random()
        return if (isFemale) {
            when {
                missedCount == 0 -> listOf("Kocham Cię! ❤️" to "Wypij szklankę wody. Dbaj o siebie.", "Jesteś Super! 🌟" to "Szybki łyk i wracamy do bycia super.", "Puk puk! 🚪" to "To ja, Twoja woda. Wpuścisz mnie?", "Czas na przerwę 🥤" to randomFact, "Nawadnianie! 💧" to "Zrób to dla zdrowia (i dla mnie).").random()
                missedCount == 1 -> "Halo, tu Woda 🌊" to "Czuję się ignorowana... Napij się!"
                else -> "Zamieniasz się w kaktusa 🌵" to "Serio, ile można czekać? Pij natychmiast!"
            }
        } else {
            when {
                missedCount == 0 -> listOf("Czas na wodę 💧" to randomFact, "Jesteś Super! 🔥" to "Utrzymaj dobrą passę.", "Łykamy? 🥤" to "Organizm woła o paliwo.").random()
                missedCount == 1 -> "Halo? 🤨" to "Zapomniałeś o mnie. Nadrabiamy!"
                else -> "Zaraz uschniesz 💀" to "Nie świruj, pij tę wodę."
            }
        }
    }

    private fun scheduleNextAlarm(context: Context) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("alert_interval", 60)
        val wakeUpHour = prefs.getInt("wake_up_hour", 8)
        val sleepHour = prefs.getInt("sleep_hour", 22)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, ReminderReceiver::class.java)
        val nextPendingIntent = PendingIntent.getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, intervalMinutes)
        val nextAlarmHour = calendar.get(Calendar.HOUR_OF_DAY)
        if (nextAlarmHour >= sleepHour || nextAlarmHour < wakeUpHour) {
            if (nextAlarmHour >= sleepHour) calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, wakeUpHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        }
        try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, nextPendingIntent) } catch (e: SecurityException) {}
    }
}