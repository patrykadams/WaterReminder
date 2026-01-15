package com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmLog"

    // Stałe konfiguracyjne - minimalny i maksymalny odstęp (w minutach)
    private const val MIN_INTERVAL = 45
    private const val MAX_INTERVAL = 180

    fun scheduleNextAlarm(context: Context) {
        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        val dailyGoal = prefs.getInt("daily_goal", 2000)

        val dao = WaterDatabase.getDatabase(context).waterDao()
        val todayDate = LocalDate.now().toString()

        CoroutineScope(Dispatchers.IO).launch {
            val entry = dao.getTodayWater(todayDate)
            val currentAmount = entry?.amount ?: 0

            // 1. NAJPIERW USUŃ WSZYSTKIE STARE ALARMY (To naprawia podwójne powiadomienia)
            cancelAlarm(context)

            if (currentAmount >= dailyGoal) {
                Log.d(TAG, "Cel osiągnięty. Nie ustawiam alarmu.")
                return@launch
            }

            // 2. OBLICZ NOWY INTERWAŁ
            val intervalMinutes = calculateSimpleInterval(prefs, currentAmount, dailyGoal)

            if (intervalMinutes > 0) {
                setAlarm(context, intervalMinutes)
            }
        }
    }

    private fun calculateSimpleInterval(prefs: android.content.SharedPreferences, currentAmount: Int, dailyGoal: Int): Int {
        val quickAddAmount = prefs.getInt("quick_add_amount", 250)

        // Pobierz godziny użytkownika
        val wakeUpTotal = prefs.getInt("wake_up_total", 8 * 60) // np. 480 (8:00)
        val sleepTotal = prefs.getInt("sleep_total", 22 * 60)   // np. 1320 (22:00)

        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        // LOGIKA NOCNA: Jeśli jest już po czasie spania, nie wysyłaj
        if (currentMinutes >= sleepTotal) {
            Log.d(TAG, "Jest po czasie spania. Brak alarmu.")
            return 0
        }

        // LOGIKA PORANNA: Jeśli jest przed pobudką, ustaw alarm na czas pobudki + 30 min
        if (currentMinutes < wakeUpTotal) {
            val minutesToWakeUp = wakeUpTotal - currentMinutes
            Log.d(TAG, "Jest przed pobudką. Alarm za ${minutesToWakeUp + 30} min.")
            return minutesToWakeUp + 30
        }

        // --- GŁÓWNA MATEMATYKA (Pacing) ---

        val minutesLeftToSleep = sleepTotal - currentMinutes

        // Jeśli do snu zostało mniej niż godzina, daj spokój
        if (minutesLeftToSleep < 60) return 0

        val waterRemaining = dailyGoal - currentAmount
        // Ile "porcji" wody zostało do wypicia? (np. 1000ml / 250ml = 4 porcje)
        // Math.max(1.0) zabezpiecza przed dzieleniem przez 0
        val portionsRemaining = (waterRemaining.toDouble() / quickAddAmount.toDouble()).coerceAtLeast(1.0)

        // Czas do snu podzielony przez liczbę porcji
        // Np. 600 min / 4 porcje = co 150 min
        val calculatedInterval = (minutesLeftToSleep / portionsRemaining).toInt()

        // OGRANICZNIKI (CLAMP)
        // Nie rzadziej niż co 3h (180min) i nie częściej niż co 45min
        // Dzięki temu nie dostaniesz spamu, ani nie będziesz czekać 5 godzin
        return calculatedInterval.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
    }

    private fun setAlarm(context: Context, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        // FLAG_UPDATE_CURRENT - kluczowe, żeby nadpisać stary intent, a nie tworzyć nowego ducha
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)

        // Zapisz czas nastpenego alarmu do wyświetlenia w UI
        context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("next_alarm_time", triggerTime)
            .apply()

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Alarm ustawiony za $intervalMinutes minut.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Brak uprawnień do alarmu: ${e.message}")
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        // Musi być taki sam PendingIntent jak przy tworzeniu (to samo ID 1001)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Poprzednie alarmy anulowane.")
    }
}