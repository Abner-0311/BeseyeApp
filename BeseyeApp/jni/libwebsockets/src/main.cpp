/*
 * Copyright 2011 - Churn Labs, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This is mostly based off of the FFMPEG tutorial:
 * http://dranger.com/ffmpeg/
 * With a few updates to support Android output mechanisms and to update
 * places where the APIs have shifted.
 */
#ifndef __JAVA_EXPORT__
#define __JAVA_EXPORT__

#ifdef __cplusplus
 extern "C" {

#include "websocket_mgr.h"

#endif

#endif

JNIEXPORT jint JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_swTest(JNIEnv * env, jobject thisObj){
	websocket_test();
 	return 0;
 }

//static jclass cls;
//static jmethodID  s_getBitmapMethod, s_drawStreamBitmapMethod, s_rtmpStatusCBMethod, s_rtmpErrorCBMethod/*, s_updateBitmapMethod = NULL*/;
//
///* Main activity */
//static jclass mActivityClass = NULL;
//static jobject mThisObj= NULL;
///* method signatures */
//static jmethodID midAudioInit= NULL;
//static jmethodID midAudioGetBufSize= NULL;
//static jmethodID midAudioWriteShortBuffer= NULL;
//static jmethodID midAudioWriteByteBuffer= NULL;
//static jmethodID midAudioQuit= NULL;

//JNIEXPORT jboolean JNICALL Java_com_app_beseye_CameraViewActivity_nativeClassInit(JNIEnv *env, jclass clss)
//{
//	LOGE("nativeClassInit()+");
////	if (JNI_OK != env->GetJavaVM(&jvm))
////	{
////		LOGE("GetJavaVM failed");
////		return 0;
////	}
////
////	cls = env->FindClass("com/app/beseye/CameraViewActivity");
////	if(NULL == cls){
////		LOGE("cls is empty");
////		return 0;
////	}
////
////	s_getBitmapMethod = env->GetMethodID(cls, "getBitmapBySize", "(II)Landroid/graphics/Bitmap;");
////	if(NULL == s_getBitmapMethod){
////		LOGE("s_getBitmapMethod is empty");
////		return 0;
////	}
////
//////	env->NewGlobalRef(s_getBitmapMethod);
////
////	s_drawStreamBitmapMethod = env->GetMethodID(cls, "drawStreamBitmap", "()V");
////	if(NULL == s_drawStreamBitmapMethod){
////		LOGE("s_drawStreamBitmapMethod is empty");
////		return 0;
////	}
////
////	s_rtmpStatusCBMethod = env->GetMethodID(cls, "updateRTMPStatus", "(ILjava/lang/String;)V");
////	if(NULL == s_rtmpStatusCBMethod){
////		LOGE("s_rtmpStatusCBMethod is empty");
////		return 0;
////	}
////
////	s_rtmpErrorCBMethod = env->GetMethodID(cls, "updateRTMPErrorCallback", "(IILjava/lang/String;)V");
////	if(NULL == s_rtmpErrorCBMethod){
////		LOGE("s_rtmpErrorCBMethod is empty");
////		return 0;
////	}
////
////	mActivityClass = (jclass)(env->NewGlobalRef(clss));
////
////	midAudioInit = env->GetStaticMethodID(mActivityClass,
////								"audioInit", "(IZZI)I");
////
////	midAudioGetBufSize = env->GetStaticMethodID(mActivityClass,
////								"getAudioBufSize", "(I)I");
////
////	midAudioWriteShortBuffer = env->GetStaticMethodID(mActivityClass,
////								"audioWriteShortBuffer", "([S)V");
////
////	midAudioWriteByteBuffer = env->GetStaticMethodID(mActivityClass,
////								"audioWriteByteBuffer", "([B)V");
////
////	midAudioQuit = env->GetStaticMethodID(mActivityClass,
////								"audioQuit", "()V");
//
//
//
//	return 1;
//}

#ifdef __cplusplus
 }
#endif
