package com.example.crc_wear_os

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.google.android.gms.wearable.*
import java.io.ByteArrayOutputStream
import java.io.File

class LastSurvey : Activity() {

    private val TAG = "LastSurvey"

    lateinit var dataClient : DataClient
    lateinit var putDataMapRequest : PutDataMapRequest
    lateinit var dataMap : DataMap
    lateinit var putDataRequest: PutDataRequest
//    lateinit var putDataTask

    lateinit var directory : File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")
        setContentView(R.layout.activity_last_survey)

        if (Build.VERSION.SDK_INT <= 29) {
            // 외부저장소 경로 (~ API 28)
            val dirPath = Environment.getExternalStorageDirectory().absolutePath + "/HCILabData"
            //디렉토리 없으면 생성
            directory = File(dirPath)
            if (!directory.exists()) {
                directory.mkdir()
            }
            // 앱 고유 외부저장소 경로 (API 29 ~)
        } else {
            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        }

/*
        dataClient = Wearable.getDataClient(applicationContext)
        putDataMapRequest = PutDataMapRequest.create(path)
*/
    }
/*
    // TODO() : send data to phone by bluetooth
    private fun createAsset(file: File): Asset =
        ByteArrayOutputStream().let { stream ->
            file.readBytes()
        }
*/

}