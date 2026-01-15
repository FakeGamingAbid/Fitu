package com.fitu.aicoach

/**
 * Enum representing supported exercise types for AI Coach.
 * 
 * @property displayName Human-readable name for the exercise
 * @property isTimeBased True for plank (time tracking), false for rep-based exercises
 * @property emoji Icon emoji for the exercise
 */
enum class ExerciseType(
    val displayName: String,
    val isTimeBased: Boolean,
    val emoji: String
) {
    PUSH_UP(
        displayName = "Push-up",
        isTimeBased = false,
        emoji = "💪"
    ),
    SQUAT(
        displayName = "Squat",
        isTimeBased = false,
        emoji = "🦵"
    ),
    PLANK(
        displayName = "Plank",
        isTimeBased = true,
        emoji = "🧘"
    ),
    DUMBBELL_CURL(
        displayName = "Curl",
        isTimeBased = false,
        emoji = "🏋️"
    ),
    CRUNCH(
        displayName = "Crunch",
        isTimeBased = false,
        emoji = "🔥"
    );

    companion object {
        /**
         * Get all rep-based exercises
         */
        fun getRepBasedExercises(): List<ExerciseType> = entries.filter { !it.isTimeBased }

        /**
         * Get all time-based exercises
         */
        fun getTimeBasedExercises(): List<ExerciseType> = entries.filter { it.isTimeBased }
    }
}
