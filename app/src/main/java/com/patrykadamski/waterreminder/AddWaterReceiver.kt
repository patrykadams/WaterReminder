package com.patrykadamski.waterreminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Zamknij powiadomienie (bo już kliknąłeś)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        // 2. Pobierz ustawioną ilość "szybkiej wody" z pamięci
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val amountToAdd = prefs.getInt("quick_add_amount", 250)

        // 3. Dodaj do bazy danych (w tle)
        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        // Uruchamiamy wątek w tle (IO), żeby nie zablokować telefonu
        CoroutineScope(Dispatchers.IO).launch {
            // Pobierz aktualny stan
            val currentEntry = dao.getTodayWater(todayDate)
            val currentAmount = currentEntry?.amount ?: 0

            // Oblicz nową sumę
            val newAmount = currentAmount + amountToAdd

            // Zapisz
            val entity = WaterEntity(date = todayDate, amount = newAmount)
            dao.insert(entity)

            // 4. Pokaż komunikat użytkownikowi (na głównym wątku)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Dodano $amountToAdd ml! 💧", Toast.LENGTH_SHORT).show()
            }
        }
    }
}