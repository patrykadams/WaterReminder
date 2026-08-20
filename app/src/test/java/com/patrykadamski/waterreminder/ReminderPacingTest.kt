package com.patrykadamski.waterreminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPacingTest {

    // wake 08:00, sleep 22:00 -> 840 minute window
    private val wakeUpHour = 8
    private val sleepHour = 22

    @Test
    fun baseInterval_evenlySplitsWindowIntoGoalPortions() {
        // 2250 / 250 = 9 portions over 840 minutes = 93.33 -> 93
        val interval = ReminderPacing.baseIntervalMinutes(
            wakeUpHour, sleepHour, dailyGoal = 2250, quickAddAmount = 250,
            frequency = ReminderPacing.FREQUENCY_NORMAL
        )
        assertEquals(93, interval)
    }

    @Test
    fun baseInterval_lessFrequencyStretchesInterval() {
        val normal = ReminderPacing.baseIntervalMinutes(
            wakeUpHour, sleepHour, 2250, 250, ReminderPacing.FREQUENCY_NORMAL
        )
        val less = ReminderPacing.baseIntervalMinutes(
            wakeUpHour, sleepHour, 2250, 250, ReminderPacing.FREQUENCY_LESS
        )
        val more = ReminderPacing.baseIntervalMinutes(
            wakeUpHour, sleepHour, 2250, 250, ReminderPacing.FREQUENCY_MORE
        )
        assertTrue("rzadziej powinno dawać dłuższy interwał", less > normal)
        assertTrue("częściej powinno dawać krótszy interwał", more < normal)
    }

    @Test
    fun nextInterval_aheadOfPace_pushesNextReminderLater() {
        // 12:00, goal 2250, expected so far = (240/840)*2250 ~= 643ml.
        // Already drunk 1500ml -> well ahead -> interval should exceed base.
        val base = 93
        val interval = ReminderPacing.nextIntervalMinutes(
            nowMinuteOfDay = 12 * 60,
            wakeUpHour = wakeUpHour, sleepHour = sleepHour,
            dailyGoal = 2250, currentAmount = 1500, quickAddAmount = 250,
            baseInterval = base
        )
        assertTrue("do przodu -> interwał dłuższy niż bazowy ($interval <= $base)", interval > base)
    }

    @Test
    fun nextInterval_behindInEvening_compressesRemainingReminders() {
        // 19:20, goal 2250, nothing drunk yet -> far behind, 81% through the day -> compress.
        val base = 93
        val interval = ReminderPacing.nextIntervalMinutes(
            nowMinuteOfDay = 19 * 60 + 20,
            wakeUpHour = wakeUpHour, sleepHour = sleepHour,
            dailyGoal = 2250, currentAmount = 0, quickAddAmount = 250,
            baseInterval = base
        )
        assertTrue("w tyle wieczorem -> interwał krótszy niż bazowy ($interval >= $base)", interval < base)
        assertEquals(30, interval) // clamped to the minimum spacing floor
    }

    @Test
    fun nextInterval_behindEarlyInDay_keepsBasePace() {
        // 09:00, nothing drunk yet - technically "behind" the linear curve already,
        // but it's early - should NOT compress, per requirement: only evening compresses.
        val base = 93
        val interval = ReminderPacing.nextIntervalMinutes(
            nowMinuteOfDay = 9 * 60,
            wakeUpHour = wakeUpHour, sleepHour = sleepHour,
            dailyGoal = 2250, currentAmount = 0, quickAddAmount = 250,
            baseInterval = base
        )
        assertEquals(base, interval)
    }

    @Test
    fun nextInterval_goalMet_returnsZero() {
        val interval = ReminderPacing.nextIntervalMinutes(
            nowMinuteOfDay = 12 * 60,
            wakeUpHour = wakeUpHour, sleepHour = sleepHour,
            dailyGoal = 2250, currentAmount = 2250, quickAddAmount = 250,
            baseInterval = 93
        )
        assertEquals(0, interval)
    }

    @Test
    fun nextInterval_neverExceedsMinutesLeftBeforeSleep() {
        // 21:50, only 10 minutes left before sleep - interval must not overshoot bedtime.
        val interval = ReminderPacing.nextIntervalMinutes(
            nowMinuteOfDay = 21 * 60 + 50,
            wakeUpHour = wakeUpHour, sleepHour = sleepHour,
            dailyGoal = 2250, currentAmount = 2000, quickAddAmount = 250,
            baseInterval = 93
        )
        assertTrue(interval <= 10)
    }
}
