package com.example.mpgcalculator.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records ORDER BY timestampMs DESC")
    fun getAll(): LiveData<List<FuelRecord>>

    @Insert
    suspend fun insert(record: FuelRecord)

    @Update
    suspend fun update(record: FuelRecord)

    @Delete
    suspend fun delete(record: FuelRecord)

    @Query("DELETE FROM fuel_records")
    suspend fun deleteAll()
}
