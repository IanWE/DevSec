/*
This file contains all tools functions for android, 
such as Java_com_SMU_DevSec_CacheScan_GetPattern to get the activated pattern etc.
*/
#include <jni.h>
#include <ostream>
#include <pthread.h>
#include <libflush/hit.h>
#include <logoutput.h>
#include <vector>

//#include "native-lib.cpp"
extern int finishtrial1;
extern jint* filter;
extern int t[];

extern int* flags;
extern size_t *addr;
extern int compiler_position;
extern int sum_length;
extern pthread_mutex_t g_lock;
extern uint64_t threshold;
//for compiler
extern int log_length;
extern int logs[100000];
extern long times[100000];
extern int thresholds[100000];//the extern definition have to be the same with the original
extern int pausescan;

extern int length_of_camera_audio[2];
extern int* camera_pattern;
extern int* audio_pattern;
int length = 0;
int printpattern[] = {0,0};
extern std::vector<std::string> camera_list;
extern std::vector<std::string> audio_list;
extern int running;

extern "C" JNIEXPORT jintArray JNICALL
        Java_com_SMU_DevSec_CacheScan_CacheCheck(JNIEnv *env, jobject thiz){
        jintArray ja = env->NewIntArray(compiler_position);
        jint *arr = env->GetIntArrayElements(ja, NULL);
        if(flags!=NULL) {
            pthread_mutex_lock(&g_lock);
            memcpy(arr,flags,sizeof(int)*(compiler_position-1));//no need to retrieve the compiler activation
            pthread_mutex_unlock(&g_lock);
            env->ReleaseIntArrayElements(ja, arr, 0);
            return ja;
        }
        return NULL;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_GetT(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(2);
    jint *arr = env->GetIntArrayElements(ja, NULL);
    memcpy(arr,t,sizeof(int)*2);
    env->ReleaseIntArrayElements(ja, arr, 0);
    return ja;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_GetPattern(JNIEnv *env, jobject thiz, jint c){
    int length = length_of_camera_audio[c-1];
    jintArray ja = env->NewIntArray(length);
    jint *arr = env->GetIntArrayElements(ja, NULL);
    if (audio_pattern != NULL) {//audio is after camera
        int *t = (c==1?camera_pattern:audio_pattern);
        if(printpattern[c-1]==0) {
            LOGD("=======================================");
            for (int i = 0; i < length_of_camera_audio[c-1]; i++) {//camera
                if (t[i] == 1) {
                    if (c == 1)
                        LOGD("*** Camera Pattern %s ***", camera_list[i].c_str());
                    else
                        LOGD("*** Audio Pattern %s ***", audio_list[i].c_str());
                }
            }
            //printpattern[c-1]++;
            LOGD("=======================================");
        }
        pthread_mutex_lock(&g_lock);
        memcpy(arr, t, sizeof(int) * length);
        pthread_mutex_unlock(&g_lock);
        env->ReleaseIntArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_CacheScan_ClearPattern(JNIEnv *env, jobject thiz,jint c){
    pthread_mutex_lock(&g_lock);
    if(c==1)
        memset(camera_pattern,0,length_of_camera_audio[0]*sizeof(int));//clear the pattern
    if(c==2)
        memset(audio_pattern,0,length_of_camera_audio[1]*sizeof(int));//clear the pattern
    if(c==3) {
        memset(camera_pattern, 0, length_of_camera_audio[0] * sizeof(int));//clear the pattern
        memset(audio_pattern, 0, length_of_camera_audio[1] * sizeof(int));//clear the pattern
    }
    pthread_mutex_unlock(&g_lock);
}


extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_addr(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(compiler_position);
    jint *arr = env->GetIntArrayElements(ja, NULL);
    if(addr!=NULL) {
        memcpy(arr,addr,sizeof(size_t)*compiler_position);
        env->ReleaseIntArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}


//functions to get logs and times
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_SMU_DevSec_CacheScan_GetTimes(JNIEnv *env, jobject thiz){
    length = log_length;
    //LOGD("xxxxxxxxxxxxxxxxxxxxxxx Loglength %d",length);
    if(length-1>0) {
        jlongArray ja = env->NewLongArray(length);
        jlong *arr = env->GetLongArrayElements(ja, NULL);
        memcpy(arr,times,sizeof(long)*length);
        env->ReleaseLongArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_GetThresholds(JNIEnv *env, jobject thiz){
    if(length!=0) {
        jintArray ja = env->NewIntArray(length);
        jint *arr = env->GetIntArrayElements(ja, NULL);
        memcpy(arr,thresholds,sizeof(int)*length);
        env->ReleaseIntArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}

//do not check the length because it only called after GetTimes.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_GetLogs(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(length);
    jint *arr = env->GetIntArrayElements(ja, NULL);
    memcpy(arr,logs,sizeof(int)*length);
    env->ReleaseIntArrayElements(ja, arr, 0);
    pthread_mutex_lock(&g_lock);
    log_length = 0;
    pthread_mutex_unlock(&g_lock);
    return ja;
}

/*
extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_thd(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(3);
    jint *arr = env->GetIntArrayElements(ja, NULL);
    if(thd[0]!=0) {
        for (int i = 0; i < 3; i++) {
            arr[i] = thd[i];
        }
        env->ReleaseIntArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}
*/
extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_CacheScan_HandleCapture(JNIEnv *env, jobject thiz, jint i){
    pthread_mutex_lock(&g_lock);
    flags[i] = 0;
    pthread_mutex_unlock(&g_lock);
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_CacheScan_increase(JNIEnv *env, jobject thiz){
    threshold += 5;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_CacheScan_decrease(JNIEnv *env, jobject thiz){
    threshold -= 5;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_CacheScan_setthreshold(JNIEnv *env, jobject thiz,jint new_thresh){
    threshold = new_thresh;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_SMU_DevSec_CacheScan_getthreshold(JNIEnv *env, jobject thiz){
    return threshold;
}

/*
extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_CacheScan_filteraddr(JNIEnv *env, jobject thiz, jint index){
    pthread_mutex_lock(&g_lock);
    addr[index] = 0;
    pthread_mutex_unlock(&g_lock);
}
 */

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_TrialModelStages_getFilter(JNIEnv *env, jclass clazz) {
    // TODO: implement getFilter()
    finishtrial1 = 1;//stop scan
    int length_function = length_of_camera_audio[0]+length_of_camera_audio[1];
    jintArray ja = env->NewIntArray(length_function);
    jint* arr = env->GetIntArrayElements(ja, NULL);
    memcpy(arr,filter,sizeof(int)*length_function);
    env->ReleaseIntArrayElements(ja, arr, 0);
    return ja;
}

/*
extern "C"
JNIEXPORT void JNICALL
Java_com_SMU_DevSec_TrialModelStages_flush(JNIEnv *env, jclass clazz,jint c) {
    flush_address(c,length_of_camera_audio[c-1]);
}
 */

extern "C" JNIEXPORT jint JNICALL
Java_com_SMU_DevSec_MainActivity_isrunning(JNIEnv *env, jobject thiz){
    return running;
}
