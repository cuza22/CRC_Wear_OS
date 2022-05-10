package com.example.crc_wear_os

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import java.lang.Exception

class BackGroundCollecting: Service() {
    private val TAG : String = "BackGroundColelcting"

    // Sensors
    private lateinit var sensorManager : SensorManager

    private lateinit var gravityListener : SensorEventListener
    private lateinit var gravitySensor : Sensor

    var graX : Float = 0.0f
    var graY : Float = 0.0f
    var graZ : Float = 0.0f


    // GPS
    private lateinit var location : Location

    // data
    var remainedTime : Int = 660
    private lateinit var collectingThread : CollectingThread
    lateinit var sensorData : List<String>

    // write
    val mode : String = ""



    override fun onBind(intent: Intent?): IBinder? {
        TODO()
    }


    override fun onCreate() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gravityListener = GravityListener()
        sensorManager.registerListener(gravityListener, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST)

    }

    override fun onDestroy() {
        super.onDestroy()

        // stop sensor listener
        sensorManager.unregisterListener(gravityListener)

        // stop location listener
        TODO()

        // write data as .csv
        TODO()
    }

    fun getMainData() {
        TODO()
        Log.i(TAG, "Gra : $graX, $graY, $graZ")
    }
    fun getGPSData() {
        TODO()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // getMainData()
        // getGPSData()
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
                // getMainData()

                try {
                    if (second == 60) {
                        second = 0
                        GPSsecond++
                        remainedTime--
                    }

                    if (GPSsecond == 5) {
                        // getGPSData()
                        GPSsecond = 0
                    }

                    if (remainedTime == 0) {
                        stopCollecting = true
                    }

                    sleep(16)

                } catch (e: Exception){
                    Log.e (TAG, e.toString())
                }
            }

        }
    }
}