#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include <libflush/libflush.h>
#include <libflush/hit.h>
#include <libflush/calibrate.h>
#include "split.c"
#include "ReadOffset.h"
#include "logoutput.h"
//#include "CheckFlags.cpp"


int finishtrial1 = 0;
jint* filter;

int continueRun = 0;
size_t threshold = 0;
int *flags;
int sum_length = 0;
size_t* addr= NULL;

int compiler_position = 5;
int log_length = 0;
int logs[300000] = {0};
long times[300000] = {0};
int thresholds[300000] = {0};
int length_of_camera_audio[2] = {0,0};
pthread_mutex_t g_lock;

int* camera_pattern;
int* audio_pattern;
int camera_audio[] = {1,2};//indexes of camera list and audio list

extern "C" JNIEXPORT jstring JNICALL
Java_com_SMU_DevSec_SideChannelJob_scan(
        JNIEnv *env,
        jobject thiz,jintArray ptfilter) {
    int* arrp = env->GetIntArrayElements(ptfilter,0);
    continueRun = 1;
    for(int i=0;i<length_of_camera_audio[0];i++)//camera
    {
        if (arrp[i] == 1) {
            LOGD("Filter camera address:%d-%p",i,*((size_t *) addr[camera_audio[0]] + i));
            *((size_t *) addr[camera_audio[0]] + i) = 0;
        }
    }
    for(int i=length_of_camera_audio[0];i<length_of_camera_audio[0]+length_of_camera_audio[1];i++)//audio
    {
        if(arrp[i]==1) {
            LOGD("Filter audio address:%d-%p", i - length_of_camera_audio[0],
                 *((size_t *) addr[camera_audio[1]] + i - length_of_camera_audio[0]));
            *((size_t *) addr[camera_audio[1]] + i - length_of_camera_audio[0]) = 0;
        }
    }
    for(int i=1;i<3;i++){
        int c = i-1;
        int t1 = 0;
        int t2 = 0;
        for (int j = 0; j < length_of_camera_audio[c]; j++) {
            size_t target = *((size_t *) addr[i] + j);
            if (target == 0) {//if the target is 0, skip it.
                t1++;
                continue;
            }
            t2++;
        }
        if(i==1)
            LOGD("In Camera List, %d are null functions, %d are available functions.\n",t1,t2);
        else
            LOGD("In Audio List, %d are null functions, %d are available functions.\n",t1,t2);
    }
    hit(&g_lock, compiler_position, &continueRun,
        threshold, flags, times, thresholds, logs, log_length,sum_length,
        camera_pattern, audio_pattern, length_of_camera_audio, addr);
    LOGD("Finished scanning");
    return env->NewStringUTF("");
}

void swap(size_t *a,size_t *b)
{
    size_t temp;
    temp=*a;
    *a=*b;
    *b=temp;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_TrialModelStages_trial1(
        JNIEnv *env, jobject thiz) {
    LOGD("Start trial 1.\n");
    int length_alive_function = length_of_camera_audio[0]+length_of_camera_audio[1];
    memset(filter,0,length_alive_function*sizeof(int));
    stage1_(filter, threshold, length_of_camera_audio, addr, camera_audio, &finishtrial1); //eliminate all poping functions.
    LOGD("Finish TrialMode 1");
    return;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_SideChannelJob_trial2(
        JNIEnv *env, jobject thiz) {
    LOGD("Start trial 2.\n");
    continueRun = 1;
    hit(&g_lock, compiler_position, &continueRun,
        threshold, flags, times, thresholds, logs, log_length, sum_length,
        camera_pattern, audio_pattern, length_of_camera_audio, addr);
    LOGD("Finish TrialMode 2");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_SMU_DevSec_CacheScan_init(
        JNIEnv *env,
        jobject thiz,
        jobjectArray dexlist, jobjectArray filenames, jobjectArray func_lists) {
    pthread_mutex_init(&g_lock, NULL);
    jsize size = env->GetArrayLength(dexlist);
    char** func_list; //functions' offsets of every library;
    //get address list
    for(int i=0;i<size;i++)
    {
        jstring obj = (jstring)env->GetObjectArrayElement(dexlist,i);
        std::string dex = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(filenames,i);
        std::string filename = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(func_lists,i);
        int length=0;
        func_list  = split(',',(char*)env->GetStringUTFChars(obj,NULL), &length);//split a string into function list
        LOGD("Filename %s, Length %d.", filename.c_str(), length);
        //expand addr[];
        sum_length = sum_length + length;
        addr = static_cast<size_t *>(realloc(addr,sum_length*sizeof(size_t)));
        ReadOffset(env,dex,addr,func_list,length,filename);
    }
    LOGD("Functions Length %d",sum_length);
    LOGD("Camera List: %d, Audio List: %d",length_of_camera_audio[0],length_of_camera_audio[1]);
    threshold = get_threshold();
    threshold = adjust_threshold(threshold, length_of_camera_audio, addr, camera_audio, &finishtrial1);//
    camera_pattern = (int*)malloc(sizeof(int)*length_of_camera_audio[0]);
    memset(camera_pattern,0,sizeof(int)*length_of_camera_audio[0]);
    audio_pattern = (int*)malloc(sizeof(int)*length_of_camera_audio[1]);
    memset(audio_pattern,0,sizeof(int)*length_of_camera_audio[1]);
    filter = (int*)malloc(sizeof(int)*(length_of_camera_audio[0]+length_of_camera_audio[1]));
    //disorder the array
    /*
    srand(1);
    for(int i=0;i<length_of_camera_audio[0];i++){
        swap(((size_t*)addr[1]+i),((size_t*)addr[1]+rand()%length_of_camera_audio[0]));
    }
    for(int i=0;i<length_of_camera_audio[1];i++){
        swap(((size_t*)addr[2]+i),((size_t*)addr[2]+rand()%length_of_camera_audio[1]));
    }
     */
    flags = (int*)malloc(sum_length*sizeof(int));
    memset(flags,0,sum_length*sizeof(int));
    LOGD("Finish Initializtion");
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_SMU_DevSec_SideChannelJob_pause(JNIEnv *env, jobject thiz) {
    // to stop scanning;
    continueRun=0;
}

