package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    fun scheduleNextAlarm(context: Context) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getInt("daily_goal", 2000)

        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val entry = dao.getTodayWater(todayDate)
            val currentAmount = entry?.amount ?: 0

            // Traktujemy pierwszy (główny) rozmiar naczynia jako "typową porcję" do wyliczeń.
            val quickAddAmount = VesselSizePrefs.load(prefs).first().amountMl
            val wakeUpHour = prefs.getInt("wake_up_hour", 8)
            val sleepHour = prefs.getInt("sleep_hour", 22)
            val frequency = prefs.getString("reminder_frequency", ReminderPacing.FREQUENCY_NORMAL)
                ?: ReminderPacing.FREQUENCY_NORMAL

            val baseInterval = ReminderPacing.baseIntervalMinutes(
                wakeUpHour = wakeUpHour,
                sleepHour = sleepHour,
                dailyGoal = dailyGoal,
                quickAddAmount = quickAddAmount,
                frequency = frequency
            )

            val calendar = Calendar.getInstance()
            val nowMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            // CHANGE: We ALWAYS calculate an alarm.
            // If goal is met, 'nextIntervalMinutes' returns 0, which we handle as "Schedule for Tomorrow".
            val dynamicInterval = ReminderPacing.nextIntervalMinutes(
                nowMinuteOfDay = nowMinuteOfDay,
                wakeUpHour = wakeUpHour,
                sleepHour = sleepHour,
                dailyGoal = dailyGoal,
                currentAmount = currentAmount,
                quickAddAmount = quickAddAmount,
                baseInterval = baseInterval
            )

            setAlarm(context, prefs, dynamicInterval)

            // Update UI Toast
            if (currentAmount >= dailyGoal) {
                showToast(context, "Cel osiągnięty! Widzimy się jutro rano ☀️")
            } else {
                showToast(context, "Następne przypomnienie za $dynamicInterval min ⏳")
            }
        }
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

        val plannedCalendar = Calendar.getInstance()
        plannedCalendar.add(Calendar.MINUTE, intervalMinutes)
        val plannedHour = plannedCalendar.get(Calendar.HOUR_OF_DAY)

        val isNightNow = (currentHour >= sleepHour || currentHour < wakeUpHour)
        val willBeNight = (plannedHour >= sleepHour || plannedHour < wakeUpHour)
        val isGoalMet = (intervalMinutes == 0) // Treat 0 interval as "Goal Met / Day Over"

        var triggerAtMillis: Long = 0

        if (isNightNow || willBeNight || isGoalMet) {
            // --- MORNING LOGIC ---
            val nextAlarm = Calendar.getInstance()

            // 1. Reset to base wake-up time
            nextAlarm.set(Calendar.HOUR_OF_DAY, wakeUpHour)
            nextAlarm.set(Calendar.MINUTE, 0)
            nextAlarm.set(Calendar.SECOND, 0)
            nextAlarm.set(Calendar.MILLISECOND, 0)

            // 2. If that time is in the past (e.g., it's 2 PM now), add 1 day
            if (System.currentTimeMillis() > nextAlarm.timeInMillis) {
                nextAlarm.add(Calendar.DAY_OF_YEAR, 1)
            }

            // 3. APPLY DELAY: 90 minutes after wake-up
            nextAlarm.add(Calendar.MINUTE, 90)

            triggerAtMillis = nextAlarm.timeInMillis
        } else {
            // --- STANDARD LOGIC ---
            calendar.add(Calendar.MINUTE, intervalMinutes)
            triggerAtMillis = calendar.timeInMillis
        }

        prefs.edit().putLong("next_alarm_time", triggerAtMillis).apply()

        if (hasExactAlarmPermission(context)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm permission revoked mid-flight, falling back to inexact alarm", e)
            }
        }

        // No exact-alarm permission (or it was just revoked): fall back to an inexact
        // alarm so reminders keep firing (roughly on time) instead of silently stopping.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Removed cancelAlarm() as we never want to completely kill the cycle anymore.

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}