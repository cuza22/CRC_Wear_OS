package com.example.crc_wear_os

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
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

    internal lateinit var intent : Intent
    private lateinit var mode : String

    private val COLLECTING_TIME : Int = 15
    private var remainingTime : Int = COLLECTING_TIME
    private var stop : Boolean = false
    private lateinit var binder : IMyAidlInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collecting)

        // get information from intent
        mode = getIntent().extras?.get("mode") as String

        // keep the app awake - alarm
        ambientUpdateAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ambientUpdatePendingIntent = Intent(AMBIENT_UPDATE_ACTION).let { ambientUpdateIntent ->
            PendingIntent.getBroadcast(this, 0, ambientUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        ambientUpdateBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDisplayAndSetNextUpdate()
            }
        }

        // count-down UI
        progressBar = findViewById(R.id.progress_bar)
        progressBar.progress = remainingTime

//        countDownTimer = object : CountDownTimer((COLLECTING_TIME * 1000).toLong(), 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                remainingTime = (millisUntilFinished/1000).toInt()
//                progressBar.progress = remainingTime
//                Log.d(TAG, "remaining: $remainingTime")
//            }
//
//            override fun onFinish() {
//                Log.d(TAG, "count down timer finished !!!")
//            }
//        }.start()

        // start collecting thread (runs background, ends automatically)
        intent = Intent(applicationContext, BackGroundCollecting::class.java).apply {
            setPackage("com.example.crc_wear_os")
            putExtra("mode", mode)
        }
        startForegroundService(intent)
        startService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)

        // start counting thread (ends automatically)
        CountingThread().start()
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

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = IMyAidlInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        endCollecting()
    }

    private fun updateRemainingTimeUI() {
        remainingTime = binder.getRemainingTime()
        progressBar.progress = remainingTime
    }

    private fun endCollecting() {
        // stop the alarm
        ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent)

        // finish binding
        stopService(intent)
        unbindService(connection)

        // start new activity
        val survey_intent = Intent(applicationContext, LastSurvey::class.java)
        survey_intent.putExtra("mode", mode)
        startActivity(survey_intent)

        // finish this activity (collecting)
        finish()
    }

    // timer thread
    inner class CountingThread : Thread() {
        override fun run() {
            super.run()

            while(!stop) {

                updateRemainingTimeUI()

                try {
                    if (remainingTime == 0) {
                        stop = true
                        // notification
                        val notification : Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                        ringtone.play()
                        val vibrator : Vibrator = getSystemService(VIBRATOR_MANAGER_SERVICE) as Vibrator
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))

                        // end collecting
                        endCollecting()
                    }
                    sleep(1000)
                } catch (e : InterruptedException) {}
            }
        }
    }
}
