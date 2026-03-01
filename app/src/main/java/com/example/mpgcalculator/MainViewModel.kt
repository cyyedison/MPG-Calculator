package com.example.mpgcalculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mpgcalculator.data.FuelDatabase
import com.example.mpgcalculator.data.FuelRecord
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = FuelDatabase.getDatabase(application).fuelDao()

    val records = dao.getAll()

    fun insert(record: FuelRecord) = viewModelScope.launch {
        dao.insert(record)
    }

    fun update(record: FuelRecord) = viewModelScope.launch {
        dao.update(record)
    }

    fun delete(record: FuelRecord) = viewModelScope.launch {
        dao.delete(record)
    }

    fun deleteAll() = viewModelScope.launch {
        dao.deleteAll()
    }
}
