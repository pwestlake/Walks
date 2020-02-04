package com.pwestlake.walks.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.pwestlake.walks.utils.Trkpt
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.stream.Collectors

class GPSService : Service() {
    private val binder = LocalBinder()
    private val TAG = "GPSService"
    private val CHANNEL_ID = "walk"
    private val MAX_SPEED = 3F

    private lateinit var velocityMeasureMentJob: Job
    val path = LinkedHashSet<Trkpt>()

    private var velocity = floatArrayOf(0F, 0F) // m/s
    private var sensorSpeed = 0F
    private var velocityMeasuredAt = Date()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationRequest = LocationRequest.create()?.apply {
        interval = 5000
        fastestInterval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        smallestDisplacement = 10F
    }

    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Ignore
        }

        override fun onSensorChanged(event: SensorEvent?) {
            val t = Date()
            val interval = (t.time - velocityMeasuredAt.time) / 1000.0F
            velocityMeasuredAt = t

            val ax = event?.values?.get(0) ?: 0.0F
            val ay = event?.values?.get(1) ?: 0.0F

            velocity[0] = velocity[0] + (ax * interval)
            velocity[1] = velocity[1] + (ay * interval)

            sensorSpeed = Math.sqrt(Math.pow(velocity[0].toDouble(), 2.0)
                + Math.pow(velocity[1].toDouble(), 2.0)).toFloat()
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            var lastLocation: Location? = null
            var lastMeasurement = Date()

            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    val displacement: Float = lastLocation?.distanceTo(location) ?: 0F
                    val speed = (displacement * 1000)/ (Date().time - lastMeasurement.time) // m/s

                    if (speed <= MAX_SPEED) {
                        val trkpt = Trkpt(
                            location.altitude,
                            location.latitude,
                            location.longitude,
                            velocity
                        )

                        if (path.add(trkpt)) {

                            Intent().also { intent ->
                                intent.setAction("com.pwestlake.action.LOCATION_UPDATE")
                                intent.putExtra("trkpt", trkpt)
                                sendBroadcast(intent)
                            }
                        }
                    }

                    lastLocation = location
                    lastMeasurement = Date()
                }
            }
        }
    }

    fun startTracking(): Unit {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.pwestlake.walks.R.drawable.ic_directions_walk_24px)
            .setContentTitle("Walks")
            .setContentText("Service running")
            .setPriority(NotificationCompat.PRIORITY_LOW)

        with(NotificationManagerCompat.from(this)) {
            notify(0, builder.build())
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())

        startVelocityMeasurement()
    }

    private fun startVelocityMeasurement() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        sensor.also {
                sensor -> sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun pauseTracking(): Unit {
        stopLocationClient()
        stopVelocityMeasureMent()
    }

    private fun stopVelocityMeasureMent() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        sensor.also {
                sensor -> sensorManager.unregisterListener(sensorListener)
        }
    }

    private fun stopLocationClient() {
        val task = fusedLocationClient.removeLocationUpdates(locationCallback)
        task.addOnSuccessListener {
            Log.i(TAG, "Successfully removed location client")
        }

        task.addOnFailureListener {
            Log.w(TAG, "Failed to remove location client")
        }
    }

    fun stopTracking(): Unit {
        stopLocationClient()
        stopVelocityMeasureMent()
        path.clear()

        with(NotificationManagerCompat.from(this)) {
            cancel(0)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): GPSService = this@GPSService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "Destroying service")
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "walkchannel"
            val descriptionText = "Notification channel for walks app"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
