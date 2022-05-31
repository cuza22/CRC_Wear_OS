package com.example.crc_wear_os

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    val TAG = "AlarmReceiver"
    lateinit var mode: String
    lateinit var date: String
    var remaining: Int = 0

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive()")
        if (intent != null) {
            mode = intent.getStringExtra("mode") as String
            val i = Intent(context, RestartService::class.java).apply {
                putExtra("mode", mode)
                action = "ACTION.Restart.BackGroundCollecting"
            }
            context?.startForegroundService(i)
        }
    }
}