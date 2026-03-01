package com.example.mpgcalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long = 1,
    val odometerMiles: Double,   // always in miles — used for all distance calculations
    val odometerUnit: String = "MILES", // original unit the user typed ("MILES" | "KM")
    val fuelAmount: Double,
    val fuelUnit: String,
    val timestampMs: Long,
    val isPartial: Boolean = false
)
