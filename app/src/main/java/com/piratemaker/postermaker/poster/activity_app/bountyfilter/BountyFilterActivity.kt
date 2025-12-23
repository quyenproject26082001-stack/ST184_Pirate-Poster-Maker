package com.piratemaker.postermaker.poster.activity_app.bountyfilter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.checkPermissions
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.goToSettings
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.SoundHelper
import com.piratemaker.postermaker.poster.databinding.ActivityBountyFilterBinding
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

class BountyFilterActivity : BaseActivity<ActivityBountyFilterBinding>() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val handler = Handler(Looper.getMainLooper())
    private var randomRunnable: Runnable? = null
    private var isCountingDown = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun setViewBinding(): ActivityBountyFilterBinding {
        return ActivityBountyFilterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.actionBar.btnActionBarLeft.visible()

        // Load camera sound
        if (!SoundHelper.isSoundNotNull(R.raw.camera_sound)) {
            SoundHelper.loadSound(this, R.raw.camera_sound)
        }

        // TODO: REMOVE THIS LINE AFTER TESTING - Reset counter for testing
        // sharePreference.setCameraPermission(0)

        // Initial state: show all elements normally - no dark overlay
        binding.apply {
            // Show all elements in their normal positions
            imgPlay.visible()
            imgCamera.gone()
            tvBountyFilter.gone()
            btnPlay.visible()
            flashOverlay.gone()
        }
    }

    override fun viewListener() {
        binding.btnPlay.setOnSingleClick {
            if (!isCountingDown) {
                if (allPermissionsGranted()) {
                    startBountyFilterSequence()
                } else {
                    handleCameraPermissionRequest()
                }
            }
        }

        binding.actionBar.btnActionBarLeft.setOnSingleClick {
            finish()
        }
    }

    private fun handleCameraPermissionRequest() {
        val currentCounter = sharePreference.getCameraPermission()
        android.util.Log.d("BountyFilter", "Camera permission counter: $currentCounter")

        if (checkPermissions(REQUIRED_PERMISSIONS)) {
            // Permission already granted
            startBountyFilterSequence()
        } else if (needGoToSettings()) {
            // User has denied permission more than 2 times, go to settings
            android.util.Log.d("BountyFilter", "Going to settings (counter >= 2)")
            goToSettings()
        } else {
            // Request permission
            android.util.Log.d("BountyFilter", "Requesting permission (counter < 2)")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun needGoToSettings(): Boolean {
        return sharePreference.getCameraPermission() >= 2
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = getString(R.string.bountyFilter)

            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRight.gone()
        }
    }

    private fun startBountyFilterSequence() {
        isCountingDown = true

        // Step 1: Open camera
        startCamera()

        // Step 2: Start countdown after camera is ready (delay 500ms)
        handler.postDelayed({
            startCountdown()
        }, 500)
    }

    private fun startCountdown() {
        val countdownNumbers = listOf("3", "2", "1")
        var currentIndex = 0

        binding.tvBountyFilter.apply {
            animate().cancel()
            clearAnimation()

            // ✅ set trước để khỏi ló 1 frame
            text = countdownNumbers[0]
            textSize = 80f
            setTextColor(Color.WHITE)

            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            visible()
        }

        val countdownRunnable = object : Runnable {
            override fun run() {
                if (currentIndex < countdownNumbers.size) {
                    binding.tvBountyFilter.animate().cancel()

                    binding.tvBountyFilter.text = countdownNumbers[currentIndex]
                    binding.tvBountyFilter.apply {
                        alpha = 0f
                        scaleX = 0.5f
                        scaleY = 0.5f

                        animate()
                            .scaleX(1.2f).scaleY(1.2f).alpha(1f)
                            .setDuration(300)
                            .withEndAction {
                                animate()
                                    .scaleX(0.8f).scaleY(0.8f).alpha(0.3f)
                                    .setDuration(700)
                                    .start()
                            }
                            .start()
                    }

                    currentIndex++
                    handler.postDelayed(this, 1000)
                } else {
                    onCountdownFinished()
                }
            }
        }

        // ✅ chạy ngay số 3 luôn, không đợi 1 nhịp handler
        countdownRunnable.run()
    }

    private fun onCountdownFinished() {
        binding.apply {
            // Hide btnPlay with fade animation
            btnPlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    btnPlay.gone()
                }
                .start()

            // Change imgPlay to use img_bounty_playing
            imgPlay.setImageResource(R.drawable.img_bounty_playing)
            imgPlay.visible()

            // Keep camera visible in imgCamera area
            imgCamera.visible()

            // Start random bounty filter animation
            startRandomBountyAnimation()
        }
    }

    private fun startRandomBountyAnimation() {
        binding.tvBountyFilter.apply {
            textSize = 40f
            setTextColor(Color.parseColor("#3B2104"))
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            visible()

            // Animate in
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start()
        }

        val startTime = System.currentTimeMillis()
        val duration = 4000L // 4 seconds

        randomRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime

                if (elapsed < duration) {
                    // 30% probability to show infinity symbol, 70% to show random number
                    val showInfinity = Random.nextInt(100) < 30

                    val displayText = if (showInfinity) {
                        "∞"  // Infinity symbol
                    } else {
                        // Generate random bounty value between 100,000 and 10,000,000
                        val randomValue = Random.nextInt(100000, 10000001)
                        NumberFormat.getNumberInstance(Locale.US).format(randomValue)
                    }

                    binding.tvBountyFilter.text = displayText

                    // Add slight scale pulse effect during animation
                    binding.tvBountyFilter.apply {
                        animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(50)
                            .withEndAction {
                                animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(50)
                                    .start()
                            }
                            .start()
                    }

                    // Update every 100ms for smooth animation
                    handler.postDelayed(this, 100)
                } else {
                    // Animation finished, take photo
                    takePhoto()
                }
            }
        }

        handler.post(randomRunnable!!)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.imgCamera.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Camera is ready but stays hidden until countdown finishes
                // imgCamera will be made visible in onCountdownFinished()

            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Play camera sound
        SoundHelper.playSound(R.raw.camera_sound)

        // Show white flash effect
        binding.flashOverlay.apply {
            visible()
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(100)
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            gone()
                        }
                        .start()
                }
                .start()
        }

        // Capture photo
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Convert image to bitmap and save
                    val bitmap = image.toBitmap()
                    val photoFile = saveBitmapToFile(bitmap)

                    image.close()

                    // Navigate to SuccessfulBountyActivity
                    if (photoFile != null) {
                        val intent = Intent(this@BountyFilterActivity, SuccessfulBountyActivity::class.java).apply {
                            putExtra("PHOTO_PATH", photoFile.absolutePath)
                            putExtra("BOUNTY_VALUE", binding.tvBountyFilter.text.toString())
                        }
                        showInterAll {
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(
                            this@BountyFilterActivity,
                            "Failed to save photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@BountyFilterActivity,
                        "Photo capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val fileName = "bounty_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val granted = grantResults.isNotEmpty() &&
                         grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (granted) {
                // Reset counter when permission is granted
                android.util.Log.d("BountyFilter", "Permission granted, resetting counter to 0")
                sharePreference.setCameraPermission(0)
                startBountyFilterSequence()
            } else {
                // Increment counter when permission is denied
                val currentCount = sharePreference.getCameraPermission()
                val newCount = currentCount + 1
                sharePreference.setCameraPermission(newCount)
                android.util.Log.d("BountyFilter", "Permission denied, counter: $currentCount -> $newCount")

                // Just show toast, don't go to settings yet
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun initAds() {
        // Load interstitial ad
        // Load native collapsible ad
        initNativeCollab()
    }
    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(this,
            getString(R.string.native_collap_fillter),
            binding.nativeClBounty)
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handler.removeCallbacks(randomRunnable ?: return)
    }

    override fun onStart() {
        super.onStart()
        // Reset counter if permission is now granted (user came back from settings)
        if (allPermissionsGranted()) {
            sharePreference.setCameraPermission(0)
        }
    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Clean up and go back
        handler.removeCallbacks(randomRunnable ?: return)
        super.onBackPressed()
    }
}
