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
    private var isFailing = false

    private var anchorTapCount = 0
    private var overheat = 0
    private var lastStartTapMs = 0L

    private lateinit var playButton: Button
    private lateinit var statusTV: TextView
    private lateinit var cableTV: CableView  // custom plug/socket view
    private lateinit var anchorTV: AnchorView // triangle pointer — draws own stable/unstable indicator
    private lateinit var standView: StandView // tappable metallic stand
    private lateinit var wheelTV: WheelView
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

        playButton    = findViewById(R.id.playTV)
        statusTV      = findViewById(R.id.statusTV)
        cableTV       = findViewById(R.id.cableTV)
        anchorTV      = findViewById(R.id.anchorTV)
        standView     = findViewById(R.id.standViewBg)
        wheelTV       = findViewById(R.id.wheelTV)
        dimOverlay    = findViewById(R.id.dimOverlay)

        cableTV.setOnClickListener  { plugPowerCable() }
        anchorTV.setOnClickListener { stabilizeAnchor() }
        standView.setOnClickListener { fixStand() }

        resetRound(fullReset = true)
    }

    // ──────────────────────────────────────────────────
    //  Room lighting
    // ──────────────────────────────────────────────────

    private fun lightUpRoom() {
        dimOverlay.animate().alpha(0f).setDuration(900).start()
        playButton.backgroundTintList = tintFor(R.color.start_glow)
    }

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

        statusTV.text = getString(R.string.status_no_power)
        cableTV.isConnected = false
        // anchorTV & standView are custom Views — state shown visually, no text labels
        standView.isFixed = false
        standView.rotation = 0f
        standView.translationY = 0f
        wheelTV.isOnFire = false
        wheelTV.visibility = View.VISIBLE
        wheelTV.rotation = WHEEL_INITIAL_ROTATION
        wheelTV.translationY = 0f
        wheelTV.scaleX = 1f
        wheelTV.scaleY = 1f
        anchorTV.rotation = 0f
        isFailing = false
        disablePlayButton()
        dimRoom()
        updatePuzzleVisuals()
        updateGuidanceAnimations()

        if (!fullReset) statusTV.text = getString(R.string.status_reset)
    }

    // ──────────────────────────────────────────────────
    //  Puzzle interactions
    // ──────────────────────────────────────────────────

    private fun plugPowerCable() {
        if (isPowered) { statusTV.text = getString(R.string.status_already_powered); return }
        isPowered = true
        cableTV.isConnected = true        // plug snaps in, LED turns green
        enablePlayButton()
        statusTV.text = getString(R.string.status_powered)
        updatePuzzleVisuals()
        lightUpRoom()
    }

    private fun stabilizeAnchor() {
        if (isAnchorStable) { statusTV.text = getString(R.string.status_anchor_stable); return }
        anchorTapCount += 1
        statusTV.text = getString(R.string.status_anchor_progress, anchorTapCount, REQUIRED_ANCHOR_TAPS)
        if (anchorTapCount >= REQUIRED_ANCHOR_TAPS) {
            isAnchorStable = true
            statusTV.text = getString(R.string.status_anchor_done)
            anchorTV.rotation = 0f
            updatePuzzleVisuals()
            updateGuidanceAnimations()
        }
    }

    private fun fixStand() {
        if (isStandFixed) { statusTV.text = getString(R.string.status_stand_fixed); return }
        isStandFixed = true
        standView.isFixed = true          // bolt turns green instantly
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
        if (!wheelExists || isFailing) { statusTV.text = getString(R.string.status_wheel_missing); return }
        if (!isPowered) { statusTV.text = getString(R.string.status_no_power); return }
        if (!isAnchorStable) { triggerAnchorFall(); return }
        if (!isStandFixed)   { triggerDramaticFailure(); return }
        if (overheat >= FIRE_THRESHOLD) { triggerFireFailure(); return }
        spinWheel()
    }

    // ──────────────────────────────────────────────────
    //  Anchor fall
    // ──────────────────────────────────────────────────

    private fun triggerAnchorFall() {
        if (isFailing) return
        isFailing = true
        disablePlayButton()

        anchorTV.animate().translationYBy(560f).rotationBy(200f).alpha(0f).setDuration(620).start()

        anchorTV.postDelayed({
            statusTV.text = resources.getStringArray(R.array.anchor_fall_messages).random()
        }, 280)

        anchorTV.postDelayed({
            anchorTV.translationY = 0f
            anchorTV.rotation = 0f
            anchorTV.alpha = 0f
            anchorTV.animate().alpha(1f).setDuration(280).start()
            isAnchorStable = false
            anchorTapCount = 0
            isFailing = false
            updatePuzzleVisuals()
            enablePlayButton()
            updateGuidanceAnimations()
        }, 1300)
    }

    // ──────────────────────────────────────────────────
    //  Dramatic failure — wheel collapses
    // ──────────────────────────────────────────────────

    private fun triggerDramaticFailure() {
        if (isFailing) return
        isFailing = true
        isSpinning = false
        wheelTV.animate().cancel()
        disablePlayButton()
        stopPuzzleHint()

        statusTV.text = resources.getStringArray(R.array.failure_messages).random()
        ObjectAnimator.ofFloat(wheelTV, View.ROTATION, 0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f)
            .apply { duration = 380 }.start()

        // Phase 2: wheel falls, stand tips over
        wheelTV.postDelayed({
            wheelTV.animate()
                .translationYBy(420f).rotation(100f).scaleX(0.15f).scaleY(0.15f)
                .setDuration(480).start()
            standView.animate().rotationBy(70f).translationYBy(80f).setDuration(320).start()
        }, 380)

        wheelTV.postDelayed({ wheelTV.isOnFire = true }, 820)
        wheelTV.postDelayed({ wheelTV.visibility = View.INVISIBLE }, 1050)
        wheelTV.postDelayed({ respawnAfterFailure() }, 2000)
    }

    private fun respawnAfterFailure() {
        isAnchorStable = false
        isStandFixed = false
        anchorTapCount = 0
        overheat = 0
        wheelExists = true
        isFailing = false

        anchorTV.rotation = 0f
        standView.isFixed = false
        standView.animate().rotation(0f).translationY(0f).setDuration(280).start()

        wheelTV.isOnFire = false
        wheelTV.translationY = 0f
        wheelTV.rotation = WHEEL_INITIAL_ROTATION
        wheelTV.scaleX = 0f
        wheelTV.scaleY = 0f
        wheelTV.visibility = View.VISIBLE

        wheelTV.animate().scaleX(1.12f).scaleY(1.12f).setDuration(260).withEndAction {
            wheelTV.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
        }.start()

        statusTV.text = resources.getStringArray(R.array.respawn_messages).random()
        updatePuzzleVisuals()
        enablePlayButton()
        updateGuidanceAnimations()
    }

    private fun triggerFireFailure() {
        isSpinning = false
        wheelTV.animate().cancel()
        disablePlayButton()
        stopPuzzleHint()
        statusTV.text = getString(R.string.status_fire)

        // Phase 1 (0ms): fire tint on + violent shake
        wheelTV.isOnFire = true
        ObjectAnimator.ofFloat(wheelTV, View.ROTATION, 0f, -20f, 20f, -15f, 15f, -8f, 8f, 0f)
            .apply { duration = 320 }.start()

        // Phase 2 (320ms): flash scale-up → collapse to nothing
        wheelTV.postDelayed({
            wheelTV.animate()
                .scaleX(1.45f).scaleY(1.45f)
                .setDuration(140)
                .withEndAction {
                    wheelTV.animate()
                        .scaleX(0f).scaleY(0f).alpha(0f)
                        .setDuration(220)
                        .withEndAction {
                            // Restore transform state before hiding
                            wheelTV.visibility = View.INVISIBLE
                            wheelTV.alpha  = 1f
                            wheelTV.scaleX = 1f
                            wheelTV.scaleY = 1f
                            wheelExists = false
                            wheelTV.postDelayed({ resetRound(fullReset = false) }, 900)
                        }.start()
                }.start()
        }, 320)
    }

    private fun spinWheel() {
        isSpinning = true
        val randomNumber = (1..10).random()
        val speedMultiplier = 1f + (overheat * 0.2f)
        val extraSpins = (3 * speedMultiplier).toInt().coerceAtLeast(3) * 360f

        val sliceIndex = randomNumber - 1
        val halfSlice = WheelView.SLICE_ANGLE / 2f
        val targetR = (360f - halfSlice - sliceIndex * WheelView.SLICE_ANGLE + 360f) % 360f
        val currentR = ((wheelTV.rotation % 360f) + 360f) % 360f
        val deltaR = (targetR - currentR + 360f) % 360f
        val finalRotation = wheelTV.rotation + extraSpins + deltaR

        statusTV.text = getString(R.string.status_spinning, String.format(Locale.US, "%.1f", speedMultiplier))

        wheelTV.animate().rotation(finalRotation).setDuration(900).withEndAction {
            statusTV.text = getString(R.string.status_result, randomNumber)
            isSpinning = false
            overheat = 0
            updatePuzzleVisuals()
            enablePlayButton()
        }.start()
    }

    // ──────────────────────────────────────────────────
    //  UX polish
    // ──────────────────────────────────────────────────

    private fun updatePuzzleVisuals() {
        cableTV.isConnected = isPowered      // plug draws its own connected/disconnected state
        anchorTV.isStable   = isAnchorStable
        standView.isFixed   = isStandFixed
    }

    private fun updateGuidanceAnimations() {
        stopPuzzleHint()
        stopStartPulse()
        val target: View? = when {
            !isPowered    -> cableTV
            !isAnchorStable -> anchorTV
            !isStandFixed   -> standView
            else            -> null
        }
        if (target != null) {
            puzzleHintAnimator = createPulseAnimator(target, 1.05f).also { it.start() }
        } else if (wheelExists && !isSpinning && playButton.isEnabled) {
            startHintAnimator = createPulseAnimator(playButton, 1.03f).also { it.start() }
        }
    }

    private fun createPulseAnimator(target: View, maxScale: Float): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, maxScale).apply {
            duration = 520; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, maxScale).apply {
            duration = 520; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().apply { playTogether(scaleX, scaleY) }
    }

    private fun stopPuzzleHint() {
        puzzleHintAnimator?.cancel()
        puzzleHintAnimator = null
        listOf<View>(cableTV, anchorTV, standView).forEach { it.scaleX = 1f; it.scaleY = 1f }
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
        if (isFailing) return
        updateSpamOverheat()
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
        private const val WHEEL_INITIAL_ROTATION = 342f
    }
}

