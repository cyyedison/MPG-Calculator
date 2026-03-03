package com.eddiec.mpgcalculator

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.eddiec.mpgcalculator.data.FuelRecord
import com.eddiec.mpgcalculator.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip

class TutorialDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val recordAdapter = FuelRecordAdapter()
    private val chartAdapter = ChartHeaderAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        addChip("My Car", selected = true)
        addChip("Work Van", selected = false)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ConcatAdapter(chartAdapter, recordAdapter)

        val displayUnit = "MPG_UK"
        val records = buildFakeRecords()
        recordAdapter.displayUnit = displayUnit
        recordAdapter.submitList(records)
        binding.emptyView.visibility = View.GONE

        // Build chart data
        val points = mutableListOf<Double>()
        val indices = mutableListOf<Int>()
        for (i in 0 until records.size - 1) {
            val r = records[i]
            val prev = records[i + 1]
            if (!r.isPartial) {
                val trip = r.odometerMiles - prev.odometerMiles
                val v = FuelRecordAdapter.computeConsumptionValue(trip, r.fuelAmount, r.fuelUnit, displayUnit)
                if (v != null && v > 0) {
                    points.add(v)
                    indices.add(i)
                }
            }
        }
        chartAdapter.setData(points.reversed(), indices.reversed(), FuelRecordAdapter.displayUnitLabel(displayUnit))

        val screenshotMode = intent.getBooleanExtra(EXTRA_SCREENSHOT_MODE, false)
        if (!screenshotMode) {
            binding.root.post { startTutorial() }
        }
    }

    companion object {
        const val EXTRA_SCREENSHOT_MODE = "screenshot_mode"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun addChip(name: String, selected: Boolean) {
        val dp = resources.displayMetrics.density
        val chip = Chip(this).apply {
            text = name
            isCheckable = true
            isChecked = selected
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (4 * dp).toInt() }
        binding.carChipContainer.addView(chip, params)
    }

    private fun buildFakeRecords(): List<FuelRecord> {
        val day = 86_400_000L
        val now = System.currentTimeMillis()
        return listOf(
            FuelRecord(id = 5, carId = 1, odometerMiles = 15432.0, odometerUnit = "MILES", fuelAmount = 38.2, fuelUnit = "LITRES", timestampMs = now - day * 2,  isPartial = false),
            FuelRecord(id = 4, carId = 1, odometerMiles = 14987.0, odometerUnit = "MILES", fuelAmount = 41.5, fuelUnit = "LITRES", timestampMs = now - day * 17, isPartial = false),
            FuelRecord(id = 3, carId = 1, odometerMiles = 14521.0, odometerUnit = "MILES", fuelAmount = 39.8, fuelUnit = "LITRES", timestampMs = now - day * 32, isPartial = false),
            FuelRecord(id = 2, carId = 1, odometerMiles = 14063.0, odometerUnit = "MILES", fuelAmount = 42.0, fuelUnit = "LITRES", timestampMs = now - day * 47, isPartial = false),
            FuelRecord(id = 1, carId = 1, odometerMiles = 13612.0, odometerUnit = "MILES", fuelAmount = 40.0, fuelUnit = "LITRES", timestampMs = now - day * 62, isPartial = false)
        )
    }

    private fun startTutorial() {
        val lm = binding.recyclerView.layoutManager as? LinearLayoutManager
        val steps = listOf(
            TutorialStep(
                title = "Welcome to MPG Calculator",
                message = "This quick tour shows you how to track your fuel economy. Tap anywhere to go to the next step."
            ),
            TutorialStep(
                title = "Your cars",
                message = "Switch between your cars here. Tap the selected chip to rename it, or long-press any chip to rename.",
                getTargetView = { binding.carChipContainer }
            ),
            TutorialStep(
                title = "Log a fill-up",
                message = "Always fill your tank to the brim, then tap + to record it. Enter your current odometer reading and how much fuel you added.",
                getTargetView = { binding.fab }
            ),
            TutorialStep(
                title = "Fill-up history",
                message = "Your fill-ups appear here, newest first. Each card shows the date, odometer, trip distance, and calculated fuel economy.",
                getTargetView = { binding.recyclerView }
            ),
            TutorialStep(
                title = "Edit or delete",
                message = "Swipe a card right to edit it, or left to delete it. You can also tap a card to open the editor.",
                getTargetView = { lm?.findViewByPosition(1) ?: binding.recyclerView }
            ),
            TutorialStep(
                title = "Fuel economy chart",
                message = "With 2+ fill-ups, a chart appears here showing your trend over time. Tap a dot on the chart to jump to that record in the list.",
                getTargetView = { lm?.findViewByPosition(0) },
                preAction = { binding.recyclerView.scrollToPosition(0) }
            ),
            TutorialStep(
                title = "Settings & cars",
                message = "Tap the car icon to add a new car. Tap the gear icon to open Settings — change units, set fuel cost, currency symbol, and theme.",
                getTargetViews = {
                    listOfNotNull(
                        binding.toolbar.findViewById(R.id.action_cars),
                        binding.toolbar.findViewById(R.id.action_settings)
                    )
                }
            ),
            TutorialStep(
                title = "You're all set!",
                message = "You can reopen this tutorial anytime from Settings → How to Use."
            )
        )

        val overlay = TutorialOverlayView(this, steps)
        overlay.onDismiss = { finish() }
        (window.decorView as ViewGroup).addView(
            overlay,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }
}
