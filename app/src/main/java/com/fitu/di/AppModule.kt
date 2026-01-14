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

    @Provides
    @Singleton
    fun provideDatabase(application: Application): FituDatabase {
        return Room.databaseBuilder(
            application,
            FituDatabase::class.java,
            "fitu_db"
        )
        .addMigrations(
            MIGRATION_5_6,
            MIGRATION_6_7  // ✅ Add new migration
        )
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

// Existing migration
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Placeholder migration
    }
}

// ✅ NEW: Migration to add photoUri column to meals table
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE meals ADD COLUMN photoUri TEXT DEFAULT NULL")
    }
}
