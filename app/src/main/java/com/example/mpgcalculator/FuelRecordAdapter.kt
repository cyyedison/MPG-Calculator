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

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FuelRecord>() {
            override fun areItemsTheSame(oldItem: FuelRecord, newItem: FuelRecord) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FuelRecord, newItem: FuelRecord) =
                oldItem == newItem
        }
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        private fun toImperialGallons(amount: Double, unit: String): Double = when (unit) {
            "US_GAL" -> amount * 0.832674
            "LITRES" -> amount * 0.219969
            else -> amount // UK_GAL
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
                else -> "UK gal"
            }
            binding.tvFuel.text = "Fuel: %.2f $unitLabel".format(record.fuelAmount)
            if (previousRecord == null) {
                binding.tvMpg.text = "First fill-up"
            } else {
                val tripMiles = record.odometerMiles - previousRecord.odometerMiles
                val imperialGallons = toImperialGallons(record.fuelAmount, record.fuelUnit)
                binding.tvMpg.text = if (imperialGallons > 0 && tripMiles > 0) {
                    "%.1f MPG (UK)".format(tripMiles / imperialGallons)
                } else {
                    "—"
                }
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
        // List is DESC by timestamp; previous chronological record is the next item (higher index)
        val previousRecord = if (position < itemCount - 1) getItem(position + 1) else null
        holder.bind(getItem(position), previousRecord)
    }
}
