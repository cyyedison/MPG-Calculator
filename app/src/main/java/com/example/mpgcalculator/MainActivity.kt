package com.example.mpgcalculator

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpgcalculator.data.FuelRecord
import com.example.mpgcalculator.databinding.ActivityMainBinding
import com.example.mpgcalculator.databinding.DialogAddRecordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val adapter = FuelRecordAdapter()

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
        binding.recyclerView.adapter = adapter

        viewModel.records.observe(this) { records ->
            // Capture size before DiffUtil commits so we can detect an insertion.
            val previousSize = adapter.currentList.size
            // Use the commit callback to force a full rebind after DiffUtil settles.
            // This is necessary because each card's MPG depends on its *neighbour's*
            // odometer, so a single update affects both the updated card and the one
            // immediately after it in the list.
            adapter.submitList(records) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
                // Scroll to top so the newest record (position 0) is visible after an insert.
                if (records.size > previousSize) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
            binding.emptyView.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
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

        val vehicleName = prefs.getString(SettingsActivity.KEY_VEHICLE_NAME, "") ?: ""
        supportActionBar?.subtitle = vehicleName.ifEmpty { null }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                        // Restore item visually while awaiting confirmation
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
                    // Swipe right — edit (blue)
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
                    // Swipe left — delete (red)
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

        // Wire odometer unit toggle → update hint text
        dialogBinding.rgOdometerUnit.setOnCheckedChangeListener { _, checkedId ->
            dialogBinding.tilOdometer.hint = getString(
                if (checkedId == R.id.rbKm) R.string.hint_odometer_km else R.string.hint_odometer_miles
            )
        }

        if (existingRecord != null) {
            // Editing — restore the exact unit and value the user originally typed
            if (existingRecord.odometerUnit == "KM") {
                dialogBinding.rgOdometerUnit.check(R.id.rbKm)
                dialogBinding.tilOdometer.hint = getString(R.string.hint_odometer_km)
                // miles × 1.60934 = km
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
            // New record — apply saved defaults
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
                // 1 mile = 1.60934 km  →  km ÷ 1.60934 = miles
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
