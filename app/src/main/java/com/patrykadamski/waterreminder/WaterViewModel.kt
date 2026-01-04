package com.patrykadamski.waterreminder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    // Stan licznika widoczny dla ekranu
    var waterIntake by mutableIntStateOf(0)
        private set

    // Dostęp do bazy danych
    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString() // Np. "2023-10-27"

    init {
        // AUTOMATYCZNIE przy starcie: Wczytaj wynik z dzisiaj
        loadTodayWater()
    }

    private fun loadTodayWater() {
        viewModelScope.launch {
            val entry = dao.getTodayWater(todayDate)
            if (entry != null) {
                waterIntake = entry.amount
            } else {
                waterIntake = 0
            }
        }
    }

    fun addWater(amount: Int) {
        // 1. Zmień na ekranie
        waterIntake += amount
        // 2. Zapisz w bazie
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
        }
    }
}