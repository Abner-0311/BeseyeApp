/*****************************************************************************
 * Common.h  common head file 
 * 
 *   COPYRIGHT (C) 2014 BesEye Co. 
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    01/22/2014 - Chris Hsin - Created.
 *
 *****************************************************************************/
#ifndef _COMMON_H_
#define _COMMON_H_
#ifdef __cplusplus
extern "C" {
#endif
typedef   unsigned char		U8;
typedef   char            	S8;
typedef   unsigned short    U16;
typedef   short    			S16;
typedef   unsigned int    	U32;
typedef   int    			S32;
typedef   unsigned char   	BOOLEAN;

enum BoolVal{
  BFALSE,
  BTRUE,
};

enum EnableVal{
  DISABLE,
  ENABLE,
};

enum CAM_HANDL{
	CAM_HANDL_OK=0,
	CAM_HANDL_HTTP_CGI_GET_SESSION_FAIL,
	CAM_HANDL_HTTP_CGI_GET_SESSION_NOT_EXIST,
	CAM_HANDL_SOCKET_CREATE_FAIL,
	CAM_HANDL_SOCKET_ACCEPT_FAIL,
	CAM_HANDL_CREATE_THREAD_FAIL,
};

S8* sbGetCurSession(void);

#ifdef DEBUG
#define DEBUG_TEST 1
#else
#define DEBUG_TEST 0
#endif

#define URL_SIZE 256
#define BUF_SIZE 10*1024
#define SESSION_SIZE 64

#ifdef __APPLE__
#define GEN_TONE_ONLY //for client side
#endif

#define DEBUG_PRINT(fmt, args...) \
        do { if (DEBUG_TEST) fprintf(stderr, "%s:%d:%s(): " fmt, __FILE__, \
                                __LINE__, __FUNCTION__, ##args); } while (0)

#ifdef ANDROID
#define  LOG_TAG    "BesEye"
#include <jni.h>
#include <android/log.h>
#define CAM_URL "192.168.2.85"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include "error.h"
#define CAM_URL "127.0.0.1"
#define  LOGD(fmt, args...)  //printf(__VA_ARGS__)
#define  LOGI(fmt, args...)  printf("[%s][%d][%s()], " fmt, __FILE__, \
        					 __LINE__, __FUNCTION__, ##args);
#define  LOGW(fmt, args...)  printf("[%s][%d][%s()], " fmt, __FILE__, \
        					 __LINE__, __FUNCTION__, ##args);
#define  LOGE(fmt, args...)  fprintf(stderr, "[%s][%d][%s()], " fmt, __FILE__, \
							 __LINE__, __FUNCTION__, ##args);
#endif


typedef BOOLEAN BOOL;
#ifndef FALSE
#define FALSE 0
#define TRUE 1
#endif

#define DEF_VCAM_ID "Bes0001"
#define DEF_UA "Beseye_Cam_Sw"

#define CAM_WS_PORT 5432
#define CAM_BE_HOST "http://54.199.158.71:80"
#define NETWORK_CHECK_HOST "www.google.com"

#define CAM_TIME_TRACE 0
#define CAM_CORRECTION_TRACE 0

#define FREE(obj) \
	if(obj){ \
		free(obj); \
		obj = NULL; \
	}

#ifdef __cplusplus
}
#endif
#endif /* _COMMON_H_ */
