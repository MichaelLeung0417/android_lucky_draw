package com.example.luckydraw

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var cableTV: TextView
    private lateinit var anchorTV: TextView
    private lateinit var standTV: TextView
    private lateinit var wheelTV: TextView

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

        cableTV.setOnClickListener { plugPowerCable() }
        anchorTV.setOnClickListener { stabilizeAnchor() }
        standTV.setOnClickListener { fixStand() }

        resetRound(fullReset = true)
    }

    private fun disablePlayButton() {
        playButton.isEnabled = false
    }

    private fun enablePlayButton() {
        playButton.isEnabled = true
    }

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
        wheelTV.scaleX = 1f
        wheelTV.scaleY = 1f
        disablePlayButton()

        if (!fullReset) {
            statusTV.text = getString(R.string.status_reset)
        }
    }

    private fun plugPowerCable() {
        if (isPowered) {
            statusTV.text = getString(R.string.status_already_powered)
            return
        }

        isPowered = true
        cableTV.text = getString(R.string.cable_connected)
        enablePlayButton()
        statusTV.text = getString(R.string.status_powered)
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
    }

    private fun updateSpamOverheat() {
        val now = System.currentTimeMillis()
        overheat = if (now - lastStartTapMs <= SPAM_WINDOW_MS) {
            overheat + 1
        } else {
            1
        }
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
            anchorTV.rotation = if (anchorTV.rotation == 0f) -10f else 10f
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

        wheelTV.postDelayed({
            wheelTV.translationY = 0f
            wheelTV.rotation = 0f
            enablePlayButton()
            statusTV.text = getString(R.string.status_after_collapse)
        }, 900)
    }

    private fun triggerFireFailure() {
        wheelExists = false
        wheelTV.visibility = View.INVISIBLE
        statusTV.text = getString(R.string.status_fire)
        luckyNumberTV.text = "X"
        disablePlayButton()

        wheelTV.postDelayed({
            resetRound(fullReset = false)
        }, 1500)
    }

    private fun spinWheel() {
        isSpinning = true
        disablePlayButton()

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
            enablePlayButton()
        }.start()
    }

    @Suppress("UNUSED_PARAMETER")
    fun play(_view: View) {
        if (isSpinning) return

        updateSpamOverheat()
        tryStartSpin()
    }

    companion object {
        private const val REQUIRED_ANCHOR_TAPS = 5
        private const val SPAM_WINDOW_MS = 350
        private const val FIRE_THRESHOLD = 6
    }
}