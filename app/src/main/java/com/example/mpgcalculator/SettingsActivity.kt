package com.example.mpgcalculator

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.example.mpgcalculator.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    // Tracks which fuel unit is currently selected so cost conversion works correctly
    private var currentDefaultFuelUnit = DEFAULT_FUEL_UNIT

    // Suppress the cost TextWatcher while we're programmatically updating the field
    private var updatingCostField = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupDisplayUnit()
        setupDefaultOdometerUnit()
        setupDefaultFuelUnit()
        setupFuelCost()
        setupCurrencySymbol()
        setupTheme()

        binding.btnHowToUse.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.how_to_use_title)
                .setMessage(R.string.how_to_use_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    // ── Fuel consumption display unit ─────────────────────────────────────────

    private fun setupDisplayUnit() {
        val currentUnit = prefs.getString(KEY_DISPLAY_UNIT, DEFAULT_DISPLAY_UNIT)
        binding.rgDisplayUnit.check(
            when (currentUnit) {
                "MPG_US" -> R.id.rbMpgUs
                "MPL"    -> R.id.rbMpl
                "KML"    -> R.id.rbKml
                "L100KM" -> R.id.rbL100km
                else     -> R.id.rbMpgUk
            }
        )
        binding.rgDisplayUnit.setOnCheckedChangeListener { _, checkedId ->
            val unit = when (checkedId) {
                R.id.rbMpgUs  -> "MPG_US"
                R.id.rbMpl    -> "MPL"
                R.id.rbKml    -> "KML"
                R.id.rbL100km -> "L100KM"
                else          -> "MPG_UK"
            }
            prefs.edit().putString(KEY_DISPLAY_UNIT, unit).apply()
        }
    }

    // ── Default odometer unit ─────────────────────────────────────────────────

    private fun setupDefaultOdometerUnit() {
        val current = prefs.getString(KEY_DEFAULT_ODOMETER_UNIT, DEFAULT_ODOMETER_UNIT)
        binding.rgDefaultOdometerUnit.check(
            if (current == "KM") R.id.rbDefaultKm else R.id.rbDefaultMiles
        )
        binding.rgDefaultOdometerUnit.setOnCheckedChangeListener { _, checkedId ->
            val unit = if (checkedId == R.id.rbDefaultKm) "KM" else "MILES"
            prefs.edit().putString(KEY_DEFAULT_ODOMETER_UNIT, unit).apply()
        }
    }

    // ── Default fuel unit ─────────────────────────────────────────────────────

    private fun setupDefaultFuelUnit() {
        currentDefaultFuelUnit = prefs.getString(KEY_DEFAULT_FUEL_UNIT, DEFAULT_FUEL_UNIT)
            ?: DEFAULT_FUEL_UNIT

        binding.rgDefaultFuelUnit.check(
            when (currentDefaultFuelUnit) {
                "UK_GAL" -> R.id.rbDefaultUkGal
                "US_GAL" -> R.id.rbDefaultUsGal
                else     -> R.id.rbDefaultLitres
            }
        )

        binding.rgDefaultFuelUnit.setOnCheckedChangeListener { _, checkedId ->
            val newUnit = when (checkedId) {
                R.id.rbDefaultUkGal -> "UK_GAL"
                R.id.rbDefaultUsGal -> "US_GAL"
                else                -> "LITRES"
            }
            if (newUnit != currentDefaultFuelUnit) {
                convertCostDisplayToUnit(newUnit)
                currentDefaultFuelUnit = newUnit
                updateCostHint(newUnit)
            }
            prefs.edit().putString(KEY_DEFAULT_FUEL_UNIT, newUnit).apply()
        }
    }

    // ── Fuel cost ─────────────────────────────────────────────────────────────

    private fun setupFuelCost() {
        updateCostHint(currentDefaultFuelUnit)

        // Convert stored per-litre value back to the user's display unit
        val costPerLitre = prefs.getFloat(KEY_COST_PER_LITRE, 0f).toDouble()
        if (costPerLitre > 0) {
            val display = costPerLitre * fuelUnitFactor(currentDefaultFuelUnit)
            binding.etFuelCost.setText("%.4f".format(display))
        }

        binding.etFuelCost.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (updatingCostField) return
                val input = s?.toString()?.toDoubleOrNull() ?: 0.0
                val perLitre = if (input > 0) input / fuelUnitFactor(currentDefaultFuelUnit) else 0.0
                prefs.edit().putFloat(KEY_COST_PER_LITRE, perLitre.toFloat()).apply()
            }
        })
    }

    /** When the fuel unit selection changes, convert the displayed cost to the new unit. */
    private fun convertCostDisplayToUnit(newUnit: String) {
        val displayedCost = binding.etFuelCost.text?.toString()?.toDoubleOrNull() ?: return
        val perLitre = displayedCost / fuelUnitFactor(currentDefaultFuelUnit)
        val inNewUnit = perLitre * fuelUnitFactor(newUnit)
        updatingCostField = true
        binding.etFuelCost.setText("%.4f".format(inNewUnit))
        updatingCostField = false
    }

    private fun updateCostHint(unit: String) {
        binding.tilFuelCost.hint = getString(
            when (unit) {
                "UK_GAL" -> R.string.hint_cost_per_uk_gal
                "US_GAL" -> R.string.hint_cost_per_us_gal
                else     -> R.string.hint_cost_per_litre
            }
        )
    }

    private fun fuelUnitFactor(unit: String): Double = when (unit) {
        "UK_GAL" -> 4.54609
        "US_GAL" -> 3.78541
        else     -> 1.0
    }

    // ── Currency symbol ───────────────────────────────────────────────────────

    private fun setupCurrencySymbol() {
        val current = prefs.getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL)
        binding.rgCurrencySymbol.check(
            when (current) {
                "$" -> R.id.rbCurrencyUsd
                "€" -> R.id.rbCurrencyEur
                ""  -> R.id.rbCurrencyNone
                else -> R.id.rbCurrencyGbp
            }
        )
        binding.rgCurrencySymbol.setOnCheckedChangeListener { _, checkedId ->
            val symbol = when (checkedId) {
                R.id.rbCurrencyUsd  -> "$"
                R.id.rbCurrencyEur  -> "€"
                R.id.rbCurrencyNone -> ""
                else                -> "£"
            }
            prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private fun setupTheme() {
        val current = prefs.getString(KEY_THEME, "SYSTEM")
        binding.rgTheme.check(
            when (current) {
                "LIGHT"  -> R.id.rbThemeLight
                "DARK"   -> R.id.rbThemeDark
                else     -> R.id.rbThemeSystem
            }
        )
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.rbThemeLight -> "LIGHT"
                R.id.rbThemeDark  -> "DARK"
                else              -> "SYSTEM"
            }
            prefs.edit().putString(KEY_THEME, theme).apply()
            MpgApplication.applyTheme(theme)
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Convenience base class to avoid boilerplate in TextWatchers. */
    abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    companion object {
        const val PREFS_NAME = "mpg_prefs"

        const val KEY_DISPLAY_UNIT          = "display_unit"
        const val KEY_DEFAULT_ODOMETER_UNIT = "default_odometer_unit"
        const val KEY_DEFAULT_FUEL_UNIT     = "default_fuel_unit"
        const val KEY_VEHICLE_NAME          = "vehicle_name"
        const val KEY_COST_PER_LITRE        = "cost_per_litre"
        const val KEY_CURRENCY_SYMBOL       = "currency_symbol"
        const val KEY_THEME                 = "theme"

        const val DEFAULT_DISPLAY_UNIT      = "MPG_UK"
        const val DEFAULT_ODOMETER_UNIT     = "MILES"
        const val DEFAULT_FUEL_UNIT         = "LITRES"
        const val DEFAULT_CURRENCY_SYMBOL   = "£"
    }
}
