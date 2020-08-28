# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)
project(libflush)

#/home/finder/Android/Sdk/ndk/21.0.6113669
set(CMAKE_VERBOSE_MAKEFILE on)
#set(distribution_DIR ${CMAKE_SOURCE_DIR}/libflush/libs/)
#include_directories(libflush/
#                   libflush/eviction/)

INCLUDE(config.cmake)
file(GLOB LOCAL_SRC_FILES libflush/*.c)
file(GLOB LOCAL_SRC_FILES_EVICTION libflush/eviction/*.c)


#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -D__STDC_CONSTANT_MACROS -D__STDC_FORMAT_MACROS -fpermissive")
set(CMAKE_C_FLAGS " -std=gnu11 -pedantic -Wall -Wextra -fPIC -O3 $(INCS)")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#abiFilters "armeabi", "armeabi-v7a" , "arm64-v8a", "x86", "x86_64", "mips", "mips64"
if(${ANDROID_ABI} MATCHES "arm64-v8a") #针对不同 ABI 设置不同的编译代码 或 if(${ANDROID_ABI} STREQUAL "areambi")
    SET (SRC_LIST ${SRC_LIST} test.cpp)
elseif(${ANDROID_ABI} MATCHES  "x86")
    SET (SRC_LIST ${SRC_LIST} test.cpp)
elseif(${ANDROID_ABI} MATCHES  "armeabi")
    SET (SRC_LIST ${SRC_LIST} test2.cpp)
elseif(${ANDROID_ABI} MATCHES  "armeabi-v7a")
    SET (SRC_LIST ${SRC_LIST} test2.cpp)
endif()


ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += $(wildcard libflush/armv7/*.c)
USE_EVICTION = 1
endif

# OatParser
add_library(
        liboatparser
        #code for parser
        SHARED
        #oatparser
        oat-parser/base/bit_vector-inl.h
        oat-parser/oat/instruction_set.h
        oat-parser/elf/elfloader.cpp
        oat-parser/elf/map_file.h
        oat-parser/zlib/adler32.c
        oat-parser/oat/DexHeader.h
        oat-parser/oat/OATHeader.cpp
        oat-parser/art/compiler_filter.cpp
        oat-parser/oat/OATParser.cpp
        oat-parser/logoutput.h
        oat-parser/StringPiece.cpp
)

# Add our executable
# ==================
#add_executable(HelloLIEF main.cpp)
#if (MSVC)
    # Used for the 'and', 'or' ... keywords - See: http://www.cplusplus.com/reference/ciso646/
#    target_compile_options(HelloLIEF PUBLIC /FIiso646.h)
#    set_property(TARGET HelloLIEF PROPERTY LINK_FLAGS /NODEFAULTLIB:MSVCRT)
#endif()
# Setup the LIEF include directory
#target_include_directories(native-lib
#        PUBLIC
#        ${LIEF_INCLUDE_DIRS}
#        )

# Enable C++11
#set_property(TARGET native-lib PROPERTY CXX_STANDARD           11)
#set_property(TARGET native-lib PROPERTY CXX_STANDARD_REQUIRED  ON)

# Link the executable with LIEF
#target_link_libraries(native-lib PUBLIC ${LIB_LIEF})
#add_dependencies(native-lib.cpp LIEF)
#=====================================================================
#add_subdirectory(LIEF/)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib
        log)

set(libflush ${CMAKE_SOURCE_DIR}/libflush/libs/${CMAKE_ANDROID_ARCH_ABI}/libflush.so)

# add_subdirectory(DEX)

target_link_libraries( # Specifies the target library.
        native-lib
        ${libflush}
        liboatparser
        #${LIB_LIEF}
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}) #NDK

target_link_libraries(
        liboatparser
        ${log-lib}
)

#add_dependencies(native-lib LIEF)