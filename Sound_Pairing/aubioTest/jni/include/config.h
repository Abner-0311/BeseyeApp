#ifndef __CONFIG_H__
#define __CONFIG_H__

#ifdef ANDROID
#include <jni.h>
#include <android/log.h>
#define  LOG_TAG    "SoundPairing"

#ifndef LOGD
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif
//#define GEN_TONE_ONLY //for client side

//#define CAM_AUDIO
#define CAM_URL "192.168.2.4"
#else
#include <stdint.h>
#define CAM_AUDIO

#ifndef LOGD
#define  LOGD(fmt, args...)  //printf(__VA_ARGS__)
#define  LOGI(fmt, args...)  printf("[%s][%d][%s()], " fmt, __FILE__, \
        					 __LINE__, __FUNCTION__, ##args);
#define  LOGW(fmt, args...)  printf("[%s][%d][%s()], " fmt, __FILE__, \
        					 __LINE__, __FUNCTION__, ##args);
#define  LOGE(fmt, args...)  fprintf(stderr, "[%s][%d][%s()], " fmt, __FILE__, \
							 __LINE__, __FUNCTION__, ##args);

#endif

#ifndef CAM_ENV
#define GEN_TONE_ONLY //for client side
#endif

#define CAM_URL "127.0.0.1"
#endif

#endif
