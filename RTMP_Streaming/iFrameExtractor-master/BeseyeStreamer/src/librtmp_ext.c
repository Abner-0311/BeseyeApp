#include "librtmp_ext.h"

typedef struct LibRTMPContext {
    const AVClass *class;
    RTMP rtmp;
    char *app;
    char *playpath;
} LibRTMPContext;


#define SAVC(x)	static const AVal av_##x = AVC(#x)

SAVC(play);
SAVC(createStream);

int gen_play_wrapper(URLContext *urlCtx, const char *path){
	//av_log(NULL, AV_LOG_INFO,"gen_play_wrapper()+, path: '%s', \n", path);
	int iRet = 0;
	LibRTMPContext *ctx = urlCtx->priv_data;
	RTMP *r = &ctx->rtmp;
    if(r){
    	iRet = gen_play_wrapper_rtmp(r, path);
    }
EXIT:
	av_log(NULL, AV_LOG_INFO,"gen_play_wrapper()-, iRet: '%d', \n", iRet);
    return iRet;
}

int gen_play_wrapper_rtmp(void *rtmpCtx, const char *path){
	av_log(NULL, AV_LOG_INFO,"gen_play_wrapper_rtmp()+, path: '%s', \n", path);
	int iRet = 0;
	RTMP *r = (RTMP*)rtmpCtx;
    if(r){
      r->m_numInvokes--;
      //rt->playpath = path;

      RTMPPacket packet;
	  char pbuf[1024], *pend = pbuf + sizeof(pbuf);
	  char *enc;

	  packet.m_nChannel = 0x08;	/* we make 8 our stream channel */
	  packet.m_headerType = RTMP_PACKET_SIZE_MEDIUM;
	  packet.m_packetType = 0x11;	/* INVOKE */
	  packet.m_nTimeStamp = 0;
	  packet.m_nInfoField2 = r->m_stream_id;	/*0x01000000; */
	  packet.m_hasAbsTimestamp = 0;
	  packet.m_body = pbuf + RTMP_MAX_HEADER_SIZE;

	  enc = packet.m_body;
	  *enc++ = 0;
	  enc = AMF_EncodeString(enc, pend, &av_play);
	  enc = AMF_EncodeNumber(enc, pend, /*++r->m_numInvokes*/0);
	  *enc++ = AMF_NULL;

	  AVal av_pathOut = {path, strlen(path)};
	  //RTMP_ParsePlaypath(&av_path, &av_pathOut);
	  //av_log(urlCtx, AV_LOG_INFO, "gen_play_wrapper, vaav_pathOutl: '%s', len: %d \n", av_pathOut.av_val, av_pathOut.av_len);
	  //av_log(NULL, AV_LOG_INFO,"gen_play_wrapper, vaav_pathOutl: '%s', len: %d \n", av_pathOut.av_val, av_pathOut.av_len);
	  enc = AMF_EncodeString(enc, pend, &av_pathOut);

	  if (!enc)
		  goto EXIT;

	  /* Optional parameters start and len.
	   *
	   * start: -2, -1, 0, positive number
	   *  -2: looks for a live stream, then a recorded stream,
	   *      if not found any open a live stream
	   *  -1: plays a live stream
	   * >=0: plays a recorded streams from 'start' milliseconds
	   */
	  if (r->Link.lFlags & RTMP_LF_LIVE)
		enc = AMF_EncodeNumber(enc, pend, -1000.0);
	  else
		{
		  if (r->Link.seekTime > 0.0)
		enc = AMF_EncodeNumber(enc, pend, r->Link.seekTime);	/* resume from here */
		  else
		enc = AMF_EncodeNumber(enc, pend, 0.0);	/*-2000.0);*/ /* recorded as default, -2000.0 is not reliable since that freezes the player if the stream is not found */
		}
	  if (!enc)
		  goto EXIT;

	  /* len: -1, 0, positive number
	   *  -1: plays live or recorded stream to the end (default)
	   *   0: plays a frame 'start' ms away from the beginning
	   *  >0: plays a live or recoded stream for 'len' milliseconds
	   */
	  /*enc += EncodeNumber(enc, -1.0); */ /* len */
	  if (r->Link.stopTime)
		{
		  enc = AMF_EncodeNumber(enc, pend, r->Link.stopTime - r->Link.seekTime);
		  if (!enc)
			goto EXIT;
	  }

	  enc = AMF_EncodeNumber(enc, pend, -1000.0);
	  enc = AMF_EncodeBoolean(enc, pend, 0);

	  packet.m_nBodySize = enc - packet.m_body;

	  iRet =  RTMP_SendPacket(r, &packet, TRUE);
//	  if(iRet)
//		  r->m_read.status = RTMP_READ_IGNORE;
    }
EXIT:
	av_log(NULL, AV_LOG_INFO,"gen_play_wrapper_rtmp()-, iRet: '%d', \n", iRet);
    return iRet;
}

int cancel_rtmp_blocking_queue(void *rtmpCtx){
	int iRet = 0;
	//LibRTMPContext *ctx = urlCtx->priv_data;
	RTMP *r = (RTMP *)rtmpCtx;
	if(r){
		int i = 0;
//		for (; i < r->m_numCalls; i++){
//			if (AVMATCH(&r->m_methodCalls[i].name, &av_createStream)){
//				av_log(NULL, AV_LOG_INFO,"cancel_rtmp_blocking_queue(), find i:%d\n", i);
//				RTMP_DropRequest(r, i, TRUE);
//				  //AV_erase(r->m_methodCalls, &r->m_numCalls, i, TRUE);
//				break;
//			}
//		}

		for (i = 0; i < r->m_numCalls; i++){
			if (AVMATCH(&r->m_methodCalls[i].name, &av_play)){
				av_log(NULL, AV_LOG_INFO,"cancel_rtmp_blocking_queue(), find i:%d\n", i);
				r->m_bExitFlag = TRUE;
				RTMP_DropRequest(r, i, TRUE);
				  //AV_erase(r->m_methodCalls, &r->m_numCalls, i, TRUE);
				break;
			}
		}
	}else{
		av_log(NULL, AV_LOG_INFO,"cancel_rtmp_blocking_queue(), r is null\n");
	}
	av_log(NULL, AV_LOG_INFO,"cancel_rtmp_blocking_queue()-, iRet: '%d', \n", iRet);
	return iRet;
}

int register_librtmp_CB(URLContext *urlCtx,
						void (*rtmpCallback)(void* , const AVal*, const AVal*, void*),
						void (*rtmpStatusCallback)(void* , int , void*),
                        void (*rtmpErrorCallback)(void* , int , void*),
						void* userData){
	LibRTMPContext *ctx = urlCtx->priv_data;
	RTMP *r = &ctx->rtmp;
	if(r){
		RTMP_RegisterCB(r, rtmpCallback, rtmpStatusCallback, rtmpErrorCallback, userData);
	}else{
		av_log(NULL, AV_LOG_INFO,"register_librtmp_CB()-, r is null\n");
	}
    return 0;
}

