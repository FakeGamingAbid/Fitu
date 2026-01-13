 package com.fitu.di

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitu.data.local.FituDatabase
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.dao.WorkoutPlanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideSensorManager(context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideSecureStorage(context: Context): SecureStorage {
        return SecureStorage(context)
    }

    /**
     * ✅ FIX #1: Proper database migrations instead of destructive migration
     * 
     * Migration history:
     * - Version 1-4: Initial versions (legacy)
     * - Version 5: Current stable version
     * - Version 6+: Future versions with proper migrations
     */
    @Provides
    @Singleton
    fun provideDatabase(application: Application): FituDatabase {
        return Room.databaseBuilder(
            application,
            FituDatabase::class.java,
            "fitu_db"
        )
        // ✅ Add migrations here when you update the database schema
        .addMigrations(
            MIGRATION_5_6
        )
        // ✅ Only use destructive migration as LAST RESORT for old versions
        // This handles upgrades from very old versions (1-4) that we can't migrate
        .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
        .build()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(db: FituDatabase): WorkoutDao {
        return db.workoutDao()
    }

    @Provides
    @Singleton
    fun provideMealDao(db: FituDatabase): MealDao {
        return db.mealDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutPlanDao(db: FituDatabase): WorkoutPlanDao {
        return db.workoutPlanDao()
    }

    @Provides
    @Singleton
    fun provideFoodCacheDao(db: FituDatabase): com.fitu.data.local.dao.FoodCacheDao {
        return db.foodCacheDao()
    }

    @Provides
    @Singleton
    fun provideStepDao(db: FituDatabase): StepDao {
        return db.stepDao()
    }
}

/**
 * ✅ Migration from version 5 to 6 (template for future migrations)
 * 
 * When you need to add a new migration:
 * 1. Increment the version number in FituDatabase.kt
 * 2. Create a new MIGRATION_X_Y object below
 * 3. Add the migration to .addMigrations() in provideDatabase()
 * 
 * Example migrations:
 * - Add new column: database.execSQL("ALTER TABLE table_name ADD COLUMN column_name TYPE DEFAULT value")
 * - Add new table: database.execSQL("CREATE TABLE IF NOT EXISTS ...")
 * - Add index: database.execSQL("CREATE INDEX IF NOT EXISTS ...")
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ✅ This is a placeholder migration (no schema changes from 5 to 6)
        // When you need to make schema changes, add the SQL here
        // Example: database.execSQL("ALTER TABLE daily_steps ADD COLUMN distance_km REAL DEFAULT 0.0")
    }
}

// ✅ Template for future migrations - copy and modify as needed
/*
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add your migration SQL here
        // Example: Add a new column
        // database.execSQL("ALTER TABLE meals ADD COLUMN fiber INTEGER DEFAULT 0")
        
        // Example: Create a new table
        // database.execSQL("""
        //     CREATE TABLE IF NOT EXISTS water_intake (
        //         id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        //         date TEXT NOT NULL,
        //         amount_ml INTEGER NOT NULL,
        //         timestamp INTEGER NOT NULL
        //     )
        // """)
    }
}
*/ 
