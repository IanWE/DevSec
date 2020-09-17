#include <android/log.h>

#define TAG_NAME "ARMAGEDDON" // 这个是自定义的LOG的标识
/*
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型
*/
#define LOGI(fmt,args...) __android_log_print(ANDROID_LOG_INFO, TAG_NAME, (const char *) fmt, ##args)
#define LOGE(fmt,args...) __android_log_print(ANDROID_LOG_ERROR, TAG_NAME, (const char *) fmt, ##args)
#define LOGD(fmt,args...) __android_log_print(ANDROID_LOG_DEBUG, TAG_NAME, (const char *) fmt, ##args)
