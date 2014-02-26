
#ifndef __JAVA_EXPORT__
#define __JAVA_EXPORT__

#include <stdlib.h>
#include <assert.h>
#include <stdint.h>

#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <math.h>

#include <android/log.h>
#include "cam_event_mgr.h"

//#include <aubio/aubio.h>
#define AUBIO_UNSTABLE 1
//#include <aubio.h>
//#include <dywapitchtrack.h>

#include <speex_preprocess.h>
#include "FFT.h"
//#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>

//using zxing::ReedSolomonDecoder;

long getTickCount(VOID)
{
	//const clock_t now = clock();
	//long dwMillisecond = static_cast<DWORD>((static_cast<double>(now) / CLOCKS_PER_SEC) * 1000);

	return clock();
}


#define DECLARE_JNIENV_WITHOUT_RETURN() \
	JNIEnv* jni_env; \
	if (JNI_OK != (*jvm)->AttachCurrentThread(jvm, &jni_env, NULL)) {  return; } \

//static jmethodID  playMethod, getBufMethod, getBufSizeMethod = NULL;
static jobject   jni_host = NULL;
static JavaVM*   jvm = NULL;

JNIEXPORT jboolean JNICALL Java_com_example_aubiotest_AubioTestActivity_nativeClassInit(JNIEnv *env, jclass class)
{
	LOGE("nativeClassInit()+");
	if (JNI_OK != (*env)->GetJavaVM(env, &jvm))
	{
		LOGE("GetJavaVM failed");
		return 0;
	}

	jclass cls = (*env)->FindClass(env, "com/example/aubiotest/AubioTestActivity");
	if(NULL == cls){
		LOGE("cls is empty");
		return 0;
	}

//	playMethod = (*env)->GetMethodID(env, cls, "playSound", "(I)V");
//	if(NULL == playMethod){
//		LOGE("playMethod is empty");
//		return 0;
//	}


	LOGE("nativeClassInit()-");
	return 1;
}

//static aubio_pitch_t *o, *o2;
//static fvec_t* in, *in2;
//static fvec_t* out, *out2;
////static smpl_t* smpldata, *smpldata2;
//static dywapitchtracker pitchtracker;
//static aubio_filter_t * filter;
//static fvec_t * outPeak;
//static aubio_peakpicker_t* peakpicker;
static SpeexPreprocessState* st, *st2;

static int SAMPLE_RATE = 16000;
static int FRAME_SIZE  = 512;
static int HALF_FRAME_SIZE  = 0;
static float BIN_SIZE = 0;//(SAMPLE_RATE/2)/(float)FRAME_SIZE;
static int NS_INDEX  = 0;
static float AGC_LEVEL = 0;
static float FFT_TOL = 0.5f;
static jboolean	ENABLE_DEVERB = 0;
static float DEVERB_DECAY = 0.0f;
static float DEVERB_LEVEL = 0.0f;
static int HIGH_PASS_CRITERIA = 1000;
static int HIGH_PASS_INDEX = 0;

static int LOW_PASS_CRITERIA = 3300;
static int LOW_PASS_INDEX = 0;

JNIEXPORT void JNICALL Java_com_example_aubiotest_AubioTestActivity_startRecord(JNIEnv * env, jobject this, int fd, int iSampleRate, int iFrameSize, int iNSIndex, float iAGCLevel, jboolean bDeverb, float fDeverbDecay, float dDeverbLevel)
{
	//LOGE("startRecord()+, HAVE_FFTW3:%d\n", HAVE_FFTW3);

	SAMPLE_RATE = iSampleRate;
	FRAME_SIZE  = iFrameSize;
	HALF_FRAME_SIZE  = FRAME_SIZE/2;
	BIN_SIZE = (SAMPLE_RATE)/(float)FRAME_SIZE;
	NS_INDEX = iNSIndex;
	AGC_LEVEL = iAGCLevel;
	ENABLE_DEVERB = bDeverb;
	DEVERB_DECAY = fDeverbDecay;
	DEVERB_LEVEL = dDeverbLevel;

	int i = 1;
	while(BIN_SIZE > 0){
		if(BIN_SIZE*i++ > HIGH_PASS_CRITERIA){
			HIGH_PASS_INDEX = i -1;
			break;
		}
	}

	while(BIN_SIZE > 0){
		if(BIN_SIZE*i++ > LOW_PASS_CRITERIA){
			LOW_PASS_INDEX = i -1;
			break;
		}
	}

	LOGE("startRecord()+, iBufSize:%d, SAMPLE_RATE:%d, FRAME_SIZE:%d, HIGH_PASS_INDEX:%d, LOW_PASS_INDEX:%d\n", fd, SAMPLE_RATE, FRAME_SIZE, HIGH_PASS_INDEX, LOW_PASS_INDEX);
//
//	//initialAubio();
//
//	//outPeak = new_fvec (1);
//	//peakpicker = new_aubio_peakpicker();
//	//aubio_peakpicker_set_threshold (peakpicker, 0.3);
//
//	//filter = new_aubio_filter_c_weighting (samplerate);
//	//filter = new_aubio_filter_a_weighting (samplerate);
////	filter = new_aubio_filter(2);
////	aubio_filter_set_samplerate (filter, samplerate);
////	lvec_t *bs = aubio_filter_get_feedforward (filter);
////	lvec_t *as = aubio_filter_get_feedback (filter);
////	lsmp_t *b = bs->data, *a = as->data;
////	uint_t order = aubio_filter_get_order (filter);
////
////    b[0] =  1.000000000000000000000000000000000000000000000000000000e+00;
////    b[1] = -1.000000000000000000000000000000000000000000000000000000e+00;
////    a[0] =  1.000000000000000000000000000000000000000000000000000000e+00;
////    a[1] =  1.000000000000000000000000000000000000000000000000000000e+00;
//
//	//aubio_pitchdetection_mode mode = aubio_pitchm_freq;
//	//aubio_pitchdetection_type type = aubio_pitch_yinfft;
//									 //aubio_pitch_yin;
//									 //aubio_pitch_mcomb;
//									 //aubio_pitch_schmitt;
//									 //aubio_pitch_fcomb;
//
//	//o  = new_aubio_pitchdetection( win_s, hop_s, channels, samplerate, type, mode );
//
//	//in = new_fvec (hop_s, channels); /* input buffer */
//
//
//
//	//filter = new_aubio_cdsgn_filter(44100);
//	//filter = new_aubio_adsgn_filter(44100);
//	//filter = new_aubio_filter(44100, 5);
//	dywapitch_inittracking(&pitchtracker);

	LOGE("startRecord()-");
}

void initialAubio(){
//	uint_t win_s      = FRAME_SIZE;//512;//2048;    /* window size */
//	uint_t hop_s      = FRAME_SIZE;                 /* hop size */
//	uint_t samplerate = SAMPLE_RATE;                /* samplerate */
//	uint_t channels   = 1;                          /* number of channel */
//
//	o = new_aubio_pitch ("default", win_s, hop_s, samplerate);
//	aubio_pitch_set_tolerance(o, FFT_TOL);
//
//	in = new_fvec (win_s); // input buffer
//	out = new_fvec (1); // output candidates
//
//	//smpldata = (smpl_t*)malloc(win_s*sizeof(smpl_t));
}

void initialAubio2(){
//	uint_t win_s      = FRAME_SIZE;//512;//2048;    /* window size */
//	uint_t hop_s      = FRAME_SIZE;                 /* hop size */
//	uint_t samplerate = SAMPLE_RATE;                /* samplerate */
//	uint_t channels   = 1;                          /* number of channel */
//
//	o2 = new_aubio_pitch ("default", win_s, hop_s, samplerate);
//	aubio_pitch_set_tolerance(o2, FFT_TOL);
//
//	in2 = new_fvec (win_s); // input buffer
//	out2 = new_fvec (1); // output candidates
//
//	//smpldata2 = (smpl_t*)malloc(win_s*sizeof(smpl_t));
}

void deinitialAubio(){
//	if(NULL != smpldata)
//		free(smpldata);
//
//	smpldata = NULL;

//	if(NULL != filter)
//		del_aubio_filter (filter);
//
//	filter = NULL;
//
//	if(NULL != peakpicker)
//		del_aubio_peakpicker(peakpicker);
//
//	peakpicker = NULL;
//
//	if(NULL != outPeak)
//		del_fvec(outPeak);
//
//	outPeak = NULL;
//
//	if(NULL != in)
//		del_fvec(in);
//
//	in = NULL;
//
//	if(NULL != out)
//		del_fvec(out);
//
//	out = NULL;
//	//del_aubio_pitchyinfft(o);
//
//	if(NULL != o)
//		del_aubio_pitch(o);
//
//	o = NULL;
}

void deinitialAubio2(){
//	if(NULL != smpldata2)
//		free(smpldata2);
//
//	smpldata2 = NULL;
//
//	if(NULL != in2)
//		del_fvec(in2);
//
//	in2=NULL;
//
//	if(NULL != out2)
//		del_fvec(out2);
//
//	out2 = NULL;
//
//	if(NULL != o2)
//		del_aubio_pitch(o2);
//
//	o2 = NULL;
}

//Frame Rate-Independent Low-Pass Filter
//http://phrogz.net/js/framerate-independent-low-pass-filter.html
void smoothArray(jshort *bytes, jshort smoothing, int iLen){
	jshort value = bytes[0];
	int i = 1;
	for(i = 1; i < iLen; i++){
		jshort curValue = bytes[i];
		value += (curValue - value) / smoothing;
		bytes[i] = value;
	}
}

void setSpeexPreprocess(SpeexPreprocessState* sps){
	if(NULL != sps){
		if(NS_INDEX < 0){
			int denoise = 1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DENOISE, &denoise);

			int iRet = 0;
			//speex_preprocess_ctl(sps, SPEEX_PREPROCESS_GET_NOISE_SUPPRESS, &iRet);
			//LOGI("recordAudio+, SPEEX_PREPROCESS_GET_NOISE_SUPPRESS:%d\n", iRet);

			iRet = NS_INDEX;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &iRet);
		}

		if(AGC_LEVEL >0){
			int i=1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_AGC, &i);
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_AGC_LEVEL, &AGC_LEVEL);
		}

		if(ENABLE_DEVERB){
			int i=1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB, &i);
			if(0 < DEVERB_DECAY){
				speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB_DECAY, &DEVERB_DECAY);
			}

			if(0 < DEVERB_LEVEL){
				speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB_LEVEL, &DEVERB_LEVEL);
			}
		}
	}
}

void runSpeexPreprocess(SpeexPreprocessState* sps, jshort *bytes){
	if(NULL != sps && NULL != bytes){
		if((NS_INDEX < 0 || AGC_LEVEL >0 || ENABLE_DEVERB)){
			int iRet = speex_preprocess_run(sps, bytes);
			//LOGI("runSpeexPreprocess+, speex_preprocess_run:%d\n", iRet);
		}
	}
}

//jfloat analyzeViaAubio(JNIEnv * env, jobject this, jshortArray array, int iBufSize, aubio_pitch_t* pitch_obj, fvec_t* inBuf, fvec_t* outBuf, SpeexPreprocessState* speexPrep){
//
//	DECLARE_JNIENV_WITHOUT_RETURN()
//	jfloat ret = 0.0;
////
////	jshort *bytes = (*jni_env)->GetShortArrayElements(jni_env, array, NULL);
////
////	runSpeexPreprocess(speexPrep, bytes);
////
////	int i = 0;
////	for(i = 0 ; i < iBufSize ; i++){
////		inBuf->data[i] = bytes[i];
////	}
////
////	if(NULL != filter)
////		aubio_filter_do(filter, inBuf);
////
////	if(NULL != peakpicker){
////		aubio_peakpicker_do(peakpicker, inBuf, outPeak);
////		if(0 < outPeak->data[0])
////			LOGE("##########################recordAudio-, peakpicker = %f, ts:%d", outPeak->data[0], getTickCount());
////	}
////
////	aubio_pitch_do(pitch_obj, inBuf, outBuf);
////	ret = outBuf->data[0];
////
////	(*jni_env)->ReleaseShortArrayElements(jni_env, array, bytes, 0);
//	return ret;
//}

JNIEXPORT jfloat JNICALL Java_com_example_aubiotest_AubioTestActivity_recordAudio(JNIEnv * env, jobject this, jshortArray array, int iBufSize, jboolean bReset)
{
	LOGD("recordAudio+, iBufSize:%d\n", iBufSize);
	DECLARE_JNIENV_WITHOUT_RETURN()
//	if(bReset){
//		//LOGI("recordAudio+, bReset Aubio --------------------------------------------------------\n", bReset);
//		deinitialAubio();
//	}
//
//	if(NULL == o){
//		initialAubio();
//	}
//
//	if(NULL == st && (NS_INDEX < 0 || AGC_LEVEL >0 || ENABLE_DEVERB)){
//		st = speex_preprocess_state_init(FRAME_SIZE, SAMPLE_RATE);
//		setSpeexPreprocess(st);
//	}

	return 0.0;//analyzeViaAubio(env, this, array, iBufSize, o, in, out, st);
}

JNIEXPORT jfloat JNICALL Java_com_example_aubiotest_AubioTestActivity_analyzeAudio(JNIEnv * env, jobject this, jshortArray array, int iBufSize, jboolean bReset){
	LOGD("analyzeAudio+, iBufSize:%d\n", iBufSize);
//	if(bReset){
//		deinitialAubio2();
//	}
//
//	if(NULL == o2){
//		initialAubio2();
//	}
//
//	if(NULL == st2 && (NS_INDEX < 0 || AGC_LEVEL >0 || ENABLE_DEVERB)){
//		st2 = speex_preprocess_state_init(FRAME_SIZE, SAMPLE_RATE);
//		setSpeexPreprocess(st2);
//	}

	return 0.0;//analyzeViaAubio(env, this, array, iBufSize, o2, in2, out2, st2);
}

void performSpeexPreprocess(JNIEnv * env, jobject this, jshortArray array, jboolean bReset, SpeexPreprocessState** speexPrep){
	DECLARE_JNIENV_WITHOUT_RETURN()

	if(NULL == *speexPrep && (NS_INDEX < 0 || AGC_LEVEL >0 || ENABLE_DEVERB)){
		*speexPrep = speex_preprocess_state_init(FRAME_SIZE, SAMPLE_RATE);
		setSpeexPreprocess(*speexPrep);
	}

	jshort *bytes = (*jni_env)->GetShortArrayElements(jni_env, array, NULL);

	runSpeexPreprocess(*speexPrep, bytes);

	(*jni_env)->ReleaseShortArrayElements(jni_env, array, bytes, 0);
}

static SpeexPreprocessState* stPreprocess;
JNIEXPORT void JNICALL Java_com_example_aubiotest_AubioTestActivity_runAudioPreprocess(JNIEnv * env, jobject this, jshortArray array, jboolean bReset)
{
	LOGD("runAudioPreprocess+\n");
	DECLARE_JNIENV_WITHOUT_RETURN()

	performSpeexPreprocess(env, this, array, bReset, &stPreprocess);
}

static SpeexPreprocessState* stPreprocessAC;
JNIEXPORT void JNICALL Java_com_example_aubiotest_AubioTestActivity_runAudioPreprocessAC(JNIEnv * env, jobject this, jshortArray array, jboolean bReset)
{
	DECLARE_JNIENV_WITHOUT_RETURN()
	performSpeexPreprocess(env, this, array, bReset, &stPreprocessAC);
}

JNIEXPORT jfloat JNICALL Java_com_example_aubiotest_AubioTestActivity_analyzeAudioViaDywa(JNIEnv * env, jobject this, jshortArray array, int iBufSize)
{
	//LOGE("recordAudio+, iBufSize:%d\n", iBufSize);
	DECLARE_JNIENV_WITHOUT_RETURN()

    //jshort *bytes = (*jni_env)->GetShortArrayElements(jni_env, array, NULL);

	jfloat ret;
//	double* smpldata = (smpl_t*)malloc(iBufSize*sizeof(double));
//	int i;
//
//	for(i=0 ; i<iBufSize ; i++){
//		smpldata[i] = bytes[i];
//	}
//
//	ret = dywapitch_computepitch(&pitchtracker, smpldata, 0, iBufSize);
//	(*jni_env)->ReleaseShortArrayElements(jni_env, array, bytes, 0);
//	//LOGE("analyzeAudioViaDywa-, ret = %f", ret);
//	free(smpldata);
	return ret;
}

//Audacity FFT analysis
static float *inBuffer = NULL;
static float *outBuffer = NULL;
static float *win = NULL;
static double wss ;
static int windowFunc = 3;//hannings

void performWindowFunc(float *winBuf){
	if(NULL != winBuf){
		int i=0;
		for(i=0; i < FRAME_SIZE; i++)
			winBuf[i] = 1.0;

		WindowFunc(windowFunc, FRAME_SIZE, winBuf);
	}
}

void initAudacity(){
	if(NULL == inBuffer){
		inBuffer = (float*)malloc(FRAME_SIZE*sizeof(float));
	}

	if(NULL == outBuffer){
		outBuffer = (float*)malloc(FRAME_SIZE*sizeof(float));//new float[FRAME_SIZE];
	}

	if(NULL == win){
		win = (float*)malloc(FRAME_SIZE*sizeof(float));//new float[FRAME_SIZE];
	}

	performWindowFunc(win);

	wss = 0;
	int i=0;
	for(i=0; i<FRAME_SIZE; i++)
	   wss += win[i];

	if(wss > 0)
	   wss = 4.0 / (wss*wss);
	else
	   wss = 1.0;
}

void deinitAudacity(){
	if(NULL != inBuffer){
		free(inBuffer);
		inBuffer = NULL;
	}

	if(NULL == outBuffer){
		free(outBuffer);
		outBuffer = NULL;
	}

	if(NULL == win){
		free(win);
		win = NULL;	}
}

jfloat performAudacityFFT(JNIEnv * env, jobject this, jshortArray array, jboolean bReset, SpeexPreprocessState** speexPrep, int iLastDet, jintArray iFFTValues){
	//LOGE("performAudacityFFT+\n");

	DECLARE_JNIENV_WITHOUT_RETURN()
	jfloat fRet = 0.0;
	if(bReset){
		deinitAudacity();
		initAudacity();
	}

	if(NULL == inBuffer){
		initAudacity();
	}

	performSpeexPreprocess(env, this, array, bReset, speexPrep);

	performWindowFunc(win);

	int iDx = 0;
	int iDx2 = 0;
	int iDx3 = 0;
	int iDx4 = 0;
	int iDx5 = 0;

	jshort *bytes = (*jni_env)->GetShortArrayElements(jni_env, array, NULL);
	if(NULL != bytes){
		int i=0;
		for (i = 0; i < FRAME_SIZE; i++)
			inBuffer[i] = win[i] * bytes[i];

		PowerSpectrum(FRAME_SIZE, inBuffer, outBuffer);

		(*jni_env)->ReleaseShortArrayElements(jni_env, array, bytes, 0);

		fRet = outBuffer[0];
		for (i = HIGH_PASS_INDEX; i < HALF_FRAME_SIZE && i <= LOW_PASS_INDEX; i++){
			//LOGE("analyzeAudioViaAudacity+, outBuffer[%d] = %f\n", i, outBuffer[i]);
			if(outBuffer[i] > fRet){
				iDx5 = iDx4;
				iDx4 = iDx3;
				iDx3 = iDx2;
				iDx2 = iDx;
				iDx  = i;
				fRet = outBuffer[i];
			}
		}

		if(NULL != iFFTValues){
			jint *iDxValues = NULL;
			iDxValues = (*jni_env)->GetIntArrayElements(jni_env, iFFTValues, NULL);
			if(NULL != iDxValues){
				iDxValues[0] = iDx;
				iDxValues[1] = iDx2;
				iDxValues[2] = iDx3;
				iDxValues[3] = iDx4;
				iDxValues[4] = iDx5;
				(*jni_env)->ReleaseIntArrayElements(jni_env, iFFTValues, iDxValues, 0);
			}
		}

		if(0 < iLastDet){
			//LOGE("performAudacityFFT()-------------------------, iLastDetTone = [%.2f]=>%d, iDx0~5 = [%d, %d, %d, %d, %d]\n", iLastDet*BIN_SIZE, iLastDet, iDx, iDx2, iDx3, iDx4, iDx5);
			if(iDx > 0 && iDx2 >0 && iLastDet > 0 && (iDx - iLastDet <=1 && iDx - iLastDet >= -1) && (iDx - iDx2 >=2 || iDx - iDx2 <= -2)){
				LOGE("performAudacityFFT()^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^, iLastDetTone=%.2f, change from %.2f to %.2f \n", iLastDet*BIN_SIZE, iDx*BIN_SIZE,  iDx2*BIN_SIZE);
				iDx = iDx2;
			}
		}
	}else{
		LOGE("performAudacityFFT(), bytes is null\n");
	}

	return BIN_SIZE*iDx;
}

JNIEXPORT jfloat JNICALL Java_com_example_aubiotest_AubioTestActivity_analyzeAudioViaAudacity(JNIEnv * env, jobject this, jshortArray array, int iBufSize, jboolean bReset ,int iLastDetect, jintArray iFFTValues){
	return performAudacityFFT(env, this, array, bReset, &stPreprocess, iLastDetect, iFFTValues);
}

JNIEXPORT jfloat JNICALL Java_com_example_aubiotest_AubioTestActivity_analyzeAudioViaAudacityAC(JNIEnv * env, jobject this, jshortArray array, int iBufSize, jboolean bReset,int iLastDetect, jintArray iFFTValues){
	return performAudacityFFT(env, this, array, bReset, &stPreprocessAC, iLastDetect, iFFTValues);
}

JNIEXPORT void JNICALL Java_com_example_aubiotest_AubioTestActivity_endRecord(JNIEnv * env, jobject this)
{
	LOGE("endRecord()+");
//    del_aubio_pitchdetection(o);
//    del_fvec(in);

	deinitAudacity();
	//deinitialAubio2();
	//deinitialAubio();
	//aubio_cleanup();

	LOGE("endRecord-");
}

