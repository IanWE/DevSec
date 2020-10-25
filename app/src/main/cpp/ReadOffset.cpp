#include <jni.h>

#include <string>
#include <sys/stat.h>
#include <iostream>
#include <memory>
#include <dlfcn.h>
#include "fakedl.h"
#include <oat/OATParser.h>
#include <dexinfo.h>
#include <sys/mman.h>
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include "ReadOffset.h"
#include "logoutput.h"

extern int length_of_camera_audio[2];
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
    if (!oatParser.ParseOatFile(read_file)) {//parse OAT file
        LOGD("Parsing Oat:%s Error!",read_file.c_str());
        return;
    }
    ////split class_function into classes and functions
    char **c = (char**)malloc(length*sizeof(char*));
    char **f = (char**)malloc(length*sizeof(char*));
    for(size_t t=0; t<length; t++){
        c[t] = strtok(funcs[t], "_");
        f[t] = strtok(NULL,"_");
        addr[current_length+t]=0;
        if(f[t]==NULL)//if it is only class
            addr[current_length+t] = reinterpret_cast<size_t>(malloc(sizeof(size_t *) * 1));
    }
    //Read function offset
    std::vector<const Art::OatDexFile*> oat_dex_storages= oatParser.GetOatDexes();
    LOGD("Size: 0x%x and %d Oat DexFile",oatParser.Size(),oat_dex_storages.size());
    const char *jarfile=oat_dex_storages[0]->GetLocation().c_str();
    if(jarfile==NULL){
        jarfile = jar_file.c_str();
    }
    LOGD("xxxxxxxxxxxxxxxxxxxx4");
    jstring jstr = env->NewStringUTF(jarfile);//turn to jstring
    LOGD("Jar File: %s",jarfile);//normally output
    jobjectArray dexlist = Decompress(env,jstr);
    if(dexlist==NULL){
        LOGD("Unable to find dex files of %s",read_file.c_str());
        for(int t=0;t<length;t++)
            addr[current_length+t] = 0;
        return;
    }
    LOGD("xxxxxxxxxxxxxxxxxxxx5");
    for(int i=0;i<oat_dex_storages.size();i++){
        std::vector<std::vector<std::string>> func_list;
        std::vector<std::string> classnames;
        jstring obj = (jstring)env->GetObjectArrayElement(dexlist,i);
        string dexfile = env->GetStringUTFChars(obj,NULL);
        func_list = dexparse(dexfile.c_str(), classnames);
        //func_list is all func in dex file, funcs is our target function
        for(size_t t=0; t<length; t++) { //
            if(addr[current_length+t]!=0&&f[t]!=NULL)//if it is a specific function
                continue;
            int k = 0;
            for(int cls=0;cls<func_list.size();cls++) {//find the class
                if (!strcmp((char *)classnames[cls].c_str(), c[t])){
                    if(!strcmp((char *)classnames[cls].c_str(), "AudioManager.java")) {//if it is Audio
                        k = 1;
                    }
                    for(int fc = 0; fc < func_list[cls].size(); fc++) {//find the function
                        //LOGD("Compare Function: %d: %s and %s",func_list[cls].size(),func_list[cls][fc].c_str(), f[t]);
                        if(f[t]==NULL){//record the addr only if it is the first run
                            //LOGD("Found class %s",(char *)classnames[cls].c_str());
                            length_of_camera_audio[k]++;
                            int l = length_of_camera_audio[k];
                            Art::OATParser::OatClass oatcls = oat_dex_storages[i]->GetOatClass(cls);
                            Art::OATParser::OatMethod m = oatcls.GetOatMethod(fc);
                            addr[current_length+t] = reinterpret_cast<size_t>(realloc(
                                    reinterpret_cast<void *>(addr[current_length+t]),
                                    l * sizeof(size_t)));
                            *((size_t *) addr[current_length + t] + l - 1) = 0;
                            //store the addr in a list
                            if(m.GetOffset()!=0) {
                                *((size_t *) addr[current_length + t] + l - 1) =
                                        (size_t) start + m.GetOffset() +
                                        oatParser.GetOatHeaderOffset();
                                LOGD("Function: %s and offset %x, %p",
                                     func_list[cls][fc].c_str(), m.GetOffset(),
                                     *((size_t *) addr[current_length + t] + l - 1));
                            }
                        }
                        else if(!strcmp((char *) func_list[cls][fc].c_str(), f[t])) {
                            //LOGD("Find Function: %s",func_list[cls][fc].c_str()); //normally output
                            Art::OATParser::OatClass oatcls = oat_dex_storages[i]->GetOatClass(cls);
                            Art::OATParser::OatMethod m = oatcls.GetOatMethod(fc);
                            if(m.GetOffset()!=0) {
                                addr[current_length+t] = (size_t)start + m.GetOffset() + oatParser.GetOatHeaderOffset();
                                LOGD("OutputFunction: %s and offset %x, %p",func_list[cls][fc].c_str(),m.GetOffset(),addr[current_length+t]);
                                break;
                            }
                        }
                    }//for function
                }//found class
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
        void* f = 0;
        return f;
    }
    size_t offset;
    void* f = fake_dlsym(handle, func, offset);
    if(f!=0){
        f = (void*)((size_t)s + offset);
    }
    //LOGD("Load %s address: %p", func,f);//0x7a9f98f770,0x7a9f946000  camera:0x7aa099b000 0x7aa09ce514
    return f;//st_value 0x49770 st_value 0x33514
}

void ReadSo(JNIEnv* env, void* start, size_t* addr, char** funcs, \
        std::string read_file, size_t length, size_t &current_length){
    for(int i=0;i<length;i++){
        //size_t address = (size_t)ExtractOffset(start,read_file.c_str(),funcs[i]);
        addr[current_length++] = (size_t)ExtractOffset(start,read_file.c_str(),funcs[i]);//find the addr of function in memory;
    //addr[current_length++]=0;
    }
}//1

void ReadOffset(JNIEnv* env, std::string dex, size_t* addr, char** funcs, \
         size_t length, std::string filename) {
    static size_t current_length = 0;
    void *start = NULL;
    void *end = NULL;
    //get all address list
    //=================Read Offset===============================
    string suffix = filename.substr(filename.find_last_of('.') + 1);
    //LOGD("The suffix is %s", suffix.c_str());
    //map file
    int fd;
    struct stat sb;
    if((access(filename.c_str(),F_OK))==-1)
    {
        LOGD("Filename %s do not exists",filename.c_str());
        for(int i=0;i<length;i++){
            addr[current_length++] = 0;
        }
        return;
    }
    fd = open(filename.c_str(), O_RDONLY);
    fstat(fd, &sb);
    unsigned char* s = (unsigned char *)mmap(0, sb.st_size, PROT_READ, MAP_SHARED, fd, 0);
    if(s == MAP_FAILED)
    {
        LOGD("Mapping Error, file is too big or app do not have the permisson!");
        exit(0);
    }
    LOGD("size: %d of filename %s, loaded at %p",sb.st_size,filename.c_str(),s);
    //fclose(reinterpret_cast<FILE *>(fd));
    if (!strcmp(suffix.c_str(), "oat") || !strcmp(suffix.c_str(), "odex")) {
        ReadOatOffset(env, s, dex, addr, funcs, filename, length, current_length);
    }
    // ==================Read so library==========================
    if (!strcmp(suffix.c_str(), "so")) {
        ReadSo(env, s, addr, funcs, filename, length, current_length);
    }
}