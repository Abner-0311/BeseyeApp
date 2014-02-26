#include "ffmpeg_ext.h"

#include "libavcodec/raw.h"
#include "libavutil/avassert.h"
#include "libavutil/opt.h"
#include "libavutil/pixdesc.h"
#include "libavutil/avassert.h"
#include "libavutil/avstring.h"
#include "libavutil/mathematics.h"
#include "libavutil/parseutils.h"
#include "libavutil/timestamp.h"
#include "libavutil/intreadwrite.h"
#include "libavformat/url.h"
#include "libavformat/internal.h"

#define RELATIVE_TS_BASE (INT64_MAX - (1LL<<48))
#define FF_MAX_EXTRADATA_SIZE ((1 << 28) - FF_INPUT_BUFFER_PADDING_SIZE)

static int is_relative(int64_t ts) {
    return ts > (RELATIVE_TS_BASE - (1LL<<48));
}

static int get_std_framerate(int i){
    if(i<60*12) return (i+1)*1001;
    else        return ((const int[]){24,30,60,12,15,48})[i-60*12]*1000*12;
}

/**
 * Return TRUE if the stream has accurate duration in any stream.
 *
 * @return TRUE if the stream has accurate duration for at least one component.
 */
static int has_duration(AVFormatContext *ic)
{
    int i;
    AVStream *st;

    for(i = 0;i < ic->nb_streams; i++) {
        st = ic->streams[i];
        if (st->duration != AV_NOPTS_VALUE)
            return 1;
    }
    if (ic->duration != AV_NOPTS_VALUE)
        return 1;
    return 0;
}

static AVPacket *add_to_pktbuf(AVPacketList **packet_buffer, AVPacket *pkt,
                               AVPacketList **plast_pktl){
    AVPacketList *pktl = av_mallocz(sizeof(AVPacketList));
    if (!pktl)
        return NULL;

    if (*packet_buffer)
        (*plast_pktl)->next = pktl;
    else
        *packet_buffer = pktl;

    /* add the packet in the buffered packet list */
    *plast_pktl = pktl;
    pktl->pkt= *pkt;
    return &pktl->pkt;
}



/*
 * Is the time base unreliable.
 * This is a heuristic to balance between quick acceptance of the values in
 * the headers vs. some extra checks.
 * Old DivX and Xvid often have nonsense timebases like 1fps or 2fps.
 * MPEG-2 commonly misuses field repeat flags to store different framerates.
 * And there are "variable" fps files this needs to detect as well.
 */
static int tb_unreliable(AVCodecContext *c){
    if(   c->time_base.den >= 101L*c->time_base.num
       || c->time_base.den <    5L*c->time_base.num
/*       || c->codec_tag == AV_RL32("DIVX")
       || c->codec_tag == AV_RL32("XVID")*/
       || c->codec_id == CODEC_ID_MPEG2VIDEO
       || c->codec_id == CODEC_ID_H264
       )
        return 1;
    return 0;
}

static int determinable_frame_size_ext(AVCodecContext *avctx)
{
    if (/*avctx->codec_id == CODEC_ID_AAC ||*/
        avctx->codec_id == CODEC_ID_MP1 ||
        avctx->codec_id == CODEC_ID_MP2 ||
        avctx->codec_id == CODEC_ID_MP3/* ||
        avctx->codec_id == CODEC_ID_CELT*/)
        return 1;
    return 0;
}

static void compute_chapters_end(AVFormatContext *s)
{
    unsigned int i, j;
    int64_t max_time = s->duration + ((s->start_time == AV_NOPTS_VALUE) ? 0 : s->start_time);

    for (i = 0; i < s->nb_chapters; i++)
        if (s->chapters[i]->end == AV_NOPTS_VALUE) {
            AVChapter *ch = s->chapters[i];
            int64_t   end = max_time ? av_rescale_q(max_time, AV_TIME_BASE_Q, ch->time_base)
                                     : INT64_MAX;

            for (j = 0; j < s->nb_chapters; j++) {
                AVChapter *ch1 = s->chapters[j];
                int64_t next_start = av_rescale_q(ch1->start, ch1->time_base, ch->time_base);
                if (j != i && next_start > ch->start && next_start < end)
                    end = next_start;
            }
            ch->end = (end == INT64_MAX) ? ch->start : end;
        }
}



static void estimate_timings_from_bit_rate(AVFormatContext *ic)
{
    int64_t filesize, duration;
    int bit_rate, i;
    AVStream *st;

    /* if bit_rate is already set, we believe it */
    if (ic->bit_rate <= 0) {
        bit_rate = 0;
        for(i=0;i<ic->nb_streams;i++) {
            st = ic->streams[i];
            if (st->codec->bit_rate > 0)
            bit_rate += st->codec->bit_rate;
        }
        ic->bit_rate = bit_rate;
    }

    /* if duration is already set, we believe it */
    if (ic->duration == AV_NOPTS_VALUE &&
        ic->bit_rate != 0) {
        filesize = ic->pb ? avio_size(ic->pb) : 0;
        if (filesize > 0) {
            for(i = 0; i < ic->nb_streams; i++) {
                st = ic->streams[i];
                duration= av_rescale(8*filesize, st->time_base.den, ic->bit_rate*(int64_t)st->time_base.num);
                if (st->duration == AV_NOPTS_VALUE)
                    st->duration = duration;
            }
        }
    }
}

static void free_packet_buffer(AVPacketList **pkt_buf, AVPacketList **pkt_buf_end)
{
    while (*pkt_buf) {
        AVPacketList *pktl = *pkt_buf;
        *pkt_buf = pktl->next;
        av_free_packet(&pktl->pkt);
        av_freep(&pktl);
    }
    *pkt_buf_end = NULL;
}

/* XXX: suppress the packet queue */
static void flush_packet_queue(AVFormatContext *s)
{
    free_packet_buffer(&s->parse_queue,       &s->parse_queue_end);
    free_packet_buffer(&s->packet_buffer,     &s->packet_buffer_end);
    free_packet_buffer(&s->raw_packet_buffer, &s->raw_packet_buffer_end);

    s->raw_packet_buffer_remaining_size = RAW_PACKET_BUFFER_SIZE;
}

static int has_codec_parameters_ext(AVStream *st, const char **errmsg_ptr)
{
	//av_log(st, AV_LOG_INFO, "has_codec_parameters_ext()++\n");
    AVCodecContext *avctx = st->codec;

#define FAIL(errmsg) do {                                         \
        if (errmsg_ptr){                                           \
            *errmsg_ptr = errmsg;                                 \
            av_log(st, AV_LOG_INFO, "has_codec_parameters_ext: %s\n", errmsg);\
    	}                                                         \
        return 0;                                                 \
    } while (0)

    //av_log(st, AV_LOG_INFO, "has_codec_parameters_ext: avctx->codec_type->%d\n", avctx->codec_type);

    switch (avctx->codec_type) {
    case AVMEDIA_TYPE_AUDIO:
//        if (!avctx->frame_size && determinable_frame_size_ext(avctx))
//            FAIL("unspecified sample size");
//        if (st->info->found_decoder >= 0 && avctx->sample_fmt == AV_SAMPLE_FMT_NONE)
//            FAIL("unspecified sample format");
//        if (!avctx->sample_rate)
//            FAIL("unspecified sample rate");
//        if (!avctx->channels)
//            FAIL("unspecified number of channels");
        break;
    case AVMEDIA_TYPE_VIDEO:
        if (!avctx->width)
            FAIL("unspecified size");
        if (st->info->found_decoder >= 0 && avctx->pix_fmt == PIX_FMT_NONE)
            FAIL("unspecified pixel format");
        break;
    case AVMEDIA_TYPE_DATA:
        if(avctx->codec_id == CODEC_ID_NONE) return 1;
    }

    if (avctx->codec_type == AVMEDIA_TYPE_VIDEO && avctx->codec_id == CODEC_ID_NONE)
        FAIL("unknown codec");
    return 1;
}

static int has_decode_delay_been_guessed(AVStream *st)
{
    if(st->codec->codec_id != CODEC_ID_H264) return 1;
#if CONFIG_H264_DECODER
    if(st->codec->has_b_frames &&
       avpriv_h264_has_num_reorder_frames(st->codec) == st->codec->has_b_frames)
        return 1;
#endif
    if(st->codec->has_b_frames<3)
        return st->info->nb_decoded_frames >= 6;
    else if(st->codec->has_b_frames<4)
        return st->info->nb_decoded_frames >= 18;
    else
        return st->info->nb_decoded_frames >= 20;
}

/* returns 1 or 0 if or if not decoded data was returned, or a negative error */
static int try_decode_frame(AVStream *st, AVPacket *avpkt, AVDictionary **options)
{
    AVCodec *codec;
    int got_picture = 1, ret = 0;
    AVFrame picture;
    AVPacket pkt = *avpkt;

    if (!avcodec_is_open(st->codec) && !st->info->found_decoder) {
        AVDictionary *thread_opt = NULL;

        codec = st->codec->codec ? st->codec->codec :
                                   avcodec_find_decoder(st->codec->codec_id);

        if (!codec) {
            st->info->found_decoder = -1;
            return -1;
        }

        /* force thread count to 1 since the h264 decoder will not extract SPS
         *  and PPS to extradata during multi-threaded decoding */
        av_dict_set(options ? options : &thread_opt, "threads", "1", 0);
        ret = avcodec_open2(st->codec, codec, options ? options : &thread_opt);
        if (!options)
            av_dict_free(&thread_opt);
        if (ret < 0) {
            st->info->found_decoder = -1;
            return ret;
        }
        st->info->found_decoder = 1;
    } else if (!st->info->found_decoder)
        st->info->found_decoder = 1;

    if (st->info->found_decoder < 0)
        return -1;

    while ((pkt.size > 0 || (!pkt.data && got_picture)) &&
           ret >= 0 &&
           (!has_codec_parameters_ext(st, NULL)   ||
           !has_decode_delay_been_guessed(st) ||
           (!st->codec_info_nb_frames && st->codec->codec->capabilities & CODEC_CAP_CHANNEL_CONF))) {
        got_picture = 0;
        avcodec_get_frame_defaults(&picture);
        switch(st->codec->codec_type) {
        case AVMEDIA_TYPE_VIDEO:
            ret = avcodec_decode_video2(st->codec, &picture,
                                        &got_picture, &pkt);
            break;
        case AVMEDIA_TYPE_AUDIO:
            ret = avcodec_decode_audio4(st->codec, &picture, &got_picture, &pkt);
            break;
        default:
            break;
        }
        if (ret >= 0) {
            if (got_picture)
                st->info->nb_decoded_frames++;
            pkt.data += ret;
            pkt.size -= ret;
            ret       = got_picture;
        }
    }
    if(!pkt.data && !got_picture)
        return -1;
    return ret;
}

/**
 * Get the number of samples of an audio frame. Return -1 on error.
 */
static int get_audio_frame_size(AVCodecContext *enc, int size, int mux)
{
    int frame_size;

    /* give frame_size priority if demuxing */
    if (!mux && enc->frame_size > 1)
        return enc->frame_size;

    if ((frame_size = av_get_audio_frame_duration(enc, size)) > 0)
        return frame_size;

    /* fallback to using frame_size if muxing */
    if (enc->frame_size > 1)
        return enc->frame_size;

    return -1;
}

static void compute_frame_duration(int *pnum, int *pden, AVStream *st,
                                   AVCodecParserContext *pc, AVPacket *pkt)
{
    int frame_size;

    *pnum = 0;
    *pden = 0;
    switch(st->codec->codec_type) {
    case AVMEDIA_TYPE_VIDEO:
        if (st->r_frame_rate.num && !pc) {
            *pnum = st->r_frame_rate.den;
            *pden = st->r_frame_rate.num;
        } else if(st->time_base.num*1000LL > st->time_base.den) {
            *pnum = st->time_base.num;
            *pden = st->time_base.den;
        }else if(st->codec->time_base.num*1000LL > st->codec->time_base.den){
            *pnum = st->codec->time_base.num;
            *pden = st->codec->time_base.den;
            if (pc && pc->repeat_pict) {
                *pnum = (*pnum) * (1 + pc->repeat_pict);
            }
            //If this codec can be interlaced or progressive then we need a parser to compute duration of a packet
            //Thus if we have no parser in such case leave duration undefined.
            if(st->codec->ticks_per_frame>1 && !pc){
                *pnum = *pden = 0;
            }
        }
        break;
    case AVMEDIA_TYPE_AUDIO:
        frame_size = get_audio_frame_size(st->codec, pkt->size, 0);
        if (frame_size <= 0 || st->codec->sample_rate <= 0)
            break;
        *pnum = frame_size;
        *pden = st->codec->sample_rate;
        break;
    default:
        break;
    }
}

static AVPacketList *get_next_pkt(AVFormatContext *s, AVStream *st, AVPacketList *pktl)
{
    if (pktl->next)
        return pktl->next;
    if (pktl == s->parse_queue_end)
        return s->packet_buffer;
    return NULL;
}

static void update_initial_timestamps(AVFormatContext *s, int stream_index,
                                      int64_t dts, int64_t pts)
{
    AVStream *st= s->streams[stream_index];
    AVPacketList *pktl= s->parse_queue ? s->parse_queue : s->packet_buffer;

    if(st->first_dts != AV_NOPTS_VALUE || dts == AV_NOPTS_VALUE || st->cur_dts == AV_NOPTS_VALUE || is_relative(dts))
        return;

    st->first_dts= dts - (st->cur_dts - RELATIVE_TS_BASE);
    st->cur_dts= dts;

    if (is_relative(pts))
        pts += st->first_dts - RELATIVE_TS_BASE;

    for(; pktl; pktl= get_next_pkt(s, st, pktl)){
        if(pktl->pkt.stream_index != stream_index)
            continue;
        if(is_relative(pktl->pkt.pts))
            pktl->pkt.pts += st->first_dts - RELATIVE_TS_BASE;

        if(is_relative(pktl->pkt.dts))
            pktl->pkt.dts += st->first_dts - RELATIVE_TS_BASE;

        if(st->start_time == AV_NOPTS_VALUE && pktl->pkt.pts != AV_NOPTS_VALUE)
            st->start_time= pktl->pkt.pts;
    }
    if (st->start_time == AV_NOPTS_VALUE)
        st->start_time = pts;
}

static void update_initial_durations(AVFormatContext *s, AVStream *st,
                                     int stream_index, int duration)
{
    AVPacketList *pktl= s->parse_queue ? s->parse_queue : s->packet_buffer;
    int64_t cur_dts= RELATIVE_TS_BASE;

    if (st->skip_samples && st->codec->sample_rate && st->time_base.num)
        cur_dts -= av_rescale_q(st->skip_samples,
                                (AVRational){ 1, st->codec->sample_rate },
                                st->time_base);
    if(st->first_dts != AV_NOPTS_VALUE){
        cur_dts= st->first_dts;
        for(; pktl; pktl= get_next_pkt(s, st, pktl)){
            if(pktl->pkt.stream_index == stream_index){
                if(pktl->pkt.pts != pktl->pkt.dts || pktl->pkt.dts != AV_NOPTS_VALUE || pktl->pkt.duration)
                    break;
                cur_dts -= duration;
            }
        }
        if(pktl && pktl->pkt.dts != st->first_dts) {
            av_log(s, AV_LOG_DEBUG, "first_dts %s not matching first dts %s in que\n", av_ts2str(st->first_dts), av_ts2str(pktl->pkt.dts));
            return;
        }
        if(!pktl) {
            av_log(s, AV_LOG_DEBUG, "first_dts %s but no packet with dts in ques\n", av_ts2str(st->first_dts));
            return;
        }
        pktl= s->parse_queue ? s->parse_queue : s->packet_buffer;
        st->first_dts = cur_dts;
    }else if(st->cur_dts != RELATIVE_TS_BASE)
        return;

    for(; pktl; pktl= get_next_pkt(s, st, pktl)){
        if(pktl->pkt.stream_index != stream_index)
            continue;
        if(pktl->pkt.pts == pktl->pkt.dts && (pktl->pkt.dts == AV_NOPTS_VALUE || pktl->pkt.dts == st->first_dts)
           && !pktl->pkt.duration){
            pktl->pkt.dts= cur_dts;
            if(!st->codec->has_b_frames)
                pktl->pkt.pts= cur_dts;
//            if (st->codec->codec_type != AVMEDIA_TYPE_AUDIO)
                pktl->pkt.duration = duration;
        }else
            break;
        cur_dts = pktl->pkt.dts + pktl->pkt.duration;
    }
    if(!pktl)
        st->cur_dts= cur_dts;
}

static int is_intra_only(AVCodecContext *enc){
    if(enc->codec_type == AVMEDIA_TYPE_AUDIO){
        return 1;
    }else if(enc->codec_type == AVMEDIA_TYPE_VIDEO){
        switch(enc->codec_id){
        case CODEC_ID_MJPEG:
        case CODEC_ID_MJPEGB:
        case CODEC_ID_LJPEG:
        case CODEC_ID_PRORES:
        case CODEC_ID_RAWVIDEO:
        case CODEC_ID_V210:
        case CODEC_ID_DVVIDEO:
        case CODEC_ID_HUFFYUV:
        case CODEC_ID_FFVHUFF:
        case CODEC_ID_ASV1:
        case CODEC_ID_ASV2:
        case CODEC_ID_VCR1:
        case CODEC_ID_DNXHD:
        case CODEC_ID_JPEG2000:
        case CODEC_ID_MDEC:
        case CODEC_ID_UTVIDEO:
            return 1;
        default: break;
        }
    }
    return 0;
}

static void compute_pkt_fields(AVFormatContext *s, AVStream *st,
                               AVCodecParserContext *pc, AVPacket *pkt)
{
    int num, den, presentation_delayed, delay, i;
    int64_t offset;

    if (s->flags & AVFMT_FLAG_NOFILLIN)
        return;

    if((s->flags & AVFMT_FLAG_IGNDTS) && pkt->pts != AV_NOPTS_VALUE)
        pkt->dts= AV_NOPTS_VALUE;

    if (st->codec->codec_id != CODEC_ID_H264 && pc && pc->pict_type == AV_PICTURE_TYPE_B)
        //FIXME Set low_delay = 0 when has_b_frames = 1
        st->codec->has_b_frames = 1;

    /* do we have a video B-frame ? */
    delay= st->codec->has_b_frames;
    presentation_delayed = 0;

    /* XXX: need has_b_frame, but cannot get it if the codec is
        not initialized */
    if (delay &&
        pc && pc->pict_type != AV_PICTURE_TYPE_B)
        presentation_delayed = 1;

    if(pkt->pts != AV_NOPTS_VALUE && pkt->dts != AV_NOPTS_VALUE && pkt->dts - (1LL<<(st->pts_wrap_bits-1)) > pkt->pts && st->pts_wrap_bits<63){
        pkt->dts -= 1LL<<st->pts_wrap_bits;
    }

    // some mpeg2 in mpeg-ps lack dts (issue171 / input_file.mpg)
    // we take the conservative approach and discard both
    // Note, if this is misbehaving for a H.264 file then possibly presentation_delayed is not set correctly.
    if(delay==1 && pkt->dts == pkt->pts && pkt->dts != AV_NOPTS_VALUE && presentation_delayed){
        av_log(s, AV_LOG_DEBUG, "invalid dts/pts combination %"PRIi64"\n", pkt->dts);
        pkt->dts= AV_NOPTS_VALUE;
    }

    if (pkt->duration == 0) {
        compute_frame_duration(&num, &den, st, pc, pkt);
        if (den && num) {
            pkt->duration = av_rescale_rnd(1, num * (int64_t)st->time_base.den, den * (int64_t)st->time_base.num, AV_ROUND_DOWN);
        }
    }
    if(pkt->duration != 0 && (s->packet_buffer || s->parse_queue))
        update_initial_durations(s, st, pkt->stream_index, pkt->duration);

    /* correct timestamps with byte offset if demuxers only have timestamps
       on packet boundaries */
    if(pc && st->need_parsing == AVSTREAM_PARSE_TIMESTAMPS && pkt->size){
        /* this will estimate bitrate based on this frame's duration and size */
        offset = av_rescale(pc->offset, pkt->duration, pkt->size);
        if(pkt->pts != AV_NOPTS_VALUE)
            pkt->pts += offset;
        if(pkt->dts != AV_NOPTS_VALUE)
            pkt->dts += offset;
    }

    if (pc && pc->dts_sync_point >= 0) {
        // we have synchronization info from the parser
        int64_t den = st->codec->time_base.den * (int64_t) st->time_base.num;
        if (den > 0) {
            int64_t num = st->codec->time_base.num * (int64_t) st->time_base.den;
            if (pkt->dts != AV_NOPTS_VALUE) {
                // got DTS from the stream, update reference timestamp
                st->reference_dts = pkt->dts - pc->dts_ref_dts_delta * num / den;
                pkt->pts = pkt->dts + pc->pts_dts_delta * num / den;
            } else if (st->reference_dts != AV_NOPTS_VALUE) {
                // compute DTS based on reference timestamp
                pkt->dts = st->reference_dts + pc->dts_ref_dts_delta * num / den;
                pkt->pts = pkt->dts + pc->pts_dts_delta * num / den;
            }
            if (pc->dts_sync_point > 0)
                st->reference_dts = pkt->dts; // new reference
        }
    }

    /* This may be redundant, but it should not hurt. */
    if(pkt->dts != AV_NOPTS_VALUE && pkt->pts != AV_NOPTS_VALUE && pkt->pts > pkt->dts)
        presentation_delayed = 1;

//    av_log(NULL, AV_LOG_DEBUG, "IN delayed:%d pts:%s, dts:%s cur_dts:%s st:%d pc:%p duration:%d\n",
//           presentation_delayed, av_ts2str(pkt->pts), av_ts2str(pkt->dts), av_ts2str(st->cur_dts), pkt->stream_index, pc, pkt->duration);
    /* interpolate PTS and DTS if they are not present */
    //We skip H264 currently because delay and has_b_frames are not reliably set
    if((delay==0 || (delay==1 && pc)) && st->codec->codec_id != CODEC_ID_H264){
        if (presentation_delayed) {
            /* DTS = decompression timestamp */
            /* PTS = presentation timestamp */
            if (pkt->dts == AV_NOPTS_VALUE)
                pkt->dts = st->last_IP_pts;
            update_initial_timestamps(s, pkt->stream_index, pkt->dts, pkt->pts);
            if (pkt->dts == AV_NOPTS_VALUE)
                pkt->dts = st->cur_dts;

            /* this is tricky: the dts must be incremented by the duration
            of the frame we are displaying, i.e. the last I- or P-frame */
            if (st->last_IP_duration == 0)
                st->last_IP_duration = pkt->duration;
            if(pkt->dts != AV_NOPTS_VALUE)
                st->cur_dts = pkt->dts + st->last_IP_duration;
            st->last_IP_duration  = pkt->duration;
            st->last_IP_pts= pkt->pts;
            /* cannot compute PTS if not present (we can compute it only
            by knowing the future */
        } else if (pkt->pts != AV_NOPTS_VALUE ||
                   pkt->dts != AV_NOPTS_VALUE ||
                   pkt->duration                ) {
            int duration = pkt->duration;

            if(pkt->pts != AV_NOPTS_VALUE && duration){
                int64_t old_diff= FFABS(st->cur_dts - duration - pkt->pts);
                int64_t new_diff= FFABS(st->cur_dts - pkt->pts);
                if(   old_diff < new_diff && old_diff < (duration>>3)
                   && st->codec->codec_type == AVMEDIA_TYPE_VIDEO
                   && (!strcmp(s->iformat->name, "mpeg") ||
                       !strcmp(s->iformat->name, "mpegts"))){
                    pkt->pts += duration;
                    av_log(s, AV_LOG_WARNING, "Adjusting PTS forward\n");
//                    av_log(NULL, AV_LOG_DEBUG, "id:%d old:%"PRId64" new:%"PRId64" dur:%d cur:%s size:%d\n",
//                           pkt->stream_index, old_diff, new_diff, pkt->duration, av_ts2str(st->cur_dts), pkt->size);
                }
            }

            /* presentation is not delayed : PTS and DTS are the same */
            if (pkt->pts == AV_NOPTS_VALUE)
                pkt->pts = pkt->dts;
            update_initial_timestamps(s, pkt->stream_index, pkt->pts,
                                      pkt->pts);
            if (pkt->pts == AV_NOPTS_VALUE)
                pkt->pts = st->cur_dts;
            pkt->dts = pkt->pts;
            if (pkt->pts != AV_NOPTS_VALUE)
                st->cur_dts = pkt->pts + duration;
        }
    }

    if(pkt->pts != AV_NOPTS_VALUE && delay <= MAX_REORDER_DELAY){
        st->pts_buffer[0]= pkt->pts;
        for(i=0; i<delay && st->pts_buffer[i] > st->pts_buffer[i+1]; i++)
            FFSWAP(int64_t, st->pts_buffer[i], st->pts_buffer[i+1]);
        if(pkt->dts == AV_NOPTS_VALUE)
            pkt->dts= st->pts_buffer[0];
        if(st->codec->codec_id == CODEC_ID_H264){ // we skipped it above so we try here
            update_initial_timestamps(s, pkt->stream_index, pkt->dts, pkt->pts); // this should happen on the first packet
        }
        if(pkt->dts > st->cur_dts)
            st->cur_dts = pkt->dts;
    }

//    av_log(NULL, AV_LOG_ERROR, "OUTdelayed:%d/%d pts:%s, dts:%s cur_dts:%s\n",
//           presentation_delayed, delay, av_ts2str(pkt->pts), av_ts2str(pkt->dts), av_ts2str(st->cur_dts));

    /* update flags */
    if(is_intra_only(st->codec))
        pkt->flags |= AV_PKT_FLAG_KEY;
    if (pc)
        pkt->convergence_duration = pc->convergence_duration;
}

static int parse_packet(AVFormatContext *s, AVPacket *pkt, int stream_index)
{
    AVPacket out_pkt = { 0 }, flush_pkt = { 0 };
    AVStream     *st = s->streams[stream_index];
    uint8_t    *data = pkt ? pkt->data : NULL;
    int         size = pkt ? pkt->size : 0;
    int ret = 0, got_output = 0;

    if (!pkt) {
        av_init_packet(&flush_pkt);
        pkt = &flush_pkt;
        got_output = 1;
    } else if (!size && st->parser->flags & PARSER_FLAG_COMPLETE_FRAMES) {
        // preserve 0-size sync packets
        compute_pkt_fields(s, st, st->parser, pkt);
    }

    while (size > 0 || (pkt == &flush_pkt && got_output)) {
        int len;

        av_init_packet(&out_pkt);
        len = av_parser_parse2(st->parser,  st->codec,
                               &out_pkt.data, &out_pkt.size, data, size,
                               pkt->pts, pkt->dts, pkt->pos);

        pkt->pts = pkt->dts = AV_NOPTS_VALUE;
        /* increment read pointer */
        data += len;
        size -= len;

        got_output = !!out_pkt.size;

        if (!out_pkt.size)
            continue;

        /* set the duration */
        out_pkt.duration = 0;
        if (st->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (st->codec->sample_rate > 0) {
                out_pkt.duration = av_rescale_q_rnd(st->parser->duration,
                                                    (AVRational){ 1, st->codec->sample_rate },
                                                    st->time_base,
                                                    AV_ROUND_DOWN);
            }
        } else if (st->codec->time_base.num != 0 &&
                   st->codec->time_base.den != 0) {
            out_pkt.duration = av_rescale_q_rnd(st->parser->duration,
                                                st->codec->time_base,
                                                st->time_base,
                                                AV_ROUND_DOWN);
        }

        out_pkt.stream_index = st->index;
        out_pkt.pts = st->parser->pts;
        out_pkt.dts = st->parser->dts;
        out_pkt.pos = st->parser->pos;

        if (st->parser->key_frame == 1 ||
            (st->parser->key_frame == -1 &&
             st->parser->pict_type == AV_PICTURE_TYPE_I))
            out_pkt.flags |= AV_PKT_FLAG_KEY;

        if(st->parser->key_frame == -1 && st->parser->pict_type==AV_PICTURE_TYPE_NONE && (pkt->flags&AV_PKT_FLAG_KEY))
            out_pkt.flags |= AV_PKT_FLAG_KEY;

        compute_pkt_fields(s, st, st->parser, &out_pkt);

        if ((s->iformat->flags & AVFMT_GENERIC_INDEX) &&
            out_pkt.flags & AV_PKT_FLAG_KEY) {
            int64_t pos= (st->parser->flags & PARSER_FLAG_COMPLETE_FRAMES) ? out_pkt.pos : st->parser->frame_offset;
            ff_reduce_index(s, st->index);
            av_add_index_entry(st, pos, out_pkt.dts,
                               0, 0, AVINDEX_KEYFRAME);
        }

        if (out_pkt.data == pkt->data && out_pkt.size == pkt->size) {
            out_pkt.destruct = pkt->destruct;
            pkt->destruct = NULL;
        }
        if ((ret = av_dup_packet(&out_pkt)) < 0)
            goto fail;

        if (!add_to_pktbuf(&s->parse_queue, &out_pkt, &s->parse_queue_end)) {
            av_free_packet(&out_pkt);
            ret = AVERROR(ENOMEM);
            goto fail;
        }
    }


    /* end of the stream => close and free the parser */
    if (pkt == &flush_pkt) {
        av_parser_close(st->parser);
        st->parser = NULL;
    }

fail:
    av_free_packet(pkt);
    return ret;
}

static int read_from_packet_buffer(AVPacketList **pkt_buffer,
                                   AVPacketList **pkt_buffer_end,
                                   AVPacket      *pkt)
{
    AVPacketList *pktl;
    av_assert0(*pkt_buffer);
    pktl = *pkt_buffer;
    *pkt = pktl->pkt;
    *pkt_buffer = pktl->next;
    if (!pktl->next)
        *pkt_buffer_end = NULL;
    av_freep(&pktl);
    return 0;
}

static int read_frame_internal(AVFormatContext *s, AVPacket *pkt)
{
    int ret = 0, i, got_packet = 0;

    av_init_packet(pkt);

    while (!got_packet && !s->parse_queue) {
        AVStream *st;
        AVPacket cur_pkt;

        /* read next packet */
        ret = ff_read_packet(s, &cur_pkt);
        if (ret < 0) {
            if (ret == AVERROR(EAGAIN))
                return ret;
            /* flush the parsers */
            for(i = 0; i < s->nb_streams; i++) {
                st = s->streams[i];
                if (st->parser && st->need_parsing)
                    parse_packet(s, NULL, st->index);
            }
            /* all remaining packets are now in parse_queue =>
             * really terminate parsing */
            break;
        }
        ret = 0;
        st  = s->streams[cur_pkt.stream_index];

        if (cur_pkt.pts != AV_NOPTS_VALUE &&
            cur_pkt.dts != AV_NOPTS_VALUE &&
            cur_pkt.pts < cur_pkt.dts) {
            av_log(s, AV_LOG_WARNING, "Invalid timestamps stream=%d, pts=%s, dts=%s, size=%d\n",
                   cur_pkt.stream_index,
                   av_ts2str(cur_pkt.pts),
                   av_ts2str(cur_pkt.dts),
                   cur_pkt.size);
        }
        if (s->debug & FF_FDEBUG_TS)
            av_log(s, AV_LOG_DEBUG, "ff_read_packet stream=%d, pts=%s, dts=%s, size=%d, duration=%d, flags=%d\n",
                   cur_pkt.stream_index,
                   av_ts2str(cur_pkt.pts),
                   av_ts2str(cur_pkt.dts),
                   cur_pkt.size,
                   cur_pkt.duration,
                   cur_pkt.flags);

        if (st->need_parsing && !st->parser && !(s->flags & AVFMT_FLAG_NOPARSE)) {
            st->parser = av_parser_init(st->codec->codec_id);
            if (!st->parser) {
                av_log(s, AV_LOG_VERBOSE, "parser not found for codec "
                       "%s, packets or times may be invalid.\n",
                       avcodec_get_name(st->codec->codec_id));
                /* no parser available: just output the raw packets */
                st->need_parsing = AVSTREAM_PARSE_NONE;
            } else if(st->need_parsing == AVSTREAM_PARSE_HEADERS) {
                st->parser->flags |= PARSER_FLAG_COMPLETE_FRAMES;
            } else if(st->need_parsing == AVSTREAM_PARSE_FULL_ONCE) {
                st->parser->flags |= PARSER_FLAG_ONCE;
            } else if(st->need_parsing == AVSTREAM_PARSE_FULL_RAW) {
                st->parser->flags |= PARSER_FLAG_USE_CODEC_TS;
            }
        }

        if (!st->need_parsing || !st->parser) {
            /* no parsing needed: we just output the packet as is */
            *pkt = cur_pkt;
            compute_pkt_fields(s, st, NULL, pkt);
            if ((s->iformat->flags & AVFMT_GENERIC_INDEX) &&
                (pkt->flags & AV_PKT_FLAG_KEY) && pkt->dts != AV_NOPTS_VALUE) {
                ff_reduce_index(s, st->index);
                av_add_index_entry(st, pkt->pos, pkt->dts, 0, 0, AVINDEX_KEYFRAME);
            }
            got_packet = 1;
        } else if (st->discard < AVDISCARD_ALL) {
            if ((ret = parse_packet(s, &cur_pkt, cur_pkt.stream_index)) < 0)
                return ret;
        } else {
            /* free packet */
            av_free_packet(&cur_pkt);
        }
        if (pkt->flags & AV_PKT_FLAG_KEY)
            st->skip_to_keyframe = 0;
        if (st->skip_to_keyframe) {
            av_free_packet(&cur_pkt);
            got_packet = 0;
        }
    }

    if (!got_packet && s->parse_queue)
        ret = read_from_packet_buffer(&s->parse_queue, &s->parse_queue_end, pkt);

    if(s->debug & FF_FDEBUG_TS)
        av_log(s, AV_LOG_DEBUG, "read_frame_internal stream=%d, pts=%s, dts=%s, size=%d, duration=%d, flags=%d\n",
            pkt->stream_index,
            av_ts2str(pkt->pts),
            av_ts2str(pkt->dts),
            pkt->size,
            pkt->duration,
            pkt->flags);

    return ret;
}

/**
 * Estimate the stream timings from the one of each components.
 *
 * Also computes the global bitrate if possible.
 */
static void update_stream_timings(AVFormatContext *ic)
{
    int64_t start_time, start_time1, start_time_text, end_time, end_time1;
    int64_t duration, duration1, filesize;
    int i;
    AVStream *st;

    start_time = INT64_MAX;
    start_time_text = INT64_MAX;
    end_time = INT64_MIN;
    duration = INT64_MIN;
    for(i = 0;i < ic->nb_streams; i++) {
        st = ic->streams[i];
        if (st->start_time != AV_NOPTS_VALUE && st->time_base.den) {
            start_time1= av_rescale_q(st->start_time, st->time_base, AV_TIME_BASE_Q);
            if (st->codec->codec_type == AVMEDIA_TYPE_SUBTITLE || st->codec->codec_type == AVMEDIA_TYPE_DATA) {
                if (start_time1 < start_time_text)
                    start_time_text = start_time1;
            } else
                start_time = FFMIN(start_time, start_time1);
            if (st->duration != AV_NOPTS_VALUE) {
                end_time1 = start_time1
                          + av_rescale_q(st->duration, st->time_base, AV_TIME_BASE_Q);
                end_time = FFMAX(end_time, end_time1);
            }
        }
        if (st->duration != AV_NOPTS_VALUE) {
            duration1 = av_rescale_q(st->duration, st->time_base, AV_TIME_BASE_Q);
            duration = FFMAX(duration, duration1);
        }
    }
    if (start_time == INT64_MAX || (start_time > start_time_text && start_time - start_time_text < AV_TIME_BASE))
        start_time = start_time_text;
    else if(start_time > start_time_text)
        av_log(ic, AV_LOG_VERBOSE, "Ignoring outlier non primary stream starttime %f\n", start_time_text / (float)AV_TIME_BASE);

    if (start_time != INT64_MAX) {
        ic->start_time = start_time;
        if (end_time != INT64_MIN)
            duration = FFMAX(duration, end_time - start_time);
    }
    if (duration != INT64_MIN && ic->duration == AV_NOPTS_VALUE) {
        ic->duration = duration;
    }
        if (ic->pb && (filesize = avio_size(ic->pb)) > 0 && ic->duration != AV_NOPTS_VALUE) {
            /* compute the bitrate */
            ic->bit_rate = (double)filesize * 8.0 * AV_TIME_BASE /
                (double)ic->duration;
        }
}

static void fill_all_stream_timings(AVFormatContext *ic)
{
    int i;
    AVStream *st;

    update_stream_timings(ic);
    for(i = 0;i < ic->nb_streams; i++) {
        st = ic->streams[i];
        if (st->start_time == AV_NOPTS_VALUE) {
            if(ic->start_time != AV_NOPTS_VALUE)
                st->start_time = av_rescale_q(ic->start_time, AV_TIME_BASE_Q, st->time_base);
            if(ic->duration != AV_NOPTS_VALUE)
                st->duration = av_rescale_q(ic->duration, AV_TIME_BASE_Q, st->time_base);
        }
    }
}

#define DURATION_MAX_READ_SIZE 250000
#define DURATION_MAX_RETRY 3

/* only usable for MPEG-PS streams */
static void estimate_timings_from_pts(AVFormatContext *ic, int64_t old_offset)
{
    AVPacket pkt1, *pkt = &pkt1;
    AVStream *st;
    int read_size, i, ret;
    int64_t end_time;
    int64_t filesize, offset, duration;
    int retry=0;

    /* flush packet queue */
    flush_packet_queue(ic);

    for (i=0; i<ic->nb_streams; i++) {
        st = ic->streams[i];
        if (st->start_time == AV_NOPTS_VALUE && st->first_dts == AV_NOPTS_VALUE)
            av_log(st->codec, AV_LOG_WARNING, "start time is not set in estimate_timings_from_pts\n");

        if (st->parser) {
            av_parser_close(st->parser);
            st->parser= NULL;
        }
    }

    /* estimate the end time (duration) */
    /* XXX: may need to support wrapping */
    filesize = ic->pb ? avio_size(ic->pb) : 0;
    end_time = AV_NOPTS_VALUE;
    do{
        offset = filesize - (DURATION_MAX_READ_SIZE<<retry);
        if (offset < 0)
            offset = 0;

        avio_seek(ic->pb, offset, SEEK_SET);
        read_size = 0;
        for(;;) {
            if (read_size >= DURATION_MAX_READ_SIZE<<(FFMAX(retry-1,0)))
                break;

            do {
                ret = ff_read_packet(ic, pkt);
            } while(ret == AVERROR(EAGAIN));
            if (ret != 0)
                break;
            read_size += pkt->size;
            st = ic->streams[pkt->stream_index];
            if (pkt->pts != AV_NOPTS_VALUE &&
                (st->start_time != AV_NOPTS_VALUE ||
                 st->first_dts  != AV_NOPTS_VALUE)) {
                duration = end_time = pkt->pts;
                if (st->start_time != AV_NOPTS_VALUE)
                    duration -= st->start_time;
                else
                    duration -= st->first_dts;
                if (duration < 0)
                    duration += 1LL<<st->pts_wrap_bits;
                if (duration > 0) {
                    if (st->duration == AV_NOPTS_VALUE || st->duration < duration)
                        st->duration = duration;
                }
            }
            av_free_packet(pkt);
        }
    }while(   end_time==AV_NOPTS_VALUE
           && filesize > (DURATION_MAX_READ_SIZE<<retry)
           && ++retry <= DURATION_MAX_RETRY);

    fill_all_stream_timings(ic);

    avio_seek(ic->pb, old_offset, SEEK_SET);
    for (i=0; i<ic->nb_streams; i++) {
        st= ic->streams[i];
        st->cur_dts= st->first_dts;
        st->last_IP_pts = AV_NOPTS_VALUE;
        st->reference_dts = AV_NOPTS_VALUE;
    }
}

static void estimate_timings(AVFormatContext *ic, int64_t old_offset)
{
    int64_t file_size;

    /* get the file size, if possible */
    if (ic->iformat->flags & AVFMT_NOFILE) {
        file_size = 0;
    } else {
        file_size = avio_size(ic->pb);
        file_size = FFMAX(0, file_size);
    }

    if ((!strcmp(ic->iformat->name, "mpeg") ||
         !strcmp(ic->iformat->name, "mpegts")) &&
        file_size && ic->pb->seekable) {
        /* get accurate estimate from the PTSes */
        estimate_timings_from_pts(ic, old_offset);
        ic->duration_estimation_method = AVFMT_DURATION_FROM_PTS;
    } else if (has_duration(ic)) {
        /* at least one component has timings - we use them for all
           the components */
        fill_all_stream_timings(ic);
        ic->duration_estimation_method = AVFMT_DURATION_FROM_STREAM;
    } else {
        av_log(ic, AV_LOG_WARNING, "Estimating duration from bitrate, this may be inaccurate\n");
        /* less precise: use bitrate info */
        estimate_timings_from_bit_rate(ic);
        ic->duration_estimation_method = AVFMT_DURATION_FROM_BITRATE;
    }
    update_stream_timings(ic);

    {
        int i;
        AVStream av_unused *st;
        for(i = 0;i < ic->nb_streams; i++) {
            st = ic->streams[i];
            av_dlog(ic, "%d: start_time: %0.3f duration: %0.3f\n", i,
                    (double) st->start_time / AV_TIME_BASE,
                    (double) st->duration   / AV_TIME_BASE);
        }
        av_dlog(ic, "stream: start_time: %0.3f duration: %0.3f bitrate=%d kb/s\n",
                (double) ic->start_time / AV_TIME_BASE,
                (double) ic->duration   / AV_TIME_BASE,
                ic->bit_rate / 1000);
    }
}

int avformat_find_stream_info_ext(AVFormatContext *ic, AVDictionary **options)
{
    int i, count, ret, read_size, j;
    AVStream *st;
    AVPacket pkt1, *pkt;
    int64_t old_offset = avio_tell(ic->pb);
    int orig_nb_streams = ic->nb_streams;        // new streams might appear, no options for those
    int flush_codecs = 1;

    if(ic->pb)
        av_log(ic, AV_LOG_DEBUG, "File position before avformat_find_stream_info() is %"PRId64"\n", avio_tell(ic->pb));

    av_log(ic, AV_LOG_INFO, "1++++++++++++\n");
    for(i=0;i<ic->nb_streams;i++) {
        AVCodec *codec;
        AVDictionary *thread_opt = NULL;
        st = ic->streams[i];

        if (st->codec->codec_type == AVMEDIA_TYPE_VIDEO ||
            st->codec->codec_type == AVMEDIA_TYPE_SUBTITLE) {
/*            if(!st->time_base.num)
                st->time_base= */
            if(!st->codec->time_base.num)
                st->codec->time_base= st->time_base;
        }
        //only for the split stuff
        if (!st->parser && !(ic->flags & AVFMT_FLAG_NOPARSE)) {
            st->parser = av_parser_init(st->codec->codec_id);
            if(st->parser){
                if(st->need_parsing == AVSTREAM_PARSE_HEADERS){
                    st->parser->flags |= PARSER_FLAG_COMPLETE_FRAMES;
                } else if(st->need_parsing == AVSTREAM_PARSE_FULL_RAW) {
                    st->parser->flags |= PARSER_FLAG_USE_CODEC_TS;
                }
            } else if (st->need_parsing) {
                av_log(ic, AV_LOG_VERBOSE, "parser not found for codec "
                       "%s, packets or times may be invalid.\n",
                       avcodec_get_name(st->codec->codec_id));
            }
        }
        codec = st->codec->codec ? st->codec->codec :
                                   avcodec_find_decoder(st->codec->codec_id);

        /* force thread count to 1 since the h264 decoder will not extract SPS
         *  and PPS to extradata during multi-threaded decoding */
        av_dict_set(options ? &options[i] : &thread_opt, "threads", "1", 0);

        /* Ensure that subtitle_header is properly set. */
        if (st->codec->codec_type == AVMEDIA_TYPE_SUBTITLE
            && codec && !st->codec->codec)
            avcodec_open2(st->codec, codec, options ? &options[i]
                              : &thread_opt);

        //try to just open decoders, in case this is enough to get parameters
        if (!has_codec_parameters_ext(st, NULL)) {
            if (codec && !st->codec->codec)
                avcodec_open2(st->codec, codec, options ? &options[i]
                              : &thread_opt);
        }
        if (!options)
            av_dict_free(&thread_opt);

        av_log(ic, AV_LOG_INFO, "1-1, %d: %d", i, (codec != NULL)?codec->id:0);
    }

    av_log(ic, AV_LOG_INFO, "2++++++++++++\n");
    for (i=0; i<ic->nb_streams; i++) {
        ic->streams[i]->info->last_dts = AV_NOPTS_VALUE;
    }

    av_log(ic, AV_LOG_INFO, "3++++++++++++\n");
    count = 0;
    read_size = 0;
    for(;;) {
        if (ff_check_interrupt(&ic->interrupt_callback)){
            ret= AVERROR_EXIT;
            av_log(ic, AV_LOG_DEBUG, "interrupted\n\n");
            break;
        }

        av_log(ic, AV_LOG_INFO, "3-1 ++++++++++++\n");
        /* check if one codec still needs to be handled */
        for(i=0;i<ic->nb_streams;i++) {
            int fps_analyze_framecount = 20;

            st = ic->streams[i];
            if (!has_codec_parameters_ext(st, NULL))
                break;
            /* if the timebase is coarse (like the usual millisecond precision
               of mkv), we need to analyze more frames to reliably arrive at
               the correct fps */
            if (av_q2d(st->time_base) > 0.0005)
                fps_analyze_framecount *= 2;
            if (ic->fps_probe_size >= 0)
                fps_analyze_framecount = ic->fps_probe_size;

//            if(st->codec->codec_type == AVMEDIA_TYPE_VIDEO){
//            	st->r_frame_rate.num = 15;
//            	st->avg_frame_rate.num = 15;
//            }
            av_log(ic, AV_LOG_INFO, "3-1-1, type:%d, st->info->duration_count:%d, st->r_frame_rate.num:%d, st->avg_frame_rate.num:%d, st->first_dts:%d", i, st->info->duration_count, st->r_frame_rate.num, st->avg_frame_rate.num, st->first_dts);

            /* variable fps and no guess at the real fps */
            if(   tb_unreliable(st->codec) && !(st->r_frame_rate.num && st->avg_frame_rate.num)
               && st->info->duration_count < fps_analyze_framecount
               && st->codec->codec_type == AVMEDIA_TYPE_VIDEO)
                break;
            av_log(ic, AV_LOG_INFO, "3-1-2 ++++++++++++\n");
            if(st->parser && st->parser->parser->split && !st->codec->extradata)
                break;
            av_log(ic, AV_LOG_INFO, "3-1-3 ++++++++++++\n");
            if (st->first_dts == AV_NOPTS_VALUE &&
                (st->codec->codec_type == AVMEDIA_TYPE_VIDEO ||
                 st->codec->codec_type == AVMEDIA_TYPE_AUDIO))
                break;
            av_log(ic, AV_LOG_INFO, "3-1-4 ++++++++++++\n");
        }

        av_log(ic, AV_LOG_INFO, "3-1 -------------, st:%d\n", st);

        if(has_codec_parameters_ext(st, NULL)){
        	break;
        }

        av_log(ic, AV_LOG_INFO, "3-2, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        if (i == ic->nb_streams) {
            /* NOTE: if the format has no header, then we need to read
               some packets to get most of the streams, so we cannot
               stop here */
            if (!(ic->ctx_flags & AVFMTCTX_NOHEADER)) {
                /* if we found the info for all the codecs, we can stop */
                ret = count;
                av_log(ic, AV_LOG_DEBUG, "All info found\n\n");
                flush_codecs = 0;
                break;
            }
        }

        av_log(ic, AV_LOG_INFO, "3-3, 0: %d, %d\n",read_size, ic->probesize);
        /* we did not get all the codec info, but we read too much data */
        if (read_size >= ic->probesize) {
            ret = count;
            av_log(ic, AV_LOG_DEBUG, "Probe buffer size limit %d reached\n", ic->probesize);
            for (i = 0; i < ic->nb_streams; i++)
                if (!ic->streams[i]->r_frame_rate.num &&
                    ic->streams[i]->info->duration_count <= 1)
                    av_log(ic, AV_LOG_WARNING,
                           "Stream #%d: not enough frames to estimate rate; "
                           "consider increasing probesize\n", i);
            break;
        }

        av_log(ic, AV_LOG_INFO, "3-4, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        /* NOTE: a new stream can be added there if no header in file
           (AVFMTCTX_NOHEADER) */
        ret = read_frame_internal(ic, &pkt1);
        if (ret == AVERROR(EAGAIN))
            continue;

        if (ret < 0) {
            /* EOF or error*/
            break;
        }

        av_log(ic, AV_LOG_INFO, "3-5, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        pkt= add_to_pktbuf(&ic->packet_buffer, &pkt1, &ic->packet_buffer_end);
        if ((ret = av_dup_packet(pkt)) < 0)
            goto find_stream_info_err;

        read_size += pkt->size;

        av_log(ic, AV_LOG_INFO, "3-6, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        st = ic->streams[pkt->stream_index];
        if (st->codec_info_nb_frames>1) {
            int64_t t=0;
            if (st->time_base.den > 0)
                t = av_rescale_q(st->info->codec_info_duration, st->time_base, AV_TIME_BASE_Q);
            if (st->avg_frame_rate.num > 0)
                t = FFMAX(t, av_rescale_q(st->codec_info_nb_frames, (AVRational){st->avg_frame_rate.den, st->avg_frame_rate.num}, AV_TIME_BASE_Q));

            if (t >= ic->max_analyze_duration) {
                av_log(ic, AV_LOG_WARNING, "max_analyze_duration %d reached at %"PRId64"\n", ic->max_analyze_duration, t);
                break;
            }
            st->info->codec_info_duration += pkt->duration;
        }
        {
            int64_t last = st->info->last_dts;
            av_log(ic, AV_LOG_INFO, "3-7, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
            if(pkt->dts != AV_NOPTS_VALUE && last != AV_NOPTS_VALUE && pkt->dts > last){
                double dts= (is_relative(pkt->dts) ?  pkt->dts - RELATIVE_TS_BASE : pkt->dts) * av_q2d(st->time_base);
                int64_t duration= pkt->dts - last;

//                 if(st->codec->codec_type == AVMEDIA_TYPE_VIDEO)
//                     av_log(NULL, AV_LOG_ERROR, "%f\n", dts);
                for (i=0; i<FF_ARRAY_ELEMS(st->info->duration_error[0][0]); i++) {
                    int framerate= get_std_framerate(i);
                    double sdts= dts*framerate/(1001*12);
                    for(j=0; j<2; j++){
                        int ticks= lrintf(sdts+j*0.5);
                        double error= sdts - ticks + j*0.5;
                        st->info->duration_error[j][0][i] += error;
                        st->info->duration_error[j][1][i] += error*error;
                    }
                }
                st->info->duration_count++;
                // ignore the first 4 values, they might have some random jitter
                if (st->info->duration_count > 3)
                    st->info->duration_gcd = av_gcd(st->info->duration_gcd, duration);
            }
            if (last == AV_NOPTS_VALUE || st->info->duration_count <= 1)
                st->info->last_dts = pkt->dts;
        }
        av_log(ic, AV_LOG_INFO, "3-8, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        if(st->parser && st->parser->parser->split && !st->codec->extradata){
            int i= st->parser->parser->split(st->codec, pkt->data, pkt->size);
            if (i > 0 && i < FF_MAX_EXTRADATA_SIZE) {
                st->codec->extradata_size= i;
                st->codec->extradata= av_malloc(st->codec->extradata_size + FF_INPUT_BUFFER_PADDING_SIZE);
                if (!st->codec->extradata)
                    return AVERROR(ENOMEM);
                memcpy(st->codec->extradata, pkt->data, st->codec->extradata_size);
                memset(st->codec->extradata + i, 0, FF_INPUT_BUFFER_PADDING_SIZE);
            }
        }

        /* if still no information, we try to open the codec and to
           decompress the frame. We try to avoid that in most cases as
           it takes longer and uses more memory. For MPEG-4, we need to
           decompress for QuickTime.

           If CODEC_CAP_CHANNEL_CONF is set this will force decoding of at
           least one frame of codec data, this makes sure the codec initializes
           the channel configuration and does not only trust the values from the container.
        */
        try_decode_frame(st, pkt, (options && i < orig_nb_streams ) ? &options[i] : NULL);

        st->codec_info_nb_frames++;
        count++;

        av_log(ic, AV_LOG_INFO, "3-9, 0: %d\n", (ic->streams[0] && ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        av_log(ic, AV_LOG_INFO, "3-9, 1: %d\n", (ic->streams[1] && ic->streams[1]->codec != NULL)?ic->streams[1]->codec->codec_id:0);
    }
    av_log(ic, AV_LOG_INFO, "4++++++++++++\n");

    if (flush_codecs) {
        AVPacket empty_pkt = { 0 };
        int err = 0;
        av_init_packet(&empty_pkt);

        ret = -1; /* we could not have all the codec parameters before EOF */
        for(i=0;i<ic->nb_streams;i++) {
            const char *errmsg;

            st = ic->streams[i];
            av_log(ic, AV_LOG_INFO, "4-1++++++++++++, i:%d, st:%d, %d\n", i, st, st->info->found_decoder);

            /* flush the decoders */
            if (st->info->found_decoder == 1) {
                do {
                    err = try_decode_frame(st, &empty_pkt,
                                            (options && i < orig_nb_streams) ?
                                            &options[i] : NULL);
                } while (err > 0 && !has_codec_parameters_ext(st, NULL));

                if (err < 0) {
                    av_log(ic, AV_LOG_INFO,
                        "decoding for stream %d failed\n", st->index);
                }
            }

            if (!has_codec_parameters_ext(st, &errmsg)) {
                char buf[256];
                avcodec_string(buf, sizeof(buf), st->codec, 0);
                av_log(ic, AV_LOG_WARNING,
                       "Could not find codec parameters for stream %d (%s): %s\n"
                       "Consider increasing the value for the 'analyzeduration' and 'probesize' options\n",
                       i, buf, errmsg);
            } else {
                ret = 0;
            }
            av_log(ic, AV_LOG_INFO, "4-1, %d: %d",i, (st->codec != NULL)?st->codec->codec_id:0);
        }
        //av_log(ic, AV_LOG_INFO, "4-1, 0: %d", (ic->streams[0]->codec != NULL)?ic->streams[0]->codec->codec_id:0);
        //av_log(ic, AV_LOG_INFO, "4-1, 1: %d", (ic->streams[1]->codec != NULL)?ic->streams[1]->codec->codec_id:0);
    }

    av_log(ic, AV_LOG_INFO, "5++++++++++++\n");

    // close codecs which were opened in try_decode_frame()
    for(i=0;i<ic->nb_streams;i++) {
        st = ic->streams[i];
        avcodec_close(st->codec);
    }
    av_log(ic, AV_LOG_INFO, "6++++++++++++\n");
    for(i=0;i<ic->nb_streams;i++) {
        st = ic->streams[i];
        if (st->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            if(st->codec->codec_id == CODEC_ID_RAWVIDEO && !st->codec->codec_tag && !st->codec->bits_per_coded_sample){
                uint32_t tag= avcodec_pix_fmt_to_codec_tag(st->codec->pix_fmt);
                if(ff_find_pix_fmt(ff_raw_pix_fmt_tags, tag) == st->codec->pix_fmt)
                    st->codec->codec_tag= tag;
            }

            if (st->codec_info_nb_frames>2 && !st->avg_frame_rate.num && st->info->codec_info_duration)
                av_reduce(&st->avg_frame_rate.num, &st->avg_frame_rate.den,
                          (st->codec_info_nb_frames-2)*(int64_t)st->time_base.den,
                          st->info->codec_info_duration*(int64_t)st->time_base.num, 60000);
            // the check for tb_unreliable() is not completely correct, since this is not about handling
            // a unreliable/inexact time base, but a time base that is finer than necessary, as e.g.
            // ipmovie.c produces.
            if (tb_unreliable(st->codec) && st->info->duration_count > 15 && st->info->duration_gcd > FFMAX(1, st->time_base.den/(500LL*st->time_base.num)) && !st->r_frame_rate.num)
                av_reduce(&st->r_frame_rate.num, &st->r_frame_rate.den, st->time_base.den, st->time_base.num * st->info->duration_gcd, INT_MAX);
            if (st->info->duration_count && !st->r_frame_rate.num
               && tb_unreliable(st->codec) /*&&
               //FIXME we should not special-case MPEG-2, but this needs testing with non-MPEG-2 ...
               st->time_base.num*duration_sum[i]/st->info->duration_count*101LL > st->time_base.den*/){
                int num = 0;
                double best_error= 0.01;

                for (j=0; j<FF_ARRAY_ELEMS(st->info->duration_error[0][0]); j++) {
                    int k;

                    if(st->info->codec_info_duration && st->info->codec_info_duration*av_q2d(st->time_base) < (1001*12.0)/get_std_framerate(j))
                        continue;
                    if(!st->info->codec_info_duration && 1.0 < (1001*12.0)/get_std_framerate(j))
                        continue;
                    for(k=0; k<2; k++){
                        int n= st->info->duration_count;
                        double a= st->info->duration_error[k][0][j] / n;
                        double error= st->info->duration_error[k][1][j]/n - a*a;

                        if(error < best_error && best_error> 0.000000001){
                            best_error= error;
                            num = get_std_framerate(j);
                        }
                        if(error < 0.02)
                            av_log(NULL, AV_LOG_DEBUG, "rfps: %f %f\n", get_std_framerate(j) / 12.0/1001, error);
                    }
                }
                // do not increase frame rate by more than 1 % in order to match a standard rate.
                if (num && (!st->r_frame_rate.num || (double)num/(12*1001) < 1.01 * av_q2d(st->r_frame_rate)))
                    av_reduce(&st->r_frame_rate.num, &st->r_frame_rate.den, num, 12*1001, INT_MAX);
            }

            if (!st->r_frame_rate.num){
                if(    st->codec->time_base.den * (int64_t)st->time_base.num
                    <= st->codec->time_base.num * st->codec->ticks_per_frame * (int64_t)st->time_base.den){
                    st->r_frame_rate.num = st->codec->time_base.den;
                    st->r_frame_rate.den = st->codec->time_base.num * st->codec->ticks_per_frame;
                }else{
                    st->r_frame_rate.num = st->time_base.den;
                    st->r_frame_rate.den = st->time_base.num;
                }
            }
        }else if(st->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            if(!st->codec->bits_per_coded_sample)
                st->codec->bits_per_coded_sample= av_get_bits_per_sample(st->codec->codec_id);
            // set stream disposition based on audio service type
            switch (st->codec->audio_service_type) {
            case AV_AUDIO_SERVICE_TYPE_EFFECTS:
                st->disposition = AV_DISPOSITION_CLEAN_EFFECTS;    break;
            case AV_AUDIO_SERVICE_TYPE_VISUALLY_IMPAIRED:
                st->disposition = AV_DISPOSITION_VISUAL_IMPAIRED;  break;
            case AV_AUDIO_SERVICE_TYPE_HEARING_IMPAIRED:
                st->disposition = AV_DISPOSITION_HEARING_IMPAIRED; break;
            case AV_AUDIO_SERVICE_TYPE_COMMENTARY:
                st->disposition = AV_DISPOSITION_COMMENT;          break;
            case AV_AUDIO_SERVICE_TYPE_KARAOKE:
                st->disposition = AV_DISPOSITION_KARAOKE;          break;
            }
        }
    }

    av_log(ic, AV_LOG_INFO, "7++++++++++++\n");
    estimate_timings(ic, old_offset);

    compute_chapters_end(ic);

 find_stream_info_err:
    for (i=0; i < ic->nb_streams; i++) {
        if (ic->streams[i]->codec)
            ic->streams[i]->codec->thread_count = 0;
        av_freep(&ic->streams[i]->info);
    }
    if(ic->pb)
        av_log(ic, AV_LOG_DEBUG, "File position after avformat_find_stream_info() is %"PRId64"\n", avio_tell(ic->pb));
    return ret;
}

int av_read_frame_ext(AVFormatContext *s, AVPacket *pkt)
{
    const int genpts = s->flags & AVFMT_FLAG_GENPTS;
    int          eof = 0;
    int ret;

   // av_log(s, AV_LOG_INFO, "av_read_frame_ext, genpts:%d, s->flags:0x%x, s->packet_buffer:%d", genpts, s->flags, s->packet_buffer);
    if (!genpts) {
        ret = s->packet_buffer ? read_from_packet_buffer(&s->packet_buffer,
                                                          &s->packet_buffer_end,
                                                          pkt) :
                                  read_frame_internal(s, pkt);
        goto return_packet;
    }

    for (;;) {
        AVPacketList *pktl = s->packet_buffer;

        if (pktl) {
            AVPacket *next_pkt = &pktl->pkt;

            av_log(s, AV_LOG_INFO, "av_read_frame_ext, next_pkt->dts:%d", next_pkt->dts);
            if (next_pkt->dts != AV_NOPTS_VALUE) {
                int wrap_bits = s->streams[next_pkt->stream_index]->pts_wrap_bits;
                av_log(s, AV_LOG_INFO, "av_read_frame_ext, wrap_bits:%d", wrap_bits);
                // last dts seen for this stream. if any of packets following
                // current one had no dts, we will set this to AV_NOPTS_VALUE.
                int64_t last_dts = next_pkt->dts;
                while (pktl && next_pkt->pts == AV_NOPTS_VALUE) {
                    if (pktl->pkt.stream_index == next_pkt->stream_index &&
                        (av_compare_mod(next_pkt->dts, pktl->pkt.dts, 2LL << (wrap_bits - 1)) < 0)) {
                        if (av_compare_mod(pktl->pkt.pts, pktl->pkt.dts, 2LL << (wrap_bits - 1))) { //not b frame
                            next_pkt->pts = pktl->pkt.dts;
                        }
                        if (last_dts != AV_NOPTS_VALUE) {
                            // Once last dts was set to AV_NOPTS_VALUE, we don't change it.
                            last_dts = pktl->pkt.dts;
                        }
                    }
                    pktl = pktl->next;
                }
                if (eof && next_pkt->pts == AV_NOPTS_VALUE && last_dts != AV_NOPTS_VALUE) {
                    // Fixing the last reference frame had none pts issue (For MXF etc).
                    // We only do this when
                    // 1. eof.
                    // 2. we are not able to resolve a pts value for current packet.
                    // 3. the packets for this stream at the end of the files had valid dts.
                    next_pkt->pts = last_dts + next_pkt->duration;
                    av_log(s, AV_LOG_INFO, "av_read_frame_ext, next_pkt->pts:%d", next_pkt->pts );
                }
                pktl = s->packet_buffer;
            }

            /* read packet from packet buffer, if there is data */
            if (!(next_pkt->pts == AV_NOPTS_VALUE &&
                  next_pkt->dts != AV_NOPTS_VALUE && !eof)) {
                ret = read_from_packet_buffer(&s->packet_buffer,
                                               &s->packet_buffer_end, pkt);
                goto return_packet;
            }
        }
        av_log(s, AV_LOG_INFO, "av_read_frame_ext, read_frame_internal" );
        ret = read_frame_internal(s, pkt);
        if (ret < 0) {
            if (pktl && ret != AVERROR(EAGAIN)) {
                eof = 1;
                continue;
            } else
                return ret;
        }

        if (av_dup_packet(add_to_pktbuf(&s->packet_buffer, pkt,
                          &s->packet_buffer_end)) < 0)
            return AVERROR(ENOMEM);
    }

return_packet:

    if(s->streams[pkt->stream_index]->skip_samples) {
        uint8_t *p = av_packet_new_side_data(pkt, AV_PKT_DATA_SKIP_SAMPLES, 10);
        AV_WL32(p, s->streams[pkt->stream_index]->skip_samples);
        av_log(s, AV_LOG_DEBUG, "demuxer injecting skip %d\n", s->streams[pkt->stream_index]->skip_samples);
        s->streams[pkt->stream_index]->skip_samples = 0;
    }

    if (is_relative(pkt->dts))
        pkt->dts -= RELATIVE_TS_BASE;
    if (is_relative(pkt->pts))
        pkt->pts -= RELATIVE_TS_BASE;
    return ret;
}


