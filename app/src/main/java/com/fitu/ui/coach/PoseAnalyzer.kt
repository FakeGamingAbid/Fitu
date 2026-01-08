package com.fitu.ui.coach

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan2

data class FormFeedback(
    val isCorrect: Boolean,
    val message: String,
    val incorrectLandmarks: Set<Int> = emptySet()
)

data class ExerciseResult(
    val exercise: String,
    val repCount: Int,
    val formFeedback: FormFeedback
)

class PoseAnalyzer(
    private val onPoseDetected: (Pose) -> Unit,
    private val onExerciseUpdate: (ExerciseResult) -> Unit,
    private var selectedExercise: String = "Squat",
    private var isWorkoutActive: Boolean = false
) : ImageAnalysis.Analyzer {

    // Use accurate pose detector for better landmark tracking
    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()
    private val detector: PoseDetector = PoseDetection.getClient(options)

    // Rep counting state for each exercise
    private var squatState = ExerciseState.UP
    private var squatCount = 0

    private var pushupState = ExerciseState.UP
    private var pushupCount = 0

    private var situpState = ExerciseState.UP
    private var situpCount = 0

    private var bicepCurlState = ExerciseState.UP
    private var bicepCurlCount = 0

    // Minimum confidence threshold
    private val MIN_CONFIDENCE = 0.7f

    enum class ExerciseState { UP, DOWN }

    fun setExercise(exercise: String) {
        selectedExercise = exercise
    }

    fun setWorkoutActive(active: Boolean) {
        isWorkoutActive = active
    }

    fun resetCounts() {
        squatCount = 0
        pushupCount = 0
        situpCount = 0
        bicepCurlCount = 0
        squatState = ExerciseState.UP
        pushupState = ExerciseState.UP
        situpState = ExerciseState.UP
        bicepCurlState = ExerciseState.UP
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { pose ->
                    onPoseDetected(pose)
                    if (isWorkoutActive) {
                        analyzePose(pose)
                    }
                }
                .addOnFailureListener {
                    // Handle failure silently
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun analyzePose(pose: Pose) {
        when (selectedExercise) {
            "Squat" -> analyzeSquat(pose)
            "Push-up" -> analyzePushup(pose)
            "Sit-up" -> analyzeSitup(pose)
            "Bicep Curl" -> analyzeBicepCurl(pose)
        }
    }

    private fun analyzeSquat(pose: Pose) {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (!checkConfidence(listOf(leftHip, leftKnee, leftAnkle, rightHip, rightKnee, rightAnkle))) {
            return
        }

        val leftKneeAngle = calculateAngle(leftHip!!, leftKnee!!, leftAnkle!!)
        val rightKneeAngle = calculateAngle(rightHip!!, rightKnee!!, rightAnkle!!)
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2

        var formFeedback = FormFeedback(true, "Great form! Keep going!")
        val incorrectLandmarks = mutableSetOf<Int>()

        // Check form
        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            // Check if back is straight (shoulders aligned with hips horizontally)
            val shoulderMidX = (leftShoulder.position.x + rightShoulder.position.x) / 2
            val hipMidX = (leftHip.position.x + rightHip.position.x) / 2
            if (abs(shoulderMidX - hipMidX) > 50) {
                formFeedback = FormFeedback(false, "Keep your back straight!", 
                    setOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER))
                incorrectLandmarks.addAll(setOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER))
            }
        }

        // Check if knees go past toes (bad form)
        if (leftKnee.position.x > leftAnkle.position.x + 30 || 
            rightKnee.position.x > rightAnkle.position.x + 30) {
            formFeedback = FormFeedback(false, "Don't let knees go past toes!", 
                setOf(PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE))
        }

        // Count reps
        if (avgKneeAngle < 100) { // Deep squat
            if (squatState == ExerciseState.UP) {
                squatState = ExerciseState.DOWN
            }
        } else if (avgKneeAngle > 160) { // Standing up
            if (squatState == ExerciseState.DOWN) {
                squatState = ExerciseState.UP
                squatCount++
            }
        }

        onExerciseUpdate(ExerciseResult("Squat", squatCount, formFeedback))
    }

    private fun analyzePushup(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (!checkConfidence(listOf(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist))) {
            return
        }

        val leftElbowAngle = calculateAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
        val rightElbowAngle = calculateAngle(rightShoulder!!, rightElbow!!, rightWrist!!)
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2

        var formFeedback = FormFeedback(true, "Great push-up form!")

        // Check body alignment
        if (leftHip != null && rightHip != null) {
            val hipMidY = (leftHip.position.y + rightHip.position.y) / 2
            val shoulderMidY = (leftShoulder.position.y + rightShoulder.position.y) / 2
            if (hipMidY < shoulderMidY - 50) {
                formFeedback = FormFeedback(false, "Keep your hips lower!", 
                    setOf(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP))
            } else if (hipMidY > shoulderMidY + 50) {
                formFeedback = FormFeedback(false, "Don't let your hips sag!", 
                    setOf(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP))
            }
        }

        // Count reps
        if (avgElbowAngle < 90) { // Down position
            if (pushupState == ExerciseState.UP) {
                pushupState = ExerciseState.DOWN
            }
        } else if (avgElbowAngle > 160) { // Up position
            if (pushupState == ExerciseState.DOWN) {
                pushupState = ExerciseState.UP
                pushupCount++
            }
        }

        onExerciseUpdate(ExerciseResult("Push-up", pushupCount, formFeedback))
    }

    private fun analyzeSitup(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

        if (!checkConfidence(listOf(leftShoulder, leftHip, leftKnee, rightShoulder, rightHip, rightKnee))) {
            return
        }

        val leftTorsoAngle = calculateAngle(leftShoulder!!, leftHip!!, leftKnee!!)
        val rightTorsoAngle = calculateAngle(rightShoulder!!, rightHip!!, rightKnee!!)
        val avgTorsoAngle = (leftTorsoAngle + rightTorsoAngle) / 2

        var formFeedback = FormFeedback(true, "Good sit-up form!")

        // Count reps
        if (avgTorsoAngle < 70) { // Up position
            if (situpState == ExerciseState.DOWN) {
                situpState = ExerciseState.UP
                situpCount++
            }
        } else if (avgTorsoAngle > 140) { // Down position
            if (situpState == ExerciseState.UP) {
                situpState = ExerciseState.DOWN
            }
        }

        onExerciseUpdate(ExerciseResult("Sit-up", situpCount, formFeedback))
    }

    private fun analyzeBicepCurl(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (!checkConfidence(listOf(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist))) {
            return
        }

        val leftElbowAngle = calculateAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
        val rightElbowAngle = calculateAngle(rightShoulder!!, rightElbow!!, rightWrist!!)
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2

        var formFeedback = FormFeedback(true, "Good curl! Control the movement.")

        // Check if elbows are stable (not swinging)
        val leftElbowY = leftElbow.position.y
        val rightElbowY = rightElbow.position.y
        val leftShoulderY = leftShoulder.position.y
        val rightShoulderY = rightShoulder.position.y

        if (abs(leftElbowY - leftShoulderY) > 100 || abs(rightElbowY - rightShoulderY) > 100) {
            formFeedback = FormFeedback(false, "Keep elbows close to body!", 
                setOf(PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW))
        }

        // Count reps
        if (avgElbowAngle < 50) { // Curled up
            if (bicepCurlState == ExerciseState.DOWN) {
                bicepCurlState = ExerciseState.UP
                bicepCurlCount++
            }
        } else if (avgElbowAngle > 150) { // Extended down
            if (bicepCurlState == ExerciseState.UP) {
                bicepCurlState = ExerciseState.DOWN
            }
        }

        onExerciseUpdate(ExerciseResult("Bicep Curl", bicepCurlCount, formFeedback))
    }

    private fun checkConfidence(landmarks: List<PoseLandmark?>): Boolean {
        return landmarks.all { it != null && it.inFrameLikelihood >= MIN_CONFIDENCE }
    }

    private fun calculateAngle(first: PoseLandmark, middle: PoseLandmark, last: PoseLandmark): Double {
        var angle = Math.toDegrees(
            atan2(
                (last.position.y - middle.position.y).toDouble(),
                (last.position.x - middle.position.x).toDouble()
            ) - atan2(
                (first.position.y - middle.position.y).toDouble(),
                (first.position.x - middle.position.x).toDouble()
            )
        )
        angle = abs(angle)
        if (angle > 180) {
            angle = 360.0 - angle
        }
        return angle
    }
}
