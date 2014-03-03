/*****************************************************************************
 * main.h  websokcet client source file 
 * 
 *   COPYRIGHT (C) 2014 BesEye Co. 
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    01/10/2013 - Chris Hsin - Created.
 *
 *****************************************************************************/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <getopt.h>
#include <signal.h>
//#include <netinet/in.h>
//#include <sys/socket.h>

#include <json-c/json.h>
#include <websocket_mgr.h>

#include <pthread.h>

static struct libwebsocket *wsi_cam = NULL;
static struct libwebsocket *wsi_audio = NULL;
static void (*wsCb)(const char* cb_type, const char* cb_value, void* data);
static pthread_t sWsThread;
static pthread_mutex_t sSyncObj;
static pthread_cond_t sSyncObjCond;
static bool volatile bInited = FALSE;

#define DEF_HOST_NAME "54.199.196.101"
#define DEF_HOST_PORT 80
#define DEBUG_HOST_PORT 3001
#define DEF_WS_PATH "/websocket"

static char* host_name = DEF_HOST_NAME;
static int   host_port = DEF_HOST_PORT;
static char* ws_path   = DEF_WS_PATH;

static int deny_deflate;
static int deny_mux;
static int was_closed;
static int force_exit = 0;

enum demo_protocols {

	PROTOCOL_DUMB_INCREMENT,

	/* always last */
	DEMO_PROTOCOL_COUNT
};

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
			LOGE( "writeToWebSocket(), content is null\n");
		}
	}else{
		LOGE( "writeToWebSocket(), wsi is null\n");
	}

	return iRet;
}

void callbackDemo(const char* cb_type, const char* cb_value, void* data){
	LOGE( "callbackDemo(), cb_type:[%s], cb_value:[%s]\n", (cb_type)?cb_type:"", (cb_value)?cb_value:"");
}

void invokeCallback(const char* cb_type, const char* cb_value, void* data){
	if(wsCb){
		wsCb(cb_type, cb_value, data);
	}
}

static int ws_packet_process(const char *pkt,struct libwebsocket *wsi)
{
	struct json_object *new_obj;
	struct json_object *cmd_obj;
	struct json_object *body_obj;
	unsigned int jtype = json_type_null;
	unsigned int array_leng = 0;
	int res = -1;
	char *cmd = NULL;
	char *cmd_val = NULL;
	new_obj = json_tokener_parse(pkt);

	array_leng = json_object_array_length(new_obj);
	while(array_leng < 2){
		if(array_leng < 1){
			return 1;
		}
		new_obj = json_object_array_get_idx(new_obj,0);
		array_leng = json_object_array_length(new_obj);
	}
	cmd_obj = json_object_array_get_idx(new_obj,0);
	body_obj = json_object_array_get_idx(new_obj,1);
	//jtype = json_object_get_type(new_obj);
	//LOGE( "length:%u\n", json_object_array_length(new_obj));
	//LOGE( "type:%u\n", jtype);
	cmd = (char*)json_object_get_string(cmd_obj);
	cmd_val = (char*)json_object_get_string(body_obj);
	invokeCallback(cmd, cmd_val, NULL);
	//LOGE( "cmd:%s\n", cmd);
	if(strcmp(cmd, "client_connected") == 0){
		LOGE( "cmd:client_connected\n");
		struct json_object *tmp_obj;
		tmp_obj = json_object_object_get(json_object_object_get(body_obj, "data"),"connection_id");
		LOGE( "connection_id:%s\n", json_object_get_string(tmp_obj));

		res = writeToWebSocket(wsi, "[\"wsc_connected\", {\"data\":\"\"}]");
		LOGE( "send wsc_connected, res:%i\n", res);

		res = writeToWebSocket(wsi, "[\"wsc_keep_alive\", {\"data\":\"\"}]");
		LOGE( "send wsc_keep_alive, res:%i\n", res);

//		res = writeToWebSocket(wsi, "[\"wsc_loopback\", {\"data\":\"Abner Test\"}]");
//		LOGE( "send wsc_loopback, res:%i\n", res);

	}else if(strcmp(cmd, "websocket_rails.ping") == 0){
		LOGE( "cmd:websocket_rails.ping\n");
		//send ping back
		res = writeToWebSocket(wsi, "[\"websocket_rails.ping\", {\"data\":\"\"}]");
		LOGE( "send websocket_rails.ping, res:%i\n", res);

	}else if(strcmp(cmd, "wss_keep_alive") == 0){
		LOGE( "cmd:wss_keep_alive\n");
		res = writeToWebSocket(wsi, "[\"wsc_keep_alive\", {\"data\":\"\"}]");
		LOGE( "send wsc_keep_alive, res:%i\n", res);
	}
	else{

	}
	return 0;
}
/* dumb_increment protocol */

static int
callback_dumb_increment(struct libwebsocket_context *this,
			struct libwebsocket *wsi,
			enum libwebsocket_callback_reasons reason,
					       void *user, void *in, size_t len)
{
	LOGE( "callback_dumb_increment, reason:%d\n", reason);
	switch (reason) {

	case LWS_CALLBACK_CLIENT_ESTABLISHED:
		LOGE( "LWS_CALLBACK_CLIENT_ESTABLISHED\n");
		break;
	case LWS_CALLBACK_CLOSED:
		LOGE( "LWS_CALLBACK_CLOSED\n");
		was_closed = 1;
		break;

	case LWS_CALLBACK_CLIENT_RECEIVE:
		((char *)in)[len] = '\0';
		//LOGE( "rx %d '%s'\n", (int)len, (char *)in);
		ws_packet_process(in, wsi);
		break;

	/* because we are protocols[0] ... */

	case LWS_CALLBACK_CLIENT_CONFIRM_EXTENSION_SUPPORTED:
		if ((strcmp(in, "deflate-stream") == 0) && deny_deflate) {
			LOGE( "denied deflate-stream extension\n");
			return 1;
		}
		if ((strcmp(in, "x-google-mux") == 0) && deny_mux) {
			LOGE( "denied x-google-mux extension\n");
			return 1;
		}

		break;

	default:
		break;
	}

	return 0;
}

/* list of supported protocols and callbacks */

static struct libwebsocket_protocols protocols[] = {
	{
		"dumb-increment-protocol",
		callback_dumb_increment,
		0,
		256,
	},
	{ NULL, NULL, 0, 0 } /* end */
};

void sighandler(int sig)
{
	force_exit = 1;
}

void constructWebSockets(void* userData){
	int n = 0;
	int ret = 0;
	int use_ssl = 0;
	struct libwebsocket_context *context;
	//const char *address="beseye-ls-1";

	int ietf_version = -1; /* latest */
	struct lws_context_creation_info info;

	memset(&info, 0, sizeof info);

	signal(SIGINT, sighandler);

	/*
	 * create the websockets context.  This tracks open connections and
	 * knows how to route any traffic and which protocol version to use,
	 * and if each connection is client or server side.
	 *
	 * For this client-only demo, we tell it to not listen on any port.
	 */

	info.port = CONTEXT_PORT_NO_LISTEN;
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

	/* create a client websocket using dumb increment protocol */
	pthread_mutex_lock(&sSyncObj);
	wsi_cam = libwebsocket_client_connect(context, host_name, host_port, use_ssl,
			ws_path, host_name, host_name,
			 protocols[PROTOCOL_DUMB_INCREMENT].name, ietf_version);
	pthread_mutex_unlock(&sSyncObj);
	if (wsi_cam == NULL) {
		LOGE( "libwebsocket dumb connect failed\n");
		ret = 1;
		goto fail;
	}

	LOGE( "Websocket connection opened\n");

	n = 0;
	while (n >= 0 && !was_closed && !force_exit) {
		n = libwebsocket_service(context, 10);

		if (n < 0)
			continue;
		sleep(1);
	}

fail:
	libwebsocket_context_destroy(context);

sWsThread = NULL;

deinit_websocket_mgr();

LOGE( "constructWebSockets() -- \n");
}

int init_websocket_mgr(void (*cb)(const char* cb_type, const char* cb_value, void* data)){
	int iRet = -1;
	if(!bInited){
		pthread_mutex_init(&sSyncObj, NULL);
		pthread_cond_init(&sSyncObjCond, NULL);

		pthread_mutex_lock(&sSyncObj);
		if(sWsThread == NULL){
			int errno = 0;
			if (0 != (errno = pthread_create(&sWsThread, NULL, constructWebSockets, NULL))) {
				LOGE("init_websocket_mgr(), error when create sWsThread,%d\n", errno);
				iRet = -1;
				deinit_websocket_mgr();
			}else{
				iRet = 0;
	#ifdef ANDROID
				pthread_setname_np(sWsThread, "WsThread");
	#endif
			}

			if(0 == iRet){
				bInited = true;
			}
		}

		wsCb = cb;

		pthread_mutex_unlock(&sSyncObj);
		iRet = 0;
	}
	return iRet;
}

BOOL websocket_mgr_inited(){
	return bInited;
}

int deinit_websocket_mgr(){
	int iRet = -1;
	LOGD("deinit_websocket_mgr()++\n");
	if(bInited){
		force_exit = 1;

		pthread_mutex_lock(&sSyncObj);
		pthread_cond_signal(&sSyncObjCond);
		pthread_mutex_unlock(&sSyncObj);

		while(sWsThread != NULL){
			sleep(1);
		}

		pthread_cond_destroy(&sSyncObjCond);
		pthread_mutex_destroy(&sSyncObj);
		bInited = false;
		iRet = 0;
	}else{
		LOGD("deinit_websocket_mgr(), have deinited\n");
	}
	LOGD("deinit_websocket_mgr()--\n");
	return iRet;
}

//int construct_cam_ws_channel(struct libwebsocket** wsi);
int construct_audio_ws_channel(struct libwebsocket** wsi){
	return -1;
}
BOOL is_ws_channel_valid(struct libwebsocket* wsi){
	return (!wsi_cam && wsi_cam == wsi) || (!wsi_audio && wsi_audio == wsi);
}

int send_cmd_to_cam_via_ws(const char* cmd){
	int iRet = -1;
	pthread_mutex_lock(&sSyncObj);
	if(bInited && wsi_cam){
		iRet = writeToWebSocket(wsi_cam, cmd);
		LOGI( "send_cmd_to_cam_via_ws(), iRet:%i\n", iRet);
	}
	pthread_mutex_unlock(&sSyncObj);
	return iRet;
}

//int destruct_ws_channel(struct libwebsocket* wsi);

void websocket_test(){
	init_websocket_mgr(callbackDemo);

	sleep(5);
	send_cmd_to_cam_via_ws("[\"wsc_loopback\", {\"data\":\"Abner Test\"}]");

	//deinit_websocket_mgr();
}

//#ifndef ANDROID
//int main(int argc, char** argv) {
//	websocket_test();
//	while(!force_exit){
//		sleep(1);
//	}
//    return 0;
//}
//#endif
