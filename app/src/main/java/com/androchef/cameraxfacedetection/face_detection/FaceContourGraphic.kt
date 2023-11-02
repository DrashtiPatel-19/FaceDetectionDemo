package com.androchef.cameraxfacedetection.face_detection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.androchef.cameraxfacedetection.camerax.GraphicOverlay
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

class FaceContourGraphic(
    overlay: GraphicOverlay,
    private val face: Face,
    private val imageRect: Rect
) : GraphicOverlay.Graphic(overlay) {

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint

    init {
        val selectedColor = Color.WHITE

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        idPaint = Paint()
        idPaint.color = selectedColor

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    override fun draw(canvas: Canvas?) {
        val rect = calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            face.boundingBox
        )
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

        leftEye?.position?.let { leftEyePos ->
            canvas?.drawCircle(leftEyePos.x, leftEyePos.y, DOT_RADIUS, facePositionPaint)
        }

        rightEye?.position?.let { rightEyePos ->
            canvas?.drawCircle(rightEyePos.x, rightEyePos.y, DOT_RADIUS, facePositionPaint)
        }
        canvas?.drawOval(rect, boxPaint)
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 3.0f
        private const val DOT_RADIUS = 1.0f // Adjust the radius as needed
    }
}