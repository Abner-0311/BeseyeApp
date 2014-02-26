#ifndef __FREQGENERATOR_H__
#define __FREQGENERATOR_H__

#include <vector>
#include "sp_config.h"
#include "beseye_sound_pairing.h"
#include <stdlib.h>
#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/reedsolomon/ReedSolomonEncoder.h>

using zxing::Ref;
using zxing::Counted;
using zxing::ArrayRef;
using zxing::GenericGF;
using zxing::ReedSolomonEncoder;
using namespace std;

class IOnPlayToneCallback{
public:
	virtual void onStartGen(string strCode) = 0;
	virtual void onStopGen(string strCode) = 0;
	virtual void onCurFreqChanged(double dFreq) = 0;
	virtual void onErrCorrectionCode(string strCode, string strEC, string strEncodeMark) = 0;
};

class EncodeItm : public Counted{
public:
	EncodeItm(string strCodeInputAscii, string strCodeInput, string strECCode, string strEncodeMark, string strEncode) ;
	string strCodeInputAscii;
	string strCodeInput;
	string strECCode;
	string strEncodeMark;
	string strEncode;

	string toString();
};

class FreqGenerator : public Counted{
public:
    enum Play_Tone_Status{
		PLAY_TONE_BEGIN,
		PLAY_TONE_END,
		PLAY_TONE_ERROR,
		PLAY_TONE_TYPE_COUNT
    };
private:
	static double TWO_PI;
	static double HALF_PI;
	static double TARGET_ANGLE;
	static msec_t sTs;
	static string DEFAULT_BYTE_MODE_ENCODING;
	static Ref<FreqGenerator> sFreqGenerator;

	FreqGenerator();
	void writeTone(double* sample, byte* generatedSnd, int iLen);
	void writeToneToBuf(double* sample, int iLen);
	static int getNumOfEcBytes(int numDataBytes);
	static void genTestData(vector<string> lstRet, string strPre, int iRestDigital);
	static int getRandomNumDigit(int iMin, int iMax);
	static string getNextLegalCode(string strCode);

	IOnPlayToneCallback* mOnPlayToneCallback;
	pthread_t mThreadPlayTone;
	pthread_mutex_t mSyncObj;
	pthread_cond_t mSyncObjCond;
	pthread_cond_t mSyncObjCondSelfTest;
	bool mbStopPlayCodeThread;
	vector<Ref<EncodeItm> > mlstEncodeList;

	//audio interface
	int getAudioBufSize();
	bool initAudioDev();
	bool deinitAudioDev();
    void(* mPlayToneCB)(void*, Play_Tone_Status, const char *, int) ;
    void* mCbUserData;
public:
	static Ref<FreqGenerator> getInstance();
	static bool destroyInstance();

	static string getECCode(string strCode);
	static string encode(string content);
	static string encode2(string content);
	static vector<string> getTestList(int iNumDigital);
	static string genNextTestData(string strCurCode, int iNumDigits);
	static string genNextRandomData(int iMinDigit);

	static Ref<ReedSolomonEncoder> rsEncoder;

	virtual ~FreqGenerator();

	void setOnPlayToneCallback(IOnPlayToneCallback* cb);
    void setOnPlayToneCallback(void(* playToneCB)(void*, Play_Tone_Status, const char *, int) , void* userData);
	//void playCode(const string strCodeInput, const bool bNeedEncode);
	bool playCode2(const string strCodeInput, const bool bNeedEncode);
	static void* runPlayCode2(void* userdata);
	void invokePlayCode2();

	void playCode3(const string strCodeInput, const bool bNeedEncode);
	static void* runPlayCode3(void* userdata);
	void invokePlayCode3();

	//void stopPlay();
	void stopPlay2();

	void notifySelfTestCond();
};

#endif
