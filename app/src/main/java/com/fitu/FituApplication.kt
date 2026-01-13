 package com.fitu

import android.app.Application
import com.fitu.data.local.dao.StepDao
import com.fitu.data.service.StepCounterService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FituApplication : Application() {
    
    @Inject
    lateinit var stepDao: StepDao
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // ✅ FIX: Pre-load steps from database IMMEDIATELY when app starts
        // This ensures steps are loaded before any UI is shown
        applicationScope.launch {
            try {
                StepCounterService.preloadSteps(stepDao)
            } catch (e: Exception) {
                // Ignore errors, service will load steps when it starts
            }
        }
    }
} 
