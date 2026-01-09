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
import java.util.Calendar

/**
 * ViewModel responsible for managing the application state, business logic,
 * and database interactions. It survives configuration changes.
 */
class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
    private val dao = WaterDatabase.getDatabase(application).waterDao()
    private val todayDate = LocalDate.now().toString()
    private var isFirstLoad = true

    // --- State Variables (Compose observable state) ---

    var waterIntake by mutableIntStateOf(0)
        private set

    var dailyGoal by mutableIntStateOf(prefs.getInt("daily_goal", 2000))
        private set

    var userGender by mutableStateOf(prefs.getString("user_gender", "M") ?: "M")
        private set

    var userActivity by mutableStateOf(prefs.getString("user_activity", "NONE") ?: "NONE")
        private set

    var alertInterval by mutableIntStateOf(prefs.getInt("alert_interval", 60))
        private set

    var userWeight by mutableIntStateOf(prefs.getInt("user_weight", 70))
        private set

    var quickAddAmount by mutableIntStateOf(prefs.getInt("quick_add_amount", 250))
        private set

    // Total minutes from the start of the day (e.g., 8:00 = 480)
    var wakeUpTotalMinutes by mutableIntStateOf(prefs.getInt("wake_up_total", 8 * 60))
        private set
    var sleepTotalMinutes by mutableIntStateOf(prefs.getInt("sleep_total", 22 * 60))
        private set

    var records by mutableStateOf(listOf<WaterEntity>())
        private set

    var streakDays by mutableIntStateOf(0)
        private set

    var showConfetti by mutableStateOf(false)
        private set

    var nextAlarmTime by mutableStateOf("")
        private set

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private var lastDrinkTime by mutableLongStateOf(0L)
    var lastAddedAmount by mutableIntStateOf(0)
        private set

    init {
        // Observe today's water intake changes in real-time
        viewModelScope.launch {
            dao.getTodayWaterFlow(todayDate).collectLatest { entry ->
                val newAmount = entry?.amount ?: 0

                if (isFirstLoad) {
                    waterIntake = newAmount
                    isFirstLoad = false
                    updateNextAlarmDisplay()
                    return@collectLatest
                }

                // Detect if water was added to trigger confetti or reset logic
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

                // Allow some time for Scheduler to save the new alarm time before UI update
                delay(500)
                updateNextAlarmDisplay()
            }
        }

        // Load 7-day history for the chart
        viewModelScope.launch {
            dao.getLast7DaysFlow().collectLatest { list ->
                records = list
            }
        }
    }

    /**
     * Adds a specific amount of water to the database.
     * Prevents spamming adds (1-minute cooldown).
     */
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
            AlarmScheduler.scheduleNextAlarm(getApplication())
            delay(100)
            updateNextAlarmDisplay()
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

                AlarmScheduler.scheduleNextAlarm(getApplication())
                delay(100)
                updateNextAlarmDisplay()
            }
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            val entity = WaterEntity(date = todayDate, amount = 0)
            dao.insert(entity)
            lastDrinkTime = 0L
            lastAddedAmount = 0

            AlarmScheduler.scheduleNextAlarm(getApplication())
            delay(100)
            updateNextAlarmDisplay()
        }
    }

    /**
     * Reads the next scheduled alarm time from SharedPreferences to display on UI.
     */
    fun updateNextAlarmDisplay() {
        val nextTime = prefs.getLong("next_alarm_time", 0L)
        if (nextTime > System.currentTimeMillis()) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = nextTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            nextAlarmTime = String.format("%02d:%02d", hour, minute)
        } else {
            nextAlarmTime = ""
        }
    }

    fun formatTime(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return String.format("%02d:%02d", h, m)
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

    /**
     * Persists user settings and recalculates the alarm schedule.
     */
    fun saveSettings(newGoal: Int, newWeight: Int, newQuickAdd: Int, wakeTotal: Int, sleepTotal: Int, newGender: String, newActivity: String) {
        dailyGoal = newGoal
        userWeight = newWeight
        quickAddAmount = newQuickAdd
        wakeUpTotalMinutes = wakeTotal
        sleepTotalMinutes = sleepTotal
        userGender = newGender
        userActivity = newActivity

        prefs.edit()
            .putInt("daily_goal", newGoal)
            .putInt("user_weight", newWeight)
            .putInt("quick_add_amount", newQuickAdd)
            .putInt("wake_up_total", wakeTotal)
            .putInt("sleep_total", sleepTotal)
            .putString("user_gender", newGender)
            .putString("user_activity", newActivity)
            .apply()

        AlarmScheduler.scheduleNextAlarm(getApplication())
        viewModelScope.launch {
            delay(200)
            updateNextAlarmDisplay()
        }
        recalculateStreak()
    }

    fun refreshData() {
        updateNextAlarmDisplay()
    }
}