package com.patrykadamski.waterreminder

import kotlin.math.roundToInt

/**
 * Pure adaptive-scheduling logic for water reminders. Decides WHEN the next
 * reminder should fire - never touches AlarmManager itself (that stays in
 * AlarmScheduler).
 *
 * Model: reminders are spread evenly across the wake-sleep window so the
 * "expected" cumulative intake grows linearly towards the daily goal. Each
 * time a reminder is (re)scheduled, the next interval is nudged away from
 * that even base depending on whether the user is ahead or behind the linear
 * pace right now.
 */
object ReminderPacing {

    const val FREQUENCY_LESS = "LESS"
    const val FREQUENCY_NORMAL = "NORMAL"
    const val FREQUENCY_MORE = "MORE"

    private const val MIN_INTERVAL_MINUTES = 30
    private const val MAX_INTERVAL_MINUTES = 180

    // How far ahead being "pushed later" can stretch the interval, as a multiple of the base.
    private const val MAX_AHEAD_MULTIPLIER = 3.0

    // Compression (falling behind) only kicks in once this fraction of the awake window
    // has elapsed - being behind earlier in the day still has time to even out naturally.
    private const val EVENING_THRESHOLD = 0.7

    fun frequencyScale(frequency: String): Double = when (frequency) {
        FREQUENCY_LESS -> 1.4
        FREQUENCY_MORE -> 0.7
        else -> 1.0
    }

    fun awakeWindowMinutes(wakeUpHour: Int, sleepHour: Int): Int {
        val wakeMinutes = wakeUpHour * 60
        var sleepMinutes = sleepHour * 60
        if (sleepHour < 5) sleepMinutes += 24 * 60 // bedtime after midnight
        return (sleepMinutes - wakeMinutes).coerceAtLeast(60)
    }

    /**
     * The even, linear-pace cadence for a full day: the wake-sleep window split into
     * as many slots as it takes to reach the goal in [quickAddAmount]-sized portions,
     * scaled by the user's chosen frequency. This is the stable baseline the frequency
     * slider controls directly - it is not recomputed from the day's remaining water.
     */
    fun baseIntervalMinutes(
        wakeUpHour: Int,
        sleepHour: Int,
        dailyGoal: Int,
        quickAddAmount: Int,
        frequency: String
    ): Int {
        val windowMinutes = awakeWindowMinutes(wakeUpHour, sleepHour)
        val portions = (dailyGoal.toDouble() / quickAddAmount.coerceAtLeast(1).toDouble()).coerceAtLeast(1.0)
        val evenInterval = windowMinutes / portions
        return (evenInterval * frequencyScale(frequency))
            .roundToInt()
            .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
    }

    /**
     * Minutes until the next reminder, adapted from [baseInterval] to the user's current
     * pace against the linear schedule. Returns 0 to mean "goal met" or "day is over" -
     * the same signal AlarmScheduler already treats as "schedule for tomorrow".
     */
    fun nextIntervalMinutes(
        nowMinuteOfDay: Int,
        wakeUpHour: Int,
        sleepHour: Int,
        dailyGoal: Int,
        currentAmount: Int,
        quickAddAmount: Int,
        baseInterval: Int
    ): Int {
        if (currentAmount >= dailyGoal) return 0

        val wakeMinutes = wakeUpHour * 60
        var sleepMinutes = sleepHour * 60
        if (sleepHour < 5) sleepMinutes += 24 * 60
        var now = nowMinuteOfDay
        if (now < wakeMinutes) now += 24 * 60 // "now" during a post-midnight awake window

        val windowMinutes = (sleepMinutes - wakeMinutes).coerceAtLeast(60)
        val minutesLeft = sleepMinutes - now
        if (minutesLeft <= 0) return 0

        val elapsedRatio = ((now - wakeMinutes).toDouble() / windowMinutes).coerceIn(0.0, 1.0)
        val expectedAmount = dailyGoal * elapsedRatio
        val paceDelta = currentAmount - expectedAmount

        val interval = when {
            // Ahead of the linear pace: push the next reminder out, proportionally to the surplus.
            paceDelta > 0 -> {
                val aheadRatio = (paceDelta / dailyGoal.toDouble()).coerceIn(0.0, 1.0)
                baseInterval * (1.0 + aheadRatio * MAX_AHEAD_MULTIPLIER)
            }
            // Clearly behind AND late in the day: compress to whatever cadence is still
            // needed to realistically reach the goal before bedtime (never slower than base).
            paceDelta < 0 && elapsedRatio >= EVENING_THRESHOLD -> {
                val remainingMl = (dailyGoal - currentAmount).coerceAtLeast(0)
                val remainingPortions = (remainingMl.toDouble() / quickAddAmount.coerceAtLeast(1))
                    .coerceAtLeast(1.0)
                val catchUpInterval = minutesLeft / remainingPortions
                minOf(baseInterval.toDouble(), catchUpInterval)
            }
            // On pace, or behind but still early enough in the day to even out naturally.
            else -> baseInterval.toDouble()
        }

        return interval.roundToInt().coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES).coerceAtMost(minutesLeft)
    }
}
