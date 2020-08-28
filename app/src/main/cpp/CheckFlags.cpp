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
extern int sum_length;
extern pthread_mutex_t g_lock;
extern int threshold;

extern "C" JNIEXPORT jintArray JNICALL
        Java_com_SMU_DevSec_CacheScan_CacheCheck(JNIEnv *env, jobject thiz){
        jintArray ja = env->NewIntArray(sum_length);;
        jint *arr = env->GetIntArrayElements(ja, NULL);
        int flags_copy[sum_length];

        if(flags!=NULL) {
            pthread_mutex_lock(&g_lock);
            memcpy(flags_copy,flags,sizeof(int)*sum_length);
            pthread_mutex_unlock(&g_lock);
            for (int i = 0; i < sum_length; i++) {
                if (flags_copy[i] != 0) {
                    arr[i] = flags_copy[i];
                } else {
                    arr[i] = 0;
                }
            }
            env->ReleaseIntArrayElements(ja, arr, 0);
            return ja;
        }
        return NULL;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_addr(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(sum_length);;
    jint *arr = env->GetIntArrayElements(ja, NULL);
    int addr_copy[sum_length];
    if(addr!=NULL) {
        memcpy(addr_copy,addr,sizeof(int)*sum_length);
        for (int i = 0; i < sum_length; i++) {
            if (addr_copy[i] != 0) {
                arr[i] = addr_copy[i];
            } else {
                arr[i] = 0;
            }
        }
        env->ReleaseIntArrayElements(ja, arr, 0);
        return ja;
    }
    return NULL;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_SMU_DevSec_CacheScan_thd(JNIEnv *env, jobject thiz){
    jintArray ja = env->NewIntArray(3);;
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

extern "C" JNIEXPORT jint JNICALL
Java_com_SMU_DevSec_CacheScan_getthreshold(JNIEnv *env, jobject thiz){
    return threshold;
}