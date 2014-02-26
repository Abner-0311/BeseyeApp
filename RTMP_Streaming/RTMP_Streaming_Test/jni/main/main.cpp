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

static jmethodID  playMethod, getBufMethod, getBufSizeMethod = NULL;
static jmethodID  recordMethod, endLoopMethod = NULL;

static JavaVM*   jvm = NULL;

//CBeseyeAudioStreamer* audioStreamer = NULL;

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_startRecord(JNIEnv * env, jobject obj, int fd){
	int iRet = 0;
	LOGE("startRecord()++++++++++++++++++++++++++");
	/*if(NULL == audioStreamer)*/{
		CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
		if(CBeseyeAudioStreamer::checkExit() && audioStreamer.setStreamingInfo("rtmp://192.168.2.224:1935/myapp/audiostream", "/data/data/com.churnlabs.ffmpegsample/beseye.fifo")){
			iRet = audioStreamer.startAudioStreaming();
		}
	}

	LOGE("startRecord()-");
	return iRet;
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_isRecording(JNIEnv * env, jobject obj){
	int iRet = 0;

	CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
	LOGW("isRecording(), status:%d",audioStreamer.get_Stream_Status());
	iRet = audioStreamer.isStreamingPlaying();

	//LOGE("isRecording()-");
	return iRet;
}

JNIEXPORT void JNICALL Java_com_app_beseye_CameraViewActivity_recordAudio(JNIEnv * env, jobject obj, jbyteArray array, int iBufSize)
{
	DECLARE_JNIENV_WITHOUT_RETURN()
	CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
	jbyte *bytes = jni_env->GetByteArrayElements(array, NULL);
	audioStreamer.writeAudioBuffer((char*)bytes, iBufSize);
	jni_env->ReleaseByteArrayElements( array, bytes, 0);
    //LOGE("recordAudio()-");
}

JNIEXPORT void JNICALL Java_com_app_beseye_CameraViewActivity_endRecord(JNIEnv * env, jobject obj){

	CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
	audioStreamer.closeAudioStreaming();
	LOGE("endRecord()-");
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_CameraViewActivity_nativeClassInit(JNIEnv *env, jclass clss)
{
	LOGE("nativeClassInit()+");
	if (JNI_OK != env->GetJavaVM(&jvm))
	{
		LOGE("GetJavaVM failed");
		return 0;
	}

	jclass cls = env->FindClass("com/churnlabs/ffmpegsample/MainActivity");
	if(NULL == cls){
		LOGE("cls is empty");
		return 0;
	}

	playMethod = env->GetMethodID(cls, "playSound", "(I)V");
	if(NULL == playMethod){
		LOGE("playMethod is empty");
		return 0;
	}

	getBufMethod = env->GetMethodID(cls, "getRecordSoundBuf", "()[B");
	if(NULL == getBufMethod){
		LOGE("getBufMethod is empty");
		return 0;
	}

	getBufSizeMethod = env->GetMethodID( cls, "getRecordSampleRead", "()I");
	if(NULL == getBufSizeMethod){
		LOGE("getBufMethod is empty");
		return 0;
	}

	endLoopMethod = env->GetMethodID( cls, "stopFeedPipe", "()V");
	if(NULL == endLoopMethod){
		LOGE("endLoopMethod is empty");
		return 0;
	}

	//LOGE("nativeClassInit()-, playMethod:%d",playMethod);
	LOGE("nativeClassInit()-");

	av_log_set_level(AV_LOG_INFO);
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

const int MAX_STREAM_COUNT = 2;

CBeseyePlayer* player[MAX_STREAM_COUNT] ={};

void videoCallback(void* anw, uint8_t* srcbuf, uint_t iFormat, uint_t linesize, uint_t iWidth, uint_t iHeight){
	//av_log(NULL, AV_LOG_ERROR, "videoCallback, format:%d, %d, %d, %d\n", srcbuf, linesize, iWidth, iHeight);
	ANativeWindow* window = (ANativeWindow*)anw;
	if(window){
		ANativeWindow_Buffer buffer;

		int lockResult = ANativeWindow_lock(window, &buffer, NULL);
		if (lockResult == 0) {

			uint8_t *frameLine, *line;
			int yy;
			uint_t iBytePxl = (buffer.format == WINDOW_FORMAT_RGBA_8888)?4:2;

			//av_log(NULL, AV_LOG_ERROR, "videoCallback, buffer=>%d, %d, %d,  iBytePxl:%d\n", buffer.width, buffer.height, buffer.stride, iBytePxl);

			uint_t iSizeCpy = iBytePxl*sizeof(uint8_t);

			if(buffer.width == iWidth && buffer.height == iHeight && iBytePxl == (linesize/iWidth)){
				if( buffer.width == buffer.stride)
					memcpy(buffer.bits , srcbuf, iWidth*iHeight*iBytePxl);
				else{
					for (int iRow = 0; iRow < buffer.height; iRow++) {
						line      = (uint8_t*)buffer.bits          + (iRow * buffer.stride*iBytePxl);
						frameLine = (uint8_t*)srcbuf   			   + (iRow * linesize);

						memcpy(line, frameLine, linesize);
					}
				}
			}else{
//						av_log(NULL, AV_LOG_ERROR, "fill_bitmap(), surface_height:%d, surface_width:%d, pFrameRGB->linesize[0]:%d, info->stride:%d, vp->height:%d, vp->width:%d",
//								buffer.height , buffer.width, pFrameRGB->linesize[0], buffer.stride, vp->width, vp->height);
				for (yy = 0; yy < buffer.height && yy < iHeight; yy++) {
					line      = (uint8_t*)buffer.bits          + (yy * buffer.stride*iBytePxl);
					frameLine = (uint8_t*)srcbuf   			   + (yy * linesize);

					int xx;
					for (xx = 0; xx < buffer.width && xx < iWidth; xx++) {
						int out_offset = xx * iBytePxl;
						int in_offset = xx * iBytePxl;

						memcpy(line + out_offset, frameLine+in_offset, iSizeCpy);
					}
				}
			}

			//av_log(NULL, AV_LOG_INFO, "ANativeWindow_unlockAndPost");
			ANativeWindow_unlockAndPost(window);
		}
		else{
			av_log(NULL, AV_LOG_ERROR, "ANativeWindow_lock failed error %d",lockResult);
		}
	}
}

void videoDeinitCallback(void* anw){
	ANativeWindow* window = (ANativeWindow*)anw;
	if(window)
		ANativeWindow_release(window);
}

JNIEXPORT int JNICALL Java_com_app_beseye_CameraViewActivity_openStreaming(JNIEnv * env, jobject obj, int iStreamIdx, jobject surface, jstring path)
{
	DECLARE_JNIENV_WITH_RETURN()

	if(0 <= iStreamIdx && iStreamIdx < MAX_STREAM_COUNT){
		if(NULL == player[iStreamIdx]){
			char *nativeString = NULL;
			ANativeWindow* anw = ANativeWindow_fromSurface(env, surface);

			int surface_width = NULL != anw ?ANativeWindow_getWidth(anw):0;
			int surface_height = NULL != anw ?ANativeWindow_getHeight(anw):0;

			LOGI("anw is [%d]", anw);

			LOGE("openStreaming(), iIdx:[%d]", iStreamIdx);
			player[iStreamIdx] = new CBeseyePlayer(anw, PIX_FMT_RGB565LE, surface_width, surface_height);
			player[iStreamIdx]->registerVideoCallback(videoCallback, videoDeinitCallback);

			nativeString = (char *)jni_env->GetStringUTFChars(path, 0);
			player[iStreamIdx]->createStreaming(nativeString);
			jni_env->ReleaseStringUTFChars( path, nativeString);

			player[iStreamIdx]->unregisterVideoCallback();

			delete player[iStreamIdx];
			player[iStreamIdx] = NULL;
		}else{
			LOGE("openStreaming(), stream[%d] is playing", iStreamIdx);
		}
	}
$ERR:
	return iStreamIdx;
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

///* Called from the main */
int main(int argc, char **argv)
{
    return 0;
}

#ifdef __cplusplus
 }
#endif
