#pragma once

#ifdef __cplusplus
 extern "C" {

#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#endif

#include "utils.h"
#include "librtmp_ext.h"

class CBeseyeRTMPObserver{
public:
	CBeseyeRTMPObserver();
	virtual ~CBeseyeRTMPObserver();
	virtual Stream_Status get_Stream_Status();

	enum Player_Callback {
		STREAM_STATUS_CB,
		ERROR_CB
	};

	enum Player_Major_Error {
		INTERNAL_STREAM_ERR,//refer to Stream_Error in rtmp.h
		NO_NETWORK_ERR,
		NOMEM_CB,
		UNKNOWN_ERR
	};

    //For client notifications
	virtual void registerCallback(void(* cb)(CBeseyeRTMPObserver *, CBeseyeRTMPObserver::Player_Callback, const char *, int, int) );
	virtual void unregisterCallback();

	//forward function
	virtual void triggerPlayCB(CBeseyeRTMPObserver::Player_Callback cb, const char * msg, int iMajorType, int iMinorType);

    //For internal rtmp status notifications
	virtual void registerRtmpCallback(AVFormatContext *pFCtx);
	virtual void unregisterRtmpCallback(AVFormatContext *pFCtx);

	virtual void invokeRtmpStreamMethodCallback(const AVal*, const AVal*, void* extra) = 0;
	virtual void invokeRtmpStatusCallback(int iStatus, void* extra) = 0;
    virtual void invokeRtmpErrorCallback(int iError, void* extra) = 0;

protected:
	virtual void set_Stream_Status(Stream_Status status);

	enum Stream_Status mStream_Status;
	void(* mPlayCB)(CBeseyeRTMPObserver *, Player_Callback, const char *, int, int) ;

};

//callback functions for librtmp
static const AVal RTMP_RESULT = AVC("_result");
static const AVal RTMP_ON_STATUS = AVC("onStatus");

static const AVal av_NetStream_Failed = AVC("NetStream.Failed");
static const AVal av_NetStream_Play_Failed = AVC("NetStream.Play.Failed");
static const AVal av_NetStream_Play_StreamNotFound = AVC("NetStream.Play.StreamNotFound");
static const AVal av_NetConnection_Connect_InvalidApp = AVC("NetConnection.Connect.InvalidApp");
static const AVal av_NetStream_Play_Start = AVC("NetStream.Play.Start");
static const AVal av_NetStream_Play_Complete = AVC("NetStream.Play.Complete");
static const AVal av_NetStream_Play_Stop = AVC("NetStream.Play.Stop");
static const AVal av_NetStream_Seek_Notify = AVC("NetStream.Seek.Notify");
static const AVal av_NetStream_Pause_Notify = AVC("NetStream.Pause.Notify");
static const AVal av_NetStream_Play_UnpublishNotify = AVC("NetStream.Play.UnpublishNotify");
static const AVal av_NetStream_Publish_Start = AVC("NetStream.Publish.Start");

static const AVal av_description = AVC("description");
static const AVal av_details = AVC("details");

void rtmpStreamMethodCallback(void* , const AVal*, const AVal*, void*);
void rtmpStatusCallback(void* , int iStatus, void* extra);
void rtmpErrorCallback(void* , int iError, void* extra);

#ifdef __cplusplus
 }
#endif
