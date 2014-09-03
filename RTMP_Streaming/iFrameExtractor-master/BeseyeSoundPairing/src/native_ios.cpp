#include <pthread.h>
#include "SDL.h"
#include "utils.h"
#include "beseye_sound_pairing.h"

static int iInited = 0;
static Uint8 * s_stream = NULL;
static int s_stream_len = 0;
static int s_stream_len_used = 0;
static pthread_mutex_t mSyncObj;
static pthread_cond_t mSyncObjCond;

static pthread_mutex_t mSyncObjBufUsed;
static pthread_cond_t mSyncObjCondBufUsed;

/* prepare a new audio buffer */
void sdl_audio_callback(void *opaque, Uint8 *stream, int len){
    if(len > 0) {
    	LOGD("sdl_audio_callback(), len:%d\n", len );
    	pthread_mutex_lock(&mSyncObj);
    	s_stream = stream;
    	s_stream_len = len;
        s_stream_len_used = 0;
    	pthread_cond_signal(&mSyncObjCond);
    	pthread_mutex_unlock(&mSyncObj);
    }

    while( iInited && s_stream_len_used < len){
    	pthread_mutex_lock(&mSyncObjBufUsed);
    	LOGD("sdl_audio_callback(), mSyncObjBufUsed wait begin\n" );
    	pthread_cond_wait(&mSyncObjCondBufUsed, &mSyncObjBufUsed);
    	LOGD("sdl_audio_callback(), mSyncObjBufUsed wait end\n" );
    	pthread_mutex_unlock(&mSyncObjBufUsed);
    }
}

int Delegate_OpenAudioDevice(int wanted_sample_rate, int is16Bit, int wanted_nb_channels, int desiredBufferFrames){
    LOGD("Delegate_OpenAudioDevice()\n" );
	SDL_AudioSpec wanted_spec, spec;
	const char *env;
	const int next_nb_channels[] = {0, 0, 1, 6, 2, 6, 4, 6};
    //LOGD("Delegate_OpenAudioDevice()1" );

	env = SDL_getenv("SDL_AUDIO_CHANNELS");
	if (env) {
		wanted_nb_channels = SDL_atoi(env);
		//wanted_channel_layout = av_get_default_channel_layout(wanted_nb_channels);
	}
    //LOGD("Delegate_OpenAudioDevice()2" );

	wanted_spec.channels = 1;
	wanted_spec.freq = wanted_sample_rate;
	if (wanted_spec.freq <= 0 || wanted_spec.channels <= 0) {
		fprintf(stderr, "Invalid sample rate or channel count!\n");
		return -1;
	}
	wanted_spec.format = AUDIO_S16SYS;
	wanted_spec.silence = 0;
	wanted_spec.samples = desiredBufferFrames;
	wanted_spec.callback = sdl_audio_callback;
//	CallbackInfo* ci = (CallbackInfo*)av_mallocz(sizeof(CallbackInfo));;
//	ci->player = this;
//	ci->is = (VideoState *)opaque;
//	wanted_spec.userdata = ci;//opaque;
	while (SDL_OpenAudio(&wanted_spec, &spec) < 0) {
		fprintf(stderr, "SDL_OpenAudio (%d channels): %s\n", wanted_spec.channels, SDL_GetError());
//		wanted_spec.channels = next_nb_channels[FFMIN(7, wanted_spec.channels)];
//		if (!wanted_spec.channels) {
//			fprintf(stderr, "No more channel combinations to try, audio open failed\n");
//			return -1;
//		}
		//wanted_channel_layout = av_get_default_channel_layout(wanted_spec.channels);
	}
	if (spec.format != AUDIO_S16SYS) {
		fprintf(stderr, "SDL advised audio format %d is not supported!\n", spec.format);
		return -1;
	}
//	if (spec.channels != wanted_spec.channels) {
//		wanted_channel_layout = av_get_default_channel_layout(spec.channels);
//		if (!wanted_channel_layout) {
//			fprintf(stderr, "SDL advised channel count %d is not supported!\n", spec.channels);
//			return -1;
//		}
//	}
    LOGD("Delegate_OpenAudioDevice(), spec.format:%d, spec.channels:%d, spec.size:%d\n",spec.format, spec.channels ,spec.size);

	pthread_mutex_init(&mSyncObj, NULL);
	pthread_cond_init(&mSyncObjCond, NULL);

	pthread_mutex_init(&mSyncObjBufUsed, NULL);
	pthread_cond_init(&mSyncObjCondBufUsed, NULL);

	//audio_hw_params->fmt = AV_SAMPLE_FMT_S16;
	//audio_hw_params->freq = spec.freq;
	//audio_hw_params->channel_layout = wanted_channel_layout;
	//audio_hw_params->channels =  spec.channels;
    
    /* Start playing */
    SDL_PauseAudio(0);
    iInited = 1;
	return spec.size;
}

void * Delegate_GetAudioBuffer(){
	while(iInited && NULL == s_stream){
		pthread_mutex_lock(&mSyncObj);
		LOGD("Delegate_GetAudioBuffer(), mSyncObj wait begin\n" );
		pthread_cond_wait(&mSyncObjCond, &mSyncObj);
		LOGD("Delegate_GetAudioBuffer(), mSyncObj wait end\n" );
		pthread_mutex_unlock(&mSyncObj);
	}
	LOGD("Delegate_GetAudioBuffer(), get\n" );
    return s_stream;
}

int Delegate_GetAudioBufferSize(int sampleRate){
	return s_stream_len;
}

void Delegate_WriteAudioBuffer(int iLen){
	LOGD("Delegate_WriteAudioBuffer(), iLen:%d\n",iLen );
    pthread_mutex_lock(&mSyncObj);
	pthread_mutex_lock(&mSyncObjBufUsed);
    if(s_stream_len == iLen){
        s_stream_len_used =iLen;
        s_stream = NULL;
        s_stream_len = 0;
    }
    LOGD("Delegate_WriteAudioBuffer(), signal\n" );
	pthread_cond_signal(&mSyncObjCondBufUsed);
	pthread_mutex_unlock(&mSyncObjBufUsed);
    pthread_mutex_unlock(&mSyncObj);
}

void Delegate_CloseAudioDevice(){
	LOGE("Delegate_CloseAudioDevice(), iInited:%d\n", iInited );
	if(iInited){
        iInited = 0;
		pthread_mutex_lock(&mSyncObjBufUsed);
		pthread_cond_signal(&mSyncObjCondBufUsed);
		pthread_mutex_unlock(&mSyncObjBufUsed);

		pthread_cond_destroy(&mSyncObjCondBufUsed);
		pthread_mutex_destroy(&mSyncObjBufUsed);

		pthread_mutex_lock(&mSyncObj);
		pthread_cond_signal(&mSyncObjCond);
		pthread_mutex_unlock(&mSyncObj);

		pthread_cond_destroy(&mSyncObjCond);
		pthread_mutex_destroy(&mSyncObj);
		SDL_CloseAudio();
		
	}
    LOGE("Delegate_CloseAudioDevice()---, iInited:%d\n", iInited );
}

void Delegate_detachCurrentThread(){

}
