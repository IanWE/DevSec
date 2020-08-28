#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include "libflush/libflush/libflush.h"
#include "split.c"
#include "ReadOffset.cpp"
#include "logoutput.h"
#include "CheckFlags.cpp"

size_t first_run = 1;
size_t l=0;
size_t f;
size_t *continueRun;
int threshold = 0;
int *flags;
int sum_length = 0;
size_t* addr= NULL;
int thd[3]={0};
pthread_mutex_t g_lock;

int (*hit)(int, int*, pthread_mutex_t*, size_t *,size_t, size_t, int*,int*, size_t*);
void (*acs)(void*);
extern "C" JNIEXPORT jstring JNICALL
Java_com_SMU_DevSec_SideChannelJob_scan(
        JNIEnv *env,
        jobject thiz,
        jint cpu, jobjectArray ranges, jobjectArray offsets, jint fork, jobjectArray filenames,
        jobjectArray func_lists, jstring target_lib,jstring target_func) {
    if (!first_run)  { //if it is not the first run, then only need to set continueRun as 1;
        *continueRun = 1;
        LOGD("Keep scanning");
        return env->NewStringUTF("");
    }
    first_run=0;
    //lock
    pthread_mutex_init(&g_lock, NULL);

    jsize size = env->GetArrayLength(ranges);
    char** func_list; //functions' offsets of every library;
    //get address list
    for(int i=0;i<size;i++)
    {
        jstring obj = (jstring)env->GetObjectArrayElement(ranges,i);
        string range = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(offsets,i);
        string offset = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(filenames,i);
        string filename = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(func_lists,i);
        int length=0;
        func_list  = split(',',(char*)env->GetStringUTFChars(obj,NULL), &length);//split a string into function list
        LOGD("Filename %s, Length %d.", filename.c_str(), length);
        //LOGD("xxxxxxxx %s : %s",func_list[0],(char*)env->GetStringUTFChars(obj,NULL));
        //expand addr[];
        sum_length = sum_length + length;
        addr = static_cast<size_t *>(realloc(addr,sum_length*sizeof(size_t)));
        ReadOffset(env,range,offset,addr,func_list,length,filename);
        if(sum_length>1024) //limit the length to 2048
            break;
    }

    //Load libflush
    void *handle;
    handle = dlopen ("libflush.so", RTLD_LAZY);
    if (handle) {
        LOGD("Loading libflush sucessfully");
    }
    hit = (int (*)(int, int*,pthread_mutex_t* , size_t *, size_t, size_t, int*, int*, size_t*)) dlsym(handle, "hit");
    if (!hit)  {
        LOGD("Loading libflush error");
    }
    continueRun=(size_t*) malloc(sizeof(size_t));
    *continueRun = 1;
    /* //remove non-compiled function
    int temp=0;
    for(int i=0;i<sum_length;i++){
        if(addr[i]==0){
            continue;
        }
        addr[temp]=addr[i];
        temp++;
    }
    sum_length=temp;
    */
    LOGD("Functions Length %d",sum_length);
    flags = (int*)malloc(sum_length*sizeof(int));
    memset(flags,0,sum_length*sizeof(int));
    //addr[sum_length-1]=0;
    //addr[2] = 0;
    hit(cpu,&threshold,&g_lock,addr,sum_length,l+f,flags,thd,continueRun);// start scaning
    LOGD("Stop scanning");
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_SMU_DevSec_SideChannelJob_pause(JNIEnv *env, jobject thiz) {
    // to stop scanning;
    *continueRun=0;
}