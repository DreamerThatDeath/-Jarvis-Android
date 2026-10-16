
package com.dreamerthatdeath.jarvis

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val dark = Color.rgb(10, 10, 10)
    private val blue = Color.rgb(0, 168, 255)
    private lateinit var messages: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
            setPadding(20, 24, 20, 12)
        }

        val title = TextView(this).apply {
            text = "JARVIS AI"
            textSize = 25f
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 20)
        }
        root.addView(title)

        val scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(messages)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        addMessage("Jarvis", "Halo! Aku Jarvis, asisten AI kamu. 💙")

        val input = EditText(this).apply {
            hint = "Tulis pesan..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
        }

        val send = Button(this).apply {
            text = "Kirim"
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    addMessage("Kamu", message)
                    addMessage(
                        "Jarvis",
                        "Tampilan chat sudah aktif! Sambungan AI akan kita tambahkan berikutnya."
                    )
                    input.text.clear()
                }
            }
        }

        root.addView(input)
        root.addView(send)
        setContentView(root)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), 100
            )
        }
    }

    private fun addMessage(sender: String, message: String) {
        val text = TextView(this).apply {
            this.text = "$sender: $message"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(12, 14, 12, 14)
        }
        messages.addView(text)
    }
}
