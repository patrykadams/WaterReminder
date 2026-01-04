package com.patrykadamski.waterreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf // Nowy import
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    var waterIntake by mutableIntStateOf(0)
        private set

    // NOWE: Lista z historią do wyświetlenia na ekranie
    var records by mutableStateOf(listOf<WaterEntity>())
        private set

    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString()

    init {
        refreshData()
    }

    // Połączyliśmy wczytywanie dzisiejszej wody i historii w jedną funkcję
    private fun refreshData() {
        viewModelScope.launch {
            // 1. Pobierz dzisiejszy wynik
            val entry = dao.getTodayWater(todayDate)
            waterIntake = entry?.amount ?: 0

            // 2. Pobierz historię
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

    private fun saveToDatabase() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = waterIntake)
            dao.insert(entity)
            refreshData() // Odśwież listę po zapisaniu
        }
    }
}