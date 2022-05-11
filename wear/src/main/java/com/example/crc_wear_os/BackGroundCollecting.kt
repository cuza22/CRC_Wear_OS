package com.example.crc_wear_os

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.w3c.dom.ls.LSException
import java.lang.Exception
import java.util.*
import java.util.Calendar.*

class BackGroundCollecting: Service() {
    private val TAG : String = "BackGroundCollecting"

    // Sensors
    private lateinit var sensorManager : SensorManager

    private lateinit var gravityListener : SensorEventListener
    private lateinit var gravitySensor : Sensor

    var graX : Float = 0.0f
    var graY : Float = 0.0f
    var graZ : Float = 0.0f

    // GPS
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback : LocationCallback

    private lateinit var location : Location

    var latitude : Double = 0.0
    var longitude : Double = 0.0

    // data
    var remainedTime : Int = 660
    val SENSOR_FREQUENCY : Int = 5
    val LOCATION_INTERVAL : Int = 5

    private lateinit var collectingThread : CollectingThread
    private var sensorData : String = ""
    private var locationData : String = ""

    // write
    val mode : String = ""
    lateinit var cw : CSVWrite



    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind()")
        TODO()
    }


    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()

        // sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gravityListener = GravityListener()

        sensorManager.registerListener(gravityListener, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST)

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

        // data write
        cw = CSVWrite()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()

        // stop sensor listener
        sensorManager.unregisterListener(gravityListener)

        // stop location listener
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // write data as .csv
        val date = currentDate()
        cw.writeCsv(sensorData, date, "SensorData")
        cw.writeCsv(locationData, date, "GPSData")
    }

    fun getMainData() {
        Log.i(TAG, "Gra : $graX, $graY, $graZ")
        sensorData.plus("$graX, $graY, $graZ")
    }
    fun getLocationData() {
        Log.i(TAG, "lat: $latitude   lon: $longitude")
        locationData.plus("$latitude, $longitude")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        getMainData()
        getLocationData()
        collectingThread = CollectingThread()
        collectingThread.start()

        return super.onStartCommand(intent, flags, startId)
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


    inner class CollectingThread : Thread() {
        var stopCollecting : Boolean = false

        override fun run() {
            var second : Int = 0
            var GPSsecond : Int = 0

            while (!stopCollecting) {
                second++
                getMainData()

                try {
                    if (second == SENSOR_FREQUENCY) {
                        second = 0
                        GPSsecond++
                        remainedTime--

                        Log.i(TAG, sensorData)
                    }

                    if (GPSsecond == LOCATION_INTERVAL) {
                         getLocationData()
                        GPSsecond = 0
                    }

                    if (remainedTime == 0) {
                        stopCollecting = true
                    }

                    sleep((1000/SENSOR_FREQUENCY).toLong())

                } catch (e: Exception){
                    Log.e (TAG, e.toString())
                }
            }

        }
    }

    fun currentDate(): String {
        val calendar : Calendar = GregorianCalendar(Locale.KOREA)
        return "${calendar.get(YEAR)}_${calendar.get(MONTH)+1}_${calendar.get(DATE)}_${calendar.get(
            HOUR_OF_DAY)}_${calendar.get(MINUTE)}_${calendar.get(SECOND)}_$mode"
    }
}