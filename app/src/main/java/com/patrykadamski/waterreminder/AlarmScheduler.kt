package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

/**
 * Singleton object responsible for calculating dynamic intervals
 * and scheduling AlarmManager events.
 */
object AlarmScheduler {

    // Minimum time (in minutes) between notifications to prevent spam.
    private const val MIN_COOLDOWN_MINUTES = 15

    /**
     * Checks current progress and schedules (or cancels) the next alarm.
     */
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

    /**
     * Calculates the optimal time interval for the next notification.
     * Factors: Remaining water, remaining time in day, cooldowns, evening constraints.
     */
    private fun calculateDynamicInterval(prefs: android.content.SharedPreferences, currentAmount: Int, dailyGoal: Int): Int {
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)
        // Retrieve sleep time in total minutes (e.g., 22:30 = 1350)
        val sleepTimeTotal = prefs.getInt("sleep_total", 22 * 60)

        val lastNotifTime = prefs.getLong("last_notification_time", 0L)
        val currentTime = System.currentTimeMillis()

        val remainingWater = dailyGoal - currentAmount
        val portionsLeft = remainingWater.toDouble() / quickAddAmount.toDouble()

        val calendar = Calendar.getInstance()
        val nowInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        var sleepInMinutes = sleepTimeTotal
        // Handle midnight crossover (e.g., sleep at 01:00 AM)
        if (sleepInMinutes <= nowInMinutes && sleepInMinutes < 300) {
            sleepInMinutes += 24 * 60
        } else if (sleepInMinutes < nowInMinutes) {
            // Already past bedtime
            return 0
        }

        val minutesLeftToday = sleepInMinutes - nowInMinutes

        // 1. Evening Block: Silence notifications 60 min before sleep
        if (minutesLeftToday < 60) return 0

        // 2. Last Glass Logic: Relaxed interval if only 1 portion remains
        if (portionsLeft <= 1.1) return 90.coerceAtMost(minutesLeftToday)

        // 3. Mathematical calculation: Time / Portions
        var calculatedInterval = (minutesLeftToday / portionsLeft).toInt()

        // 4. Cooldown Safety: Ensure MIN_COOLDOWN_MINUTES passed since last notification
        val minutesSinceLastNotif = ((currentTime - lastNotifTime) / 60000).toInt()

        if (minutesSinceLastNotif < MIN_COOLDOWN_MINUTES) {
            val waitTime = MIN_COOLDOWN_MINUTES - minutesSinceLastNotif
            calculatedInterval = calculatedInterval.coerceAtLeast(waitTime)
        }

        // 5. Hard Limits: Min 45m (anti-spam), Max 180m (don't stay silent too long)
        return calculatedInterval.coerceIn(45, 180)
    }

    private fun setAlarm(context: Context, prefs: android.content.SharedPreferences, intervalMinutes: Int) {
        val wakeUpTotal = prefs.getInt("wake_up_total", 8 * 60)
        val sleepTotal = prefs.getInt("sleep_total", 22 * 60)

        val wakeUpHour = wakeUpTotal / 60
        val wakeUpMinute = wakeUpTotal % 60

        val sleepHour = sleepTotal / 60

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        var triggerAtMillis: Long = 0

        // Schedule for tomorrow morning if it's night or interval is 0 (evening block)
        val isNight = (currentHour >= sleepHour && sleepHour > 4) || (currentHour < wakeUpHour)

        if (intervalMinutes == 0 || isNight) {
            val nextAlarm = Calendar.getInstance()
            if (currentHour >= sleepHour && sleepHour > 4) {
                nextAlarm.add(Calendar.DAY_OF_YEAR, 1)
            }
            nextAlarm.set(Calendar.HOUR_OF_DAY, wakeUpHour)
            nextAlarm.set(Calendar.MINUTE, wakeUpMinute)
            nextAlarm.set(Calendar.SECOND, 0)
            triggerAtMillis = nextAlarm.timeInMillis
        } else {
            calendar.add(Calendar.MINUTE, intervalMinutes)
            triggerAtMillis = calendar.timeInMillis
        }

        // Save time for UI display
        prefs.edit().putLong("next_alarm_time", triggerAtMillis).apply()

        try {
            // allowWhileIdle ensures alarm fires even in Doze mode
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