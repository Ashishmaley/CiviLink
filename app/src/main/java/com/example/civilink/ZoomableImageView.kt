package com.example.civilink

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private val matrix = Matrix()
    private var mode = NONE
    private val savedMatrix = Matrix()
    private val start = PointF()
    private val mid = PointF()

    companion object {
        private const val NONE = 0
        private const val ZOOM = 2
    }

    private var newDist = 0f
    private var oldDist = 1f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == ZOOM) {
                    newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                        imageMatrix = matrix
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }
        return true
    }

    private fun spacing(event: MotionEvent): Float {
        if (event.pointerCount >= 2) {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return kotlin.math.sqrt(x * x + y * y)
        }
        return 0f
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = kotlin.math.atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }
}
