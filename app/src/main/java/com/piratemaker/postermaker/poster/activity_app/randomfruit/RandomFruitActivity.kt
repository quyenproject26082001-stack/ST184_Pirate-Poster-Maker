package com.piratemaker.postermaker.poster.activity_app.randomfruit

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.FaceDetector
import android.os.Build
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.handleBackLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.startIntentRightToLeft
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.databinding.ActivityRandomFruitBinding
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.drawable.Drawable

class RandomFruitActivity : BaseActivity<ActivityRandomFruitBinding>() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var fruitAnimator: ValueAnimator? = null
    private lateinit var analysisExecutor: ExecutorService
    private var isRunning = false
    private var isAnalyzing = false
    private var headAnchorX = 0f
    private var headAnchorY = 0f
    private var usingFrontCamera = true
    private var selectedFruitFolder: String? = null
    private var selectedFruitSource: Any? = null
    private var canShowInformation = false
    private val fruitAssets by lazy { loadFruitAssets() }
    private var lastRandomTick = -1

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startFruitFlow()
            } else {
                showToast(R.string.camera_permission_is_required)
            }
        }

    override fun setViewBinding(): ActivityRandomFruitBinding {
        return ActivityRandomFruitBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        analysisExecutor = Executors.newSingleThreadExecutor()

        binding.apply {
            imgCamera.gone()
            imgPlayFruit.gone()
            imgCapturedFruit.gone()
            btnPlay.text = getString(R.string.play)
            btnPlay.visible()
            flashOverlay.gone()
        }
    }

    override fun viewListener() {
        binding.btnPlay.setOnSingleClick(500) {
            if (canShowInformation && selectedFruitFolder != null) {
                showInterAll {  startIntentRightToLeft(
                    FruitInformationActivity::class.java,
                    FruitInformationActivity.EXTRA_FRUIT_FOLDER,
                    selectedFruitFolder.orEmpty()
                ) }
            } else if (!isRunning) {
                ensureCameraPermission()
            }
        }

        binding.actionBar.btnActionBarLeft.setOnSingleClick {
            handleBackLeftToRight()
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
//            tvCenter.text = getString(R.string.random_fruit)
//            tvCenter.visible()
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRight.gone()
        }
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startFruitFlow()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startFruitFlow() {
        isRunning = true
        canShowInformation = false
        selectedFruitFolder = null
        binding.btnPlay.gone()
        binding.btnPlay.text = getString(R.string.play)
        binding.imgCapturedFruit.gone()
        binding.imgCamera.visible()
        startCamera()
        binding.frameCamera.post {
            headAnchorX = binding.frameCamera.width / 2f
            headAnchorY = binding.frameCamera.height * 0.32f
            randomizeFruitImage()
            startFruitAnimation()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.imgCamera.surfaceProvider) }

            val cameraSelector = try {
                if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    usingFrontCamera = true
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    usingFrontCamera = false
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            } catch (_: Exception) {
                usingFrontCamera = true
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        detectHead(imageProxy)
                    }
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exception: Exception) {
                isRunning = false
                binding.btnPlay.visible()
                showToast("Failed to start camera: ${exception.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadFruitAssets(): List<Pair<String, String>> {
        return assets.list("fruit").orEmpty().mapNotNull { folder ->
            assets.list("fruit/$folder")
                .orEmpty()
                .firstOrNull { file ->
                    file.equals("img.webp", true) ||
                            file.endsWith(".png", true) ||
                            file.endsWith(".jpg", true) ||
                            file.endsWith(".jpeg", true) ||
                            file.endsWith(".webp", true)
                }
                ?.let { file -> folder to "file:///android_asset/fruit/$folder/$file" }
        }
    }

    private fun randomizeFruitImage() {
        val randomFruit = fruitAssets.randomOrNull()
        selectedFruitFolder = randomFruit?.first
        val source = randomFruit?.second ?: R.drawable.img_fruit
        selectedFruitSource = source
        Glide.with(this)
            .load(source)
            .into(binding.imgPlayFruit)
    }

    private fun startFruitAnimation() {
        fruitAnimator?.cancel()

        val frame = binding.frameCamera
        val fruit = binding.imgPlayFruit
        val fruitSize = (frame.width * 0.54f).toInt().coerceAtLeast(dp(72))

        fruit.layoutParams = fruit.layoutParams.apply {
            width = fruitSize
            height = fruitSize
        }
        fruit.visible()
        lastRandomTick = -1

        val radiusX = frame.width * Random.nextDouble(0.18, 0.29).toFloat()
        val radiusY = frame.height * Random.nextDouble(0.10, 0.17).toFloat()
        val wobble = frame.height * 0.035f
        val startAngle = Random.nextDouble(0.0, PI * 2).toFloat()

        fruitAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FLOW_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val randomTick = (progress * FLOW_DURATION_MS / RANDOM_INTERVAL_MS).toInt()
                if (randomTick != lastRandomTick) {
                    lastRandomTick = randomTick
                    randomizeFruitImage()
                }

                val angle = startAngle + progress * PI.toFloat() * 6f
                val x = headAnchorX + cos(angle) * radiusX
                val y = headAnchorY + sin(angle) * radiusY + sin(angle * 2.3f) * wobble

                fruit.x = x - fruit.width / 2f
                fruit.y = y - fruit.height / 2f
                fruit.rotation = progress * 720f
                val scale = 0.9f + sin(angle * 1.7f) * 0.08f
                fruit.scaleX = scale
                fruit.scaleY = scale
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishFruitFlow()
                }

                override fun onAnimationCancel(animation: Animator) {
                    finishFruitFlow()
                }
            })
            start()
        }
    }

    private fun detectHead(imageProxy: ImageProxy) {
        if (isAnalyzing || !isRunning) {
            imageProxy.close()
            return
        }

        isAnalyzing = true
        try {
            val bitmap = imageProxyToBitmap(imageProxy) ?: return
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
            val faceBitmap = rotatedBitmap.copy(Bitmap.Config.RGB_565, false)
            val faces = arrayOfNulls<FaceDetector.Face>(1)
            val count = FaceDetector(faceBitmap.width, faceBitmap.height, faces.size).findFaces(faceBitmap, faces)

            if (count > 0) {
                faces.firstOrNull()?.let { face ->
                    updateHeadAnchor(face, faceBitmap.width, faceBitmap.height)
                }
            }
        } catch (_: Exception) {
        } finally {
            isAnalyzing = false
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val nv21 = yuv420ToNv21(imageProxy)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, output)
        val bytes = output.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun yuv420ToNv21(imageProxy: ImageProxy): ByteArray {
        val width = imageProxy.width
        val height = imageProxy.height
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]
        val output = ByteArray(width * height * 3 / 2)

        val yBuffer = yPlane.buffer
        var outputOffset = 0
        for (row in 0 until height) {
            val rowOffset = row * yPlane.rowStride
            for (col in 0 until width) {
                output[outputOffset++] = yBuffer.get(rowOffset + col * yPlane.pixelStride)
            }
        }

        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                output[outputOffset++] = vBuffer.get(vIndex)
                output[outputOffset++] = uBuffer.get(uIndex)
            }
        }

        return output
    }

    private fun updateHeadAnchor(face: FaceDetector.Face, imageWidth: Int, imageHeight: Int) {
        if (imageWidth <= 0 || imageHeight <= 0) return

        val frame = binding.frameCamera
        if (frame.width <= 0 || frame.height <= 0) return

        val faceCenter = PointF()
        face.getMidPoint(faceCenter)
        val eyeDistance = face.eyesDistance()

        val scale = max(frame.width / imageWidth.toFloat(), frame.height / imageHeight.toFloat())
        val offsetX = (frame.width - imageWidth * scale) / 2f
        val offsetY = (frame.height - imageHeight * scale) / 2f

        var detectedX = faceCenter.x * scale + offsetX
        if (usingFrontCamera) {
            detectedX = frame.width - detectedX
        }

        val detectedY = (faceCenter.y - eyeDistance * 0.9f) * scale + offsetY

        runOnUiThread {
            headAnchorX = headAnchorX * 0.72f + detectedX * 0.28f
            headAnchorY = headAnchorY * 0.72f + detectedY * 0.28f
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun finishFruitFlow() {
        if (!isRunning) return
        isRunning = false
        fruitAnimator = null
        captureRandomResultAfterFruitReady()
    }

    private fun captureRandomResultAfterFruitReady() {
        val source = selectedFruitSource ?: R.drawable.img_fruit
        Glide.with(this)
            .asBitmap()
            .load(source)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.imgPlayFruit.setImageBitmap(resource)
                    binding.imgPlayFruit.post {
                        captureRandomResult()
                        finishCaptureState()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    binding.imgPlayFruit.post {
                        captureRandomResult()
                        finishCaptureState()
                    }
                }
            })
    }

    private fun finishCaptureState() {
        canShowInformation = selectedFruitFolder != null
        binding.btnPlay.text = if (canShowInformation) getString(R.string.information) else getString(R.string.play)
        binding.btnPlay.visible()
        cameraProvider?.unbindAll()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureRandomResult() {
        val preview = binding.imgCamera
        preview.bitmap?.let { previewBitmap ->
            val output = previewBitmap.copy(Bitmap.Config.ARGB_8888, true)
            drawFruitOverlay(output)
            showCapturedResult(output)
            return
        }

        val output = Bitmap.createBitmap(
            preview.width.coerceAtLeast(1),
            preview.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )

        if (preview.width <= 0 || preview.height <= 0) {
            drawFruitOverlay(output)
            showCapturedResult(output)
            return
        }

        val location = IntArray(2)
        preview.getLocationInWindow(location)
        val sourceRect = Rect(location[0], location[1], location[0] + preview.width, location[1] + preview.height)

        PixelCopy.request(window, sourceRect, output, { result ->
            if (result == PixelCopy.SUCCESS) {
                drawFruitOverlay(output)
                showCapturedResult(output)
            } else {
                drawFruitOverlay(output)
                showCapturedResult(output)
            }
        }, binding.frameCamera.handler)
    }

    private fun drawFruitOverlay(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val fruit = binding.imgPlayFruit
        val scaleX = bitmap.width / binding.frameCamera.width.toFloat()
        val scaleY = bitmap.height / binding.frameCamera.height.toFloat()

        canvas.save()
        canvas.scale(scaleX, scaleY)
        canvas.translate(fruit.x + fruit.width / 2f, fruit.y + fruit.height / 2f)
        canvas.rotate(fruit.rotation)
        canvas.scale(fruit.scaleX, fruit.scaleY)
        canvas.translate(-fruit.width / 2f, -fruit.height / 2f)
        fruit.draw(canvas)
        canvas.restore()
    }

    private fun showCapturedResult(bitmap: Bitmap) {
        binding.imgCapturedFruit.setImageBitmap(bitmap)
        binding.imgCapturedFruit.visible()
        binding.imgCamera.gone()
        binding.imgPlayFruit.gone()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        fruitAnimator?.cancel()
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val FLOW_DURATION_MS = 10_000L
        private const val RANDOM_INTERVAL_MS = 200L
    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }

    override fun initAds() {
        super.initAds()
        initNativeCollab()
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollap(this,
            getString(R.string.native_collap_fruitFilter),
            binding.nativeClBounty)
    }
}
