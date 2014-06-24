#ifndef __UTILS_H__
#define __UTILS_H__

#include <stdlib.h>
#include <assert.h>
#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define  LOG_TAG    "BesEye"
#ifdef ANDROID
#include <jni.h>
#include <android/log.h>
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#else
#include <unistd.h>
#define  LOGD(...)  av_log(LOG_TAG, AV_LOG_DEBUG, __VA_ARGS__)
#define  LOGI(...)  av_log(LOG_TAG, AV_LOG_INFO, __VA_ARGS__)
#define  LOGW(...)  av_log(LOG_TAG, AV_LOG_WARNING, __VA_ARGS__)
#define  LOGE(...)  av_log(LOG_TAG, AV_LOG_ERROR, __VA_ARGS__)
#endif //ANDROID

long getTickCount();

//void getTimeSpecByDelay(struct timespec &spec, long lDelayInMS);

#include <sys/time.h>
// Used to measure intervals and absolute times
typedef int64_t msec_t;

typedef unsigned char byte;

// Get current time in milliseconds from the Epoch (Unix)
// or the time the system started (Windows).
msec_t time_ms(void);

#ifdef __cplusplus
 extern "C" {

#include "libavutil/log.h"
#include <librtmp/log.h>

#define STRMATCH(a1,a2)	(NULL != a1 && NULL != a2 && strlen(a1) == strlen(a2) && !strncmp(a1,a2,strlen(a1)))
}
#endif//__cplusplus

#endif//__UTILS_H__
