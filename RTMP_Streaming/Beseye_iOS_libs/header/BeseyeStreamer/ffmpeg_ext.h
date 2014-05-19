#include "utils.h"
#include "libavutil/dict.h"
#include "libavformat/avformat.h"

int avformat_find_stream_info_ext(AVFormatContext *ic, AVDictionary **options);
int av_read_frame_ext(AVFormatContext *s, AVPacket *pkt);
