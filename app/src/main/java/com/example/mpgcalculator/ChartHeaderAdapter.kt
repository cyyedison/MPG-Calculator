package com.eddiec.mpgcalculator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eddiec.mpgcalculator.databinding.ItemChartHeaderBinding

class ChartHeaderAdapter : RecyclerView.Adapter<ChartHeaderAdapter.ViewHolder>() {

    private var data: List<Double> = emptyList()
    private var recordIndices: List<Int> = emptyList()
    private var unitLabel: String = ""
    private var isVisible = false

    /** Called with the adapter position in the fuel records list when a chart point is tapped. */
    var onBarTapped: ((Int) -> Unit)? = null

    fun setData(points: List<Double>, indices: List<Int>, label: String) {
        val wasVisible = isVisible
        data = points
        recordIndices = indices
        unitLabel = label
        isVisible = points.size >= 2
        when {
            !wasVisible && isVisible -> notifyItemInserted(0)
            wasVisible && !isVisible -> notifyItemRemoved(0)
            isVisible -> notifyItemChanged(0)
        }
    }

    override fun getItemCount() = if (isVisible) 1 else 0

    inner class ViewHolder(private val binding: ItemChartHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.chartView.dataPoints = data
            binding.chartView.unitLabel = unitLabel
            binding.chartView.onPointTapped = { chartIdx ->
                val adapterPos = recordIndices.getOrNull(chartIdx)
                if (adapterPos != null) onBarTapped?.invoke(adapterPos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChartHeaderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }
}
