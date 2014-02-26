#ifndef __UTILS_H__
#define __UTILS_H__

#include <cmath>
#include <limits>

#include "config.h"

bool sameValue(double a, double b);
bool largeThan(double a, double b);
bool largeEqualThan(double a, double b);
bool lessThan(double a, double b);
bool lessEqualThan(double a, double b);

long getTickCount();

void getTimeSpecByDelay(struct timespec &spec, long lDelayInMS);

#include <sys/time.h>
// Used to measure intervals and absolute times
typedef int64_t msec_t;

typedef unsigned char byte;

// Get current time in milliseconds from the Epoch (Unix)
// or the time the system started (Windows).
msec_t time_ms(void);

//template<typename T, int size>
//int getArrLength(T(&)[size]){return size;}

#endif
