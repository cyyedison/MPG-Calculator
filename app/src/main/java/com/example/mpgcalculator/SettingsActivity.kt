package com.example.mpgcalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mpgcalculator.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
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

        binding.btnHowToUse.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.how_to_use_title)
                .setMessage(R.string.how_to_use_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val PREFS_NAME = "mpg_prefs"
        const val KEY_DISPLAY_UNIT = "display_unit"
        const val DEFAULT_DISPLAY_UNIT = "MPG_UK"
    }
}
