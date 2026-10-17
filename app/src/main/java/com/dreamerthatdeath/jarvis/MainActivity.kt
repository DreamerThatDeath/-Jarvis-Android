
package com.dreamerthatdeath.jarvis

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.content.Context
import android.widget.Toast
import java.util.Locale
import android.content.pm.PackageManager
import android.Manifest
import android.content.Intent
import android.os.Build

class MainActivity : Activity() {

    private val bg = Color.rgb(5, 9, 18)
    private val panel = Color.rgb(14, 23, 38)
    private val cyan = Color.rgb(50, 215, 255)
    private val white = Color.rgb(235, 246, 255)

    private val apiUrl =
        "https://jarvis-ai-backend-iota.vercel.app/api/chat"

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(10), dp(18), dp(12))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = TextView(this).apply {
            text = "JARVIS"
            textSize = 23f
            letterSpacing = 0.18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
        }

        val version = TextView(this).apply {
            text = "  /  AI ASSISTANT"
            textSize = 10f
            letterSpacing = 0.08f
            setTextColor(Color.LTGRAY)
        }

        header.addView(brand)
        header.addView(version)
        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1, dp(42)
            )
        )

        val orb = OrbView()
        root.addView(
            orb,
            LinearLayout.LayoutParams(
                dp(220), dp(200)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(2)
            }
        )

        status = TextView(this).apply {
            text = "●  JARVIS SYSTEM"
            textSize = 11f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(cyan)
        }
        root.addView(
            status,
            LinearLayout.LayoutParams(-1, dp(25))
        )

        val caption = TextView(this).apply {
            text = "Your personal AI"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(135, 159, 185))
        }
        root.addView(
            caption,
            LinearLayout.LayoutParams(-1, dp(26))
        )

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(29, 58, 79))
        }
        root.addView(
            divider,
            LinearLayout.LayoutParams(-1, dp(1)).apply {
                topMargin = dp(6)
                bottomMargin = dp(8)
            }
        )

        scroll = ScrollView(this).apply {
            isFillViewport = false
        }

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(messages)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        addMessage(
            "JARVIS",
            "Sistem siap. Halo! Apa yang bisa kubantu hari ini?",
            false
        )

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(9), dp(5), dp(5), dp(5))
            background = rounded(panel, 24, Color.rgb(35, 68, 91))
        }

        input = EditText(this).apply {
            hint = "Tanya Jarvis..."
            setHintTextColor(Color.rgb(120, 144, 166))
            setTextColor(white)
            textSize = 15f
            maxLines = 3
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(10), dp(8), dp(8), dp(8))
            background = null
        }

        sendButton = Button(this).apply {
            text = "➤"
            textSize = 20f
            setTextColor(bg)
            background = rounded(cyan, 18)
            setPadding(0, 0, 0, 0)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setOnClickListener { sendMessage() }
        }

        composer.addView(
            input,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        composer.addView(
            sendButton,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        root.addView(
            composer,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                topMargin = dp(8)
            }
        )

        val footer = TextView(this).apply {
            text = "JARVIS CAN MAKE MISTAKES"
            textSize = 9f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(88, 112, 139))
        }
        root.addView(
            footer,
            LinearLayout.LayoutParams(-1, dp(25))
        )

        setContentView(root)

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        addMessage("KAMU", message, true)
        input.text.clear()
        sendButton.isEnabled = false
        status.text = "●  JARVIS SEDANG BERPIKIR..."
        status.setTextColor(Color.rgb(255, 205, 90))
        addMessage("JARVIS", "Sedang memproses pesan...", false, true)

        executor.execute {
            var answer: String
            try {
                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type", "application/json; charset=UTF-8"
                )
                connection.setRequestProperty(
                    "Accept", "application/json"
                )

                val payload = JSONObject()
                    .put("message", message)
                    .put("mode", "ramah")
                    .put("language", "id")

                OutputStreamWriter(
                    connection.outputStream, Charsets.UTF_8
                ).use {
                    it.write(payload.toString())
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = BufferedReader(
                    InputStreamReader(stream, Charsets.UTF_8)
                ).use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)
                answer = json.optString("reply", "")

                if (code !in 200..299 || answer.isBlank()) {
                    val detail = json.optString("error", "")
                    throw Exception(
                        if (detail.isNotBlank()) detail
                        else "Server merespons dengan kode $code"
                    )
                }
            } catch (e: Exception) {
                answer = "Jarvis belum bisa terhubung ke AI. " +
                    "Periksa koneksi internet dan backend Gemini kamu. " +
                    "Detail: ${e.message ?: "koneksi gagal"}"
            }

            runOnUiThread {
                removeLoadingMessage()
                addMessage("JARVIS", answer, false)
                sendButton.isEnabled = true
                status.text = "●  JARVIS SYSTEM"
                status.setTextColor(cyan)
            }
        }
    }

    private fun addMessage(
        sender: String,
        content: String,
        isUser: Boolean,
        loading: Boolean = false
    ) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isUser) Gravity.END else Gravity.START
            tag = if (loading) "loading" else null
            setPadding(0, dp(5), 0, dp(5))
        }

        val label = TextView(this).apply {
            text = sender
            textSize = 10f
            letterSpacing = 0.1f
            setTextColor(if (isUser) Color.LTGRAY else cyan)
        }

        val bubble = TextView(this).apply {
            text = content
            textSize = 14f
            setTextColor(white)
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = rounded(
                if (isUser) Color.rgb(20, 48, 67) else panel,
                16,
                if (isUser) Color.rgb(34, 100, 130)
                else Color.rgb(28, 54, 75)
            )
        }

        wrapper.addView(label)
        wrapper.addView(
            bubble,
            LinearLayout.LayoutParams(
                -2, -2
            ).apply {
                topMargin = dp(4)
            }
        )

        messages.addView(
            wrapper,
            LinearLayout.LayoutParams(-1, -2)
        )

        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun removeLoadingMessage() {
        for (i in messages.childCount - 1 downTo 0) {
            val child = messages.getChildAt(i)
            if (child.tag == "loading") {
                messages.removeViewAt(i)
                return
            }
        }
    }

    private fun rounded(
        color: Int,
        radius: Int,
        stroke: Int? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) {
                setStroke(dp(1), stroke)
            }
        }
    }

    
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density


    inner class OrbView : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bounds = RectF()

        init {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val cx = width / 2f
            val cy = height / 2f
            val t = SystemClock.uptimeMillis() / 1000f
            val pulse = 0.92f + 0.08f * kotlin.math.sin(t * 2.2f)
            val radius = dp(53) * pulse

            // Outer blue glow
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx, cy, dp(95).toFloat(),
                intArrayOf(
                    Color.argb(100, 0, 160, 255),
                    Color.argb(30, 0, 100, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, dp(95).toFloat(), paint)
            paint.shader = null

            // Rotating holographic rings
            canvas.save()
            canvas.rotate(t * 24f, cx, cy)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.4f).toFloat()
            paint.color = Color.argb(190, 50, 215, 255)
            bounds.set(cx - dp(76), cy - dp(24),
                cx + dp(76), cy + dp(24))
            canvas.drawArc(bounds, 0f, 300f, false, paint)

            paint.color = Color.argb(120, 70, 150, 255)
            bounds.set(cx - dp(85), cy - dp(33),
                cx + dp(85), cy + dp(33))
            canvas.drawArc(bounds, 170f, 205f, false, paint)
            canvas.restore()

            canvas.save()
            canvas.rotate(-t * 35f, cx, cy)
            paint.strokeWidth = dp(2).toFloat()
            paint.color = Color.argb(150, 50, 215, 255)
            bounds.set(cx - dp(68), cy - dp(68),
                cx + dp(68), cy + dp(68))
            canvas.drawArc(bounds, 205f, 105f, false, paint)
            canvas.drawArc(bounds, 20f, 55f, false, paint)
            canvas.restore()

            // Main energy sphere
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx - radius * 0.28f,
                cy - radius * 0.32f,
                radius * 1.45f,
                intArrayOf(
                    Color.WHITE,
                    Color.rgb(115, 239, 255),
                    Color.rgb(0, 150, 255),
                    Color.rgb(14, 40, 140),
                    Color.rgb(3, 10, 35)
                ),
                floatArrayOf(0f, 0.18f, 0.42f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null

            // Fine luminous outline
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2).toFloat()
            paint.color = Color.argb(220, 125, 245, 255)
            canvas.drawCircle(cx, cy, radius, paint)

            // Small orbital particles
            paint.style = Paint.Style.FILL
            for (i in 0 until 6) {
                val a = t * 0.8f + i * (Math.PI / 3.0)
                val px = cx + kotlin.math.cos(a).toFloat() * dp(78)
                val py = cy + kotlin.math.sin(a).toFloat() * dp(34)
                paint.color = if (i % 2 == 0) {
                    Color.WHITE
                } else {
                    cyan
                }
                canvas.drawCircle(px, py, dp(2).toFloat(), paint)
            }

            postInvalidateDelayed(33)
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
