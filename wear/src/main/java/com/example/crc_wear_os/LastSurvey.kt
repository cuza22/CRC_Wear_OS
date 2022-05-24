package com.example.crc_wear_os

import android.app.Activity
import android.os.Bundle
import android.util.Log

class LastSurvey : Activity() {

    private val TAG = "LastSurvey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")
        setContentView(R.layout.activity_last_survey)
    }
    // TODO() : show text that data collecting has finished

    // TODO() : send data to phone by bluetooth

}