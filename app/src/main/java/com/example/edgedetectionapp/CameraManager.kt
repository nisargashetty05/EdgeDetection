package com.example.edgedetectionapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size

class CameraManager(
    private val context: Context,
    private val onFrameAvailable: (ByteArray, Int, Int) -> Unit
) {
    private val TAG = "CameraManager"
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null

    private var WIDTH = 640
    private var HEIGHT = 480

    @SuppressLint("MissingPermission")
    fun startCamera() {
        Log.d(TAG, "Starting camera...")
        startBackgroundThread()

        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = selectCamera(manager)

            // Get supported sizes
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()

            // Find best size close to 640x480
            val targetSize = findBestSize(sizes)
            WIDTH = targetSize.width
            HEIGHT = targetSize.height

            Log.d(TAG, "Using camera size: ${WIDTH}x${HEIGHT}")

            imageReader = ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        try {
                            val bytes = imageToByteArray(image)
                            onFrameAvailable(bytes, WIDTH, HEIGHT)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing image", e)
                        } finally {
                            image.close()
                        }
                    }
                }, backgroundHandler)
            }

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened successfully")
                    cameraDevice = camera
                    startCapture()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d(TAG, "Camera disconnected")
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
        }
    }

    private fun selectCamera(manager: android.hardware.camera2.CameraManager): String {
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }
        return manager.cameraIdList[0]
    }

    private fun findBestSize(sizes: Array<Size>): Size {
        // Target 640x480 or closest
        var bestSize = sizes.firstOrNull() ?: Size(640, 480)
        var minDiff = Int.MAX_VALUE

        for (size in sizes) {
            if (size.width <= 1280 && size.height <= 960) {
                val diff = Math.abs(size.width - 640) + Math.abs(size.height - 480)
                if (diff < minDiff) {
                    minDiff = diff
                    bestSize = size
                }
            }
        }

        return bestSize
    }

    private fun startCapture() {
        try {
            val surface = imageReader?.surface ?: return
            val device = cameraDevice ?: return

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.setRepeatingRequest(captureBuilder.build(), null, backgroundHandler)
                            Log.d(TAG, "Capture session started successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating request", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
        }
    }

    fun stopCamera() {
        try {
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        } finally {
            captureSession = null
            cameraDevice = null
            imageReader = null
            stopBackgroundThread()
        }
    }

    private fun imageToByteArray(image: Image): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val data = ByteArray(ySize + uSize + vSize)
        yBuffer.get(data, 0, ySize)
        vBuffer.get(data, ySize, vSize)
        uBuffer.get(data, ySize + vSize, uSize)

        return data
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Background thread interrupted", e)
        } finally {
            backgroundThread = null
            backgroundHandler = null
        }
    }
}
