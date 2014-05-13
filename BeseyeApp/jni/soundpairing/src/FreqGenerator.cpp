#include "FreqGenerator.h"
#include "soundpairing_error.h"
#include <sys/time.h>
#include <zxing/common/Array.h>

#ifndef GEN_TONE_ONLY
#include "AudioBufferMgr.h"
#endif

using zxing::Array;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::RS_ENCODE_ERROR;

Ref<FreqGenerator> FreqGenerator::sFreqGenerator;
double FreqGenerator::TWO_PI = 2 * M_PI;
double FreqGenerator::HALF_PI = 0.5 * M_PI;
double FreqGenerator::TARGET_ANGLE = HALF_PI/128;
msec_t FreqGenerator::sTs = 0;;

string FreqGenerator::DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1";
Ref<ReedSolomonEncoder> FreqGenerator::rsEncoder = Ref<ReedSolomonEncoder>(new ReedSolomonEncoder(GenericGF::QR_CODE_FIELD_256));

FreqGenerator::FreqGenerator():mbStopPlayCodeThread(false),mThreadPlayTone(0), mOnPlayToneCallback(NULL), mPlayToneCB(NULL), mCbUserData(NULL){
	/* initialize random seed: */
	srand (time(NULL));

	pthread_mutex_init(&mSyncObj, NULL);
	pthread_cond_init(&mSyncObjCond, NULL);
	pthread_cond_init(&mSyncObjCondSelfTest, NULL);
}

FreqGenerator::~FreqGenerator(){
	pthread_cond_destroy(&mSyncObjCondSelfTest);
	pthread_cond_destroy(&mSyncObjCond);
	pthread_mutex_destroy(&mSyncObj);
}

Ref<FreqGenerator> FreqGenerator::getInstance(){
	if(sFreqGenerator.empty())
		sFreqGenerator = Ref<FreqGenerator>(new FreqGenerator());

	return sFreqGenerator;
}

bool FreqGenerator::destroyInstance(){
	if(!sFreqGenerator.empty()){
		sFreqGenerator.reset(NULL);
		return true;
	}
	return false;
}

void FreqGenerator::setOnPlayToneCallback(IOnPlayToneCallback* cb){
	mOnPlayToneCallback = cb;
}

void FreqGenerator::setOnPlayToneCallback(void(* playToneCB)(void*, Play_Tone_Status, const char *, unsigned int), void* userData){
    mPlayToneCB =playToneCB;
    mCbUserData = userData;
}

//void FreqGenerator::playCode(const string strCodeInput, const bool bNeedEncode){
//
//}

bool FreqGenerator::playCode2(const string strCodeInputASCII, const bool bNeedEncode){
	LOGE("playCode2(), strCodeInputASCII:%s, %d\n", strCodeInputASCII.c_str(), bNeedEncode);
	bool bRet = false;
#ifdef GEN_TONE_ONLY
    LOGE("playCode2(), strCodeInputASCII:%s, %d\n", strCodeInputASCII.c_str(), bNeedEncode);

	stringstream sstrCodeInput;
	int iPower = SoundPair_Config::getPowerByFFTYPE();
	int iLen = strCodeInputASCII.length();

	for(int i = 0; i< iLen;i++){
		char ch = strCodeInputASCII.at(i);
		sstrCodeInput << SoundPair_Config::sCodeTable.at(ch >> iPower);
		sstrCodeInput << SoundPair_Config::sCodeTable.at(ch & 0x0f);

		//LOGE("playCode2(), i=%d, ch:%c, 0x%x, (%s, %s)\n",i, ch, ch, SoundPair_Config::sCodeTable.at(ch >> iPower).c_str(), SoundPair_Config::sCodeTable.at(ch & 0x0f).c_str());
	}
	string strCodeInput = sstrCodeInput.str();
#else
	string strCodeInput = strCodeInputASCII;
#endif
    LOGE("playCode2(), strCodeInput:%s\n", strCodeInput.c_str());
    
	string strEncode = bNeedEncode?encode(strCodeInput):strCodeInput;
	const string strECCode = strEncode.substr(strCodeInput.length());

	string strEncodeMark = strEncode;//SoundPair_Config::encodeConsecutiveDigits(strEncode);
	string strCode = bNeedEncode?
							(SoundPair_Config::PREFIX_DECODE+(SoundPair_Config::PRE_EMPTY?"X":"")+strEncodeMark+SoundPair_Config::POSTFIX_DECODE+(SoundPair_Config::sCodeTable.at(SoundPair_Config::sCodeTable.size()-4))):
							strEncode;

	pthread_mutex_lock(&mSyncObj);
	mlstEncodeList.push_back(Ref<EncodeItm>(new EncodeItm(strCodeInputASCII, strCodeInput, strECCode, strEncodeMark, strCode)));
	pthread_cond_signal(&mSyncObjCond);
	pthread_mutex_unlock(&mSyncObj);

	if(0 == mThreadPlayTone){
		int errno = 0;
        LOGE("playCode2(), pthread_create\n");

		if (0 != (errno = pthread_create(&mThreadPlayTone, NULL, FreqGenerator::runPlayCode2, this))) {
			LOGE("playCode2(),error when create pthread,%d\n", errno);
		}else{
			LOGE("playCode2(),pthread_create mThreadPlayTone success\n");
			bRet = true;
		}
	}else{
		LOGE("playCode2(), mThreadPlayTone has been created\n");
		bRet = true;
	}
	return bRet;
}

void* FreqGenerator::runPlayCode2(void* userdata){
	((FreqGenerator*)userdata)->invokePlayCode2();
	return 0;
}

void FreqGenerator::invokePlayCode2(){
	mbStopPlayCodeThread = false;
	string strLastCode;
	struct timespec outtime;
	float duration = SoundPair_Config::TONE_DURATION;
	int numSamples = (int) ((duration * SoundPair_Config::SAMPLE_RATE_PLAY)*SoundPair_Config::SILENCE_RATIO);
	int realNumSample = (int) ((duration * SoundPair_Config::SAMPLE_RATE_PLAY));

	int iDeltaRange = (int) (0.1*realNumSample);
	int iDoubleDeltaRange = iDeltaRange*4;
#ifdef ANDROID
	const int iMinBufSize =  getAudioBufSize();
    initAudioDev();
#else
	const int iMinBufSize = numSamples;
    Delegate_OpenAudioDevice(SoundPair_Config::SAMPLE_RATE_PLAY, 1, 1, iMinBufSize);
#endif
	LOGI("invokePlayCode2(), numSamples:%d, iMinBufSize:%d, siSampleRatePlay = %d\n", numSamples, iMinBufSize, SoundPair_Config::SAMPLE_RATE_PLAY);


	//byte generatedSnd[iMinBufSize];
#ifdef ANDROID
    const int iBufLen = iMinBufSize/2;
	double sample [iBufLen];
	byte* generatedSnd = (byte*)Delegate_GetAudioBuffer();
#else
    const int iBufLen = iMinBufSize;
	double sample [iBufLen];
	byte* generatedSnd = NULL;
#endif

	Ref<EncodeItm> itmCode;
	while(!mbStopPlayCodeThread){
		pthread_mutex_lock(&mSyncObj);
		if(0 == mlstEncodeList.size()){
			LOGE("invokePlayCode2(), mPlayCodeThread begin to wait\n" );
			pthread_cond_wait(&mSyncObjCond, &mSyncObj);
			LOGE("invokePlayCode2(), mPlayCodeThread exit to wait\n" );
			if(mbStopPlayCodeThread){
				pthread_mutex_unlock(&mSyncObj);
				LOGE("invokePlayCode2(), mbStopPlayCodeThread:%d\n",mbStopPlayCodeThread );
				break;
			}
		}
#ifndef GEN_TONE_ONLY
		//wait for 1 sec for continuous tests
		getTimeSpecByDelay(outtime, 1000);
		pthread_cond_timedwait(&mSyncObjCond, &mSyncObj,  &outtime);
#endif
//		if(NULL != itmCode){
//			delete itmCode;
//			itmCode = NULL;
//		}

		itmCode = mlstEncodeList.front();
		mlstEncodeList.erase(mlstEncodeList.begin());

		LOGE("invokePlayCode2(), itmCode:%s\n", itmCode->toString().c_str());
#ifndef GEN_TONE_ONLY
		//I suspect that there is redundant inputcode from sender, workaround temporarily
		if((0==strLastCode.compare(itmCode->strCodeInput))){
			LOGE("invokePlayCode2(), ++++++++++++++++++++++++++++++++++++++++++++++++++++++ same as strLastCode:%s\n", strLastCode.c_str());
			continue;
		}
#endif
		strLastCode = itmCode->strCodeInput;

		if(NULL != mOnPlayToneCallback)
			mOnPlayToneCallback->onStartGen(itmCode->strCodeInputAscii);
        
        if(NULL != mPlayToneCB)
            mPlayToneCB(mCbUserData, PLAY_TONE_BEGIN, itmCode->strCodeInputAscii.c_str(), 0);

		if(NULL != mOnPlayToneCallback)
			mOnPlayToneCallback->onErrCorrectionCode(itmCode->strCodeInput, itmCode->strECCode, itmCode->strEncodeMark);

		int iCodeLen = itmCode->strEncode.length();
		int iTotalSamples = numSamples*iCodeLen;
		int i, iCurCodeIndex = 0;
		double dFreq = 0.0;

		//LOGI("invokePlayCode2(), iDeltaRange = "+iDeltaRange);

		float fBaseAmpRatio = 1.0f;

		//LOGE("invokePlayCode2(), iTotalSamples= %d, numSamples=%d, iBufLen=%d\n", iTotalSamples, numSamples, iBufLen);
		for (i = 0; i < iTotalSamples; ++i) {
			if(mbStopPlayCodeThread){
				pthread_mutex_unlock(&mSyncObj);
				LOGE("invokePlayCode2(), mbStopPlayCodeThread:%d\n",mbStopPlayCodeThread );
				break;
			}
			if(0 < i && 0 == i % iBufLen){
				writeTone(sample, generatedSnd, iBufLen);
				memset(sample, 0, iBufLen*sizeof(double));
#ifdef ANDROID
				memset(generatedSnd, 0, iMinBufSize*sizeof(byte));
#endif
			}

			if(0 == i % numSamples){
				iCurCodeIndex = i/numSamples;
				//LOGI("invokePlayCode2(), strCode["+iCurCodeIndex+"] = "+strCode.charAt(iCurCodeIndex));
				string strMatch = itmCode->strEncode.substr(iCurCodeIndex, 1);
				std::map<string,double>::iterator it = SoundPair_Config::sAlphabetTable.find(strMatch);
				if(SoundPair_Config::sAlphabetTable.end() != it){
					dFreq = it->second;
				}else{
					dFreq = 0.0;
				}

				if(SoundPair_Config::AMP_TUNE){
					fBaseAmpRatio = 1.0f;
					int idxFound = SoundPair_Config::findIdxFromCodeTable(strMatch);
					if(0 <= idxFound && idxFound < SoundPair_Config::TONE_TYPE){
						fBaseAmpRatio = SoundPair_Config::AMP_BASE_RATIO[idxFound];
						//LOGD("invokePlayCode2(), fBaseAmpRatio= %f for %s", fBaseAmpRatio, strMatch.c_str());
					}
				}
			}

			int iCurPos = (i % numSamples);

			if(realNumSample < iCurPos){
				sample[i% iBufLen] = 0;
			}else{
				float fRatio = 1.0f;

				if((realNumSample - iDoubleDeltaRange) <= iCurPos){
					fRatio = (float) (0.10f + 0.90f * ( 1 - sin(HALF_PI * (((float) (iCurPos - (realNumSample - iDoubleDeltaRange) )) /(iDoubleDeltaRange)))));
				}
				sample[i% iBufLen] = fBaseAmpRatio * fRatio * sin(TWO_PI * (i) * dFreq / SoundPair_Config::SAMPLE_RATE_PLAY);
			}

		}

		if(0 < iTotalSamples%iBufLen){
			writeTone(sample, generatedSnd, iTotalSamples%iBufLen);
			memset(sample, 0, iBufLen*sizeof(double));
#ifdef ANDROID
			memset(generatedSnd, 0, iMinBufSize*sizeof(byte));
#endif
		}

//                        	if(NULL != mOnPlayToneCallback)
//                        		mOnPlayToneCallback->onCurFreqChanged(dFreq);

		if(NULL != mOnPlayToneCallback)
			mOnPlayToneCallback->onStopGen(itmCode->strCodeInputAscii);
        
        if(NULL != mPlayToneCB)
            mPlayToneCB(mCbUserData, PLAY_TONE_END, itmCode->strCodeInputAscii.c_str(), 0);
        
		pthread_mutex_unlock(&mSyncObj);
	}
	LOGE("invokePlayCode2()------, thread end");
	mThreadPlayTone = 0;
	deinitAudioDev();
	Delegate_detachCurrentThread();
}

void FreqGenerator::playCode3(const string strCodeInput, const bool bNeedEncode){
	string strEncode = bNeedEncode?encode(strCodeInput):strCodeInput;
	const string strECCode = strEncode.substr(strCodeInput.length());

	string strEncodeMark = strEncode;//SoundPair_Config::encodeConsecutiveDigits(strEncode);
	string strCode = bNeedEncode?("X"+SoundPair_Config::PREFIX_DECODE+(SoundPair_Config::PRE_EMPTY?"X":"")+strEncodeMark+SoundPair_Config::POSTFIX_DECODE+(SoundPair_Config::sCodeTable.at(SoundPair_Config::sCodeTable.size()-4)))+"XXXXXX":strEncode;
//	string strCode = bNeedEncode?
//							(SoundPair_Config::PREFIX_DECODE+(SoundPair_Config::PRE_EMPTY?"X":"")+strEncodeMark+SoundPair_Config::POSTFIX_DECODE+(SoundPair_Config::sCodeTable.at(SoundPair_Config::sCodeTable.size()-4))):
//							strEncode;

	pthread_mutex_lock(&mSyncObj);
	mlstEncodeList.push_back(Ref<EncodeItm>(new EncodeItm(NULL, strCodeInput, strECCode, strEncodeMark, strCode)));
	pthread_cond_signal(&mSyncObjCond);
	pthread_mutex_unlock(&mSyncObj);

	if(0 == mThreadPlayTone){
		int errno = 0;
		if (0 != (errno = pthread_create(&mThreadPlayTone, NULL, FreqGenerator::runPlayCode3, this))) {
			LOGE("error when create pthread,%d\n", errno);
		}
	}
}

void* FreqGenerator::runPlayCode3(void* userdata){
	((FreqGenerator*)userdata)->invokePlayCode3();
	return 0;
}

void FreqGenerator::invokePlayCode3(){
	mbStopPlayCodeThread = false;
	const int iMinBufSize =  getAudioBufSize();

	LOGI("invokePlayCode3(), iMinBufSize:%d, siSampleRatePlay = %d", iMinBufSize, SoundPair_Config::SAMPLE_RATE_PLAY);

	const int iBufLen = iMinBufSize/2;
	double sample [iBufLen];
	byte generatedSnd[iMinBufSize];

	string strLastCode;
	float duration = SoundPair_Config::TONE_DURATION;
	int numSamples = (int) ((duration * SoundPair_Config::SAMPLE_RATE_PLAY)*SoundPair_Config::SILENCE_RATIO);
	int realNumSample = (int) ((duration * SoundPair_Config::SAMPLE_RATE_PLAY));
	int iDeltaRange = (int) (0.1*realNumSample);
	int iDoubleDeltaRange = iDeltaRange*4;
	int iShiftIdx = 0;

	while(!mbStopPlayCodeThread){
		pthread_mutex_lock(&mSyncObj);
		if(0 == mlstEncodeList.size()){
			LOGE("invokePlayCode3(), mPlayCodeThread begin to wait" );
			pthread_cond_wait(&mSyncObjCond, &mSyncObj);
			LOGE("invokePlayCode3(), mPlayCodeThread exit to wait" );
			if(mbStopPlayCodeThread){
				pthread_mutex_unlock(&mSyncObj);
				LOGE("invokePlayCode3(), mbStopPlayCodeThread:%d",mbStopPlayCodeThread );
				break;
			}
		}

		Ref<EncodeItm> itmCode = mlstEncodeList.front();
		mlstEncodeList.erase(mlstEncodeList.begin());

#ifndef GEN_TONE_ONLY
		//I suspect that there is redundant inputcode from sender, workaround temporarily
		if((0==strLastCode.compare(itmCode->strCodeInput))){
			LOGE("invokePlayCode3(), ++++++++++++++++++++++++++++++++++++++++++++++++++++++ same as strLastCode:%s", strLastCode.c_str());
			continue;
		}
#endif

		strLastCode = itmCode->strCodeInput;

		if(NULL != mOnPlayToneCallback)
			mOnPlayToneCallback->onStartGen(itmCode->strCodeInput);

		if(NULL != mOnPlayToneCallback)
			mOnPlayToneCallback->onErrCorrectionCode(itmCode->strCodeInput, itmCode->strECCode, itmCode->strEncodeMark);

		int iCodeLen = itmCode->strEncode.length();
		int iTotalSamples = numSamples*iCodeLen;
		int i, iCurCodeIndex = 0;
		double dFreq = 0.0;
		int iDelta = (int)(((iShiftIdx%10)/10.0f)*SoundPair_Config::FRAME_SIZE_REC);
		LOGE("invokePlayCode3(), iDelta:%d",iDelta );
		string iIndex = itmCode->strEncode.substr(iCurCodeIndex, 1);
		std::map<string,double>::iterator it = SoundPair_Config::sAlphabetTable.find(iIndex);
		if(SoundPair_Config::sAlphabetTable.end() != it){
			dFreq = it->second;
		}else{
			dFreq = 0.0;
		}
		float fBaseAmpRatio = 1.0f;

		for (i = 0; i < iTotalSamples; ++i) {
			if(mbStopPlayCodeThread){
				pthread_mutex_unlock(&mSyncObj);
				LOGE("invokePlayCode3(), mbStopPlayCodeThread:%d",mbStopPlayCodeThread );
				break;
			}
			if(0 < i && 0 == i % iBufLen){
				writeToneToBuf(sample, iBufLen);
				memset(sample, 0, iBufLen*sizeof(double));
				//memset(generatedSnd, 0, iMinBufSize*sizeof(byte));
			}

			int iCurPos = ((i-iDelta) % numSamples);

			if(0 == iCurPos){
				iCurCodeIndex = std::min((int)(itmCode->strEncode.length()-1), ((i-iDelta)/numSamples));
				//iEmptyCount = (int) (((iCurCodeIndex%10)/10.0f)*SoundPair_Config::FRAME_SIZE_REC);

				iIndex = itmCode->strEncode.substr(iCurCodeIndex, 1);
				it = SoundPair_Config::sAlphabetTable.find(iIndex);
				if(SoundPair_Config::sAlphabetTable.end() != it){
					dFreq = it->second;
				}else{
					dFreq = 0.0;
				}

				//LOGI("run(), strCode[%d] = %s, freq:%f",iCurCodeIndex,ItmCode->strEncode.substr(iCurCodeIndex, iCurCodeIndex+1).c_str(),dFreq);
				//Log.i(TAG, "run(), freq:"+dFreq );

				if(SoundPair_Config::AMP_TUNE){
					fBaseAmpRatio = 1.0f;
					int idxFound = SoundPair_Config::findIdxFromCodeTable(iIndex);
					if(0 <= idxFound && idxFound < SoundPair_Config::TONE_TYPE){
						fBaseAmpRatio = SoundPair_Config::AMP_BASE_RATIO[idxFound];
					}
				}
			}

			if(i < iDelta){
				sample[i% iBufLen] = 0.000000000000000f;
			}else if(realNumSample < iCurPos){
				sample[i% iBufLen] = 0.000000000000000f;
			}else{

				float fRatio = 1.0f;
				if((realNumSample - iDoubleDeltaRange) <= iCurPos)
					fRatio = (float) (0.10f + 0.90f * ( 1 - sin(HALF_PI * (((float) (iCurPos - (realNumSample - iDoubleDeltaRange) )) /(iDoubleDeltaRange)))));

				sample[i% iBufLen] = fBaseAmpRatio * fRatio * sin(TWO_PI * (i-iDelta) * dFreq / SoundPair_Config::SAMPLE_RATE_REC);
			}
			//Log.i(TAG, "run(), sample["+i+"] = "+sample[i] );
		}

		if(0 < (iTotalSamples+iDelta)%iBufLen){
			writeToneToBuf(sample, iTotalSamples%iBufLen);
			memset(sample, 0, iBufLen*sizeof(double));
			//java.util.Arrays.fill(generatedSnd,(byte) 0);
		}

		if(NULL != mOnPlayToneCallback)
			mOnPlayToneCallback->onStopGen(itmCode->strEncode);

		pthread_mutex_unlock(&mSyncObj);
	}
	LOGE("invokePlayCode3()------, thread end");
	mThreadPlayTone = 0;
	deinitAudioDev();
	Delegate_detachCurrentThread();
}
//void FreqGenerator::stopPlay(){
//
//}

void FreqGenerator::stopPlay2(){
	LOGE("stopPlay2()+++");
	//deinitAudioDev();
	mbStopPlayCodeThread = true;
	pthread_mutex_lock(&mSyncObj);
	pthread_cond_broadcast(&mSyncObjCond);
	pthread_mutex_unlock(&mSyncObj);
	LOGE("stopPlay2()---");
}


string FreqGenerator::getECCode(string strCode){
	string strEncode = encode(strCode);
	return strEncode.substr(strCode.length());
}


void FreqGenerator::notifySelfTestCond(){
	pthread_mutex_lock(&mSyncObj);
	pthread_cond_signal(&mSyncObjCondSelfTest);
	pthread_mutex_unlock(&mSyncObj);
}

void FreqGenerator::writeToneToBuf(double* sample, int iLen){
#ifndef GEN_TONE_ONLY
	// convert to 16 bit pcm sound array
	// assumes the sample buffer is normalised.
	ArrayRef<short> shortsRec = NULL;
	while(NULL == (shortsRec = AudioBufferMgr::getInstance()->getAvailableBuf())){
		timespec sleepValue = {0};
		sleepValue.tv_nsec = 10000000;//10 ms
		nanosleep(&sleepValue, NULL);
	}
	int iLenRec = AudioBufferMgr::getInstance()->getBufferSize();
	int idx = 0, i =0;

	struct timespec outtime;
	long lDelayms = 20;
	long lDelay2ms = 100;

	for (i =0; i< iLen;i++) {
		if(NULL == shortsRec){
			while(NULL == (shortsRec = AudioBufferMgr::getInstance()->getAvailableBuf())){
				timespec sleepValue = {0};
				sleepValue.tv_nsec = 10000000;//10 ms
				nanosleep(&sleepValue, NULL);
			}
		}

		// scale to maximum amplitude
		shortsRec[i%iLenRec] = (short) ((sample[i] * 32767));

		if(0 == (i+1)%iLenRec){
			sTs+=SoundPair_Config::FRAME_TS;
			AudioBufferMgr::getInstance()->addToDataBuf(sTs, shortsRec, iLenRec);
			getTimeSpecByDelay(outtime, lDelayms);

			pthread_cond_timedwait(&mSyncObjCondSelfTest, &mSyncObj, &outtime);
			shortsRec = NULL;
		}
	}

	if(NULL != shortsRec){
		sTs+=SoundPair_Config::FRAME_TS;
		AudioBufferMgr::getInstance()->addToDataBuf(sTs, shortsRec, i%iLenRec);

		getTimeSpecByDelay(outtime, lDelay2ms);
		pthread_cond_timedwait(&mSyncObjCondSelfTest, &mSyncObj, &outtime);
	}
#endif
}

int FreqGenerator::getNumOfEcBytes(int numDataBytes){
	return 2*((int) ceil(SoundPair_Config::EC_RATIO*numDataBytes));//6;
}

string FreqGenerator::encode(string content) {
	LOGE("encode(), content= [%s]\n", content.c_str() );
	if(0 == content.length()){
		LOGI("encode(), content is empty" );
		return "";
	}

	int iMultiply = SoundPair_Config::getMultiplyByFFTYPE();
	int iPower = SoundPair_Config::getPowerByFFTYPE();

	int numDataBytes = content.length();
	int numEcBytesInBlock = getNumOfEcBytes(numDataBytes/iMultiply)*iMultiply;

	LOGI("encode(), numDataBytes:%d, numEcBytesInBlock:%d\n", numDataBytes, numEcBytesInBlock);
	//int* toEncode = NULL;
	int iCount = (numDataBytes + numEcBytesInBlock)/iMultiply;
//	if(1 == SoundPair_Config::getMultiplyByFFTYPE()){
//		toEncode = new int[numDataBytes + numEcBytesInBlock];
//		int iLen = content.length();
//		for(int i =0;i < iLen;i++){
//			toEncode[i] = sCodeTable.indexOf(content.substr(i, i+1));
//		}
//	}else{
		//toEncode = new int[iCount];
	LOGI("encode(), iCount:%d\n", iCount);
		ArrayRef<int> toEncode(new Array<int>(iCount));
		int iLen = numDataBytes/iMultiply;
		for(int i =0;i < iLen;i++){
			toEncode[i] = 0;
			for(int j = 0;j < iMultiply;j++){
				toEncode[i] <<= iPower;
				toEncode[i] += SoundPair_Config::findIdxFromCodeTable(content.substr(i*iMultiply+j, 1));
			}
			LOGD("encode(), toEncode[%d]= %d\n",i,toEncode[i] );
		}
	//}

	LOGI("encode(), begin encode\n" );
	RS_ENCODE_ERROR	err = FreqGenerator::rsEncoder->encode(toEncode, numEcBytesInBlock/iMultiply);

	stringstream ret;
	if(0 == err){
		for(int i =0;i < iCount;i++){
			LOGD("encode(), after encode, toEncode[%d]= %d\n",i,toEncode[i] );
		}

		for(int idx = 0; idx < iCount; idx++){
	//		if(1 == SoundPair_Config::getMultiplyByFFTYPE()){
	//			ret.append(sCodeTable.get(toEncode[idx]));
	//		}else{
				string tmp("");
				for(int j = iMultiply-1 ;j >=0 ;j--){
					tmp = SoundPair_Config::sCodeTable.at(((toEncode[idx]) & ((0x1<<iPower) -1))) + tmp;

					//ret.insert(idx*iMultiply, SoundPair_Config::sCodeTable.at(((toEncode[idx]) & ((0x1<<iPower) -1))));
					toEncode[idx] >>= iPower;
				}
				ret<<tmp;
	//		}
		}

		LOGE("encode(), ret= [%s]\n", ret.str().c_str());
	}

	return ret.str();
}

string FreqGenerator::genNextRandomData(int iMinDigit){
	//LOGI("genNextRandomData(), iMinDigi=%d", iMinDigit);
	stringstream strRet;
	int iDivision = SoundPair_Config::getDivisionByFFTYPE();

	int iMaxDigit = min(SoundPair_Config::MAX_ENCODE_DATA_LEN*SoundPair_Config::getMultiplyByFFTYPE(), (int) ((pow(2.0, (double)(SoundPair_Config::getPowerByFFTYPE()*SoundPair_Config::getMultiplyByFFTYPE()) -1 ))* 0.6666666666666f));

	//LOGI("genNextRandomData(), iDivision:%d, iMaxDigit=%d", iDivision, iMaxDigit);

	int iLen = getRandomNumDigit(iMinDigit, iMaxDigit)*SoundPair_Config::getMultiplyByFFTYPE();

	//LOGI("genNextRandomData(), iLen:%d, iMaxDigit=%d", iLen, iMaxDigit);
	//Log.e(TAG, "genNextRandomData(), iMaxDigit= "+iMaxDigit+", iLen="+iLen );

	for(int i =0;i<iLen;i++){
		strRet<<(SoundPair_Config::sCodeTable.at(rand() % (iDivision)));
	}

	return strRet.str();
}

int FreqGenerator::getRandomNumDigit(int iMin, int iMax){
	return iMin + rand() % (iMax - iMin);
}

string FreqGenerator::getNextLegalCode(string strCode){
	string strRet;
	int iIndex = SoundPair_Config::findIdxFromCodeTable(strCode);
	if((SoundPair_Config::getDivisionByFFTYPE() -1) > iIndex){
		strRet = SoundPair_Config::sCodeTable.at(iIndex+1);
	}
	//Log.e(TAG, "getNextLegalCode(), strCode= "+strCode+", strRet="+strRet );
	return strRet;
}

int FreqGenerator::getAudioBufSize(){
	return Delegate_GetAudioBufferSize(SoundPair_Config::SAMPLE_RATE_PLAY);
}

bool FreqGenerator::initAudioDev(){
	Delegate_OpenAudioDevice(SoundPair_Config::SAMPLE_RATE_PLAY, 1, 1, 0);
	return true;
}

bool FreqGenerator::deinitAudioDev(){

	Delegate_CloseAudioDevice();
	return true;
}

void FreqGenerator::writeTone(double sample[], byte generatedSnd[], int iLen){
	//if(NULL != mToneAudioTrack){
		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.
    //LOGE("writeTone(), iLen:%d\n",iLen);
#ifndef ANDROID
	    generatedSnd = (byte*)Delegate_GetAudioBuffer();
#endif
		int idx = 0;
		for (int i =0; i< iLen;i++) {
			// scale to maximum amplitude
			short val = (short) ((sample[i] * 32767));
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
		}
		//LOGE("writeTone(), generatedSnd[1200] = %d\n", generatedSnd[1200]);
#ifdef ANDROID
		Delegate_WriteAudioBuffer(iLen);
#else
        Delegate_WriteAudioBuffer(iLen*2);
#endif
		//mToneAudioTrack.write(generatedSnd, 0, iLen*2);
	//}
}

#define LEN_OF_MAC_ADDR 12

unsigned int FreqGenerator::playPairingCode(const char* macAddr, const char* wifiKey, unsigned short tmpUserToken){
	unsigned int iRet = R_OK;
	char codeToPlay[1024]={0};
	codeToPlay[1023] = '/0';
	static const char sep = 0x1B;

	if(!macAddr){
		iRet = E_FE_MOD_SP_INVALID_MACADDR;
		goto ERR;
	}else{
		int iLen = strlen(macAddr);
		if(iLen != LEN_OF_MAC_ADDR){//mac addr contains 12 hex values
			iRet = E_FE_MOD_SP_INVALID_MACADDR;
			goto ERR;
		}else{
			for(int idx = 0; idx < iLen; idx++){
				if(!((0x30 <= macAddr[idx] && macAddr[idx] <= 0x39) //0~9
					 || (0x61 <= macAddr[idx] && macAddr[idx] <= 0x66))){ //a~f
					iRet = E_FE_MOD_SP_INVALID_MACADDR;
					goto ERR;
				}
			}
		}
	}

//	if(!(0 <= secType && secType <=3)){
//		iRet = E_FE_MOD_SP_INVALID_SEC_TYPE;
//		goto ERR;
//	}
//
//	if(0 < secType){
//		if(!wifiKey){
//			iRet = E_FE_MOD_SP_INVALID_WIFI_KEY;
//			goto ERR;
//		}else{
//			int iKeyLen = strlen(wifiKey);
//			if(1 == secType && (5 != iKeyLen || 13 != iKeyLen || 29 != iKeyLen)){//WEP http://compnetworking.about.com/od/wirelessfaqs/f/wep_keys.htm
//				iRet = E_FE_MOD_SP_INVALID_WIFI_KEY;
//				goto ERR;
//			}else if(2 <= secType && secType <=3 && (8 > iKeyLen || iKeyLen > 64)){//WPA/WPA2
//				iRet = E_FE_MOD_SP_INVALID_WIFI_KEY;
//				goto ERR;
//			}
//		}
//	}

//	if(!wifiKey){
//		iRet = E_FE_MOD_SP_INVALID_WIFI_KEY;
//		goto ERR;
//	}else{
//		int iKeyLen = strlen(wifiKey);
//		if(5 > iKeyLen){
//			iRet = E_FE_MOD_SP_INVALID_WIFI_KEY;
//			goto ERR;
//		}
//	}

	{
		int iMultiply = SoundPair_Config::getMultiplyByFFTYPE();
		int iPower = SoundPair_Config::getPowerByFFTYPE();

		const int iLen = LEN_OF_MAC_ADDR/iMultiply;
		char* macAddrZip = (char*)malloc((iLen+1)*sizeof(char));
		macAddrZip[iLen] = '\0';
		for(int i =0;i < iLen;i++){
			macAddrZip[i] = 0;
			for(int j = 0;j < iMultiply;j++){
				macAddrZip[i] <<= iPower;
				string code(1, macAddr[i*iMultiply+j]);
				int idx = SoundPair_Config::findIdxFromCodeTable(code);
				//LOGE("code= [%s],idx:%d\n", code.c_str(), idx);
				macAddrZip[i] += idx;
			}
			//LOGD("encode(), toEncode[%d]= %d\n",i,toEncode[i] );
		}

		//tmpUserToken = 0xff;

		//LOGE("macAddrZip= [%s]\n", macAddrZip);
		if(tmpUserToken > 0xff){
			sprintf(codeToPlay, "%s%c%s%c%c%c", macAddrZip, sep, wifiKey?wifiKey:"", sep, (tmpUserToken&0xff00)>>8, (tmpUserToken&0xff));
		}else{
			sprintf(codeToPlay, "%s%c%s%c%c", macAddrZip, sep, wifiKey?wifiKey:"", sep, (tmpUserToken&0xff));
		}

		if(macAddrZip){
			free(macAddrZip);
		}
	}
	//LOGE("codeToPlay= [%s]\n", codeToPlay);
	if(!FreqGenerator::getInstance()->playCode2(codeToPlay, true)){
		iRet = E_FE_MOD_SP_PLAY_CODE_ERR;
		goto ERR;
	}
ERR:
	//LOGE("iRet= [0x%x]\n", iRet);
	return iRet;
}

unsigned int FreqGenerator::playPairingCode(const char* macAddr, const char* wifiKey, unsigned int secType, unsigned short tmpUserToken){
	return playPairingCode(macAddr, wifiKey, tmpUserToken);
}

EncodeItm::EncodeItm(string strCodeInputASCII, string strCodeInput, string strECCode, string strEncodeMark, string strEncode) {
	//LOGE("EncodeItm(), strCodeInput:%s,\n %s,\n %s,\n %s\n", strCodeInput.c_str(), strECCode.c_str(), strEncodeMark.c_str(), strEncode.c_str());
	this->strCodeInputAscii = strCodeInputASCII;
	this->strCodeInput = strCodeInput;
	this->strECCode = strECCode;
	this->strEncodeMark = strEncodeMark;
	this->strEncode = strEncode;
}

string EncodeItm::toString() {
	std::stringstream s;
	s<<"EncodeItm [strCodeInputAscii="<<strCodeInputAscii<<", strCodeInput=" << strCodeInput << ", strECCode="
			<< strECCode << ", strEncodeMark=" << strEncodeMark << ", strEncode=" << strEncode << "]\n";
	return s.str();
}
