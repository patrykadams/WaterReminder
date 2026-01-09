package com.patrykadamski.waterreminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) providing methods to interact with the 'water_table'.
 * Uses Kotlin Flow for reactive data updates in the UI.
 */
@Dao
interface WaterDao {

    /**
     * Inserts a new record or replaces an existing one if a conflict occurs.
     * Note: Since we query by date logic in ViewModel, simplistic insert is sufficient here.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(water: WaterEntity)

    /**
     * Retrieves the water record for a specific date (synchronous/suspend).
     * Used by AlarmScheduler to check current progress in the background.
     */
    @Query("SELECT * FROM water_table WHERE date = :date LIMIT 1")
    suspend fun getTodayWater(date: String): WaterEntity?

    /**
     * Retrieves the water record for a specific date as a Flow.
     * Used by ViewModel to update the UI immediately when data changes.
     */
    @Query("SELECT * FROM water_table WHERE date = :date LIMIT 1")
    fun getTodayWaterFlow(date: String): Flow<WaterEntity?>

    /**
     * Retrieves the last 7 records for the statistics chart.
     */
    @Query("SELECT * FROM water_table ORDER BY date DESC LIMIT 7")
    fun getLast7DaysFlow(): Flow<List<WaterEntity>>

    /**
     * Retrieves all history records. Used for streak calculation.
     */
    @Query("SELECT * FROM water_table ORDER BY date DESC")
    suspend fun getAllHistory(): List<WaterEntity>
}