package com.example.mpgcalculator

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpgcalculator.data.Car
import com.example.mpgcalculator.data.FuelRecord
import com.example.mpgcalculator.databinding.ActivityMainBinding
import com.example.mpgcalculator.databinding.DialogAddRecordBinding
import com.example.mpgcalculator.databinding.DialogManageCarsBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val adapter = FuelRecordAdapter()
    private val chartAdapter = ChartHeaderAdapter()
    private var latestRecords: List<FuelRecord> = emptyList()

    private val swipePaint = Paint()
    private var deleteIcon: Drawable? = null
    private var editIcon: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setSupportActionBar(binding.toolbar)

        deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)
        editIcon = ContextCompat.getDrawable(this, R.drawable.ic_edit)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ConcatAdapter(chartAdapter, adapter)

        viewModel.records.observe(this) { records ->
            latestRecords = records
            val previousSize = adapter.currentList.size
            adapter.submitList(records) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
                if (records.size > previousSize) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
            binding.emptyView.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            updateChart()
        }

        // Update chip strip whenever the car list or selection changes
        viewModel.cars.observe(this) { cars -> rebuildCarChips(cars) }
        viewModel.selectedCarId.observe(this) {
            viewModel.cars.value?.let { rebuildCarChips(it) }
        }

        adapter.onItemClick = { record -> showAddDialog(record) }

        binding.fab.setOnClickListener { showAddDialog() }

        binding.fabDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_all_title)
                .setMessage(R.string.dialog_delete_all_msg)
                .setPositiveButton(R.string.dialog_delete) { _, _ -> viewModel.deleteAll() }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        setupSwipeActions()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        adapter.displayUnit = prefs.getString(
            SettingsActivity.KEY_DISPLAY_UNIT, SettingsActivity.DEFAULT_DISPLAY_UNIT
        ) ?: SettingsActivity.DEFAULT_DISPLAY_UNIT
        adapter.costPerLitre = prefs.getFloat(SettingsActivity.KEY_COST_PER_LITRE, 0f).toDouble()
        updateChart()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cars -> {
                showAddCarDialog()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── Car chip strip ────────────────────────────────────────────────────────

    private fun rebuildCarChips(cars: List<Car>) {
        val container = binding.carChipContainer
        container.removeAllViews()
        val selectedId = viewModel.selectedCarId.value ?: 1L
        val chipMargin = (4 * resources.displayMetrics.density).toInt()
        for (car in cars) {
            val chip = Chip(this).apply {
                text = car.name
                isCheckable = true
                isChecked = car.id == selectedId
                // Prevent the chip from toggling its own checked state on click —
                // we manage selection state explicitly via rebuildCarChips.
                setOnCheckedChangeListener { chip, checked ->
                    if (!checked && car.id == selectedId) chip.isChecked = true
                }
                setOnClickListener {
                    if (car.id == selectedId) showRenameCarDialog(car)
                    else viewModel.selectCar(car.id)
                }
                setOnLongClickListener { showRenameCarDialog(car); true }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = chipMargin }
            container.addView(chip, params)
        }
    }

    private fun showAddCarDialog() {
        val defaultName = "Car ${(viewModel.cars.value?.size ?: 0) + 1}"
        val input = EditText(this).apply {
            setText(defaultName)
            selectAll()
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val container = android.widget.FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, 0, p, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cars_add)
            .setView(container)
            .setPositiveButton(R.string.dialog_add) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.insertCar(name)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showManageCarsDialog() {
        val dialogBinding = DialogManageCarsBinding.inflate(LayoutInflater.from(this))
        val carAdapter = CarAdapter(
            onSelect = { car ->
                viewModel.selectCar(car.id)
            },
            onEdit = { car ->
                showRenameCarDialog(car)
            }
        )
        carAdapter.selectedCarId = viewModel.selectedCarId.value ?: 1L
        dialogBinding.rvCars.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvCars.adapter = carAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cars_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_cancel, null)
            .create()

        // Observe car list while dialog is open
        val observer = androidx.lifecycle.Observer<List<Car>> { cars ->
            carAdapter.selectedCarId = viewModel.selectedCarId.value ?: 1L
            carAdapter.submitList(cars)
        }
        viewModel.cars.observe(this, observer)
        viewModel.selectedCarId.observe(this) { id ->
            carAdapter.selectedCarId = id
        }

        dialog.setOnDismissListener {
            viewModel.cars.removeObserver(observer)
        }

        dialogBinding.btnAddCar.setOnClickListener {
            viewModel.addCar()
        }

        dialog.show()
    }

    private fun showRenameCarDialog(car: Car) {
        val cars = viewModel.cars.value ?: emptyList()
        val canDelete = cars.size > 1

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(car.name)
            selectAll()
            hint = getString(R.string.cars_rename_hint)
            setPadding(
                (20 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (4 * resources.displayMetrics.density).toInt()
            )
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cars_rename)
            .setView(input)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val newName = input.text?.toString()?.trim()
                if (!newName.isNullOrEmpty()) {
                    viewModel.renameCar(car, newName)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)

        if (canDelete) {
            builder.setNeutralButton(R.string.cars_delete) { _, _ ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cars_delete_confirm_title)
                    .setMessage(R.string.cars_delete_confirm_msg)
                    .setPositiveButton(R.string.dialog_delete) { _, _ ->
                        viewModel.deleteCar(car)
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            }
        }

        builder.show()
    }

    private fun updateChart() {
        val displayUnit = adapter.displayUnit
        val points = mutableListOf<Double>()
        for (i in 0 until latestRecords.size - 1) {
            val record = latestRecords[i]
            val prev = latestRecords[i + 1]
            if (!record.isPartial) {
                val tripMiles = record.odometerMiles - prev.odometerMiles
                val v = FuelRecordAdapter.computeConsumptionValue(
                    tripMiles, record.fuelAmount, record.fuelUnit, displayUnit
                )
                if (v != null && v > 0) points.add(v)
            }
        }
        chartAdapter.setData(points.reversed(), FuelRecordAdapter.displayUnitLabel(displayUnit))
    }

    private fun setupSwipeActions() {
        val iconSize = (24 * resources.displayMetrics.density).toInt()
        val iconPadding = (20 * resources.displayMetrics.density).toInt()

        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val record = adapter.currentList[pos]
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        adapter.notifyItemChanged(pos)
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle(R.string.dialog_delete_confirm_title)
                            .setMessage(R.string.dialog_delete_confirm_msg)
                            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                                viewModel.delete(record)
                            }
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .show()
                    }
                    ItemTouchHelper.RIGHT -> {
                        adapter.notifyItemChanged(pos)
                        showAddDialog(record)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX > 0) {
                    swipePaint.color = Color.parseColor("#2196F3")
                    c.drawRect(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat(), swipePaint
                    )
                    editIcon?.let { icon ->
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        val iconLeft = itemView.left + iconPadding
                        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        icon.draw(c)
                    }
                } else if (dX < 0) {
                    swipePaint.color = Color.parseColor("#F44336")
                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), swipePaint
                    )
                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        val iconLeft = itemView.right - iconPadding - iconSize
                        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        helper.attachToRecyclerView(binding.recyclerView)
    }

    private fun showAddDialog(existingRecord: FuelRecord? = null) {
        val dialogBinding = DialogAddRecordBinding.inflate(LayoutInflater.from(this))
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)

        dialogBinding.rgOdometerUnit.setOnCheckedChangeListener { _, checkedId ->
            dialogBinding.tilOdometer.hint = getString(
                if (checkedId == R.id.rbKm) R.string.hint_odometer_km else R.string.hint_odometer_miles
            )
        }

        if (existingRecord != null) {
            if (existingRecord.odometerUnit == "KM") {
                dialogBinding.rgOdometerUnit.check(R.id.rbKm)
                dialogBinding.tilOdometer.hint = getString(R.string.hint_odometer_km)
                dialogBinding.etOdometer.setText("%.1f".format(existingRecord.odometerMiles * 1.60934))
            } else {
                dialogBinding.etOdometer.setText("%.1f".format(existingRecord.odometerMiles))
            }
            dialogBinding.etFuel.setText(existingRecord.fuelAmount.toString())
            dialogBinding.rgUnit.check(
                when (existingRecord.fuelUnit) {
                    "UK_GAL" -> R.id.rbUkGal
                    "US_GAL" -> R.id.rbUsGal
                    else     -> R.id.rbLitres
                }
            )
            dialogBinding.cbMissedFillups.isChecked = existingRecord.isPartial
        } else {
            val defaultOdo = prefs.getString(
                SettingsActivity.KEY_DEFAULT_ODOMETER_UNIT, SettingsActivity.DEFAULT_ODOMETER_UNIT
            )
            if (defaultOdo == "KM") {
                dialogBinding.rgOdometerUnit.check(R.id.rbKm)
                dialogBinding.tilOdometer.hint = getString(R.string.hint_odometer_km)
            }

            val defaultFuel = prefs.getString(
                SettingsActivity.KEY_DEFAULT_FUEL_UNIT, SettingsActivity.DEFAULT_FUEL_UNIT
            )
            dialogBinding.rgUnit.check(
                when (defaultFuel) {
                    "UK_GAL" -> R.id.rbUkGal
                    "US_GAL" -> R.id.rbUsGal
                    else     -> R.id.rbLitres
                }
            )
        }

        val isEdit = existingRecord != null
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) R.string.dialog_edit_title else R.string.dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) R.string.dialog_save else R.string.dialog_add) { _, _ ->
                val odoText = dialogBinding.etOdometer.text?.toString()?.trim()
                val fuelText = dialogBinding.etFuel.text?.toString()?.trim()
                if (odoText.isNullOrEmpty() || fuelText.isNullOrEmpty()) {
                    Toast.makeText(this, R.string.error_fields_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val odoInput = odoText.toDoubleOrNull()
                val fuel = fuelText.toDoubleOrNull()
                if (odoInput == null || fuel == null) {
                    Toast.makeText(this, R.string.error_invalid_number, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val isKm = dialogBinding.rgOdometerUnit.checkedRadioButtonId == R.id.rbKm
                val odometerMiles = if (isKm) odoInput / 1.60934 else odoInput
                val odometerUnit = if (isKm) "KM" else "MILES"
                val unit = when (dialogBinding.rgUnit.checkedRadioButtonId) {
                    R.id.rbUkGal  -> "UK_GAL"
                    R.id.rbUsGal  -> "US_GAL"
                    else          -> "LITRES"
                }
                val isPartial = dialogBinding.cbMissedFillups.isChecked
                if (isEdit) {
                    viewModel.update(existingRecord!!.copy(
                        odometerMiles = odometerMiles,
                        odometerUnit = odometerUnit,
                        fuelAmount = fuel,
                        fuelUnit = unit,
                        isPartial = isPartial
                    ))
                } else {
                    viewModel.insert(FuelRecord(
                        carId = viewModel.selectedCarId.value ?: 1L,
                        odometerMiles = odometerMiles,
                        odometerUnit = odometerUnit,
                        fuelAmount = fuel,
                        fuelUnit = unit,
                        timestampMs = System.currentTimeMillis(),
                        isPartial = isPartial
                    ))
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)

        if (isEdit) {
            builder.setNeutralButton(R.string.dialog_delete) { _, _ ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_delete_confirm_title)
                    .setMessage(R.string.dialog_delete_confirm_msg)
                    .setPositiveButton(R.string.dialog_delete) { _, _ ->
                        viewModel.delete(existingRecord!!)
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            }
        }

        builder.show()
    }
}
