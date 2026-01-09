package com.patrykadamski.waterreminder

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main database holder.
 * Implements the Singleton pattern to prevent multiple instances of the database opening at the same time.
 */
@Database(entities = [WaterEntity::class], version = 1, exportSchema = false)
abstract class WaterDatabase : RoomDatabase() {

    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: WaterDatabase? = null

        /**
         * Returns the singleton instance of WaterDatabase.
         * Creates the database if it does not exist.
         */
        fun getDatabase(context: Context): WaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterDatabase::class.java,
                    "water_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}