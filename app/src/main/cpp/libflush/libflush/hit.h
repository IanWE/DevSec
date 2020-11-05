//
// Created by finder on 20-10-16.
//
#ifndef DEVSEC_HIT_H
#define DEVSEC_HIT_H

#ifdef __cplusplus
extern "C" {
#endif
void stage1_(int* arr,size_t threshold, int* length_of_camera_audio, size_t* addr, int* camera_audio, int* finishtrial1,int sum_length);
void flush_address(size_t* address,int length);
int hit(pthread_mutex_t *g_lock, int compiler_position, int *continueRun, int threshold, int *flags,
        long *times, int *thresholds, int *logs, int *log_length, int sum_length,
        size_t *addr, int *camera_pattern, int *audio_pattern, int *length_of_camera_audio);
void stage1(int *arr, size_t threshold, int *length_of_camera_audio, size_t *addr, int *pInt,
            int *pInt1);
int get_threshold();
int
adjust_threshold(int threshold, int* length_of_camera_audio, size_t* addr, int* camera_audio, int* finishtrial1);
#ifdef __cplusplus
}
#endif
#endif //DEVSEC_HIT_H
