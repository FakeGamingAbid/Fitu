package com.fitu.aicoach

/**
 * State machine for counting exercise repetitions.
 * 
 * Prevents flicker counting by requiring a FULL CYCLE before incrementing reps:
 * - Starting state: UNKNOWN (no position detected yet)
 * - After detecting UP position: state = UP
 * - After detecting DOWN position (from UP): state = DOWN
 * - After detecting UP position again (from DOWN): state = UP, rep count += 1
 * 
 * The state machine only counts a rep when completing: UP → DOWN → UP
 * 
 * Hysteresis is implemented by using separate thresholds:
 * - downThreshold: angle must go BELOW this to transition to DOWN
 * - upThreshold: angle must go ABOVE this to transition to UP
 * 
 * This prevents rapid state changes when angle hovers near a threshold.
 */
class RepCounter(
    private val downThreshold: Float,
    private val upThreshold: Float,
    private val exerciseType: ExerciseType
) {
    /**
     * Current state of the exercise motion
     */
    enum class State {
        UNKNOWN,  // Initial state, position not yet determined
        UP,       // Extended position (start/end of rep)
        DOWN      // Contracted position (bottom of rep)
    }

    private var state: State = State.UNKNOWN
    private var _repCount: Int = 0
    
    /**
     * Current rep count
     */
    val repCount: Int get() = _repCount

    /**
     * Current state for UI display
     */
    val currentState: State get() = state

    /**
     * Update the counter with a new angle measurement.
     * 
     * @param angle Current angle in degrees (0-180)
     * @return True if a new rep was counted this frame
     */
    fun update(angle: Float): Boolean {
        if (angle < 0) {
            // Invalid angle, don't update state
            return false
        }

        var repCounted = false

        // Handle different exercises with inverted logic
        when (exerciseType) {
            ExerciseType.DUMBBELL_CURL, ExerciseType.CRUNCH -> {
                // For curls/crunches: DOWN means arm/torso extended (angle > downThreshold)
                //                     UP means arm/torso contracted (angle < upThreshold)
                repCounted = updateInverted(angle)
            }
            else -> {
                // For push-ups/squats: DOWN means bent (angle < downThreshold)
                //                       UP means extended (angle > upThreshold)
                repCounted = updateNormal(angle)
            }
        }

        return repCounted
    }

    /**
     * Normal update logic for push-ups and squats.
     * DOWN = angle < downThreshold (bent position)
     * UP = angle > upThreshold (extended position)
     */
    private fun updateNormal(angle: Float): Boolean {
        var repCounted = false

        when (state) {
            State.UNKNOWN -> {
                // Determine initial state based on current angle
                if (angle > upThreshold) {
                    state = State.UP
                } else if (angle < downThreshold) {
                    state = State.DOWN
                }
            }
            State.UP -> {
                // Check if user went DOWN
                if (angle < downThreshold) {
                    state = State.DOWN
                }
            }
            State.DOWN -> {
                // Check if user came back UP → count rep!
                if (angle > upThreshold) {
                    state = State.UP
                    _repCount++
                    repCounted = true
                }
            }
        }

        return repCounted
    }

    /**
     * Inverted update logic for curls and crunches.
     * DOWN = angle > downThreshold (extended/relaxed position)
     * UP = angle < upThreshold (contracted position)
     */
    private fun updateInverted(angle: Float): Boolean {
        var repCounted = false

        when (state) {
            State.UNKNOWN -> {
                // Determine initial state based on current angle
                if (angle > downThreshold) {
                    state = State.DOWN  // Extended position = start
                } else if (angle < upThreshold) {
                    state = State.UP    // Contracted position
                }
            }
            State.DOWN -> {
                // Check if user contracted (UP)
                if (angle < upThreshold) {
                    state = State.UP
                }
            }
            State.UP -> {
                // Check if user extended back (DOWN) → count rep!
                if (angle > downThreshold) {
                    state = State.DOWN
                    _repCount++
                    repCounted = true
                }
            }
        }

        return repCounted
    }

    /**
     * Reset the counter to initial state
     */
    fun reset() {
        state = State.UNKNOWN
        _repCount = 0
    }

    /**
     * Get state as a display string
     */
    fun getStateDisplay(): String {
        return when (state) {
            State.UNKNOWN -> "Ready"
            State.UP -> "Up"
            State.DOWN -> "Down"
        }
    }

    companion object {
        /**
         * Create a RepCounter configured for a specific exercise
         */
        fun forExercise(config: ExerciseConfig): RepCounter {
            return RepCounter(
                downThreshold = config.downThreshold,
                upThreshold = config.upThreshold,
                exerciseType = config.exerciseType
            )
        }
    }
}
