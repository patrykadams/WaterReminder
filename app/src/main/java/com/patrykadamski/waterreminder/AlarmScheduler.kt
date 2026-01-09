package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

object AlarmScheduler {

    // --- NOWOŚĆ: Sztywny bezpiecznik - minimum 15 minut przerwy ---
    private const val MIN_COOLDOWN_MINUTES = 15

    fun scheduleNextAlarm(context: Context) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getInt("daily_goal", 2000)
        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val entry = dao.getTodayWater(todayDate)
            val currentAmount = entry?.amount ?: 0

            if (currentAmount >= dailyGoal) {
                cancelAlarm(context)
            } else {
                val dynamicInterval = calculateDynamicInterval(prefs, currentAmount, dailyGoal)
                setAlarm(context, prefs, dynamicInterval)
            }
        }
    }

    private fun calculateDynamicInterval(prefs: android.content.SharedPreferences, currentAmount: Int, dailyGoal: Int): Int {
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)
        val sleepHour = prefs.getInt("sleep_hour", 22)

        // Pobierz czas ostatniego powiadomienia (zapisywany w ReminderReceiver)
        val lastNotifTime = prefs.getLong("last_notification_time", 0L)
        val currentTime = System.currentTimeMillis()

        val remainingWater = dailyGoal - currentAmount
        val portionsLeft = remainingWater.toDouble() / quickAddAmount.toDouble()

        val calendar = Calendar.getInstance()
        val nowInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        var sleepInMinutes = sleepHour * 60
        if (sleepHour < 5) sleepInMinutes += 24 * 60

        val minutesLeftToday = sleepInMinutes - nowInMinutes

        // 1. Blokada wieczorna (60 min przed snem)
        if (minutesLeftToday < 60) return 0

        // 2. Jeśli została tylko jedna porcja
        if (portionsLeft <= 1.1) return 90.coerceAtMost(minutesLeftToday)

        // 3. Obliczenie interwału
        var calculatedInterval = (minutesLeftToday / portionsLeft).toInt()

        // --- NOWOŚĆ: Zapewnienie bezpiecznika czasowego ---
        val minutesSinceLastNotif = ((currentTime - lastNotifTime) / 60000).toInt()

        // Jeśli od ostatniego powiadomienia minęło np. 5 minut,
        // a interwał ma być 45, to musimy przesunąć alarm tak,
        // aby nastąpił najwcześniej 15 min (MIN_COOLDOWN) po poprzednim.
        if (minutesSinceLastNotif < MIN_COOLDOWN_MINUTES) {
            val waitTime = MIN_COOLDOWN_MINUTES - minutesSinceLastNotif
            calculatedInterval = calculatedInterval.coerceAtLeast(waitTime)
        }

        // 4. Sztywne granice (min 45 min dla standardu, max 180 min)
        return calculatedInterval.coerceIn(45, 180)
    }

    private fun setAlarm(context: Context, prefs: android.content.SharedPreferences, intervalMinutes: Int) {
        val wakeUpHour = prefs.getInt("wake_up_hour", 8)
        val sleepHour = prefs.getInt("sleep_hour", 22)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        var triggerAtMillis: Long = 0

        if (intervalMinutes == 0 || currentHour >= sleepHour || currentHour < wakeUpHour) {
            val nextAlarm = Calendar.getInstance()
            if (currentHour >= sleepHour) nextAlarm.add(Calendar.DAY_OF_YEAR, 1)
            nextAlarm.set(Calendar.HOUR_OF_DAY, wakeUpHour)
            nextAlarm.set(Calendar.MINUTE, 0)
            nextAlarm.set(Calendar.SECOND, 0)
            triggerAtMillis = nextAlarm.timeInMillis
        } else {
            calendar.add(Calendar.MINUTE, intervalMinutes)
            triggerAtMillis = calendar.timeInMillis
        }

        prefs.edit().putLong("next_alarm_time", triggerAtMillis).apply()

        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
        context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE).edit().remove("next_alarm_time").apply()
    }
}