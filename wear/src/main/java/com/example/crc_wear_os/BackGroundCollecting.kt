package com.example.crc_wear_os

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.getActivity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.lights.Light
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.w3c.dom.ls.LSException
import java.lang.Exception
import java.util.*
import java.util.Calendar.*

class BackGroundCollecting: Service() {
    private val TAG : String = "BackGroundCollecting"

    // constants
    val COLLECTING_TIME : Int = 660
    val SENSOR_FREQUENCY : Int = 50
    val LOCATION_INTERVAL : Int = 5
    //    val SLEEP_TIME : Long = (1000/SENSOR_FREQUENCY).toLong()
    val SLEEP_TIME : Long = 16
    var startTime : Long = 0
//    val SAVE_INTERVAL : Int = 30
    lateinit var date: String

    // Sensors
    private lateinit var sensorManager : SensorManager

    private lateinit var gravityListener : SensorEventListener
    private lateinit var gravitySensor : Sensor
    private lateinit var accelerometerListener : SensorEventListener
    private lateinit var accelerometerSensor : Sensor
    private lateinit var gyrometerListener: SensorEventListener
    private lateinit var gyrometerSensor : Sensor
    private lateinit var magnetometerListener: SensorEventListener
    private lateinit var magnetometerSensor: Sensor
    private lateinit var lightListener: SensorEventListener
    private lateinit var lightSensor: Sensor
    private lateinit var baroListener: SensorEventListener
    private lateinit var baroSensor: Sensor
    private lateinit var heartRateListener: SensorEventListener
    private lateinit var heartRateSensor: Sensor

    var graX : Float = 0.0f
    var graY : Float = 0.0f
    var graZ : Float = 0.0f
    var accX : Float = 0.0f
    var accY : Float = 0.0f
    var accZ : Float = 0.0f
    var gyroX : Float = 0.0f
    var gyroY : Float = 0.0f
    var gyroZ : Float = 0.0f
    var magX : Float = 0.0f
    var magY : Float = 0.0f
    var magZ : Float = 0.0f
    var light : Float = 0.0f
    var barometer : Float = 0.0f
    var heartRate : Int = 0

    // GPS
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback : LocationCallback

    private lateinit var location : Location

    var latitude : Double = 0.0
    var longitude : Double = 0.0

    // data
    var remaining : Int = COLLECTING_TIME
    private lateinit var collectingThread : CollectingThread
    var stopCollecting : Boolean = false
    var isFirstCollecting : Boolean = true

    private var sensorDataHeader : String = "year, month, day, hour, min, sec, ms, graX, graY, graZ, accX, accY, accZ, gyroX, gyroY, gyroZ, magX, magY, magZ, light, barometer, hr\n"
    private var locationDataHeader : String = "year, month, day, hour, min, sec, ms, latitude, longitude\n"
    private var sensorData : String = ""
    private var locationData : String = ""

    // write
    var mode : String = ""
    lateinit var cw : CSVWrite

    // to keep service alive
    lateinit var serviceIntent : Intent
    lateinit var restartIntent : Intent
    var isBackgroundServiceAlive : Boolean = false
    lateinit var sender : PendingIntent
    lateinit var ac : AlarmManager.AlarmClockInfo
    lateinit var am : AlarmManager

    // when binding service
    private val binder = object : IMyAidlInterface.Stub() {
        override fun getRemainingTime() : Int {
            return remaining
        }
    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopCollecting = true
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()

        // data write
        cw = CSVWrite()

        // sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

//        Log.d(TAG, "Type of Sensors: " + sensorManager.getSensorList(Sensor.TYPE_ALL))

        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gravityListener = GravityListener()
        sensorManager.registerListener(gravityListener, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST)

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerListener = AccListener()
        sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST)

        gyrometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyrometerListener = GyroListener()
        sensorManager.registerListener(gyrometerListener, gyrometerSensor, SensorManager.SENSOR_DELAY_FASTEST)

        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        magnetometerListener = MagListener()
        sensorManager.registerListener(magnetometerListener, magnetometerSensor, SensorManager.SENSOR_DELAY_FASTEST)

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightListener = LightListener()
        sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST)

        baroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        baroListener = BaroListener()
        sensorManager.registerListener(baroListener, baroSensor, SensorManager.SENSOR_DELAY_FASTEST)

        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        heartRateListener = HeartRateListener()
        sensorManager.registerListener(heartRateListener,heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST)

        // location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(baseContext)
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(LOCATION_INTERVAL.toLong())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult.equals(null)) {
//                    Log.e(TAG, "location result null")
                        return
                }

                location = locationResult.lastLocation
                latitude = location.latitude
                longitude = location.longitude
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { return }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d(TAG, fusedLocationClient.toString())

        // for keeping BackGroundCollecting(Service) alive
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        var isWhiteListing = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(applicationContext.packageName)
        }
        if (!isWhiteListing) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + applicationContext.packageName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        restartIntent = Intent(this, RestartService::class.java)

        isBackgroundServiceAlive = true


    }

    fun setAlarm() {
        Log.d(TAG, "setAlarm()")
        val restart = getInstance()
        restart.timeInMillis = System.currentTimeMillis()
        restart.add(MILLISECOND, 16)

        restartIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("isFirstCollecting", false)
            putExtra("date", date)
            putExtra("mode", mode)
            putExtra("remaining", remaining)
        }
        sender = PendingIntent.getBroadcast(this, 0, restartIntent, 0)
        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.RTC_WAKEUP, restart.timeInMillis, sender)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        isFirstCollecting = intent?.getStringExtra("isFirstCollecting") as Boolean

        if (isFirstCollecting) {
            isFirstCollecting = false
            cw.createCsv(sensorDataHeader, "$date $mode", "sensorData")
            cw.createCsv(locationDataHeader, "$date $mode", "GPSData")
            date = currentDate()
        } else {
            date = intent?.extras?.get("date") as String
            mode = intent?.extras?.get("mode") as String
            remaining = intent?.extras?.get("remaining") as Int
        }

        collectingThread = CollectingThread()
        collectingThread.priority = Thread.MAX_PRIORITY
        collectingThread.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()

        isBackgroundServiceAlive = false

        setAlarm()
        Thread.currentThread().interrupt()

        // stop sensor listener
        sensorManager.unregisterListener(gravityListener)
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(gyrometerListener)
        sensorManager.unregisterListener(magnetometerListener)
        sensorManager.unregisterListener(lightListener)
        sensorManager.unregisterListener(heartRateListener)

        // stop location listener
//        fusedLocationClient.removeLocationUpdates(locationCallback)

        // write data as .csv
//        cw.writeCsv(sensorData, "SensorData")
//        cw.writeCsv(locationData, "GPSData")
    }

    fun setAlarmTimer() {
        var c : Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.SECOND, 1)
        }
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    fun getMainData() {
//        Log.i(TAG, "getMainData()")
//        Log.i(TAG, "Sensors : $graX, $accX, $gyroX, $magX   HR : $heartRate")
        sensorData += dataCollectedDate() + "$graX, $graY, $graZ, $accX, $accY, $accZ, $gyroX, $gyroY, $gyroZ, $magX, $magY, $magZ, $light, $barometer, $heartRate\n"

    }
    fun getLocationData() {
//        Log.i(TAG, "getLocationData()")
        Log.i(TAG, "lat: $latitude   lon: $longitude")
        locationData += dataCollectedDate() + "$latitude, $longitude\n"
    }



    inner class GravityListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                graX = event.values[0]
                graY = event.values[1]
                graZ = event.values[2]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    inner class AccListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                accX = event.values[0]
                accY = event.values[1]
                accZ = event.values[2]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    inner class GyroListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    inner class MagListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                magX = event.values[0]
                magY = event.values[1]
                magZ = event.values[2]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    inner class LightListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                light = event.values[0]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

    }

    inner class BaroListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                barometer = event.values[0]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    inner class HeartRateListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                heartRate = event.values[0].toInt()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

    }

    fun endService() {
        // write as csv
//        cw.writeCsv(sensorData, "SensorData")
//        cw.writeCsv(locationData, "GPSData")

        // start new activity
        val survey_intent = Intent(applicationContext, LastSurvey::class.java).apply {
            putExtra("mode", mode)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(survey_intent)

        // end BackGroundCollecting Service
        this.stopSelf()
    }

    inner class CollectingThread : Thread() {
        override fun run() {
//            super.run()
            Log.d(TAG, "CollectingThread run()")

            var second : Int = 0
            var GPSsecond : Int = 0

            startTime = System.currentTimeMillis()
            while (!stopCollecting) {
                second++
                try {
//                    Log.d(TAG, "CollectingThread is ${this.isAlive}")
//                    Log.d(TAG, "frequencyCount: $second   GPSsecond: $GPSsecond")
                    getMainData()

                    if (second == SENSOR_FREQUENCY) {
                        second = 0
                        GPSsecond++
                        remaining--

                        Log.d(TAG, "remaining: $remaining")
//                        Log.d(TAG, "remain_system: " + (remaining - (System.currentTimeMillis()-startTime)/1000).toString())
//                        Log.i(TAG, sensorData)
                    }

                    if (GPSsecond == LOCATION_INTERVAL) {
                        getLocationData()
                        GPSsecond = 0

//                        Log.i(TAG, locationData)

                        cw.writeCsv(sensorData, "sensorData")
                        cw.writeCsv(locationData, "GPSData")

                        sensorData = ""
                        locationData = ""
                    }

//                    if (((System.currentTimeMillis()-startTime)/1000) > remaining) {
                    if (remaining == 0) {
//                        Log.i(TAG, "remaining time 0")

                        stopCollecting = true
                        endService()

                    }

//                    Log.d(TAG, "remaining : $remaining")
                    sleep(SLEEP_TIME)
//                    sleep(12)

                } catch (e: Exception){
                    Log.e (TAG, e.toString())
                }
            }

        }

    }

    private fun currentDate(): String {
        val calendar : Calendar = GregorianCalendar(Locale.KOREA)
        return "${calendar.get(YEAR)}_${calendar.get(MONTH)+1}_${calendar.get(DATE)}_${calendar.get(
            HOUR_OF_DAY)}_${calendar.get(MINUTE)}_${calendar.get(SECOND)}_$mode"
    }
    private fun dataCollectedDate(): String {
        val calendar : Calendar = GregorianCalendar(Locale.KOREA)
        return "${calendar.get(YEAR)},${calendar.get(MONTH)+1},${calendar.get(DATE)},${calendar.get(
            HOUR_OF_DAY)},${calendar.get(MINUTE)},${calendar.get(SECOND)},${calendar.get(MILLISECOND)},"
    }
}