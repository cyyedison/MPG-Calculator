package com.example.mpgcalculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class ConsumptionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var dataPoints: List<Double> = emptyList()
        set(value) { field = value; invalidate() }

    var unitLabel: String = ""
        set(value) { field = value; invalidate() }

    private val dp = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avgLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2f * dp
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeJoin = Paint.Join.ROUND

        dotPaint.style = Paint.Style.FILL

        avgLinePaint.style = Paint.Style.STROKE
        avgLinePaint.strokeWidth = 1.5f * dp
        avgLinePaint.pathEffect = DashPathEffect(floatArrayOf(8f * dp, 4f * dp), 0f)

        axisLabelPaint.textSize = 17f * dp
        axisLabelPaint.textAlign = Paint.Align.RIGHT

        avgLabelPaint.textSize = 18f * dp
        avgLabelPaint.textAlign = Paint.Align.CENTER
        avgLabelPaint.isFakeBoldText = true

        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 0.5f * dp

        val tv = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, tv, true)
        val colorPrimary = tv.data
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true)
        val colorOnSurface = tv.data
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorError, tv, true)
        val colorError = tv.data

        linePaint.color = colorPrimary
        dotPaint.color = colorPrimary
        avgLinePaint.color = colorError
        avgLabelPaint.color = colorError
        axisLabelPaint.color = colorOnSurface
        axisLabelPaint.alpha = 180
        gridPaint.color = colorOnSurface
        gridPaint.alpha = 30
    }

    override fun onDraw(canvas: Canvas) {
        if (dataPoints.size < 2) return

        val n = dataPoints.size
        val minVal = dataPoints.min()
        val maxVal = dataPoints.max()
        val avg = dataPoints.average()

        // Layout regions (all in pixels)
        // axisW == padR so the plot area is horizontally centred in the view
        val axisW = 60f * dp
        val padR = 60f * dp
        val padT = 28f * dp   // space above the plot for the avg label
        val padB = 12f * dp

        val plotLeft = axisW
        val plotRight = width - padR
        val plotTop = padT
        val plotBottom = height.toFloat() - padB
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        // Value range with 10% margin so points aren't clipped at the edges
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal
        val dispMin = minVal - range * 0.1
        val dispMax = maxVal + range * 0.1

        fun xOf(i: Int): Float = plotLeft + i.toFloat() * plotWidth / (n - 1)
        fun yOf(v: Double): Float =
            plotBottom - ((v - dispMin) / (dispMax - dispMin) * plotHeight).toFloat()

        // "Avg: X.X unit" label — above the top grid line, centred on the full view width
        canvas.drawText(
            "Avg: ${"%.1f".format(avg)} $unitLabel",
            width / 2f,
            plotTop - 6f * dp,
            avgLabelPaint
        )

        // Grid lines at top and bottom of plot area
        canvas.drawLine(plotLeft, plotTop, plotRight, plotTop, gridPaint)
        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, gridPaint)

        // Y-axis labels: max at top, min at bottom
        canvas.drawText(
            "%.1f".format(maxVal),
            plotLeft - 4f * dp,
            plotTop + axisLabelPaint.textSize * 0.8f,
            axisLabelPaint
        )
        canvas.drawText(
            "%.1f".format(minVal),
            plotLeft - 4f * dp,
            plotBottom - 2f * dp,
            axisLabelPaint
        )

        // Dashed average line
        val avgY = yOf(avg)
        canvas.drawLine(plotLeft, avgY, plotRight, avgY, avgLinePaint)

        // Connecting line through all points
        path.reset()
        path.moveTo(xOf(0), yOf(dataPoints[0]))
        for (i in 1 until n) {
            path.lineTo(xOf(i), yOf(dataPoints[i]))
        }
        canvas.drawPath(path, linePaint)

        // Filled dot at each data point
        val dotR = 4f * dp
        for (i in 0 until n) {
            canvas.drawCircle(xOf(i), yOf(dataPoints[i]), dotR, dotPaint)
        }
    }
}
