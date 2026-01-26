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

class AddCustomWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val inputString = remoteInput.getCharSequence("key_custom_amount").toString()
            val amountToAdd = inputString.toIntOrNull()

            if (amountToAdd != null && amountToAdd > 0) {
                addToDatabase(context, amountToAdd)
            } else {
                Toast.makeText(context, "Wpisz poprawną liczbę!", Toast.LENGTH_SHORT).show()
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancelAll()
    }

    private fun addToDatabase(context: Context, amount: Int) {
        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val currentEntry = dao.getTodayWater(todayDate)
            val currentAmount = currentEntry?.amount ?: 0
            val newAmount = currentAmount + amount

            val entity = WaterEntity(date = todayDate, amount = newAmount)
            dao.insert(entity)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Dodano $amount ml! 💧", Toast.LENGTH_SHORT).show()
            }

            // Resetujemy zegar (Inteligentny Interwał)
            AlarmScheduler.scheduleNextAlarm(context)
        }
    }
}