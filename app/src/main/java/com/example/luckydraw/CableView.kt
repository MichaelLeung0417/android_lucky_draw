package com.example.luckydraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Draws a power plug + socket item that replaces the plain cable Button.
 *
 *  isConnected = false  →  plug pulled away from socket, LED off (red)
 *  isConnected = true   →  plug inserted, LED on (green)
 *
 * The socket is on the right side; the plug head hangs to the left when
 * disconnected and moves flush with the socket when connected.
 */
class CableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Paints ────────────────────────────────────────────────────────────────

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bodyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 30, 30, 30)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val prongPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 200, 200, 210)   // silver prongs
        style = Paint.Style.FILL
    }

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 40, 40, 40)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ledGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(3f, 0f, 1f, Color.argb(180, 0, 0, 0))
    }

    // ── Geometry (pre-computed in onSizeChanged) ──────────────────────────────

    private val socketRect    = RectF()
    private val plugRect      = RectF()   // plug head position — shifts on connect
    private val prong1        = RectF()
    private val prong2        = RectF()
    private val wirePath      = Path()

    // ── State ─────────────────────────────────────────────────────────────────

    var isConnected: Boolean = false
        set(value) {
            field = value
            recomputeGeometry(width, height)
            invalidate()
        }

    // ── Measurement ───────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        recomputeGeometry(w, h)
    }

    private fun recomputeGeometry(w: Int, h: Int) {
        val dp = resources.displayMetrics.density

        bodyBorderPaint.strokeWidth = 1.5f * dp
        wirePaint.strokeWidth       = 3f * dp
        labelPaint.textSize         = (h * 0.22f).coerceAtLeast(10f * dp)

        // Socket: right 30 % of the view, centered vertically
        val sockW = w * 0.28f
        val sockH = h * 0.52f
        val sockLeft = w - sockW - 4f * dp
        val sockTop  = (h - sockH) / 2f
        socketRect.set(sockLeft, sockTop, sockLeft + sockW, sockTop + sockH)

        // Plug body: same height as socket, width ~22 % of view
        val plugW = w * 0.22f
        val plugH = sockH
        val plugLeft = if (isConnected) sockLeft - plugW         // flush against socket
        else            sockLeft - plugW - w * 0.18f   // pulled back (gap)
        val plugTop  = sockTop
        plugRect.set(plugLeft, plugTop, plugLeft + plugW, plugTop + plugH)

        // Prongs — two small rounded rects protruding from right of plug
        val prongW   = plugW * 0.25f
        val prongH   = plugH * 0.18f
        val prongX   = plugRect.right
        val gap      = plugH * 0.14f
        val centerY  = plugRect.centerY()
        prong1.set(prongX, centerY - gap - prongH, prongX + prongW, centerY - gap)
        prong2.set(prongX, centerY + gap,           prongX + prongW, centerY + gap + prongH)

        // Wire: cubic bezier from left of plug toward left edge of view
        wirePath.reset()
        val wireStartX = plugRect.left
        val wireStartY = plugRect.centerY()
        val wireEndX   = 4f * dp
        val wireEndY   = h * 0.82f
        if (isConnected) {
            // Straight taut cable
            wirePath.moveTo(wireStartX, wireStartY)
            wirePath.lineTo(wireEndX, wireEndY)
        } else {
            // Loose drooping cable
            wirePath.moveTo(wireStartX, wireStartY)
            wirePath.cubicTo(
                wireStartX - w * 0.12f, wireStartY + h * 0.3f,
                wireEndX   + w * 0.12f, wireEndY   - h * 0.2f,
                wireEndX, wireEndY
            )
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val dp = resources.displayMetrics.density
        val corner = 4f * dp

        // 1. Wire / cable
        canvas.drawPath(wirePath, wirePaint)

        // 2. Socket body
        bodyPaint.color = Color.argb(230, 55, 55, 60)
        canvas.drawRoundRect(socketRect, corner, corner, bodyPaint)
        canvas.drawRoundRect(socketRect, corner, corner, bodyBorderPaint)

        // Socket holes (two small ovals)
        val holeW    = socketRect.width()  * 0.22f
        val holeH    = socketRect.height() * 0.16f
        val holeCX   = socketRect.left + socketRect.width() * 0.38f
        val gap      = socketRect.height() * 0.14f
        val holeCY1  = socketRect.centerY() - gap - holeH / 2f
        val holeCY2  = socketRect.centerY() + gap + holeH / 2f
        bodyPaint.color = Color.argb(200, 15, 15, 15)
        canvas.drawOval(holeCX - holeW/2, holeCY1 - holeH/2, holeCX + holeW/2, holeCY1 + holeH/2, bodyPaint)
        canvas.drawOval(holeCX - holeW/2, holeCY2 - holeH/2, holeCX + holeW/2, holeCY2 + holeH/2, bodyPaint)

        // 3. Plug body
        bodyPaint.color = if (isConnected) Color.argb(230, 45, 55, 80) else Color.argb(230, 70, 65, 50)
        canvas.drawRoundRect(plugRect, corner, corner, bodyPaint)
        canvas.drawRoundRect(plugRect, corner, corner, bodyBorderPaint)

        // 4. Prongs
        canvas.drawRoundRect(prong1, 2f * dp, 2f * dp, prongPaint)
        canvas.drawRoundRect(prong2, 2f * dp, 2f * dp, prongPaint)

        // 5. LED indicator on the plug body
        val ledRadius = (socketRect.height() * 0.10f).coerceAtLeast(5f * dp)
        val ledCX     = plugRect.centerX()
        val ledCY     = plugRect.top + ledRadius * 1.6f

        if (isConnected) {
            ledGlowPaint.color = Color.argb(60,  0, 230, 80)   // green glow
            ledPaint.color     = Color.argb(230, 30, 200, 60)   // green LED
        } else {
            ledGlowPaint.color = Color.argb(50, 220, 50, 50)    // red glow
            ledPaint.color     = Color.argb(200, 200, 50, 50)   // red LED
        }
        canvas.drawCircle(ledCX, ledCY, ledRadius * 1.8f, ledGlowPaint)
        canvas.drawCircle(ledCX, ledCY, ledRadius, ledPaint)

        // 6. Label below
        val label = if (isConnected) "Plugged ✓" else "Plug me!"
        canvas.drawText(label, width / 2f, height - 3f * dp, labelPaint)
    }
}

