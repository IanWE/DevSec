cmake_minimum_required(VERSION 2.8)
project(liboatparser C CXX)
#/home/finder/Android/Sdk/ndk/21.0.6113669
set(CMAKE_VERBOSE_MAKEFILE on)
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -D__STDC_CONSTANT_MACROS -D__STDC_FORMAT_MACROS -fpermissive")

file(GLOB LOCAL_SRC_FILES *.cpp)
file(GLOB LOCAL_SRC_FILES_ART art/*.cpp)
#file(GLOB LOCAL_SRC_FILES_BASE base/bit_vector.cc)
#file(GLOB LOCAL_SRC_FILES_DEX dex/*)
file(GLOB LOCAL_SRC_FILES_ELF elf/*.cpp)
file(GLOB LOCAL_SRC_FILES_OAT oat/*.cpp)
file(GLOB LOCAL_SRC_FILES_ZLIB zlib/*.c)#std::__ndk1::basic_string<char, std::__ndk1::char_traits<char>, std::__ndk1::allocator<char>

#message("SRC_FLIES:${LOCAL_SRC_FILES}
#        ${LOCAL_SRC_FILES_ART}
#        ${LOCAL_SRC_FILES_DEX}
#        ${LOCAL_SRC_FILES_ELF}
#        ${LOCAL_SRC_FILES_OAT}
#        ${LOCAL_SRC_FILES_ZLIB}
#        ${LOCAL_SRC_FILES_BASE}"
#        )
add_library(
        oatparser
        #code for parser
        SHARED
        #oatparser
        oat/OATParser.cpp
        elf/elfloader.cpp
        #${LOCAL_SRC_FILES}
        #${LOCAL_SRC_FILES_OAT}
        #${LOCAL_SRC_FILES_ART}
        #${LOCAL_SRC_FILES_DEX}
        #${LOCAL_SRC_FILES_ELF}
        #${LOCAL_SRC_FILES_ZLIB}
        #${LOCAL_SRC_FILES_BASE}
)

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( # Specifies the target library.
        oatparser
        ${log-lib}) #NDK





#add_executable(oatparser ${SOURCE_FILES})
