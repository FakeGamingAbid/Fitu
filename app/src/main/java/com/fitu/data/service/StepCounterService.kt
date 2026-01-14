package com.fitu.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
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
    
    // Step tracking state
    private var initialStepCount: Int = -1
    private var lastKnownHardwareSteps: Int = -1
    private var stepsAtStartOfDay: Int = 0
    
    private var currentDate: String = ""
    private var dailyStepGoal: Int = 10000
    
    // Thread-safe milestone tracking
    private val notifiedMilestones = ConcurrentHashMap.newKeySet<Int>()

    // Persistent storage for step state
    private val stepStatePrefs: SharedPreferences by lazy {
        getSharedPreferences("fitu_step_state", Context.MODE_PRIVATE)
    }

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
        
        // Pref keys for persisting step state
        private const val PREF_INITIAL_STEP_COUNT = "initial_step_count"
        private const val PREF_LAST_HARDWARE_STEPS = "last_hardware_steps"
        private const val PREF_STEPS_AT_START_OF_DAY = "steps_at_start_of_day"
        private const val PREF_CURRENT_DATE = "current_date"
        private const val PREF_LAST_SAVED_STEPS = "last_saved_steps"
        
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
        
        // Restore persisted state first
        restorePersistedState()
        
        // Load today's steps from database
        loadTodayStepsAsync()
        
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

    /**
     * Restore persisted step tracking state.
     * This handles the case where the service was killed and restarted.
     */
    private fun restorePersistedState() {
        val savedDate = stepStatePrefs.getString(PREF_CURRENT_DATE, "") ?: ""
        val today = getTodayDate()
        
        if (savedDate == today) {
            // Same day - restore state
            initialStepCount = stepStatePrefs.getInt(PREF_INITIAL_STEP_COUNT, -1)
            lastKnownHardwareSteps = stepStatePrefs.getInt(PREF_LAST_HARDWARE_STEPS, -1)
            stepsAtStartOfDay = stepStatePrefs.getInt(PREF_STEPS_AT_START_OF_DAY, 0)
            
            // Restore the last saved step count to avoid showing 0 briefly
            val lastSavedSteps = stepStatePrefs.getInt(PREF_LAST_SAVED_STEPS, 0)
            _stepCount.value = lastSavedSteps
            
            Log.d(TAG, "Restored state: initialStepCount=$initialStepCount, " +
                    "lastHardwareSteps=$lastKnownHardwareSteps, " +
                    "stepsAtStartOfDay=$stepsAtStartOfDay, " +
                    "lastSavedSteps=$lastSavedSteps")
        } else {
            // New day or first run - reset state
            Log.d(TAG, "New day or first run, resetting state (saved: $savedDate, today: $today)")
            resetPersistedState()
        }
    }

    /**
     * Persist current step tracking state.
     */
    private fun persistState() {
        stepStatePrefs.edit()
            .putString(PREF_CURRENT_DATE, currentDate)
            .putInt(PREF_INITIAL_STEP_COUNT, initialStepCount)
            .putInt(PREF_LAST_HARDWARE_STEPS, lastKnownHardwareSteps)
            .putInt(PREF_STEPS_AT_START_OF_DAY, stepsAtStartOfDay)
            .putInt(PREF_LAST_SAVED_STEPS, _stepCount.value)
            .apply()
    }

    /**
     * Reset persisted state for a new day.
     */
    private fun resetPersistedState() {
        initialStepCount = -1
        lastKnownHardwareSteps = -1
        stepsAtStartOfDay = 0
        
        stepStatePrefs.edit()
            .putString(PREF_CURRENT_DATE, currentDate)
            .putInt(PREF_INITIAL_STEP_COUNT, -1)
            .putInt(PREF_LAST_HARDWARE_STEPS, -1)
            .putInt(PREF_STEPS_AT_START_OF_DAY, 0)
            .putInt(PREF_LAST_SAVED_STEPS, 0)
            .apply()
    }

    private fun handleDayChange(newDate: String) {
        Log.d(TAG, "Day changed from $currentDate to $newDate")
        
        // Save final steps for the old day
        saveSteps(_stepCount.value)
        
        // Update to new day
        currentDate = newDate
        
        // Reset for new day
        _stepCount.value = 0
        stepsAtStartOfDay = 0
        initialStepCount = -1
        lastKnownHardwareSteps = -1
        
        // Clear milestones for new day
        notifiedMilestones.clear()
        clearNotifiedMilestones()
        
        // Reset persisted state
        resetPersistedState()
        
        // Load any existing steps for new day (e.g., from another device sync)
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
        
        // First reading after service start/restart
        if (initialStepCount < 0) {
            // Check if we have a valid last known hardware step count from before restart
            if (lastKnownHardwareSteps > 0 && totalStepsSinceBoot >= lastKnownHardwareSteps) {
                // Service was restarted - calculate steps taken during downtime
                val stepsDuringDowntime = totalStepsSinceBoot - lastKnownHardwareSteps
                
                // Add downtime steps to current count
                val previousSteps = _stepCount.value
                _stepCount.value = previousSteps + stepsDuringDowntime
                
                Log.d(TAG, "Service restart detected. Steps during downtime: $stepsDuringDowntime, " +
                        "Previous: $previousSteps, New total: ${_stepCount.value}")
                
                // Set initial count to current hardware steps
                initialStepCount = totalStepsSinceBoot
                stepsAtStartOfDay = _stepCount.value
            } else if (lastKnownHardwareSteps > 0 && totalStepsSinceBoot < lastKnownHardwareSteps) {
                // Device was rebooted (hardware counter reset)
                Log.d(TAG, "Device reboot detected (hardware steps reset from $lastKnownHardwareSteps to $totalStepsSinceBoot)")
                
                // Keep our current step count, just update the initial reference
                initialStepCount = totalStepsSinceBoot
                stepsAtStartOfDay = _stepCount.value
            } else {
                // Fresh start (no previous state)
                initialStepCount = totalStepsSinceBoot
                stepsAtStartOfDay = _stepCount.value
                Log.d(TAG, "Fresh start. Initial step count: $initialStepCount, steps at start: $stepsAtStartOfDay")
            }
        }
        
        // Calculate today's steps
        val stepsSinceServiceStart = totalStepsSinceBoot - initialStepCount
        val todaySteps = stepsAtStartOfDay + stepsSinceServiceStart
        
        // Update last known hardware steps for next restart
        lastKnownHardwareSteps = totalStepsSinceBoot
        
        if (todaySteps != _stepCount.value && todaySteps >= 0) {
            _stepCount.value = todaySteps
            checkMilestones(todaySteps)
            saveStepsPeriodically(todaySteps)
            
            // Persist state for recovery
            persistState()
        }
    }

    private fun handleStepDetector() {
        val newSteps = _stepCount.value + 1
        _stepCount.value = newSteps
        checkMilestones(newSteps)
        saveStepsPeriodically(newSteps)
        persistState()
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
                val newSteps = _stepCount.value + 1
                _stepCount.value = newSteps
                lastStepTime = now
                isBelowReset = false
                checkMilestones(newSteps)
                saveStepsPeriodically(newSteps)
                persistState()
            }
        } else if (smoothedMag < RESET_THRESHOLD) {
            isBelowReset = true
        }
    }

    // Thread-safe save timing
    private val lastSaveTime = AtomicLong(0L)
    private val SAVE_INTERVAL = 10000L // 10 seconds

    private fun saveStepsPeriodically(steps: Int) {
        val now = System.currentTimeMillis()
        val lastSave = lastSaveTime.get()
        
        if (now - lastSave > SAVE_INTERVAL) {
            if (lastSaveTime.compareAndSet(lastSave, now)) {
                saveSteps(steps)
            }
        }
    }

    private fun saveSteps(steps: Int) {
        serviceScope.launch {
            try {
                val entity = StepEntity(
                    date = currentDate,
                    steps = steps,
                    lastUpdated = System.currentTimeMillis()
                )
                stepDao.insertOrUpdate(entity)
                updateNotification(steps)
                
                // Also persist to SharedPreferences for quick recovery
                stepStatePrefs.edit()
                    .putInt(PREF_LAST_SAVED_STEPS, steps)
                    .apply()
                    
                Log.d(TAG, "Saved steps: $steps for date: $currentDate")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving steps", e)
            }
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
            savedMilestones.mapNotNull { it.toIntOrNull() }.forEach { notifiedMilestones.add(it) }
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

    private fun loadTodayStepsAsync() {
        serviceScope.launch {
            try {
                val todaySteps = stepDao.getStepsForDate(currentDate)
                val dbSteps = todaySteps?.steps ?: 0
                
                // Only update if DB has more steps (e.g., synced from another source)
                // or if we don't have any steps yet
                if (dbSteps > _stepCount.value || _stepCount.value == 0) {
                    _stepCount.value = dbSteps
                    stepsAtStartOfDay = dbSteps
                    
                    // Update persisted state
                    stepStatePrefs.edit()
                        .putInt(PREF_STEPS_AT_START_OF_DAY, dbSteps)
                        .putInt(PREF_LAST_SAVED_STEPS, dbSteps)
                        .apply()
                }
                
                _isInitialized.value = true
                Log.d(TAG, "Loaded steps from DB: $dbSteps, current: ${_stepCount.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading steps from DB", e)
                _isInitialized.value = true
            }
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
            if (currentProgress >= milestone && !notifiedMilestones.contains(milestone)) {
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
        Log.d(TAG, "Service onDestroy - saving final state")
        
        sensorManager.unregisterListener(this)
        
        // Save final state before destruction
        saveSteps(_stepCount.value)
        persistState()
        
        serviceScope.cancel()
    }
}
