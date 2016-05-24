#pragma once

#ifdef __cplusplus
 extern "C" {

#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>

#endif

#include "utils.h"
#include "player.h"
#include "beseye_rtmp_observer.h"

using namespace std;

class CBeseyePlayer: public CBeseyeRTMPObserver{
public:

	typedef struct BeseyeAllocEventProps {
		CBeseyePlayer *player;
	    AVFrame *frame;
	} BeseyeAllocEventProps;

	CBeseyePlayer(void* window, int iFrameFormat, int screen_width, int screen_height);
	virtual ~CBeseyePlayer();

	//for streaming client use
	virtual int createStreaming(const char* fullPath);
	virtual int createStreaming(const char* fullPath, int iSeekTimeInMs);
	virtual int createStreaming(const char* streamHost, const char** streamPathList, int iStreamCount, int iSeekTimeInMs);
	virtual int addStreamingPath(const char *streamPath);
	virtual int addStreamingPathList(const char** streamPathList, int iCount);

	virtual int isStreamPlaying();
	virtual int isStreamPaused();

	virtual int pauseStreaming();//buggy
	virtual int resumeStreaming();//buggy

	virtual int closeStreaming();

	virtual int updateWindow(void* window, int iFrameFormat, int screen_width, int screen_height);
	virtual void* getWindow();

	//For update video stream on device
	virtual void registerVideoCallback(void(* videoCallback)(void* window, uint8_t* srcbuf, uint32_t iFormat, uint32_t linesize, uint32_t iWidth, uint32_t iHeight),
									   void(* mVideoDeinitCallback)(void* window));
	virtual void unregisterVideoCallback();

	//For update window info when creating stream
	virtual void setWindowHolder(void* window_holder,
								 void*(* getWindowFunc)(void* window_holder, uint32_t iWidth, uint32_t iHeight));



	//internal use
	virtual void invokeRtmpStreamMethodCallback(const AVal*, const AVal*, void* extra);
	virtual void invokeRtmpStatusCallback(int iStatus, void* extra);
    virtual void invokeRtmpErrorCallback(int iError, void* extra);

	static int workaround_bugs;

	virtual int stream_component_open(VideoState *is, int stream_index);
	virtual void stream_component_close(VideoState *is, int stream_index);
	virtual void stream_seek(VideoState *is, int64_t pos, int64_t rel, int seek_by_bytes);

	virtual int queue_picture(VideoState *is, AVFrame *src_frame, double pts1, int64_t pos);
	virtual int get_video_frame(VideoState *is, AVFrame *frame, int64_t *pts, AVPacket *pkt);
	virtual void stream_toggle_pause(VideoState *is);
	virtual void update_sample_display(VideoState *is, short *samples, int samples_size);
	virtual int audio_decode_frame(VideoState *is, double *pts_ptr);

	virtual int64_t get_start_time();
	virtual int64_t get_duration();
	virtual int64_t get_audio_callback_time();
	virtual void set_audio_callback_time(int64_t t);

	virtual int get_rdftspeed();
	virtual int get_genpts();
	virtual int get_seek_by_bytes();
	virtual void set_seek_by_bytes(int sbb);

	virtual int get_loop();
	virtual int set_loop(int l);

	virtual int get_infinite_buffer();
	virtual int get_autoexit();

	virtual int get_wanted_stream(int idx);
	virtual VideoState::ShowMode get_show_mode();

	virtual void addPendingStreamPaths();

	void setStreamClock(double dClock);
	void setStreamSeekOffset(double dOffset);

	virtual void setAudioMute(bool bMute);
	virtual bool isAudioMute();

	virtual void setRTMPLinkTimeout(int iTimeoutInSec);
	virtual int getRTMPLinkTimeout();
	virtual void setRTMPReadTimeout(int iTimeoutInSec);
	virtual int getRTMPReadTimeout();
	virtual void setRTMPIgoreURLDecode(bool bIgnore);
	virtual bool getRTMPIgoreURLDecode();

	virtual int set_buffer_length(int iBuuferInMS);

private:
	inline void fill_rectangle(SDL_Surface *screen, int x, int y, int w, int h, int color){
	    SDL_Rect rect;
	    rect.x = x;
	    rect.y = y;
	    rect.w = w;
	    rect.h = h;
	    SDL_FillRect(screen, &rect, color);
	}

	inline int compute_mod(int a, int b){ return a < 0 ? a%b + b : a%b; }

	//virtual void blend_subrect(AVPicture *dst, const AVSubtitleRect *rect, int imgw, int imgh);
	virtual void free_subpicture(SubPicture *sp);
	virtual void video_image_display(VideoState *is);
	//virtual void video_audio_display(VideoState *s);
	virtual void stream_close(VideoState *is);
	virtual void do_exit(VideoState *is);
	virtual void sigterm_handler(int sig);
	//virtual int video_open(VideoState *is, int force_set_video_mode);
	virtual void video_display(VideoState *is);

	virtual double get_audio_clock(VideoState *is);
	virtual double get_video_clock(VideoState *is);
	virtual double get_external_clock(VideoState *is);
	virtual double get_master_clock(VideoState *is);

	virtual double compute_target_delay(double delay, VideoState *is);
	virtual void pictq_next_picture(VideoState *is);
	virtual void update_video_pts(VideoState *is, double pts, int64_t pos);
	virtual void video_refresh(void *opaque);
	virtual void alloc_picture(BeseyeAllocEventProps *event_props);

//#if CONFIG_AVFILTER
//	virtual int configure_filtergraph(AVFilterGraph *graph, const char *filtergraph,
//                                 AVFilterContext *source_ctx, AVFilterContext *sink_ctx);
//	virtual int configure_video_filters(AVFilterGraph *graph, VideoState *is, const char *vfilters);
//#endif

	virtual int synchronize_audio(VideoState *is, int nb_samples);

	virtual int audio_open(void *opaque, int64_t wanted_channel_layout, int wanted_nb_channels, int wanted_sample_rate, struct AudioParams *audio_hw_params);

	virtual VideoState *stream_open(const char *filename, AVInputFormat *iformat);
	virtual void stream_cycle_channel(VideoState *is, int codec_type);

	//virtual void toggle_full_screen(VideoState *is);
	virtual void toggle_pause(VideoState *is);
	virtual void step_to_next_frame(VideoState *is);
	//virtual void toggle_audio_display(VideoState *is);

	virtual void event_loop(CBeseyePlayer *cur_player);
	static int lockmgr(void **mtx, enum AVLockOp op);

private:
	VideoState *is;
	AVInputFormat *file_iformat;
	const char *input_filename;
	const char *window_title;
	int fs_screen_width;
	int fs_screen_height;
	int screen_width;
	int screen_height;
    int miFrameFormat;
	AVFrame *pFrameRGB;
	void* rtmpRef;
	void* window;
	void* window_holder;
	void*(* getWindowByHolderFunc)(void* window_holder, uint32_t iWidth, uint32_t iHeight) ;

	AVPacket flush_pkt;

	int64_t start_time;
	int64_t duration;
	int64_t audio_callback_time;

	pthread_mutex_t mDVRVecMux;
	pthread_mutex_t mDVRRestCountMux;
	int miRestDVRCount;
	char** mVecPendingStreamPaths;
	int iNumOfPendingStreamPaths;
	void freePendingStreamPaths();

	int rdftspeed;
	int genpts;
	int seek_by_bytes;
	int show_status;
	int loop;
    int infinite_buffer;
    int autoexit;
	int display_disable;
	int av_sync_type;

	int wanted_stream[AVMEDIA_TYPE_NB];
	enum VideoState::ShowMode show_mode;

	int fast;
	int lowres;
	int idct;
	enum AVDiscard skip_frame;
	enum AVDiscard skip_idct;
	enum AVDiscard skip_loop_filter;
	int error_concealment;
	int decoder_reorder_pts;
	double mdStreamClock;
	double mdSeekOffset ;

	int exit_on_keydown;
	int exit_on_mousedown;
	int framedrop;

	const char *audio_codec_name;
	const char *subtitle_codec_name;
	const char *video_codec_name;

//	#if CONFIG_AVFILTER
//	char *vfilters;
//	#endif

	/* current context */
	int is_full_screen;

	bool mbIsMute;
	int miRTMPLinkTimeout;
	int miRTMPReadTimeout;
	bool mbIgnoreURLDecode;

	//SDL_Surface *screen;

	void(* mVideoCallback)(void* window, uint8_t* srcbuf, uint32_t iFormat, uint32_t linesize, uint32_t iWidth, uint32_t iHeight) ;
	void(* mVideoDeinitCallback)(void* window) ;

	int addStreamingPath(const char *path, int iIgnoreVec);
	//void(* mPlayCB)(CBeseyePlayer *, Player_Callback, const char *, int) ;
};

struct CallbackInfo{
	CBeseyePlayer* player;
	VideoState *is;
};

int read_thread(void *arg);
int refresh_thread(void *opaque);
int video_thread(void *arg);
int subtitle_thread(void *arg);

void sdl_audio_callback(void *opaque, Uint8 *stream, int len);
int decode_interrupt_cb(void *ctx);

#ifdef __cplusplus
 }
#endif
