#pragma once

#ifdef __cplusplus
 extern "C" {

#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#include <stdio.h>
#endif

#include "utils.h"
#include "librtmp_ext.h"
#include "pkt_queue.h"
#include "beseye_rtmp_observer.h"

#include "SDL.h"
#include "SDL_thread.h"

class CBeseyeAudioStreamer: public CBeseyeRTMPObserver{
public:
	static CBeseyeAudioStreamer& getInstance();
 	virtual ~CBeseyeAudioStreamer();

 	virtual int setStreamingInfo(char* path, const char* pipePath);
    virtual int setStreamingInfo(char* path, int iReadFd, int iWriteFd, void(* pipeDeinitCallback)(void));
 	virtual int startAudioStreaming();
 	virtual int isStreamingPlaying();

 	virtual int writeAudioBuffer(char* buf, const int iBufSize);
 	virtual int closeAudioStreaming();

 	//internal use
 	static int checkExit();//1: still in loop
 	virtual int feedAudioBuffer();
 	int isFifoValid();
 	virtual void invokeRtmpStreamMethodCallback(const AVal*, const AVal*, void* extra);
 	virtual void invokeRtmpStatusCallback(int iStatus, void* extra);
    virtual void invokeRtmpErrorCallback(int iError, void* extra);

 	void invokeAVFormatContextCB(AVFormatContext *pFCtx);

private:
 	CBeseyeAudioStreamer();
 	//CBeseyeAudioStreamer(char* path, const char* fifoPath);
 	//CBeseyeAudioStreamer(CBeseyeAudioStreamer const&);              // Don't Implement
 	void operator=(CBeseyeAudioStreamer const&); // Don't implement
 	int openFifo();
 	int closeFifo();

 	void deinitParams();
 	void constructResource();
 	void destroyResource();
 	void signalCond();

 	static void initLoopCtrl();
 	static void deinitLoopCtrl();

private:
 	char* mAudioStreamPath;
 	const char* mFifoPath;
 	int mIsFifoOpen;
 	int mPipeRFD;
 	int mPipeWFD;
 	AVFormatContext* mpFCtx;

 	int mTriggerExit;

 	enum Stream_Status mStream_Status;

 	SDL_Thread *mAudioTid;
 	PacketQueue mAudioQueue;
 	SDL_mutex * mAudioMutex;
 	SDL_cond * mAudioCond;

 	void(* mPipeDeinitCallback)(void) ;

 	static int sInStreamingLoop;
 	static SDL_mutex * sLoopCtrlMutex;
 	static SDL_cond * sLoopCtrlCond;
};

int audio_thread(void*);

//For invoke ffmpeg
int runAudioStreaming(int argc, char **argv, void(* cb)(AVFormatContext*, void*), void (*rtmpStatusCallback)(void* , int , void*), void* userdata);
void onAVFormatContextCB(AVFormatContext *pFCtx, void* userdata);
void sigterm_handler_wrapper(int sig);
void sigterm_handler_reset();

#ifdef __cplusplus
 }
#endif
