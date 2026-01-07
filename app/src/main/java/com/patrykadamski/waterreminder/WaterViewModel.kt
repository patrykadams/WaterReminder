package com.patrykadamski.waterreminder

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

    var waterIntake by mutableIntStateOf(0)
        private set

    var dailyGoal by mutableIntStateOf(prefs.getInt("daily_goal", 2000))
        private set

    var userGender by mutableStateOf(prefs.getString("user_gender", "M") ?: "M")
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

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private var lastDrinkTime by mutableLongStateOf(0L)
    var lastAddedAmount by mutableIntStateOf(0)
        private set

    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString()
    private var isFirstLoad = true

    init {
        viewModelScope.launch {
            dao.getTodayWaterFlow(todayDate).collectLatest { entry ->
                val newAmount = entry?.amount ?: 0

                if (isFirstLoad) {
                    waterIntake = newAmount
                    isFirstLoad = false
                    return@collectLatest
                }

                val diff = newAmount - waterIntake
                if (diff > 0) {
                    lastAddedAmount = diff
                    lastDrinkTime = System.currentTimeMillis()

                    resetMissedReminders()

                    if (waterIntake < dailyGoal && newAmount >= dailyGoal) {
                        showConfetti = true
                        launch {
                            delay(4000)
                            showConfetti = false
                        }
                    }
                }
                waterIntake = newAmount
                recalculateStreak()
            }
        }

        viewModelScope.launch {
            dao.getLast7DaysFlow().collectLatest { list ->
                records = list
            }
        }
    }

    fun addWater(amount: Int) {
        val currentTime = System.currentTimeMillis()
        if (waterIntake > 0 && currentTime - lastDrinkTime < 60_000) {
            val secondsLeft = 60 - ((currentTime - lastDrinkTime) / 1000)
            viewModelScope.launch { _toastMessage.emit("Zwolnij! Odczekaj jeszcze $secondsLeft sek. ⏳") }
            return
        }
        viewModelScope.launch {
            val currentAmount = waterIntake
            val entity = WaterEntity(date = todayDate, amount = currentAmount + amount)
            dao.insert(entity)
        }
    }

    fun undoLastAdd() {
        if (lastAddedAmount > 0) {
            val amountToSubtract = lastAddedAmount
            viewModelScope.launch {
                val newAmount = (waterIntake - amountToSubtract).coerceAtLeast(0)
                val entity = WaterEntity(date = todayDate, amount = newAmount)
                dao.insert(entity)
                _toastMessage.emit("Cofnięto dodanie $amountToSubtract ml ↩️")
                lastAddedAmount = 0
                lastDrinkTime = 0L
            }
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = 0)
            dao.insert(entity)
            lastDrinkTime = 0L
            lastAddedAmount = 0
        }
    }

    private fun resetMissedReminders() {
        prefs.edit().putInt("missed_reminders_count", 0).apply()
    }

    private fun recalculateStreak() {
        viewModelScope.launch {
            val history = dao.getAllHistory()
            var currentStreak = 0
            var checkDate = LocalDate.now()
            val todayEntry = history.find { it.date == checkDate.toString() }
            if (todayEntry != null && todayEntry.amount >= dailyGoal) currentStreak++
            while (true) {
                checkDate = checkDate.minusDays(1)
                val entry = history.find { it.date == checkDate.toString() }
                if (entry != null && entry.amount >= dailyGoal) currentStreak++ else break
            }
            streakDays = currentStreak
        }
    }

    // --- ZMIANA: Automatyczne obliczanie interwału ---
    // Usunęliśmy argument "newInterval", bo teraz liczymy go sami
    fun saveSettings(newGoal: Int, newWeight: Int, newQuickAdd: Int, newWakeUp: Int, newSleep: Int, newGender: String) {
        dailyGoal = newGoal
        userWeight = newWeight
        quickAddAmount = newQuickAdd
        wakeUpHour = newWakeUp
        sleepHour = newSleep
        userGender = newGender

        // 1. Obliczamy czas aktywności w minutach
        var activeHours = newSleep - newWakeUp
        if (activeHours < 0) activeHours += 24 // Obsługa przypadku gdy ktoś idzie spać po północy (np. wstaje 10, spać 2)
        val activeMinutes = activeHours * 60

        // 2. Obliczamy ile porcji trzeba wypić
        // (np. Cel 2000 / Porcja 250 = 8 porcji)
        val portionsNeeded = if (newQuickAdd > 0) newGoal.toFloat() / newQuickAdd.toFloat() else 1f

        // 3. Obliczamy interwał (Czas / Porcje)
        // Np. 960 minut / 8 porcji = 120 minut (co 2h)
        // Zabezpieczamy się przed zerem i ustawiamy minimum 30 min, żeby nie spamować
        val calculatedInterval = (activeMinutes / portionsNeeded).toInt().coerceAtLeast(30)

        alertInterval = calculatedInterval

        prefs.edit()
            .putInt("daily_goal", newGoal)
            .putInt("alert_interval", calculatedInterval) // Zapisujemy wyliczony czas
            .putInt("user_weight", newWeight)
            .putInt("quick_add_amount", newQuickAdd)
            .putInt("wake_up_hour", newWakeUp)
            .putInt("sleep_hour", newSleep)
            .putString("user_gender", newGender)
            .apply()

        recalculateStreak()
    }

    fun refreshData() { }
}