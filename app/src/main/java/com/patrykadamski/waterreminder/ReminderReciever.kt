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

    // FIXED: Constant ID to prevent notifications from stacking
    companion object {
        const val NOTIFICATION_ID = 777
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getInt("daily_goal", 2000)

        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val entry = dao.getTodayWater(todayDate)
            val currentAmount = entry?.amount ?: 0

            if (currentAmount >= dailyGoal) {
                AlarmScheduler.scheduleNextAlarm(context)
                return@launch
            }

            launchNotification(context, prefs, currentAmount)
        }
    }

    private fun launchNotification(context: Context, prefs: android.content.SharedPreferences, currentAmount: Int) {
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)

        val title = context.getString(R.string.water_reminder_notification_title)
        val text = context.resources.getStringArray(R.array.water_reminder_messages).random()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "WaterReminder:WakeUpScreen")
            wakeLock.acquire(3000)
        }

        // Intents
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(context, NOTIFICATION_ID, openAppIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)

        val addWaterIntent = Intent(context, AddWaterReceiver::class.java)
        val addWaterPendingIntent = android.app.PendingIntent.getBroadcast(context, NOTIFICATION_ID, addWaterIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)

        val remoteInput = RemoteInput.Builder("key_custom_amount").setLabel("Ile wypiłeś?").build()
        val customWaterIntent = Intent(context, AddCustomWaterReceiver::class.java)
        val customWaterPendingIntent = android.app.PendingIntent.getBroadcast(context, NOTIFICATION_ID, customWaterIntent, android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)

        val customAction = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_edit, "Inna ilość", customWaterPendingIntent).addRemoteInput(remoteInput).build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notification = NotificationCompat.Builder(context, WaterReminderApp.CHANNEL_ID)
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
            .setGroup("WATER_REMINDER_GROUP") // Optional now, since we overwrite
            .build()

        // FIXED: Use the constant ID
        notificationManager.notify(NOTIFICATION_ID, notification)

        AlarmScheduler.scheduleNextAlarm(context)
    }
}