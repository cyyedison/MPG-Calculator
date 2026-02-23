package com.example.mpgcalculator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mpgcalculator.data.FuelRecord
import com.example.mpgcalculator.databinding.ItemFuelRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FuelRecordAdapter : ListAdapter<FuelRecord, FuelRecordAdapter.ViewHolder>(DIFF) {

    var displayUnit: String = SettingsActivity.DEFAULT_DISPLAY_UNIT
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FuelRecord>() {
            override fun areItemsTheSame(oldItem: FuelRecord, newItem: FuelRecord) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FuelRecord, newItem: FuelRecord) =
                oldItem == newItem
        }
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        private fun computeConsumption(
            tripMiles: Double,
            fuelAmount: Double,
            fuelUnit: String,
            displayUnit: String
        ): String {
            val fuelLitres = when (fuelUnit) {
                "UK_GAL" -> fuelAmount * 4.54609
                "US_GAL" -> fuelAmount * 3.78541
                else     -> fuelAmount // LITRES
            }
            if (fuelLitres <= 0 || tripMiles <= 0) return "—"
            val tripKm = tripMiles * 1.60934
            return when (displayUnit) {
                "MPG_US" -> "%.1f MPG (US)".format(tripMiles / fuelLitres * 3.78541)
                "MPL"    -> "%.2f mi/L".format(tripMiles / fuelLitres)
                "KML"    -> "%.2f km/L".format(tripKm / fuelLitres)
                "L100KM" -> "%.2f L/100km".format(fuelLitres / tripKm * 100)
                else     -> "%.1f MPG (UK)".format(tripMiles / fuelLitres * 4.54609)
            }
        }
    }

    inner class ViewHolder(private val binding: ItemFuelRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: FuelRecord, previousRecord: FuelRecord?) {
            binding.tvDateTime.text = DATE_FORMAT.format(Date(record.timestampMs))
            binding.tvOdometer.text = "Odometer: %.1f mi".format(record.odometerMiles)
            val unitLabel = when (record.fuelUnit) {
                "US_GAL" -> "US gal"
                "LITRES" -> "L"
                else     -> "UK gal"
            }
            binding.tvFuel.text = "Fuel: %.2f $unitLabel".format(record.fuelAmount)
            binding.tvMpg.text = if (previousRecord == null) {
                "First fill-up"
            } else {
                computeConsumption(
                    record.odometerMiles - previousRecord.odometerMiles,
                    record.fuelAmount,
                    record.fuelUnit,
                    displayUnit
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFuelRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val previousRecord = if (position < itemCount - 1) getItem(position + 1) else null
        holder.bind(getItem(position), previousRecord)
    }
}
