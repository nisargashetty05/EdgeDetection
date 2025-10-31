package com.example.edgedetectionapp

import android.util.Log

object NativeProcessor {

    private const val TAG = "NativeProcessor"

    init {
        try {
            System.loadLibrary("opencv_java4")
            Log.d(TAG, "✅ OpenCV library loaded")

            System.loadLibrary("edgedetection")
            Log.d(TAG, "✅ edgedetection library loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Failed to load native libraries", e)
            throw e
        }
    }

    external fun testOpenCV(): String

    /**
     * Process a camera frame with Canny edge detection
     * @param input YUV_420_888 frame data
     * @param width frame width
     * @param height frame height
     * @param output RGB output buffer (must be width * height * 3 bytes)
     */
    external fun processFrame(input: ByteArray, width: Int, height: Int, output: ByteArray)
}