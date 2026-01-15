package com.patrykadamski.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles the "Quick Add" action directly from the notification.
 *
 * Implements a check-then-update logic:
 * 1. Queries the database for the existing entry for today.
 * 2. Retrieves the ID of that entry.
 * 3. Performs an update using that ID to prevent duplicate rows.
 */
class AddWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val amountToAdd = prefs.getInt("quick_add_amount", 250)

        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Fetch current entry to get the ID
            val currentEntry = dao.getTodayWater(todayDate)

            val currentAmount = currentEntry?.amount ?: 0
            val idToUse = currentEntry?.id ?: 0 // Use 0 only if no entry exists (creates new)

            val newAmount = currentAmount + amountToAdd

            // 2. Insert with the correct ID (triggers REPLACE strategy in DAO)
            val entity = WaterEntity(id = idToUse, date = todayDate, amount = newAmount)
            dao.insert(entity)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Added $amountToAdd ml! 💧", Toast.LENGTH_SHORT).show()
            }

            // Dismiss notifications
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancelAll()

            // Reschedule next alarm
            AlarmScheduler.scheduleNextAlarm(context)
        }
    }
}