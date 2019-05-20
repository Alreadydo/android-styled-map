package com.trien.dnmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.ArrayList

object Utils {
    /**
     * Default log tag name for log message.
     */
    private val LOG_TAG = MapsActivity::class.java.name

    /**
     * Keyword constants for reading values from polylines.csv.
     * Important: these keywords values must be exactly the same as ones in polylines.csv file in raw folder.
     */
    val ENCODED_POINTS = "encodedPoints"
    val LAT_LNG_POINT = "latLngPoint"
    val MARKER = "marker"

    /**
     * Helper method to get polyline points by decoding an encoded coordinates string read from CSV file.
     */
    fun readEncodedPolyLinePointsFromCSV(context: Context, lineKeyword: String): List<LatLng> {

        // Create an InputStream object.
        val `is` = context.resources.openRawResource(R.raw.polylines)
        // Create a BufferedReader object to read values from CSV file.
        val reader = BufferedReader(InputStreamReader(`is`, Charset.forName("UTF-8")))
        var line = ""
        // Create a list of LatLng objects.
        val latLngList = ArrayList<LatLng>()

        try {
            while (true) {
                line = reader.readLine() ?: break
                Log.v("trienoi", line)
                // Split the line into different tokens (using the comma as a separator).
                val tokens = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // Only add the right latlng points to a desired line by color.
                if (tokens.isNotEmpty())
                    if (tokens[0].trim().equals(lineKeyword) && tokens[1].trim().equals(ENCODED_POINTS)) {
                        // Use PolyUtil to decode the polylines path into list of LatLng objects.
                        latLngList.addAll(PolyUtil.decode(tokens[2].trim { it <= ' ' }.replace("\\\\", "\\")))

                        Log.d(LOG_TAG + lineKeyword, tokens[2].trim { it <= ' ' })
                        for (lat in latLngList) {
                            Log.d(LOG_TAG + lineKeyword, lat.latitude.toString() + ", " + lat.longitude)
                        }

                    } else {
                        Log.d(LOG_TAG, "null")
                    }
            }
        } catch (e1: IOException) {
            Log.e(LOG_TAG, "Error$line", e1)
            e1.printStackTrace()
        }

        return latLngList
    }

    /**
     * Helper method to read polyline points from CSV file.
     */
    fun readPolyLinePointsFromCSV(context: Context, lineKeyword: String): List<LatLng> {

        // Create an InputStream object.
        val `is` = context.resources.openRawResource(R.raw.polylines)
        // Create a BufferedReader object to read values from CSV file.
        val reader = BufferedReader(
                InputStreamReader(`is`, Charset.forName("UTF-8")))
        var line = ""
        // Create a list of LatLng objects.
        val latLngList = ArrayList<LatLng>()

        try {
            while (true) {
                line = reader.readLine() ?: break
                // Split the line into different tokens (using the comma as a separator).
                val tokens = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // Only add the right latlng points to a desired line by color.
                if (tokens.isNotEmpty())
                    if (tokens[0].trim().equals(lineKeyword) && tokens[1].trim().equals(LAT_LNG_POINT)) {
                        latLngList.add(LatLng(java.lang.Double.parseDouble(tokens[2].trim { it <= ' ' }), java.lang.Double.parseDouble(tokens[3].trim { it <= ' ' })))
                    } else {
                        Log.d(LOG_TAG, " null")
                    }
            }
        } catch (e1: IOException) {
            Log.e(LOG_TAG, "Error$line", e1)
            e1.printStackTrace()
        }

        for (lat in latLngList) {
            Log.d(LOG_TAG + lineKeyword, lat.latitude.toString() + ", " + lat.longitude)
        }
        return latLngList
    }

    /**
     * Helper method to read markers lat lng values from CSV file.
     */
    fun readMarkersFromCSV(context: Context, lineKeyword: String): List<LatLng> {

        // Create an InputStream object
        val `is` = context.resources.openRawResource(R.raw.polylines)
        // Create a BufferedReader object to read values from CSV file.
        val reader = BufferedReader(
                InputStreamReader(`is`, Charset.forName("UTF-8")))
        var line = ""
        // Create a list of LatLng objects.
        val latLngList = ArrayList<LatLng>()

        try {
            while (true) {
                line = reader.readLine() ?: break
                // Split the line into different tokens (using the comma as a separator).
                val tokens = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // Only add the right latlng points by color.
                if (tokens.isNotEmpty())
                    if (tokens[0].trim().equals(lineKeyword) && tokens[1].trim().equals(MARKER)) {
                        latLngList.add(LatLng(java.lang.Double.parseDouble(tokens[2].trim { it <= ' ' }), java.lang.Double.parseDouble(tokens[3].trim { it <= ' ' })))
                        Log.d(LOG_TAG + lineKeyword, tokens[2].trim { it <= ' ' } + tokens[3].trim { it <= ' ' })
                    } else {
                        Log.d(LOG_TAG, " null")
                    }
            }
        } catch (e1: IOException) {
            Log.e(LOG_TAG, "Error$line", e1)
            e1.printStackTrace()
        }

        return latLngList
    }

    /**
     * Marker bitmap resize tool. This will create a sized bitmap to apply in markers based on input drawable.
     */
    fun resizeMarker(context: Context, drawable: Int): Bitmap {
        val bitmapDrawable = context.resources.getDrawable(drawable) as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap
        // Change expectedWidth's value to your desired one.
        val expectedWidth = 60
        return Bitmap.createScaledBitmap(bitmap, expectedWidth, bitmap.height * expectedWidth / bitmap.width, false)
    }

    /**
     * Annotation bitmap resize tool. This will create a sized bitmap to apply in annotations based on input drawable.
     */
    fun resizeCommonAnnotation(context: Context, drawable: Int): Bitmap {
        val bitmapDrawable = context.resources.getDrawable(drawable) as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap
        // Change scale's value to your desired one.
        val scale = 0.15f
        val newWidth = (bitmapDrawable.bitmap.width * scale).toInt()
        val newHeight = (bitmapDrawable.bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }

}
