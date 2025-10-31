package com.example.edgedetectionapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val CAMERA_PERMISSION_CODE = 100

    private lateinit var imageView: ImageView
    private lateinit var statusText: TextView
    private lateinit var saveButton: Button
    private var cameraManager: CameraManager? = null

    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        statusText = findViewById(R.id.statusText)
        saveButton = findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            saveCurrentFrame()
        }

        // Test OpenCV
        try {
            val version = NativeProcessor.testOpenCV()
            Log.d(TAG, "✅ OpenCV: $version")
            statusText.text = "OpenCV: $version"
        } catch (e: Exception) {
            Log.e(TAG, "❌ OpenCV error", e)
            statusText.text = "Error: ${e.message}"
            return
        }

        // Check camera permission
        if (checkPermission()) {
            startCamera()
        } else {
            requestPermission()
        }
    }

    private fun checkPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCamera() {
        statusText.text = "Starting camera..."
        cameraManager = CameraManager(this) { bytes, width, height ->
            processFrame(bytes, width, height)
        }
        cameraManager?.startCamera()
    }

    private fun processFrame(input: ByteArray, width: Int, height: Int) {
        try {
            val output = ByteArray(width * height * 3)

            val start = System.currentTimeMillis()
            NativeProcessor.processFrame(input, width, height, output)
            val time = System.currentTimeMillis() - start

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)

            for (i in 0 until width * height) {
                val idx = i * 3
                val r = output[idx].toInt() and 0xFF
                val g = output[idx + 1].toInt() and 0xFF
                val b = output[idx + 2].toInt() and 0xFF
                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            runOnUiThread {
                currentBitmap?.recycle()
                currentBitmap = bitmap
                imageView.setImageBitmap(bitmap)
                updateFps(time)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        }
    }

    private fun saveCurrentFrame() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "No frame to save", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Save to Pictures directory
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "EdgeDetection")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "edge_detection_$timestamp.png"
            val file = File(appDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Toast.makeText(this, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Frame saved: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error saving frame", e)
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateFps(processingTime: Long) {
        frameCount++
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsTime

        if (elapsed >= 1000) {
            val fps = (frameCount * 1000.0) / elapsed
            frameCount = 0
            lastFpsTime = now
            statusText.text = "FPS: %.1f | %dms | 640x480".format(fps, processingTime)
        }
    }

    override fun onPause() {
        super.onPause()
        cameraManager?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            cameraManager?.startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.stopCamera()
        currentBitmap?.recycle()
    }
}
