 package com.fitu.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fitu.MainActivity
import com.fitu.R
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    @Inject
    lateinit var sensorManager: SensorManager
    
    @Inject
    lateinit var stepDao: StepDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    
    private var initialStepCount: Int = -1
    private var stepsAtStartOfDay: Int = 0
    
    private var currentDate: String = ""
    private var dailyStepGoal: Int = 10000
    private val notifiedMilestones = mutableSetOf<Int>()

    companion object {
        private const val TAG = "StepCounterService"
        
        private val _stepCount = MutableStateFlow(0)
        val stepCount: StateFlow<Int> = _stepCount
        
        private val _motionMagnitude = MutableStateFlow(0f)
        val motionMagnitude: StateFlow<Float> = _motionMagnitude
        
        private val _isInitialized = MutableStateFlow(false)
        val isInitialized: StateFlow<Boolean> = _isInitialized
        
        private val _usesHardwareCounter = MutableStateFlow(false)
        val usesHardwareCounter: StateFlow<Boolean> = _usesHardwareCounter
        
        private const val NOTIFICATION_ID = 1
        private const val MILESTONE_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "step_counter_channel"
        private const val MILESTONE_CHANNEL_ID = "step_milestone_channel"
        
        val MILESTONES = listOf(20, 40, 60, 80, 100)
        
        fun getTodayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        
        suspend fun preloadSteps(stepDao: StepDao) {
            val today = getTodayDate()
            val todaySteps = stepDao.getStepsForDate(today)
            _stepCount.value = todaySteps?.steps ?: 0
            _isInitialized.value = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentDate = getTodayDate()
        
        loadTodayStepsSync()
        loadStepGoal()
        loadNotifiedMilestones()
        
        startForegroundService()
        registerSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val today = getTodayDate()
        if (today != currentDate) {
            handleDayChange(today)
        }
        
        intent?.getIntExtra("step_goal", -1)?.let { goal ->
            if (goal > 0) {
                dailyStepGoal = goal
            }
        }
        
        return START_STICKY
    }

    private fun handleDayChange(newDate: String) {
        currentDate = newDate
        saveSteps(_stepCount.value)
        
        _stepCount.value = 0
        stepsAtStartOfDay = 0
        initialStepCount = -1
        
        notifiedMilestones.clear()
        clearNotifiedMilestones()
        loadTodayStepsAsync()
    }

    private fun registerSensors() {
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        if (stepCounterSensor != null) {
            Log.d(TAG, "Using hardware TYPE_STEP_COUNTER")
            _usesHardwareCounter.value = true
            sensorManager.registerListener(
                this, 
                stepCounterSensor, 
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            
            if (stepDetectorSensor != null) {
                Log.d(TAG, "Using TYPE_STEP_DETECTOR (fallback)")
                _usesHardwareCounter.value = true
                sensorManager.registerListener(
                    this,
                    stepDetectorSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else {
                Log.w(TAG, "No hardware step sensor! Falling back to accelerometer")
                _usesHardwareCounter.value = false
                registerAccelerometerFallback()
            }
        }
    }

    private fun registerAccelerometerFallback() {
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        
        val today = getTodayDate()
        if (today != currentDate) {
            handleDayChange(today)
        }

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event)
            Sensor.TYPE_STEP_DETECTOR -> handleStepDetector()
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    private fun handleStepCounter(event: SensorEvent) {
        val totalStepsSinceBoot = event.values[0].toInt()
        
        if (initialStepCount < 0) {
            initialStepCount = totalStepsSinceBoot
            stepsAtStartOfDay = _stepCount.value
            Log.d(TAG, "Initial step count: $initialStepCount, steps at start: $stepsAtStartOfDay")
        }
        
        val stepsSinceServiceStart = totalStepsSinceBoot - initialStepCount
        val todaySteps = stepsAtStartOfDay + stepsSinceServiceStart
        
        if (todaySteps != _stepCount.value) {
            _stepCount.value = todaySteps
            checkMilestones(todaySteps)
            saveStepsPeriodically(todaySteps)
        }
    }

    private fun handleStepDetector() {
        _stepCount.value += 1
        checkMilestones(_stepCount.value)
        saveStepsPeriodically(_stepCount.value)
    }

    // Accelerometer fallback variables
    private val gravity = FloatArray(3)
    private var smoothedMag = 0.0
    private var isBelowReset = true
    private var lastStepTime = 0L

    private val ALPHA = 0.92f
    private val SMOOTH_FACTOR = 0.7f
    private val STEP_THRESHOLD = 2.4f
    private val RESET_THRESHOLD = 1.2f
    private val MIN_STEP_TIME = 420L

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z

        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]

        val rawMag = Math.sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()
        smoothedMag = (SMOOTH_FACTOR * smoothedMag) + ((1 - SMOOTH_FACTOR) * rawMag)
        _motionMagnitude.value = smoothedMag.toFloat()

        val now = System.currentTimeMillis()
        if (smoothedMag > STEP_THRESHOLD && isBelowReset) {
            if (now - lastStepTime > MIN_STEP_TIME) {
                _stepCount.value += 1
                lastStepTime = now
                isBelowReset = false
                checkMilestones(_stepCount.value)
                saveStepsPeriodically(_stepCount.value)
            }
        } else if (smoothedMag < RESET_THRESHOLD) {
            isBelowReset = true
        }
    }

    private var lastSaveTime = 0L
    private val SAVE_INTERVAL = 10000L

    private fun saveStepsPeriodically(steps: Int) {
        val now = System.currentTimeMillis()
        if (now - lastSaveTime > SAVE_INTERVAL) {
            saveSteps(steps)
            lastSaveTime = now
        }
    }

    private fun saveSteps(steps: Int) {
        serviceScope.launch {
            val entity = StepEntity(
                date = currentDate,
                steps = steps,
                lastUpdated = System.currentTimeMillis()
            )
            stepDao.insertOrUpdate(entity)
            updateNotification(steps)
        }
    }

    private fun loadStepGoal() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        dailyStepGoal = prefs.getInt("daily_step_goal", 10000)
    }

    private fun loadNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("milestone_date", "")
        
        if (savedDate == currentDate) {
            val savedMilestones = prefs.getStringSet("notified_milestones", emptySet()) ?: emptySet()
            notifiedMilestones.clear()
            notifiedMilestones.addAll(savedMilestones.mapNotNull { it.toIntOrNull() })
        } else {
            notifiedMilestones.clear()
            clearNotifiedMilestones()
        }
    }

    private fun saveNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("milestone_date", currentDate)
            .putStringSet("notified_milestones", notifiedMilestones.map { it.toString() }.toSet())
            .apply()
    }

    private fun clearNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("milestone_date", currentDate)
            .putStringSet("notified_milestones", emptySet())
            .apply()
    }

    private fun loadTodayStepsSync() {
        try {
            runBlocking(Dispatchers.IO) {
                val todaySteps = stepDao.getStepsForDate(currentDate)
                _stepCount.value = todaySteps?.steps ?: 0
                stepsAtStartOfDay = _stepCount.value
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading steps", e)
            loadTodayStepsAsync()
        }
    }

    private fun loadTodayStepsAsync() {
        serviceScope.launch {
            val todaySteps = stepDao.getStepsForDate(currentDate)
            _stepCount.value = todaySteps?.steps ?: 0
            stepsAtStartOfDay = _stepCount.value
            _isInitialized.value = true
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val stepChannel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your daily steps"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(stepChannel)
            
            val milestoneChannel = NotificationChannel(
                MILESTONE_CHANNEL_ID,
                "Step Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for step goal progress"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(milestoneChannel)
        }

        val notification = buildNotification(_stepCount.value)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(steps: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val progress = if (dailyStepGoal > 0) ((steps.toFloat() / dailyStepGoal) * 100).toInt() else 0
        val sensorType = if (_usesHardwareCounter.value) "HW" else "SW"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏃 Fitu Step Counter")
            .setContentText("$steps steps today ($progress%) [$sensorType]")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(steps: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun checkMilestones(steps: Int) {
        if (dailyStepGoal <= 0) return
        
        val currentProgress = ((steps.toFloat() / dailyStepGoal) * 100).toInt()
        
        for (milestone in MILESTONES) {
            if (currentProgress >= milestone && milestone !in notifiedMilestones) {
                sendMilestoneNotification(steps, milestone)
                notifiedMilestones.add(milestone)
                saveNotifiedMilestones()
            }
        }
    }

    private fun sendMilestoneNotification(steps: Int, milestone: Int) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val (title, message) = when (milestone) {
            20 -> "🚶 20% Complete!" to "You've taken $steps steps. Keep moving!"
            40 -> "🏃 40% Complete!" to "You've taken $steps steps. Almost halfway there!"
            60 -> "💪 60% Complete!" to "You've taken $steps steps. Over halfway!"
            80 -> "🔥 80% Complete!" to "You've taken $steps steps. Almost at your goal!"
            100 -> "🎉 GOAL REACHED!" to "Congratulations! You've completed $steps steps today!"
            else -> "Step Progress" to "You've taken $steps steps"
        }
        
        val notification = NotificationCompat.Builder(this, MILESTONE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(MILESTONE_NOTIFICATION_ID + milestone, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        saveSteps(_stepCount.value)
        serviceScope.cancel()
    }
} 
