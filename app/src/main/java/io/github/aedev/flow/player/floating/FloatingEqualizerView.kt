package io.github.aedev.flow.player.floating

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class FloatingEqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0033.toInt() // Red visualizer bar matching user screenshot
        style = Paint.Style.FILL
    }

    private val restingFractions = floatArrayOf(0.4f, 0.9f, 0.6f, 0.8f)
    private val currentFractions = floatArrayOf(0.4f, 0.9f, 0.6f, 0.8f)
    private val barRect = RectF()

    private var isAnimating = false
    private var animator: ValueAnimator? = null

    fun setPlaying(playing: Boolean) {
        if (playing) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimation() {
        if (isAnimating) return
        isAnimating = true
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                currentFractions[0] = 0.25f + 0.65f * Math.abs(Math.sin((fraction * Math.PI)).toFloat())
                currentFractions[1] = 0.35f + 0.60f * Math.abs(Math.cos((fraction * Math.PI)).toFloat())
                currentFractions[2] = 0.20f + 0.75f * Math.abs(Math.sin(((fraction + 0.5f) * Math.PI)).toFloat())
                currentFractions[3] = 0.40f + 0.55f * Math.abs(Math.cos(((fraction + 0.25f) * Math.PI)).toFloat())
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        isAnimating = false
        animator?.cancel()
        animator = null
        for (i in restingFractions.indices) {
            currentFractions[i] = restingFractions[i]
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val barCount = 4
        val totalSpacing = (barCount - 1) * (w * 0.12f)
        val barWidth = (w - totalSpacing) / barCount
        val cornerRadius = barWidth / 2f

        var left = 0f
        for (i in 0 until barCount) {
            val barH = (h * currentFractions[i]).coerceIn(cornerRadius * 2f, h)
            val top = h - barH
            val right = left + barWidth
            val bottom = h
            barRect.set(left, top, right, bottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)
            left += barWidth + (w * 0.12f)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
