package com.example.mpgcalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val odometerMiles: Double,
    val fuelAmount: Double,
    val fuelUnit: String,
    val timestampMs: Long
)
