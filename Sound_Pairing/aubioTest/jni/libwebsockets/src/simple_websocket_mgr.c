/*****************************************************************************
 * simple_websocket_mgr.c  simple websokcet mgr source file
 * 
 *   COPYRIGHT (C) 2014 BesEye Co. 
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    04/25/2014 - Abner Huang - Created.
 *
 *****************************************************************************/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <getopt.h>
#include <signal.h>

#include <json-c/json.h>
#include <websocket_utils.h>
#include <simple_websocket_mgr.h>
#include <ws_attr.h>
#include "json_utils.h"
#include "http_cgi.h"
#include "error.h"

#include <pthread.h>

static bool volatile bIsMgrInited = FALSE;
static int force_exit = 0;

void sighandler(int sig){
	stop_websocket_server();
}

void stop_websocket_server(){
	force_exit = 1;
}

int writeToWebSocket(struct libwebsocket *wsi, const char* content){
	int iRet = -1;
	if(wsi){
		if(content){
			const int iLen = strlen(content);

			unsigned char buf[LWS_SEND_BUFFER_PRE_PADDING + (iLen+1) +LWS_SEND_BUFFER_POST_PADDING];
			unsigned char *data = &buf[LWS_SEND_BUFFER_PRE_PADDING];

			strcpy(data, content);
			iRet = libwebsocket_write(wsi, data, iLen, LWS_WRITE_TEXT);
		}else{
			LOGE( "content is null\n");
		}
	}else{
		LOGE( "wsi is null\n");
	}

	return iRet;
}

static struct libwebsocket *wsi_server = NULL;
static void (*serverWSCb)(const char* cb_msg, void* data);
static pthread_t sWsServerThread;
static pthread_mutex_t sWSServerSyncObj;
static pthread_cond_t sWSServerSyncObjCond;

static bool volatile bWSServerAuth = FALSE;
static char ws_server_auth_job_id[32] = "\0";
static int ws_server_deny_deflate;
static int ws_server_deny_mux;
static int ws_server_was_closed;

const static int   server_host_port_listen = 5432;

static int callback_soundpair_server(struct libwebsocket_context *this,
										struct libwebsocket *wsi,
										enum libwebsocket_callback_reasons reason,
										void *user, void *in, size_t len);
enum beseye_soundpair_ws_protocols {
	PROTOCOL_BESEYE_SOUNDPAIR_SERVER,
	/* always last */
	BESEYE_PROTOCOL_COUNT
};

/* list of supported protocols and callbacks */

static struct libwebsocket_protocols protocols[] = {
	{
		"beseye-soundpair-protocol",
		callback_soundpair_server,
		0,
		1024,
	},
	{ NULL, NULL, 0, 0 } /* end */
};

static char* server_host_name = DEF_HOST_NAME;
static int   server_host_port = DEF_HOST_PORT;
static char* server_ws_path   = DEF_WS_PATH;

static struct libwebsocket *ws_client = NULL;
static void (*clientWSCb)(const char* cb_msg, void* data);
static pthread_t sWsClientThread;
static pthread_mutex_t sWSClientSyncObj;
static pthread_cond_t sWSClientSyncObjCond;
static bool volatile bIsWSClientInited = FALSE;
static bool volatile bWSClientAuth = FALSE;
static int ws_client_force_exit = 0;

static char ws_client_auth_job_id[32] = "\0";
static int volatile ws_client_was_closed;

enum beseye_ws_client_protocols {
	PROTOCOL_BESEYE_SOUNDPAIR_CLIENT,
	/* always last */
	BESEYE_AUDIO_PROTOCOL_COUNT
};

static int callback_soundpair_client(struct libwebsocket_context *this,
								 struct libwebsocket *wsi,
								 enum libwebsocket_callback_reasons reason,
								 void *user, void *in, size_t len);

static struct libwebsocket_protocols client_protocols[] = {
	{
		"beseye-soundpair-protocol",
		callback_soundpair_client,
		0,
		1024,
	},
	{ NULL, NULL, 0, 0 } /* end */
};

static int
callback_soundpair_server(struct libwebsocket_context *this,
			struct libwebsocket *wsi,
			enum libwebsocket_callback_reasons reason,
					       void *user, void *in, size_t len)
{
	LOGE( "user:%d, reason:%d\n", user, reason);
	switch (reason) {

	case LWS_CALLBACK_CLIENT_ESTABLISHED:
		LOGE( "LWS_CALLBACK_CLIENT_ESTABLISHED\n");
		wsi_server = wsi;
		break;

	case LWS_CALLBACK_CLOSED:{
		LOGE( "LWS_CALLBACK_CLOSED\n");
		ws_server_was_closed = 1;
		if(serverWSCb && !ws_server_was_closed){
			serverWSCb(MSG_WS_CLOSED, wsi);
			//serverWSCb = NULL;
		}
		break;
	}
	case LWS_CALLBACK_WSI_DESTROY:{
		LOGE( "LWS_CALLBACK_WSI_DESTROY\n");
		ws_server_was_closed = 1;
		if(serverWSCb && !ws_server_was_closed){
			serverWSCb(MSG_WS_CLOSED, wsi);
			//serverWSCb = NULL;
		}
		break;
	}
	case LWS_CALLBACK_RECEIVE:{
		wsi_server = wsi;
		int isBainry = lws_frame_is_binary(wsi);
		//LOGE( "isBainry: %d, len:%d \n", isBainry, len);
		if(!isBainry){
			((char *)in)[len] = '\0';
			LOGE( "rx %d '%s'\n", (int)len, (char *)in);

			if(serverWSCb){
				serverWSCb(in, wsi);
			}
		}
	}

	default:
		break;
	}

	return 0;
}

void constructWebSocketsServer(void* userData){
	do{
		int n = 0;
		int ret = 0;
		int use_ssl = 0;
		struct libwebsocket_context *context;

		int ietf_version = -1; /* latest */
		struct lws_context_creation_info info;

		memset(&info, 0, sizeof info);

		//signal(SIGINT, sighandler);

		/*
		 * create the websockets context.  This tracks open connections and
		 * knows how to route any traffic and which protocol version to use,
		 * and if each connection is client or server side.
		 *
		 * For this client-only demo, we tell it to not listen on any port.
		 */

		info.port = server_host_port_listen;
		info.protocols = protocols;
	#ifndef LWS_NO_EXTENSIONS
		info.extensions = libwebsocket_get_internal_extensions();
	#endif
		info.gid = -1;
		info.uid = -1;

		context = libwebsocket_create_context(&info);
		if (context == NULL) {
			LOGE( "Creating libwebsocket context failed\n");
			return 1;
		}

	//	/* create a client websocket using dumb increment protocol */
	//	pthread_mutex_lock(&sWSServerSyncObj);
	//	LOGE( "libwebsocket connect to %s:%d\n", server_host_name, server_host_port);
	//
	//	wsi_server = libwebsocket_client_connect(context, server_host_name, server_host_port, use_ssl,
	//			server_ws_path, server_host_name, server_host_name,
	//			 protocols[PROTOCOL_BESEYE_SOUNDPAIR_SERVER].name, ietf_version);
	//	pthread_mutex_unlock(&sWSServerSyncObj);
	//	if (wsi_server == NULL) {
	//		LOGE( "libwebsocket dumb connect failed\n");
	//		ret = 1;
	//		goto fail;
	//	}

		LOGE( "Websocket Server connection opened\n");

		n = 0;
		while (n >= 0 && !ws_server_was_closed && !force_exit) {
			n = libwebsocket_service(context, 10);

			if (n < 0)
				continue;
			sleep(1);
		}

		fail:
			libwebsocket_context_destroy(context);

		LOGE( "-- \n");
		if(!force_exit){
			ws_server_was_closed = 0;
			LOGE( "restart server \n");
		}
	}while(!force_exit);

	sWsServerThread = NULL;

	Delegate_detachCurrentThread();
}

int init_websocket_server(void (*wsCb)(const char* cb_msg, void* data)){
	int iRet = -1;
	if(!bIsMgrInited){
		//init_websocket_utils();

		pthread_mutex_init(&sWSServerSyncObj, NULL);
		pthread_cond_init(&sWSServerSyncObjCond, NULL);

		pthread_mutex_lock(&sWSServerSyncObj);
		if(sWsServerThread == NULL){
			int errno = 0;
			if (0 != (errno = pthread_create(&sWsServerThread, NULL, constructWebSocketsServer, NULL))) {
				LOGE(", error when create sWsServerThread,%d\n", errno);
				iRet = -1;
				deinit_websocket_server();
			}else{
				iRet = 0;
	#ifdef ANDROID
				pthread_setname_np(sWsServerThread, "sWsServerThread");
	#endif
			}

			if(0 == iRet){
				bIsMgrInited = true;
			}
		}

		serverWSCb = wsCb;
		pthread_mutex_unlock(&sWSServerSyncObj);
		iRet = 0;
	}
	return iRet;
}

BOOL is_websocket_server_inited(){
	return bIsMgrInited;
}

int deinit_websocket_server(){
	int iRet = -1;
	LOGI("deinit_websocket_server()++\n");
	if(bIsMgrInited){
		stop_websocket_server();

		pthread_mutex_lock(&sWSServerSyncObj);
		pthread_cond_signal(&sWSServerSyncObjCond);
		pthread_mutex_unlock(&sWSServerSyncObj);

		while(sWsServerThread != NULL){
			sleep(1);
		}

		pthread_cond_destroy(&sWSServerSyncObjCond);
		pthread_mutex_destroy(&sWSServerSyncObj);

		//deinit_websocket_utils();
		bIsMgrInited = false;
		iRet = 0;
	}else{
		LOGI("deinit_websocket_server(), have deinited\n");
	}
	LOGI("deinit_websocket_server()--\n");
	return iRet;
}

int send_msg_to_client(const char* msg){
	int iRet = -1;
	pthread_mutex_lock(&sWSServerSyncObj);
	if(bIsMgrInited && wsi_server){
		iRet = writeToWebSocket(wsi_server, msg);
		LOGI( "send_msg_to_client(), iRet:%d\n", iRet);
	}else{
		LOGI( "send_msg_to_client(), bIsMgrInited:%d, wsi_server is %d\n", bIsMgrInited, wsi_server);
	}
	pthread_mutex_unlock(&sWSServerSyncObj);
	return iRet;
}

//Audio WS channel part

static int
callback_soundpair_client(struct libwebsocket_context *this,
			struct libwebsocket *wsi,
			enum libwebsocket_callback_reasons reason,
					       void *user, void *in, size_t len)
{
	LOGE( "callback_soundpair_client(), user:%d, reason:%d\n", user, reason);
	switch (reason) {

	case LWS_CALLBACK_CLIENT_ESTABLISHED:
		LOGE( "LWS_CALLBACK_CLIENT_ESTABLISHED\n");
		if(clientWSCb){
			clientWSCb(MSG_WS_CONNECTED, wsi);
		}
		break;
	case LWS_CALLBACK_CLOSED:
		LOGE( "LWS_CALLBACK_CLOSED\n");
		ws_client_was_closed = 1;
		if(clientWSCb){
			clientWSCb(MSG_WS_CLOSED, wsi);
			clientWSCb = NULL;
		}
		break;
	case LWS_CALLBACK_WSI_DESTROY:{
		LOGE( "LWS_CALLBACK_WSI_DESTROY\n");
		if(clientWSCb){
			clientWSCb(MSG_WS_CLOSED, wsi);
			clientWSCb = NULL;
		}
		break;
	}
	case LWS_CALLBACK_CLIENT_RECEIVE:{
		int isBainry = lws_frame_is_binary(wsi);
		//LOGE( "isBainry: %d \n", isBainry);
		((char *)in)[len] = '\0';
		if(!isBainry){
			((char *)in)[len] = '\0';
			LOGD( "rx %d '%s'\n", (int)len, (char *)in);
			if(clientWSCb){
				clientWSCb(in, wsi);
			}
		}

		break;
	}

	default:
		break;
	}

	return 0;
}

void constructWebSocketsClient(void* userData){
	LOGE( "constructWebSocketsClient()++\n");
	int n = 0;
	int ret = 0;
	int use_ssl = 0;
	struct libwebsocket_context *context;

	int ietf_version = -1; /* latest */
	struct lws_context_creation_info info;

	memset(&info, 0, sizeof info);

	//signal(SIGINT, sighandler);

	/*
	 * create the websockets context.  This tracks open connections and
	 * knows how to route any traffic and which protocol version to use,
	 * and if each connection is client or server side.
	 *
	 * For this client-only demo, we tell it to not listen on any port.
	 */

	info.port = CONTEXT_PORT_NO_LISTEN;
	info.protocols = client_protocols;
#ifndef LWS_NO_EXTENSIONS
	info.extensions = libwebsocket_get_internal_extensions();
#endif
	info.gid = -1;
	info.uid = -1;

	context = libwebsocket_create_context(&info);
	if (context == NULL) {
		LOGE( "Creating libwebsocket context failed\n");
		return 1;
	}

	/* create a client websocket using dumb increment protocol */
	pthread_mutex_lock(&sWSServerSyncObj);
	LOGE( "libwebsocket connect to %s:%d\n", server_host_name, server_host_port);

	if(clientWSCb){
		clientWSCb(MSG_WS_CONNECTING, NULL);
	}

	ws_client = libwebsocket_client_connect(context, server_host_name, server_host_port, use_ssl,
			server_ws_path, server_host_name, server_host_name,
			client_protocols[PROTOCOL_BESEYE_SOUNDPAIR_CLIENT].name, ietf_version);

	pthread_mutex_unlock(&sWSServerSyncObj);
	if (ws_client == NULL) {
		LOGE( "libwebsocket dumb connect failed\n");
		ret = 1;
		goto fail;
	}

	LOGE( " Websocket Client connection opened\n");

	n = 0;
	while (n >= 0 && !ws_client_was_closed && !ws_client_force_exit) {
		n = libwebsocket_service(context, 10);

		if (n < 0)
			continue;
		//sleep(1);
	}

fail:
	libwebsocket_context_destroy(context);

sWsClientThread = NULL;
clientWSCb = NULL;
ws_client = NULL;
bIsWSClientInited = false;
//deinit_websocket_mgr();

LOGE( "constructWebSocketsClient()-- \n");
Delegate_detachCurrentThread();
}

int init_websocket_client(const char* server_ip, int server_port, void (*wsCb)(const char* cb_msg, void* data)){
	LOGE( "init_websocket_client++\n");
	int iRet = -1;
	server_host_name = strdup(server_ip);
	server_host_port = server_port;

	pthread_mutex_lock(&sWSClientSyncObj);
	if(false == bIsWSClientInited){
		if(sWsClientThread == NULL){
			int errno = 0;
			clientWSCb = wsCb;
			if (0 != (errno = pthread_create(&sWsClientThread, NULL, constructWebSocketsClient, NULL))) {
				LOGE(", error when create sWsClientThread,%d\n", errno);
				iRet = -1;
				deinit_websocket_client();
			}else{
				iRet = 0;
				ws_client_force_exit =0;
	#ifdef ANDROID
				pthread_setname_np(sWsClientThread, "AudioWsThread");
	#endif
			}

			if(0 != iRet){
				bIsWSClientInited = false;
			}else{
				bIsWSClientInited = true;
			}
		}
	}
	pthread_mutex_unlock(&sWSClientSyncObj);
	LOGE( "init_websocket_client--\n");
}

int deinit_websocket_client(){
	int iRet = -1;
	LOGI("++, bIsWSClientInited:%d\n",bIsWSClientInited);
	if(bIsWSClientInited){
		ws_client_force_exit = 1;

		pthread_mutex_lock(&sWSClientSyncObj);
		pthread_cond_signal(&sWSClientSyncObjCond);
		pthread_mutex_unlock(&sWSClientSyncObj);

		LOGI("wait sWsClientThread--\n");
		while(sWsClientThread != NULL){
			sleep(1);
		}

		iRet = 0;
	}
	LOGI("--\n");
	return iRet;
}

BOOL is_websocket_client_inited(){
	pthread_mutex_lock(&sWSClientSyncObj);
	BOOL bRet = (NULL != ws_client) && bIsWSClientInited;
	pthread_mutex_unlock(&sWSClientSyncObj);
	return bRet;
}

int send_msg_to_server(const char* cmd){
	int iRet = -1;
	pthread_mutex_lock(&sWSClientSyncObj);
	if(bIsWSClientInited && ws_client){
		iRet = writeToWebSocket(ws_client, cmd);
		//LOGI( "send_cmd_to_audio_be(), iRet:%i\n", iRet);
	}
	pthread_mutex_unlock(&sWSClientSyncObj);
	return iRet;
}
