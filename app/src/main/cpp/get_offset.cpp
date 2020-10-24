#include <string.h>
#include <dlfcn.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>

#define TAG "ARMAGEDDON" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

size_t get_offset(const char* range, const char* offset, size_t *addrs, char* func_list[], size_t length) {
    static size_t current_length = 0;
    //void* handle = dlopen(module_path, RTLD_NOW);
    void* start = NULL;
    void* end   = NULL;
    if (!sscanf(range, "%p-%p", &start, &end)) {
        LOGE("Could not parse range parameter(range): %s\n", range);
        exit(0);
    }
    size_t ofs;
    if(!sscanf(offset, "%zx", &ofs)){
        LOGE("Could not parse range parameter(ofs): %s\n", offset);
        exit(0);
    }
    ofs = ofs & ~(0x3F);//clean
    start = (void *) ((size_t) start-ofs);
    for (int i = 0; i < length; i++) {
        //LOGE("Could not parse range %s",func_list[i]);
        sscanf(func_list[i], "%zx", &addrs[current_length]);
        if((size_t)start+addrs[current_length]>(size_t) end && addrs[current_length]>=0) {
            LOGD("Addr %p is larger than %p", addrs[current_length]+(size_t)start,end);
            current_length--;
            break;
        }
        addrs[current_length] = addrs[current_length] + (size_t) start;
        current_length++;
    }
    return current_length;
}
