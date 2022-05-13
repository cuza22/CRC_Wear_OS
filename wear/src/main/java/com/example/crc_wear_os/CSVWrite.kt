package com.example.crc_wear_os

import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CSVWrite {
    private val TAG : String = "CSVWrite"

    lateinit var directory : File
    lateinit var writer : FileWriter

    fun writeCsv(data : String, fileName : String, dataType : String) {
        if (Build.VERSION.SDK_INT <= 29) {
            val dirPath : String = Environment.getExternalStorageDirectory().absolutePath + "/HCILabData"
            directory = File(dirPath)
            if (!directory.exists()) { directory.mkdir() }
        } else {
            directory = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)
        }

        try {
            writer = FileWriter("$directory/$fileName _$dataType.csv")
            try {
                writer.write(data)
            } finally {
                writer.close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Can't save file")
            e.printStackTrace()
        }
    }
}