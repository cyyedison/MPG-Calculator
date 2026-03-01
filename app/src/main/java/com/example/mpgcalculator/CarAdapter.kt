package com.example.mpgcalculator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mpgcalculator.data.Car
import com.example.mpgcalculator.databinding.ItemCarBinding

class CarAdapter(
    private val onSelect: (Car) -> Unit,
    private val onEdit: (Car) -> Unit
) : ListAdapter<Car, CarAdapter.ViewHolder>(DIFF) {

    var selectedCarId: Long = -1L
        set(value) { field = value; notifyDataSetChanged() }

    inner class ViewHolder(private val binding: ItemCarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            binding.tvCarName.text = car.name
            binding.rbSelected.isChecked = car.id == selectedCarId
            binding.root.setOnClickListener { onSelect(car) }
            binding.btnEditCar.setOnClickListener { onEdit(car) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Car>() {
            override fun areItemsTheSame(a: Car, b: Car) = a.id == b.id
            override fun areContentsTheSame(a: Car, b: Car) = a == b
        }
    }
}
