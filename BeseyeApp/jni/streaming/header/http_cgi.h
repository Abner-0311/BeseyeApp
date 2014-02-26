/*****************************************************************************
 * http_cgi.h  HTTP CGI head file 
 * 
 *   COPYRIGHT (C) 2013 BesEye Co. 
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    12/31/2013 - Chris Hsin - Created.
 *
 *****************************************************************************/
#ifndef _HTTP_CGI_H_
#define _HTTP_CGI_H_

#include <utils.h>

#ifdef __cplusplus
extern "C" {
#endif

// should be big enough for most things
// more flexibility may need to be added for a real application
// An average tracks packet is about 1.5k (4 tracks), and settings resources
// are generally < .5k.
#define URL_SIZE 256
#define BUF_SIZE 10*1024
#define AUDIO_BUF_SIZE 1280
#define SESSION_SIZE 64
#define HOST_NAME "http://192.168.2.2/sray"
#define HOST_NAME_AUDIO "http://192.168.2.2/cgi/audio"
	
int getCGI(char* url, char* data);
int postCGI(char* url, char* data);
void handleSettings(char* data);
int GetSession(char *session);
int GetCGI(const char* command, char* data, const char* session);

int GetAudioBufCGI(const char* command, char* data, const char* session, void(* writeBufCB)(unsigned char*, int));
int getdataExt(char* url, const char* command, char* data, void(* writeBufCB)(unsigned char*, int));
int stopReceiveAudioBuf();

#ifdef __cplusplus
}
#endif
#endif /* _HTTP_CGI_H_ */
