
#ifndef __JAVA_EXPORT__
#define __JAVA_EXPORT__

#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/reedsolomon/ReedSolomonEncoder.h>
#include "sp_config.h"
#include "beseye_sound_pairing.h"
#include "FreqGenerator.h"
#include <pthread.h>

using zxing::Ref;
using zxing::ArrayRef;
using zxing::GenericGF;
using zxing::ReedSolomonEncoder;

#ifndef GEN_TONE_ONLY
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>
#include "AudioTest.h"
using zxing::ReedSolomonDecoder;
#endif //GEN_TONE_ONLY

#ifdef __cplusplus
 extern "C" {

#include <stdlib.h>
#include <assert.h>
#include <stdint.h>

#include <util.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <math.h>
#endif //__cplusplus

#ifndef GEN_TONE_ONLY
#include <speex_preprocess.h>
#include "FFT.h"
#endif //GEN_TONE_ONLY

#include "utils.h"
#endif //__JAVA_EXPORT__

#define DECLARE_JNIENV_WITHOUT_RETURN() \
	JNIEnv* jni_env; \
	if (JNI_OK != jvm->AttachCurrentThread(&jni_env, NULL)) {  return; } \

#define DECLARE_JNIENV_WITH_RETURN() \
	JNIEnv* jni_env; \
	if (JNI_OK != jvm->AttachCurrentThread(&jni_env, NULL)) {  return 0; } \


//static jmethodID  playMethod, getBufMethod, getBufSizeMethod = NULL;
static jobject   jni_host = NULL;
static JavaVM*   jvm = NULL;

void Delegate_detachCurrentThread(){
 	DECLARE_JNIENV_WITHOUT_RETURN()
 	jvm->DetachCurrentThread();
}

/* Main activity */
static jclass mActivityClass = NULL;
static jobject mThisObj= NULL;
/* method signatures */
static jmethodID midAudioInit= NULL;
static jmethodID midAudioGetBufSize= NULL;
static jmethodID midAudioWriteShortBuffer= NULL;
static jmethodID midAudioWriteByteBuffer= NULL;
static jmethodID midAudioQuit= NULL;

static jmethodID midOnStartGen= NULL;
static jmethodID midOnStopGen= NULL;
static jmethodID midOnFreqChanged= NULL;
static jmethodID midOnErrCorrectionCode= NULL;

#ifndef GEN_TONE_ONLY
static jmethodID midAudioRecordInit= NULL;
static jmethodID midGetAudioRecordBuf= NULL;
static jmethodID midAudioRecordQuit= NULL;

static jmethodID midWaitForAnalyze= NULL;
static jmethodID midUpdateFreq= NULL;
static jmethodID midResetData= NULL;
static jmethodID midFeedbackMatchRet= NULL;

static jmethodID midsendBTMsg= NULL;
#endif

//JNIEXPORT jboolean JNICALL Java_com_example_aubiotest_AubioTestActivity_nativeClassInit(JNIEnv *env, jclass jcls)
JNIEXPORT jboolean JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_nativeClassInit(JNIEnv *env, jclass jcls)
{
	LOGE("nativeClassInit()+");
	if (JNI_OK != env->GetJavaVM(&jvm)){
		LOGE("GetJavaVM failed");
		return 0;
	}

	mActivityClass = (jclass)(env->NewGlobalRef(jcls));

	midAudioInit = env->GetStaticMethodID(mActivityClass,
								"audioInit", "(IZZI)I");

	midAudioGetBufSize = env->GetStaticMethodID(mActivityClass,
								"getAudioBufSize", "(I)I");

	midAudioWriteShortBuffer = env->GetStaticMethodID(mActivityClass,
								"audioWriteShortBuffer", "([SI)V");

	midAudioWriteByteBuffer = env->GetStaticMethodID(mActivityClass,
								"audioWriteByteBuffer", "([B)V");

	midAudioQuit = env->GetStaticMethodID(mActivityClass,
								"audioQuit", "()V");
#ifdef GEN_TONE_ONLY
	midOnStartGen = env->GetStaticMethodID(mActivityClass,
								"onStartGen", "(Ljava/lang/String;)V");

	midOnStopGen = env->GetStaticMethodID(mActivityClass,
								"onStopGen", "(Ljava/lang/String;)V");

	midOnFreqChanged = env->GetStaticMethodID(mActivityClass,
								"onCurFreqChanged", "(D)V");

	midOnErrCorrectionCode = env->GetStaticMethodID(mActivityClass,
								"onErrCorrectionCode", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
#endif
#ifndef GEN_TONE_ONLY
	midAudioRecordInit = env->GetStaticMethodID(mActivityClass,
								"audioRecordInit", "(IZ)I");

	midGetAudioRecordBuf = env->GetStaticMethodID(mActivityClass,
								"getAudioRecordBuf", "(I)[S");

	midAudioRecordQuit = env->GetStaticMethodID(mActivityClass,
								"audioRecordDeinit", "()V");

	SoundPair_Config::init();

//	jclass cls = env->FindClass("com/example/aubiotest/AubioTestActivity");
//	if(NULL == cls){
//		LOGE("cls is empty");
//		return 0;
//	}

	midWaitForAnalyze = env->GetMethodID(mActivityClass, "waitForAnalyze", "()V");
	if(NULL == midWaitForAnalyze){
		LOGE("waitForAnalyze is empty");
		return 0;
	}

	midUpdateFreq = env->GetMethodID(mActivityClass, "updateFreq", "(JF)V");
	if(NULL == midUpdateFreq){
		LOGE("midUpdateFreq is empty");
		return 0;
	}

	midResetData = env->GetMethodID(mActivityClass, "resetData", "()V");
	if(NULL == midResetData){
		LOGE("midResetData is empty");
		return 0;
	}


	midFeedbackMatchRet = env->GetMethodID(mActivityClass, "feedbackMatchRet", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZ)V");
	if(NULL == midFeedbackMatchRet){
		LOGE("midFeedbackMatchRet is empty");
		return 0;
	}

	midsendBTMsg = env->GetMethodID(mActivityClass, "sendBTMsg", "(Ljava/lang/String;)V");
	if(NULL == midsendBTMsg){
		LOGE("midsendBTMsg is empty");
		return 0;
	}
#endif

	LOGE("nativeClassInit()-");
	return 1;
}

/*
 * Audio support
 */
static jboolean audioBuffer16Bit = JNI_FALSE;
static jboolean audioBufferStereo = JNI_FALSE;
static jobject audioBuffer = NULL;
static void* audioBufferPinned = NULL;

int Delegate_OpenAudioDevice(int sampleRate, int is16Bit, int channelCount, int desiredBufferFrames){
	DECLARE_JNIENV_WITH_RETURN()

    int audioBufferFrames;

    if (!jni_env) {
        LOGE("callback_handler: failed to attach current thread");
    }
   // Delegate_SetupThread();

    //__android_log_print(ANDROID_LOG_VERBOSE, "SDL", "SDL audio: opening device");
    audioBuffer16Bit = is16Bit;
    audioBufferStereo = channelCount > 1;

    audioBufferFrames = desiredBufferFrames = Delegate_GetAudioBufferSize(sampleRate);

    if (jni_env->CallStaticIntMethod(mActivityClass, midAudioInit, sampleRate, audioBuffer16Bit, audioBufferStereo, desiredBufferFrames) != 0) {
        /* Error during audio initialization */
        LOGW("SDL audio: error on AudioTrack initialization!");
        return 0;
    }

    /* Allocating the audio buffer from the Java side and passing it as the return value for audioInit no longer works on
     * Android >= 4.2 due to a "stale global reference" error. So now we allocate this buffer directly from this side. */

    if (is16Bit) {
        jshortArray audioBufferLocal = jni_env->NewShortArray(desiredBufferFrames * (audioBufferStereo ? 2 : 1));
        if (audioBufferLocal) {
            audioBuffer = jni_env->NewGlobalRef(audioBufferLocal);
            jni_env->DeleteLocalRef(audioBufferLocal);
        }
    }
    else {
        jbyteArray audioBufferLocal = jni_env->NewByteArray(desiredBufferFrames * (audioBufferStereo ? 2 : 1));
        if (audioBufferLocal) {
            audioBuffer = jni_env->NewGlobalRef(audioBufferLocal);
            jni_env->DeleteLocalRef(audioBufferLocal);
        }
    }

    if (audioBuffer == NULL) {
        __android_log_print(ANDROID_LOG_WARN, "SDL", "SDL audio: could not allocate an audio buffer!");
        return 0;
    }

    jboolean isCopy = JNI_FALSE;
    if (audioBuffer16Bit) {
        audioBufferPinned = jni_env->GetShortArrayElements((jshortArray)audioBuffer, &isCopy);
        audioBufferFrames = jni_env->GetArrayLength((jshortArray)audioBuffer);
    } else {
        audioBufferPinned = jni_env->GetByteArrayElements((jbyteArray)audioBuffer, &isCopy);
        audioBufferFrames = jni_env->GetArrayLength( (jbyteArray)audioBuffer);
    }
//    if (audioBufferStereo) {
//        audioBufferFrames /= 2;
//    }
    //jvm->DetachCurrentThread();
    return audioBufferFrames;
}

void * Delegate_GetAudioBuffer(){
    return audioBufferPinned;
}

int Delegate_GetAudioBufferSize(int sampleRate){
	DECLARE_JNIENV_WITH_RETURN()
    int iRet = jni_env->CallStaticIntMethod(mActivityClass, midAudioGetBufSize, sampleRate);
	//jvm->DetachCurrentThread();
	return iRet;
}

void Delegate_WriteAudioBuffer(int iLen){
	//LOGE("Delegate_WriteAudioBuffer(), iLen:%d\n",iLen);
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(audioBuffer16Bit){
		jni_env->ReleaseShortArrayElements((jshortArray)audioBuffer, (jshort *)audioBufferPinned, JNI_COMMIT);
		jni_env->CallStaticVoidMethod(mActivityClass, midAudioWriteShortBuffer, (jshortArray)audioBuffer, iLen);
	}else{
		jni_env->ReleaseByteArrayElements((jbyteArray)audioBuffer, (jbyte *)audioBufferPinned, JNI_COMMIT);
		jni_env->CallStaticVoidMethod(mActivityClass, midAudioWriteByteBuffer, (jbyteArray)audioBuffer);
	}

	//LOGW("Delegate_WriteAudioBuffer(), audioBuffer[100]=%d", audioBuffer[100]);
    /* JNI_COMMIT means the changes are committed to the VM but the buffer remains pinned */
	//jvm->DetachCurrentThread();
}

void Delegate_CloseAudioDevice(){
	DECLARE_JNIENV_WITHOUT_RETURN()

    jni_env->CallStaticVoidMethod(mActivityClass, midAudioQuit);

    if (audioBuffer) {
        jni_env->DeleteGlobalRef(audioBuffer);
        audioBuffer = NULL;
        audioBufferPinned = NULL;
    }
    //jvm->DetachCurrentThread();
}

jstring str2jstring(const string pat) {
	DECLARE_JNIENV_WITH_RETURN()
	jstring ret = (jstring)jni_env->NewStringUTF(pat.c_str());
	jni_env->DeleteLocalRef(ret);
    return ret;
}
#ifdef GEN_TONE_ONLY
class OnPlayToneCallbackReceiver : public IOnPlayToneCallback{
public:
	OnPlayToneCallbackReceiver(){};
	virtual ~OnPlayToneCallbackReceiver(){};

	virtual void onStartGen(string strCode){
		//LOGW("onStartGen()");
		DECLARE_JNIENV_WITHOUT_RETURN()
		jstring code = (jstring)jni_env->NewStringUTF(strCode.c_str());
		jni_env->CallStaticVoidMethod(mActivityClass, midOnStartGen, code);
		jni_env->DeleteLocalRef(code);

		//LOGW("onStartGen()-");
		//jvm->DetachCurrentThread();
	}

	virtual void onStopGen(string strCode){
		DECLARE_JNIENV_WITHOUT_RETURN()
		jstring code = (jstring)jni_env->NewStringUTF(strCode.c_str());
		jni_env->CallStaticVoidMethod(mActivityClass, midOnStopGen, code);
		jni_env->DeleteLocalRef(code);
	}

	virtual void onCurFreqChanged(double dFreq){
		DECLARE_JNIENV_WITHOUT_RETURN()
		jni_env->CallStaticVoidMethod(mActivityClass, midOnFreqChanged, dFreq);
	}

	virtual void onErrCorrectionCode(string strCode, string strEC, string strEncodeMark){
		DECLARE_JNIENV_WITHOUT_RETURN()
		jstring code = (jstring)jni_env->NewStringUTF(strCode.c_str());
		jstring codeEc = (jstring)jni_env->NewStringUTF(strEC.c_str());
		jstring codeMark = (jstring)jni_env->NewStringUTF(strEncodeMark.c_str());
		jni_env->CallStaticVoidMethod(mActivityClass, midOnErrCorrectionCode, code, codeEc, codeMark);
		jni_env->DeleteLocalRef(code);
		jni_env->DeleteLocalRef(codeEc);
		jni_env->DeleteLocalRef(codeMark);
	}
};

static OnPlayToneCallbackReceiver* sOnPlayToneCallbackReceiver = new OnPlayToneCallbackReceiver();

JNIEXPORT jboolean JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_playCode(JNIEnv * env, jobject thisObj, jstring strCode, jboolean bNeedEncode){
	DECLARE_JNIENV_WITH_RETURN()
	SoundPair_Config::init();
	char *code = (char *)jni_env->GetStringUTFChars( strCode, 0);
	string curCode(code);
	jni_env->ReleaseStringUTFChars( strCode, code);
	FreqGenerator::getInstance()->setOnPlayToneCallback(sOnPlayToneCallbackReceiver);
	return FreqGenerator::getInstance()->playCode2(curCode, bNeedEncode);
}

JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_finishPlayCode(JNIEnv * env, jobject thisObj){
	DECLARE_JNIENV_WITHOUT_RETURN()
	FreqGenerator::getInstance()->stopPlay2();
	SoundPair_Config::uninit();
}
#endif
#ifndef GEN_TONE_ONLY

int Delegate_OpenAudioRecordDevice(int sampleRate, int is16Bit){
	DECLARE_JNIENV_WITH_RETURN()

	if (jni_env->CallStaticIntMethod(mActivityClass, midAudioRecordInit, sampleRate, is16Bit) != 0) {
		/* Error during audio initialization */
		LOGW("SDL audio: error on AudioRecord initialization!");
		return 0;
	}
	//jvm->DetachCurrentThread();
	return 1;
}

int Delegate_getAudioRecordBuf(ArrayRef<short> buf, int iLen){
	//LOGW("Delegate_getAudioRecordBuf()+, buf[99] = %d", buf[99]);
	//LOGW("Delegate_getAudioRecordBuf()+");
	DECLARE_JNIENV_WITH_RETURN()
	int ret = 0;
	//LOGW("Delegate_getAudioRecordBuf()+1");
	jshortArray array = (jshortArray)jni_env->CallStaticObjectMethod(mActivityClass, midGetAudioRecordBuf, iLen);
	//LOGW("Delegate_getAudioRecordBuf()+2");
	if(NULL != array){
		//LOGW("Delegate_getAudioRecordBuf()+3");
		int iRealLen = jni_env->GetArrayLength(array);
		//LOGW("Delegate_getAudioRecordBuf()+4");
		jshort* sBuf = jni_env->GetShortArrayElements(array,JNI_FALSE);
		//LOGW("Delegate_getAudioRecordBuf()+5");
		if(sBuf){
			//LOGW("Delegate_getAudioRecordBuf()+6");
			memcpy(&buf[0], sBuf, ((iLen > iRealLen)?iRealLen:iLen)*sizeof(jshort));
			//LOGW("Delegate_getAudioRecordBuf()+7");
			jni_env->ReleaseShortArrayElements(array,sBuf,0);
			//LOGW("Delegate_getAudioRecordBuf()+8");
			jni_env->DeleteLocalRef(array);
			//LOGW("Delegate_getAudioRecordBuf()+9");
			ret = iRealLen;
		}
		//LOGW("Delegate_getAudioRecordBuf()-, buf[99] = %d", buf[99]);
	}
//	jshortArray audioBufferLocal = jni_env->NewShortArray(iLen);
//	jni_env->SetShortArrayRegion(audioBufferLocal, 0, iLen, buf);
//	int ret = jni_env->CallStaticIntMethod(mActivityClass, midGetAudioRecordBuf, audioBufferLocal, iLen);
//	jni_env->DeleteLocalRef(audioBufferLocal);
	//LOGW("Delegate_getAudioRecordBuf()-, buf[99] = %d", buf[99]);
	//jvm->DetachCurrentThread();
	return ret;
}

void Delegate_CloseAudioRecordDevice(){
	DECLARE_JNIENV_WITHOUT_RETURN()
	jni_env->CallStaticIntMethod(mActivityClass, midAudioRecordQuit);
	//jvm->DetachCurrentThread();
}

void Delegate_UpdateFreq(msec_t lts, float freq){
	DECLARE_JNIENV_WITHOUT_RETURN()
	if(midUpdateFreq)
		jni_env->CallVoidMethod(mThisObj, midUpdateFreq, lts, freq);
	//jvm->DetachCurrentThread();
}

void Delegate_ResetData(){
	DECLARE_JNIENV_WITHOUT_RETURN()
	jni_env->CallVoidMethod(mThisObj, midResetData);
	//jvm->DetachCurrentThread();
}

void Delegate_FeedbackMatchResult(string strCode, string strECCode, string strEncodeMark, string strDecode, string strDecodeUnmark, string strDecodeMark, int iMatchDesc, bool bFromAutoCorrection){
	DECLARE_JNIENV_WITHOUT_RETURN()
//			stringstream strLog;
//	strLog <<"Delegate_FeedbackMatchResult(), Case "<<iMatchDesc<<" ===>>> "<<bFromAutoCorrection<<"\n" <<
//								"curCode          = ["<<strCode<<"], \n" <<
//								"curECCode        = ["<<strECCode<<"], \n" <<
//								"curEncodeMark    = ["<<strEncodeMark<<"], \n" <<
//								"strDecodeMark    = ["<<strDecodeMark<<"]\n"<<
//								"strDecodeUnmark  = ["<<strDecodeUnmark<<"] \n"<<
//								"strCode          = ["<<strCode<<"]\n";
//	LOGE(strLog.str().c_str());
	//jstring test = str2jstring(strCode);
	//LOGE("test=%s", test);
	jni_env->CallVoidMethod(mThisObj, midFeedbackMatchRet, str2jstring(strCode),
			 	 	 	 	 	 	 	 	 	 	 	   str2jstring(strECCode),
			 	 	 	 	 	 	 	 	 	 	 	   str2jstring(strEncodeMark),
			 	 	 	 	 	 	 	 	 	 	 	   str2jstring(strDecode),
			 	 	 	 	 	 	 	 	 	 	 	   str2jstring(strDecodeUnmark),
			 	 	 	 	 	 	 	 	 	 	 	   str2jstring(strDecodeMark),
			 	 	 	 	 	 	 	 	 	 	 	   iMatchDesc,
			 	 	 	 	 	 	 	 	 	 	 	   bFromAutoCorrection);
	//jvm->DetachCurrentThread();

}

void Delegate_SendMsgByBT(string strCode){
	LOGE("Delegate_SendMsgByBT(), ------------------------------->strCode:%s\n", strCode.c_str());
	DECLARE_JNIENV_WITHOUT_RETURN()
	jni_env->CallVoidMethod(mThisObj, midsendBTMsg, str2jstring(strCode));
}

//
//class OnPlayToneCallback : public IOnPlayToneCallback{
//public:
//	virtual void onStartGen(string strCode){
//		LOGI("onStartGen() strCode:%s\n",strCode.c_str());
//	}
//
//	virtual void onStopGen(string strCode){
//		LOGI("onStopGen() strCode:%s\n",strCode.c_str());
//	}
//
//	virtual void onCurFreqChanged(double dFreq){
//		LOGI("onCurFreqChanged() dFreq:%f\n",dFreq);
//	}
//
//	virtual void onErrCorrectionCode(string strCode, string strEC, string strEncodeMark){
//		LOGI("onErrCorrectionCode() strCode:%s, strEC:%s, strEncodeMark:%s\n",strCode.c_str(), strEC.c_str(), strEncodeMark.c_str());
//	}
//};
//
//static int iStopGenerator = 0;
//
//JNIEXPORT jint JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_launchFreqGenerator(JNIEnv * env, jobject thisObj, jstring strCurCode, jint iDigitalToTest)
//{
//	DECLARE_JNIENV_WITH_RETURN()
//
//	LOGE("launchFreqGenerator+\n");
//	iStopGenerator = 0;
//	OnPlayToneCallback cb;
//	FreqGenerator::getInstance()->setOnPlayToneCallback(&cb);
//	char *nativeString = (char *)jni_env->GetStringUTFChars( strCurCode, 0);
//	string curCode(nativeString);
//	LOGE("launchFreqGenerator+, strCurCode:%s\n", curCode.c_str());
//	jni_env->ReleaseStringUTFChars( strCurCode, nativeString);
//
//
//	while(0 < (curCode = FreqGenerator::genNextRandomData(iDigitalToTest)).length()){
//		LOGE("launchFreqGenerator+, in loop\n");
//		if(iStopGenerator){
//			LOGE("runAutoTest(), break loop");
//			FreqGenerator::getInstance()->stopPlay2();
//			break;
//		}
//
////		if(false == checkTimer()){
////			enterStopMode();
////			break;
////		}
//		LOGE("launchFreqGenerator+, FreqGenerator::getInstance() is %d\n", FreqGenerator::getInstance());
//
////		if(SELF_TEST)
////			FreqGenerator.getInstance().playCode3(/*lstTestData.get(i)*/curCode, true);
////		else
//			FreqGenerator::getInstance()->playCode2(/*lstTestData.get(i)*/curCode, true);
//
//		jni_env->CallVoidMethod(thisObj, midWaitForAnalyze);
//	}
//	return 0;
//}
//
//
//JNIEXPORT jint JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_stopFreqGenerator(JNIEnv * env, jobject thisObj)
//{
//	LOGD("stopFreqGenerator+,\n");
//	iStopGenerator = 1;
//	return 0;
//}

JNIEXPORT jint JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_startAutoTest(JNIEnv * env, jobject thisObj, jstring strCurCode, jint iDigitalToTest){
	DECLARE_JNIENV_WITH_RETURN()
	mThisObj = (jobject)(env->NewGlobalRef(thisObj));
	char *nativeString = (char *)jni_env->GetStringUTFChars( strCurCode, 0);
	string curCode(nativeString?nativeString:"");
	LOGE("startAutoTest+, strCurCode:[%s]\n", curCode.c_str());
	jni_env->ReleaseStringUTFChars( strCurCode, nativeString);
	bool ret = AudioTest::getInstance()->startAutoTest(curCode, iDigitalToTest);
	return ret;
}

JNIEXPORT jint JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_stopAutoTest(JNIEnv * env, jobject thisObj){
	DECLARE_JNIENV_WITH_RETURN()
	return AudioTest::getInstance()->stopAutoTest();
}

JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_setTestMode(JNIEnv * env, jobject thisObj, jboolean senderMode, jboolean receiverMode){
	if(senderMode){
		AudioTest::getInstance()->setSenderMode();
	}else if(receiverMode){
		AudioTest::getInstance()->setReceiverMode();
	}else{
		AudioTest::getInstance()->setAutoTestMode();
	}
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

//static SpeexPreprocessState* st, *st2;

static int sSampleRate = 16000;
static int sFrameSize  = 512;
static int sHalfFrameSize  = 0;
static float sBinSize = 0;
static int sNSIndex  = 0;
static float sAGCLevel = 0;
static float sFFTTol = 0.5f;
static jboolean	sEnableDeverb = 0;
static float sDeverbDecay = 0.0f;
static float sDeverbLevel = 0.0f;
static int LOW_PASS_CRITERIA_FREQ = 1000;
static int sLowPassIndex = 0;

static int HIGH_PASS_CRITERIA_FREQ = 3300;
static int sHighPassIndex = 0;

static Ref<GenericGF> gf = GenericGF::QR_CODE_FIELD_256;
static ReedSolomonDecoder rsDecoder(gf);
static ReedSolomonEncoder rsEncoder(gf);

JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_startRecord(JNIEnv * env, jobject obj, int fd, int iSampleRate, int iFrameSize, int iNSIndex, float iAGCLevel, jboolean bDeverb, float fDeverbDecay, float dDeverbLevel)
{
	//LOGE("startRecord()+, HAVE_FFTW3:%d\n", HAVE_FFTW3);

	sSampleRate = iSampleRate;
	sFrameSize  = iFrameSize;
	sHalfFrameSize  = sFrameSize/2;
	sBinSize = (sSampleRate)/(float)sFrameSize;
	sNSIndex = iNSIndex;
	sAGCLevel = iAGCLevel;
	sEnableDeverb = bDeverb;
	sDeverbDecay = fDeverbDecay;
	sDeverbLevel = dDeverbLevel;

	int i = 1;
	while(sBinSize > 0){
		if(sBinSize*i++ > LOW_PASS_CRITERIA_FREQ){
			sLowPassIndex = i -1;
			break;
		}
	}

	while(sBinSize > 0){
		if(sBinSize*i++ > HIGH_PASS_CRITERIA_FREQ){
			sHighPassIndex = i -1;
			break;
		}
	}

	LOGE("startRecord()+, iBufSize:%d, sSampleRate:%d, sFrameSize:%d, sLowPassIndex:%d, sHighPassIndex:%d\n", fd, sSampleRate, sFrameSize, sLowPassIndex, sHighPassIndex);

	LOGE("startRecord()-");
}

//Frame Rate-Independent Low-Pass Filter
//http://phrogz.net/js/framerate-independent-low-pass-filter.html
//void smoothArray(jshort *bytes, jshort smoothing, int iLen){
//	jshort value = bytes[0];
//	int i = 1;
//	for(i = 1; i < iLen; i++){
//		jshort curValue = bytes[i];
//		value += (curValue - value) / smoothing;
//		bytes[i] = value;
//	}
//}

void setSpeexPreprocess(SpeexPreprocessState* sps){
	if(NULL != sps){
		if(sNSIndex < 0){
			int denoise = 1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DENOISE, &denoise);

			int iRet = 0;
			//speex_preprocess_ctl(sps, SPEEX_PREPROCESS_GET_NOISE_SUPPRESS, &iRet);
			//LOGI("recordAudio+, SPEEX_PREPROCESS_GET_NOISE_SUPPRESS:%d\n", iRet);

			iRet = sNSIndex;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &iRet);
		}

		if(sAGCLevel >0){
			int i=1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_AGC, &i);
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_AGC_LEVEL, &sAGCLevel);
		}

		if(sEnableDeverb){
			int i=1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB, &i);
			if(0 < sDeverbDecay){
				speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB_DECAY, &sDeverbDecay);
			}

			if(0 < sDeverbLevel){
				speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB_LEVEL, &sDeverbLevel);
			}
		}
	}
}

void runSpeexPreprocess(SpeexPreprocessState* sps, jshort *bytes){
	if(NULL != sps && NULL != bytes){
		if((sNSIndex < 0 || sAGCLevel >0 || sEnableDeverb)){
			int iRet = speex_preprocess_run(sps, bytes);
			//LOGI("runSpeexPreprocess+, speex_preprocess_run:%d\n", iRet);
		}
	}
}

//jfloat analyzeViaAubio(JNIEnv * env, jobject thisObj, jshortArray array, int iBufSize, aubio_pitch_t* pitch_obj, fvec_t* inBuf, fvec_t* outBuf, SpeexPreprocessState* speexPrep){
//
//	DECLARE_JNIENV_WITHOUT_RETURN()
//	jfloat ret = 0.0;
////
////	jshort *bytes = jni_env->GetShortArrayElements(array, NULL);
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
////	jni_env->ReleaseShortArrayElements(array, bytes, 0);
//	return ret;
//}

JNIEXPORT jfloat JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_recordAudio(JNIEnv * env, jobject thisObj, jshortArray array, int iBufSize, jboolean bReset)
{
	LOGD("recordAudio+, iBufSize:%d\n", iBufSize);
//	DECLARE_JNIENV_WITHOUT_RETURN()
//	if(bReset){
//		//LOGI("recordAudio+, bReset Aubio --------------------------------------------------------\n", bReset);
//		deinitialAubio();
//	}
//
//	if(NULL == o){
//		initialAubio();
//	}
//
//	if(NULL == st && (sNSIndex < 0 || sAGCLevel >0 || sEnableDeverb)){
//		st = speex_preprocess_state_init(sFrameSize, sSampleRate);
//		setSpeexPreprocess(st);
//	}

	return 0.0;//analyzeViaAubio(env, thisObj, array, iBufSize, o, in, out, st);
}

JNIEXPORT jfloat JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_analyzeAudio(JNIEnv * env, jobject thisObj, jshortArray array, int iBufSize, jboolean bReset){
	LOGD("analyzeAudio+, iBufSize:%d\n", iBufSize);
//	if(bReset){
//		deinitialAubio2();
//	}
//
//	if(NULL == o2){
//		initialAubio2();
//	}
//
//	if(NULL == st2 && (sNSIndex < 0 || sAGCLevel >0 || sEnableDeverb)){
//		st2 = speex_preprocess_state_init(sFrameSize, sSampleRate);
//		setSpeexPreprocess(st2);
//	}

	return 0.0;//analyzeViaAubio(env, thisObj, array, iBufSize, o2, in2, out2, st2);
}

void performSpeexPreprocess(JNIEnv * env, jobject thisObj, jshortArray array, jboolean bReset, SpeexPreprocessState** speexPrep){
	DECLARE_JNIENV_WITHOUT_RETURN()

	if(NULL == *speexPrep && (sNSIndex < 0 || sAGCLevel >0 || sEnableDeverb)){
		*speexPrep = speex_preprocess_state_init(sFrameSize, sSampleRate);
		setSpeexPreprocess(*speexPrep);
	}

	jshort *bytes = jni_env->GetShortArrayElements(array, NULL);

	runSpeexPreprocess(*speexPrep, bytes);

	jni_env->ReleaseShortArrayElements(array, bytes, 0);
}

static SpeexPreprocessState* stPreprocess;
JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_runAudioPreprocess(JNIEnv * env, jobject thisObj, jshortArray array, jboolean bReset)
{
	LOGD("runAudioPreprocess+\n");
	DECLARE_JNIENV_WITHOUT_RETURN()

	performSpeexPreprocess(env, thisObj, array, bReset, &stPreprocess);
}

static SpeexPreprocessState* stPreprocessAC;
JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_runAudioPreprocessAC(JNIEnv * env, jobject thisObj, jshortArray array, jboolean bReset)
{
	DECLARE_JNIENV_WITHOUT_RETURN()
	performSpeexPreprocess(env, thisObj, array, bReset, &stPreprocessAC);
}

JNIEXPORT jfloat JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_analyzeAudioViaDywa(JNIEnv * env, jobject thisObj, jshortArray array, int iBufSize)
{
	//LOGE("recordAudio+, iBufSize:%d\n", iBufSize);
//	DECLARE_JNIENV_WITHOUT_RETURN()

    //jshort *bytes = jni_env->GetShortArrayElements(array, NULL);

	jfloat ret;
//	double* smpldata = (smpl_t*)malloc(iBufSize*sizeof(double));
//	int i;
//
//	for(i=0 ; i<iBufSize ; i++){
//		smpldata[i] = bytes[i];
//	}
//
//	ret = dywapitch_computepitch(&pitchtracker, smpldata, 0, iBufSize);
//	jni_env->ReleaseShortArrayElements(array, bytes, 0);
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
		for(i=0; i < sFrameSize; i++)
			winBuf[i] = 1.0;

		WindowFunc(windowFunc, sFrameSize, winBuf);
	}
}

void initAudacity(){
	if(NULL == inBuffer){
		inBuffer = (float*)malloc(sFrameSize*sizeof(float));
	}

	if(NULL == outBuffer){
		outBuffer = (float*)malloc(sFrameSize*sizeof(float));//new float[sFrameSize];
	}

	if(NULL == win){
		win = (float*)malloc(sFrameSize*sizeof(float));//new float[sFrameSize];
	}

	performWindowFunc(win);

	wss = 0;
	int i=0;
	for(i=0; i<sFrameSize; i++)
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

jfloat performAudacityFFT(JNIEnv * env, jobject thisObj, jshortArray array, jboolean bReset, SpeexPreprocessState** speexPrep, int iLastDet, jintArray iFFTValues){
	//LOGE("performAudacityFFT+\n");

	DECLARE_JNIENV_WITH_RETURN()
	jfloat fRet = 0.0;
	if(bReset){
		deinitAudacity();
		initAudacity();
	}

	if(NULL == inBuffer){
		initAudacity();
	}

	performSpeexPreprocess(env, thisObj, array, bReset, speexPrep);

	performWindowFunc(win);

	int iDx = 0;
	int iDx2 = 0;
	int iDx3 = 0;
	int iDx4 = 0;
	int iDx5 = 0;

	jshort *bytes = jni_env->GetShortArrayElements(array, NULL);
	if(NULL != bytes){
		int i=0;
		for (i = 0; i < sFrameSize; i++)
			inBuffer[i] = win[i] * bytes[i];

		PowerSpectrum(sFrameSize, inBuffer, outBuffer);

		jni_env->ReleaseShortArrayElements(array, bytes, 0);

		fRet = outBuffer[0];
		for (i = sLowPassIndex; i < sHalfFrameSize && i <= sHighPassIndex; i++){
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
			iDxValues = jni_env->GetIntArrayElements(iFFTValues, NULL);
			if(NULL != iDxValues){
				iDxValues[0] = iDx;
				iDxValues[1] = iDx2;
				iDxValues[2] = iDx3;
				iDxValues[3] = iDx4;
				iDxValues[4] = iDx5;
				jni_env->ReleaseIntArrayElements(iFFTValues, iDxValues, 0);
			}
		}

		if(0 < iLastDet){
			//LOGE("performAudacityFFT()-------------------------, iLastDetTone = [%.2f]=>%d, iDx0~5 = [%d, %d, %d, %d, %d]\n", iLastDet*sBinSize, iLastDet, iDx, iDx2, iDx3, iDx4, iDx5);
			if(iDx > 0 && iDx2 >0 && iLastDet > 0 && (iDx - iLastDet <=1 && iDx - iLastDet >= -1) && (iDx - iDx2 >=2 || iDx - iDx2 <= -2)){
				LOGE("performAudacityFFT()^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^, iLastDetTone=%.2f, change from %.2f to %.2f \n", iLastDet*sBinSize, iDx*sBinSize,  iDx2*sBinSize);
				iDx = iDx2;
			}
		}
	}else{
		LOGE("performAudacityFFT(), bytes is null\n");
	}

	return sBinSize*iDx;
}

JNIEXPORT jfloat JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_analyzeAudioViaAudacity(JNIEnv * env, jobject thisObj, jshortArray array, int iBufSize, jboolean bReset ,int iLastDetect, jintArray iFFTValues){
	return performAudacityFFT(env, thisObj, array, bReset, &stPreprocess, iLastDetect, iFFTValues);
}

JNIEXPORT jfloat JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_analyzeAudioViaAudacityAC(JNIEnv * env, jobject thisObj, jshortArray array, int iBufSize, jboolean bReset,int iLastDetect, jintArray iFFTValues){
	return performAudacityFFT(env, thisObj, array, bReset, &stPreprocessAC, iLastDetect, iFFTValues);
}

JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_endRecord(JNIEnv * env, jobject thisObj)
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

JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_encodeRS(JNIEnv * env, jclass jcls, jintArray array, int iCount, int numECCodewords)
{
	LOGE("encodeRS()\n");
	DECLARE_JNIENV_WITHOUT_RETURN()
	jint* data = jni_env->GetIntArrayElements(array, NULL);

	ArrayRef<int> dataWords(data, iCount);
	rsEncoder.encode(dataWords, numECCodewords);

	std::vector<int> values =  dataWords->values();
	std::copy(values.begin(), values.begin()+values.size(), data);

	//LOGE("encodeRS(), dataWords[24]:%d, data[24]:%d",dataWords[24], data[24]);
	jni_env->ReleaseIntArrayElements(array, data, 0);
}

JNIEXPORT void JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_decodeRS(JNIEnv * env, jclass jcls, jintArray array, int iCount, int numECCodewords)
{
	LOGE("decodeRS()\n");
	DECLARE_JNIENV_WITHOUT_RETURN()
	jint* data = jni_env->GetIntArrayElements(array, NULL);

	ArrayRef<int> dataWords(data, iCount);
	rsDecoder.decode(dataWords, numECCodewords);

	std::vector<int> values =  dataWords->values();
	std::copy(values.begin(), values.begin()+values.size(), data);

	jni_env->ReleaseIntArrayElements(array, data, 0);
}

#include "http_cgi.h"
using namespace std;

static pthread_t mReceiveAudioThread(0);

//const int G711_SAMPLES_PER_FRAME = 160;
const int TABLE_SIZE = 8;
const int BIAS = 0x84;		/* Bias for linear code. */
const int CLIP = 8159;
const int SIGN_BIT = 0x80;	/* Sign bit for a A-law byte. */
const int QUANT_MASK = 0xf;  /* Quantization field mask. */
const int NSEGS = 8;         /* Number of A-law segments. */
const int SEG_SHIFT = 4;     /* Left shift for segment number. */
const int SEG_MASK = 0x70;   /* Segment field mask. */

short ulaw2linear(unsigned char u_val)
{
   short t;

   /* Complement to obtain normal u-law value. */
   u_val = ~u_val;

   /*
    * Extract and bias the quantization bits. Then
    * shift up by the segment number and subtract out the bias.
    */
   t = ((u_val & QUANT_MASK) << 3) + BIAS;
   t <<= ((unsigned)u_val & SEG_MASK) >> SEG_SHIFT;

   return ((u_val & SIGN_BIT) ? (BIAS - t) : (t - BIAS));
}

void writeBuf(unsigned char* charBuf, int iLen){
	//LOGW("writeBuf:%d", iLen);
	short* shortBuf = (short*)audioBufferPinned;
//	for(int i = 0 ; i < iLen; i++){
//		shortBuf[i] = charBuf[i];//ulaw2linear(charBuf[i]);
//	}
	int iPart = 4;
	unsigned short tmp = 0;
	for(int i = 0 ; i < iLen/iPart; i++){
//		unsigned short tmp2 = 0;
//		tmp2 = (((unsigned short)charBuf[iPart*i+2])<<8 | (charBuf[iPart*i+3]));
//		shortBuf[i] = ((long) tmp2) - 32767;
//
//		if(i == 0){
//			tmp = tmp2;
//		}
		shortBuf[i] = (((short)charBuf[iPart*i+3])<<8 | (charBuf[iPart*i+2]));
	}

	//LOGE("writeBuf(), shortBuf[25]:%d, tmp:%d (%u, %u)\n",shortBuf[0], tmp, charBuf[3], charBuf[2]);

	Delegate_WriteAudioBuffer(iLen/iPart);
}

void* runReceiveBufViaCGI(void* userdata){
	int res;
	Delegate_OpenAudioDevice(16000, 1, 1, 1280);
	//char data[BUF_SIZE];
	char session[SESSION_SIZE];

	// clear our memory
	memset(session, 0, sizeof(session));
	//memset(data, 0, BUF_SIZE*sizeof(char));

	if(GetSession(session) != 0) {
		LOGE("Get session failed.");
		//return 0;
	}else{
		//res = GetCGI("getSystemName", data, session);
		res = GetAudioBufCGI("receive", session, writeBuf);
		LOGE("GetAudioBufCGI:res(%d)\n%s",res);
		Delegate_CloseAudioDevice();
	}
	Delegate_detachCurrentThread();
	mReceiveAudioThread = 0;
}

void receiveBufViaCGI(){
	int errno = 0;
	if(!mReceiveAudioThread){
		if (0 != (errno = pthread_create(&mReceiveAudioThread, NULL, runReceiveBufViaCGI, NULL))) {
			LOGE("receiveBufViaCGI(), error when create mReceiveAudioThread,%d\n", errno);
		}else{
			pthread_setname_np(mReceiveAudioThread, "mReceiveAudioThread");
		}
	}
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_receiveAudioBufFromCam(JNIEnv * env, jobject obj, jstring strPath)
{
	DECLARE_JNIENV_WITH_RETURN()
	jboolean iRet = false;
	if(0 == mReceiveAudioThread){
		receiveBufViaCGI();
	}
	return iRet;
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_receiveAudioBufThreadRunning(){
	return mReceiveAudioThread != 0;
}

JNIEXPORT jboolean JNICALL Java_com_app_beseye_pairing_SoundPairingActivity_stopReceiveAudioBufThread(){
	if(mReceiveAudioThread){
		stopReceiveAudioBuf();
		return true;
	}
	return false;
}

#endif

#ifdef __cplusplus
 }
#endif //__cplusplus
