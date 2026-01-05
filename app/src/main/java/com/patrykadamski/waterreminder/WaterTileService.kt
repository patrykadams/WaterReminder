package com.patrykadamski.waterreminder

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class WaterTileService : TileService() {

    // Ta funkcja wywołuje się, gdy rozwijasz belkę powiadomień
    override fun onStartListening() {
        super.onStartListening()
        // Ustawiamy kafelek jako "Aktywny" (świeci się na biało/niebiesko)
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.label = "Dodaj Wodę" // Podpis pod kafelkiem
        qsTile.updateTile()
    }

    // Ta funkcja wywołuje się po kliknięciu
    override fun onClick() {
        super.onClick()

        // 1. Pobieramy ile wody dodać (z ustawień, domyślnie 250ml)
        val prefs = getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val amountToAdd = prefs.getInt("quick_add_amount", 250)

        // 2. Dodajemy do bazy danych w tle
        val dao = WaterDatabase.getDatabase(applicationContext).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val currentEntry = dao.getTodayWater(todayDate)
            val currentAmount = currentEntry?.amount ?: 0
            val newAmount = currentAmount + amountToAdd

            val entity = WaterEntity(date = todayDate, amount = newAmount)
            dao.insert(entity)

            // 3. Pokaż dymek potwierdzenia (na głównym wątku)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, "Dodano $amountToAdd ml! 💧", Toast.LENGTH_SHORT).show()
            }
        }
    }
}