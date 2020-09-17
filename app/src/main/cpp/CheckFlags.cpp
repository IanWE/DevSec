//
// Created by Jianwen on 20-8-16.
//
#include<jni.h>
#include <ostream>
#include <pthread.h>
//#include "native-lib.cpp"

extern int* flags;
extern size_t* addr;
extern int thd[3];
extern int compiler_position;
extern int sum_length;
extern pthread_mutex_t g_lock;
extern int threshold;
//for compiler
extern int log_length;
extern int logs[300000];
extern long times[300000];
extern int thresholds[300000];

int length = 0;
extern "C" JNIEXPORT jintArray JNICALL
        Java_com_SMU_DevSec_CacheScan_CacheCheck(JNIEnv *env, jobject thiz){
        jintArray ja = env->NewIntArray(compiler_position);
        jint *arr = env->GetIntArrayElements(ja, NULL);
        if(flags!=NULL) {
            pthread_mutex_lock(&g_lock);
            memcpy(arr,flags,sizeof(int)*compiler_position);
            pthread_mutex_unlock(&g_lock);
            env->ReleaseIntArrayElements(ja, arr, 0);
            return ja;
        }
        return NULL;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_addr(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(sum_length);
    jint *arr = env->GetIntArrayElements(ja, NULL);
    if(addr!=NULL) {
        memcpy(arr,addr,sizeof(int)*sum_length);
        env->ReleaseIntArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}

//functions to get logs and times
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_SMU_DevSec_CacheScan_GetTimes(JNIEnv *env, jobject thiz){
    length = log_length;
    if(length!=0) {
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