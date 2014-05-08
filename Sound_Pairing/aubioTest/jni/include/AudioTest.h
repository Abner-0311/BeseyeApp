#ifndef __AUDIOTEST_H__
#define __AUDIOTEST_H__

#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>
#include "sp_config.h"
#include "beseye_sound_pairing.h"
#include "AudioBufferMgr.h"
#include "FreqAnalyzer.h"
#include "FreqGenerator.h"

#ifndef CAM_AUDIO
#include <zxing/common/reedsolomon/ReedSolomonEncoder.h>
using zxing::ReedSolomonEncoder;
#endif

using zxing::Ref;
using zxing::ArrayRef;
using zxing::GenericGF;
using zxing::ReedSolomonDecoder;

class AudioTest : public IOnPlayToneCallback, public IFreqAnalyzeResultCB{
public :
	virtual ~AudioTest();
	static AudioTest* getInstance();
	static bool destroyInstance();

	virtual bool setSenderMode();
	virtual bool setReceiverMode(bool bAutoTest = false);
	virtual bool setAutoTestMode();

	virtual bool startAutoTest(string strInitCode, int iDigitalToTest);
	virtual bool startPairingAnalysis();
	virtual bool stopAutoTest();

	virtual bool playTone(string strCode, bool bNeedEncode);
	virtual bool startGenerateTone(string strInitCode, int iDigitalToTest);
	virtual bool stopGenerateTone();

	virtual bool startAnalyzeTone();
	virtual bool stopAnalyzeTone();

	virtual void onStartGen(string strCode);
	virtual void onStopGen(string strCode);
	virtual void onCurFreqChanged(double dFreq);
	virtual void onErrCorrectionCode(string strCode, string strEC, string strEncodeMark);

	virtual void onDetectStart();
	virtual void onAppendResult(string strCode);
	virtual void onSetResult(string strCode, string strDecodeMark, string strDecodeUnmark, bool bFromAutoCorrection, MatchRetSet* prevMatchRet);
	virtual void onTimeout(void* freqAnalyzer, bool bFromAutoCorrection, MatchRetSet* prevMatchRet);
	virtual float onBufCheck(ArrayRef<short> buf, msec_t lBufTs, bool bResetFFT, int* iFFTValues);
	virtual void decodeRSCode(int* data, int iCount, int iNumErr);
#ifdef ANDROID
	virtual void soundpairSenderCallback(const char* cb_type, void* data);
#endif
	virtual void soundpairReceiverCallback(const char* cb_type, void* data);

	bool isAutoTestBeginAnalyzeOnReceiver(){return mbAutoTestBeginAnalyzeOnReceiver;}
	bool isPairingAnalysisMode(){return mbPairingAnalysisMode;}
	void setPairingReturnCode(int code){miPairingReturnCode = code;}
	virtual int getPairingReturnCode(){return miPairingReturnCode;}

#ifdef ANDROID
	virtual void setCamCamWSServerInfo(string strHost, int iPort);
	virtual int connectCamCamWSServer();
	virtual int disconnectCamCamWSServer();
	virtual bool isCamCamWSServerConnected();
#endif
private:
	static AudioTest* sAudioTest;
	AudioTest();

	bool mbStopControlThreadFlag;
	bool mbStopBufRecordFlag;
	bool mbStopAnalysisThreadFlag;
	pthread_t mControlThread;
	pthread_t mBufRecordThread;
	pthread_t mAnalysisThread;

	pthread_mutex_t mSyncObj;
	pthread_cond_t mSyncObjCond;

	string mstrCurTransferCode;
	string mstrCurTransferTs;
	bool mbSenderAcked;

	//Control pairing code transfer
	pthread_mutex_t mSendPairingCodeObj;
	pthread_cond_t mSendPairingCodeObjCond;

	//Control auto test round
	pthread_mutex_t mAutoTestCtrlObj;
	pthread_cond_t mAutoTestCtrlObjCond;
	bool mbAutoTestBeginOnReceiver;
	bool mbAutoTestBeginAnalyzeOnReceiver;

	void sendPlayPairingCode(string strCode);

	//ws client info
	string mstrCamWSServerIP;
	int miCamWSServerPort;

	ArrayRef<short> bufSegment;

	static void* runAutoTestControl(void* userdata);
	static void* runAudioBufRecord(void* userdata);
	static void* runAudioBufAnalysis(void* userdata);

	static Ref<BufRecord> getBuf();
	static Ref<BufRecord> getBuf(int iNumToRest);

	bool mbNeedToResetFFT;
	stringstream tmpRet;
	string strInitCode;
	string curCode;
	string curECCode;
	string curEncodeMark;
	int miDigitalToTest;

	bool mbPairingAnalysisMode;
	int miPairingReturnCode;

	static string findDifference(string strSrc, string strDecode);
	void adaptPrevMatchRet(MatchRetSet* prevMatchRet);
	void resetBuffer();
	void deinitTestRound();

	bool mIsSenderMode;
	bool mIsReceiverMode;

	bool isSenderMode();
	bool isReceiverMode();
	bool isAutoTestMode();
};

#endif
