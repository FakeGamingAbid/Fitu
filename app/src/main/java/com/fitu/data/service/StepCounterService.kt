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

    private var accelerometerSensor: Sensor? = null
    
    // Current date tracking
    private var currentDate: String = ""
    
    // ✅ NEW: Daily step goal (will be set from preferences)
    private var dailyStepGoal: Int = 10000
    
    // ✅ NEW: Track which milestones have been notified today
    private val notifiedMilestones = mutableSetOf<Int>()

    companion object {
        private val _stepCount = MutableStateFlow(0)
        val stepCount: StateFlow<Int> = _stepCount
        
        private val _motionMagnitude = MutableStateFlow(0f)
        val motionMagnitude: StateFlow<Float> = _motionMagnitude
        
        // Track if steps have been loaded from database
        private val _isInitialized = MutableStateFlow(false)
        val isInitialized: StateFlow<Boolean> = _isInitialized
        
        private const val NOTIFICATION_ID = 1
        private const val MILESTONE_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "step_counter_channel"
        private const val MILESTONE_CHANNEL_ID = "step_milestone_channel"
        
        // ✅ NEW: Milestone percentages
        val MILESTONES = listOf(20, 40, 60, 80, 100)
        
        fun getTodayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        
        /**
         * Pre-load steps from database before service fully starts
         */
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
        
        // Load steps synchronously to prevent showing 0
        loadTodayStepsSync()
        
        // Load step goal from shared preferences
        loadStepGoal()
        
        // Load notified milestones for today
        loadNotifiedMilestones()
        
        startForegroundService()
        registerSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if date changed (past midnight)
        val today = getTodayDate()
        if (today != currentDate) {
            currentDate = today
            _stepCount.value = 0
            notifiedMilestones.clear()  // ✅ Reset milestones for new day
            clearNotifiedMilestones()
            loadTodayStepsAsync()
        }
        
        // ✅ Update step goal if passed via intent
        intent?.getIntExtra("step_goal", -1)?.let { goal ->
            if (goal > 0) {
                dailyStepGoal = goal
            }
        }
        
        registerSensors()
        return START_STICKY
    }

    /**
     * Load step goal from SharedPreferences
     */
    private fun loadStepGoal() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        dailyStepGoal = prefs.getInt("daily_step_goal", 10000)
    }
    
    /**
     * ✅ NEW: Save step goal (called from outside)
     */
    fun updateStepGoal(goal: Int) {
        dailyStepGoal = goal
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("daily_step_goal", goal).apply()
    }

    /**
     * ✅ NEW: Load which milestones were already notified today
     */
    private fun loadNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("milestone_date", "")
        
        if (savedDate == currentDate) {
            // Same day, load saved milestones
            val savedMilestones = prefs.getStringSet("notified_milestones", emptySet()) ?: emptySet()
            notifiedMilestones.clear()
            notifiedMilestones.addAll(savedMilestones.mapNotNull { it.toIntOrNull() })
        } else {
            // New day, clear milestones
            notifiedMilestones.clear()
            clearNotifiedMilestones()
        }
    }
    
    /**
     * ✅ NEW: Save notified milestones
     */
    private fun saveNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("milestone_date", currentDate)
            .putStringSet("notified_milestones", notifiedMilestones.map { it.toString() }.toSet())
            .apply()
    }
    
    /**
     * ✅ NEW: Clear notified milestones (new day)
     */
    private fun clearNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("milestone_date", currentDate)
            .putStringSet("notified_milestones", emptySet())
            .apply()
    }

    /**
     * Load steps synchronously (blocks briefly but prevents 0 display)
     */
    private fun loadTodayStepsSync() {
        try {
            runBlocking(Dispatchers.IO) {
                val todaySteps = stepDao.getStepsForDate(currentDate)
                _stepCount.value = todaySteps?.steps ?: 0
                _isInitialized.value = true
            }
        } catch (e: Exception) {
            loadTodayStepsAsync()
        }
    }

    /**
     * Load steps asynchronously (used for date changes)
     */
    private fun loadTodayStepsAsync() {
        serviceScope.launch {
            val todaySteps = stepDao.getStepsForDate(currentDate)
            _stepCount.value = todaySteps?.steps ?: 0
            _isInitialized.value = true
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
            
            // Update notification with current step count
            updateNotification(steps)
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ✅ Channel for ongoing step counter
            val stepChannel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your daily steps"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(stepChannel)
            
            // ✅ NEW: Channel for milestone notifications (higher importance)
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
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏃 Fitu Step Counter")
            .setContentText("$steps steps today ($progress%)")
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
    
    /**
     * ✅ NEW: Check and send milestone notifications
     */
    private fun checkMilestones(steps: Int) {
        if (dailyStepGoal <= 0) return
        
        val currentProgress = ((steps.toFloat() / dailyStepGoal) * 100).toInt()
        
        for (milestone in MILESTONES) {
            if (currentProgress >= milestone && milestone !in notifiedMilestones) {
                // Send milestone notification
                sendMilestoneNotification(steps, milestone)
                notifiedMilestones.add(milestone)
                saveNotifiedMilestones()
            }
        }
    }
    
    /**
     * ✅ NEW: Send milestone notification
     */
    private fun sendMilestoneNotification(steps: Int, milestone: Int) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val (title, message, emoji) = when (milestone) {
            20 -> Triple(
                "🚶 20% Complete!",
                "You've taken $steps steps. Keep moving!",
                "🚶"
            )
            40 -> Triple(
                "🏃 40% Complete!",
                "You've taken $steps steps. Almost halfway there!",
                "🏃"
            )
            60 -> Triple(
                "💪 60% Complete!",
                "You've taken $steps steps. Over halfway!",
                "💪"
            )
            80 -> Triple(
                "🔥 80% Complete!",
                "You've taken $steps steps. Almost at your goal!",
                "🔥"
            )
            100 -> Triple(
                "🎉 GOAL REACHED!",
                "Congratulations! You've completed $steps steps today!",
                "🎉"
            )
            else -> Triple(
                "Step Progress",
                "You've taken $steps steps",
                "👟"
            )
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
        // Use different notification ID for each milestone so they don't replace each other
        manager.notify(MILESTONE_NOTIFICATION_ID + milestone, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- High Precision Algorithm Refs ---
    private val gravity = FloatArray(3)
    private var smoothedMag = 0.0
    private var isBelowReset = true
    private var lastStepTime = 0L
    private var lastSaveTime = 0L

    // Tuning Constants for Accuracy
    private val ALPHA = 0.92f
    private val SMOOTH_FACTOR = 0.7f
    private val STEP_THRESHOLD = 2.4f
    private val RESET_THRESHOLD = 1.2f
    private val MIN_STEP_TIME = 420L
    private val SAVE_INTERVAL = 5000L // Save every 5 seconds

    private fun registerSensors() {
        if (accelerometerSensor == null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Check for date change
            val today = getTodayDate()
            if (today != currentDate) {
                currentDate = today
                _stepCount.value = 0
                notifiedMilestones.clear()
                clearNotifiedMilestones()
                loadTodayStepsAsync()
            }
            
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
                    
                    // ✅ NEW: Check for milestone notifications
                    checkMilestones(_stepCount.value)
                    
                    // Save periodically (not every step to reduce DB writes)
                    if (now - lastSaveTime > SAVE_INTERVAL) {
                        saveSteps(_stepCount.value)
                        lastSaveTime = now
                    }
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
        
        // Save final step count when service is destroyed
        saveSteps(_stepCount.value)
        
        // Cancel the coroutine scope to prevent memory leak
        serviceScope.cancel()
    }
} 
