package com.patrykadamski.waterreminder

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Tu mówimy: Ta baza korzysta z tabeli "WaterEntity"
@Database(entities = [WaterEntity::class], version = 1)
abstract class WaterDatabase : RoomDatabase() {

    // Baza musi wiedzieć, jakiego ma "Kelnera"
    abstract fun waterDao(): WaterDao

    // To poniżej to tzw. Singleton.
    // Chodzi o to, żeby nie otwierać 100 połączeń do bazy naraz, tylko zawsze używać jednego.
    companion object {
        @Volatile
        private var INSTANCE: WaterDatabase? = null

        fun getDatabase(context: Context): WaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterDatabase::class.java,
                    "water_database" // Nazwa pliku w telefonie
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}