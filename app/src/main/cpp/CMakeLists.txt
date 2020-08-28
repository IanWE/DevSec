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

add_library( # Sets the name of the library.
        native-lib
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        #fakedl.cpp
        #logoutput.h
        #ReadOffset.cpp
        native-lib.cpp
        )

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

find_library( # Sets the name of the path variable.
        log-lib
        log)

#add_library(libflush SHARED IMPORTED)
#set_target_properties(libflush
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/libflush/libs/${CMAKE_ANDROID_ARCH_ABI}/libflush.so)
set(libflush ${CMAKE_SOURCE_DIR}/libflush/libs/${CMAKE_ANDROID_ARCH_ABI}/libflush.so)
#include_directories(${CMAKE_SOURCE_DIR}/libflush/libflush/)
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