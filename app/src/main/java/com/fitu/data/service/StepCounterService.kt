package com.fitu.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fitu.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    @Inject
    lateinit var sensorManager: SensorManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var stepCounterSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    companion object {
        private val _stepCount = MutableStateFlow(0)
        val stepCount: StateFlow<Int> = _stepCount
        
        // For visualization
        private val _motionMagnitude = MutableStateFlow(0f)
        val motionMagnitude: StateFlow<Float> = _motionMagnitude
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        registerSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerSensors()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "step_counter_channel"
        val channelName = "Step Counter"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fitu Step Counter")
            .setContentText("Tracking your steps...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- High Precision Algorithm Refs (Ported from React) ---
    private val gravity = FloatArray(3)
    private var smoothedMag = 0.0
    private var isBelowReset = true
    private var lastStepTime = 0L

    // Tuning Constants for Accuracy (Exact matches from React)
    private val ALPHA = 0.92f
    private val SMOOTH_FACTOR = 0.7f
    private val STEP_THRESHOLD = 2.4f
    private val RESET_THRESHOLD = 1.2f
    private val MIN_STEP_TIME = 420L

    private fun registerSensors() {
        // Prioritize Accelerometer as per user request
        if (accelerometerSensor == null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        
        // Register Accelerometer
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) // Faster delay for better detection
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // 1. Gravity Filter
            gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x
            gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y
            gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z

            // 2. Linear Acceleration (Remove Gravity)
            val lx = x - gravity[0]
            val ly = y - gravity[1]
            val lz = z - gravity[2]

            // 3. Magnitude
            val rawMag = Math.sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()

            // 4. Smoothing
            smoothedMag = (SMOOTH_FACTOR * smoothedMag) + ((1 - SMOOTH_FACTOR) * rawMag)
            _motionMagnitude.value = smoothedMag.toFloat()

            // 5. Step Detection Logic
            val now = System.currentTimeMillis()
            if (smoothedMag > STEP_THRESHOLD && isBelowReset) {
                if (now - lastStepTime > MIN_STEP_TIME) {
                    // STEP DETECTED
                    _stepCount.value += 1
                    lastStepTime = now
                    isBelowReset = false
                }
            } else if (smoothedMag < RESET_THRESHOLD) {
                isBelowReset = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
