
package com.dreamerthatdeath.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.openwakeword.OpenWakeWord

class JarvisVoiceService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_voice_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "com.dreamerthatdeath.jarvis.STOP_VOICE"
    }

    private var detector: OpenWakeWord? = null
    private var isListening = false
    private var wakeTriggered = false
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopDetector()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis AI")
            .setContentText("Layanan suara aktif")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo
                        .FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            if (!isListening) {
                detector = OpenWakeWord.Builder(this)
                    .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                    .setThreshold(0.5f)
                    .build()

                detector?.start { score ->
    if (wakeTriggered) {
        return@start
    }

    wakeTriggered = true

    Log.i("JarvisWakeWord", "Hey Jarvis detected: $score")

    stopDetector()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()

    val wakeIntent = Intent(
        this,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

        putExtra("WAKE_WORD_TRIGGERED", true)
    }

    try {
        startActivity(wakeIntent)
    } catch (e: Exception) {
        Log.e(
            "JarvisWakeWord",
            "Gagal membuka Jarvis AI",
            e
        )
    }
                }

                isListening = true
            }
        } catch (e: Exception) {
            Log.e("JarvisWakeWord", "Gagal memulai deteksi", e)
            stopDetector()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun stopDetector() {
        try {
            detector?.stop()
            detector?.release()
        } catch (e: Exception) {
            Log.e("JarvisWakeWord", "Gagal menghentikan deteksi", e)
        }

        detector = null
        isListening = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Voice",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopDetector()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
