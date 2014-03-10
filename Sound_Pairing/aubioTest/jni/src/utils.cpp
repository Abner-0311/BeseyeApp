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
