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

#include "utils.h"
#include "ffmpeg_ext.h"

#include "SDL_config.h"
/* Include the SDL main definition header */
#include "SDL_main.h"

#include <android/bitmap.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavformat/url.h>

#endif

int main(int argc, char **argv);
extern void writeBufToPipe();
extern void endLoop();

/* Cheat to keep things simple and just use some globals. */
AVFormatContext *pFormatCtx;
AVCodecContext *pCodecCtx;
AVCodecContext * pACodecCtx;
AVFrame *pFrame, *pAudioFrame;
AVFrame *pFrameRGB;
int videoStream, audioStream;

static void fill_bitmap(AndroidBitmapInfo*  info, void *pixels, AVFrame *pFrame)
{
    uint8_t *frameLine, *line;
    int  yy;
    int iSizeCpy = 3*sizeof(uint8_t);
    LOGE("fill_bitmap(), info->height:%d, info->width:%d, pFrame->linesize[0]:%d, info->stride:%d", info->height , info->width, pFrame->linesize[0], info->stride);
    for (yy = 0; yy < info->height && yy < pCodecCtx->height; yy++) {
        line      = (uint8_t*)pixels          + (yy * info->stride);;
        frameLine = (uint8_t*)pFrame->data[0] + (yy * pFrame->linesize[0]);
        int xx;
        for (xx = 0; xx < info->width && xx < pCodecCtx->width; xx++) {
            int out_offset = xx * 4;
            int in_offset = xx * 3;

            memcpy(line + out_offset, frameLine+in_offset, iSizeCpy);
            line[out_offset+3] = 0xff;//frameLine[in_offset+2];
            //LOGE("check line[%d]= %d", out_offset, line[out_offset]  );
        }
        //pixels = (uint8_t*)pixels + info->stride;
        //LOGE("check 003");
    }
}

#define DECLARE_JNIENV_WITHOUT_RETURN() \
	JNIEnv* jni_env; \
	if (JNI_OK != (*jvm)->AttachCurrentThread(jvm, &jni_env, NULL)) {  return; } \

static jmethodID  playMethod, getBufMethod, getBufSizeMethod = NULL;
static jmethodID  recordMethod, endLoopMethod = NULL;
static jobject   jni_host = NULL;
static JavaVM*   jvm = NULL;
int pipeRFD = 0, pipeWFD=0;
static jobject   jni_host_rec = NULL;

JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_startRecord2(JNIEnv * env, jobject this, int fd)
{
	jni_host_rec = this;
	if(0 == pipeRFD){
		LOGE("recordAudio2+, open pipe\n");

		char* fifo = "/data/data/com.churnlabs.ffmpegsample/beseye.fifo";
		if (mkfifo(fifo, 0666) < 0 && errno != EEXIST) {
			LOGE("mkfifo failed");
		}

		if ((pipeRFD = open(fifo, O_RDONLY|O_NONBLOCK)) == -1) {
			LOGE("open pipeRFD failed");
		}

		if ((pipeWFD= open(fifo, O_WRONLY|O_NONBLOCK)) == -1) {
			LOGE("open pipeWFD failed");
		}
	}

	//pipeRFD = fd;
	LOGE("startRecord2()-, pipeRFD:%d, pipeWFD:%d, fd:%d",pipeRFD, pipeWFD, fd);
	char* pipeFile = malloc(sizeof(char*)*256);
	memset(pipeFile, 0, sizeof(pipeFile));
	sprintf(pipeFile, "pipe:%d", pipeRFD);

//	const int argc = 17;
//	const char *argv[] = {"ffmpeg",
//						  "-re",
//						  "-f",
//						  "u16be",
//						  //"-b",
//						  //"64000",
//						  "-acodec",
//						  "pcm_u16be",
//						  "-ar",
//						  "16000",
//						  "-ac",
//						  "1" ,
//						  "-i",
//						  "-", //pipeFile,
//						  "-acodec",
//						  "libspeex",
//						  "-f",
//						  "flv",
//						  "rtmp://192.168.2.106/myapp/mystream"};



	//char* argv[]={"ffmpeg", "-re", "-i", "/sdcard/test2.mp3", "-acodec", "libspeex", "-ar", "16000","-f", "flv", "rtmp://192.168.2.106/myapp/mystream"};
	char* argv[]={"ffmpeg", "-re","-vn", "-f", "s16le","-sample_fmt","AV_SAMPLE_FMT_S16","-acodec", "pcm_s16le", "-ar", "44100", "-ac", "1", "-i", pipeFile, "-acodec", "pcm_alaw", "-ar", "8000","-ab","64k","-f", "flv",
			"rtmp://192.168.2.106/myapp/mystream"};
	//char* argv[]={"ffmpeg", "-re", "-f", "amr","-acodec", "aac", "-ar", "44100", "-ac", "1", "-i", pipeFile, "-c", "copy", "-ar","-f", "flv", "rtmp://192.168.2.106/myapp/mystream"};
	int argc = sizeof(argv)/sizeof(char*);
	main(argc, argv);
	LOGE("startRecord2()-");
}

JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_recordAudio2(JNIEnv * env, jobject this, jbyteArray array, int iBufSize)
{
	LOGE("recordAudio2+, iBufSize:%d\n", iBufSize);
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(0 == pipeRFD){
		LOGE("recordAudio2+, open pipe\n");

		char* fifo = "/data/data/com.churnlabs.ffmpegsample/beseye.fifo";
		if (mkfifo(fifo, 0666) < 0 && errno != EEXIST) {
			LOGE("mkfifo failed");
		}

		if ((pipeRFD = open(fifo, O_RDONLY|O_NONBLOCK)) == -1) {
			LOGE("open pipeRFD failed");
		}

		if ((pipeWFD= open(fifo, O_WRONLY|O_NONBLOCK)) == -1) {
			LOGE("open pipeWFD failed");
		}
	}

    jbyte *bytes = (*jni_env)->GetByteArrayElements(jni_env, array, NULL);
	//LOGE("recordAudio2-1+, 1%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
	int ret = -1;
	int iTrial = 0;
	do{
		ret = write(pipeWFD,bytes,iBufSize);
		if (0 > ret) {
			sleep(10);
			LOGE("recordAudio2, iTrial = %d^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^",++iTrial);
		}
	}while(0 > ret || iTrial >10);
	LOGE("recordAudio2, write = %d---------------------------------------------------------",ret);

	//LOGE("recordAudio2-2+, 2\n");
	(*jni_env)->ReleaseByteArrayElements(jni_env, array, bytes, 0);

    LOGE("recordAudio2-");
}

JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_endRecord2(JNIEnv * env, jobject this)
{
	LOGE("endRecord2()+");
	if(0 < pipeRFD){
		if(0 > close(pipeRFD)){
			LOGE("close pipeRFD failed");
		}
	}

	if(0 < pipeWFD){
		if(0 > close(pipeWFD)){
			LOGE("close pipeWFD failed");
		}
	}
	LOGE("endRecord2-");
}

void writeBufToPipe(){
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(NULL != getBufMethod && NULL != getBufSizeMethod && NULL != jni_host_rec){
		jbyteArray array = (*jni_env)->CallObjectMethod(jni_env, jni_host_rec, getBufMethod);
		if(NULL != array){
			int iBufSize = (*jni_env)->CallIntMethod(jni_env, jni_host_rec, getBufSizeMethod);
			if(0 < iBufSize){
				jbyte *bytes = (*jni_env)->GetByteArrayElements(jni_env, array, NULL);
					LOGE("writeBufToPipe-1+, 1\n");
					int ret = -1;
					do{
						ret = write(pipeWFD,bytes,iBufSize);
						if (0 > ret) {
							sleep(20);
						}
					}while(0 > ret);
					LOGE("writeBufToPipe, write = %d**********************************",ret);

					LOGE("writeBufToPipe-2+, 2\n");
					(*jni_env)->ReleaseByteArrayElements(jni_env, array, bytes, 0);
			}else{
				LOGE("writeBufToPipe, iBufSize <=0\n");
			}
		}else{
			LOGE("writeBufToPipe, array is NULL\n");
		}
	}else{
		LOGE("writeBufToPipe, getBufMethod is NULL\n");
	}
}

void endLoop(){
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(NULL != endLoopMethod && NULL != jni_host_rec){
		(*jni_env)->CallVoidMethod(jni_env, jni_host_rec, endLoopMethod);
	}
}

void rtmp_log_internal(int level, const char *fmt, va_list args)
{
	if(NULL != fmt && level <=2)
		LOGE(fmt, args);
}

void rtmp_log_internal2(int level, const char *msg)
{
	if(NULL != msg && level <=RTMP_LOGINFO)
		LOGE(msg);
}

void ffmpeg_vlog(void* avcl, int level, const char *fmt, va_list vl){
	if(NULL != fmt && level <=AV_LOG_DEBUG)
		LOGE(fmt, vl);
}

void ffmpeg_vlog2(void* avcl, int level, const char *msg){
	if(NULL != msg && level <=AV_LOG_INFO)
		LOGE(msg);
}

JNIEXPORT jboolean JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_nativeClassInit(JNIEnv *env, jclass class)
{
	LOGE("nativeClassInit()+");
	if (JNI_OK != (*env)->GetJavaVM(env, &jvm))
	{
		LOGE("GetJavaVM failed");
		return 0;
	}

	jclass cls = (*env)->FindClass(env, "com/churnlabs/ffmpegsample/MainActivity");
	if(NULL == cls){
		LOGE("cls is empty");
		return 0;
	}

	playMethod = (*env)->GetMethodID(env, cls, "playSound", "(I)V");
	if(NULL == playMethod){
		LOGE("playMethod is empty");
		return 0;
	}

	getBufMethod = (*env)->GetMethodID(env, cls, "getRecordSoundBuf", "()[B");
	if(NULL == getBufMethod){
		LOGE("getBufMethod is empty");
		return 0;
	}

	getBufSizeMethod = (*env)->GetMethodID(env, cls, "getRecordSampleRead", "()I");
	if(NULL == getBufSizeMethod){
		LOGE("getBufMethod is empty");
		return 0;
	}

	endLoopMethod = (*env)->GetMethodID(env, cls, "stopFeedPipe", "()V");
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
void JNICALL Java_com_churnlabs_ffmpegsample_SDLActivity_nativeInit(JNIEnv* env, jclass cls, jobject obj)
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

static int addPlaypth(AVFormatContext *pFCtx, char *path){
	int iRet = -1;
	if(NULL != pFCtx){
		AVIOContext* ioCtx =  pFCtx->pb;
		if(NULL != ioCtx){
			URLContext* urlCtx = (URLContext*)ioCtx->opaque;
			if(NULL != urlCtx){
				iRet = gen_play_wrapper(urlCtx, path);
			}else{
				LOGE("addPlaypth(), urlCtx is null");
			}
		}else{
			LOGE("addPlaypth(), ioCtx is null");
		}
	}else{
		LOGE("addPlaypth(), pFCtx is null");
	}
	//LOGI("addPlaypth(), path:%s , len: %d, iRet:%d", path, strlen(path), iRet);
	return iRet;
}

JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_openFile(JNIEnv * env, jobject this)
{
	createStreaming("rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");

	const int bManualSet = 0;
	//
	jni_host = this;
    int ret;
    int err;
    int i;
    AVCodec *pVCodec;
    AVCodec *pACodec;
    uint8_t *buffer;
    int numBytes;

    //av_register_all();
    avcodec_register_all();
    av_register_all();
    avformat_network_init();

    av_log_set_level(AV_LOG_INFO);

    LOGE("Registered formats");

    if(bManualSet){
        pFormatCtx = avformat_alloc_context();

        pFormatCtx->video_codec_id   = CODEC_ID_H264;
        pFormatCtx->audio_codec_id   = CODEC_ID_SPEEX;
        pFormatCtx->flags |= AVFMT_FLAG_NONBLOCK;
    }


    char * fileNmae = //"http://live.3gv.ifeng.com/live/zixun.m3u8?fmt=x264_512k_mpegts&size=320x240";
    //"rtmp://live.goldia.cn/live/livestream";
    //"rtmp://planeta-online.tv:1936/live/channel_22";
    //"rtmp://192.168.2.106/myapp2/mystream";
    //"rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4";
    "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4";
    //"file:/sdcard/test.mp4";
    //err = av_open_input_file(&pFormatCtx, fileNmae/*"rtmp://live.goldia.cn/live/livestream"*/, NULL, 0, NULL);

    //rtmp_metadata_set_callback(rtmp_metdata_cb);

    LOGE("Called open file %s", fileNmae);
    err = avformat_open_input(&pFormatCtx, fileNmae, NULL, NULL);
    if(err!=0) {
        LOGE("Couldn't open file %s", fileNmae);
        return;
    }
    LOGE("Opened file %s", fileNmae);
    videoStream = 0;
    audioStream = 1;

    //av_dump_format(&pFormatCtx, 0, fileNmae, 0);
//    int idx = 1;
//    for(; idx <= 10;idx++){
//
//    }
    //addPlaypth(pFormatCtx, "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_1.mp4");
    if(!bManualSet){
    	LOGE("av_find_stream_info()+");
		if(avformat_find_stream_info_ext(pFormatCtx, NULL)<0) {
			LOGE("Unable to get stream info");
			return;
		}

		LOGE("av_find_stream_info()-");

		if ((videoStream =  av_find_best_stream(pFormatCtx, AVMEDIA_TYPE_VIDEO, -1, -1, &pVCodec, 0)) < 0) {
			LOGE("Unable to find best stream");
			return;
		}
		if ((audioStream =  av_find_best_stream(pFormatCtx, AVMEDIA_TYPE_AUDIO, -1, -1, &pACodec, 0)) < 0) {
			LOGE("Unable to find best audio stream");
			//return;
		}else{
			LOGE("audio codec: %s", pACodec->name);
		}

//		for (i=0; i<pFormatCtx->nb_streams; i++) {
//			if(pFormatCtx->streams[i]->codec->codec_type==CODEC_TYPE_VIDEO) {
//				videoStream = i;
//				break;
//			}
//		}
    }

    if(videoStream==-1) {
        LOGE("Unable to find video stream");
        return;
    }

    LOGI("Video stream is [%d]", videoStream);
    LOGI("Audio stream is [%d]", audioStream);

    if(0 <= audioStream){
    	pACodecCtx = pFormatCtx->streams[audioStream]->codec;
		if(bManualSet){
			pACodecCtx->codec_id = pFormatCtx->audio_codec_id;
			pACodecCtx->codec_type = AVMEDIA_TYPE_AUDIO;
			pACodecCtx->sample_rate    = 44100;//select_sample_rate(codecSpeex);
			pACodecCtx->channel_layout = AV_CH_LAYOUT_MONO ;//select_channel_layout(codecSpeex);
			pACodecCtx->channels       = 1;
		}

		pACodec=avcodec_find_decoder(pACodecCtx->codec_id);
		if(pACodec==NULL) {
			LOGE("Unsupported audio codec");
			return;
		}

		LOGE("audio codec: %s", pACodec->name);

		if(avcodec_open2(pACodecCtx, pACodec, NULL)<0) {
			LOGE("Unable to open audio codec");
			return;
		}
    }

    if(0 <= videoStream){
		pCodecCtx=pFormatCtx->streams[videoStream]->codec;
		if(bManualSet){
			pCodecCtx->codec_id = pFormatCtx->video_codec_id;
			pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
			pCodecCtx->width = 640;
			pCodecCtx->height = 360;
			pCodecCtx->pix_fmt = PIX_FMT_YUV420P;
		}

		pVCodec=avcodec_find_decoder(pCodecCtx->codec_id);
		if(pVCodec==NULL) {
			LOGE("Unsupported codec");
			return;
		}
		LOGE("video codec: %s", pVCodec->name);

		if(avcodec_open2(pCodecCtx, pVCodec, NULL)<0) {
			LOGE("Unable to open codec");
			return;
		}
    }

    pFrame=avcodec_alloc_frame();
    pAudioFrame=avcodec_alloc_frame();
    pFrameRGB=avcodec_alloc_frame();
    LOGI("Video size is [%d x %d]", pCodecCtx->width, pCodecCtx->height);

    numBytes=avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width, pCodecCtx->height);
    LOGI("numBytes is [%d]", numBytes);
    LOGI("pFrameRGB->linesize is [%d]", pFrameRGB->linesize);
    LOGI("pFrame->linesize is [%d]", pFrame->linesize);
    buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));

    avpicture_fill((AVPicture *)pFrameRGB, buffer, PIX_FMT_RGB24,pCodecCtx->width, pCodecCtx->height);
    LOGI("pFrameRGB is [%d, %d]", pFrameRGB->width, pFrameRGB->height);
}

JNIEXPORT int JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_addStreamingPath(JNIEnv * env, jobject this, jstring path)
{
	DECLARE_JNIENV_WITHOUT_RETURN()
	const char *nativeString = (*jni_env)->GetStringUTFChars(jni_env, path, 0);
	addPlaypth(pFormatCtx, nativeString);
	(*jni_env)->ReleaseStringUTFChars(jni_env, path, nativeString);
}

JNIEXPORT int JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_drawFrame(JNIEnv * env, jobject this, jobject bitmap, jbyteArray array, int iLenArray)
{
	DECLARE_JNIENV_WITHOUT_RETURN()
	int iRet = 0;
    AndroidBitmapInfo  info;
    void*              pixels = NULL;
    int                ret = -1;

    int err;
    int i;
    int frameFinished = 0, out_size=0;
    AVPacket packet;
    static struct SwsContext *img_convert_ctx;
    int64_t seek_target;
    uint8_t *outbuf= NULL;

    //jclass  cls = (*env)->GetObjectClass(env, this);
    //playMethod = (*env)->GetMethodID(env, cls, "playSound", "([BI)V");
    if (NULL == playMethod) {
        LOGE("playMethod is null");
        goto EXIT;
    }

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        goto EXIT;
    }
    //LOGE("Checked on the bitmap");

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    }
    //LOGE("Grabbed the pixels");
    //outbuf = malloc(/*AVCODEC_MAX_AUDIO_FRAME_SIZE*/4096);

    i = 0;
    while((i==0) && (av_read_frame_ext(pFormatCtx, &packet)>=0)) {
    	//LOGE("check 0");
  		if(packet.stream_index==videoStream) {
  			//LOGE("check 1");
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
            //LOGE("check 2");

    		if(frameFinished) {
                LOGE("videoStream packet pts %llu", packet.pts);
                // This is much different than the tutorial, sws_scale
                // replaces img_convert, but it's not a complete drop in.
                // This version keeps the image the same size but swaps to
                // RGB24 format, which works perfect for PPM output.
                int target_width = pCodecCtx->width;
                int target_height = pCodecCtx->height;

                //LOGE("packet pts %llu ...", packet.pts);
                img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, 
                       pCodecCtx->pix_fmt, 
                       target_width, target_height, PIX_FMT_RGB24, SWS_BICUBIC,
                       NULL, NULL, NULL);
               // LOGE("check 3");
                if(img_convert_ctx == NULL) {
                    LOGE("could not initialize conversion context\n");
                    goto EXIT;
                }
                //LOGE("check 4");
                sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data, pFrameRGB->linesize);
                LOGI("pFrameRGB is [%d, %d]", pFrameRGB->width, pFrameRGB->height);
                //LOGE("check 5");
                // save_frame(pFrameRGB, target_width, target_height, i);
                fill_bitmap(&info, pixels, pFrameRGB);
                //LOGE("check 6");
                iRet =i = 1;
                goto EXIT;
    	    }
        }else if (packet.stream_index==audioStream){
        	out_size = 4096;//AVCODEC_MAX_AUDIO_FRAME_SIZE;
        	//LOGE("audioStream packet 1, %d, %d, %d", (NULL == pACodecCtx), (NULL == outbuf), out_size);
        	//avcodec_decode_audio3(pACodecCtx, (short *)outbuf, &out_size, &packet);
        	out_size = avcodec_decode_audio4(pACodecCtx, pAudioFrame, &frameFinished, &packet);
        	//LOGE("audioStream packet 2");
        	if (out_size > 0) {
        	  /* if a frame has been decoded, output it */
        	  //LOGE("audioStream packet 3");
        	  int data_size = av_samples_get_buffer_size(NULL, pACodecCtx->channels,pAudioFrame->nb_samples, pACodecCtx->sample_fmt, 1);

        	  LOGE("audioStream packet pts %llu, %d, %d", packet.pts, out_size, out_size);
        	  if(data_size > 0){
        		  jbyte *bytes = (*jni_env)->GetByteArrayElements(jni_env, array, NULL);
        		  int iCurPos = 0;
        		  while(data_size > iCurPos){
        			  int iLenToCopy = ((data_size - iCurPos) > iLenArray)?iLenArray:(data_size - iCurPos);
        			  memcpy(bytes + iCurPos, pAudioFrame->data[0], iLenToCopy);
        			  (*jni_env)->CallVoidMethod(jni_env, jni_host, playMethod, iLenToCopy);
        			  iCurPos += iLenToCopy;
        		  }

        		  (*jni_env)->ReleaseByteArrayElements(jni_env, array, bytes, 0);
        	  }


        	  //LOGE("audioStream packet 4");
//        	  if((iRet+out_size) > 5224){
//        		  (*jni_env)->CallVoidMethod(jni_env, jni_host, playMethod, iRet);
//        		  iRet = 0;
//        		  memcpy(bytes, pAudioFrame->data[0], out_size);
//        	  }
//        	  else
//        		  memcpy(bytes+(iRet*sizeof(jbyte)), pAudioFrame->data[0], out_size); //
//        	  //LOGE("audioStream packet 5");
//        	  (*jni_env)->ReleaseByteArrayElements(jni_env, array, bytes, 0);
//        	  //LOGE("audioStream packet 6, %d, %d, %d", (NULL == playMethod), (NULL == array), out_size);
//        	  (*jni_env)->CallVoidMethod(jni_env, jni_host, playMethod, out_size);
//        	  //iRet +=out_size;
//        	  //LOGE("audioStream packet 7, %d, %d", out_size, iRet);
        	}
  		}
//  		if(iRet> 0){
//  		  (*jni_env)->CallVoidMethod(jni_env, jni_host, playMethod, iRet);
//  		  iRet = 0;
//  		}
    }
EXIT:
	av_free_packet(&packet);

//	if(NULL != outbuf)
//		free(outbuf);
	if(NULL != pixels)
		AndroidBitmap_unlockPixels(env, bitmap);
    return iRet ;
}


JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_drawFrameAt(JNIEnv * env, jobject this, jstring bitmap, jint secs)
{
    AndroidBitmapInfo  info;
    void*              pixels;
    int                ret;

    int err;
    int i;
    int frameFinished = 0;
    AVPacket packet;
    static struct SwsContext *img_convert_ctx;
    int64_t seek_target;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    LOGE("Checked on the bitmap");

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    LOGE("Grabbed the pixels");

    seek_frame(secs * 1000);

    i = 0;
    while ((i== 0) && (av_read_frame(pFormatCtx, &packet)>=0)) {
  		if(packet.stream_index==videoStream) {
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
    
    		if(frameFinished) {
                // This is much different than the tutorial, sws_scale
                // replaces img_convert, but it's not a complete drop in.
                // This version keeps the image the same size but swaps to
                // RGB24 format, which works perfect for PPM output.
                int target_width = pCodecCtx->width;
                int target_height = pCodecCtx->height;
                img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, 
                       pCodecCtx->pix_fmt, 
                       target_width, target_height, PIX_FMT_RGB24, SWS_BICUBIC,
                       NULL, NULL, NULL);
                if(img_convert_ctx == NULL) {
                    LOGE("could not initialize conversion context\n");
                    return;
                }
                sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data, pFrameRGB->linesize);

                // save_frame(pFrameRGB, target_width, target_height, i);
                fill_bitmap(&info, pixels, pFrameRGB);
                i = 1;
    	    }
        }
        av_free_packet(&packet);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}

//////////////////////////////////////////////////////////////////


const char *filename = "rtmp://192.168.173.100/myapp2/mystream";
AVOutputFormat *fmt = NULL;
AVFormatContext *oc = NULL;
AVStream *audio_st = NULL;
AVCodec *codecSpeex = NULL;
AVCodecContext *c = NULL;
double audio_pts;

static int16_t *samples = NULL;
static uint8_t *audio_outbuf = NULL;
static int audio_outbuf_size;
static int audio_input_frame_size;
static int buffer_size;


static void close_audio(AVFormatContext *oc, AVStream *st)
{
    avcodec_close(st->codec);

    av_free(samples);
    av_free(audio_outbuf);
}

static int check_sample_fmt(AVCodec *codec, enum AVSampleFormat sample_fmt)
{
	const enum AVSampleFormat *p = codec->sample_fmts;
    while (*p != AV_SAMPLE_FMT_NONE) {
		if (*p == sample_fmt)
		   return 1;
		p++;
	}
	return 0;
}

static int select_sample_rate(AVCodec *codec)
{
     const int *p;
     int best_samplerate = 0;

	if (!codec->supported_samplerates)
		return 44100;

	p = codec->supported_samplerates;
	while (*p) {
		best_samplerate = FFMAX(*p, best_samplerate);
		p++;
	}
	return best_samplerate;
}

/* select layout with the highest channel count */
static int select_channel_layout(AVCodec *codec)
{
	const uint64_t *p;
	uint64_t best_ch_layout = 0;
	int best_nb_channels   = 0;

	if (!codec->channel_layouts)
		return AV_CH_LAYOUT_STEREO;

	p = codec->channel_layouts;
	while (*p) {
		int nb_channels = av_get_channel_layout_nb_channels(*p);

		if (nb_channels > best_nb_channels) {
			best_ch_layout    = *p;
			best_nb_channels = nb_channels;
		}
		p++;
	}
	return best_ch_layout;
}


static AVStream *add_audio_stream(AVFormatContext *oc, enum CodecID codec_id)
{
    //AVCodecContext *c;
    AVStream *st;
    AVCodec *codec;

    codecSpeex = codec = avcodec_find_encoder(codec_id);
    if (!(codec)) {
    	LOGE("Could not find codec\n");
    	return NULL;
    }

    LOGE("audio output codec: %s", codec->name);

    st = avformat_new_stream(oc, codec);
    if (!st) {
    	LOGE("Could not alloc audio stream\n");
        return NULL;
    }
    st->id = 1;

    c = st->codec;

    LOGE("add_audio_stream 1, %d, %d",  c->channels, c->frame_size);

    c->codec_id = codec_id;
    c->codec_type = AVMEDIA_TYPE_AUDIO;

    /* put sample parameters */
    c->sample_fmt = AV_SAMPLE_FMT_S16;
    c->bit_rate = 64000;
    //c->sample_rate = 44100;
    //c->channels = 1;

    /* select other audio parameters supported by the encoder */
	c->sample_rate    = 16000;//select_sample_rate(codecSpeex);
	c->channel_layout = AV_CH_LAYOUT_MONO ;//select_channel_layout(codecSpeex);
	c->channels       = av_get_channel_layout_nb_channels(c->channel_layout);

	LOGE("add_audio_stream 2, %d, %d, %d",  c->sample_rate, c->frame_size, c->channels);

    if (!check_sample_fmt(codecSpeex, c->sample_fmt)) {
    	LOGE("Encoder does not support sample format %s",av_get_sample_fmt_name(c->sample_fmt));
        return;
    }

    // some formats want stream headers to be separate
    if (oc->oformat->flags & AVFMT_GLOBALHEADER)
        c->flags |= CODEC_FLAG_GLOBAL_HEADER;

	//LOGE("startRecord+ 5, %d, %d", c->channels, c->frame_size);

	/* open it */
	if (avcodec_open2(c, codecSpeex, NULL) < 0) {
		LOGE("Could not avcodec_open2\n");
		return;
	}

    return st;
}

JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_startRecord(JNIEnv * env, jobject this, int iBufSize)
{
	LOGE("startRecord+, iBufSize:%d\n", iBufSize);
	/* initialize libavcodec, and register all codecs and formats */
	//avcodec_init();
	avcodec_register_all();
	//avfilter_register_all();
	av_register_all();
	avformat_network_init();

	avformat_alloc_output_context2(&oc, NULL, NULL, filename);
	if (!oc) {
		LOGE("Could not deduce output format from file extension: using FLV.\n");
		LOGE("startRecord+ 0, %d\n", oc);
		avformat_alloc_output_context2(&oc, NULL, /*"mpeg"*/"flv", filename);
		LOGE("startRecord+ 01, %d\n", oc);
	}

	if (!oc)  {
		LOGE("oc is null\n");
		return ;
	}

//	codecSpeex = avcodec_find_encoder(CODEC_ID_SPEEX);
//	if (!codecSpeex)
//		LOGE("codecSpeex not found\n");
//		return;
//	}

//	AVCodecContext *cc =avcodec_alloc_context3(codecSpeex);
//	if (!cc) {
//		LOGE("Could not allocate audio codec context\n");
//		return;
//	}
//	LOGE("startRecord+ 4, %d, %d",  c->channels, c->frame_size);
//
//	/* put sample parameters */
//	c->bit_rate = 64000;
//	/* check that the encoder supports s16 pcm input */
//	c->sample_fmt = AV_SAMPLE_FMT_S16;
//	if (!check_sample_fmt(codecSpeex, c->sample_fmt)) {
//		LOGE("Encoder does not support sample format %s",av_get_sample_fmt_name(c->sample_fmt));
//        return;
//	}
//	/* select other audio parameters supported by the encoder */
//	c->sample_rate    = select_sample_rate(codecSpeex);
//	c->channel_layout = select_channel_layout(codecSpeex);
//	c->channels       = av_get_channel_layout_nb_channels(c->channel_layout);
//
//	LOGE("startRecord+ 5, %d, %d", c->channels, c->frame_size);
//
//	/* open it */
//	if (avcodec_open2(c, codecSpeex, NULL) < 0) {
//		LOGE("Could not avcodec_open2\n");
//				return;
//	}

	LOGE("startRecord+ 1\n");
	oc->oformat->audio_codec = CODEC_ID_SPEEX;

	fmt = oc->oformat;

	LOGE("startRecord+ 2\n");
	if (fmt->audio_codec != CODEC_ID_NONE) {
		LOGE("startRecord+ 2-1\n");
		audio_st = add_audio_stream(oc, fmt->audio_codec);
	}

	LOGE("startRecord+ 3, %d\n", audio_st);
	av_dump_format(oc, 0, filename, 1);
	LOGE("startRecord+ 4, %d\n", oc);
	//avformat_write_header(oc, NULL);
	LOGE("startRecord+ 5");
	//AVCodecContext *c;
	//c = audio_st->codec;

	int sample_size = av_get_bytes_per_sample(c->sample_fmt);
	int planar      = av_sample_fmt_is_planar(c->sample_fmt);
	LOGE("startRecord+ 6, %d, %d, %d, %d", sample_size, planar, c->channels, c->frame_size);

    buffer_size = av_samples_get_buffer_size(NULL, c->channels, c->frame_size, c->sample_fmt, 0)*2;
    LOGE("startRecord+ 7, %d", buffer_size);
    samples = av_malloc(buffer_size);
	if (!samples) {
		LOGE("Could not allocate %d bytes for samples buffer\n",buffer_size);
		return;
	}

	//audio_outbuf_size = iBufSize;
	//audio_outbuf = (uint8_t *)av_malloc(audio_outbuf_size);

	/* ugly hack for PCM codecs (will be removed ASAP with new PCM
	    support to compute the input frame size in samples */
//	if (c->frame_size <= 1) {
//		LOGE("startRecord+ 5-1, %d\n", c->channels);
//		audio_input_frame_size = audio_outbuf_size / c->channels;
//		switch(audio_st->codec->codec_id) {
//		case CODEC_ID_PCM_S16LE:
//		case CODEC_ID_PCM_S16BE:
//		case CODEC_ID_PCM_U16LE:
//		case CODEC_ID_PCM_U16BE:
//			audio_input_frame_size >>= 1;
//			break;
//		default:
//			break;
//		}
//	} else {
//		audio_input_frame_size = c->frame_size;
//	}
//	LOGE("startRecord+ 6, %d, %d, %d\n", c->frame_size, audio_input_frame_size, av_get_bytes_per_sample(c->sample_fmt));
//	samples = (int16_t *)av_malloc(audio_input_frame_size *av_get_bytes_per_sample(c->sample_fmt) * c->channels);
//	LOGE("startRecord-\n");
}


static int iCurPos = 0;
JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_recordAudio(JNIEnv * env, jobject this, jbyteArray array, int iBufSize)
{
	LOGE("recordAudio+, iBufSize:%d\n", iBufSize);
	DECLARE_JNIENV_WITHOUT_RETURN()
    //AVCodecContext *c;
    AVPacket pkt;
    AVFrame *frame;
    int got_output;
    av_init_packet(&pkt);
    pkt.data = NULL;
    pkt.size = 0;
//    c = audio_st->codec;

//    if (!check_sample_fmt(audio_st->codec, c->sample_fmt)) {
//    	LOGE("Encoder does not support sample format %s",av_get_sample_fmt_name(c->sample_fmt));
//        return;
//    }
    LOGE("recordAudio+, 0\n");
    frame = avcodec_alloc_frame();
    if (!frame) {
        LOGE("Could not allocate audio frame");
        return;
    }
    LOGE("recordAudio+, 0-1\n");
    frame->nb_samples     = c->frame_size;
    frame->format         = c->sample_fmt;
    frame->channel_layout = c->channel_layout;

	/* setup the data pointers in the AVFrame */
    LOGE("recordAudio+, 0-2\n");
    int ret = avcodec_fill_audio_frame(frame, c->channels, c->sample_fmt,(const uint8_t*)samples, buffer_size, 0);
	if (ret < 0) {
		LOGE("Could not setup audio frame\n");
		return;
	}

	LOGE("recordAudio+, 0-3\n");
    jbyte *bytes = (*jni_env)->GetByteArrayElements(jni_env, array, NULL);
	LOGE("recordAudio+, 1\n");
	//if((iCurPos+iBufSize) < )
	memcpy(samples, bytes, iBufSize); //
	LOGE("recordAudio+, 2\n");
	(*jni_env)->ReleaseByteArrayElements(jni_env, array, bytes, 0);

    //get_audio_frame(samples, audio_input_frame_size, c->channels);

    //pkt.size = avcodec_encode_audio(c, audio_outbuf, audio_outbuf_size, samples);
	ret = avcodec_encode_audio2(c, &pkt, frame, &got_output);
	if (ret < 0) {
		LOGE("Error encoding audio frame\n");
		return;
	}
//	if (got_output) {
//		fwrite(pkt.data, 1, pkt.size, f);
//		av_free_packet(&pkt);
//	}
    LOGE("recordAudio+, 3, %d, %d, %d, %d, %d \n", pkt.size, got_output, pkt.flags, pkt.stream_index, pkt.pts);
    if (c->coded_frame && c->coded_frame->pts != AV_NOPTS_VALUE)
        pkt.pts= av_rescale_q(c->coded_frame->pts, c->time_base, audio_st->time_base);
//    LOGE("recordAudio+, 4, %d\n", pkt.pts);
    pkt.flags |= AV_PKT_FLAG_KEY;
    pkt.stream_index = audio_st->index;
    //pkt.data = audio_outbuf;
    LOGE("recordAudio+, 4, %d, %d, %d, %d, %d \n", pkt.size, got_output, pkt.flags, pkt.stream_index, pkt.pts);
    /* write the compressed frame in the media file */
    //if (av_interleaved_write_frame(oc, &pkt) != 0) {
    if (av_write_frame(oc, &pkt) != 0) {
        LOGE("Error while writing audio frame\n");
    }
    LOGE("recordAudio+, 4, %d\n", pkt.size);
    av_freep(&samples);
    av_free_packet(&pkt);
    //avcodec_free_frame(&frame);

    LOGE("recordAudio-");
}

JNIEXPORT void JNICALL Java_com_churnlabs_ffmpegsample_MainActivity_endRecord(JNIEnv * env, jobject this)
{
	av_write_trailer(oc);

	/* close each codec */
	if (audio_st)
		close_audio(oc, audio_st);

	/* free the streams */
	int i;
	for(i = 0; i < oc->nb_streams; i++) {
		av_freep(&oc->streams[i]->codec);
		av_freep(&oc->streams[i]);
	}

	/* free the stream */
	av_free(oc);
}

//static void get_audio_frame(int16_t *samples, int frame_size, int nb_channels)
//{
////    int j, i, v;
////    int16_t *q;
////
////    q = samples;
////    for (j = 0; j < frame_size; j++) {
////        v = (int)(sin(t) * 10000);
////        for(i = 0; i < nb_channels; i++)
////            *q++ = v;
////        t += tincr;
////        tincr += tincr2;
////    }
//}

//static void write_audio_frame(AVFormatContext *oc, AVStream *st)
//{
//    AVCodecContext *c;
//    AVPacket pkt;
//    av_init_packet(&pkt);
//
//    c = st->codec;
//
//    get_audio_frame(samples, audio_input_frame_size, c->channels);
//
//    pkt.size = avcodec_encode_audio(c, audio_outbuf, audio_outbuf_size, samples);
//
//    if (c->coded_frame && c->coded_frame->pts != AV_NOPTS_VALUE)
//        pkt.pts= av_rescale_q(c->coded_frame->pts, c->time_base, st->time_base);
//    pkt.flags |= AV_PKT_FLAG_KEY;
//    pkt.stream_index = st->index;
//    pkt.data = audio_outbuf;
//
//    /* write the compressed frame in the media file */
//    if (av_interleaved_write_frame(oc, &pkt) != 0) {
//        LOGE("Error while writing audio frame\n");
//    }
//}


int seek_frame(int tsms)
{
    int64_t frame;

    frame = av_rescale(tsms,pFormatCtx->streams[videoStream]->time_base.den,pFormatCtx->streams[videoStream]->time_base.num);
    frame/=1000;

    if(avformat_seek_file(pFormatCtx,videoStream,0,frame,frame,AVSEEK_FLAG_FRAME)<0) {
        return 0;
    }

    avcodec_flush_buffers(pCodecCtx);

    return 1;
}
