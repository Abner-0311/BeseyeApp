/*****************************************************************************
 * simple_websocket_mgr.h  HTTP CGI head file
 * 
 *   COPYRIGHT (C) 2013 BesEye Co. 
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    04/25/2014 - Abner Huang - Created.
 *
 *****************************************************************************/
#ifndef _SPL_WEBSOCKET_MGR_H_
#define _SPL_WEBSOCKET_MGR_H_
#ifdef __cplusplus
extern "C" {
#endif

#include <Common.h>
#include <libwebsockets.h>
#include <websocket_cmd.h>

int init_websocket_server(void (*wsCb)(const char* cb_msg, void* data));
BOOL is_websocket_server_inited();
int send_msg_to_client(const char* msg);
void stop_websocket_server();
int deinit_websocket_server();

int init_websocket_client(const char* server_ip, int server_port, void (*wsCb)(const char* cb_msg, void* data));
BOOL is_websocket_client_inited();
int send_msg_to_server(const char* msg);
int deinit_websocket_client();

#ifdef __cplusplus
}
#endif
#endif /* _SPL_WEBSOCKET_MGR_H_ */

