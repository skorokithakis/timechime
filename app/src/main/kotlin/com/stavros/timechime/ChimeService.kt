package com.stavros.timechime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

private const val NOTIFICATION_CHANNEL_ID = "chime_service"
private const val NOTIFICATION_ID = 1
const val ACTION_STOP = "com.stavros.timechime.ACTION_STOP"

class ChimeService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            getSharedPreferences("timechime_prefs", MODE_PRIVATE)
                .edit().putBoolean("is_running", false).apply()
            stopSelf()
            return START_NOT_STICKY
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ChimeService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Time Chime is running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        scheduleNextChimeAlarm(this)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelChimeAlarm(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Chime Service",
            NotificationManager.IMPORTANCE_LOW,
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
