package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class EqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 7
    private val barHeights = FloatArray(barCount) { 0.2f }
    private val animators = mutableListOf<ValueAnimator>()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rBounds = RectF()

    private val animTimes = floatArrayOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.6f, 0.5f)

    init {
        for (i in 0 until barCount) {
            val animator = ValueAnimator.ofFloat(0.2f, 1.0f).apply {
                duration = (1000 * animTimes[i % animTimes.size]).toLong()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    barHeights[i] = animation.animatedValue as Float
                    invalidate()
                }
            }
            animators.add(animator)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animators.forEach { it.start() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animators.forEach { it.cancel() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth == 0f || viewHeight == 0f) return

        val spacing = resources.displayMetrics.density * 5f
        val barWidth = (viewWidth - (spacing * (barCount - 1))) / barCount

        val secondaryPink = 0xFFC2185B.toInt()
        val lightPink = 0xFFF06292.toInt()

        barPaint.shader = LinearGradient(
            0f, viewHeight, 0f, 0f,
            intArrayOf(secondaryPink, lightPink),
            null,
            Shader.TileMode.CLAMP
        )

        for (i in 0 until barCount) {
            val barHeightVal = barHeights[i] * viewHeight
            val left = i * (barWidth + spacing)
            val right = left + barWidth
            val top = viewHeight - barHeightVal
            val bottom = viewHeight

            rBounds.set(left, top, right, bottom)
            canvas.drawRoundRect(rBounds, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }
}
