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

    var alertInterval by mutableIntStateOf(prefs.getInt("alert_interval", 60))
        private set

    var userWeight by mutableIntStateOf(prefs.getInt("user_weight", 70))
        private set

    var quickAddAmount by mutableIntStateOf(prefs.getInt("quick_add_amount", 250))
        private set

    // NOWOŚĆ: Godziny ciszy
    var wakeUpHour by mutableIntStateOf(prefs.getInt("wake_up_hour", 8)) // Domyślnie 8:00
        private set
    var sleepHour by mutableIntStateOf(prefs.getInt("sleep_hour", 22))   // Domyślnie 22:00
        private set

    var records by mutableStateOf(listOf<WaterEntity>())
        private set

    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString()

    init {
        refreshData()
    }

    fun refreshData() {
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

    // Aktualizacja: Zapisujemy teraz też godziny snu
    fun saveSettings(newGoal: Int, newInterval: Int, newWeight: Int, newQuickAdd: Int, newWakeUp: Int, newSleep: Int) {
        dailyGoal = newGoal
        alertInterval = newInterval
        userWeight = newWeight
        quickAddAmount = newQuickAdd
        wakeUpHour = newWakeUp
        sleepHour = newSleep

        prefs.edit()
            .putInt("daily_goal", newGoal)
            .putInt("alert_interval", newInterval)
            .putInt("user_weight", newWeight)
            .putInt("quick_add_amount", newQuickAdd)
            .putInt("wake_up_hour", newWakeUp)
            .putInt("sleep_hour", newSleep)
            .apply()
    }

    private fun saveToDatabase() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = waterIntake)
            dao.insert(entity)
            refreshData()
        }
    }
}