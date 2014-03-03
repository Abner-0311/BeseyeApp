/*****************************************************************************
 * websocket_mgr.h  HTTP CGI head file
 * 
 *   COPYRIGHT (C) 2013 BesEye Co. 
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    02/27/2014 - Abner Huang - Created.
 *
 *****************************************************************************/
#ifndef _WEBSOCKET_MGR_H_
#define _WEBSOCKET_MGR_H_
#ifdef __cplusplus
extern "C" {
#endif

#include <Common.h>
#include <libwebsockets.h>

int init_websocket_mgr(void (*wsCb)(const char* cb_type, const char* cb_value, void* data));
BOOL websocket_mgr_inited();
int deinit_websocket_mgr();

int send_cmd_to_cam_via_ws(const char* cmd);

//int construct_cam_ws_channel(struct libwebsocket** wsi);
int construct_audio_ws_channel(struct libwebsocket** wsi);
BOOL is_ws_channel_valid(struct libwebsocket* wsi);
//int destruct_ws_channel(struct libwebsocket* wsi);

void websocket_test();

#ifdef __cplusplus
}
#endif
#endif /* _WEBSOCKET_MGR_H_ */

