package com.eddiec.mpgcalculator

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.eddiec.mpgcalculator.data.Car
import com.eddiec.mpgcalculator.data.FuelDatabase
import com.eddiec.mpgcalculator.data.FuelRecord
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val fuelDao = FuelDatabase.getDatabase(application).fuelDao()
    private val carDao = FuelDatabase.getDatabase(application).carDao()
    private val prefs = application.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

    val cars = carDao.getAll()

    private val _selectedCarId = MutableLiveData(prefs.getLong(KEY_SELECTED_CAR_ID, 1L))
    val selectedCarId: MutableLiveData<Long> = _selectedCarId

    val records = _selectedCarId.switchMap { carId -> fuelDao.getForCar(carId) }

    init {
        // On fresh install (no cars yet), seed "Car 1" and initialise the counter
        viewModelScope.launch {
            if (carDao.getCount() == 0) {
                val id = carDao.insert(Car(name = "Car 1"))
                _selectedCarId.postValue(id)
                prefs.edit()
                    .putLong(KEY_SELECTED_CAR_ID, id)
                    .putInt(KEY_NEXT_CAR_NUMBER, 2)
                    .apply()
            }
        }
    }

    fun selectCar(id: Long) {
        _selectedCarId.value = id
        prefs.edit().putLong(KEY_SELECTED_CAR_ID, id).apply()
    }

    fun insertCar(name: String) = viewModelScope.launch {
        val id = carDao.insert(Car(name = name.trim()))
        _selectedCarId.postValue(id)
        prefs.edit().putLong(KEY_SELECTED_CAR_ID, id).apply()
    }

    fun addCar() = viewModelScope.launch {
        val n = prefs.getInt(KEY_NEXT_CAR_NUMBER, 2)
        prefs.edit().putInt(KEY_NEXT_CAR_NUMBER, n + 1).apply()
        val id = carDao.insert(Car(name = "Car $n"))
        _selectedCarId.postValue(id)
        prefs.edit().putLong(KEY_SELECTED_CAR_ID, id).apply()
    }

    fun renameCar(car: Car, newName: String) = viewModelScope.launch {
        carDao.update(car.copy(name = newName.trim()))
    }

    fun deleteCar(car: Car) = viewModelScope.launch {
        fuelDao.deleteAllForCar(car.id)
        carDao.delete(car)
        // If the deleted car was selected, switch to first remaining car
        if (_selectedCarId.value == car.id) {
            val first = carDao.getAllSync().firstOrNull()
            if (first != null) {
                _selectedCarId.postValue(first.id)
                prefs.edit().putLong(KEY_SELECTED_CAR_ID, first.id).apply()
            }
        }
    }

    fun insert(record: FuelRecord) = viewModelScope.launch { fuelDao.insert(record) }
    fun update(record: FuelRecord) = viewModelScope.launch { fuelDao.update(record) }
    fun delete(record: FuelRecord) = viewModelScope.launch { fuelDao.delete(record) }
    fun deleteAll() = viewModelScope.launch {
        fuelDao.deleteAllForCar(_selectedCarId.value ?: 1L)
    }

    companion object {
        const val KEY_SELECTED_CAR_ID  = "selected_car_id"
        const val KEY_NEXT_CAR_NUMBER  = "next_car_number"
    }
}
