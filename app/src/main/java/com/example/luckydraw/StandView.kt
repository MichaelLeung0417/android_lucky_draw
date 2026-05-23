package com.example.luckydraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

/**
 * Draws a two-part metallic stand (pillar + base) behind the wheel.
 * A bolt indicator on the pillar shows loose (red) vs fixed (green).
 * Tap the view to fix the stand — wired in MainActivity.
 */
class StandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Paints ────────────────────────────────────────────────────────────────

    private val pillarPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#2E3F49".toColorInt()
        style = Paint.Style.FILL
    }

    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        style = Paint.Style.STROKE
    }

    /** Bolt fill — red when loose, green when fixed. */
    private val boltFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** Dark border ring around the bolt head. */
    private val boltBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A2A30".toColorInt()
        style = Paint.Style.STROKE
    }

    /** Crosshead slot lines on the bolt face. */
    private val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 15, 15, 15)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    private val pillarRect = RectF()
    private val baseRect   = RectF()
    private var cornerRadius = 0f
    private var boltRadius   = 0f
    private var boltCx       = 0f
    private var boltCy       = 0f

    // ── State ─────────────────────────────────────────────────────────────────

    /** Toggle to refresh bolt color — red = loose, green = fixed. */
    var isFixed: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // ── Measurement ───────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        val dp   = resources.displayMetrics.density
        val cx   = w / 2f

        val pillarW = w * 0.055f
        val baseW   = w * 0.50f
        val baseH   = 14f * dp

        cornerRadius            = 5f * dp
        specularPaint.strokeWidth = dp

        pillarRect.set(cx - pillarW / 2f, 0f, cx + pillarW / 2f, h - baseH)
        baseRect.set(cx - baseW / 2f, h - baseH, cx + baseW / 2f, h.toFloat())

        // Bolt sits mid-pillar — clearly tappable and clearly part of the structure
        boltRadius = (pillarW * 2.4f).coerceAtLeast(10f * dp)
        boltCx = cx
        boltCy = pillarRect.top + (pillarRect.height() * 0.35f)

        boltBorderPaint.strokeWidth = 2f * dp
        slotPaint.strokeWidth       = 2.5f * dp

        pillarPaint.shader = LinearGradient(
            pillarRect.left, 0f, pillarRect.right, 0f,
            intArrayOf(
                "#37474F".toColorInt(),
                "#B0BEC5".toColorInt(),
                "#546E7A".toColorInt(),
                "#2E3F49".toColorInt(),
            ),
            floatArrayOf(0f, 0.30f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        // 1. Metallic pillar
        canvas.drawRect(pillarRect, pillarPaint)

        // 2. Rounded ground base
        canvas.drawRoundRect(baseRect, cornerRadius, cornerRadius, basePaint)

        // 3. Specular highlight on base top edge
        val hlY = baseRect.top + specularPaint.strokeWidth
        canvas.drawLine(
            baseRect.left  + cornerRadius, hlY,
            baseRect.right - cornerRadius, hlY,
            specularPaint
        )

        // 4. Bolt indicator — color communicates loose vs fixed
        boltFillPaint.color = if (isFixed)
            Color.argb(230, 76, 175, 80)    // green  — secured
        else
            Color.argb(230, 229, 57, 53)    // red    — needs tightening

        canvas.drawCircle(boltCx, boltCy, boltRadius, boltFillPaint)
        canvas.drawCircle(boltCx, boltCy, boltRadius, boltBorderPaint)

        // Crosshead slot (Phillips screwhead) — visual affordance for "tap to tighten"
        val slotLen = boltRadius * 0.58f
        canvas.drawLine(boltCx - slotLen, boltCy, boltCx + slotLen, boltCy, slotPaint)
        canvas.drawLine(boltCx, boltCy - slotLen, boltCx, boltCy + slotLen, slotPaint)
    }
}
