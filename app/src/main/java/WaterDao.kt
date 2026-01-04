package com.patrykadamski.waterreminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WaterDao {
    // Kelnerze, przynieś mi wynik z dzisiaj!
    // (SQL: Wybierz wszystko z tabeli, gdzie data to dzisiaj, daj tylko 1 wynik)
    @Query("SELECT * FROM water_table WHERE date = :todayDate LIMIT 1")
    suspend fun getTodayWater(todayDate: String): WaterEntity?

    // Kelnerze, zapisz to! A jak już jest wpis z tą datą, to go nadpisz (REPLACE).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(water: WaterEntity)
}