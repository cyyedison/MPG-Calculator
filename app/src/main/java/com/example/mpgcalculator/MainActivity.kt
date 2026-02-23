package com.example.mpgcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mpgcalculator.data.FuelRecord
import com.example.mpgcalculator.databinding.ActivityMainBinding
import com.example.mpgcalculator.databinding.DialogAddRecordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val adapter = FuelRecordAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setSupportActionBar(binding.toolbar)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.records.observe(this) { records ->
            adapter.submitList(records)
            binding.emptyView.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fab.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddRecordBinding.inflate(LayoutInflater.from(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_add) { _, _ ->
                val odoText = dialogBinding.etOdometer.text?.toString()?.trim()
                val fuelText = dialogBinding.etFuel.text?.toString()?.trim()
                if (odoText.isNullOrEmpty() || fuelText.isNullOrEmpty()) {
                    Toast.makeText(this, R.string.error_fields_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val odometer = odoText.toDoubleOrNull()
                val fuel = fuelText.toDoubleOrNull()
                if (odometer == null || fuel == null) {
                    Toast.makeText(this, R.string.error_invalid_number, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val unit = when (dialogBinding.rgUnit.checkedRadioButtonId) {
                    R.id.rbUsGal -> "US_GAL"
                    R.id.rbLitres -> "LITRES"
                    else -> "UK_GAL"
                }
                viewModel.insert(
                    FuelRecord(
                        odometerMiles = odometer,
                        fuelAmount = fuel,
                        fuelUnit = unit,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
