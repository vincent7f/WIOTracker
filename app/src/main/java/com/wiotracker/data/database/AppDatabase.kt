package com.wiotracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wiotracker.data.database.dao.WifiScanDao
import com.wiotracker.data.database.entity.WifiScanRecord

@Database(
    entities = [WifiScanRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiScanDao(): WifiScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifi_tracker_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
