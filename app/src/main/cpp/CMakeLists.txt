cmake_minimum_required(VERSION 3.18.1)

project("healthub_native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# For now, we'll use a pre-built Whisper library instead of building from source
# This avoids the complex NDK build issues

# Placeholder native library (can be used for future native code)
add_library(
    healthub_native
    SHARED
    placeholder.cpp
)

target_link_libraries(
    healthub_native
    android
    log
)
