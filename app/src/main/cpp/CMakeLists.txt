# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

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

include_directories(dexinfo)
include_directories(libflush)
include_directories(oat-parser)

add_subdirectory(libflush)
add_subdirectory(dexinfo)
add_subdirectory(oat-parser)
#target_include_directories(native-lib PRIVATE ${CMAKE_SOURCE_DIR})

add_library( # Sets the name of the library.
        native-lib
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        fakedl.cpp
        ReadOffset.cpp
        CheckFlags.cpp
        native-lib.cpp
        )

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( # Specifies the target library.
        native-lib
        oatparser
        flush
        dexinfo
        ${log-lib}) #NDK


#add_dependencies(native-lib LIEF)