package com.example.crc_wear_os

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

class Collecting : Activity() {

    private val TAG = "Collecting"

//    var locationManager : LocationManager = TODO()
//    var connectivityManager : ConnectivityManager = TODO()

    private val AMBIENT_UPDATE_ACTION = "com.your.package.action.AMBIENT_UPDATE"
    private lateinit var ambientUpdateAlarmManager: AlarmManager
    private lateinit var ambientUpdatePendingIntent: PendingIntent
    private lateinit var ambientUpdateBroadcastReceiver: BroadcastReceiver

    private lateinit var progressBar : ProgressBar
    private lateinit var countDownTimer : CountDownTimer
    private var remainingTime : Int = 0
    private val DURATION_TIME : Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collecting)

        /// keep the app awake
        ambientUpdateAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        ambientUpdatePendingIntent = Intent(AMBIENT_UPDATE_ACTION).let { ambientUpdateIntent ->
            PendingIntent.getBroadcast(this, 0, ambientUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        ambientUpdateBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDisplayAndSetNextUpdate()
            }
        }

        /// count-down UI
        progressBar = findViewById(R.id.progress_bar)
        remainingTime = DURATION_TIME
        progressBar.progress = remainingTime

        countDownTimer = object : CountDownTimer((DURATION_TIME * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = (millisUntilFinished/1000).toInt()
                progressBar.progress = remainingTime
                Log.d(TAG, "remaining: $remainingTime")
            }

            override fun onFinish() {
                val notification : Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                ringtone.play()

//                val vibrator : Vibrator = getSystemService(VIBRATOR_MANAGER_SERVICE) as Vibrator
//                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }.start()
    }

    private val AMBIENT_INTERVAL_MS: Long = TimeUnit.SECONDS.toMillis(1000)
    private fun refreshDisplayAndSetNextUpdate() {
        val timeMs: Long = System.currentTimeMillis()
        val delayMs: Long = AMBIENT_INTERVAL_MS - timeMs % AMBIENT_INTERVAL_MS
        val triggerTimeMs: Long = timeMs + delayMs
        ambientUpdateAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMs,
            ambientUpdatePendingIntent
        )
        Log.d(TAG, "alarm: $triggerTimeMs")
    }

    override fun onDestroy() {
        super.onDestroy()
        ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent)
        countDownTimer.cancel()
    }

}
