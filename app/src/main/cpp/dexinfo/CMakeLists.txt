# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
project(libdexinfo)
# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)

#/home/finder/Android/Sdk/ndk/21.0.6113669
set(CMAKE_VERBOSE_MAKEFILE on)
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -D__STDC_CONSTANT_MACROS -D__STDC_FORMAT_MACROS -fpermissive")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        dexinfo
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        dexinfo.cpp
        )

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( # Specifies the target library.
        dexinfo
        ${log-lib}) #NDK

#add_dependencies(native-lib LIEF)