package com.androchef.cameraxfacedetection.face_detection

import kotlin.math.sqrt
import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.util.Log
import android.widget.TextView
import com.androchef.cameraxfacedetection.camerax.BaseImageAnalyzer
import com.androchef.cameraxfacedetection.camerax.GraphicOverlay
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.math.pow

class FaceContourDetectionProcessor(
    private val view: GraphicOverlay,
    private val orientationStatusTextView: TextView,
    private val distanceStatusTextView: TextView
) : BaseImageAnalyzer<List<Face>>() {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    override val graphicOverlay: GraphicOverlay
        get() = view


    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
            .continueWith { task ->
                if (task.isSuccessful) {
                    val detectedFaces = task.result
                    if (detectedFaces?.isNotEmpty() == true) {
                        val largestFace =
                            detectedFaces.maxBy { it.boundingBox.width() * it.boundingBox.height() }
                        if (largestFace != null) {
                            listOf(largestFace)
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    // Handle the case where face detection failed
                    emptyList()
                }
            }
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
        }
    }

    override fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect
    ) {
        /* Handler().postDelayed({  */
        graphicOverlay.clear()
        val faceStatus = if (results.isNotEmpty()) {
            val face = results[0]
            val boundingBox = face.boundingBox
            val boxSize = (boundingBox.width() + boundingBox.height()) / 2 // Average of width and height

            val nearThreshold = 100.0 // Adjust the threshold for "near" face size
            val farThreshold = 200.0 // Adjust the threshold for "far" face size

            val rotationY = face.headEulerAngleY
            val rotationThreshold = 15.0 // Adjust the threshold for rotation detection

            val verticalRotationThreshold = 10.0 // Adjust the threshold for vertical rotation detection

            Log.e("boxSize", "=$boxSize")
            Log.e("farThreshold", "=$farThreshold")
            Log.e("nearThreshold", "=$nearThreshold")
            Log.e("rotationY", "=$rotationY")

            val orientationStatus: String = when {
                rotationY < -rotationThreshold -> "Move left"
                rotationY > rotationThreshold -> "Move right"
                else -> "Stay straight"
            }

            val verticalOrientationStatus: String = when {
                face.headEulerAngleX < -verticalRotationThreshold -> "Move up"
                face.headEulerAngleX > verticalRotationThreshold -> "Move down"
                else -> "Stay straight"
            }
            val distanceStatus: String = when {
                boxSize < nearThreshold -> "Come near"
                boxSize > farThreshold -> "Go far"
                boxSize > farThreshold && boxSize<250-> "Stay still"
                else -> "Stay still"
            }

            // Determine the final status based on the order of conditions
            val finalStatus = when {
                orientationStatus == "Move left" -> "$orientationStatus"
                orientationStatus == "Move right" -> "$orientationStatus"
               // distanceStatus == "Go away" -> "Face Status: $distanceStatus"
               // distanceStatus == "Come Near" -> "Face Status: $distanceStatus"
                verticalOrientationStatus == "Move up" -> "$verticalOrientationStatus"
                verticalOrientationStatus == "Move down" -> "$verticalOrientationStatus"
                else -> "Stay straight"
            }
            Handler().postDelayed({
                distanceStatusTextView.text = distanceStatus
                orientationStatusTextView.text = finalStatus
            }, 200) // De

        } else {
            Handler().postDelayed({
                orientationStatusTextView.text = "No Face Detected"
                distanceStatusTextView.text = "No Face Detected"
            }, 200)

        }

        results.forEach {
            val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
            graphicOverlay.add(faceGraphic)
        }
        graphicOverlay.postInvalidate()  /*}, 1000)*/

    }


    /*override fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect
    ) {
        graphicOverlay.clear()
        val faceStatus = if (results.isNotEmpty()) {
            val face = results[0]
            val boundingBox = face.boundingBox
            val boxSize = (boundingBox.width() + boundingBox.height()) / 2 // Average of width and height

            val nearThreshold = 100.0 // Adjust the threshold for "near" face size
            val farThreshold = 200.0 // Adjust the threshold for "far" face size

            val rotationY = face.headEulerAngleY
            val rotationThreshold = 15.0 // Adjust the threshold for rotation detection

            Log.e("boxSize", "=$boxSize");
            Log.e("farThreshold", "=$farThreshold");
            Log.e("nearThreshold", "=$nearThreshold");
            Log.e("rotationY", "=$rotationY");

            val distanceStatus: String = when {
                boxSize < nearThreshold -> "Come Near"
                boxSize > farThreshold -> "Go away"
                else -> "Stay "
            }
            Log.e("rotationYcc", "=$rotationY");
            Log.e("rotationThreshold", "=$rotationThreshold");

            val orientationStatus: String = when {
                rotationY < -rotationThreshold -> "Left"
                rotationY > rotationThreshold -> "Right"
                else -> "Straight"
            }


            val verticalRotationThreshold = 10.0 // Adjust the threshold for vertical rotation detection

            val verticalOrientationStatus: String = when {
                face.headEulerAngleX < -verticalRotationThreshold -> "Up"
                face.headEulerAngleX > verticalRotationThreshold -> "Down"
                else -> "Front"
            }

            val finalStatus = "Face Status: $distanceStatus, $orientationStatus, $verticalOrientationStatus"
            statusTextView.text = finalStatus
        } else {
            statusTextView.text = "Face Status: No Face Detected"
        }

        results.forEach {
            val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
            graphicOverlay.add(faceGraphic)
        }
        graphicOverlay.postInvalidate()
    }*/

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }


    /* override fun onSuccess(
         results: List<Face>,
         graphicOverlay: GraphicOverlay,
         rect: Rect
     ) {
         graphicOverlay.clear()
         results.forEach {
             val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
             graphicOverlay.add(faceGraphic)
         }
         graphicOverlay.postInvalidate()
     }*/

    /* override fun onSuccess(
         results: List<Face>,
         graphicOverlay: GraphicOverlay,
         rect: Rect
     ) {
         graphicOverlay.clear()
         val faceStatus = if (results.isNotEmpty()) {
             val face = results[0]
             val boundingBox = face.boundingBox
             val boxSize = (boundingBox.width() + boundingBox.height()) / 2 // Average of width and height

             val nearThreshold = 100.0 // Adjust the threshold for "near" face size
             val farThreshold = 200.0 // Adjust the threshold for "far" face size
             Log.e("boxSize","="+boxSize);
             Log.e("farThreshold","="+farThreshold);
             Log.e("nearThreshold","="+nearThreshold);


             if (boxSize < nearThreshold) {
                 statusTextView.text = "Face Status: Far Away"
             } else if (boxSize > farThreshold) {
                 statusTextView.text = "Face Status:Near "
             } else {
                 statusTextView.text = "Face Status: Intermediate"
             }
         } else {
             statusTextView.text = "Face Status: No Face Detected"
         }

         results.forEach {
             val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
             graphicOverlay.add(faceGraphic)
         }
         graphicOverlay.postInvalidate()
     }

  */
    /*

        override fun onSuccess(
            results: List<Face>,
            graphicOverlay: GraphicOverlay,
            rect: Rect
        ) {
            graphicOverlay.clear()

            if (results.isNotEmpty()) {
                val face = results[0]
                val landmarks = face.allLandmarks
                Log.e("landmarks","="+landmarks);

                if (landmarks.isNotEmpty()) {
                    // Check if landmarks are detected
                    val leftEar = landmarks.find { it.landmarkType == FaceLandmark.LEFT_EAR }?.position
                    val rightEar = landmarks.find { it.landmarkType == FaceLandmark.RIGHT_EAR }?.position
                    val noseBase = landmarks.find { it.landmarkType == FaceLandmark.NOSE_BASE }?.position
                    Log.e("noseBase","="+noseBase);

                    if (leftEar != null && rightEar != null && noseBase != null) {
                        // Calculate distances or angles based on landmarks
                        val earDistance = calculateDistance(leftEar, rightEar)
                        val noseToEarDistance = calculateDistance(noseBase, rightEar)
                        val earToNoseRatio = earDistance / noseToEarDistance
                        Log.e("earDistance","="+earDistance);
                        Log.e("earDistance","="+earDistance);
                        Log.e("noseToEarDistance","="+noseToEarDistance);

                        if (earToNoseRatio < 0.7) {
                            statusTextView.text = "Face Status: Near"
                        } else if (earToNoseRatio > 1.3) {
                            statusTextView.text = "Face Status: Far"
                        } else {
                            statusTextView.text = "Face Status: Intermediate"
                        }
                    } else {
                        statusTextView.text = "Face Status: Landmarks not detected"
                    }
                } else {
                    statusTextView.text = "Face Status: Landmarks not detected"
                }
            } else {
                statusTextView.text = "Face Status: No Face Detected"
            }

            results.forEach {
                val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
                graphicOverlay.add(faceGraphic)
            }
            graphicOverlay.postInvalidate()
        }
    */
    private fun calculateDistance(point1: PointF, point2: PointF): Float {
        return sqrt((point1.x - point2.x).pow(2) + (point1.y - point2.y).pow(2))
    }
    /*

            override fun detectInImage(image: InputImage): Task<List<Face>> {
                return detector.process(image)
            }
        */
}