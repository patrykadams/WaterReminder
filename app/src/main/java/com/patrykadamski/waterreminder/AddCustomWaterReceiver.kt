package com.patrykadamski.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles custom text input from the notification shade.
 *
 * Ensures database consistency by querying the existing record ID before
 * saving the new total amount.
 */
class AddCustomWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val inputString = remoteInput.getCharSequence("key_custom_amount").toString()
            val amountToAdd = inputString.toIntOrNull()

            if (amountToAdd != null && amountToAdd > 0) {
                addToDatabase(context, amountToAdd)
            } else {
                Toast.makeText(context, "Please enter a valid number!", Toast.LENGTH_SHORT).show()
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancelAll()
    }

    private fun addToDatabase(context: Context, amount: Int) {
        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Retrieve existing ID
            val currentEntry = dao.getTodayWater(todayDate)
            val currentAmount = currentEntry?.amount ?: 0
            val idToUse = currentEntry?.id ?: 0

            val newAmount = currentAmount + amount

            // 2. Save with ID to overwrite
            val entity = WaterEntity(id = idToUse, date = todayDate, amount = newAmount)
            dao.insert(entity)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Added $amount ml! 💧", Toast.LENGTH_SHORT).show()
            }

            AlarmScheduler.scheduleNextAlarm(context)
        }
    }
}