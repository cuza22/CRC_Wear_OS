package com.example.crc_wear_os

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
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
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class Collecting : Activity() {

    private val TAG = "Collecting"

    // for keeping the app awake
    private val AMBIENT_UPDATE_ACTION = "com.your.package.action.AMBIENT_UPDATE"
    private lateinit var ambientUpdateAlarmManager: AlarmManager
    private lateinit var ambientUpdatePendingIntent: PendingIntent
    private lateinit var ambientUpdateBroadcastReceiver: BroadcastReceiver

    // UI
    private lateinit var remainingText : TextView

    // intent
    private lateinit var mode : String

    // settings
    private val COLLECTING_TIME : Int = 30
    private val SENSOR_FREQUENCY : Int = 20
    private val LOCATION_INTERVAL : Int = 5

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

    private var sensorData : String = "year, month, day, hour, min, sec, ms, graX, graY, graZ, accX, accY, accZ, gyroX, gyroY, gyroZ, magX, magY, magZ, light, barometer, HR\n"
    private var locationData : String = "latitude, longitude\n"

    // write
    lateinit var cw : CSVWrite


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collecting)

        // get information from intent
        mode = getIntent().extras?.get("mode") as String
        Log.d(TAG, "mode: $mode")

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

        // set UI
        remainingText = findViewById(R.id.remaining_time)
        remainingText.text = "$mode\n$COLLECTING_TIME sec"

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
            .setInterval((1000/SENSOR_FREQUENCY).toLong())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult.equals(null)) {
                    Log.e(TAG, "location result null")
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


        // start [counting & collecting] thread
        thread(start = true){
            Log.d(TAG, "starting thread")

            var stopCollecting = false
            var remainingTime = COLLECTING_TIME
            var frequencyCount = 0
            var GPSCount = 0

            while (!stopCollecting) {
                Log.d(TAG, "stopcollecting true")
                frequencyCount++
                getMainData()

                if (frequencyCount == SENSOR_FREQUENCY) {
                    frequencyCount = 0
                    GPSCount++
                    remainingTime--
                    runOnUiThread {
                        remainingText.text = "$mode\n$remainingTime sec"
                        Log.d(TAG, "remaining UI : $remainingTime")
                    }
                }
                if (GPSCount == LOCATION_INTERVAL) {
                    getLocationData()
                    GPSCount = 0
                }
                if (remainingTime == 0) {
                    stopCollecting = true
                }

                Log.d(TAG, "frequencyCount: $frequencyCount   GPSCount: $GPSCount")

                Thread.sleep((1000/SENSOR_FREQUENCY).toLong())
            }
        }
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

        // end things related to collecting data
        endCollecting()

        // start new activity
        val survey_intent = Intent(applicationContext, LastSurvey::class.java)
        survey_intent.putExtra("mode", mode)
        startActivity(survey_intent)

        // finish this activity (collecting)
        finish()
    }
    fun getMainData() {
//        Log.i(TAG, "getMainData()")
//        Log.i(TAG, "Gra : $graX, $graY, $graZ   HR : $heartRate")
        sensorData += dataCollectedDate() + "$graX, $graY, $graZ, $accX, $accY, $accZ, $gyroX, $gyroY, $gyroZ, $magX, $magY, $magZ, $light, $barometer, $heartRate\n"

    }
    fun getLocationData() {
//        Log.i(TAG, "getLocationData()")
        Log.i(TAG, "lat: $latitude   lon: $longitude")
        locationData += dataCollectedDate() + "$latitude, $longitude\n"
    }

    private fun endCollecting() {
        Log.d(TAG, "endCollecting()")

        // stop sensor listener
        sensorManager.unregisterListener(gravityListener)
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(gyrometerListener)
        sensorManager.unregisterListener(magnetometerListener)
        sensorManager.unregisterListener(lightListener)
        sensorManager.unregisterListener(heartRateListener)

        // stop location listener
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // write data as .csv
        val date = currentDate()
        cw.writeCsv(sensorData, date, "SensorData")
        cw.writeCsv(locationData, date, "GPSData")

        // stop the alarm
        ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent)
    }

    // listeners
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

    // calendar
    private fun currentDate(): String {
        val calendar : Calendar = GregorianCalendar(Locale.KOREA)
        return "${calendar.get(Calendar.YEAR)}_${calendar.get(Calendar.MONTH)+1}_${calendar.get(
            Calendar.DATE
        )}_${calendar.get(
            Calendar.HOUR_OF_DAY
        )}_${calendar.get(Calendar.MINUTE)}_${calendar.get(Calendar.SECOND)}_$mode"
    }
    private fun dataCollectedDate(): String {
        val calendar : Calendar = GregorianCalendar(Locale.KOREA)
        return "${calendar.get(Calendar.YEAR)},${calendar.get(Calendar.MONTH)+1},${calendar.get(
            Calendar.DATE
        )},${calendar.get(
            Calendar.HOUR_OF_DAY
        )},${calendar.get(Calendar.MINUTE)},${calendar.get(Calendar.SECOND)},${calendar.get(Calendar.MILLISECOND)},"
    }

}
