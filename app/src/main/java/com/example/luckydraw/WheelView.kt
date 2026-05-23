package com.example.luckydraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withRotation
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A custom spinning wheel that draws [SLICE_COUNT] equal colored pie slices,
 * each with its number rotated radially so it always faces outward from the center.
 * Rotation (for the spin animation) is applied externally via View.animate().rotation().
 */
class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val SLICE_COUNT = 10
        const val SLICE_ANGLE = 360f / SLICE_COUNT  // 36° per slice
    }

    // One distinct color per slice — ordered 1..10
    private val sliceColors = intArrayOf(
        0xFFE53935.toInt(), // 1  Red
        0xFFF4511E.toInt(), // 2  Deep Orange
        0xFFFDD835.toInt(), // 3  Yellow
        0xFF43A047.toInt(), // 4  Green
        0xFF00897B.toInt(), // 5  Teal
        0xFF1E88E5.toInt(), // 6  Blue
        0xFF3949AB.toInt(), // 7  Indigo
        0xFF8E24AA.toInt(), // 8  Purple
        0xFFD81B60.toInt(), // 9  Pink
        0xFFFF8F00.toInt(), // 10 Amber
    )

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        // Shadow gives readability on bright slices (e.g. Yellow, Amber)
        setShadowLayer(4f, 1f, 1f, Color.argb(200, 0, 0, 0))
    }

    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val hubBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val firePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Semi-transparent red overlay — signals the wheel overheated
        color = Color.argb(170, 255, 50, 0)
        style = Paint.Style.FILL
    }

    private val ovalRect = RectF()

    /**
     * When true, renders a red "on fire" overlay across the entire wheel.
     * Setting this triggers a redraw automatically.
     */
    var isOnFire: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - strokePaint.strokeWidth / 2f

        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        textPaint.textSize = radius * 0.25f  // scales with wheel size; fits "10" comfortably

        // ── 1. Fill slices ────────────────────────────────────────────────
        for (i in 0 until SLICE_COUNT) {
            slicePaint.color = sliceColors[i]
            slicePaint.style = Paint.Style.FILL
            canvas.drawArc(ovalRect, -90f + i * SLICE_ANGLE, SLICE_ANGLE, true, slicePaint)
        }

        // ── 2. Stroke borders (drawn after fills so they sit on top) ──────
        for (i in 0 until SLICE_COUNT) {
            canvas.drawArc(ovalRect, -90f + i * SLICE_ANGLE, SLICE_ANGLE, true, strokePaint)
        }

        // ── 3. Outer rim ──────────────────────────────────────────────────
        canvas.drawCircle(cx, cy, radius, strokePaint)

        // ── 4. Numbers — centered in each slice, rotated radially outward ─
        for (i in 0 until SLICE_COUNT) {
            val midAngle = -90f + i * SLICE_ANGLE + SLICE_ANGLE / 2f
            val textRadius = radius * 0.63f
            val rad = Math.toRadians(midAngle.toDouble())
            val textX = cx + textRadius * cos(rad).toFloat()
            val textY = cy + textRadius * sin(rad).toFloat()

            val centerY = textY - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.withRotation(midAngle + 90f, textX, textY) {
                drawText((i + 1).toString(), textX, centerY, textPaint)
            }
        }

        // ── 5. Center hub ─────────────────────────────────────────────────
        val hubRadius = radius * 0.10f
        canvas.drawCircle(cx, cy, hubRadius, hubPaint)
        canvas.drawCircle(cx, cy, hubRadius, hubBorderPaint)

        // ── 6. Fire overlay (when overheated) ─────────────────────────────
        if (isOnFire) {
            canvas.drawCircle(cx, cy, radius, firePaint)
        }
    }
}



