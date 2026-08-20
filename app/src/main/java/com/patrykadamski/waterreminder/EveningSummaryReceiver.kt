package com.patrykadamski.waterreminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Separate from ReminderReceiver on purpose: a once-a-day, neutral summary
 * of today's intake vs. goal and the current streak - never a nag, never
 * shown as a "you failed" message even if the goal wasn't reached.
 */
class EveningSummaryReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = 778
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getInt("daily_goal", 2000)
        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val todayAmount = dao.getTodayWater(todayDate)?.amount ?: 0
            val history = dao.getAllHistory()
            val streak = StreakCalculator.calculate(history, dailyGoal)

            showNotification(context, todayAmount, dailyGoal, streak)

            // Always reschedules for tomorrow - an independent daily cycle,
            // unrelated to the daytime reminder's own alarm.
            EveningSummaryScheduler.scheduleNext(context)
        }
    }

    private fun showNotification(context: Context, todayAmount: Int, dailyGoal: Int, streak: Int) {
        val text = "Dziś: $todayAmount/$dailyGoal ml. Streak: $streak ${StreakCalculator.dayWord(streak)}."

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, WaterReminderApp.EVENING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Podsumowanie dnia 🌙")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
