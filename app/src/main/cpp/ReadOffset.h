//
// Created by finder on 20-9-9.
//

#ifndef DEVSEC_READOFFSET_H
#define DEVSEC_READOFFSET_H

#include <jni.h>
#include <string>
#include "oat-parser/StringPiece.h"
#include "oat-parser/oatparser.h"
#include <unistd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <iostream>
#include <memory>
#include <dlfcn.h>

jobjectArray Decompress(JNIEnv* env,jstring jstr);

void ReadOatOffset(JNIEnv* env, void* start, std::string jar_file, size_t* addr, char** funcs, \
        std::string read_file, size_t length, size_t &current_length, \
        std::vector<std::string> &camera_list,std::vector<std::string> &audio_list);

void* ExtractOffset(void* s, const char* filename, char* func);

void ReadSo(JNIEnv* env, void* start, size_t* addr, char** funcs, \
        std::string read_file, size_t length, size_t &current_length);

size_t
ReadOffset(JNIEnv *env, std::string dexlist, size_t *addr, char **funcs, size_t length, std::string filename,
           std::vector<std::string> &camera_list, std::vector<std::string> &audio_list);

void ReadOatOffset(JNIEnv* env, void* start, std::string jar_file, size_t* addr, char** funcs, \
        std::string read_file, size_t length, size_t &current_length);
#endif //DEVSEC_READOFFSET_H
