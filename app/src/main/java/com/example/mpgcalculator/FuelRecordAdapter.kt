package com.eddiec.mpgcalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eddiec.mpgcalculator.data.FuelRecord
import com.eddiec.mpgcalculator.databinding.ItemFuelRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FuelRecordAdapter : ListAdapter<FuelRecord, FuelRecordAdapter.ViewHolder>(DIFF) {

    var displayUnit: String = SettingsActivity.DEFAULT_DISPLAY_UNIT
        set(value) { field = value; notifyDataSetChanged() }

    var costPerLitre: Double = 0.0
        set(value) { field = value; notifyDataSetChanged() }

    var currencySymbol: String = "£"
        set(value) { field = value; notifyDataSetChanged() }

    var onItemClick: ((FuelRecord) -> Unit)? = null

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FuelRecord>() {
            override fun areItemsTheSame(oldItem: FuelRecord, newItem: FuelRecord) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FuelRecord, newItem: FuelRecord) =
                oldItem == newItem
        }
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        fun computeConsumptionValue(
            tripMiles: Double,
            fuelAmount: Double,
            fuelUnit: String,
            displayUnit: String
        ): Double? {
            val fuelLitres = when (fuelUnit) {
                "UK_GAL" -> fuelAmount * 4.54609
                "US_GAL" -> fuelAmount * 3.78541
                else     -> fuelAmount
            }
            if (fuelLitres <= 0 || tripMiles <= 0) return null
            val tripKm = tripMiles * 1.60934
            return when (displayUnit) {
                "MPG_US" -> tripMiles / fuelLitres * 3.78541
                "MPL"    -> tripMiles / fuelLitres
                "KML"    -> tripKm / fuelLitres
                "L100KM" -> fuelLitres / tripKm * 100
                else     -> tripMiles / fuelLitres * 4.54609
            }
        }

        fun displayUnitLabel(displayUnit: String): String = when (displayUnit) {
            "MPG_US" -> "MPG (US)"
            "MPL"    -> "mi/L"
            "KML"    -> "km/L"
            "L100KM" -> "L/100km"
            else     -> "MPG (UK)"
        }

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
            binding.tvOdometer.text = if (record.odometerUnit == "KM") {
                "Odometer: %d km".format((record.odometerMiles * 1.60934).toInt())
            } else {
                "Odometer: %d mi".format(record.odometerMiles.toInt())
            }
            val unitLabel = when (record.fuelUnit) {
                "US_GAL" -> "US gal"
                "LITRES" -> "L"
                else     -> "UK gal"
            }
            binding.tvFuel.text = "Fuel: %.2f $unitLabel".format(record.fuelAmount)

            val tripMiles = if (previousRecord != null) {
                record.odometerMiles - previousRecord.odometerMiles
            } else 0.0

            // Trip distance
            val useKm = displayUnit == "KML" || displayUnit == "L100KM"
            if (previousRecord != null && !record.isPartial && tripMiles > 0) {
                val tripDisplay = if (useKm) tripMiles * 1.60934 else tripMiles
                val unit = if (useKm) "km" else "mi"
                binding.tvTripDistance.text = "Trip: %d %s".format(tripDisplay.toInt(), unit)
                binding.tvTripDistance.visibility = View.VISIBLE
            } else {
                binding.tvTripDistance.visibility = View.GONE
            }

            binding.tvMpg.text = when {
                record.isPartial       -> "Missed fill-up(s) — no calculation"
                previousRecord == null -> "First fill-up"
                else -> computeConsumption(tripMiles, record.fuelAmount, record.fuelUnit, displayUnit)
            }

            // Cost per mile / km
            val showCost = costPerLitre > 0 && !record.isPartial &&
                previousRecord != null && tripMiles > 0
            if (showCost) {
                val fuelLitres = when (record.fuelUnit) {
                    "UK_GAL" -> record.fuelAmount * 4.54609
                    "US_GAL" -> record.fuelAmount * 3.78541
                    else     -> record.fuelAmount
                }
                val totalCost = fuelLitres * costPerLitre
                val useKm = displayUnit == "KML" || displayUnit == "L100KM"
                val costPerUnit = if (useKm) totalCost / (tripMiles * 1.60934) else totalCost / tripMiles
                val label = if (useKm) "km" else "mi"
                val prefix = if (currencySymbol.isEmpty()) "" else currencySymbol
                binding.tvCost.text = "Cost: $prefix%.3f / %s".format(costPerUnit, label)
                binding.tvCost.visibility = View.VISIBLE
            } else {
                binding.tvCost.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick?.invoke(record) }
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
