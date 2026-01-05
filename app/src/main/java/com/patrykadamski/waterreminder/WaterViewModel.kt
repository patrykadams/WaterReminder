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

    // 1. "Notatnik" do zapisywania celu (SharedPreferences)
    private val prefs = application.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

    // 2. Stan licznika wody
    var waterIntake by mutableIntStateOf(0)
        private set

    // 3. Stan Celu (Wczytaj z notatnika, a jak pusto to domyślnie 2000)
    var dailyGoal by mutableIntStateOf(prefs.getInt("daily_goal", 2000))
        private set

    // 4. Historia
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

    // --- NOWA FUNKCJA: Zmień cel i zapisz go na stałe ---
    fun changeGoal(newGoal: Int) {
        dailyGoal = newGoal
        prefs.edit().putInt("daily_goal", newGoal).apply()
    }

    private fun saveToDatabase() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = waterIntake)
            dao.insert(entity)
            refreshData()
        }
    }
}