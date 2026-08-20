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

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

    var waterIntake by mutableIntStateOf(0)
        private set

    var dailyGoal by mutableIntStateOf(prefs.getInt("daily_goal", 2000))
        private set

    var userGender by mutableStateOf(prefs.getString("user_gender", "M") ?: "M")
        private set

    // NOWOŚĆ: Poziom aktywności (NONE, LOW, MEDIUM, HIGH)
    var userActivity by mutableStateOf(prefs.getString("user_activity", "NONE") ?: "NONE")
        private set

    var alertInterval by mutableIntStateOf(prefs.getInt("alert_interval", 60))
        private set

    var userWeight by mutableIntStateOf(prefs.getInt("user_weight", 70))
        private set

    var vesselSizes by mutableStateOf(VesselSizePrefs.load(prefs))
        private set

    var wakeUpHour by mutableIntStateOf(prefs.getInt("wake_up_hour", 8))
        private set
    var sleepHour by mutableIntStateOf(prefs.getInt("sleep_hour", 22))
        private set

    var reminderFrequency by mutableStateOf(
        prefs.getString("reminder_frequency", ReminderPacing.FREQUENCY_NORMAL) ?: ReminderPacing.FREQUENCY_NORMAL
    )
        private set

    var records by mutableStateOf(listOf<WaterEntity>())
        private set

    var streakDays by mutableIntStateOf(0)
        private set

    var showConfetti by mutableStateOf(false)
        private set

    var nextAlarmTime by mutableStateOf("")
        private set

    // null = jeszcze nie ustalono; true/false = decyzja podjęta (patrz init)
    var showOnboarding by mutableStateOf<Boolean?>(null)
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
                    updateNextAlarmDisplay()
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

                delay(500)
                updateNextAlarmDisplay()
            }
        }

        viewModelScope.launch {
            dao.getLast7DaysFlow().collectLatest { list ->
                records = list
            }
        }

        viewModelScope.launch {
            val alreadyOnboarded = prefs.getBoolean("onboarding_completed", false)
            // "daily_goal" already in prefs, or any past water history, both mean this is
            // an existing user (e.g. updating the app) rather than a genuine first launch -
            // never show onboarding to them or touch their goal automatically.
            val hasPriorUsage = prefs.contains("daily_goal") || dao.getAllHistory().isNotEmpty()
            if (!alreadyOnboarded && hasPriorUsage) {
                prefs.edit().putBoolean("onboarding_completed", true).apply()
            }
            showOnboarding = !alreadyOnboarded && !hasPriorUsage
        }

        // One-time bootstrap so users updating from a version without the evening
        // summary get it scheduled too, without needing to touch settings or reboot.
        // Independent of the daytime reminder's own scheduling below.
        if (!prefs.contains("evening_summary_alarm_time")) {
            EveningSummaryScheduler.scheduleNext(getApplication())
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

    private fun resetMissedReminders() {
        prefs.edit().putInt("missed_reminders_count", 0).apply()
    }

    private fun recalculateStreak() {
        viewModelScope.launch {
            val history = dao.getAllHistory()
            streakDays = StreakCalculator.calculate(history, dailyGoal)
        }
    }

    // --- ZAKTUALIZOWANA FUNKCJA: Zapisuje też Activity, częstotliwość przypomnień i rozmiary naczyń ---
    fun saveSettings(
        newGoal: Int,
        newWeight: Int,
        newWakeUp: Int,
        newSleep: Int,
        newGender: String,
        newActivity: String,
        newFrequency: String = reminderFrequency,
        newVesselSizes: List<VesselSize> = vesselSizes
    ) {
        // Zawsze musi zostać co najmniej jeden rozmiar - inaczej nie byłoby czym
        // logować wody z głównego ekranu ani z powiadomienia.
        val sanitizedVesselSizes = newVesselSizes.ifEmpty { VesselSizePrefs.DEFAULT_SIZES }
        val primaryAmount = sanitizedVesselSizes.first().amountMl

        dailyGoal = newGoal
        userWeight = newWeight
        wakeUpHour = newWakeUp
        sleepHour = newSleep
        userGender = newGender
        userActivity = newActivity // Zapiszemy to
        reminderFrequency = newFrequency
        vesselSizes = sanitizedVesselSizes

        // Bazowy interwał liczony tą samą funkcją co realny harmonogram w AlarmScheduler,
        // żeby podgląd w UI nigdy się z nim nie rozjechał. Jako "typowej porcji" używamy
        // pierwszego (głównego) rozmiaru naczynia z listy.
        val calculatedInterval = ReminderPacing.baseIntervalMinutes(
            wakeUpHour = newWakeUp,
            sleepHour = newSleep,
            dailyGoal = newGoal,
            quickAddAmount = primaryAmount,
            frequency = newFrequency
        )

        alertInterval = calculatedInterval

        VesselSizePrefs.save(prefs, sanitizedVesselSizes)
        prefs.edit()
            .putInt("daily_goal", newGoal)
            .putInt("alert_interval", calculatedInterval)
            .putInt("user_weight", newWeight)
            .putInt("wake_up_hour", newWakeUp)
            .putInt("sleep_hour", newSleep)
            .putString("user_gender", newGender)
            .putString("user_activity", newActivity) // Zapisujemy aktywność
            .putString("reminder_frequency", newFrequency)
            .apply()

        AlarmScheduler.scheduleNextAlarm(getApplication())
        EveningSummaryScheduler.scheduleNext(getApplication()) // sleep hour may have changed
        viewModelScope.launch {
            delay(200)
            updateNextAlarmDisplay()
        }
        recalculateStreak()
    }

    fun refreshData() {
        updateNextAlarmDisplay()
    }

    fun completeOnboarding(weightKg: Int, activityLevel: String, goal: Int) {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        saveSettings(
            newGoal = goal,
            newWeight = weightKg,
            newWakeUp = wakeUpHour,
            newSleep = sleepHour,
            newGender = userGender,
            newActivity = activityLevel
        )
        showOnboarding = false
    }
}