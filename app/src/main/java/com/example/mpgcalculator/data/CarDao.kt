package com.example.mpgcalculator.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY id ASC")
    fun getAll(): LiveData<List<Car>>

    @Query("SELECT * FROM cars ORDER BY id ASC")
    suspend fun getAllSync(): List<Car>

    @Query("SELECT COUNT(*) FROM cars")
    suspend fun getCount(): Int

    @Insert
    suspend fun insert(car: Car): Long

    @Update
    suspend fun update(car: Car)

    @Delete
    suspend fun delete(car: Car)
}
