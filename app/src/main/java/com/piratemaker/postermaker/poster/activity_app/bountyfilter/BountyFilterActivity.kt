package com.piratemaker.postermaker.poster.activity_app.bountyfilter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.visible
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

        // Initial state: dark screen with only btnPlay visible
        binding.apply {
            // Create a dark overlay view
            root.setBackgroundColor(Color.BLACK)

            // Hide all elements except btnPlay
            imgPlay.gone()
            imgCamera.gone()
            tvBountyFilter.gone()

            // Only btnPlay is visible
            btnPlay.visible()
        }
    }

    override fun viewListener() {
        binding.btnPlay.setOnSingleClick {
            if (!isCountingDown) {
                if (allPermissionsGranted()) {
                    startBountyFilterSequence()
                } else {
                    ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                    )
                }
            }
        }

        binding.actionBar.btnActionBarLeft.setOnSingleClick {
            finish()
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = getString(R.string.bountyFilter)
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
            visible()
            textSize = 80f
        }

        val countdownRunnable = object : Runnable {
            override fun run() {
                if (currentIndex < countdownNumbers.size) {
                    binding.tvBountyFilter.text = countdownNumbers[currentIndex]
                    currentIndex++
                    handler.postDelayed(this, 1000)
                } else {
                    // Countdown finished
                    onCountdownFinished()
                }
            }
        }

        handler.post(countdownRunnable)
    }

    private fun onCountdownFinished() {
        binding.apply {
            // Hide btnPlay
            btnPlay.gone()

            // Screen becomes light (restore normal background)
            root.setBackgroundResource(R.drawable.img_bg_language)

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
            visible()
        }

        val startTime = System.currentTimeMillis()
        val duration = 4000L // 4 seconds

        randomRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime

                if (elapsed < duration) {
                    // Generate random bounty value between 100,000 and 10,000,000
                    val randomValue = Random.nextInt(100000, 10000001)
                    val formattedValue = NumberFormat.getNumberInstance(Locale.US).format(randomValue)
                    binding.tvBountyFilter.text = formattedValue

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

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Photo captured successfully
                    Toast.makeText(
                        this@BountyFilterActivity,
                        "Photo captured! Bounty: ${binding.tvBountyFilter.text}",
                        Toast.LENGTH_SHORT
                    ).show()

                    image.close()

                    // TODO: Process the captured image here
                    // For now, just finish the activity after a delay
                    handler.postDelayed({
                        finish()
                    }, 1500)
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
            if (allPermissionsGranted()) {
                startBountyFilterSequence()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handler.removeCallbacks(randomRunnable ?: return)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Clean up and go back
        handler.removeCallbacks(randomRunnable ?: return)
        super.onBackPressed()
    }
}
