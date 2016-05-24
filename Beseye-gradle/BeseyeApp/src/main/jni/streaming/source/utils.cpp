#include "utils.h"

#include <pthread.h>

using namespace std;
//bool sameValue(double a, double b) {
//    return std::fabs(a - b) < std::numeric_limits<double>::epsilon();
//}
//
//bool largeThan(double a, double b) {
//    return !sameValue(a,b) && (a > b);
//}
//
//bool largeEqualThan(double a, double b) {
//    return sameValue(a,b) || (a > b);
//}
//
//bool lessThan(double a, double b) {
//    return !sameValue(a,b) && (a < b);
//}
//
//bool lessEqualThan(double a, double b) {
//    return sameValue(a,b) || (a < b);
//}

long getTickCount()
{
	return clock();
}

msec_t time_ms(void){
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (msec_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

static int siDebugMode = 1;

void setDebugMode(int iDebug){//0:off, 1:on
	siDebugMode = iDebug;
	//LOGI("setDebugMode(), siDebugMode:(%d)", siDebugMode);

	if(siDebugMode){
		av_log_set_level(AV_LOG_DEBUG);
		RTMP_LogSetLevel(RTMP_LOGINFO);
	}else{
		av_log_set_level(AV_LOG_ERROR);
		RTMP_LogSetLevel(RTMP_LOGERROR);
	}
}

int isDebugMode(){
	return siDebugMode;
}

//void getTimeSpecByDelay(struct timespec &outtime, long lDelayInMS){
//	struct timeval now;
//	gettimeofday(&now, NULL);
//	outtime.tv_sec   = now.tv_sec + lDelayInMS/1000;
//	outtime.tv_nsec  = now.tv_usec * 1000 + (1000*1000*(lDelayInMS%1000));
//	outtime.tv_sec  += outtime.tv_nsec/(1000*1000*1000);
//	outtime.tv_nsec %= (1000*1000*1000);
//	//LOGI("getTimeSpecByDelay(), lDelayInMS = %ld ms, (%d, %d) -> (%d, %d)", lDelayInMS, now.tv_sec, now.tv_usec, outtime.tv_sec , outtime.tv_nsec);
//}
