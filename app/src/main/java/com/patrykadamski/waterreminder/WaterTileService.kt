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

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.label = "Dodaj Wodę"
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val amountToAdd = prefs.getInt("quick_add_amount", 250)

        val dao = WaterDatabase.getDatabase(applicationContext).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val currentEntry = dao.getTodayWater(todayDate)
            val currentAmount = currentEntry?.amount ?: 0
            val newAmount = currentAmount + amountToAdd

            val entity = WaterEntity(date = todayDate, amount = newAmount)
            dao.insert(entity)

            // FIXED: Trigger Smart Pacing Algorithm recalculation
            AlarmScheduler.scheduleNextAlarm(applicationContext)

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, "Dodano $amountToAdd ml! 💧", Toast.LENGTH_SHORT).show()
            }
        }
    }
}