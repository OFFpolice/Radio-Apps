package com.example.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class PulseLoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = 0xFFE91E63.toInt()
    }

    private var scale1 = 1.0f
    private var alpha1 = 0.8f
    private var scale2 = 1.0f
    private var alpha2 = 0.4f

    private var animator: ValueAnimator? = null

    init {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                
                scale1 = 1.0f + fraction * 0.6f
                alpha1 = 0.8f * (1f - fraction)

                val shiftedFraction = (fraction + 0.5f) % 1.0f
                scale2 = 1.0f + shiftedFraction * 0.6f
                alpha2 = 0.4f * (1f - shiftedFraction)

                invalidate()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = Math.min(cx, cy) * 0.5f

        paint.color = (0xFFE91E63.toInt() and 0x00FFFFFF) or ((alpha1 * 255).toInt() shl 24)
        canvas.drawCircle(cx, cy, baseRadius * scale1, paint)

        paint.color = (0xFFE91E63.toInt() and 0x00FFFFFF) or ((alpha2 * 255).toInt() shl 24)
        canvas.drawCircle(cx, cy, baseRadius * scale2, paint)
    }
}
