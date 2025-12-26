package com.piratemaker.postermaker.poster.activity_app.editsticker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.piratemaker.postermaker.poster.R
import kotlin.math.atan2
import kotlin.math.hypot

@SuppressLint("ViewConstructor")
class StickerView(
    context: Context,
    private val bitmap: Bitmap
) : FrameLayout(context) {

    private val imageView: ImageView
    private val icons = ArrayList<BitmapStickerIcon>()
    private var onDeleteListener: (() -> Unit)? = null

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private var isSelected = false
    private var currentIcon: BitmapStickerIcon? = null

    // Touch handling
    private var lastX = 0f
    private var lastY = 0f
    private var startDistance = 0f
    private var startRotation = 0f
    private var downX = 0f
    private var downY = 0f

    init {
        // Create and add ImageView
        imageView = ImageView(context).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.CENTER
        })

        setupIcons()
        setWillNotDraw(false)
    }

    private fun setupIcons() {
        // Delete icon (top-right)
        val deleteIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_delete_sticker)!!,
            BitmapStickerIcon.IconPosition.TOP_RIGHT
        ).apply {
            iconEvent = DeleteIconEvent()
        }

        // Flip icon (top-left)


        val flipIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_flip_sticker)!!,
            BitmapStickerIcon.IconPosition.TOP_LEFT
        ).apply {
            iconEvent = FlipIconEvent()
        }

        // Zoom/Rotate icon (bottom-right)
        val zoomIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_rotate_scale)!!,
            BitmapStickerIcon.IconPosition.BOTTOM_RIGHT
        ).apply {
            iconEvent = ZoomIconEvent()
        }

        icons.clear()
        icons.add(deleteIcon)
        icons.add(flipIcon)
        icons.add(zoomIcon)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isSelected) {
            // Draw border
            val left = paddingLeft.toFloat()
            val top = paddingTop.toFloat()
            val right = width - paddingRight.toFloat()
            val bottom = height - paddingBottom.toFloat()

            canvas.drawLine(left, top, right, top, borderPaint)
            canvas.drawLine(right, top, right, bottom, borderPaint)
            canvas.drawLine(right, bottom, left, bottom, borderPaint)
            canvas.drawLine(left, bottom, left, top, borderPaint)

            // Draw icons
            updateIconPositions()
            for (icon in icons) {
                icon.draw(canvas, borderPaint)
            }
        }
    }

    private fun updateIconPositions() {
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = width - paddingRight.toFloat()
        val bottom = height - paddingBottom.toFloat()

        for (icon in icons) {
            when (icon.position) {
                BitmapStickerIcon.IconPosition.TOP_LEFT ->
                    icon.updatePosition(left, top, rotation)
                BitmapStickerIcon.IconPosition.TOP_RIGHT ->
                    icon.updatePosition(right, top, rotation)
                BitmapStickerIcon.IconPosition.BOTTOM_LEFT ->
                    icon.updatePosition(left, bottom, rotation)
                BitmapStickerIcon.IconPosition.BOTTOM_RIGHT ->
                    icon.updatePosition(right, bottom, rotation)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = event.rawX
                lastY = event.rawY

                // Check if icon was touched
                currentIcon = findTouchedIcon(event.x, event.y)
                if (currentIcon != null) {
                    currentIcon?.iconEvent?.onActionDown(this, event)
                    return true
                }

                // Select this sticker
                setStickerSelected(true)
                bringToFront()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (currentIcon != null) {
                    currentIcon?.iconEvent?.onActionMove(this, event)
                } else {
                    // Move the sticker
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY
                    x += deltaX
                    y += deltaY
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (currentIcon != null) {
                    currentIcon?.iconEvent?.onActionUp(this, event)
                    currentIcon = null
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedIcon(touchX: Float, touchY: Float): BitmapStickerIcon? {
        for (icon in icons) {
            val dx = icon.x - touchX
            val dy = icon.y - touchY
            val distance = hypot(dx, dy)
            if (distance <= icon.radius * 2) {
                return icon
            }
        }
        return null
    }

    fun setStickerSelected(selected: Boolean) {
        isSelected = selected
        invalidate()
    }

    fun setOnDeleteListener(listener: () -> Unit) {
        onDeleteListener = listener
    }

    fun removeSticker() {
        onDeleteListener?.invoke()
    }

    fun flipHorizontal() {
        scaleX *= -1
        invalidate()
    }

    // Zoom and rotate methods
    private var initialRotation = 0f
    private var initialScale = 1f
    private var centerX = 0f
    private var centerY = 0f

    fun onZoomAndRotateStart(event: MotionEvent) {
        centerX = x + width / 2f
        centerY = y + height / 2f
        startDistance = calculateDistance(centerX, centerY, event.rawX, event.rawY)
        startRotation = calculateRotation(centerX, centerY, event.rawX, event.rawY)
        initialRotation = rotation
        initialScale = scaleX
    }

    fun onZoomAndRotate(event: MotionEvent) {
        val newDistance = calculateDistance(centerX, centerY, event.rawX, event.rawY)
        val newRotation = calculateRotation(centerX, centerY, event.rawX, event.rawY)

        // Scale
        if (startDistance > 0) {
            val scale = newDistance / startDistance
            val newScale = initialScale * scale
            // Limit scale between 0.3 and 3.0
            if (newScale in 0.3f..3.0f) {
                scaleX = newScale
                scaleY = newScale
            }
        }

        // Rotate
        rotation = initialRotation + (newRotation - startRotation)
        invalidate()
    }

    fun onZoomAndRotateEnd() {
        // Nothing needed here for now
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return hypot(dx, dy)
    }

    private fun calculateRotation(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
}
