package com.patrykadamski.waterreminder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_table")
data class WaterEntity(
    @PrimaryKey
    val date: String, // Np. "2023-10-27" - to będzie nasz klucz (jeden wpis na dzień)
    val amount: Int   // Np. 1500 (ml)
)