package com.eddiec.mpgcalculator.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records WHERE carId = :carId ORDER BY timestampMs DESC")
    fun getForCar(carId: Long): LiveData<List<FuelRecord>>

    @Insert
    suspend fun insert(record: FuelRecord)

    @Update
    suspend fun update(record: FuelRecord)

    @Delete
    suspend fun delete(record: FuelRecord)

    @Query("DELETE FROM fuel_records WHERE carId = :carId")
    suspend fun deleteAllForCar(carId: Long)
}
