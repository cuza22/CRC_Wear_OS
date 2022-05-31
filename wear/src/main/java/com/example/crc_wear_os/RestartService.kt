package com.example.crc_wear_os

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class RestartService : Service() {
    val TAG = "RestartService"
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        val builder = NotificationCompat.Builder(this, "default")
        builder.setSmallIcon(R.mipmap.ic_launcher)

//        val notification: Notification = builder.build()
//        startForeground(9, notification)

        if (intent != null) {
            val mode = intent.getStringExtra("mode") as String
            val i = Intent(this, BackGroundCollecting::class.java).apply {
                putExtra("mode", mode)
            }
            startService(i)
        }

//        stopForeground(true)
        stopSelf()

        return START_NOT_STICKY
    }
}