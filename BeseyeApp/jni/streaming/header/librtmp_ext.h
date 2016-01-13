#include "utils.h"

#include "libavutil/avstring.h"
#include "libavutil/mathematics.h"
#include "libavutil/opt.h"
#include "libavutil/log.h"
#include "libavformat/avformat.h"
#include "libavformat/url.h"

#include <librtmp/rtmp.h>
#include <librtmp/amf.h>



int register_librtmp_CB(URLContext *urlCtx,
						void (*rtmpCallback)(void* , const AVal*, const AVal*, void*),
						void (*rtmpStatusCallback)(void* , int , void*),
                        void (*rtmpErrorCallback)(void* , int , void*),
						void* userData);

int gen_play_wrapper(URLContext *urlCtx, const char *path);
int gen_play_wrapper_rtmp(void *rtmpCtx, const char *path);

int set_play_buffer_length(URLContext *urlCtx, const int iBuuferInMS);
int set_play_buffer_length_rtmp(void *rtmpCtx, const int iBuuferInMS);

int cancel_rtmp_blocking_queue(void *rtmpCtx);
