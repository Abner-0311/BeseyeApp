#ifndef __FREQANALYZER_H__
#define __FREQANALYZER_H__

#include "sp_config.h"
#include <speex_preprocess.h>
#include "FFT.h"

#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>

using zxing::Ref;
using zxing::Counted;
using zxing::ArrayRef;
using zxing::GenericGF;
using zxing::ReedSolomonDecoder;
using namespace std;

enum MATCH_RESULTS{
	DESC_MATCH,
	DESC_MATCH_EC,
	DESC_MATCH_MSG,
	DESC_MISMATCH,
	DESC_TIMEOUT,
	DESC_TIMEOUT_MSG,
	DESC_TIMEOUT_MSG_EC,
	DESC_RESULT_TYPE
};

string MATCH_RESULTStoString(MATCH_RESULTS type);

class MatchRetSet{
public:
	MatchRetSet(MATCH_RESULTS prevMatchRet, string strDecodeMark, string strDecodeUnmark, string strCode);

	MATCH_RESULTS prevMatchRetType;
	string strDecodeMark;
	string strDecodeUnmark;
	string strCode;

	string toString();
};

class IFreqAnalyzeResultCB{
public:
	virtual void onDetectStart() = 0;
	virtual void onDetectPostFix() = 0;
	virtual void onAppendResult(string strCode)= 0;
	virtual void onSetResult(string strCode, string strDecodeMark, string strDecodeUnmark, bool bFromAutoCorrection, MatchRetSet* prevMatchRet)= 0;
	virtual void onTimeout(void* freqAnalyzer, bool bFromAutoCorrection, MatchRetSet* prevMatchRet)= 0;
	virtual float onBufCheck(ArrayRef<short> buf, msec_t lBufTs, bool bResetFFT, int* iFFTValues)= 0;
	virtual void decodeRSCode(int* data, int iCount, int iNumErr)= 0;
};

class FreqAnalyzer : public Counted{
private:
	std::vector<Ref<FreqRecord> > mFreqRecordList;
	std::vector<Ref<CodeRecord> > mCodeRecordList;

	string mLastAbandant;
	stringstream msbDecode;

	bool mbStartAppend;
//	bool sbNeedToRecFirstTs = false;
	bool mbInSenderMode;

	ArrayRef<short> bufSegment;
	int mSessionOffset;
	msec_t mSessionBeginTs;
	int mSessionBeginBufIdx;

	//the last tone recognized
	msec_t mLastCheckToneTs;

	string mstrCodeTrace ;
	msec_t mlTraceTs;
	msec_t mlMaxWaitingTime;

	bool mbNeedToAutoCorrection;
	FreqAnalyzer* selfFreqAnalyzer;
	MatchRetSet* mprevMatchRet;

	bool mbLowSoundDetected;

	FreqAnalyzer(bool bNeedToAutoCorrection);

	static Ref<FreqAnalyzer> sFreqAnalyzer;
	IFreqAnalyzeResultCB* mIFreqAnalyzeResultCBListener;

	static int getLastDetectedToneIdxOnCodeList(std::vector<Ref<CodeRecord> > lstCodeRecord, msec_t lCurTs);

	static int getToneIdxByCode(string strCode);

	static string findToneCodeByFreq(double dFreq);

	void checkTimeout(msec_t lTs);

	int getInvalidFreqCount();
	void fillEmptyCodeRecord(msec_t lCurSesBeginTs);
	void pickWithSeesion();
	void pickWithoutSession(int iCheckIndex);
	void checkEmptySlot();
	void appendRet(Ref<CodeRecord> rec);
	void appendRet(string strCode);
	string optimizeDecodeString(int iIndex);
	int checkPostfix();
	void checkFirstCharOfPrefix(Ref<CodeRecord> rec);
	void amplitudeTest(int iBufIndex);
	int segmentCheckOnFirst(bool bForcePerform);
	int segmentCheck(bool bForcePerform);
	string replaceInvalidChar(string strDecode);
	void checkResult(string strDecode);
	int checkFrameBySessionAndAutoCorrection();
	void regenDecode();
	static int getNumOfBias(vector<Ref<CodeRecord> > lstCodeRecord);
	void autoCorrection(MatchRetSet* prevMatchRet);
	static string removeDividerAndUnmark(string strDecode);
	string correctErrByDelta(int iDelta);
	string analyzeCodeRecordList();
	vector<Ref<CodeRecord> > findMaxDelta();
	Ref<CodeRecord> findMinDelta();
	int exceedToneCheckPeriod();
	static int getMsgLength(int iDataLength);
	int getMeaningfulMsgLength(int iDataLength, bool bAbove);
	static string decodeRSEC(string content);

//audio pre-process and FFT
	SpeexPreprocessState *stPreprocess, *stPreprocessAC;
	void runAudioPreprocess(short * array, bool bRese);
	void runAudioPreprocessAC(short * array, bool bRese);

	void normalAnalysis(int iIndex);

	static int sSampleRate;
	static int sFrameSize;
	static int sHalfFrameSize;
	static float sBinSize;
	static int sNSIndex;
	static float sAGCLevel;
	static float sFFTTol;
	static bool	sEnableDeverb;
	static float sDeverbDecay;
	static float sDeverbLevel;
	static int LOW_PASS_CRITERIA_FREQ;
	static int sLowPassIndex;

	static int HIGH_PASS_CRITERIA_FREQ;
	static int sHighPassIndex;

	static void setSpeexPreprocess(SpeexPreprocessState* sps);
	static void runSpeexPreprocess(SpeexPreprocessState* sps, short *bytes);
	static void performSpeexPreprocess(short * array, bool bReset, SpeexPreprocessState** speexPrep);
//Audacity FFT analysis
	static float *inBuffer;
	static float *outBuffer;
	static float *win;
	static double wss;
	static int windowFunc;

	static void initAudacity();
	static void deinitAudacity();
	static void performWindowFunc(float *winBuf);

	static float performAudacityFFT(ArrayRef<short> bytes, bool bReset, SpeexPreprocessState** speexPrep, int iLastDet, int* iDxValues);

public:
	virtual ~FreqAnalyzer();

	static Ref<FreqAnalyzer> getInstance();
	static bool destroyInstance();
	static void initAnalysisParams(int iSampleRate, int iFrameSize, int iNSIndex, float iAGCLevel, bool bDeverb, float fDeverbDecay, float dDeverbLevel);

	static Ref<ReedSolomonDecoder> rsDecoder;
	static vector<Ref<CodeRecord> > getLstCodeRecordByOffset(vector<Ref<CodeRecord> > lstCodeRecord, int iOffset);

	void setIFreqAnalyzeResultCB(IFreqAnalyzeResultCB* listener);
	void reset();
	void setSenderMode(bool bIsSenderMode);
	void beginToTrace(string strCode);
	void endToTrace();
    int getLastDetectedToneIdx(msec_t lCurTs);

    /*synchronized*/ void analyze(msec_t lTs, double dFreq, int iBufIndex, int iFFTValues[]);

    void setSessionOffset(int iOffset);
    int getSessionOffset();
    bool checkEndPoint();


    bool isDetectLowSound(){return mbLowSoundDetected;}
    void setDetectLowSound(bool flag){mbLowSoundDetected = flag;}
    void triggerTimeout();

    bool canPerformAutoCorrection();
    bool performAutoCorrection(MatchRetSet* prevMatchRet);

	float analyzeAudioViaAudacity(ArrayRef<short> array, int iBufSize, bool bReset ,int iLastDetect, int* iFFTValues);
	float analyzeAudioViaAudacityAC(ArrayRef<short> array, int iBufSize, bool bReset ,int iLastDetect, int* iFFTValues);

};

#endif
