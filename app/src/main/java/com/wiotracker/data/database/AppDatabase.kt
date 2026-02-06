package com.wiotracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wiotracker.data.database.dao.WifiScanDao
import com.wiotracker.data.database.entity.WifiScanRecord

@Database(
    entities = [WifiScanRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiScanDao(): WifiScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add scanSessionId column with default value 0
                database.execSQL("ALTER TABLE wifi_scan_records ADD COLUMN scanSessionId INTEGER NOT NULL DEFAULT 0")
                // Create index for scanSessionId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wifi_scan_records_scanSessionId ON wifi_scan_records(scanSessionId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifi_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
