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

#define DEBUG_PRINT(fmt, args...) \
        do { if (DEBUG_TEST) fprintf(stderr, "%s:%d:%s(): " fmt, __FILE__, \
                                __LINE__, __FUNCTION__, ##args); } while (0)

#ifdef ANDROID
#define  LOG_TAG    "BesEye"
#include <jni.h>
#include <android/log.h>
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#define  LOGD(...)  //printf(__VA_ARGS__)
#define  LOGI(...)  printf(__VA_ARGS__)
#define  LOGW(...)  printf(__VA_ARGS__)
#define  LOGE(...)  fprintf(stderr, __VA_ARGS__)
#endif

typedef BOOLEAN BOOL;
#ifndef FALSE
#define FALSE 0
#define TRUE 1
#endif

#ifdef __cplusplus
}
#endif
#endif /* _COMMON_H_ */
