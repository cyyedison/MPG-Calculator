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
            adapter.submitList(records)
            binding.emptyView.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fab.setOnClickListener { showAddDialog() }

        setupSwipeActions()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        adapter.displayUnit = prefs.getString(SettingsActivity.KEY_DISPLAY_UNIT, SettingsActivity.DEFAULT_DISPLAY_UNIT)
            ?: SettingsActivity.DEFAULT_DISPLAY_UNIT
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
                    ItemTouchHelper.LEFT -> viewModel.delete(record)
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

        existingRecord?.let { record ->
            dialogBinding.etOdometer.setText(record.odometerMiles.toString())
            dialogBinding.etFuel.setText(record.fuelAmount.toString())
            dialogBinding.rgUnit.check(
                when (record.fuelUnit) {
                    "UK_GAL" -> R.id.rbUkGal
                    "US_GAL" -> R.id.rbUsGal
                    else     -> R.id.rbLitres
                }
            )
        }

        val isEdit = existingRecord != null
        MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) R.string.dialog_edit_title else R.string.dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) R.string.dialog_save else R.string.dialog_add) { _, _ ->
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
                if (isEdit) {
                    viewModel.update(existingRecord!!.copy(
                        odometerMiles = odometer,
                        fuelAmount = fuel,
                        fuelUnit = unit
                    ))
                } else {
                    viewModel.insert(FuelRecord(
                        odometerMiles = odometer,
                        fuelAmount = fuel,
                        fuelUnit = unit,
                        timestampMs = System.currentTimeMillis()
                    ))
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
