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
	return (msec_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
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

static char data[BUF_SIZE] = {0};

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
			if('\n' == data[ret -1]){
				ret-=1;
			}
			if(0 < ret){
				cRet = (char*) malloc((ret+1)*sizeof(char));
				strncpy(cRet, data, ret);
			}
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
	int iRet = 0;
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

int deleteFile(const char* filePath){
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
