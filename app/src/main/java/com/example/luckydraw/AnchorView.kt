package com.example.luckydraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Downward-pointing triangle pointer that sits at the wheel's 12-o'clock.
 * Draws its own stability indicator:
 *   isStable = false  →  red fill + "!" badge  (needs tapping)
 *   isStable = true   →  green fill + "✓" badge (anchor secured)
 *
 * Setting [isStable] triggers invalidate() automatically.
 */
class AnchorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Paints ────────────────────────────────────────────────────────────────

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    private val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(2f, 0f, 1f, Color.argb(180, 0, 0, 0))
    }

    // ── Triangle paths (preallocated — reused every draw call) ────────────────

    private val trianglePath = Path()   // inset colored fill
    private val borderPath   = Path()   // full-size white border

    // ── State ─────────────────────────────────────────────────────────────────

    var isStable: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // ── Measurement ───────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        val dp = resources.displayMetrics.density
        val inset = 3f * dp

        // Full-size white border triangle
        borderPath.reset()
        borderPath.moveTo(0f, 0f)
        borderPath.lineTo(w.toFloat(), 0f)
        borderPath.lineTo(w / 2f, h.toFloat())
        borderPath.close()

        // Inset colored fill triangle
        trianglePath.reset()
        trianglePath.moveTo(inset, inset)
        trianglePath.lineTo(w - inset, inset)
        trianglePath.lineTo(w / 2f, h - inset)
        trianglePath.close()

        borderPaint.strokeWidth   = 3f * dp
        badgeBorderPaint.strokeWidth = 1.5f * dp
        badgeTextPaint.textSize   = (h * 0.36f).coerceAtLeast(12f * dp)
    }

    // ── Drawing ─────────────────────────────────────���─────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val dp = resources.displayMetrics.density
        val cx = width / 2f

        // 1. White border triangle (full size, drawn first)
        canvas.drawPath(borderPath, whiteFillPaint)

        // 2. Colored fill triangle (inset reveals white border)
        fillPaint.color = if (isStable)
            Color.argb(235, 56, 142, 60)    // green — stable
        else
            Color.argb(235, 211, 47, 47)    // red   — unstable

        canvas.drawPath(trianglePath, fillPaint)

        // 3. Badge circle in the upper-center of the triangle (easy to spot)
        val badgeRadius = (width * 0.18f).coerceAtLeast(8f * dp)
        val badgeCx = cx
        val badgeCy = height * 0.35f

        badgeFillPaint.color = if (isStable)
            Color.argb(255, 27, 94, 32)     // darker green badge background
        else
            Color.argb(255, 183, 28, 28)    // darker red badge background

        canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgeFillPaint)
        canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgeBorderPaint)

        // 4. Badge symbol — "!" when unstable, "✓" when stable
        val symbol = if (isStable) "✓" else "!"
        val textY = badgeCy - (badgeTextPaint.ascent() + badgeTextPaint.descent()) / 2f
        canvas.drawText(symbol, badgeCx, textY, badgeTextPaint)
    }
}

