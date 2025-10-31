cmake_minimum_required(VERSION 3.22.1)

project("edgedetection")

# Set C++ standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenCV configuration
set(OpenCV_DIR "C:/opencv-4.11.0-android-sdk/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)

message(STATUS "OpenCV libraries: ${OpenCV_LIBS}")
message(STATUS "OpenCV include dirs: ${OpenCV_INCLUDE_DIRS}")

# Add our native library
add_library(
        edgedetection
        SHARED
        native-lib.cpp
)

# Include OpenCV headers
target_include_directories(edgedetection PRIVATE ${OpenCV_INCLUDE_DIRS})

# Link libraries
target_link_libraries(
        edgedetection
        ${OpenCV_LIBS}
        android
        log
        jnigraphics
)