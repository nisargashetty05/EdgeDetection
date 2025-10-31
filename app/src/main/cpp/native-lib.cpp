#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>

#define TAG "NativeProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_edgedetectionapp_NativeProcessor_testOpenCV(
        JNIEnv* env,
        jobject /* this */) {

    std::string version = cv::getVersionString();
    LOGD("OpenCV Version: %s", version.c_str());

    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_edgedetectionapp_NativeProcessor_processFrame(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray input,
        jint width,
        jint height,
        jbyteArray output) {

    try {
        // Get input data
        jbyte* inputBytes = env->GetByteArrayElements(input, nullptr);
        jbyte* outputBytes = env->GetByteArrayElements(output, nullptr);

        // Convert to cv::Mat (YUV format from camera)
        cv::Mat yuvMat(height + height/2, width, CV_8UC1, (unsigned char*)inputBytes);
        cv::Mat rgbMat;
        cv::cvtColor(yuvMat, rgbMat, cv::COLOR_YUV2RGB_NV21);

        // Convert to grayscale
        cv::Mat grayMat;
        cv::cvtColor(rgbMat, grayMat, cv::COLOR_RGB2GRAY);

        // Apply Gaussian blur to reduce noise
        cv::Mat blurredMat;
        cv::GaussianBlur(grayMat, blurredMat, cv::Size(5, 5), 1.5);

        // Apply Canny edge detection
        cv::Mat edgesMat;
        cv::Canny(blurredMat, edgesMat, 50, 150);

        // Convert edges back to RGB for display
        cv::Mat resultMat;
        cv::cvtColor(edgesMat, resultMat, cv::COLOR_GRAY2RGB);

        // Copy to output
        memcpy(outputBytes, resultMat.data, resultMat.total() * resultMat.elemSize());

        // Release arrays
        env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);
        env->ReleaseByteArrayElements(output, outputBytes, 0);

    } catch (const std::exception& e) {
        LOGD("Error processing frame: %s", e.what());
    }
}