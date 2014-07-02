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

#include <pthread.h>

#ifdef __cplusplus
 extern "C" {

#include "utils.h"
#include "ffmpeg_ext.h"
#include "beseyeplayer.h"
#include "beseye_audio_streamer.h"

#include "SDL_config.h"
/* Include the SDL main definition header */
#include "SDL_main.h"

#ifdef ANDROID
#include <android/native_window_jni.h>
#include <android/bitmap.h>
#endif

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavformat/url.h>

#endif

#endif

//static jmethodID  playMethod, getBufMethod, getBufSizeMethod = NULL;
//static jmethodID  recordMethod, endLoopMethod = NULL;

static JavaVM*   jvm = NULL;

extern void writeBufToPipe();
extern void endLoop();

#define DECLARE_JNIENV_WITHOUT_RETURN() \
	JNIEnv* jni_env; \
	if (JNI_OK != jvm->AttachCurrentThread(&jni_env, NULL)) {  return; } \

#define DECLARE_JNIENV_WITH_RETURN() \
	JNIEnv* jni_env; \
	if (JNI_OK != jvm->AttachCurrentThread(&jni_env, NULL)) {  return 0; } \

void rtmp_log_internal(int level, const char *fmt, va_list args){
	if(NULL != fmt && level <=2)
		LOGE(fmt, args);
}

void rtmp_log_internal2(int level, const char *msg){
	if(NULL != msg && level <=RTMP_LOGINFO)
		LOGE(msg);
}

void ffmpeg_vlog(void* avcl, int level, const char *fmt, va_list vl){
	if(NULL != fmt && level <=AV_LOG_INFO)
		LOGE(fmt, vl);
}

void ffmpeg_vlog2(void* avcl, int level, const char *msg){
	if(NULL != msg && level <=AV_LOG_INFO)
		LOGE(msg);
}


JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_startRecord(JNIEnv * env, jobject obj, int fd){
	int iRet = 0;
	LOGE("startRecord()++++++++++++++++++++++++++");
	/*if(NULL == audioStreamer)*/{
//		CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
//		if(CBeseyeAudioStreamer::checkExit() && audioStreamer.setStreamingInfo("rtmp://192.168.2.224:1935/myapp/audiostream", "/data/data/com.churnlabs.ffmpegsample/beseye.fifo")){
//			iRet = audioStreamer.startAudioStreaming();
//		}
	}

	LOGE("startRecord()-");
	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_isRecording(JNIEnv * env, jobject obj){
	int iRet = 0;

//	CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
//	LOGW("isRecording(), status:%d",audioStreamer.get_Stream_Status());
//	iRet = audioStreamer.isStreamingPlaying();

	//LOGE("isRecording()-");
	return iRet;
}

JNIEXPORT void JNICALL Java_com_app_beseye_CameraViewActivity_recordAudio(JNIEnv * env, jobject obj, jbyteArray array, int iBufSize)
{
//	DECLARE_JNIENV_WITHOUT_RETURN()
//	CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
//	jbyte *bytes = jni_env->GetByteArrayElements(array, NULL);
//	audioStreamer.writeAudioBuffer((char*)bytes, iBufSize);
//	jni_env->ReleaseByteArrayElements( array, bytes, 0);
    //LOGE("recordAudio()-");
}

JNIEXPORT void JNICALL Java_com_app_beseye_CameraViewActivity_endRecord(JNIEnv * env, jobject obj){

//	CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
//	audioStreamer.closeAudioStreaming();
//	LOGE("endRecord()-");
}

static jclass cls;
static jmethodID  s_getBitmapMethod, s_drawStreamBitmapMethod, s_rtmpStatusCBMethod, s_rtmpErrorCBMethod, s_rtmpClockCBMethod/*, s_updateBitmapMethod = NULL*/;

/* Main activity */
static jclass mActivityClass = NULL;
static jobject mThisObj= NULL;
/* method signatures */
static jmethodID midAudioInit= NULL;
static jmethodID midAudioGetBufSize= NULL;
static jmethodID midAudioWriteShortBuffer= NULL;
static jmethodID midAudioWriteByteBuffer= NULL;
static jmethodID midAudioQuit= NULL;

JNIEXPORT jboolean JNICALL Java_com_app_beseye_CameraViewActivity_nativeClassInit(JNIEnv *env, jclass clss)
{
	LOGE("nativeClassInit()+");
	if (JNI_OK != env->GetJavaVM(&jvm))
	{
		LOGE("GetJavaVM failed");
		return 0;
	}

	cls = env->FindClass("com/app/beseye/CameraViewActivity");
	if(NULL == cls){
		LOGE("cls is empty");
		return 0;
	}

	s_getBitmapMethod = env->GetMethodID(cls, "getBitmapBySize", "(II)Landroid/graphics/Bitmap;");
	if(NULL == s_getBitmapMethod){
		LOGE("s_getBitmapMethod is empty");
		return 0;
	}

//	env->NewGlobalRef(s_getBitmapMethod);

	s_drawStreamBitmapMethod = env->GetMethodID(cls, "drawStreamBitmap", "()V");
	if(NULL == s_drawStreamBitmapMethod){
		LOGE("s_drawStreamBitmapMethod is empty");
		return 0;
	}

	s_rtmpStatusCBMethod = env->GetMethodID(cls, "updateRTMPStatus", "(ILjava/lang/String;)V");
	if(NULL == s_rtmpStatusCBMethod){
		LOGE("s_rtmpStatusCBMethod is empty");
		return 0;
	}

	s_rtmpErrorCBMethod = env->GetMethodID(cls, "updateRTMPErrorCallback", "(IILjava/lang/String;)V");
	if(NULL == s_rtmpErrorCBMethod){
		LOGE("s_rtmpErrorCBMethod is empty");
		return 0;
	}

	s_rtmpClockCBMethod= env->GetMethodID(cls, "updateRTMPClockCallback", "(I)V");
	if(NULL == s_rtmpClockCBMethod){
		LOGE("s_rtmpClockCBMethod is empty");
		return 0;
	}

	mActivityClass = (jclass)(env->NewGlobalRef(clss));

	midAudioInit = env->GetStaticMethodID(mActivityClass,
								"audioInit", "(IZZI)I");

	midAudioGetBufSize = env->GetStaticMethodID(mActivityClass,
								"getAudioBufSize", "(I)I");

	midAudioWriteShortBuffer = env->GetStaticMethodID(mActivityClass,
								"audioWriteShortBuffer", "([S)V");

	midAudioWriteByteBuffer = env->GetStaticMethodID(mActivityClass,
								"audioWriteByteBuffer", "([B)V");

	midAudioQuit = env->GetStaticMethodID(mActivityClass,
								"audioQuit", "()V");

//	s_updateBitmapMethod = env->GetStaticMethodID(cls, "updateBufferBySize", "([BII)V");
//	if(NULL == s_updateBitmapMethod){
//		LOGE("s_updateBitmapMethod is empty");
//		return 0;
//	}

//	env->NewGlobalRef(s_drawStreamBitmapMethod);


//	playMethod = env->GetMethodID(cls, "playSound", "(I)V");
//	if(NULL == playMethod){
//		LOGE("playMethod is empty");
//		return 0;
//	}
//
//	getBufMethod = env->GetMethodID(cls, "getRecordSoundBuf", "()[B");
//	if(NULL == getBufMethod){
//		LOGE("getBufMethod is empty");
//		return 0;
//	}
//
//	getBufSizeMethod = env->GetMethodID( cls, "getRecordSampleRead", "()I");
//	if(NULL == getBufSizeMethod){
//		LOGE("getBufMethod is empty");
//		return 0;
//	}
//
//	endLoopMethod = env->GetMethodID( cls, "stopFeedPipe", "()V");
//	if(NULL == endLoopMethod){
//		LOGE("endLoopMethod is empty");
//		return 0;
//	}

	//LOGE("nativeClassInit()-, playMethod:%d",playMethod);
	LOGE("nativeClassInit()-");

	av_log_set_level(AV_LOG_DEBUG);
	av_log_set_callback2(ffmpeg_vlog2);


	RTMP_LogSetLevel(RTMP_LOGINFO);
//	//RTMP_LogSetCallback(rtmp_log_internal);
	RTMP_LogSetCallback2(rtmp_log_internal2);

	return 1;
}

/* Called before SDL_main() to initialize JNI bindings in SDL library */
extern void SDL_Android_Init(JNIEnv* env, jclass cls);

/* Start up the SDL app */
void JNICALL Java_com_app_beseye_CameraViewActivity_nativeInit(JNIEnv* env, jclass cls, jobject obj)
{
    /* This interface could expand with ABI negotiation, calbacks, etc. */
    SDL_Android_Init(env, cls);

    SDL_SetMainReady();

    /* Run the application code! */
    int status;
    char *argv[2];
    argv[0] = SDL_strdup("SDL_app");
    argv[1] = NULL;
    status = SDL_main(1, argv);

    /* Do not issue an exit or the whole application will terminate instead of just the SDL thread */
    /* exit(status); */
}

int iCurStreamIdx = 0;

const int MAX_STREAM_COUNT = 10;
static jobject jni_host = NULL;

CBeseyePlayer* player[MAX_STREAM_COUNT] ={0};

void videoCallback(void* anw, uint8_t* srcbuf, uint_t iFormat, uint_t linesize, uint_t iWidth, uint_t iHeight){
//	av_log(NULL, AV_LOG_ERROR, "videoCallback, format:%d, %d, %d, %d\n", iFormat, linesize, iWidth, iHeight);

//	DECLARE_JNIENV_WITHOUT_RETURN()
//
//	int iSize = iWidth*iHeight*2;
//	jbyteArray result = jni_env->NewByteArray(iSize);
//	jni_env->SetByteArrayRegion(result, 0, iSize, (jbyte *)srcbuf);
//
//	jni_env->CallStaticObjectMethod(cls, s_updateBitmapMethod, result, iWidth, iHeight);
//
//	//jni_env->ReleaseByteArrayElements(result, (jbyte *)srcbuf, 0);
//
//	jni_env->DeleteLocalRef(result);

	//LOGI("Java_com_app_beseye_CameraViewActivity_drawBufferToBitmap()");

	DECLARE_JNIENV_WITHOUT_RETURN()
	int iRet = 0;
	AndroidBitmapInfo  info;
	void*              pixels = NULL;
	int                ret = -1;
	jobject bitmap = (jobject)anw;

	//LOGI("Java_com_app_beseye_CameraViewActivity_drawBufferToBitmap()1");
	if ((ret = AndroidBitmap_getInfo(jni_env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		goto $ERR;
	}
	//LOGE("Checked on the bitmap");

	//LOGI("Java_com_app_beseye_CameraViewActivity_drawBufferToBitmap()2");
	if ((ret = AndroidBitmap_lockPixels(jni_env, bitmap, &pixels)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		goto $ERR;
	}

	if(pixels)
		memcpy(pixels, srcbuf, linesize*iHeight);

	if(s_drawStreamBitmapMethod)
		jni_env->CallVoidMethod(jni_host, s_drawStreamBitmapMethod);
$ERR:

	//LOGI("Java_com_app_beseye_CameraViewActivity_drawBufferToBitmap()3");
	if(NULL != pixels)
		AndroidBitmap_unlockPixels(jni_env, bitmap);

	//LOGI("Java_com_app_beseye_CameraViewActivity_drawBufferToBitmap()-");

	jvm->DetachCurrentThread();
}

void videoDeinitCallback(void* anw){
//	ANativeWindow* window = (ANativeWindow*)anw;
//	if(window)
//		ANativeWindow_release(window);
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(anw){
		jni_env->DeleteGlobalRef((jobject)anw);
	}
}

jstring str2jstring(const char * msg) {
	DECLARE_JNIENV_WITH_RETURN()
	jstring ret = (msg)?(jstring)jni_env->NewStringUTF(msg):NULL;
//	if(ret)
//		jni_env->DeleteLocalRef(ret);
    return ret;
}

void rtmpStreamStatusCb(CBeseyeRTMPObserver * obj, CBeseyeRTMPObserver::Player_Callback cbType, const char * msg, int iMajorType, int iMinorType){
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(obj == player[0] || obj == player[1]){
    	//LOGI("rtmpStreamStatusCb(), %s cbType:%d, msg:%s, iMajorType:%d",(obj == player[0] ?"player1":"player2"),cbType, (msg?msg:""),iMajorType);
    	if(jni_host){
    		 if(cbType == CBeseyeRTMPObserver::STREAM_STATUS_CB){
				if(s_rtmpStatusCBMethod && jni_host)
					jni_env->CallVoidMethod(jni_host, s_rtmpStatusCBMethod, iMajorType, str2jstring(msg));

			}else if(cbType == CBeseyeRTMPObserver::ERROR_CB){
				if(s_rtmpErrorCBMethod && jni_host)
					 jni_env->CallVoidMethod(jni_host, s_rtmpErrorCBMethod, iMajorType, iMinorType, str2jstring(msg));
			}else if(cbType == CBeseyeRTMPObserver::STREAM_CLOCK_CB){
				if(s_rtmpClockCBMethod && jni_host)
					jni_env->CallVoidMethod(jni_host, s_rtmpClockCBMethod, iMajorType);
			}
    	}else{
    		LOGE("rtmpStreamStatusCb(), NULL jni_host");
    	}

    }/*else if(obj == &CBeseyeAudioStreamer::getInstance()){
    	LOGI("rtmpStreamStatusCb(), audio Streamer cbType:%d, msg:%s, iStatusType:%d",cbType, (msg?msg:""),iMajorType);
    }*/

	//jvm->DetachCurrentThread();
}

void* getWindowByHolder(void* holder, uint32_t iWidth, uint32_t iHeight){
	LOGE("getWindowByHolder(), [%d, %d]", iWidth, iHeight);
	DECLARE_JNIENV_WITH_RETURN()
	jobject jni_host = jni_env->NewGlobalRef((jobject)holder);
	jobject bitmap = jni_env->CallObjectMethod(jni_host, s_getBitmapMethod, iWidth, iHeight);
	jobject g_bitmap = jni_env->NewGlobalRef(bitmap);

	jni_env->DeleteGlobalRef(jni_host);
	return g_bitmap;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_openStreaming(JNIEnv * env, jobject obj, int iStreamIdx, jobject surface, jstring path, int iSeekOffset)
{
	DECLARE_JNIENV_WITH_RETURN()

	//jni_host = obj;
	int iRet = -1;

	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT){
		if(NULL == player[iStreamIdx]){
			jni_host = jni_env->NewGlobalRef(obj);
			char *nativeString = NULL;
			//ANativeWindow* anw = ANativeWindow_fromSurface(env, surface);

			int surface_width = 1280;//NULL != anw ?ANativeWindow_getWidth(anw):0;
			int surface_height = 720;//NULL != anw ?ANativeWindow_getHeight(anw):0;

			//LOGI("anw is [%d]", anw);

//			jobject bitmap = jni_env->CallObjectMethod(jni_host, s_getBitmapMethod, surface_width, surface_height);
//			jobject g_bitmap = jni_env->NewGlobalRef(bitmap);
			iRet = iStreamIdx;
			LOGE("openStreaming(), iIdx:[%d]", iStreamIdx);
			//player[iStreamIdx] = new CBeseyePlayer(/*anw*/g_bitmap, PIX_FMT_RGB565LE, surface_width, surface_height);
			player[iStreamIdx] = new CBeseyePlayer(NULL, PIX_FMT_RGB565LE, 0, 0);
			player[iStreamIdx]->setWindowHolder(jni_host, getWindowByHolder);
			player[iStreamIdx]->registerVideoCallback(videoCallback, videoDeinitCallback);
			player[iStreamIdx]->registerCallback(rtmpStreamStatusCb);

			nativeString = (char *)jni_env->GetStringUTFChars(path, 0);
//			const char* streamList[] = {"mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_1.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_2.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_3.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_4.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_5.mp4"};
//
//			player[iStreamIdx]->createStreaming("rtmp://54.250.149.50/vods3/_definst_/", streamList, 6, 0);
			LOGE("openStreaming(), nativeString:[%s]", nativeString);
			player[iStreamIdx]->createStreaming(nativeString, iSeekOffset);
			jni_env->ReleaseStringUTFChars( path, nativeString);

			player[iStreamIdx]->unregisterVideoCallback();
			player[iStreamIdx]->unregisterCallback();

//			if(g_bitmap)
//				jni_env->DeleteGlobalRef(g_bitmap);
//			g_bitmap = NULL;

			delete player[iStreamIdx];
			player[iStreamIdx] = NULL;
			jni_env->DeleteGlobalRef(jni_host);
		}else{
			LOGE("openStreaming(), stream[%d] is playing", iStreamIdx);
			iRet = -2;
		}
	}
	LOGE("openStreaming(), end");
$ERR:
	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_openStreamingList(JNIEnv * env, jobject obj, int iStreamIdx, jobject surface, jstring host, jobjectArray streamLst, int iSeekOffset)
{
	DECLARE_JNIENV_WITH_RETURN()

	//jni_host = obj;
	int iRet = -1;

	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT){
		if(NULL == player[iStreamIdx]){
			jni_host = jni_env->NewGlobalRef(obj);

			int surface_width = 1280;//NULL != anw ?ANativeWindow_getWidth(anw):0;
			int surface_height = 720;//NULL != anw ?ANativeWindow_getHeight(anw):0;

			//LOGI("anw is [%d]", anw);

			iRet = iStreamIdx;
			LOGE("openStreaming(), iIdx:[%d]", iStreamIdx);
			player[iStreamIdx] = new CBeseyePlayer(NULL, PIX_FMT_RGB565LE, 0, 0);
			player[iStreamIdx]->setWindowHolder(jni_host, getWindowByHolder);
			player[iStreamIdx]->registerVideoCallback(videoCallback, videoDeinitCallback);
			player[iStreamIdx]->registerCallback(rtmpStreamStatusCb);
			const char * streamHost = (const char *)jni_env->GetStringUTFChars(host, 0);

			int streamCount = jni_env->GetArrayLength(streamLst);
			char** streamList = (char**)malloc(sizeof(char*)*streamCount);

			for (int i=0; i<streamCount; i++) {
				jstring string = (jstring) jni_env->GetObjectArrayElement(streamLst, i);
				streamList[i] = (char *)jni_env->GetStringUTFChars(string, 0);//GetStringUTFChars(env, string, 0);
				//const char *rawString = GetStringUTFChars(env, string, 0);
				// Don't forget to call `ReleaseStringUTFChars` when you're done.
			}

//			const char* streamList[] = {"mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_1.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_2.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_3.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_4.mp4",
//								 "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_5.mp4"};
//
//
			LOGE("openStreaming(), streamHost:[%s]", streamHost);
			player[iStreamIdx]->createStreaming(streamHost, (const char **)streamList, streamCount, iSeekOffset);
			//player[iStreamIdx]->createStreaming(nativeString, 0);
			jni_env->ReleaseStringUTFChars( host, streamHost);

			player[iStreamIdx]->unregisterVideoCallback();
			player[iStreamIdx]->unregisterCallback();

//			if(g_bitmap)
//				jni_env->DeleteGlobalRef(g_bitmap);
//			g_bitmap = NULL;

			delete player[iStreamIdx];
			player[iStreamIdx] = NULL;
			jni_env->DeleteGlobalRef(jni_host);
		}else{
			LOGE("openStreaming(), stream[%d] is playing", iStreamIdx);
			player[iStreamIdx]->closeStreaming();
		}
	}
	LOGE("openStreaming(), end");
$ERR:
	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_addStreamingPath(JNIEnv * env, jobject obj, int iStreamIdx, jstring path)
{
	DECLARE_JNIENV_WITH_RETURN()
	int iRet = -1;
	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT && player[iStreamIdx]){
		char *nativeString = (char *)jni_env->GetStringUTFChars( path, 0);
		iRet = player[iStreamIdx]->addStreamingPath(nativeString);
		jni_env->ReleaseStringUTFChars( path, nativeString);
	}else{
		LOGE("addStreamingPath(), stream[%d] is null", iStreamIdx);
	}
	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_pauseStreaming(JNIEnv * env, jobject obj, int iStreamIdx)
{
	DECLARE_JNIENV_WITH_RETURN()
	int iRet = 0;
	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT && NULL != player[iStreamIdx]){
		iRet = player[iStreamIdx]->pauseStreaming();
	}else{
		LOGE("pauseStreaming(), stream[%d] is null", iStreamIdx);
	}

	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_resumeStreaming(JNIEnv * env, jobject obj, int iStreamIdx)
{
	DECLARE_JNIENV_WITH_RETURN()
	int iRet = 0;
	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT && NULL != player[iStreamIdx]){
		iRet = player[iStreamIdx]->resumeStreaming();
	}else{
		LOGE("resumeStreaming(), stream[%d] is null", iStreamIdx);
	}

	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_updateSurface(JNIEnv * env, jobject obj, int iStreamIdx, jobject surface)
{
	DECLARE_JNIENV_WITH_RETURN()
	int iRet = 0;
	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT && NULL != player[iStreamIdx]){
		ANativeWindow* anw = ANativeWindow_fromSurface(env, surface);
		int surface_width = NULL != anw ?ANativeWindow_getWidth(anw):0;
		int surface_height = NULL != anw ?ANativeWindow_getHeight(anw):0;
		iRet = player[iStreamIdx]->updateWindow(anw, PIX_FMT_RGB565LE, surface_width, surface_height);
	}else{
		LOGE("updateSurface(), stream[%d] is invalid", iStreamIdx);
	}

	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_closeStreaming(JNIEnv * env, jobject obj, int iStreamIdx)
{
	DECLARE_JNIENV_WITH_RETURN()
	int iRet = 1;
	LOGE("closeStreaming()++, iStreamIdx:%d", iStreamIdx);
	if((0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT && NULL != player[iStreamIdx])){
		player[iStreamIdx]->closeStreaming();
	}else{
		LOGE("closeStreaming()++, player[iStreamIdx] is null");
	}
	return iRet;
}

/*
 * Audio support
 */
static jboolean audioBuffer16Bit = JNI_FALSE;
static jboolean audioBufferStereo = JNI_FALSE;
static jobject audioBuffer = NULL;
static void* audioBufferPinned = NULL;

int Android_JNI_GetAudioBufferSize(int sampleRate){
	DECLARE_JNIENV_WITH_RETURN()
    int iRet = jni_env->CallStaticIntMethod(mActivityClass, midAudioGetBufSize, sampleRate);
	//jvm->DetachCurrentThread();
	return iRet;
}

int Android_JNI_OpenAudioDevice2(int sampleRate, int is16Bit, int channelCount, int desiredBufferFrames){
	DECLARE_JNIENV_WITH_RETURN()

    int audioBufferFrames;

    if (!jni_env) {
        LOGE("callback_handler: failed to attach current thread");
    }
   // Android_JNI_SetupThread();

    //__android_log_print(ANDROID_LOG_VERBOSE, "SDL", "SDL audio: opening device");
    audioBuffer16Bit = is16Bit;
    audioBufferStereo = channelCount > 1;

    audioBufferFrames = desiredBufferFrames = Android_JNI_GetAudioBufferSize(sampleRate);
    LOGW("audioBufferFrames:%d", audioBufferFrames);

    if (jni_env->CallStaticIntMethod(mActivityClass, midAudioInit, sampleRate, audioBuffer16Bit, audioBufferStereo, desiredBufferFrames) != 0) {
        /* Error during audio initialization */
        LOGW("SDL audio: error on AudioTrack initialization!");
        return 0;
    }

    /* Allocating the audio buffer from the Java side and passing it as the return value for audioInit no longer works on
     * Android >= 4.2 due to a "stale global reference" error. So now we allocate this buffer directly from this side. */

//    if (is16Bit) {
        jshortArray audioBufferLocal = jni_env->NewShortArray( desiredBufferFrames * (audioBufferStereo ? 2 : 1));
        if (audioBufferLocal) {
            audioBuffer = jni_env->NewGlobalRef( audioBufferLocal);
            jni_env->DeleteLocalRef( audioBufferLocal);
        }
//    }
//    else {
//        jbyteArray audioBufferLocal = jni_env->NewByteArray(desiredBufferFrames * (audioBufferStereo ? 2 : 1));
//        if (audioBufferLocal) {
//            audioBuffer = jni_env->NewGlobalRef(audioBufferLocal);
//            jni_env->DeleteLocalRef(audioBufferLocal);
//        }
//    }

    if (audioBuffer == NULL) {
        __android_log_print(ANDROID_LOG_WARN, "SDL", "SDL audio: could not allocate an audio buffer!");
        return 0;
    }

    jboolean isCopy = JNI_FALSE;
//    if (audioBuffer16Bit) {
        audioBufferPinned = jni_env->GetShortArrayElements( (jshortArray)audioBuffer, &isCopy);
        audioBufferFrames = jni_env->GetArrayLength( (jshortArray)audioBuffer);
//    } else {
//        audioBufferPinned = jni_env->GetByteArrayElements((jbyteArray)audioBuffer, &isCopy);
//        audioBufferFrames = jni_env->GetArrayLength( (jbyteArray)audioBuffer);
//    }
//    if (audioBufferStereo) {
//        audioBufferFrames /= 2;
//    }
    //jvm->DetachCurrentThread();
    return audioBufferFrames;
}

void * Android_JNI_GetAudioBuffer2(){
    return audioBufferPinned;
}

void Android_JNI_WriteAudioBuffer2(){
	//LOGW("Android_JNI_WriteAudioBuffer2()+");
	DECLARE_JNIENV_WITHOUT_RETURN()
	jni_env->ReleaseShortArrayElements((jshortArray)audioBuffer, (jshort *)audioBufferPinned, JNI_COMMIT);
	jni_env->CallStaticVoidMethod(mActivityClass, midAudioWriteByteBuffer, (jshortArray)audioBuffer);
    /* JNI_COMMIT means the changes are committed to the VM but the buffer remains pinned */
	//jvm->DetachCurrentThread();

	//LOGW("Android_JNI_WriteAudioBuffer2()-");
}

void Android_JNI_CloseAudioDevice2(){
	DECLARE_JNIENV_WITHOUT_RETURN()

    jni_env->CallStaticVoidMethod(mActivityClass, midAudioQuit);

    if (audioBuffer) {
        jni_env->DeleteGlobalRef(audioBuffer);
        audioBuffer = NULL;
        audioBufferPinned = NULL;
    }
    //jvm->DetachCurrentThread();
}

void Android_JNI_detachCurrentThread(){
	DECLARE_JNIENV_WITHOUT_RETURN()
	jvm->DetachCurrentThread();
}
#include "http_cgi.h"
using namespace std;

static pthread_t mReceiveAudioThread(0);

//const int G711_SAMPLES_PER_FRAME = 160;
const int TABLE_SIZE = 8;
const int BIAS = 0x84;		/* Bias for linear code. */
const int CLIP = 8159;
const int SIGN_BIT = 0x80;	/* Sign bit for a A-law byte. */
const int QUANT_MASK = 0xf;  /* Quantization field mask. */
const int NSEGS = 8;         /* Number of A-law segments. */
const int SEG_SHIFT = 4;     /* Left shift for segment number. */
const int SEG_MASK = 0x70;   /* Segment field mask. */

short ulaw2linear(unsigned char u_val)
{
   short t;

   /* Complement to obtain normal u-law value. */
   u_val = ~u_val;

   /*
    * Extract and bias the quantization bits. Then
    * shift up by the segment number and subtract out the bias.
    */
   t = ((u_val & QUANT_MASK) << 3) + BIAS;
   t <<= ((unsigned)u_val & SEG_MASK) >> SEG_SHIFT;

   return ((u_val & SIGN_BIT) ? (BIAS - t) : (t - BIAS));
}

void writeBuf(unsigned char* charBuf, int iLen){
	//LOGW("writeBuf:%d", iLen);
	short* shortBuf = (short*)audioBufferPinned;
	for(int i = 0 ; i < iLen; i++){
		shortBuf[i] = ulaw2linear(charBuf[i]);
	}
	Android_JNI_WriteAudioBuffer2();
}

void* runReceiveBufViaCGI(void* userdata){
	int res;
	Android_JNI_OpenAudioDevice2(16000, 1, 1, 1280);
	char data[BUF_SIZE];
	char session[SESSION_SIZE];

	// clear our memory
	memset(session, 0, sizeof(session));
	memset(data, 0, BUF_SIZE*sizeof(char));

	if(GetSession(session) != 0) {
		LOGE("Get session failed.");
		//return 0;
	}else{
		//res = GetCGI("getSystemName", data, session);
		res = GetAudioBufCGI("receive", data, session, writeBuf);
		LOGE("GetAudioBufCGI:res(%d)\n%s",res);
		Android_JNI_CloseAudioDevice2();
	}
	Android_JNI_detachCurrentThread();
	mReceiveAudioThread = 0;
}

void receiveBufViaCGI(){
	if(!mReceiveAudioThread){
		if (0 != (errno = pthread_create(&mReceiveAudioThread, NULL, runReceiveBufViaCGI, NULL))) {
			LOGE("receiveBufViaCGI(), error when create mReceiveAudioThread,%d\n", errno);
		}else{
			pthread_setname_np(mReceiveAudioThread, "mReceiveAudioThread");
		}
	}
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_CameraViewActivity_receiveAudioBufFromCam(JNIEnv * env, jobject obj, jstring strPath)
{
	DECLARE_JNIENV_WITH_RETURN()
	jboolean iRet = false;
	if(0 == mReceiveAudioThread){
		receiveBufViaCGI();
	}
	return iRet;
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_CameraViewActivity_receiveAudioBufThreadRunning(){
	return mReceiveAudioThread != 0;
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_CameraViewActivity_stopReceiveAudioBufThread(){
	if(mReceiveAudioThread){
		stopReceiveAudioBuf();
		return true;
	}
	return false;
}

///* Called from the main */
int main(int argc, char **argv)
{
    return 0;
}

#ifdef __cplusplus
 }
#endif
