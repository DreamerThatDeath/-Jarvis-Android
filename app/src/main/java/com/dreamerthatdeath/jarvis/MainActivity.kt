
package com.dreamerthatdeath.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject
import org.json.JSONArray
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import java.util.UUID
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {

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
    private lateinit var micButton: Button
    private lateinit var speakButton: Button
    private lateinit var accountButton: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val chatItems = mutableListOf<HashMap<String, Any>>()
    private var restoringHistory = false
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speakReplies = true
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        tts = TextToSpeech(this, this)
        firebaseAuth = FirebaseAuth.getInstance()

        val googleOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleOptions)

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
        root.addView(header, LinearLayout.LayoutParams(-1, dp(42)))

        root.addView(
            OrbView(),
            LinearLayout.LayoutParams(dp(220), dp(200)).apply {
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
        root.addView(status, LinearLayout.LayoutParams(-1, dp(25)))

        val caption = TextView(this).apply {
            text = "Your personal AI"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(135, 159, 185))
        }
        root.addView(caption, LinearLayout.LayoutParams(-1, dp(26)))

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

        scroll = ScrollView(this)

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        restoreLocalHistory()
        if (chatItems.isEmpty()) {
            addMessage(
                "JARVIS",
                "Sistem siap. Halo! Apa yang bisa kubantu hari ini?",
                false
            )
        }

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

        micButton = Button(this).apply {
            text = "🎙"
            textSize = 17f
            setTextColor(cyan)
            background = rounded(panel, 18)
            setPadding(0, 0, 0, 0)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setOnClickListener { toggleListening() }
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

        composer.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f))
        composer.addView(micButton, LinearLayout.LayoutParams(dp(44), dp(48)))
        composer.addView(sendButton, LinearLayout.LayoutParams(dp(48), dp(48)))

        root.addView(
            composer,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                topMargin = dp(8)
            }
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        speakButton = Button(this).apply {
            text = "🔊 Suara: ON"
            textSize = 11f
            setTextColor(cyan)
            background = rounded(panel, 12)
            setOnClickListener {
                speakReplies = !speakReplies
                text = if (speakReplies) {
                    "🔊 Suara: ON"
                } else {
                    "🔇 Suara: OFF"
                }

                if (!speakReplies) tts?.stop()
            }
        }

        controls.addView(
            speakButton,
            LinearLayout.LayoutParams(-2, dp(40)).apply {
                topMargin = dp(5)
            }
        )

        accountButton = Button(this).apply {
            textSize = 11f
            setTextColor(cyan)
            background = rounded(panel, 12)
            setOnClickListener { onAccountButtonClicked() }
        }
        controls.addView(
            accountButton,
            LinearLayout.LayoutParams(-2, dp(40)).apply {
                leftMargin = dp(8)
                topMargin = dp(5)
            }
        )
        updateAccountButton()
        root.addView(controls)

        val footer = TextView(this).apply {
            text = "JARVIS CAN MAKE MISTAKES"
            textSize = 9f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(88, 112, 139))
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, dp(25)))

        setContentView(root)

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        setupSpeechRecognizer()
    
            if (intent.getBooleanExtra("WAKE_WORD_TRIGGERED", false)) {
            window.decorView.postDelayed({
                toggleListening()
            }, 800)
        }
    }

    private fun updateAccountButton() {
        val user = firebaseAuth.currentUser
        accountButton.text = if (user == null) "Google: Sign in" else "Google: Sign out"
    }

    private fun onAccountButtonClicked() {
        if (firebaseAuth.currentUser != null) {
            firebaseAuth.signOut()
            googleSignInClient.signOut().addOnCompleteListener {
                updateAccountButton()
                Toast.makeText(this, "Berhasil keluar dari akun Google.", Toast.LENGTH_SHORT).show()
            }
        } else {
            startActivityForResult(googleSignInClient.signInIntent, 9001)
        }
    }

    @Deprecated("Google Sign-In activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { result ->
                        if (result.isSuccessful) {
                            updateAccountButton()
                            val name = firebaseAuth.currentUser?.displayName ?: "pengguna"
                            Toast.makeText(this, "Login berhasil. Halo, $name!", Toast.LENGTH_LONG).show()
                            syncChatHistory()
                        } else {
                            Toast.makeText(this, "Login Firebase gagal: ${result.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-In gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        addMessage("KAMU", message, true)
        input.text.clear()

        sendButton.isEnabled = false
        micButton.isEnabled = false
        status.text = "●  JARVIS SEDANG BERPIKIR..."
        status.setTextColor(Color.rgb(255, 205, 90))

        addMessage(
            "JARVIS",
            "Sedang memproses pesan...",
            false,
            true
        )

        executor.execute {
            val answer: String = try {
                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 30000
                    connection.readTimeout = 90000
                    connection.doOutput = true

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )
                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    val payload = JSONObject()
                        .put("message", message)
                        .put("mode", "ramah")
                        .put("language", "id")

                    OutputStreamWriter(
                        connection.outputStream,
                        Charsets.UTF_8
                    ).use {
                        it.write(payload.toString())
                    }

                    val code = connection.responseCode

                    val stream = if (code in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                    val response = if (stream != null) {
                        BufferedReader(
                            InputStreamReader(stream, Charsets.UTF_8)
                        ).use {
                            it.readText()
                        }
                    } else {
                        ""
                    }

                    val json = try {
                        JSONObject(response)
                    } catch (_: Exception) {
                        JSONObject()
                    }

                    val reply = json.optString("reply", "")

                    if (code !in 200..299 || reply.isBlank()) {
                        val detail = json.optString("error", "")
                        throw Exception(
                            detail.ifBlank {
                                "Server merespons dengan kode $code"
                            }
                        )
                    }

                    reply
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                "Jarvis belum bisa terhubung ke AI. " +
                    "Periksa koneksi internet dan backend Gemini kamu. " +
                    "Detail: ${e.message ?: "koneksi gagal"}"
            }

            runOnUiThread {
                removeLoadingMessage()
                addMessage("JARVIS", answer, false)

                if (speakReplies && ttsReady) {
                    tts?.speak(
                        answer,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "jarvis-reply"
                    )
                }

                sendButton.isEnabled = true
                micButton.isEnabled = true
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
            LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(4)
            }
        )

        messages.addView(wrapper, LinearLayout.LayoutParams(-1, -2))
        if (!loading && !restoringHistory) {
            val item = hashMapOf<String, Any>(
                "id" to UUID.randomUUID().toString(),
                "sender" to sender,
                "content" to content,
                "isUser" to isUser
            )
            chatItems.add(item)
            if (chatItems.size > 100) chatItems.removeAt(0)
            persistChatHistory()
            if (firebaseAuth.currentUser != null) uploadChatHistory()
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun restoreLocalHistory() {
        val raw = getSharedPreferences("jarvis_chat", MODE_PRIVATE)
            .getString("messages", null) ?: return
        try {
            val array = JSONArray(raw)
            restoringHistory = true
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = hashMapOf<String, Any>(
                    "id" to obj.optString("id", UUID.randomUUID().toString()),
                    "sender" to obj.optString("sender", "JARVIS"),
                    "content" to obj.optString("content", ""),
                    "isUser" to obj.optBoolean("isUser", false)
                )
                if ((item["content"] as String).isNotBlank()) {
                    chatItems.add(item)
                    addMessage(item["sender"] as String, item["content"] as String, item["isUser"] as Boolean)
                }
            }
        } catch (_: Exception) {
            chatItems.clear()
        } finally {
            restoringHistory = false
        }
    }

    private fun persistChatHistory() {
        val array = JSONArray()
        chatItems.subList((chatItems.size - 100).coerceAtLeast(0), chatItems.size).forEach { item -> array.put(JSONObject(item)) }
        getSharedPreferences("jarvis_chat", MODE_PRIVATE)
            .edit().putString("messages", array.toString()).apply()
    }

    private fun syncChatHistory() {
        val user = firebaseAuth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .collection("data").document("chat")
            .get()
            .addOnSuccessListener { snapshot ->
                val cloudItems = snapshot.get("messages") as? List<*>
                if (cloudItems.isNullOrEmpty()) {
                    uploadChatHistory()
                    return@addOnSuccessListener
                }
                val merged = LinkedHashMap<String, HashMap<String, Any>>()
                for (raw in cloudItems) {
                    val map = raw as? Map<*, *> ?: continue
                    val id = map["id"] as? String ?: continue
                    val sender = map["sender"] as? String ?: continue
                    val content = map["content"] as? String ?: continue
                    val isUser = map["isUser"] as? Boolean ?: false
                    merged[id] = hashMapOf("id" to id, "sender" to sender, "content" to content, "isUser" to isUser)
                }
                chatItems.forEach { item -> merged[item["id"] as String] = item }
                chatItems.clear()
                chatItems.addAll(merged.values.toList().let { values -> values.subList((values.size - 100).coerceAtLeast(0), values.size) })
                renderChatHistory()
                persistChatHistory()
                uploadChatHistory()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Gagal memuat chat cloud: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun renderChatHistory() {
        restoringHistory = true
        messages.removeAllViews()
        chatItems.forEach { item ->
            addMessage(item["sender"] as String, item["content"] as String, item["isUser"] as Boolean)
        }
        restoringHistory = false
    }

    private fun uploadChatHistory() {
        val user = firebaseAuth.currentUser ?: return
        val payload = hashMapOf<String, Any>(
            "messages" to chatItems.subList((chatItems.size - 100).coerceAtLeast(0), chatItems.size),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(user.uid)
            .collection("data").document("chat")
            .set(payload, SetOptions.merge())
            .addOnFailureListener { error ->
                Toast.makeText(this, "Gagal menyimpan chat cloud: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            micButton.isEnabled = false
            micButton.text = "×"
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status.text = "●  SILAKAN BICARA..."
                    status.setTextColor(cyan)
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    listening = false
                    micButton.text = "🎙"
                }

                override fun onError(error: Int) {
                    listening = false
                    micButton.text = "🎙"
                    status.text = "●  JARVIS SYSTEM"
                    status.setTextColor(cyan)

                    Toast.makeText(
                        this@MainActivity,
                        "Suara tidak terdeteksi. Coba lagi.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    micButton.text = "🎙"

                    val texts = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val spoken = texts?.firstOrNull().orEmpty()

                    if (spoken.isNotBlank()) {
                        input.setText(spoken)
                        input.setSelection(input.text.length)
                        sendMessage()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )
    }

    
private fun toggleListening() {
    if (listening) {
        speechRecognizer?.stopListening()
        listening = false
        micButton.text = "🎙"
        return
    }

    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) {
        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1001
        )
        return
    }

    if (speechRecognizer == null) {
        setupSpeechRecognizer()
    }

    try {
        val serviceIntent = Intent(
            this,
            JarvisVoiceService::class.java
        )

        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O
        ) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(
            this,
            "Layanan Hey Jarvis gagal dimulai: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }

    val speechIntent = Intent(
        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
    ).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "id-ID"
        )
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            "id-ID"
        )
        putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS,
            1
        )
    }

    try {
        speechRecognizer?.startListening(speechIntent)
        micButton.text = "■"
    } catch (e: Exception) {
        Toast.makeText(
            this,
            "Tidak dapat memulai pengenalan suara.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            toggleListening()
        } else if (requestCode == 1001) {
            Toast.makeText(
                this,
                "Izin mikrofon diperlukan untuk fitur suara.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            ttsReady =
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
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
            val pulse =
                0.92f + 0.08f * kotlin.math.sin(t * 2.2f)
            val radius = dp(53) * pulse

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

            canvas.save()
            canvas.rotate(t * 24f, cx, cy)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.4f)
            paint.color = Color.argb(190, 50, 215, 255)

            bounds.set(
                cx - dp(76), cy - dp(24),
                cx + dp(76), cy + dp(24)
            )
            canvas.drawArc(bounds, 0f, 300f, false, paint)

            paint.color = Color.argb(120, 70, 150, 255)
            bounds.set(
                cx - dp(85), cy - dp(33),
                cx + dp(85), cy + dp(33)
            )
            canvas.drawArc(bounds, 170f, 205f, false, paint)
            canvas.restore()

            canvas.save()
            canvas.rotate(-t * 35f, cx, cy)
            paint.strokeWidth = dp(2f)
            paint.color = Color.argb(150, 50, 215, 255)

            bounds.set(
                cx - dp(68), cy - dp(68),
                cx + dp(68), cy + dp(68)
            )
            canvas.drawArc(bounds, 205f, 105f, false, paint)
            canvas.drawArc(bounds, 20f, 55f, false, paint)
            canvas.restore()

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

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2f)
            paint.color = Color.argb(220, 125, 245, 255)
            canvas.drawCircle(cx, cy, radius, paint)

            paint.style = Paint.Style.FILL
            for (i in 0 until 6) {
                val a = t * 0.8f + i * (Math.PI / 3.0)
                val px = cx +
                    kotlin.math.cos(a).toFloat() * dp(78)
                val py = cy +
                    kotlin.math.sin(a).toFloat() * dp(34)

                paint.color = if (i % 2 == 0) Color.WHITE else cyan
                canvas.drawCircle(px, py, dp(2f), paint)
            }

            postInvalidateDelayed(33)
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        executor.shutdownNow()
        super.onDestroy()
    }
}
