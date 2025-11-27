package com.piratemaker.postermaker.poster.core.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer

/**
 * Helper class for removing background from images using ML Kit Selfie Segmentation
 * 
 * Features:
 * - Free and offline
 * - Works best with selfie/person images
 * - Fast processing
 * 
 * Usage:
 * ```
 * val helper = BackgroundRemovalHelper(context)
 * val resultBitmap = helper.removeBackground(originalBitmap)
 * ```
 */
object BackgroundRemovalHelper {

    /**
     * Remove background from image using ML Kit Selfie Segmentation
     * 
     * @param context Android context
     * @param bitmap Original bitmap image
     * @param confidence Confidence threshold (0.0 - 1.0). Default is 0.5
     * @return Bitmap with transparent background or null if failed
     */
    suspend fun removeBackground(
        context: Context,
        bitmap: Bitmap,
        confidence: Float = 0.5f
    ): Bitmap? {
        return try {
            // Configure segmenter options
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()

            val segmenter = Segmentation.getClient(options)

            // Create InputImage from bitmap
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Process image
            val segmentationMask = segmenter.process(inputImage).await()

            // Create result bitmap with transparent background
            val resultBitmap = applyMask(bitmap, segmentationMask, confidence)

            // Release resources
            segmenter.close()

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Apply segmentation mask to bitmap
     * 
     * @param originalBitmap Original image
     * @param mask Segmentation mask from ML Kit
     * @param confidence Confidence threshold
     * @return Bitmap with transparent background
     */
    private fun applyMask(
        originalBitmap: Bitmap,
        mask: SegmentationMask,
        confidence: Float
    ): Bitmap {
        val width = mask.width
        val height = mask.height
        val maskBuffer = mask.buffer

        // Create result bitmap with ARGB_8888 config for transparency
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Scale original bitmap to mask size if needed
        val scaledBitmap = if (originalBitmap.width != width || originalBitmap.height != height) {
            Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        } else {
            originalBitmap
        }

        // Process each pixel
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Get mask confidence value (0.0 - 1.0)
                val maskValue = maskBuffer.float

                // Get original pixel color
                val pixelColor = scaledBitmap.getPixel(x, y)

                // Apply mask based on confidence threshold
                if (maskValue >= confidence) {
                    // Keep pixel (foreground - person)
                    resultBitmap.setPixel(x, y, pixelColor)
                } else {
                    // Make pixel transparent (background)
                    resultBitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }

        // Rewind buffer for potential reuse
        maskBuffer.rewind()

        return resultBitmap
    }

    /**
     * Remove background and replace with solid color
     * 
     * @param context Android context
     * @param bitmap Original bitmap
     * @param backgroundColor Background color to replace with
     * @param confidence Confidence threshold
     * @return Bitmap with colored background
     */
    suspend fun removeBackgroundWithColor(
        context: Context,
        bitmap: Bitmap,
        backgroundColor: Int = Color.WHITE,
        confidence: Float = 0.5f
    ): Bitmap? {
        val transparentBitmap = removeBackground(context, bitmap, confidence) ?: return null
        
        // Create bitmap with colored background
        val resultBitmap = Bitmap.createBitmap(
            transparentBitmap.width,
            transparentBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(transparentBitmap, 0f, 0f, null)
        
        return resultBitmap
    }

    /**
     * Remove background with blur effect on background
     * 
     * @param context Android context
     * @param bitmap Original bitmap
     * @param blurRadius Blur radius (1-25)
     * @param confidence Confidence threshold
     * @return Bitmap with blurred background
     */
    suspend fun removeBackgroundWithBlur(
        context: Context,
        bitmap: Bitmap,
        blurRadius: Int = 15,
        confidence: Float = 0.5f
    ): Bitmap? {
        return try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()

            val segmenter = Segmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val segmentationMask = segmenter.process(inputImage).await()

            // Create blurred version of original
            val blurredBitmap = BitmapHelper.blurBitmap(context, bitmap, blurRadius.toFloat())

            // Combine foreground with blurred background
            val resultBitmap = combineWithBlurredBackground(
                bitmap,
                blurredBitmap,
                segmentationMask,
                confidence
            )

            segmenter.close()
            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Combine foreground with blurred background
     */
    private fun combineWithBlurredBackground(
        originalBitmap: Bitmap,
        blurredBitmap: Bitmap,
        mask: SegmentationMask,
        confidence: Float
    ): Bitmap {
        val width = mask.width
        val height = mask.height
        val maskBuffer = mask.buffer

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val scaledOriginal = if (originalBitmap.width != width || originalBitmap.height != height) {
            Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        } else {
            originalBitmap
        }

        val scaledBlurred = if (blurredBitmap.width != width || blurredBitmap.height != height) {
            Bitmap.createScaledBitmap(blurredBitmap, width, height, true)
        } else {
            blurredBitmap
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskValue = maskBuffer.float
                
                if (maskValue >= confidence) {
                    // Foreground - use original
                    resultBitmap.setPixel(x, y, scaledOriginal.getPixel(x, y))
                } else {
                    // Background - use blurred
                    resultBitmap.setPixel(x, y, scaledBlurred.getPixel(x, y))
                }
            }
        }

        maskBuffer.rewind()
        return resultBitmap
    }

    /**
     * Check if ML Kit Selfie Segmentation is available
     */
    fun isAvailable(): Boolean {
        return try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            segmenter.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

