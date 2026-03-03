package com.eddiec.mpgcalculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import com.eddiec.mpgcalculator.databinding.LayoutTutorialBubbleBinding

class TutorialOverlayView(
    context: Context,
    private val steps: List<TutorialStep>
) : FrameLayout(context) {

    var onDismiss: (() -> Unit)? = null

    private var currentStep = 0
    private val dp = resources.displayMetrics.density

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 0, 0)
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val spotlightRect = RectF()
    private val spotlightCorner = 16f * dp
    private val spotlightPad = 16f * dp

    private val bubbleBinding = LayoutTutorialBubbleBinding.inflate(
        LayoutInflater.from(context), this, false
    )

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        addView(bubbleBinding.root)
        setOnClickListener { advance() }
        showStep(0)
    }

    private fun advance() = showStep(currentStep + 1)

    private fun showStep(index: Int) {
        if (index >= steps.size) {
            (parent as? ViewGroup)?.removeView(this)
            onDismiss?.invoke()
            return
        }
        currentStep = index
        val step = steps[index]

        fun applyStep() {
            computeSpotlight(step)
            bubbleBinding.tvTutorialTitle.text = step.title
            bubbleBinding.tvTutorialMessage.text = step.message
            bubbleBinding.tvTutorialStep.text = "${index + 1} / ${steps.size}"
            bubbleBinding.tvTutorialHint.text =
                if (index == steps.size - 1) "Tap anywhere to finish"
                else "Tap anywhere to continue"
        }

        step.preAction?.invoke()
        if (step.preAction != null) {
            postDelayed({ if (isAttachedToWindow) applyStep() }, 150)
        } else {
            applyStep()
        }
    }

    private fun computeSpotlight(step: TutorialStep) {
        val loc = IntArray(2)
        val targets: List<android.view.View> = when {
            step.getTargetViews != null -> step.getTargetViews.invoke().filter { it.isShown }
            step.getTargetView != null  -> listOfNotNull(step.getTargetView.invoke()?.takeIf { it.isShown })
            else                        -> emptyList()
        }
        if (targets.isNotEmpty()) {
            var left   = Float.MAX_VALUE
            var top    = Float.MAX_VALUE
            var right  = -Float.MAX_VALUE
            var bottom = -Float.MAX_VALUE
            for (t in targets) {
                t.getLocationOnScreen(loc)
                left   = minOf(left,   loc[0].toFloat())
                top    = minOf(top,    loc[1].toFloat())
                right  = maxOf(right,  (loc[0] + t.width).toFloat())
                bottom = maxOf(bottom, (loc[1] + t.height).toFloat())
            }
            spotlightRect.set(left - spotlightPad, top - spotlightPad, right + spotlightPad, bottom + spotlightPad)
        } else {
            spotlightRect.setEmpty()
        }
        invalidate()
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        if (!spotlightRect.isEmpty) {
            canvas.drawRoundRect(spotlightRect, spotlightCorner, spotlightCorner, clearPaint)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val bubble = bubbleBinding.root
        bubble.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val bw = bubble.measuredWidth
        val bh = bubble.measuredHeight
        val screenW = r - l
        val screenH = b - t
        val margin = (20 * dp).toInt()
        val edge = (12 * dp).toInt()

        // Horizontally center, clamped to screen edges
        var bl = screenW / 2 - bw / 2
        var br = bl + bw
        if (bl < edge) { bl = edge; br = bl + bw }
        if (br > screenW - edge) { br = screenW - edge; bl = br - bw }

        // Vertically: below spotlight if in top half, above if in bottom half, center if no spotlight
        val bt: Int
        val bb: Int
        when {
            spotlightRect.isEmpty -> {
                bt = screenH / 2 - bh / 2
                bb = bt + bh
            }
            spotlightRect.centerY() < screenH / 2f -> {
                bt = (spotlightRect.bottom.toInt() + margin).coerceAtMost(screenH - bh - edge)
                bb = bt + bh
            }
            else -> {
                bb = (spotlightRect.top.toInt() - margin).coerceAtLeast(bh + edge)
                bt = bb - bh
            }
        }

        bubble.layout(bl, bt, br, bb)
    }

    // Consume all touch events so nothing beneath the overlay is activated
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }
}
