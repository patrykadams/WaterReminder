package com.patrykadamski.waterreminder

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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

    var wakeUpHour by mutableIntStateOf(prefs.getInt("wake_up_hour", 8))
        private set
    var sleepHour by mutableIntStateOf(prefs.getInt("sleep_hour", 22))
        private set

    var records by mutableStateOf(listOf<WaterEntity>())
        private set

    var streakDays by mutableIntStateOf(0)
        private set

    var showConfetti by mutableStateOf(false)
        private set

    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString()

    init {
        // --- KLUCZOWA ZMIANA: NASŁUCHIWANIE NA ŻYWO ---

        // 1. Nasłuchuj dzisiejszej wody
        viewModelScope.launch {
            dao.getTodayWaterFlow(todayDate).collectLatest { entry ->
                val newAmount = entry?.amount ?: 0

                // Sprawdź czy odpalić konfetti (jeśli przyszło info z bazy, że cel osiągnięty)
                if (waterIntake < dailyGoal && newAmount >= dailyGoal) {
                    showConfetti = true
                    launch {
                        delay(4000)
                        showConfetti = false
                    }
                }
                waterIntake = newAmount

                // Przy każdej zmianie przelicz Streak
                recalculateStreak()
            }
        }

        // 2. Nasłuchuj historii (do wykresu)
        viewModelScope.launch {
            dao.getLast7DaysFlow().collectLatest { list ->
                records = list
            }
        }
    }

    // Funkcja pomocnicza do ręcznego dodawania z poziomu aplikacji
    fun addWater(amount: Int) {
        // Tu tylko zapisujemy. UI samo się odświeży dzięki Flow powyżej.
        viewModelScope.launch {
            val currentAmount = waterIntake // Używamy aktualnego stanu
            val entity = WaterEntity(date = todayDate, amount = currentAmount + amount)
            dao.insert(entity)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = 0)
            dao.insert(entity)
        }
    }

    // Przeliczanie Streaka (wywoływane automatycznie przy zmianie danych)
    private fun recalculateStreak() {
        viewModelScope.launch {
            val history = dao.getAllHistory()

            var currentStreak = 0
            var checkDate = LocalDate.now()

            // Sprawdź dzisiaj
            val todayEntry = history.find { it.date == checkDate.toString() }
            if (todayEntry != null && todayEntry.amount >= dailyGoal) {
                currentStreak++
            }

            // Sprawdź dni wstecz
            while (true) {
                checkDate = checkDate.minusDays(1)
                val entry = history.find { it.date == checkDate.toString() }
                if (entry != null && entry.amount >= dailyGoal) {
                    currentStreak++
                } else {
                    break
                }
            }
            streakDays = currentStreak
        }
    }

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

        // Wymuś odświeżenie streaka (bo zmienił się cel)
        recalculateStreak()
    }

    // Funkcja refreshData nie jest już potrzebna w starej formie,
    // bo Flow robi to automatycznie, ale zostawiamy pustą, żeby nie psuć kodu w WaterScreen
    fun refreshData() { }
}