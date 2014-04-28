#ifndef __SP_CONFIG_H__
#define __SP_CONFIG_H__

#include <map>
#include <vector>
#include <string>
#include <iostream>
#include <sstream>
#include <climits>

#include "utils.h"
#include <zxing/common/reedsolomon/GenericGF.h>

using zxing::Ref;
using zxing::Counted;
using zxing::GenericGF;

using namespace std;

class FreqRangeData : public Counted{
private:
	double mdValue;
	double mdLowerBound;
	double mdUpperBound;

	void init(double dValue, double dLowerBound, double dUpperBound);
	friend class FreqRange;

public:
	FreqRangeData(double dValue, double dDelta);
	FreqRangeData(double dValue, double dLowerBound, double dUpperBound);
	virtual ~FreqRangeData();
	string toString();
	bool withinFreqRange(double dFreq);
};

class FreqRange : public Counted{
public:
	static const double MIN_FREQ = 200.0f;
	static const double MAX_FREQ = 8000.0f;

	FreqRange(double dFreq, string strCode);
	FreqRange(double dFreq, double dDelta, string strCode);
	FreqRange(double dFreq, double dUpperBound, double dLowerBound, string strCode);
	virtual ~FreqRange();

	bool withinFreqRange(double dFreq);

	bool isOverlap(Ref<FreqRangeData> frdChk);

	vector<Ref<FreqRangeData> > getLstFreqRangeData();

private:
	vector<Ref<FreqRangeData> > mlstFreqRangeData;
	string mstrCode;
	friend string findToneCodeByFreq(double dFreq);
	friend class FreqAnalyzer;

	void init(double dFreq, double dDelta, string strCode);
};

class SoundPair_Config{
public:
	static std::map<string, double> sAlphabetTable;
	static std::vector<Ref<FreqRange> > sFreqRangeTable;
	static std::vector<string> sCodeTable;
	static Ref<GenericGF> gf;

	static const bool SELF_TEST = false;
	static const bool MARK_FEATURE = false;
	static const bool PRE_EMPTY = true;
	static const bool AMP_TUNE = true;
	static const bool NOISE_SUPPRESS = true;
	static const bool SEGMENT_FEATURE = true;
	static const bool SEGMENT_OFFSET_FEATURE = true;
	static const bool AUBIO_FFT = false;
	static const bool ENABLE_LV_DISPLAY = false;
	static const int SEG_SES_OFFSET = 5;

	static const string BT_MSG_ACK;
	static const string BT_MSG_PURE;
	static const float SILENCE_RATIO = 1.0f;

	//for speex noise suppress
	static const int NOISE_SUPPRESS_INDEX = 0;
	//for speex AGC
	static const float AGC_LEVEL = 0.0f;
	//for de-reverberation
	static const bool ENABLE_DEVERB = true;
	static const float DEVERB_DECAY = 0.3f;
	static const float DEVERB_LEVEL = 0.4f;

	//for phase I segmentation solution
	static const float SILENCE_CRITERIA = 0.002f;
	static const int SILENCE_DETECTION_SAMPLE = 256;

	static const int SAMPLE_RATE_PLAY = 44100;
	static const int SAMPLE_RATE_REC  = 16000;
	static const int FRAME_SIZE_REC   = 512;
	static const float BIN_SIZE       ;//= 16000.0/512.0;
	static const int TONE_SIZE_REC    = 1536;
	static const int TONE_FRAME_COUNT = TONE_SIZE_REC/FRAME_SIZE_REC;
	static const long FRAME_TS        = (FRAME_SIZE_REC*1000)/SAMPLE_RATE_REC;
	static const float TONE_DURATION  = TONE_SIZE_REC/16000.0f;//4096.0f/44100.0f;
	static const long TONE_PERIOD     ;//:= (long)(TONE_DURATION*1000);//SELF_TEST?(long)((4096.0f/44100.0f)*1000):100L;
	static const float RANDUDANT_RATIO = 1.0f;
	static const int FFT_ANALYSIS_COUNT= 5;
	static const int TONE_TYPE = 19;

	static const string BT_BINDING_MAC;
	static const string BT_BINDING_MAC_SENDER;
	static const double dStartValue = 2500.0;
	static const double dEndValue = 4000.0;//10500.0;
	static const int iDigitalToTest = 3;
	static const int MAX_ENCODE_DATA_LEN = 4;//16;//127;
	static const double EC_RATIO = 0.25f;

	static string PREFIX_DECODE;
	static string POSTFIX_DECODE;
	static string POSTFIX_DECODE_C1;
	static string POSTFIX_DECODE_C2;
	static string PEER_SIGNAL;
	static string CONSECUTIVE_MARK;
	static string DIVIDER;
	static string BT_MSG_DIVIDER;
	static string BT_MSG_FORMAT;
	static string BT_MSG_FORMAT_SENDER;
	static string BT_MSG_SET_VOLUME;
	static string BT_MSG_SET_VOLUME_END;
	static string MSG_AUTO_TEST_BEGIN;
	static string MSG_AUTO_TEST_END;
	static string MSG_TEST_ROUND_RESULT;
	static string MISSING_CHAR;
	static char PAIRING_DIVIDER;

	//For recording buffer
	static const long MAX_RECORDING_TIME = 60L; //60 seconds
	static float AMP_BASE_RATIO[];

	static void init();
	static void uninit();
	static void normalizeRatio();
	static void resolveFreqRangeConflict();
	static string getNDigits(string strCode, int iNumDigits);
	static string decodeConsecutiveDigits(string strCode);
	static int getDivisionByFFTYPE();
	static int getMultiplyByFFTYPE();
	static int getPowerByFFTYPE();

	//utils
	static int findIdxFromCodeTable(string strCode);
	static Ref<FreqRange> findFreqRange(string strCode);
};

#ifndef GEN_TONE_ONLY

class FreqData : public Counted{
public:
	FreqData(int iType, msec_t mTime, double mdFreq) {
		this->iType = iType;
		this->mTime = mTime;
		this->mdFreq = mdFreq;
	}
private:
	int iType;
	msec_t mTime;
	double mdFreq;
};

class FreqRecord : public Counted{
private:
	int miBufIndex;
	msec_t mlTs;
	double mdFreq;
	string mstrCode;
	int miFFTValues[SoundPair_Config::FFT_ANALYSIS_COUNT];
	friend class CodeRecord;
	friend class FreqAnalyzer;

	void init(msec_t lTs, double dFreq, string strCode, int iBufIndex, int iFFTValues[]);

public:
	FreqRecord(msec_t lTs, double dFreq, string strCode, int iBufIndex, int iFFTValues[]);
	FreqRecord(Ref<FreqRecord> copyObj);
	virtual ~FreqRecord();

	Ref<FreqRecord> recheckToneCode(int iLastTone);
	string toString();
};

class CodeRecord : public Counted{
private:
	msec_t lStartTs;
	msec_t lEndTs;
	string strCdoe;
	string strReplaced;
	std::vector<Ref<FreqRecord> > mlstFreqRec;
	friend class FreqAnalyzer;

public:
	CodeRecord();
	CodeRecord(std::vector<Ref<FreqRecord> > lstFreqRec, string strReplaced);
	CodeRecord(msec_t lStartTs, msec_t lEndTs, string strCdoe);
	CodeRecord(msec_t lStartTs, msec_t lEndTs, string strCode, std::vector<Ref<FreqRecord> > lstFreqRec);
	virtual ~CodeRecord();

	static Ref<CodeRecord> combineNewCodeRecord(Ref<CodeRecord> cr1, Ref<CodeRecord> cr2, int iOffset, int iLastTone);
private:
	void setParameters(msec_t lStartTs, msec_t lEndTs, string strCdoe);
	void inferFreq();
	string toString();
	bool isSameCode();
};
#endif

#endif
