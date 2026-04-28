package com.example.luckydraw

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    var isSpinning = false

    fun disablePlayButton() {
        var playButtonTV = findViewById<Button>(R.id.playTV)
        playButtonTV.isEnabled = false
    }

    fun enablePlayButton() {
        var playButtonTV = findViewById<Button>(R.id.playTV)
        playButtonTV.isEnabled = true
    }

    fun play(view: View) {
        if(isSpinning) return

        isSpinning = true
        disablePlayButton()
        var randomNumber = (1..10).random()
        var luckyNumberTV = findViewById<TextView>(R.id.luckyNumberTV)
        luckyNumberTV.text = randomNumber.toString()
        isSpinning = false
        enablePlayButton()
    }
}