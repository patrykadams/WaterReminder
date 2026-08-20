package com.patrykadamski.waterreminder

// --- TE IMPORTY SĄ KLUCZOWE ---
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
// ------------------------------

class AddWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val amountToAdd = VesselSizePrefs.load(prefs).first().amountMl

        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val currentEntry = dao.getTodayWater(todayDate)
            val currentAmount = currentEntry?.amount ?: 0
            val newAmount = currentAmount + amountToAdd

            val entity = WaterEntity(date = todayDate, amount = newAmount)
            dao.insert(entity)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Dodano $amountToAdd ml! 💧", Toast.LENGTH_SHORT).show()
            }

            // Czyścimy powiadomienia
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancelAll()

            // Resetujemy zegar powiadomień
            AlarmScheduler.scheduleNextAlarm(context)
        }
    }
}