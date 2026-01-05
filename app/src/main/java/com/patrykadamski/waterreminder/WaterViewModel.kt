package com.patrykadamski.waterreminder

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

    var waterIntake by mutableIntStateOf(0)
        private set

    var dailyGoal by mutableIntStateOf(prefs.getInt("daily_goal", 2000))
        private set

    // NOWOŚĆ: Częstotliwość powiadomień w minutach (domyślnie 60 min)
    var alertInterval by mutableIntStateOf(prefs.getInt("alert_interval", 60))
        private set

    var records by mutableStateOf(listOf<WaterEntity>())
        private set

    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString()

    init {
        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch {
            val entry = dao.getTodayWater(todayDate)
            waterIntake = entry?.amount ?: 0
            records = dao.getLast7Days()
        }
    }

    fun addWater(amount: Int) {
        waterIntake += amount
        saveToDatabase()
    }

    fun resetWater() {
        waterIntake = 0
        saveToDatabase()
    }

    fun changeGoal(newGoal: Int) {
        dailyGoal = newGoal
        prefs.edit().putInt("daily_goal", newGoal).apply()
    }

    // NOWOŚĆ: Funkcja do zmiany częstotliwości
    fun changeInterval(newInterval: Int) {
        alertInterval = newInterval
        prefs.edit().putInt("alert_interval", newInterval).apply()
    }

    private fun saveToDatabase() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = waterIntake)
            dao.insert(entity)
            refreshData()
        }
    }
}