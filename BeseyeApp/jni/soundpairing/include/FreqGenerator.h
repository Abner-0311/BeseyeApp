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

typedef uint64_t uint64;

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
		PLAY_TONE_AUDIO_DEV_INIT,
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
    void(* mPlayToneCB)(void*, Play_Tone_Status, const char *, unsigned int) ;
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

	//void playCode(const string strCodeInput, const bool bNeedEncode);
	//void stopPlay();
	bool playCode2(const string strCodeInput, const bool bNeedEncode);
	bool playCode2(const string strMacAddr, const string strWifiKey, const string strToken, const bool bNeedEncode);
	static void* runPlayCode2(void* userdata);
	void invokePlayCode2();
	void stopPlay2();

	//For self testing
	void playCode3(const string strCodeInput, const bool bNeedEncode);
	static void* runPlayCode3(void* userdata);
	void invokePlayCode3();
	void notifySelfTestCond();
	void setOnPlayToneCallback(IOnPlayToneCallback* cb);

	//For client
	void setOnPlayToneCallback(void(* playToneCB)(void* userdata, Play_Tone_Status status, const char *msg, unsigned int type) , void* userData);

	/*Play pairing tone*/
	//macAddr => Hex values without ':' in lower case (ex. ef01cd45ab89)
	//wifiKey => ASCII values (for WEP: 5 or 13 digits; WPA/WPA2: more than 8 digits)
	//secType => 0:none; 1:WEP; 2:WPA; 3:WPA2
	//tmpUserToken => temp user token from Account BE

	unsigned long getSoundPairingDuration(const char* ssid, const char* wifiKey, bool bSSIDHash);//in milliseconds

	//WiFi AP BSSID (deprecated)
	//unsigned int playPairingCode(const char* macAddr, const char* wifiKey, unsigned int secType, unsigned short tmpUserToken);
	unsigned int playPairingCode(const char* macAddr, const char* wifiKey, unsigned short tmpUserToken);
	unsigned int playPairingCodeWithPurpose(const char* macAddr, const char* wifiKey, unsigned short tmpUserToken, unsigned char cPurpose);//iPurpose: 0=> pairing, 1:change wifi, 2:restore token

	//WiFi AP SSID
	unsigned int playSSIDPairingCode(const char* ssid, const char* wifiKey, PAIRING_SEC_TYPE secType, unsigned short tmpUserToken);
	unsigned int playSSIDPairingCodeWithPurpose(const char* ssid, const char* wifiKey, PAIRING_SEC_TYPE secType, unsigned short tmpUserToken, unsigned char cPurpose);//iPurpose: 0=> pairing, 1:change wifi, 2:restore token

	//WiFi AP SSID Hash
	uint64 getSSIDHashValue(const char* ssid);
	char* getSSIDStringHashValue(const char* ssid);//Need to release
	unsigned int playSSIDHashPairingCode(const char* ssid, const char* wifiKey, PAIRING_SEC_TYPE secType, unsigned short tmpUserToken);
	unsigned int playSSIDHashPairingCodeWithPurpose(const char* ssid, const char* wifiKey, PAIRING_SEC_TYPE secType, unsigned short tmpUserToken, unsigned char cPurpose);//iPurpose: 0=> pairing, 1:change wifi, 2:restore token
};

#endif
