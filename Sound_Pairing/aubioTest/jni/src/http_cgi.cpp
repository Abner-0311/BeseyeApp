// standard stuff
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

// curl
#include <curl/curl.h>
#include <curl/easy.h>

// json parser
#include <json-c/json.h>

#include "http_cgi.h"
#include "config.h"

long bytesWritten = 0;
long bytesRead = 0;

static int writeFn(void* buf, size_t len, size_t size, void* userdata) {
	size_t sLen = len * size;
	LOGE("writeFn.\n  Buffer length: %d\n  Current length: %d\n  Bytes to write: %d\n", BUF_SIZE, bytesWritten, sLen);

    // if this is zero, then it's done
    // we don't do any special processing on the end of the stream
    if (sLen > 0) {
        // >= to account for terminating null
        if (bytesWritten + sLen >= BUF_SIZE) {
            LOGE("Buffer size exceeded.\n  Buffer length: %d\n  Current length: %d\n  Bytes to write: %d\n", BUF_SIZE, bytesWritten, sLen);
            return 0;
        }

        memcpy(&((char*)userdata)[bytesWritten], buf, sLen);
        bytesWritten += sLen;
    }

    return sLen;
}

int getdata(char* url, char* data) {
    int res = -1;
    CURL* pCurl = curl_easy_init();

    if (!pCurl) {
        return 0;
    }

    bytesWritten = 0;
    memset(data, 0, BUF_SIZE);

    // setup curl
    curl_easy_setopt(pCurl, CURLOPT_URL, url);
    curl_easy_setopt(pCurl, CURLOPT_WRITEFUNCTION, writeFn);
    // we don't care about progress
    //curl_easy_setopt(pCurl, CURLOPT_NOPROGRESS, 1);
    curl_easy_setopt(pCurl, CURLOPT_FAILONERROR, 1);
    curl_easy_setopt(pCurl, CURLOPT_WRITEDATA, data);

    // set a 1 second timeout
    //curl_easy_setopt(pCurl, CURLOPT_TIMEOUT, 3);

    LOGE("getdata(), curl_easy_perform begin\n");
    // synchronous, but we don't really care
    res = curl_easy_perform(pCurl);

    LOGE("getdata(), curl_easy_perform end\n");
    // cleanup after ourselves
    curl_easy_cleanup(pCurl);

    return res;
}

int getSettingsGzip(char* url, char* data) {
    int res = -1;
    CURL* pCurl = curl_easy_init();

    int res1;

    if (!pCurl) {
        return 0;
    }

    bytesWritten = 0;

    // setup curl
    curl_easy_setopt(pCurl, CURLOPT_URL, url);
    curl_easy_setopt(pCurl, CURLOPT_WRITEFUNCTION, writeFn);
    // we don't care about progress
    curl_easy_setopt(pCurl, CURLOPT_NOPROGRESS, 1);
    curl_easy_setopt(pCurl, CURLOPT_FAILONERROR, 1);
    curl_easy_setopt(pCurl, CURLOPT_WRITEDATA, data);

    // add the gzip header
    curl_easy_setopt(pCurl, CURLOPT_ACCEPT_ENCODING, "gzip;q=1.0");

    // set a 1 second timeout
    curl_easy_setopt(pCurl, CURLOPT_TIMEOUT, 3);

    // synchronous, but we don't really care
    res = curl_easy_perform(pCurl);

    // cleanup after ourselves
    curl_easy_cleanup(pCurl);

    return res;
}

static size_t read_callback(void* ptr, size_t size, size_t nmemb, void* userdata) {
    size_t tLen;
    char* str;
    if (!userdata) {
        return 0;
    }

    str = (char*)userdata;
    tLen = strlen(&str[bytesRead]);
    if (tLen > size * nmemb) {
        tLen = size * nmemb;
    }

    if (tLen > 0) {
        // assign the string as the data to be sent
        memcpy(ptr, &str[bytesRead], tLen);
        bytesRead += tLen;
    }

    return tLen;
}

int postCGI(char* url, char* data) {
	int res = -1;
	char tmp[BUF_SIZE];
	LOGE("postCGI(), data:%s\n",data);
	CURL* pCurl = curl_easy_init();

	// we need to set headers later
	struct curl_slist* headers = NULL;

	if (!pCurl) {
		return 0;
	}

	bytesWritten = 0;
	bytesRead = 0;

	// we'll use data to store the result
	memset(tmp, 0, BUF_SIZE);

	// add the application/json content-type
	// so the server knows how to interpret our HTTP POST body
	headers = curl_slist_append(headers, "Content-Type: application/json");

	// setup curl
	curl_easy_setopt(pCurl, CURLOPT_URL, url);
	curl_easy_setopt(pCurl, CURLOPT_POST, 1);
	curl_easy_setopt(pCurl, CURLOPT_POSTFIELDSIZE, strlen(data));
	curl_easy_setopt(pCurl, CURLOPT_HTTPHEADER, headers);
	curl_easy_setopt(pCurl, CURLOPT_READFUNCTION, read_callback);
	curl_easy_setopt(pCurl, CURLOPT_READDATA, data);
	curl_easy_setopt(pCurl, CURLOPT_WRITEFUNCTION, writeFn);
	curl_easy_setopt(pCurl, CURLOPT_WRITEDATA, tmp);
	// we don't care about progress
	curl_easy_setopt(pCurl, CURLOPT_NOPROGRESS, 1);
	curl_easy_setopt(pCurl, CURLOPT_FAILONERROR, 1);

	// set a 1 second timeout
	curl_easy_setopt(pCurl, CURLOPT_TIMEOUT, 5);

	// synchronous, but we don't really care
	res = curl_easy_perform(pCurl);

	// cleanup after ourselves
	curl_easy_cleanup(pCurl);
	curl_slist_free_all(headers);

	// copy the response to data
	memcpy(data, tmp, BUF_SIZE);
	return res;

}

void handleSettings(char* data) {
    struct json_object* settingsJson;

    // parse the string into json
    settingsJson = json_tokener_parse(data);

    // return the updated info
    memset(data, 0, BUF_SIZE);
    strcpy(data, json_object_to_json_string(settingsJson));
}

int GetSession(const char* host_name, char *session)
{
	char url[URL_SIZE];
	char data[BUF_SIZE];
	int res;
	struct json_object *new_obj;
	strcpy(data, "{\"username\": \"admin\",\"password\": \"password\"}");
	//printf("<A>data=%s\n", data);

	sprintf(url, "%s/login.cgi", host_name);

	LOGE("url:%s\n",url);
    // set our new and improved settings
    res = postCGI(url, data);
    if (res != 0) {
    	LOGE("CURL error (from postSettings): %d\n", res);
        return res;
    }

    //printf("Result from POST:\n%s\n\n", data);
	struct json_object* result = json_object_object_get(json_tokener_parse(data), "session");
	sscanf(json_object_to_json_string(result), "\"%32[^\"]", session);

	LOGE("session:[%s]\n",session);
	return 0;
}

int GetCGI(const char* command, char* data, const char* host_name, const char* session)
{
	int res;
	char url[URL_SIZE];
	
	sprintf(url, "%s/%s.cgi?session=%s",host_name, command, session);
	//printf("url:%s\n", url);
	memset(data, 0, BUF_SIZE);
    // get the current settings
    res = getdata(url, data);
    if (res != 0) {
        // error occurred
        fprintf(stderr, "CURL error: %d\n", res);
        return res;
    }
}

static int bStopReceiveFlag = FALSE;
int stopReceiveAudioBuf(){
	if(!bStopReceiveFlag){
		bStopReceiveFlag = TRUE;
		return TRUE;
	}
	return FALSE;
}

int GetAudioBufCGI(const char* host_name, const char* command,  const char* session, void(* writeBufCB)(unsigned char*, int))
{
	int res;
	char url[URL_SIZE];

	sprintf(url, "%s?session=%s",host_name, session);
	LOGE("url:%s\n", url);
	//memset(data, 0, BUF_SIZE);
    // get the current settings
    res = getdataExt(url, session, writeBufCB);
    if (res != 0) {
        // error occurred
        fprintf(stderr, "CURL error: %d\n", res);
        return res;
    }
}

static int wait_on_socket(curl_socket_t sockfd, int for_recv, long timeout_ms)
{
  struct timeval tv;
  fd_set infd, outfd, errfd;
  int res;

  tv.tv_sec = timeout_ms / 1000;
  tv.tv_usec= (timeout_ms % 1000) * 1000;

  FD_ZERO(&infd);
  FD_ZERO(&outfd);
  FD_ZERO(&errfd);

  FD_SET(sockfd, &errfd); /* always check for error */

  if(for_recv)
  {
    FD_SET(sockfd, &infd);
  }
  else
  {
    FD_SET(sockfd, &outfd);
  }

  /* select() returns the number of signalled sockets or -1 */
  res = select(sockfd + 1, &infd, &outfd, &errfd, &tv);
  return res;
}

int getdataExt(char* url, const char* session, void(* writeBufCB)(unsigned char*, int))
{
  CURL *curl;
  CURLcode res;
  /* Minimalistic http request */
  char request[AUDIO_BUF_SIZE];

  sprintf(request, "GET /cgi/audio/receiveRaw.cgi?session=%s&httptype=singlepart HTTP/1.0\r\nHost: %s\r\n\r\n", session, CAM_URL);
  //const char *request = "GET /cgi/audio/receive.cgi?httptype=singlepart HTTP/1.0\r\nHost: 192.168.2.2\r\n\r\n";
  LOGE("getdataExt(), request:%s", request);
  curl_socket_t sockfd; /* socket */
  long sockextr;
  size_t iolen;
  curl_off_t nread;
  LOGE("getdataExt(), curl_easy_init");
  curl = curl_easy_init();
  if(curl) {
    curl_easy_setopt(curl, CURLOPT_URL, url);
    /* Do not do the transfer - only connect to host */
    curl_easy_setopt(curl, CURLOPT_CONNECT_ONLY, 1L);
    res = curl_easy_perform(curl);

    if(CURLE_OK != res){
    	LOGE("Error: %s\n", strerror(res));
      return 1;
    }

    /* Extract the socket from the curl handle - we'll need it for waiting.
     * Note that this API takes a pointer to a 'long' while we use
     * curl_socket_t for sockets otherwise.
     */
    res = curl_easy_getinfo(curl, CURLINFO_LASTSOCKET, &sockextr);

    if(CURLE_OK != res){
    	LOGE("Error: %s\n", curl_easy_strerror(res));
      return 1;
    }

    sockfd = sockextr;

    /* wait for the socket to become ready for sending */
    if(!wait_on_socket(sockfd, 0, 60000L)){
    	LOGE("Error: timeout.\n");
      return 1;
    }

    LOGE("Sending request.");
    /* Send the request. Real applications should check the iolen
     * to see if all the request has been sent */
    res = curl_easy_send(curl, request, strlen(request), &iolen);

    if(CURLE_OK != res)
    {
    	LOGE("Error: %s\n", curl_easy_strerror(res));
      return 1;
    }
    LOGE("Reading response.");

//    FILE *fp = NULL;
//    fp=fopen("/data/data/com.example.aubiotest/beseye.pcm", "wb");
//    if(!fp){
//    	 LOGE("failed to open /data/data/com.example.aubiotest/beseye.pcm");
//    }

    /* read the response */
    bStopReceiveFlag = FALSE;
    bool bFirst = true;
    while(!bStopReceiveFlag){
      unsigned char buf[1280];

      wait_on_socket(sockfd, 1, 60000L);
      res = curl_easy_recv(curl, buf, 1280, &iolen);

      if(CURLE_OK != res)
        break;

      nread = (curl_off_t)iolen;
      if(!bFirst){
    	  writeBufCB(buf, iolen);

//    	  if(fp)
//    	    fwrite(buf, sizeof(buf[0]), iolen, fp);

    	  //LOGE("Received %" CURL_FORMAT_CURL_OFF_T " bytes.\n", nread);
      }
      bFirst = false;

    }
//    if(fp)
//    	fclose(fp);
    LOGE("Finish Reading.");
    /* always cleanup */
    curl_easy_cleanup(curl);
  }else{
	  LOGE("getdataExt(), failed to curl_easy_init");
  }
  return 0;
}

