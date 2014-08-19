#ifndef __UTILS_H__
#define __UTILS_H__

#include <cmath>
#include <limits>
//#include <string>
//#include <pthread.h>

#ifdef ANDROID
#include <jni.h>
#include <android/log.h>
#define  LOG_TAG    "SoundPairing"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define GEN_TONE_ONLY //for client side

//#define CAM_AUDIO
#define CAM_URL "192.168.3.100"
#else
#include <stdint.h>
#define CAM_AUDIO
#define  LOGD(...)  //printf(__VA_ARGS__)
#define  LOGI(...)  printf(__VA_ARGS__)
#define  LOGW(...)  printf(__VA_ARGS__)
#define  LOGE(...)  fprintf(stderr, __VA_ARGS__)

#define GEN_TONE_ONLY //for client side

#define CAM_URL "127.0.0.1"
#endif

bool sameValue(double a, double b);
bool largeThan(double a, double b);
bool largeEqualThan(double a, double b);
bool lessThan(double a, double b);
bool lessEqualThan(double a, double b);

long getTickCount();

void getTimeSpecByDelay(struct timespec &spec, long lDelayInMS);

#include <sys/time.h>
// Used to measure intervals and absolute times
typedef int64_t msec_t;

typedef unsigned char byte;

// Get current time in milliseconds from the Epoch (Unix)
// or the time the system started (Windows).
msec_t time_ms(void);

//template<typename T, int size>
//int getArrLength(T(&)[size]){return size;}

#ifdef __cplusplus
 extern "C" {


}
#endif

#endif
