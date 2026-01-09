package com.patrykadamski.waterreminder

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single record in the database table 'water_table'.
 * It stores the total amount of water drunk on a specific date.
 *
 * @property id Unique identifier for the record (auto-generated).
 * @property date The date string (format: YYYY-MM-DD).
 * @property amount The total water intake in milliliters.
 */
@Entity(tableName = "water_table")
data class WaterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val amount: Int
)