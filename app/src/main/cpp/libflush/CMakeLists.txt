# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)
project(libflush)

#/home/finder/Android/Sdk/ndk/21.0.6113669
set(CMAKE_VERBOSE_MAKEFILE on)
INCLUDE(config.cmake)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/libflush/libs/${CMAKE_ANDROID_ARCH_ABI}/)
include_directories(libflush/
        libflush/eviction/)
include_directories(.)

file(GLOB LOCAL_SRC_FILES libflush/*.c)
file(GLOB LOCAL_SRC_FILES_EVICTION libflush/eviction/*.c)
set(LOCAL_SRC_FILES ${LOCAL_SRC_FILES} ${LOCAL_SRC_FILES_EVICTION})


#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -D__STDC_CONSTANT_MACROS -D__STDC_FORMAT_MACROS -fpermissive")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=gnu11 -pedantic -Wall -Wextra -fPIC -O3")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#abiFilters "armeabi", "armeabi-v7a" , "arm64-v8a", "x86", "x86_64", "mips", "mips64"
if(${ANDROID_ABI} MATCHES "arm64-v8a") #针对不同 ABI 设置不同的编译代码 或 if(${ANDROID_ABI} STREQUAL "areambi")
        file(GLOB LOCAL_SRC_ABI libflush/armv8/*.c)
        SET(USE_EVICTION 0)
        #SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-D__ARM_ARCH_8A__")
        ADD_DEFINITIONS(-D__ARM_ARCH_8A__)
        SET(LOCAL_SRC_FILES ${LOCAL_SRC_FILES} ${LOCAL_SRC_ABI})
        #message("output:${LOCAL_SRC_FILES}")
elseif(${ANDROID_ABI} MATCHES "armeabi-v7a")
        file(GLOB LOCAL_SRC_ABI libflush/armv7/*.c)
        SET(USE_EVICTION  1)
        SET(LOCAL_SRC_FILES ${LOCAL_SRC_FILES} ${LOCAL_SRC_ABI})
endif()

string(TOUPPER ${TIME_SOURCE} TIME_SOURCE_UC)
#SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DTIME_SOURCE=TIME_SOURCE_${TIME_SOURCE_UC}")
add_definitions(-DTIME_SOURCE=TIME_SOURCE_${TIME_SOURCE_UC})

message("output:${DEVICE_CONFIGURATION}")
if(NOT ${DEVICE_CONFIGURATION} EQUAL 0)
    file(GLOB dconf ${DEVICE_CONFIGURATION})
    if (${dconf})
        #SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DDEVICE_CONFIGURATION=${DEVICE_CONFIGURATION}")
        add_definitions(-DDEVICE_CONFIGURATION=${DEVICE_CONFIGURATION})
        message("checkpoint -DDEVICE_CONFIGURATION=${DEVICE_CONFIGURATION}")
    else()
        #SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DDEVICE_CONFIGURATION=\"strategies/${DEVICE_CONFIGURATION}.h\"")
        add_definitions(-DDEVICE_CONFIGURATION=strategies/${DEVICE_CONFIGURATION}.h)
endif()
endif()

if(NOT ${USE_EVICTION} EQUAL 0)
    #SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DUSE_EVICTION=${USE_EVICTION}")
    add_definitions(-DUSE_EVICTION=${USE_EVICTION})
endif()

if(NOT ${HAVE_PAGEMAP_ACCESS} EQUAL 0)
    #SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DHAVE_PAGEMAP_ACCESS=${HAVE_PAGEMAP_ACCESS}")
    add_definitions(-DHAVE_PAGEMAP_ACCESS=${HAVE_PAGEMAP_ACCESS})
endif()

#if(${ANDROID_PLATFORM} EQUAL 0)
#        SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DANDROID_PLATFORM=$(subst android-,,${ANDROID_PLATFORM")
#endif()

if(${WITH_PTHREAD} EQUAL 0)
    #SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DPTHREAD_ENABLE")
    add_definitions(-DPTHREAD_ENABLE)
endif()

#SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} "-DWITH_THREADS=1")
add_definitions(-DWITH_THREADS=1)
add_definitions(-DANDROID_PLATFORM=${ANDROID_PLATFORM})
# OatParser
add_library(
        flush
        #code for parser
        SHARED
        #libflush
        ${LOCAL_SRC_FILES}
)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
log-lib
log)

find_library(libfiu-lib
libfiu)

#FIU_INC ?= $(shell ${PKG_CONFIG} --cflags libfiu)
#FIU_LIB ?= $(shell ${PKG_CONFIG} --libs libfiu) -ldl
#set(libflush ${CMAKE_SOURCE_DIR}/libflush/libs/${CMAKE_ANDROID_ARCH_ABI}/libflush.so)
# add_subdirectory(DEX)

target_link_libraries( # Specifies the target library.
flush
${log-lib}) #NDK


#add_dependencies(native-lib LIEF)