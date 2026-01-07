package com.patrykadamski.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getInt("daily_goal", 2000)

        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val entry = dao.getTodayWater(todayDate)
            val currentAmount = entry?.amount ?: 0

            // Jeśli cel osiągnięty, nie planujemy więcej
            if (currentAmount >= dailyGoal) {
                AlarmScheduler.scheduleNextAlarm(context)
                return@launch
            }

            launchNotification(context, prefs)
        }
    }

    private fun launchNotification(context: Context, prefs: android.content.SharedPreferences) {
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)
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
        val pendingIntent = android.app.PendingIntent.getActivity(context, notificationId, openAppIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)

        val addWaterIntent = Intent(context, AddWaterReceiver::class.java)
        val addWaterPendingIntent = android.app.PendingIntent.getBroadcast(context, notificationId, addWaterIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)

        val remoteInput = RemoteInput.Builder("key_custom_amount").setLabel("Ile wypiłeś?").build()
        val customWaterIntent = Intent(context, AddCustomWaterReceiver::class.java)
        val customWaterPendingIntent = android.app.PendingIntent.getBroadcast(context, notificationId, customWaterIntent, android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        val customAction = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_edit, "Inna ilość", customWaterPendingIntent).addRemoteInput(remoteInput).build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
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

        // Planujemy kolejny alarm (inteligentne przeplanowanie)
        AlarmScheduler.scheduleNextAlarm(context)
    }

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
}