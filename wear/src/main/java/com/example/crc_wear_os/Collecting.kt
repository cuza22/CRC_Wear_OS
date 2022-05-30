package com.example.crc_wear_os

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.hardware.Sensor
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class Collecting : Activity() {

    private val TAG = "Collecting"

    private val AMBIENT_UPDATE_ACTION = "com.your.package.action.AMBIENT_UPDATE"
    private lateinit var ambientUpdateAlarmManager: AlarmManager
    private lateinit var ambientUpdatePendingIntent: PendingIntent
    private lateinit var ambientUpdateBroadcastReceiver: BroadcastReceiver

//    private lateinit var progressBar : ProgressBar
//    private lateinit var countDownTimer : CountDownTimer
    private lateinit var remainingText : TextView

    internal lateinit var intent : Intent
    private lateinit var mode : String

    private val COLLECTING_TIME : Int = 30
    private var remainingTime : Int = COLLECTING_TIME

    private lateinit var countingThread: CountingThread
    private var stopCounting : Boolean = false
    var isBound = false

//    private lateinit var service : BackGroundCollecting
    var binder : IMyAidlInterface? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binderService: IBinder?) {
            Log.i(TAG, "onServiceConnected")
            binder = IMyAidlInterface.Stub.asInterface(binderService)

//            // start counting thread (ends automatically) TODO()
//            countingThread = CountingThread()
//            countingThread.priority = Thread.MAX_PRIORITY
//            countingThread.run()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected")
            binder = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")

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

//        // start counting thread (ends automatically) TODO()
//        countingThread = CountingThread()
//        countingThread.run()
//        thread(start = true){
//            var i = COLLECTING_TIME
//            while (i > 0) {
//                i--
//                runOnUiThread {
//                    remainingText.text = "$mode\n$i sec"
//                }
//                Log.d(TAG, "remaining UI : $i")
//                Thread.sleep(1000)
//            }
//        }

        // count-down UI
//        progressBar = findViewById(R.id.progress_bar)
//        progressBar.progress = remainingTime
        remainingText = findViewById(R.id.remaining_time)
        remainingText.text = "$mode\n$remainingTime sec"

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
//        Log.d(TAG, "intent : $intent")
//        startForegroundService(intent)
        startService(intent)
        isBound = bindService(intent, connection, BIND_AUTO_CREATE)

//        // start counting thread (ends automatically)
//        CountingThread().run()
    }

//    override fun onStart() {
//        Log.d(TAG, "onStart()")
//        super.onStart()
//
//        Intent(this, BackGroundCollecting::class.java).also { intent ->
//            bindService(intent, connection, Context.BIND_AUTO_CREATE)
//        }
//
//        CountingThread().start()
//    }

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
        if (isBound) { unbindService(connection) }
        endCollecting()

        // start new activity
        val survey_intent = Intent(applicationContext, LastSurvey::class.java)
        survey_intent.putExtra("mode", mode)
        startActivity(survey_intent)

        // finish this activity (collecting)
        finish()
    }

    private fun updateRemainingTimeUI() {
        if (binder != null) {
            remainingTime = binder!!.getRemainingTime()
            remainingText.text = "$remainingTime sec"
//            progressBar.progress = remainingTime
            Log.i(TAG, "UI remaining time : $remainingTime")
        } else {
            Log.e(TAG, "binder is null")
        }
    }

    private fun endCollecting() {
        Log.d(TAG, "endCollecting()")
        // stop the alarm
        ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent)

        // finish binding
        stopService(intent)
        if (isBound) { unbindService(connection) }
        isBound = false

//        // start new activity
//        val survey_intent = Intent(applicationContext, LastSurvey::class.java)
//        survey_intent.putExtra("mode", mode)
//        startActivity(survey_intent)
//
//        // finish this activity (collecting)
//        finish()
    }

    // timer thread
    inner class CountingThread : Runnable {
        override fun run() {
//            super.run()
            Log.i(TAG, "CountingThread run()")

            while(!stopCounting) {
//                try {
                    updateRemainingTimeUI()
                    if (remainingTime == 0) {
                        Log.i(TAG, "remaining time 0")

                        stopCounting = true
                        // notification
//                        val notification : Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//                        val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
//                        ringtone.play()
//                        val vibrator : Vibrator = getSystemService(VIBRATOR_MANAGER_SERVICE) as Vibrator
//                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))

                        // end collecting
                        endCollecting()
                    }
                    Thread.sleep(1000)
//                } catch (e : InterruptedException) {}
            }
        }
    }
}
