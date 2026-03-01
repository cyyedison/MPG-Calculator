package com.example.mpgcalculator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FuelRecord::class, Car::class], version = 4, exportSchema = false)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
    abstract fun carDao(): CarDao

    companion object {
        @Volatile
        private var INSTANCE: FuelDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE fuel_records ADD COLUMN isPartial INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE fuel_records ADD COLUMN odometerUnit TEXT NOT NULL DEFAULT 'MILES'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS cars " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)"
                )
                database.execSQL("INSERT INTO cars (name) VALUES ('Car 1')")
                database.execSQL(
                    "ALTER TABLE fuel_records ADD COLUMN carId INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO cars (name) VALUES ('Car 1')")
            }
        }

        fun getDatabase(context: Context): FuelDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FuelDatabase::class.java,
                    "fuel_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(SEED_CALLBACK)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
