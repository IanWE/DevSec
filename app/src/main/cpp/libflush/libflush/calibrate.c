 /* See LICENSE file for license and copyright information */

#include <inttypes.h>
#include <sched.h>
#include <unistd.h>
#include "libflush.h"

#include "calibrate.h"
#include "logoutput.h"

#define MIN(a, b) ((a) > (b)) ? (b) : (a)

#define log_err(fmt,args...) __android_log_print(ANDROID_LOG_ERROR, TAG_NAME, (const char *) fmt, ##args)
int calibrate(libflush_session_t* libflush_session)
{
  char buffer[4096] = {0};
  void* address = &buffer[1024];
  //void* address = m;
  size_t min_cache = 300;
  size_t max_cache = 0;
  size_t min_mem = 300;
  size_t max_mem = 0;

  // Measure time it takes to access something from the cache
  size_t hit_histogram[CALIBRATION_HISTOGRAM_SIZE] = {0}; 
  //libflush_access_memory(address);
  libflush_reload_address(libflush_session, address);

  for (unsigned int i = 0; i < CALIBRATION_HISTOGRAM_ENTRIES; i++) { //entries = 100000
      uint64_t time = libflush_reload_address(libflush_session, address);
      if(max_cache<time)
	      max_cache = time;
      //hit_histogram[MIN(CALIBRATION_HISTOGRAM_SIZE - 1, time / CALIBRATION_HISTOGRAM_SCALE)]++; //scale = 5 , size = 200 //scale are used to scale the number
      hit_histogram[MIN(CALIBRATION_HISTOGRAM_SIZE, time / CALIBRATION_HISTOGRAM_SCALE)]++; //scale = 5 , size = 200 //scale are used to scale the number
      sched_yield();
  }
  // Measure time it takes to access something from memory
  size_t miss_histogram[CALIBRATION_HISTOGRAM_SIZE] = {0};
  for (unsigned int i = 0; i < CALIBRATION_HISTOGRAM_ENTRIES; i++) {
      uint64_t time = libflush_reload_address_and_flush(libflush_session, address);  // flush is available here
      if(max_mem<time)
	      max_mem = time;
      //miss_histogram[MIN(CALIBRATION_HISTOGRAM_SIZE - 1, time / CALIBRATION_HISTOGRAM_SCALE)]++; //SIZE is used to cap the maximun;
      miss_histogram[MIN(CALIBRATION_HISTOGRAM_SIZE, time / CALIBRATION_HISTOGRAM_SCALE)]++; //SIZE is used to cap the maximun;
      sched_yield();
  }

  // Get the maximum value of a cache hit and the minimum value of a cache miss
  size_t hit_maximum_index = 0;
  size_t hit_maximum = 0;

  size_t miss_minimum_index = 0;
  size_t miss_maximum = 0;
  size_t miss_maximum_index = 0;

  for (int i = 0; i < CALIBRATION_HISTOGRAM_SIZE; i++) {
      usleep(100);
      LOGD("index %d, Hit %d, Miss %d", i,hit_histogram[i],miss_histogram[i]);
      //log_err("index %d, Hit %d, Miss %d", i,hit_histogram[i],miss_histogram[i]);
      if (hit_maximum < hit_histogram[i]) {
          hit_maximum = hit_histogram[i];
          hit_maximum_index = i;
      }
      if (miss_maximum < miss_histogram[i]) {
          miss_maximum = miss_histogram[i];
          miss_maximum_index = i;
      }

      if (miss_histogram[i] > CALIBRATION_HISTOGRAM_THRESHOLD && miss_minimum_index == 0) {
          miss_minimum_index = i;
      }
  }
  int temp1 = 0;
  int temp2 = 0;
  for(int i = 0; i < CALIBRATION_HISTOGRAM_SIZE; i++) {
      if(temp1<=93000){
	    temp1 += hit_histogram[i];
      	hit_maximum_index = i;
      }
      if(temp2<1000)
        temp2 += miss_histogram[i];
	    miss_maximum_index = i;
  }
  int cache = (hit_maximum_index+1) * CALIBRATION_HISTOGRAM_SCALE;
  int mem = (miss_maximum_index+1) * CALIBRATION_HISTOGRAM_SCALE;
  int threshold = mem - (mem - cache) / 2;
  LOGD("cache %d, mem %d, max cache %d, max mem %d",cache,mem,max_cache,max_mem);
  if(hit_maximum_index>miss_maximum_index)
      return 9999;
  return cache;
}
