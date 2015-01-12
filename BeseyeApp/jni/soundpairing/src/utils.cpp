// standard stuff
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "utils.h"
using namespace std;
#include <pthread.h>

bool sameValue(double a, double b) {
    return std::fabs(a - b) < std::numeric_limits<double>::epsilon();
}

bool largeThan(double a, double b) {
    return !sameValue(a,b) && (a > b);
}

bool largeEqualThan(double a, double b) {
    return sameValue(a,b) || (a > b);
}

bool lessThan(double a, double b) {
    return !sameValue(a,b) && (a < b);
}

bool lessEqualThan(double a, double b) {
    return sameValue(a,b) || (a < b);
}

long getTickCount()
{
	return clock()/1000L;
}

msec_t time_ms(void){
	struct timeval tv;
	gettimeofday(&tv, NULL);
	msec_t ret = ((msec_t)tv.tv_sec) * 1000LL + tv.tv_usec / 1000;
	//LOGE("tv.tv_sec = %ld, tv.tv_usec = %ld, ret = %lld\n", tv.tv_sec, tv.tv_usec, ret);
	return ret;
}

void getTimeSpecByDelay(struct timespec &outtime, long lDelayInMS){
	struct timeval now;
	gettimeofday(&now, NULL);
	outtime.tv_sec   = now.tv_sec + lDelayInMS/1000;
	outtime.tv_nsec  = now.tv_usec * 1000 + (1000*1000*(lDelayInMS%1000));
	outtime.tv_sec  += outtime.tv_nsec/(1000*1000*1000);
	outtime.tv_nsec %= (1000*1000*1000);
	//LOGI("getTimeSpecByDelay(), lDelayInMS = %ld ms, (%d, %d) -> (%d, %d)", lDelayInMS, now.tv_sec, now.tv_usec, outtime.tv_sec , outtime.tv_nsec);
}

static int siDebugMode = 1;

void setSPDebugMode(int iDebug){//0:off, 1:on
	siDebugMode = iDebug;
	LOGI("setSPDebugMode(), siDebugMode:(%d)", siDebugMode);
}

int isSPDebugMode(){
	return siDebugMode;
}

#ifndef ANDROID
static char data[BUF_SIZE] = {0};

bool isFileExist(const char* filePath){
	bool bRet= false;
	FILE *fp = NULL;
	if(filePath){
		fp=fopen(filePath, "r");
		if(!fp){
			LOGE("failed to %s\n", filePath);
		}else{
			LOGD("Succeed to %s\n", filePath);
			bRet = true;
		}
	}else{
		LOGE("invalid params:[%s]",(filePath?filePath:""));
	}

	if(fp){
		fclose(fp);
		fp=NULL;
	}
	return bRet;
}

char* readFromFile(const char* filePath){
	char* cRet = NULL;
	FILE *fp = NULL;
	if(filePath){
		fp=fopen(filePath, "r");
		if(!fp){
			LOGE("failed to %s\n", filePath);
		}else{
			LOGD("Succeed to %s\n", filePath);
			int ret = fread(data, 1, BUF_SIZE, fp);
			//LOGE("data=[%s]\n", data);
			if('\n' == data[ret -1]){
				ret-=1;
			}
			//LOGE("data=[%s]2\n", data);
			if(0 < ret){
				cRet = (char*) malloc((ret+1)*sizeof(char));
				memset(cRet, 0, ret+1);

				strncpy(cRet, data, ret);
			}
			//LOGE("cRet=[%s]\n", cRet);
		}
	}else{
		LOGE("invalid params:[%s]",(filePath?filePath:""));
	}

	if(fp){
		fclose(fp);
		fp=NULL;
	}

	return cRet;
}

int saveToFile(const char* filePath, const char* content){
	int iRet = -1;
	FILE *fp = NULL;
	if(filePath && content){
		fp=fopen(filePath, "wb");
		if(!fp){
			LOGE("failed to %s\n", filePath);
		}else{
			LOGD("Succeed to %s\n", filePath);

			int iLenCpy = strlen(content);
			if('"' == content[0] && '"' == content[iLenCpy-1]){
				fwrite(content+1, sizeof(content[0]), iLenCpy-2, fp);
			}else
				fwrite(content, sizeof(content[0]), iLenCpy, fp);

			LOGE("Write [%s] to %s\n", content, filePath);
			iRet = RET_CODE_OK;
		}
	}else{
		LOGE("invalid params:[%s], content:[%s]", (filePath?filePath:""), (content?content:""));
	}

	if(fp){
	    fclose(fp);
	    fp=NULL;
	}

	return iRet;
}

void showTime(){
//	char date[20];
//	struct timeval tv;
//	gettimeofday(&tv, NULL);
//	strftime(date, sizeof(date) / sizeof(*date), "%Y-%m-%dT%H:%M:%S", gmtime(&tv.tv_sec));
	struct timeval  tv;
	struct timezone tz;
	struct tm       tm;

	char       buf[80];
	// Get current time
	//time(&now);
	// Format time, "ddd yyyy-mm-dd hh:mm:ss zzz"
	//ts = *localtime(&now);
	gettimeofday(&tv, &tz);
	tm = *localtime(&tv.tv_sec);

	strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", &tm);
	printf("[%s.%03dZ]", buf, tv.tv_usec / 1000);
	//printf("[%s.%03dZ] ", date, tv.tv_usec / 1000);
}

int saveLogFile(const char* filePath){
	int iRet = 0;
	FILE *fp = NULL;
	if(filePath){
		char date[20];
		struct timeval tv;
		gettimeofday(&tv, NULL);
		strftime(date, sizeof(date) / sizeof(*date), "%Y-%m-%dT%H:%M:%S", gmtime(&tv.tv_sec));
		char logFilePath[1024] = {0};
		sprintf(logFilePath, "%s_%s", filePath, date);
		fp=fopen(logFilePath, "wb");
		if(!fp){
			LOGE("failed to %s\n", logFilePath);
		}else{
			LOGD("Succeed to %s\n", logFilePath);
		}
	}else{
		LOGE("invalid params:[%s]", (filePath?filePath:""));
	}

	if(fp){
		fclose(fp);
		fp=NULL;
	}

	return iRet;
}

int deleteFile(const char* filePath){
	LOGE("delete file %s\n", filePath);
	return remove(filePath);
}

int strCmpEndWith(const char* toCmp, const char* strCmp){
	int iRet = -1;

	if(toCmp && strCmp){
		int iLenToCmp = strlen(toCmp);
		int iLenCmp = strlen(strCmp);
		if(iLenToCmp >= iLenCmp){
			iRet = strcmp(toCmp + (iLenToCmp - iLenCmp), strCmp);
		}
	}
	return iRet;
}

#endif

