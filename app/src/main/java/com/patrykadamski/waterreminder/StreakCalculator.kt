package com.patrykadamski.waterreminder

import java.time.LocalDate

/**
 * Shared streak logic - used by WaterViewModel (for the on-screen streak
 * counter) and by EveningSummaryReceiver (for the evening summary
 * notification), so the two can never drift apart.
 */
object StreakCalculator {
    fun calculate(history: List<WaterEntity>, dailyGoal: Int): Int {
        var currentStreak = 0
        var checkDate = LocalDate.now()
        val todayEntry = history.find { it.date == checkDate.toString() }
        if (todayEntry != null && todayEntry.amount >= dailyGoal) currentStreak++
        while (true) {
            checkDate = checkDate.minusDays(1)
            val entry = history.find { it.date == checkDate.toString() }
            if (entry != null && entry.amount >= dailyGoal) currentStreak++ else break
        }
        return currentStreak
    }

    fun dayWord(days: Int): String = if (days == 1) "dzień" else "dni"
}
