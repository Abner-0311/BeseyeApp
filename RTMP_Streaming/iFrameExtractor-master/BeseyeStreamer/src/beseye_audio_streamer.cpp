#ifdef __cplusplus
 extern "C" {
#endif

#include <errno.h>
#include "beseye_audio_streamer.h"
     
//#ifndef _ANDROID
//#import <UIKit/UIKit.h>
//#endif

#ifdef __cplusplus
 }
#endif

//CBeseyeAudioStreamer::mFifoPath = "/data/data/com.churnlabs.ffmpegsample/beseye.fifo";
int CBeseyeAudioStreamer::sInStreamingLoop = 0;
SDL_mutex * CBeseyeAudioStreamer::sLoopCtrlMutex = SDL_CreateMutex();
SDL_cond * CBeseyeAudioStreamer::sLoopCtrlCond = SDL_CreateCond();

CBeseyeAudioStreamer::CBeseyeAudioStreamer():
mAudioStreamPath(NULL),
mFifoPath(NULL),
mIsFifoOpen(0),
mPipeRFD(0),
mPipeWFD(0),
mTriggerExit(0),
mStream_Status(STREAM_UNINIT),
mpFCtx(NULL),
mAudioTid(NULL),
mAudioMutex(NULL),
mAudioCond(NULL){

}

CBeseyeAudioStreamer::~CBeseyeAudioStreamer(){
	av_log(NULL, AV_LOG_INFO, "CBeseyeAudioStreamer::~CBeseyeAudioStreamer()++\n");
	closeAudioStreaming();
    destroyResource();
	closeFifo();
	deinitParams();
	deinitLoopCtrl();
	av_log(NULL, AV_LOG_INFO, "CBeseyeAudioStreamer::~CBeseyeAudioStreamer()--\n");
}

void CBeseyeAudioStreamer::signalCond(){
	if(mAudioMutex){
		SDL_LockMutex(mAudioMutex);
		if(mAudioCond)
			SDL_CondSignal(mAudioCond);
		//av_log(NULL, AV_LOG_ERROR,"signalCond()\n");
		SDL_UnlockMutex(mAudioMutex);
	}
}

void CBeseyeAudioStreamer::constructResource(){
	//destroyResource();

	mAudioMutex = SDL_CreateMutex();
	mAudioCond  = SDL_CreateCond();
	packet_queue_init(&mAudioQueue);
	packet_queue_start(&mAudioQueue);

	mAudioTid = SDL_CreateThread(audio_thread, "audio_thread", this);
}

void CBeseyeAudioStreamer::destroyResource(){
	if(mAudioTid){
		packet_queue_abort(&mAudioQueue);
		signalCond();
		av_log(NULL, AV_LOG_INFO, "destroyResource(), SDL_WaitThread mAudioTid in\n");
		SDL_WaitThread(mAudioTid, NULL);
		av_log(NULL, AV_LOG_INFO, "destroyResource(), SDL_WaitThread mAudioTid out\n");
		mAudioTid = NULL;

		packet_queue_abort(&mAudioQueue);
		packet_queue_flush(&mAudioQueue);
		packet_queue_destroy(&mAudioQueue);
	}

	if(NULL != mAudioMutex){
		SDL_DestroyMutex(mAudioMutex);
		mAudioMutex = NULL;
	}

	if(NULL != mAudioCond){
		SDL_DestroyCond(mAudioCond);
		mAudioCond = NULL;
	}

	av_log(NULL, AV_LOG_INFO, "destroyResource()--\n");
}

int CBeseyeAudioStreamer::setStreamingInfo(char* path, const char* fifoPath){
	int iRet = 0;
	if(NULL != path && 0 < strlen(path) && NULL != fifoPath && strlen(fifoPath)){
		mAudioStreamPath = path;
		mFifoPath = fifoPath;
		iRet = 1;
	}else{
		av_log(NULL, AV_LOG_ERROR, "setStreamingInfo(), invalid info\n");
	}

	return iRet;
}

int CBeseyeAudioStreamer::setStreamingInfo(char* path, int iReadFd, int iWriteFd, void(* pipeDeinitCallback)(void) ){
    int iRet = 0;
	if(NULL != path && 0 < strlen(path) && 0 < iReadFd && 0 < iWriteFd && pipeDeinitCallback){
		mAudioStreamPath = path;
		//mFifoPath = fifoPath;
		mPipeDeinitCallback = pipeDeinitCallback;
        mPipeRFD = iReadFd;
        mPipeWFD = iWriteFd;
        mIsFifoOpen = 1;
		iRet = 1;
	}else{
		av_log(NULL, AV_LOG_ERROR, "setStreamingInfo(), invalid info\n");
	}
    
	return iRet;
}

int CBeseyeAudioStreamer::startAudioStreaming(){
	int iRet = 0;
	if(!(NULL != mAudioStreamPath && 0 < strlen(mAudioStreamPath) && ((NULL != mFifoPath && strlen(mFifoPath)) || (0 < mPipeRFD && 0 < mPipeWFD)))){
		av_log(NULL, AV_LOG_ERROR, "startAudioStreaming(), invalid info, call setStreamingInfo() first\n");
		goto EXIT;
	}

	SDL_LockMutex(sLoopCtrlMutex);
	if(!checkExit()){
		av_log(NULL, AV_LOG_ERROR, "startAudioStreaming(), previous loop didn't end, call closeStreaming() and wait\n");
		SDL_UnlockMutex(sLoopCtrlMutex);
		goto EXIT;
	}
	SDL_UnlockMutex(sLoopCtrlMutex);

	if(openFifo()){
		char* pipeFile = (char*)malloc(sizeof(char*)*256);
		memset(pipeFile, 0, sizeof(pipeFile));
		sprintf(pipeFile, "pipe:%d", mPipeRFD);

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
		//						  "rtmp://192.168.2.224/myapp/mystream"};


		//char* argv[]={"ffmpeg", "-re", "-f", "amr","-acodec", "aac", "-ar", "44100", "-ac", "1", "-i", pipeFile, "-c", "copy", "-ar","-f", "flv", "rtmp://192.168.2.106/myapp/mystream"};
		//char* argv[]={"ffmpeg", "-re", "-i", "/sdcard/test2.mp3", "-acodec", "libspeex", "-ar", "16000","-f", "flv", "rtmp://192.168.2.106/myapp/mystream"};

		if(NULL != mAudioStreamPath){
			char* argv[]={"ffmpeg", "-re","-vn", "-f", "s16le","-sample_fmt","AV_SAMPLE_FMT_S16","-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1", "-i", pipeFile,
						//"-acodec", "pcm_alaw", "-ar","8000","-ab","64k",  //bitrate: 64 kb/s -> poor quality
						//"-acodec", "pcm_mulaw", "-ar","8000","-ab","64k", //bitrate: 64 kb/s -> poor quality, better than alaw
						//"-acodec", "adpcm_swf", "-ar","11025", //bitrate: 44 kb/s -> normal quality, some noise
						"-acodec", "aac", "-ar", "44100", "-ac", "1", "-ab", "32k", "-strict", "-2",//bitrate: 32 kb/s, aac -> good quality
						"-f", "flv",
						mAudioStreamPath
						/*"rtmp://192.168.2.224:1935/myapp/audiostream"*/};

			int argc = sizeof(argv)/sizeof(char*);
			sigterm_handler_wrapper(0);
			sigterm_handler_reset();

			//initLoopCtrl();
			constructResource();
			if(sLoopCtrlMutex){
				SDL_LockMutex(sLoopCtrlMutex);
				sInStreamingLoop =1;
				SDL_UnlockMutex(sLoopCtrlMutex);
			}

			set_Stream_Status(STREAM_INIT);
			iRet = runAudioStreaming(argc, argv, onAVFormatContextCB, rtmpStatusCallback, this);

			if(sLoopCtrlMutex){
				SDL_LockMutex(sLoopCtrlMutex);
				sInStreamingLoop = 0;
				if(sLoopCtrlCond)
					SDL_CondSignal(sLoopCtrlCond);
				SDL_UnlockMutex(sLoopCtrlMutex);
			}

			signalCond();
		}else{
			av_log(NULL, AV_LOG_ERROR,"startAudioStreaming(), mAudioStreamPath is null\n");
		}
	}else{
		av_log(NULL, AV_LOG_ERROR,"startAudioStreaming(), failed to open fifo\n");
	}
EXIT:
	av_log(NULL, AV_LOG_INFO,"startAudioStreaming()--\n");
	return iRet;
}

void CBeseyeAudioStreamer::initLoopCtrl(){
	sLoopCtrlMutex = SDL_CreateMutex();
	sLoopCtrlCond  = SDL_CreateCond();
}

void CBeseyeAudioStreamer::deinitLoopCtrl(){
	if(NULL != sLoopCtrlMutex){
		SDL_DestroyMutex(sLoopCtrlMutex);
		sLoopCtrlMutex = NULL;
	}

	if(NULL != sLoopCtrlCond){
		SDL_DestroyCond(sLoopCtrlCond);
		sLoopCtrlCond = NULL;
	}
}

CBeseyeAudioStreamer& CBeseyeAudioStreamer::getInstance(){
	static CBeseyeAudioStreamer singleton;
	return singleton;
}

int CBeseyeAudioStreamer::checkExit(){
	int iRet = 1;
	if(sLoopCtrlMutex){
		SDL_LockMutex(sLoopCtrlMutex);
		av_log(NULL, AV_LOG_ERROR,"checkExit(), sInStreamingLoop:%d\n", sInStreamingLoop);
		if(sInStreamingLoop){
//			av_log(NULL, AV_LOG_ERROR,"checkExit(), wait.......\n");
//			SDL_CondWait(sLoopCtrlCond, sLoopCtrlMutex);
//			av_log(NULL, AV_LOG_ERROR,"checkExit(), wake.......\n");
			iRet = 0;
		}
		SDL_UnlockMutex(sLoopCtrlMutex);
	}
	return iRet;
}

int CBeseyeAudioStreamer::isStreamingPlaying(){
	return (sInStreamingLoop || mpFCtx != NULL) && mStream_Status < STREAM_CLOSE && !mTriggerExit;
}

int CBeseyeAudioStreamer::isFifoValid(){
	return mIsFifoOpen && mPipeRFD && mPipeWFD;
}

int CBeseyeAudioStreamer::openFifo(){
	int iRet = 0;
	av_log(NULL, AV_LOG_ERROR,"openFifo()+, open pipe\n\n");

	SDL_LockMutex(mAudioMutex);
	if(!mIsFifoOpen && NULL != mFifoPath){

		if (mkfifo(mFifoPath, 0666) < 0 && errno != EEXIST) {
			av_log(NULL, AV_LOG_ERROR,"openFifo(), mfifo failed, errno:%d\n", errno);
			goto ERR;
		}
        mIsFifoOpen = 1;
	}else{
		av_log(NULL, AV_LOG_INFO,"openFifo(), mIsFifoOpen is true\n");
	}
	SDL_UnlockMutex(mAudioMutex);
    
	SDL_LockMutex(mAudioMutex);
	if(!mPipeRFD){
		if ((mPipeRFD = open(mFifoPath, O_RDONLY|O_NONBLOCK)) == -1) {
			av_log(NULL, AV_LOG_ERROR,"openFifo(), open pipeRFD failed\n");
			goto ERR;
		}
	}else{
		av_log(NULL, AV_LOG_INFO,"openFifo(), pipeRFD exists\n");
	}
	SDL_UnlockMutex(mAudioMutex);
    
	SDL_LockMutex(mAudioMutex);
	if(!mPipeWFD){
		if ((mPipeWFD= open(mFifoPath, O_WRONLY|O_NONBLOCK)) == -1) {
			av_log(NULL, AV_LOG_ERROR,"openFifo(), open pipeWFD failed\n");
			goto ERR;
		}
	}else{
		av_log(NULL, AV_LOG_INFO,"openFifo(), pipeWFD exists\n");
	}
	SDL_UnlockMutex(mAudioMutex);

	iRet =1;
ERR:
	return iRet;
}

int CBeseyeAudioStreamer::closeFifo(){
	int iRet = 0;
	SDL_LockMutex(mAudioMutex);
	if(0 < mPipeRFD){
		if(0 > close(mPipeRFD)){
			av_log(NULL, AV_LOG_ERROR,"closeFifo(), close pipeRFD failed\n");
		}
		mPipeRFD = 0;
	}
	SDL_CondSignal(mAudioCond);
	SDL_UnlockMutex(mAudioMutex);

	SDL_LockMutex(mAudioMutex);
	if(0 < mPipeWFD){
		if(0 > close(mPipeWFD)){
			av_log(NULL, AV_LOG_ERROR,"closeFifo(), close pipeWFD failed\n");
		}
		mPipeWFD = 0;
	}
	SDL_CondSignal(mAudioCond);
	SDL_UnlockMutex(mAudioMutex);

	SDL_LockMutex(mAudioMutex);
	if(mIsFifoOpen){
		unlink(mFifoPath);
		mIsFifoOpen = 0;
	}

	if(mPipeDeinitCallback){
		mPipeDeinitCallback();
		mPipeDeinitCallback = NULL;
	}
	SDL_CondSignal(mAudioCond);
	SDL_UnlockMutex(mAudioMutex);

	iRet = 1;
	return iRet ;
}

int CBeseyeAudioStreamer::writeAudioBuffer(char* buf, const int iBufSize){
	int iRet = 0;
	if(isStreamingPlaying() && isFifoValid()){
		AVPacket pkt = {0};
		uint8_t* dupmem = (uint8_t*)malloc(sizeof(uint8_t)*iBufSize);
		if(!dupmem){
			av_log(NULL, AV_LOG_ERROR,"writeAudioBuffer(), failed to duplicate memory\n");
			goto EXIT;
		}
		memcpy(dupmem, buf, iBufSize);
		pkt.data = dupmem;
		pkt.size = iBufSize;

		av_log(NULL, AV_LOG_DEBUG,"writeAudioBuffer(), packet_queue_put, size:%d\n", pkt.size);
		packet_queue_put(&mAudioQueue, &pkt);
		signalCond();
	}else{
		av_log(NULL, AV_LOG_ERROR,"writeAudioBuffer(), invalid state\n");
	}
EXIT:
	return iRet;
}

int CBeseyeAudioStreamer::closeAudioStreaming(){
	av_log(NULL, AV_LOG_ERROR,"closeAudioStreaming()++\n");
	int iRet = 0;
	mTriggerExit = 1;
    sigterm_handler_wrapper(1);
	av_log(NULL, AV_LOG_ERROR,"closeAudioStreaming()--\n");
	return iRet;
}

void CBeseyeAudioStreamer::deinitParams(){
	mTriggerExit = 0;
	mAudioStreamPath = NULL;
	mIsFifoOpen = 0;
	mPipeRFD = 0;
	mPipeWFD = 0;
	mpFCtx = NULL;
}

void CBeseyeAudioStreamer::invokeRtmpStreamMethodCallback(const AVal* method, const AVal* content, void* extra){
	av_log(NULL, AV_LOG_ERROR, "CBeseyeAudioStreamer::invokeRtmpStreanMethodCallback(), method:%s, content:%s\n",(NULL != method)?method->av_val:"", (NULL != content)?content->av_val:"\n");
}

void CBeseyeAudioStreamer::invokeRtmpStatusCallback(int iStatus, void* extra){
	av_log(NULL, AV_LOG_ERROR, "CBeseyeAudioStreamer::invokeRtmpStatusCallback(), iStatus:%d\n",iStatus);
	triggerPlayCB(CBeseyeRTMPObserver::STREAM_STATUS_CB, NULL, iStatus, 0);

	signalCond();

	if(mStream_Status == STREAM_CLOSE){
		mpFCtx = NULL;
		closeAudioStreaming();
		set_Stream_Status(STREAM_PLAYING);//workaround
	}
}

void CBeseyeAudioStreamer::invokeRtmpErrorCallback(int iError, void* extra){
    av_log(NULL, AV_LOG_INFO, "CBeseyeAudioStreamer::invokeRtmpErrorCallback(), iError:%d",iError);
	triggerPlayCB(CBeseyeRTMPObserver::ERROR_CB, NULL, INTERNAL_STREAM_ERR, iError);
}

int CBeseyeAudioStreamer::feedAudioBuffer(){
	av_log(NULL, AV_LOG_INFO, "feedAudioBuffer()++\n");
	int iRet = 0;
	AVPacket pkt1, *pkt = &pkt1;
	for(;;){
//		if(!isStreamingPlaying() || !isFifoValid()){
//			av_log(NULL, AV_LOG_ERROR, "feedAudioBuffer(), break due to invalid state\n");
//			SDL_UnlockMutex(mAudioMutex);
//			break;
//		}
		SDL_LockMutex(mAudioMutex);
		av_log(NULL, AV_LOG_DEBUG,"feedAudioBuffer(), wait.......\n");
		SDL_CondWait(mAudioCond, mAudioMutex);
		av_log(NULL, AV_LOG_DEBUG,"feedAudioBuffer(), wake.......\n");
		SDL_UnlockMutex(mAudioMutex);

		while(packet_queue_get(&mAudioQueue, pkt, 1)){
			av_log(NULL, AV_LOG_DEBUG,"feedAudioBuffer(), packet_queue_get\n");
			if(NULL != pkt->data && 0 < pkt->size){
				int ret = 0, iTrial = 0;
				do{
					if(isStreamingPlaying() && isFifoValid()){
						//av_log(NULL, AV_LOG_ERROR,"feedAudioBuffer(), write in\n");
						ret = write(mPipeWFD,pkt->data,pkt->size);
						av_log(NULL, AV_LOG_DEBUG,"feedAudioBuffer(), write out, ret:%d", ret);
						if (0 > ret) {
							SDL_Delay(10);
							av_log(NULL, AV_LOG_WARNING,"feedAudioBuffer(), iTrial = %d^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^",++iTrial);
						}else{
							iRet = 1;
						}
					}else{
						av_log(NULL, AV_LOG_ERROR, "feedAudioBuffer(), break due to invalid state 2\n");
						break;
					}
				}while(0 > ret && iTrial < 5);

				free(pkt->data);
				pkt->data=NULL;
			}else{
				av_log(NULL, AV_LOG_ERROR,"feedAudioBuffer(), pkt->data:%d, pkt->size:%d\n",pkt->data, pkt->size);
			}

			if(!isStreamingPlaying() || !isFifoValid()){
				av_log(NULL, AV_LOG_ERROR, "feedAudioBuffer(), break due to invalid state 3\n");
				break;
			}
			//av_log(NULL, AV_LOG_DEBUG,"feedAudioBuffer(), packet_queue_get in\n");
		}

		if(!isStreamingPlaying() || !isFifoValid()){
			av_log(NULL, AV_LOG_ERROR, "feedAudioBuffer(), break due to invalid state 4\n");
			SDL_UnlockMutex(mAudioMutex);
			break;
		}
	}
	av_log(NULL, AV_LOG_INFO, "feedAudioBuffer()--\n");
	return iRet;
}

int audio_thread(void* userdata){
	int iRet = 0;
	//av_log(NULL, AV_LOG_INFO, "audio_thread()++, userdata:%d",userdata);
	CBeseyeAudioStreamer* streamer = (CBeseyeAudioStreamer*)userdata;
	if(streamer){
		streamer->feedAudioBuffer();
	}
	//av_log(NULL, AV_LOG_INFO, "audio_thread()--, userdata:%d",userdata);
	return iRet;
}

void CBeseyeAudioStreamer::invokeAVFormatContextCB(AVFormatContext *pFCtx){
	av_log(NULL, AV_LOG_INFO, "CBeseyeAudioStreamer::invokeAVFormatContextCB()++, pFCtx:%d",pFCtx);
	if(NULL == mpFCtx && pFCtx){
		registerRtmpCallback(pFCtx);
	}
}

void onAVFormatContextCB(AVFormatContext *pFCtx, void* userdata){
	CBeseyeAudioStreamer* streamer = (CBeseyeAudioStreamer*)userdata;
	if(streamer){
		streamer->invokeAVFormatContextCB(pFCtx);
	}
}
