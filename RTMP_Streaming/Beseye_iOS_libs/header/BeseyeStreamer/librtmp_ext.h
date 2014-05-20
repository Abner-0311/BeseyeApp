#include "utils.h"

#include "libavutil/avstring.h"
#include "libavutil/mathematics.h"
#include "libavutil/opt.h"
#include "libavutil/log.h"
#include "libavformat/avformat.h"
#include "libavformat/url.h"

#include <librtmp/rtmp.h>
#include <librtmp/amf.h>

int gen_play_wrapper(URLContext *urlCtx, char *path);

int register_librtmp_CB(URLContext *urlCtx,
						void (*rtmpCallback)(void* , const AVal*, const AVal*, void*),
						void (*rtmpStatusCallback)(void* , int , void*),
                        void (*rtmpErrorCallback)(void* , int , void*),
						void* userData);
