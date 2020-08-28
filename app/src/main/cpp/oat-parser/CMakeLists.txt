cmake_minimum_required(VERSION 2.8)
project(oatparser C CXX)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -D__STDC_CONSTANT_MACROS -D__STDC_FORMAT_MACROS -fpermissive")
#set(CMAKE_NDK_ROOT D:/sdk/android-ndk-r10d)
#
#include_directories(${CMAKE_NDK_ROOT}/platforms/android-9/arch-arm/usr/include
#                    ${CMAKE_NDK_ROOT}/platforms/android-9/arch-arm/usr/include/EGL
#                    ${CMAKE_NDK_ROOT}/sources/cxx-stl/stlport/stlport
#                    )

set(SOURCE_FILES
    base/bit_vector-inl.h
    oat/instruction_set.h
#    elf/elfcpp.h
#    elf/elfcpp_file.h
#    elf/elfcpp_internal.h
#    elf/elfcpp_swap.h
    elf/elfloader.cpp
#    elf/exec_buffer.h
    elf/map_file.h
    zlib/adler32.c
#    elf/sparc.h
#    elf/util.h
#    oat/DexHeader.cpp
    oat/DexHeader.h
    oat/OATHeader.cpp
    art/compiler_filter.cpp
#    oat/OATHeader.h
    oat/OATParser.cpp
#    oat/OATParser.h
#    elfloader.h
        #    oatparser.h
#    StringPiece.h
    StringPiece.cpp
)

add_executable(oatparser ${SOURCE_FILES})
