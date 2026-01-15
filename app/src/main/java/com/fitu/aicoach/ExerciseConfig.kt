package com.fitu.aicoach

import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Configuration for each exercise type.
 * Defines the landmarks to track, angle thresholds, and tracking mode.
 *
 * @property exerciseType The type of exercise
 * @property landmarks Triple of landmark types: (first, mid, last) for angle calculation
 * @property angleName Human-readable name for the angle being measured
 * @property downThreshold Angle threshold for "down" position (degrees)
 * @property upThreshold Angle threshold for "up" position (degrees)
 * @property useLeftSide Whether to use left side landmarks (true) or right side (false)
 */
data class ExerciseConfig(
    val exerciseType: ExerciseType,
    val landmarks: Triple<Int, Int, Int>,
    val angleName: String,
    val downThreshold: Float,
    val upThreshold: Float,
    val useLeftSide: Boolean = true
) {
    companion object {
        /**
         * Get the configuration for a specific exercise type.
         * 
         * Angle calculation uses three landmarks:
         * - First: The starting point of the angle
         * - Mid: The vertex (joint) where angle is measured
         * - Last: The ending point of the angle
         * 
         * Example: For elbow angle → Shoulder → Elbow → Wrist
         */
        fun forExercise(type: ExerciseType, useLeftSide: Boolean = true): ExerciseConfig {
            return when (type) {
                ExerciseType.PUSH_UP -> {
                    // Elbow angle: Shoulder → Elbow → Wrist
                        // Down: < 100° (arm bent), Up: > 150° (arm extended)
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                PoseLandmark.LEFT_SHOULDER,
                                PoseLandmark.LEFT_ELBOW,
                                PoseLandmark.LEFT_WRIST
                            )
                        } else {
                            Triple(
                                PoseLandmark.RIGHT_SHOULDER,
                                PoseLandmark.RIGHT_ELBOW,
                                PoseLandmark.RIGHT_WRIST
                            )
                        },
                        angleName = "Elbow",
                        downThreshold = 110f, // Drastically relaxed from 100f
                        upThreshold = 140f,   // Drastically relaxed from 150f
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.SQUAT -> {
                    // Knee angle: Hip → Knee → Ankle
                    // Down: < 100° (deep squat), Up: > 150° (standing)
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                PoseLandmark.LEFT_HIP,
                                PoseLandmark.LEFT_KNEE,
                                PoseLandmark.LEFT_ANKLE
                            )
                        } else {
                            Triple(
                                PoseLandmark.RIGHT_HIP,
                                PoseLandmark.RIGHT_KNEE,
                                PoseLandmark.RIGHT_ANKLE
                            )
                        },
                        angleName = "Knee",
                        downThreshold = 120f, // Drastically relaxed (quarter squat counts)
                        upThreshold = 145f,   // Relaxed from 150f
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.PLANK -> {
                    // Body line angle: Shoulder → Hip → Ankle
                    // Valid plank: 160°-180° (straight body)
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                PoseLandmark.LEFT_SHOULDER,
                                PoseLandmark.LEFT_HIP,
                                PoseLandmark.LEFT_ANKLE
                            )
                        } else {
                            Triple(
                                PoseLandmark.RIGHT_SHOULDER,
                                PoseLandmark.RIGHT_HIP,
                                PoseLandmark.RIGHT_ANKLE
                            )
                        },
                        angleName = "Body Line",
                        downThreshold = 150f, // Relaxed from 160f
                        upThreshold = 180f,
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.DUMBBELL_CURL -> {
                    // Elbow angle: Shoulder → Elbow → Wrist
                    // Down: > 150° (arm extended), Up: < 80° (fully curled)
                    // Note: Thresholds are INVERTED compared to push-up
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                PoseLandmark.LEFT_SHOULDER,
                                PoseLandmark.LEFT_ELBOW,
                                PoseLandmark.LEFT_WRIST
                            )
                        } else {
                            Triple(
                                PoseLandmark.RIGHT_SHOULDER,
                                PoseLandmark.RIGHT_ELBOW,
                                PoseLandmark.RIGHT_WRIST
                            )
                        },
                        angleName = "Elbow",
                        downThreshold = 140f, // Relaxed from 150f
                        upThreshold = 100f,   // Drastically relaxed from 80f
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.CRUNCH -> {
                    // Hip/Torso angle: Shoulder → Hip → Knee
                    // Down: > 150° (lying flat), Up: < 135° (crunched up)
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                PoseLandmark.LEFT_SHOULDER,
                                PoseLandmark.LEFT_HIP,
                                PoseLandmark.LEFT_KNEE
                            )
                        } else {
                            Triple(
                                PoseLandmark.RIGHT_SHOULDER,
                                PoseLandmark.RIGHT_HIP,
                                PoseLandmark.RIGHT_KNEE
                            )
                        },
                        angleName = "Torso",
                        downThreshold = 150f, // Keep strict-ish for lying flat
                        upThreshold = 140f,   // Relaxed from 135f
                        useLeftSide = useLeftSide
                    )
                }
            }
        }
    }
}
