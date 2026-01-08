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

    // Simple step detection variables for accelerometer fallback
    private var lastMagnitude = 0.0
    private val magnitudeThreshold = 12.0 // Adjust based on testing

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

        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Fallback to accelerometer
            accelerometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
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

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            _stepCount.value = event.values[0].toInt()
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble())
            _motionMagnitude.value = magnitude.toFloat()

            // Simple peak detection
            if (magnitude > magnitudeThreshold && lastMagnitude <= magnitudeThreshold) {
                _stepCount.value += 1
            }
            lastMagnitude = magnitude
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
