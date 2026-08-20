package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

/**
 * Schedules the once-a-day evening summary notification, roughly an hour
 * before the user's bedtime. This is a fully independent daily cycle from
 * AlarmScheduler's daytime reminder pacing: its own PendingIntent (distinct
 * request code and target receiver), its own prefs key, and it is never
 * touched by water-logging events - only by a reboot, a settings change to
 * wake/sleep hour, or by itself after firing.
 */
object EveningSummaryScheduler {
    private const val TAG = "EveningSummaryScheduler"
    private const val REQUEST_CODE = 1001
    private const val MINUTES_BEFORE_BEDTIME = 60

    fun scheduleNext(context: Context) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val sleepHour = prefs.getInt("sleep_hour", 22)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EveningSummaryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, sleepHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -MINUTES_BEFORE_BEDTIME)
        }
        if (trigger.timeInMillis <= System.currentTimeMillis()) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }

        prefs.edit().putLong("evening_summary_alarm_time", trigger.timeInMillis).apply()

        if (AlarmScheduler.hasExactAlarmPermission(context)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm permission revoked mid-flight, falling back to inexact alarm", e)
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
    }
}
