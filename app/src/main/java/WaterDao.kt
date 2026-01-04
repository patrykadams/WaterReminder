package com.patrykadamski.waterreminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_table WHERE date = :todayDate LIMIT 1")
    suspend fun getTodayWater(todayDate: String): WaterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(water: WaterEntity)

    // --- NOWA LINIA: Pobierz historię (sortuj od najnowszej) ---
    @Query("SELECT * FROM water_table ORDER BY date DESC LIMIT 7")
    suspend fun getLast7Days(): List<WaterEntity>
}