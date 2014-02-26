#ifdef __cplusplus
 extern "C" {
#endif

#include <errno.h>
#include "beseye_rtmp_observer.h"

#ifdef __cplusplus
 }
#endif

CBeseyeRTMPObserver::CBeseyeRTMPObserver():
mPlayCB(NULL),
mStream_Status(STREAM_UNINIT){

}

CBeseyeRTMPObserver::~CBeseyeRTMPObserver(){
	mStream_Status = STREAM_UNINIT;
	av_log(NULL, AV_LOG_INFO,"CBeseyeRTMPObserver::~CBeseyeRTMPObserver()--\n");
}

Stream_Status CBeseyeRTMPObserver::get_Stream_Status(){
	return mStream_Status;
}

void CBeseyeRTMPObserver::set_Stream_Status(Stream_Status status){
	av_log(NULL, AV_LOG_INFO,"set_Stream_Status(), status:%d\n", status);
	mStream_Status = status;
}

void CBeseyeRTMPObserver::registerCallback(void(* cb)(CBeseyeRTMPObserver *, CBeseyeRTMPObserver::Player_Callback, const char *, int, int) ){
	if(NULL != mPlayCB){
		av_log(NULL, AV_LOG_WARNING, "registerCallback(), remove previous mPlayCB:%d\n", mPlayCB);
	}
	mPlayCB = cb;
}

void CBeseyeRTMPObserver::unregisterCallback(){
	mPlayCB = NULL;
}

void CBeseyeRTMPObserver::triggerPlayCB(CBeseyeRTMPObserver::Player_Callback pCb, const char * msg, int iMajorType, int iMinorType){
	//av_log(NULL, AV_LOG_INFO, "void CBeseyeRTMPObserver::triggerPlayCB(), pCb:%d, iDetailType:%d, msg:%s\n", pCb, iDetailType, (NULL != msg)?msg:"");

	if(pCb == STREAM_STATUS_CB){
		set_Stream_Status((Stream_Status)iMajorType);
	}

	if(NULL != mPlayCB){
		mPlayCB(this, pCb, msg, iMajorType, iMinorType);
	}
}

void CBeseyeRTMPObserver::registerRtmpCallback(AVFormatContext *pFCtx){
	if(NULL != pFCtx){
		AVIOContext* ioCtx =  pFCtx->pb;
		if(NULL != ioCtx){
			URLContext* urlCtx = (URLContext*)ioCtx->opaque;
			if(NULL != urlCtx){
				av_log(NULL, AV_LOG_INFO,"registerRtmpCallback(), called\n");
				register_librtmp_CB(urlCtx, rtmpStreamMethodCallback, rtmpStatusCallback, rtmpErrorCallback, this);
			}else{
				av_log(NULL, AV_LOG_ERROR,"registerRtmpCallback(), urlCtx is null\n");
			}
		}else{
			av_log(NULL, AV_LOG_ERROR,"registerRtmpCallback(), ioCtx is null\n");
		}
	}else{
		av_log(NULL, AV_LOG_ERROR,"registerRtmpCallback(), pFCtx is null\n");
	}
}

void CBeseyeRTMPObserver::unregisterRtmpCallback(AVFormatContext *pFCtx){
	if(NULL != pFCtx){
		AVIOContext* ioCtx =  pFCtx->pb;
		if(NULL != ioCtx){
			URLContext* urlCtx = (URLContext*)ioCtx->opaque;
			if(NULL != urlCtx){
				av_log(NULL, AV_LOG_INFO,"unregisterRtmpCallback(), called\n");
				register_librtmp_CB(urlCtx, NULL, NULL, NULL,NULL);
			}else{
				av_log(NULL, AV_LOG_ERROR,"unregisterRtmpCallback(), urlCtx is null\n");
			}
		}else{
			av_log(NULL, AV_LOG_ERROR,"unregisterRtmpCallback(), ioCtx is null\n");
		}
	}else{
		av_log(NULL, AV_LOG_ERROR,"unregisterRtmpCallback(), pFCtx is null\n");
	}
}

void rtmpStreamMethodCallback(void* observer, const AVal* method, const AVal* content, void* extra){
	CBeseyeRTMPObserver* cObserver = (CBeseyeRTMPObserver*)observer;
    //av_log(NULL, AV_LOG_ERROR,"rtmpStreamMethodCallback(), observer is %x\n", observer);

	if(NULL != cObserver){
		cObserver->invokeRtmpStreamMethodCallback(method, content, extra);
	}
}

void rtmpStatusCallback(void* observer, int iStatus, void* extra){
	CBeseyeRTMPObserver* cObserver = (CBeseyeRTMPObserver*)observer;
	if(NULL != cObserver){
		cObserver->invokeRtmpStatusCallback(iStatus, extra);
	}
}

void rtmpErrorCallback(void* observer, int iError, void* extra){
    CBeseyeRTMPObserver* cObserver = (CBeseyeRTMPObserver*)observer;
	if(NULL != cObserver){
		cObserver->invokeRtmpErrorCallback(iError, extra);
	}
}
