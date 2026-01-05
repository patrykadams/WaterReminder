package com.patrykadamski.waterreminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // WAŻNY NOWY IMPORT

@Dao
interface WaterDao {
    // Zmiana: Zwracamy Flow, czyli "strumień" danych na żywo
    @Query("SELECT * FROM water_table WHERE date = :todayDate LIMIT 1")
    fun getTodayWaterFlow(todayDate: String): Flow<WaterEntity?>

    // Zwykła metoda do pobrania raz (np. dla serwisu kafelka)
    @Query("SELECT * FROM water_table WHERE date = :todayDate LIMIT 1")
    suspend fun getTodayWater(todayDate: String): WaterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(water: WaterEntity)

    // Wykres też będzie się aktualizował na żywo
    @Query("SELECT * FROM water_table ORDER BY date DESC LIMIT 7")
    fun getLast7DaysFlow(): Flow<List<WaterEntity>>

    // Używane w ViewModel do wykresu (stara metoda, może zostać jako Flow lub suspend)
    @Query("SELECT * FROM water_table ORDER BY date DESC LIMIT 7")
    suspend fun getLast7Days(): List<WaterEntity>

    @Query("SELECT * FROM water_table ORDER BY date DESC")
    suspend fun getAllHistory(): List<WaterEntity>
}