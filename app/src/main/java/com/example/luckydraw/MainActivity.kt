package com.example.luckydraw

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var isSpinning = false
    private var isPowered = false
    private var isAnchorStable = false
    private var isStandFixed = false
    private var wheelExists = true

    private var anchorTapCount = 0
    private var overheat = 0
    private var lastStartTapMs = 0L

    private lateinit var playButton: Button
    private lateinit var luckyNumberTV: TextView
    private lateinit var statusTV: TextView
    private lateinit var cableTV: Button
    private lateinit var anchorTV: Button
    private lateinit var standTV: Button
    private lateinit var wheelTV: TextView
    private lateinit var dimOverlay: View
    private var puzzleHintAnimator: AnimatorSet? = null
    private var startHintAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        playButton = findViewById(R.id.playTV)
        luckyNumberTV = findViewById(R.id.luckyNumberTV)
        statusTV = findViewById(R.id.statusTV)
        cableTV = findViewById(R.id.cableTV)
        anchorTV = findViewById(R.id.anchorTV)
        standTV = findViewById(R.id.standTV)
        wheelTV = findViewById(R.id.wheelTV)
        dimOverlay = findViewById(R.id.dimOverlay)

        cableTV.setOnClickListener { plugPowerCable() }
        anchorTV.setOnClickListener { stabilizeAnchor() }
        standTV.setOnClickListener { fixStand() }

        resetRound(fullReset = true)
    }

    // ──────────────────────────────────────────────────
    //  Room lighting
    // ──────────────────────────────────────────────────

    /** Fade the darkness out — called when cable is connected. */
    private fun lightUpRoom() {
        dimOverlay.animate()
            .alpha(0f)
            .setDuration(900)
            .start()
        playButton.backgroundTintList = tintFor(R.color.start_glow)
    }

    /** Instantly restore darkness — called on round reset. */
    private fun dimRoom() {
        dimOverlay.animate().cancel()
        dimOverlay.alpha = ROOM_DIM_ALPHA
        playButton.backgroundTintList = null
    }

    // ──────────────────────────────────────────────────
    //  Button enable/disable
    // ──────────────────────────────────────────────────

    private fun disablePlayButton() {
        playButton.isEnabled = false
        stopStartPulse()
    }

    private fun enablePlayButton() {
        playButton.isEnabled = true
        updateGuidanceAnimations()
    }

    // ──────────────────────────────────────────────────
    //  Round state
    // ──────────────────────────────────────────────────

    private fun resetRound(fullReset: Boolean) {
        isSpinning = false
        isPowered = false
        isAnchorStable = false
        isStandFixed = false
        anchorTapCount = 0
        overheat = 0
        lastStartTapMs = 0L
        wheelExists = true

        luckyNumberTV.text = "?"
        statusTV.text = getString(R.string.status_no_power)
        cableTV.text = getString(R.string.cable_loose)
        anchorTV.text = getString(R.string.anchor_unstable)
        standTV.text = getString(R.string.stand_loose)
        wheelTV.visibility = View.VISIBLE
        wheelTV.rotation = 0f
        anchorTV.rotation = 0f
        wheelTV.scaleX = 1f
        wheelTV.scaleY = 1f
        disablePlayButton()
        dimRoom()
        updatePuzzleVisuals()
        updateGuidanceAnimations()

        if (!fullReset) {
            statusTV.text = getString(R.string.status_reset)
        }
    }

    // ──────────────────────────────────────────────────
    //  Puzzle interactions
    // ──────────────────────────────────────────────────

    private fun plugPowerCable() {
        if (isPowered) {
            statusTV.text = getString(R.string.status_already_powered)
            return
        }
        isPowered = true
        cableTV.text = getString(R.string.cable_connected)
        enablePlayButton()
        statusTV.text = getString(R.string.status_powered)
        updatePuzzleVisuals()
        lightUpRoom()
    }

    private fun stabilizeAnchor() {
        if (isAnchorStable) {
            statusTV.text = getString(R.string.status_anchor_stable)
            return
        }
        anchorTapCount += 1
        statusTV.text = getString(R.string.status_anchor_progress, anchorTapCount, REQUIRED_ANCHOR_TAPS)
        if (anchorTapCount >= REQUIRED_ANCHOR_TAPS) {
            isAnchorStable = true
            anchorTV.text = getString(R.string.anchor_stable)
            statusTV.text = getString(R.string.status_anchor_done)
            anchorTV.rotation = 0f
            updatePuzzleVisuals()
            updateGuidanceAnimations()
        }
    }

    private fun fixStand() {
        if (isStandFixed) {
            statusTV.text = getString(R.string.status_stand_fixed)
            return
        }
        isStandFixed = true
        standTV.text = getString(R.string.stand_fixed)
        statusTV.text = getString(R.string.status_stand_done)
        updatePuzzleVisuals()
        updateGuidanceAnimations()
    }

    // ──────────────────────────────────────────────────
    //  Spin logic
    // ──────────────────────────────────────────────────

    private fun updateSpamOverheat() {
        val now = System.currentTimeMillis()
        overheat = if (now - lastStartTapMs <= SPAM_WINDOW_MS) overheat + 1 else 1
        lastStartTapMs = now
    }

    private fun tryStartSpin() {
        if (!wheelExists) {
            statusTV.text = getString(R.string.status_wheel_missing)
            return
        }
        if (!isPowered) {
            statusTV.text = getString(R.string.status_no_power)
            return
        }
        if (!isAnchorStable) {
            wobbleAnchorHint()
            statusTV.text = getString(R.string.status_anchor_needed)
            return
        }
        if (!isStandFixed) {
            triggerStandCollapse()
            return
        }
        if (overheat >= FIRE_THRESHOLD) {
            triggerFireFailure()
            return
        }
        spinWheel()
    }

    private fun triggerStandCollapse() {
        wheelTV.animate().translationYBy(120f).rotationBy(45f).setDuration(220).start()
        statusTV.text = getString(R.string.status_stand_collapse)
        disablePlayButton()
        updateGuidanceAnimations()
        wheelTV.postDelayed({
            wheelTV.translationY = 0f
            wheelTV.rotation = 0f
            enablePlayButton()
            statusTV.text = getString(R.string.status_after_collapse)
            updateGuidanceAnimations()
        }, 900)
    }

    private fun triggerFireFailure() {
        isSpinning = false               // clear spin state before animation cancel
        wheelTV.animate().cancel()       // stop any in-progress spin animation
        wheelExists = false
        wheelTV.visibility = View.INVISIBLE
        statusTV.text = getString(R.string.status_fire)
        luckyNumberTV.text = "X"
        disablePlayButton()
        stopPuzzleHint()
        wheelTV.postDelayed({ resetRound(fullReset = false) }, 1500)
    }

    private fun spinWheel() {
        isSpinning = true
        // Keep Start button enabled — player can still spam during spin to build overheat
        val speedMultiplier = 1f + (overheat * 0.2f)
        val finalRotation = wheelTV.rotation + (1080f * speedMultiplier)
        val randomNumber = (1..10).random()
        statusTV.text = getString(
            R.string.status_spinning,
            String.format(Locale.US, "%.1f", speedMultiplier)
        )
        wheelTV.animate().rotation(finalRotation).setDuration(900).withEndAction {
            luckyNumberTV.text = randomNumber.toString()
            statusTV.text = getString(R.string.status_result, randomNumber)
            isSpinning = false
            overheat = 0
            updatePuzzleVisuals()
            enablePlayButton()
        }.start()
    }

    // ──────────────────────────────────────────────────
    //  UX polish — state tints & guidance animations
    // ──────────────────────────────────────────────────

    private fun updatePuzzleVisuals() {
        cableTV.backgroundTintList = tintFor(if (isPowered) R.color.puzzle_ok else R.color.puzzle_info)
        anchorTV.backgroundTintList = tintFor(if (isAnchorStable) R.color.puzzle_ok else R.color.puzzle_warn)
        standTV.backgroundTintList = tintFor(if (isStandFixed) R.color.puzzle_ok else R.color.puzzle_danger)
    }

    private fun updateGuidanceAnimations() {
        stopPuzzleHint()
        stopStartPulse()
        val target: View? = when {
            !isPowered -> cableTV
            !isAnchorStable -> anchorTV
            !isStandFixed -> standTV
            else -> null
        }
        if (target != null) {
            puzzleHintAnimator = createPulseAnimator(target, 1.05f).also { it.start() }
        } else if (wheelExists && !isSpinning && playButton.isEnabled) {
            startHintAnimator = createPulseAnimator(playButton, 1.03f).also { it.start() }
        }
    }

    private fun wobbleAnchorHint() {
        ObjectAnimator.ofFloat(anchorTV, View.ROTATION, 0f, -10f, 10f, -6f, 6f, 0f).apply {
            duration = 380
            start()
        }
    }

    private fun createPulseAnimator(target: View, maxScale: Float): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, maxScale).apply {
            duration = 520
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, maxScale).apply {
            duration = 520
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().apply { playTogether(scaleX, scaleY) }
    }

    private fun stopPuzzleHint() {
        puzzleHintAnimator?.cancel()
        puzzleHintAnimator = null
        listOf(cableTV, anchorTV, standTV).forEach { it.scaleX = 1f; it.scaleY = 1f }
    }

    private fun stopStartPulse() {
        startHintAnimator?.cancel()
        startHintAnimator = null
        playButton.scaleX = 1f
        playButton.scaleY = 1f
    }

    private fun tintFor(colorRes: Int): ColorStateList? =
        ContextCompat.getColorStateList(this, colorRes)

    override fun onDestroy() {
        stopPuzzleHint()
        stopStartPulse()
        super.onDestroy()
    }

    @Suppress("UNUSED_PARAMETER")
    fun play(view: View) {
        // Always track overheat first — spam during a spin should still count
        updateSpamOverheat()

        // Mid-spin: only fire threshold matters; ignore all other gates
        if (isSpinning) {
            if (overheat >= FIRE_THRESHOLD) triggerFireFailure()
            return
        }

        tryStartSpin()
    }

    companion object {
        private const val REQUIRED_ANCHOR_TAPS = 5
        private const val SPAM_WINDOW_MS = 350
        private const val FIRE_THRESHOLD = 6
        private const val ROOM_DIM_ALPHA = 0.72f
    }
}