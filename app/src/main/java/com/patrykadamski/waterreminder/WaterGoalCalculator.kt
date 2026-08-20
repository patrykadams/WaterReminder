package com.patrykadamski.waterreminder

object WaterGoalCalculator {

    const val ACTIVITY_LOW = "LOW"
    const val ACTIVITY_MEDIUM = "MEDIUM"
    const val ACTIVITY_HIGH = "HIGH"

    private const val ML_PER_KG = 32
    private const val MEDIUM_ACTIVITY_BONUS_ML = 400
    private const val HIGH_ACTIVITY_BONUS_ML = 800
    private const val ROUNDING_STEP_ML = 50

    // Suggested daily goal from bodyweight and activity level. Not a medical
    // recommendation - just a starting point the user can freely override.
    fun suggestedGoalMl(weightKg: Int, activityLevel: String): Int {
        val activityBonus = when (activityLevel) {
            ACTIVITY_MEDIUM -> MEDIUM_ACTIVITY_BONUS_ML
            ACTIVITY_HIGH -> HIGH_ACTIVITY_BONUS_ML
            else -> 0
        }
        val raw = weightKg * ML_PER_KG + activityBonus
        return ((raw + ROUNDING_STEP_ML / 2) / ROUNDING_STEP_ML) * ROUNDING_STEP_ML
    }

    fun normalizeActivityLevel(value: String): String = when (value) {
        ACTIVITY_MEDIUM, ACTIVITY_HIGH -> value
        else -> ACTIVITY_LOW
    }
}
