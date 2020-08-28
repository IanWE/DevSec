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
#include "logoutput.h"
#include "oat-parser/oat/OATParser.h"
#include "dexinfo/dexinfo.c"
#include "fakedl.cpp"

jobjectArray Decompress(JNIEnv* env,jstring jstr){
    jclass jniclass = (*env).FindClass("com/SMU/DevSec/CacheScan");
    if (NULL == jniclass) {
        LOGE("ZipRead","Can't find jclass");
        return NULL;
    }
    jmethodID jnimethod = (*env).GetStaticMethodID(jniclass, "decompress","(Ljava/lang/String;)[Ljava/lang/String;");
    jobjectArray dexlist=(jobjectArray)(*env).CallStaticObjectMethod(jniclass,jnimethod,jstr);
    if(dexlist) {
        //const char *dex = (char *) (*env).GetStringUTFChars(jstr, NULL);
        (*env).DeleteLocalRef(jniclass);
        return dexlist;
    }
    return NULL;
}

void ReadOatOffset(JNIEnv* env, void* start, std::string jar_file, size_t* addr, char** funcs, \
        std::string read_file, size_t length, size_t &current_length) {
    LOGD("Parsing Oat:%s",read_file.c_str());
    Art::OATParser oatParser;
    if (!oatParser.ParseOatFile(read_file)) {
        LOGD("Parsing Oat:%s Error!",read_file.c_str());
        return;
    }
    ////split class_function into classes and functions
    char **c = (char**)malloc(length*sizeof(char*));
    char **f = (char**)malloc(length*sizeof(char*));
    for(size_t t=0; t<length; t++){
        c[t] = strtok(funcs[t], "_");
        f[t] = strtok(NULL,"_");
        //LOGD("cccc %d %s",t,c[t]);
        //LOGD("ffff %d %s %s",t,c[t],f[t]);
        addr[current_length+t]=0;
    }
    //Read function offset
    std::vector<const Art::OatDexFile*> oat_dex_storages= oatParser.GetOatDexes();
    LOGD("Size: 0x%x and %d Oat DexFile",oatParser.Size(),oat_dex_storages.size());
    const char *jarfile=oat_dex_storages[0]->GetLocation().c_str();
    if(jarfile==NULL){
        jarfile = jar_file.c_str();
    }
    jstring jstr = env->NewStringUTF(jarfile); //turn to jstring
    LOGD("Jar File: %s",jarfile);//normally output
    jobjectArray dexlist = Decompress(env,jstr);
    for(int i=0;i<oat_dex_storages.size();i++){
        vector<vector<string>> func_list;
        vector<string> classnames;
        jstring obj = (jstring)env->GetObjectArrayElement(dexlist,i);
        string dexfile = env->GetStringUTFChars(obj,NULL);
        func_list = dexparse(dexfile.c_str(),classnames);
        //func_list is all func in dex file, funcs is our target function
        for(size_t t=0; t<length; t++) { //
            if(addr[current_length+t]!=0)
                continue;
            int found = 0;
            for(int cls=0;cls<func_list.size();cls++) {//find the class
                if (!strcmp((char *)classnames[cls].c_str(), c[t])){
                    LOGD("Find Class: %d:%s, include %d functions",cls,classnames[cls].c_str(),func_list[cls].size());
                    //s = strtok(NULL, "_");
                    for (int fc = 0; fc < func_list[cls].size(); fc++) {//find the function
                        //LOGD("Compare Function: %d: %s and %s",func_list[cls].size(),func_list[cls][fc].c_str(), f[t]);
                        if(!strcmp((char *) func_list[cls][fc].c_str(), f[t])) {
                            LOGD("Find Function: %s",func_list[cls][fc].c_str());//normally output
                            Art::OATParser::OatClass oatcls = oat_dex_storages[i]->GetOatClass(cls);
                            Art::OATParser::OatMethod m = oatcls.GetOatMethod(fc);
                            if(m.GetOffset()!=0) {
                                addr[current_length+t] = (size_t)start + m.GetOffset() + oatParser.GetOatHeaderOffset();
                                LOGD("OutputFunction: %s and offset %x",func_list[cls][fc].c_str(),m.GetOffset());
                                found = 1;
                                break;
                            }
                        }
                    }//for function
                }
                if(found) break;
            }//for class
        }//for target function list
    }//for dex
    current_length = current_length+length;
}//Read Oat

void* ExtractOffset(void* s, const char* filename, char* func){
    //void* handle = dlopen(filename, RTLD_LAZY);
    void* handle = fake_dlopen(filename, RTLD_NOW);
    if (!handle) {
        LOGE("Get %s address error ", filename);
        exit(0);
    }
    size_t offset;
    void* f = fake_dlsym(handle, func, offset);
    f = (void*)((size_t)s + offset);
    LOGD("Get %s address: %p", func,f);//0x7a9f98f770,0x7a9f946000  camera:0x7aa099b000 0x7aa09ce514
    return f;//st_value 0x49770 st_value 0x33514
}

void ReadSo(JNIEnv* env, void* start, size_t* addr, char** funcs, \
        std::string read_file, size_t length, size_t &current_length){
    for(int i=0;i<length;i++){
        addr[current_length++] = (size_t)ExtractOffset(start,read_file.c_str(),funcs[i]);//find the addr of function in memory;
    //addr[current_length++]=0;
    }
}//1

void ReadOffset(JNIEnv* env, std::string range, std::string offset, size_t* addr, char** funcs, \
         size_t length, std::string filename) {
    static size_t current_length = 0;
    void *start = NULL;
    void *end = NULL;
    //get all address list
    //=================Read Offset===============================
    string suffix = filename.substr(filename.find_last_of('.') + 1);
    LOGD("The suffix is %s", suffix.c_str());
    //map file
    int fd;
    struct stat sb;
    fd = open(filename.c_str(), O_RDONLY);
    fstat(fd, &sb);
    LOGD("size: %d of filename %s",sb.st_size,filename.c_str());
    unsigned char* s = (unsigned char *)mmap(0, sb.st_size, PROT_READ, MAP_SHARED, fd, 0);
    if(s == MAP_FAILED)
    {
        LOGD("Mapping Error, file is too big or app do not have the permisson!");
        exit(0);
    }

    if (!strcmp(suffix.c_str(), "oat") || !strcmp(suffix.c_str(), "odex")) {
        ReadOatOffset(env, s, range, addr, funcs, filename, length, current_length);
    }
    // ==================Read so library==========================
    if (!strcmp(suffix.c_str(), "so")) {
        /*
        if (!sscanf(range.c_str(), "%p-%p", &start, &end)) {
            LOGE("Could not parse range parameter(range): %s\n", range.c_str());
            exit(0);
        }
        size_t ofs;
        if (!sscanf(offset.c_str(), "%zu", &ofs)) {
            LOGE("Could not parse range parameter(ofs): %s\n", offset.c_str());
            exit(0);
        }
        ofs = ofs & ~(0x3F);//clean
        start = (void *) ((size_t) start - ofs);
         */
        ReadSo(env, s, addr, funcs, filename, length, current_length);
    }
}