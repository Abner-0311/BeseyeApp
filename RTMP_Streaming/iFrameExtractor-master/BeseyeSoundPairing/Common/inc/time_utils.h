#ifndef __TIME_UTILS_H__
#define __TIME_UTILS_H__

#ifdef __cplusplus
extern "C" {
#endif


#include <sys/time.h>
// Used to measure intervals and absolute times
typedef int64_t msec_t;

// Get current time in milliseconds from the Epoch (Unix)
// or the time the system started (Windows).
msec_t time_ms(void);
void showTime();

#ifdef __cplusplus
}
#endif
#endif
