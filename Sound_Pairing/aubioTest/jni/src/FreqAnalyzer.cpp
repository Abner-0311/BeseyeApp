#include "FreqAnalyzer.h"
#include "AudioBufferMgr.h"
#include "time_utils.h"
#include <climits>
#include "fftw3.h"
#include <zxing/common/Array.h>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>

using zxing::ReedSolomonDecoder;
using zxing::Array;
using zxing::RS_DECODE_ERROR;

Ref<ReedSolomonDecoder> FreqAnalyzer::rsDecoder = Ref<ReedSolomonDecoder>(new ReedSolomonDecoder(GenericGF::QR_CODE_FIELD_256));
Ref<FreqAnalyzer> FreqAnalyzer::sFreqAnalyzer;

string MATCH_RESULTStoString(MATCH_RESULTS type){
	switch(type){
		case DESC_MATCH:
			return "Match_Before_EC";
		case DESC_MATCH_EC:
			return "Match_After_EC";
		case DESC_MATCH_MSG:
			return "Match_Msg_Only";
		case DESC_MISMATCH:
			return "Mismatch";
		case DESC_TIMEOUT:
			return "Timeout_Mismatch";
		case DESC_TIMEOUT_MSG:
			return "Timeout_Match_Msg_Only";
		case DESC_TIMEOUT_MSG_EC:
			return "Timeout_Match_Msg_EC";
		default:
			return "Invalid value";
	}
}

MatchRetSet::MatchRetSet(MATCH_RESULTS prevMatchRet, string strDecodeMark, string strDecodeUnmark, string strCode) {
	this->prevMatchRetType = prevMatchRet;
	this->strDecodeMark = strDecodeMark;
	this->strDecodeUnmark = strDecodeUnmark;
	this->strCode = strCode;
}

string MatchRetSet::toString() {
	std::stringstream s;
	s<<"MatchRetSet [prevMatchRetType=" << prevMatchRetType
			<< ", strDecodeMark=" << strDecodeMark << ", strDecodeUnmark="
			<< strDecodeUnmark << ", strCode=" << strCode << "]";
	return s.str();
}

FreqAnalyzer::FreqAnalyzer(bool bNeedToAutoCorrection):
mLastAbandant("0"),
mbStartAppend(false),
mbInSenderMode(false),
mbLowSoundDetected(false),
mbNeedToAutoCorrection(bNeedToAutoCorrection),
mSessionOffset(0),
mSessionOffsetForAmp(0),
mSessionBeginTs(-1),
mSessionBeginBufIdx(-1),
mLastCheckToneTs(0),
mlTraceTs(0),
mlMaxWaitingTime(0),
selfFreqAnalyzer(NULL),
mprevMatchRet(NULL),
mIFreqAnalyzeResultCBListener(NULL),
stPreprocess(NULL),
stPreprocessAC(NULL)
{
	//mbNeedToAutoCorrection = bNeedToAutoCorrection;
	bufSegment = ArrayRef<short>(new Array<short>(SoundPair_Config::FRAME_SIZE_REC));
}


Ref<FreqAnalyzer> FreqAnalyzer::getInstance(){
	if(sFreqAnalyzer.empty())
		sFreqAnalyzer = Ref<FreqAnalyzer>(new FreqAnalyzer(true));
	return sFreqAnalyzer;
}

bool FreqAnalyzer::destroyInstance(){
	deinitAudacity();
	if(sFreqAnalyzer.empty())
		return false;
	sFreqAnalyzer.reset(NULL);
	return true;
}

void FreqAnalyzer::setIFreqAnalyzeResultCB(IFreqAnalyzeResultCB* listener){
	mIFreqAnalyzeResultCBListener =listener;
}

FreqAnalyzer::~FreqAnalyzer(){
	mFreqRecordList.clear();
//	while(!mFreqRecordList.empty()){
//		Ref<FreqRecord> data = mFreqRecordList.back();
//		mFreqRecordList.pop_back();
//		if(data){
//			delete data;
//			data = NULL;
//		}
//	}

	mCodeRecordList.clear();
//	while(!mCodeRecordList.empty()){
//		Ref<CodeRecord> data = mCodeRecordList.back();
//		mCodeRecordList.pop_back();
//		if(data){
//			delete data;
//			data = NULL;
//		}
//	}
}

void FreqAnalyzer::reset(){
	LOGI("FreqAnalyzer::reset()\n");
	mLastCheckToneTs = -1;
	mbStartAppend = false;
	msbDecode.str("");
	msbDecode.clear();
	mFreqRecordList.clear();
	mSessionBeginTs = -1;
	mSessionOffset = 0;
	mLastAbandant = "0";
	mbLowSoundDetected = false;
}

void FreqAnalyzer::setSessionOffsetForAmp(int iOffset){
	mSessionOffsetForAmp = iOffset;
	if(DEBUG_MODE){
		LOGI("setSessionOffsetForAmp(), mSessionOffsetForAmp:%d\n", mSessionOffsetForAmp);
	}
}

void FreqAnalyzer::setSenderMode(bool bIsSenderMode){
	mbInSenderMode = bIsSenderMode;
}


void FreqAnalyzer::beginToTrace(string strCode){
	mstrCodeTrace = strCode;
	mlTraceTs = time_ms();
	mlMaxWaitingTime = (0 == strCode.length())?90000:(strCode.length()*400+4000);//90 seconds is experiment result
	if(DEBUG_MODE){
		LOGW("beginToTrace(), mlTraceTs:%lld, mlMaxWaitingTime: %lld\n", mlTraceTs, mlMaxWaitingTime);
	}
}


void FreqAnalyzer::endToTrace(){
	mlTraceTs = 0;
	mlMaxWaitingTime = 0;
	mbLowSoundDetected = false;
}

int FreqAnalyzer::getLastDetectedToneIdx(msec_t lCurTs){
	return getLastDetectedToneIdxOnCodeList(mCodeRecordList, lCurTs);
}

int FreqAnalyzer::getLastDetectedToneIdxOnCodeList(std::vector<Ref<CodeRecord> > lstCodeRecord, msec_t lCurTs){
	int iRet = 0;
	if(0 < lCurTs){
		int iSize = lstCodeRecord.size();
		if(0 < iSize){
			Ref<CodeRecord> lastRec = lstCodeRecord.back();
			if(lastRec){
				string strCode;
				msec_t lDelta = lCurTs - lastRec->lEndTs;
				if(lDelta > 0 && lDelta <= 3* SoundPair_Config::FRAME_TS){
					strCode = lastRec->strCdoe;
				}

				iRet = getToneIdxByCode(strCode);
			}
		}
	}
	return iRet;
}

int FreqAnalyzer::getToneIdxByCode(string strCode){
	int iRet = -1;
	if(strCode.length()){
		std::map<string,double>::iterator freq = SoundPair_Config::sAlphabetTable.find(strCode);
		//double dFreq = SoundPair_Config::sAlphabetTable[strCode];
		if(SoundPair_Config::sAlphabetTable.end() != freq){
			iRet = (int) (freq->second/SoundPair_Config::BIN_SIZE);
		}
	}
	return iRet;
}

string FreqAnalyzer::findToneCodeByFreq(double dFreq){
	string strCode;
	int iSize = SoundPair_Config::sFreqRangeTable.size();
	for(int idx = 0; idx < iSize;idx++){
		if(SoundPair_Config::sFreqRangeTable[idx]->withinFreqRange(dFreq)){
			strCode = SoundPair_Config::sFreqRangeTable[idx]->mstrCode;
			break;
		}
	}
	return strCode;
}

void FreqAnalyzer::checkTimeout(msec_t lTs){
	if(0 < mlTraceTs && 0 < mlMaxWaitingTime){
		msec_t lDelta = time_ms()- mlTraceTs;
		int iSize = mFreqRecordList.size();
		//LOGD("checkTimeout(), lDelta:"+lDelta+", lTs:"+lTs);
		if(false == mbStartAppend){
			if(lDelta > mlMaxWaitingTime){
				LOGE("checkTimeout(), lDelta > mlMaxWaitingTime----------\n");
				triggerTimeout();
			}else if(mbLowSoundDetected){
				LOGE("checkTimeout(), mbLowSoundDetected is true----------\n");
				triggerTimeout();
			}
		}else{

			if(0 < iSize){
				/*if(mbLowSoundDetected){
					LOGE("checkTimeout(), 2 mbLowSoundDetected is true----------\n");
					triggerTimeout();
				}else*/
				if((lTs - mFreqRecordList[iSize-1]->mlTs) >= 15 * SoundPair_Config::TONE_PERIOD || getInvalidFreqCount() >= 15 || (lDelta > mlMaxWaitingTime)){
					LOGE("checkTimeout(), cannot get ending char, lDelta:%lld, mlMaxWaitingTime:%lld\n", lDelta, mlMaxWaitingTime);
					triggerTimeout();
				}
			}
		}
	}
}

void FreqAnalyzer::triggerTimeout(){
	if(NULL != mIFreqAnalyzeResultCBListener){
		int iPosPostfix = -1;
		if(0 <= (iPosPostfix = checkPostfix())){
			if(mbStartAppend){
				normalAnalysis(iPosPostfix);
			}else{
				string strDecodeCheck(msbDecode.str());
				LOGW("detect postfix, try to find prefix in [%s] before %d\n", strDecodeCheck.c_str(), iPosPostfix);
//				string strPossibleDecode1 = strDecodeCheck.substr(0, iPosPostfix+2);
//				msbDecode.clear();
//				msbDecode<<strPossibleDecode1;

				int iPossiblePrefix = -1;
				int iPossibleSndPrefix = strDecodeCheck.rfind(SoundPair_Config::PREFIX_DECODE_C2, iPosPostfix-1);
				if(0 < iPossibleSndPrefix){
					LOGE("triggerTimeout(), detect [%s] at %d +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", SoundPair_Config::PREFIX_DECODE_C2.c_str(), iPossibleSndPrefix);
					iPossiblePrefix = iPossibleSndPrefix - 1;
				}else{
					int iPossibleFstPrefix = strDecodeCheck.rfind(SoundPair_Config::PREFIX_DECODE_C1, iPosPostfix-1);
					if(0 <= iPossibleFstPrefix){
						LOGE("triggerTimeout(), detect [%s] at %d +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", SoundPair_Config::PREFIX_DECODE_C1.c_str(), iPossibleFstPrefix);
						iPossiblePrefix = iPossibleFstPrefix ;
					}else{
						LOGE("triggerTimeout(), can not detect one of prefix +++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
						iPossiblePrefix = 0;
					}
				}

				if(-1 < iPossiblePrefix && strDecodeCheck.length() >= (iPosPostfix+2)){
					string strPossibleDecode = strDecodeCheck.substr(iPossiblePrefix+2, (iPosPostfix+2) - (iPossiblePrefix+2));//need to have postfix
					LOGE("triggerTimeout(), strPossibleDecode [%s] +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", strPossibleDecode.c_str());

					msbDecode.str("");
					msbDecode.clear();
					msbDecode<<strPossibleDecode;

//					int iCountToRemove = iPossiblePrefix;
//					while(0 < iCountToRemove--){
//						mCodeRecordList.erase(mCodeRecordList.begin());
//					}

					int iIndex = (iPosPostfix - iPossiblePrefix);

					LOGE("triggerTimeout(), to check result, iIndex:[%d]+++++++++++++++++++++++++++++++++++++++++++++++++++++\n", iIndex);
					if(0 <= iIndex && iIndex < strPossibleDecode.length()){
						checkResult(strPossibleDecode, optimizeDecodeString(iIndex, strPossibleDecode));
						mFreqRecordList.clear();
					}else{
						LOGE("triggerTimeout(), invalid iIndex:%d, strPossibleDecode.length():%d \n", iIndex, strPossibleDecode.length());
						mIFreqAnalyzeResultCBListener->onTimeout(this, !mbNeedToAutoCorrection, mprevMatchRet);
					}
				}else{
					mIFreqAnalyzeResultCBListener->onTimeout(this, !mbNeedToAutoCorrection, mprevMatchRet);
				}
			}
		}else{
			mIFreqAnalyzeResultCBListener->onTimeout(this, !mbNeedToAutoCorrection, mprevMatchRet);
		}
	}
}

void FreqAnalyzer::analyze(msec_t lTs, double dFreq, int iBufIndex, int iFFTValues[]){
	if(SoundPair_Config::PRE_EMPTY && true == mbStartAppend){
		Ref<CodeRecord> lastRec = mCodeRecordList[1];
		msec_t lEndTs = lastRec->lEndTs;
		if((lTs - lEndTs) <= SoundPair_Config::TONE_PERIOD){
			LOGW("analyze(), (%lld - %lld) <= TONE_PERIOD:%ld, return\n", lTs, lEndTs, SoundPair_Config::TONE_PERIOD);
			return;
		}
	}

	string strCode = findToneCodeByFreq(dFreq);

	if(0 == strCode.compare("")){
		if(dFreq > 0.0 || mbStartAppend){
			LOGW("analyze(), cannot find code for freq:%f at %lld\n", dFreq, lTs);
			if(mbStartAppend)
				mFreqRecordList.push_back(Ref<FreqRecord>(new FreqRecord(lTs, dFreq, SoundPair_Config::MISSING_CHAR, iBufIndex, iFFTValues)));
		}



		int iSize = mFreqRecordList.size();
		if(false == mbStartAppend && mSessionBeginTs > 0 && (0 < iSize && (lTs - mFreqRecordList[iSize-1]->mlTs) >= 3 * SoundPair_Config::TONE_PERIOD || getInvalidFreqCount() >= 3)){
			LOGE("analyze(), remove bias\n");
			reset();
		}
	}else{
		if(DEBUG_MODE){
			LOGD("analyze(), find code [%s] for freq:%f at %lld, mSessionBeginTs:%lld\n",strCode.c_str() , dFreq, lTs, mSessionBeginTs);
		}
		mFreqRecordList.push_back(Ref<FreqRecord>(new FreqRecord(lTs, dFreq, strCode, iBufIndex, iFFTValues)));
		int iCheckIndex = exceedToneCheckPeriod();
		if(-1 < iCheckIndex){
			if(SoundPair_Config::SEGMENT_FEATURE && -1 < mSessionBeginTs){
				pickWithSeesion();
			}else{
				pickWithoutSession(iCheckIndex);
			}

			checkInvalidAnalysis();
		}
	}

	checkTimeout(lTs);
}

void FreqAnalyzer::checkInvalidAnalysis(){
	if(msbDecode.str().length() > MAX_ANALYSIS_LEN){
		LOGE("msbDecode.str().length() [%d] > MAX_ANALYSIS_LEN [%d]\n",msbDecode.str().length() , MAX_ANALYSIS_LEN);
		triggerTimeout();
	}
}

int FreqAnalyzer::getInvalidFreqCount(){
	int iRet = 0;
	int iSize = mFreqRecordList.size();
	for(int idx = iSize -1; idx >= 0; idx--){
		Ref<FreqRecord> fr = mFreqRecordList[idx];
		if(NULL == fr || (0==fr->mstrCode.compare(SoundPair_Config::MISSING_CHAR)) || (0==fr->mstrCode.compare(""))){
			iRet++;
		}else{
			break;
		}
	}
	if(0 < iRet){
		LOGE("getInvalidFreqCount(), iRet:%d\n", iRet);
	}
	return iRet;
}

void FreqAnalyzer::fillEmptyCodeRecord(msec_t lCurSesBeginTs){
	int iSize = mCodeRecordList.size();
	if(0 < iSize){
		Ref<CodeRecord> lastCodeRec = mCodeRecordList[iSize -1];
		if(NULL != lastCodeRec){
			msec_t lBeginTs = lastCodeRec->lStartTs;
			msec_t lDelta = lCurSesBeginTs - lBeginTs;

			int iFillCount = 0;
			while(SoundPair_Config::TONE_PERIOD < lDelta){
				if(SoundPair_Config::PRE_EMPTY && (lBeginTs-mSessionBeginTs) == SoundPair_Config::TONE_PERIOD){
					LOGD("fillEmptyCodeRecord()**, ====>>>> no need to add CodeRecord at %lld, due to PRE_EMPTY\n", (lBeginTs+SoundPair_Config::TONE_PERIOD));
				}else{
					LOGI("fillEmptyCodeRecord()**, ====>>>> add empty CodeRecord at %lld\n", (lBeginTs+SoundPair_Config::TONE_PERIOD));
					mCodeRecordList.push_back(Ref<CodeRecord>(new CodeRecord(lBeginTs+SoundPair_Config::TONE_PERIOD,
															 lBeginTs+SoundPair_Config::TONE_PERIOD*2-SoundPair_Config::FRAME_TS,
															 SoundPair_Config::MISSING_CHAR)));
					//LOGI("fillEmptyCodeRecord()**, ====>>>>XXX add empty CodeRecord at %lld\n", (lBeginTs+SoundPair_Config::TONE_PERIOD));
				}
				lBeginTs += SoundPair_Config::TONE_PERIOD;
				lDelta = lCurSesBeginTs - lBeginTs;

				if(100 < ++iFillCount){
					LOGD("fillEmptyCodeRecord()**, ====>>>> too many fill, break !!!!!!!!!!!!!\n" );
					break;
				}
			}
		}
	}
}

void FreqAnalyzer::pickWithSeesion(){
	msec_t lSesIdx = (mFreqRecordList[0]->mlTs - mSessionBeginTs)/SoundPair_Config::TONE_PERIOD;
	msec_t lSesBeginTs = mSessionBeginTs+SoundPair_Config::TONE_PERIOD*lSesIdx;
	msec_t lSesEndTs = mSessionBeginTs+SoundPair_Config::TONE_PERIOD*(lSesIdx+1);

	if(CAM_CORRECTION_TRACE){
		if(DEBUG_MODE){
			LOGI("pickWithSeesion()**, ====>>>> (lSesBeginTs, lSesEndTs): (%lld, %lld)\n", lSesBeginTs, lSesEndTs);
		}
	}

	fillEmptyCodeRecord(lSesBeginTs);

	//Find out all session items
	vector<Ref<FreqRecord> > sesFreqList;
	while(0 < mFreqRecordList.size()){
		Ref<FreqRecord> fr = mFreqRecordList[0];
		if(fr->mlTs < lSesEndTs){
			mFreqRecordList.erase(mFreqRecordList.begin());
			sesFreqList.push_back(fr);
		}else{
			break;
		}
	}
	//LOGI("analyze(), ====>>>> sesFreqList:"+sesFreqList.toString());

	if(CAM_CORRECTION_TRACE){
		if(DEBUG_MODE){
			for(int i = 0;i<sesFreqList.size();i++){
				LOGI("pickWithSeesion()**, sesFreqList[%d]:%s\n", i, sesFreqList[i]->toString().c_str());
			}
		}
	}

	//Fill the missing items
	if(sesFreqList.size() == 0){
		if(DEBUG_MODE){
			LOGI("pickWithSeesion()**, ====>>>> no items in sesFreqList\n");
		}
		for(int idx = 0; idx < SoundPair_Config::TONE_FRAME_COUNT; idx++){
			sesFreqList.insert(sesFreqList.begin(), Ref<FreqRecord>(new FreqRecord(lSesBeginTs + idx*SoundPair_Config::FRAME_TS, -1.0f, SoundPair_Config::MISSING_CHAR, -1, NULL)));
		}
	}else{
		for(int idx = 0; idx < SoundPair_Config::TONE_FRAME_COUNT; idx++){
			//LOGI("pickWithSeesion()**, idx:%d\n", idx);
			msec_t lTsByIdx = lSesBeginTs + idx*SoundPair_Config::FRAME_TS;
			Ref<FreqRecord> fr = sesFreqList.size() > idx ? sesFreqList[idx]:Ref<FreqRecord>(NULL);
			//LOGI("pickWithSeesion()**, idx:%d, lTsByIdx:%lld\n", idx, lTsByIdx);
			if(NULL == fr || fr->mlTs != lTsByIdx){
				sesFreqList.insert(sesFreqList.begin()+idx, Ref<FreqRecord>(new FreqRecord(lTsByIdx, -1.0f, SoundPair_Config::MISSING_CHAR, -1, NULL)));
				LOGI("pickWithSeesion()**, ====>>>> add missing char to sesFreqList at %lld\n", (lTsByIdx));
			}
		}
	}

	Ref<CodeRecord> rec = Ref<CodeRecord>(new CodeRecord(sesFreqList, mLastAbandant));
	mLastCheckToneTs = rec->lEndTs;
	appendRet(rec);

	checkCodeRecordShift();
}

void FreqAnalyzer::pickWithoutSession(int iCheckIndex){
	//LOGI("analyze(), codeList = "+codeList.toString());
	vector<Ref<FreqRecord> > codeList;
	for(int i =0; i <= iCheckIndex;i++){
		Ref<FreqRecord> fr = mFreqRecordList[i];
		codeList.push_back(fr);
		if(DEBUG_MODE){
			LOGI("pickWithoutSession(), ====>>>> sFreqRecordList[%d]: %s\n", i, fr->toString().c_str());
		}
	}

	//Criteria 1: all the same code
	string strLastCheck;
	string strFirstCode;
	msec_t lFirstCodeTs = -1;
	int iNumSameFst = 1;
	int iLastIdxSameFst = 0;

	if(0 >= iCheckIndex){
		LOGE("pickWithoutSession(), invalid iCheckIndex:%d\n", iCheckIndex);
		return;
	}

	bool bDiff = false;
	for(int iIdx = 0; iIdx < codeList.size(); iIdx++){
		if(!strLastCheck.length()){
			strFirstCode = strLastCheck = codeList[iIdx]->mstrCode;
			lFirstCodeTs = codeList[iIdx]->mlTs;
			continue;
		}

		if((0!=codeList[iIdx]->mstrCode.compare(strLastCheck)) || SoundPair_Config::TONE_PERIOD <= (codeList[iIdx]->mlTs - lFirstCodeTs)){
			bDiff = true;
		}

		if((0==codeList[iIdx]->mstrCode.compare(strFirstCode)) && SoundPair_Config::TONE_PERIOD > (codeList[iIdx]->mlTs - lFirstCodeTs) && iIdx == iNumSameFst){
			iNumSameFst++;
			iLastIdxSameFst = iIdx;
		}

		strLastCheck = codeList[iIdx]->mstrCode;
	}

	vector<Ref<FreqRecord> > sesFreqList;
	if(false == bDiff){
		checkEmptySlot();

		if(DEBUG_MODE){
			LOGI("pickWithoutSession(), all the same, code = [%s]\n", mFreqRecordList.front()->mstrCode.c_str());
		}

		msec_t lFirstTs = mFreqRecordList.front()->mlTs;

		msec_t lStartTs = mFreqRecordList.front()->mlTs;

		for(int i = 0 ; i <= iCheckIndex; i++){
			if(SoundPair_Config::TONE_PERIOD > (mFreqRecordList.front()->mlTs - lFirstTs)){
				mLastCheckToneTs = mFreqRecordList.front()->mlTs;
				sesFreqList.push_back(mFreqRecordList.front());
				mFreqRecordList.erase(mFreqRecordList.begin());
			}else{
				LOGW("pickWithoutSession(), break for itm > SoundPair_Config::TONE_PERIOD\n");
				break;
			}
		}

		appendRet(Ref<CodeRecord>(new CodeRecord(lStartTs, mLastCheckToneTs, strFirstCode, sesFreqList)));
	}//else if((iNumSameFst) >= ((iCheckIndex/2)+1)){
	else if((iNumSameFst) >= 2){

		checkEmptySlot();

		if(DEBUG_MODE){
			LOGI("pickWithoutSession(), first itm happens over 50 %, code = [%s]\n", mFreqRecordList.front()->mstrCode.c_str());
		}
		msec_t lFirstTs = mFreqRecordList.front()->mlTs;

		msec_t lStartTs = mFreqRecordList.front()->mlTs;
		for(int i = 0 ; i <= iLastIdxSameFst; i++){
			if(SoundPair_Config::TONE_PERIOD > (mFreqRecordList.front()->mlTs - lFirstTs)){
				mLastCheckToneTs = mFreqRecordList.front()->mlTs;
				sesFreqList.push_back(mFreqRecordList.front());
				mFreqRecordList.erase(mFreqRecordList.begin());
			}else{
				LOGW("pickWithoutSession(), break for itm > SoundPair_Config::TONE_PERIOD\n");
				break;
			}
		}
		appendRet(Ref<CodeRecord>(new CodeRecord(lStartTs, mLastCheckToneTs, strFirstCode, sesFreqList)));
	}else if(0 == iNumSameFst){
		if(DEBUG_MODE){
			LOGI("pickWithoutSession(), remove first noise = [%s], sLastCheckToneTs=%lld\n", strFirstCode.c_str(), mLastCheckToneTs);
		}
		if(true == mbStartAppend){
			Ref<FreqRecord> rec = mFreqRecordList.front();
			if((SoundPair_Config::TONE_PERIOD*SoundPair_Config::RANDUDANT_RATIO) <= (mFreqRecordList[1]->mlTs - mLastCheckToneTs)){
				if(DEBUG_MODE){
					LOGW("analyze(), -> Add first noise = [%s]\n", strFirstCode.c_str());
				}
				mLastCheckToneTs = mFreqRecordList.front()->mlTs+SoundPair_Config::TONE_PERIOD/2;
				appendRet(Ref<CodeRecord>(new CodeRecord( mLastCheckToneTs-SoundPair_Config::TONE_PERIOD,
										  mLastCheckToneTs,
										  mFreqRecordList.front()->mstrCode)));
			}

			mFreqRecordList.erase(mFreqRecordList.begin());
			mLastAbandant = rec->mstrCode;

		}else
			mFreqRecordList.erase(mFreqRecordList.begin());
	}else if(2 <= iCheckIndex){
		if(DEBUG_MODE){
			LOGI("pickWithoutSession(), else case -> remove first noise = [%s], sLastCheckToneTs=%lld\n", strFirstCode.c_str(), mLastCheckToneTs);
		}
		if(true == mbStartAppend){
			Ref<FreqRecord> rec = mFreqRecordList.front();
			if((SoundPair_Config::TONE_PERIOD*SoundPair_Config::RANDUDANT_RATIO) <= (mFreqRecordList[1]->mlTs - mLastCheckToneTs)){
				if(DEBUG_MODE){
					LOGI("analyze(), else case -> Add first noise = [%s]\n", strFirstCode.c_str());
				}

				mLastCheckToneTs = rec->mlTs+SoundPair_Config::TONE_PERIOD/2;
				appendRet(Ref<CodeRecord>(new CodeRecord( mLastCheckToneTs-SoundPair_Config::TONE_PERIOD,
										  mLastCheckToneTs,
										  rec->mstrCode)));
			}
			mFreqRecordList.erase(mFreqRecordList.begin());
			mLastAbandant = rec->mstrCode;

		}else
			mFreqRecordList.erase(mFreqRecordList.begin());
	}
}

void FreqAnalyzer::checkEmptySlot(){
	if(true == mbStartAppend){
		if(-1 != mLastCheckToneTs){
//				msec_t lTimeFromLastOne = sFreqRecordList.get(0).mlTs - sLastCheckToneTs;
//
//				if(SoundPair_Config::TONE_PERIOD < lTimeFromLastOne){
//					int iNumRedundant = (int) (lTimeFromLastOne/SoundPair_Config::TONE_PERIOD);
//					for(int i = 0; i < iNumRedundant; i++){
//						LOGW("checkEmptySlot(), add redundant one, sLastCheckToneTs = "+sLastCheckToneTs+\n", sLastAbandant="+sLastAbandant);
//						//if((0!=sLastAbandant.compare("W")) || (0!=sLastAbandant.compare("X")) || (0!=sLastAbandant.compare("Y")) || (0!=sLastAbandant.compare("Z")))
//						if((0!=sLastAbandant.compare(sFreqRangeTable.get(sFreqRangeTable.size()-2).mstrCode)) || (0!=sLastAbandant.compare(sFreqRangeTable.get(sFreqRangeTable.size()-1).mstrCode)))
//							sLastAbandant = "0";
//
//						sLastCheckToneTs +=SoundPair_Config::TONE_PERIOD;
//						appendRet((sLastCheckToneTs-SoundPair_Config::TONE_PERIOD/2),
//								  (sLastCheckToneTs+SoundPair_Config::TONE_PERIOD/2),
//								  sLastAbandant);
//					}
//				}
		}
	}
}

void FreqAnalyzer::appendRet(Ref<CodeRecord> rec){
	msbDecode<<rec->strCdoe;
	string strDecodeCheck(msbDecode.str());
	if(DEBUG_MODE){
		LOGE("appendRet(), msbDecode:%s, mbStartAppend:%d\n", strDecodeCheck.c_str(), mbStartAppend);
	}
	if(true == mbStartAppend){
		Ref<CodeRecord> lastItm = (0 < mCodeRecordList.size())?mCodeRecordList.back():Ref<CodeRecord>(NULL);
		if(NULL != lastItm && (0==lastItm->strCdoe.compare(rec->strCdoe)) && (SoundPair_Config::TONE_PERIOD >= (rec->lEndTs - lastItm->lStartTs))){
			lastItm->lEndTs = rec->lEndTs;
		}else{
			//LOGE("appendRet(), rec:%s\n", rec->toString().c_str());
			mCodeRecordList.push_back(rec);
		}
	}else if(false == mbStartAppend){
		int iPox = strDecodeCheck.rfind(SoundPair_Config::PREFIX_DECODE);
		//LOGE("appendRet(), iPox:%d\n",iPox);
		if(-1 < iPox){
			//LOGE("appendRet(), rec:%s\n", rec->toString().c_str());
			mCodeRecordList.push_back(rec);
		}else if(SoundPair_Config::SEGMENT_FEATURE && (strDecodeCheck.length()-1) == strDecodeCheck.rfind(SoundPair_Config::PREFIX_DECODE_C1)){
			LOGE("appendRet(), detect FIRST CHAR ------------------------------------------------------\n");

			//if First Char is detected already
			if(-1 < mSessionBeginTs){
				if((rec->lStartTs - mSessionBeginTs) > SoundPair_Config::TONE_PERIOD && 0 < rec->mlstFreqRec.size()){
					if(DEBUG_MODE){
						LOGE("appendRet(), lStartTs - sSessionBeginTs > SoundPair_Config::TONE_PERIOD ==> recover and recheck\n");
					}
					for(int i = rec->mlstFreqRec.size() -1; i >= 0;i--){
						Ref<FreqRecord> fr = rec->mlstFreqRec[i];
						if((0!=fr->mstrCode.compare(SoundPair_Config::MISSING_CHAR))){
							mFreqRecordList.insert(mFreqRecordList.begin(), fr);
						}
					}
					mSessionBeginTs= -1;
					mSessionBeginBufIdx = -1;
					int iSize = mCodeRecordList.size();
					if(0 < iSize && (0==mCodeRecordList[iSize-1]->strCdoe.compare(rec->strCdoe))){
						mCodeRecordList.pop_back();
					}

					pickWithoutSession(exceedToneCheckPeriod());
					return;
				}else{
					if(DEBUG_MODE){
						LOGE("appendRet(), lStartTs - sSessionBeginTs == SoundPair_Config::TONE_PERIOD ==> no need to move\n");
					}
					checkFirstCharOfPrefix(rec);
				}
			}else{
				checkFirstCharOfPrefix(rec);
			}
		}
	}
	appendRet(rec->strCdoe);
}

static const int SHIFT_TRIGGER_CNT = 2;

void FreqAnalyzer::checkCodeRecordShift(){
	int iSize = mCodeRecordList.size();
	if(iSize <= 2){
		LOGE("iSize:[%d], return\n", iSize);
		return;
	}
	CodeRecord::Tone_Shift_Status tsStatusLastRec = (0 < iSize)?mCodeRecordList[iSize-1]->getToneShiftStatus():CodeRecord::TS_NONE;
	if(DEBUG_MODE){
		LOGE("tsStatusLastRec: [%d] , iSize:[%d]\n", tsStatusLastRec, iSize);
	}
	if(CodeRecord::TS_NONE != tsStatusLastRec){
		int iSameShiftCnt = 1;
		for(int idx = iSize -2; idx >=0;idx--){
			Ref<CodeRecord> chkRec = mCodeRecordList[idx];
			if(chkRec){
				CodeRecord::Tone_Shift_Status tsStatus = chkRec->getToneShiftStatus();

				if(DEBUG_MODE){
					LOGE("tsStatus: [%d], iSameShiftCnt:[%d] \n", tsStatus, iSameShiftCnt);
				}

				if(tsStatusLastRec != tsStatus){
					return;
				}else{
					if(++iSameShiftCnt == SHIFT_TRIGGER_CNT){
						LOGE("detect Tone_Shift_Status: [%d] for %d straight times\n", tsStatusLastRec, SHIFT_TRIGGER_CNT);
						if(CodeRecord::TS_FORWARD == tsStatusLastRec){
							Ref<FreqRecord> frFstRec, frSndRec;
							string strToChange = "";
							for(int idx = iSize-2; ((iSize-1) - idx) < SHIFT_TRIGGER_CNT;idx--){
								Ref<CodeRecord> fstRec = mCodeRecordList[idx];
								Ref<CodeRecord> sndRec = mCodeRecordList[idx+1];
								if(fstRec && sndRec){
									frFstRec = fstRec->popTailRec();
									frSndRec = sndRec->popTailRec();
									if(frFstRec && frSndRec){
										if(DEBUG_MODE){
											LOGE("frFstRec: [%s]\n", frFstRec->toString().c_str());
											LOGE("frSndRec: [%s]\n", frSndRec->toString().c_str());
										}

										if(idx == (iSize-2)){ //Begin case
											mFreqRecordList.insert(mFreqRecordList.begin(), frSndRec);
										}
										sndRec->pushToHead(frFstRec);
										strToChange = sndRec->strCdoe + strToChange;
										if(DEBUG_MODE){
											LOGE("strToChange: [%s]\n", strToChange.c_str());
										}
									}
								}else{
									LOGE("fstRec: [%d] or %d sndRec: [%d] is null\n", (NULL != fstRec), (NULL != sndRec));
								}
							}

							if(0 < strToChange.length()){
								mSessionBeginTs-=SoundPair_Config::FRAME_TS;
								string strDecodeOld(msbDecode.str());
								if(0 < strDecodeOld.length() && strToChange.length() <= strDecodeOld.length() ){
									msbDecode.str("");
									msbDecode.clear();
									msbDecode << strDecodeOld.substr(0, strDecodeOld.length() - strToChange.length());
									msbDecode << strToChange;

									LOGE("change msbDecode from [%s] "
										 "                   to [%s] \n", strDecodeOld.c_str(), msbDecode.str().c_str());

								}
							}
						}else{
							Ref<FreqRecord> frFstRec, frSndRec;
							string strToChange = "";
							for(int idx = (iSize - SHIFT_TRIGGER_CNT);  idx < (iSize-1) ;idx++){
								Ref<CodeRecord> fstRec = mCodeRecordList[idx];
								Ref<CodeRecord> sndRec = mCodeRecordList[idx+1];
								if(fstRec && sndRec){
									frFstRec = fstRec->popHeadRec();
									frSndRec = sndRec->popHeadRec();
									if(frFstRec && frSndRec){
										if(DEBUG_MODE){
											LOGE("frFstRec: [%s]\n", frFstRec->toString().c_str());
											LOGE("frSndRec: [%s]\n", frSndRec->toString().c_str());
										}

										fstRec->pushToTail(frSndRec);
										strToChange = fstRec->strCdoe + strToChange;
										if(DEBUG_MODE){
											LOGE("strToChange: [%s]\n", strToChange.c_str());
										}

										if(idx == (iSize-2)){ //End case
											mFreqRecordList.insert(mFreqRecordList.begin(), sndRec->popTailRec());
											mFreqRecordList.insert(mFreqRecordList.begin(), sndRec->popHeadRec());
										}
									}
								}else{
									LOGE("fstRec: [%d] or %d sndRec: [%d] is null\n", (NULL != fstRec), (NULL != sndRec));
								}
							}

							if(0 < strToChange.length()){
								mSessionBeginTs+=SoundPair_Config::FRAME_TS;
								string strDecodeOld(msbDecode.str());
								if(0 < strDecodeOld.length() && strToChange.length() <= strDecodeOld.length() ){
									msbDecode.str("");
									msbDecode.clear();
									msbDecode << strDecodeOld.substr(0, strDecodeOld.length() - (strToChange.length()+1));
									msbDecode << strToChange;

									LOGE("change msbDecode from [%s] "
										 "                   to [%s] \n", strDecodeOld.c_str(), msbDecode.str().c_str());

								}
							}
						}
						return;
					}
				}
			}
		}
	}
}

void FreqAnalyzer::appendRet(string strCode){
	string strDecodeCheck(msbDecode.str());
	if(false == mbInSenderMode){
		if(false == mbStartAppend){
			int iPox = strDecodeCheck.rfind(SoundPair_Config::PREFIX_DECODE);
			if(DEBUG_MODE){
				LOGE("appendRet(),SoundPair_Config::PREFIX_DECODE:%s , strCode:%s, mbInSenderMode:%d, iPox:%d\n", SoundPair_Config::PREFIX_DECODE.c_str(), strCode.c_str(), mbInSenderMode, iPox);
			}
			if(-1 < iPox){
				LOGE("appendRet(), detect SoundPair_Config::PREFIX_DECODE ------------------------------------------------------\n");
				mbStartAppend = true;
				mLastAbandant = "0";
				msbDecode.str("");
				msbDecode.clear();
				if(mIFreqAnalyzeResultCBListener){
					mIFreqAnalyzeResultCBListener->onDetectStart();
					if(mbNeedToAutoCorrection){
						mIFreqAnalyzeResultCBListener->onSetResult("", "", "", !mbNeedToAutoCorrection, mprevMatchRet);
					}
				}

				Ref<CodeRecord> lastTwoRec = mCodeRecordList[mCodeRecordList.size()-2];
				if(DEBUG_MODE){
					LOGE("appendRet(), lastTwoRec:%s\n", (lastTwoRec)?lastTwoRec->toString().c_str():"null");
				}
				Ref<CodeRecord> lastRec = mCodeRecordList.back();
				if(DEBUG_MODE){
					LOGE("appendRet(), lastRec:%s\n", (lastRec)?lastRec->toString().c_str():"null");
				}

				mCodeRecordList.clear();
				mCodeRecordList.push_back(lastTwoRec);
				mCodeRecordList.push_back(lastRec);
				if(DEBUG_MODE){
					LOGE("appendRet(), lastRec:%s\n", (lastRec)?lastRec->toString().c_str():"null");
				}

				beginToTrace("");

				mSessionOffset = segmentCheck(false);

				if(SoundPair_Config::PRE_EMPTY){
					for(int i =0; i < mFreqRecordList.size();){
						if(mFreqRecordList.front()->mlTs - lastRec->lEndTs <= SoundPair_Config::TONE_PERIOD){
							LOGW("appendRet(), (%lld - %lld) <= SoundPair_Config::TONE_PERIOD:%lld, remove\n", mFreqRecordList.front()->mlTs, lastRec->lEndTs, SoundPair_Config::TONE_PERIOD);
							mFreqRecordList.erase(mFreqRecordList.begin());
						}else{
							break;
						}
					}
				}
			}
		}else{
			int iIndex = -1;
			if(-1 < (iIndex = checkPostfix())){
				normalAnalysis(iIndex);
			}else{
				if(mIFreqAnalyzeResultCBListener)
					mIFreqAnalyzeResultCBListener->onAppendResult(strCode);
				//msbDecode.append(strCode);
			}
		}
	}else{
		if(-1 < strDecodeCheck.rfind(SoundPair_Config::PEER_SIGNAL)){
			msbDecode.str("");
			msbDecode.clear();
			if(mIFreqAnalyzeResultCBListener)
				mIFreqAnalyzeResultCBListener->onSetResult(SoundPair_Config::PEER_SIGNAL, "", "", !mbNeedToAutoCorrection, mprevMatchRet);
		}
	}
}

void FreqAnalyzer::normalAnalysis(int iIndex){
	mbStartAppend = false;
	//const int MAC_LEN = 12;
	string strDecodeCheck(msbDecode.str());
	int iFirstDiv = strDecodeCheck.find(SoundPair_Config::PAIRING_DIVIDER);

	if(DEBUG_MODE){
		LOGE("normalAnalysis(), index = %d, iFirstDiv=%d\n",iIndex,iFirstDiv);
	}

	if(0 > iIndex/* || iFirstDiv != MAC_LEN*/){
//		int iShift = checkFrameBySessionAndAutoCorrection();
//		strDecodeCheck = msbDecode.str();
//		if(0 != iShift){
			int iNewIndex = checkPostfix();
//			LOGE("normalAnalysis(), redetect index, iShift = %d, iNewIndex=%d\n",iShift,iNewIndex);

			int iDxFstC = -1;
			do{
				iDxFstC = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C1, iDxFstC+1);
			}while(0 <= iDxFstC && iDxFstC < SoundPair_Config::MIN_PAIRING_MSG_LEN);

			int iDxSndC = -1;
			do{
				iDxSndC = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C2, iDxSndC+1);
			}while(0 <= iDxSndC && iDxSndC < SoundPair_Config::MIN_PAIRING_MSG_LEN);

			if(0 <= iNewIndex){
				if(0 < iDxFstC && iDxFstC < iNewIndex && 1 >= abs(iNewIndex-iIndex) && iDxFstC >= SoundPair_Config::MIN_PAIRING_MSG_LEN && (0 == iNewIndex%2)){//special case 1: ...H...HI
					LOGE("normalAnalysis(), special case 1, iDxFstC=%d\n",iDxFstC);
					iNewIndex = iDxFstC;
				}else if(0 < iDxSndC && iDxSndC < iNewIndex+1 && 1 >= abs(iNewIndex-iIndex) && iDxSndC -1 >= SoundPair_Config::MIN_PAIRING_MSG_LEN && (0 == iNewIndex%2)){//special case 2: ...I...HI
					LOGE("normalAnalysis(), special case 2, iDxSndC=%d\n",iDxSndC);
					iNewIndex = iDxSndC - 1;
				}
			}else{
				LOGE("normalAnalysis(), can not find postfix, redetect index at first char\n");
				if(-1 == iDxFstC){
					LOGE("normalAnalysis(), can not find first car of postfix, redetect index at second char\n");
					if(-1 == iDxSndC){
						LOGE("normalAnalysis(), can not find any char of postfix, redetect index by shift one\n");
						//iNewIndex = (iIndex-iShift);
					}else{
						iNewIndex = iDxSndC - 1;
					}
				}else{
					iNewIndex = iDxFstC;
				}
			}

			if(-1 < iNewIndex && iNewIndex != iIndex){
				LOGE("normalAnalysis(), change index from %d to %d\n", iIndex, iNewIndex);
				iIndex = iNewIndex;
			}
//		}
	}

	if(DEBUG_MODE){
		LOGE("normalAnalysis(), iIndex = %d,  strDecodeCheck.length()=%d\n",iIndex, strDecodeCheck.length());
	}

	if(0 <= iIndex && iIndex < strDecodeCheck.length()){
		string strDecodeChk = strDecodeCheck.substr(0, iIndex);
		checkResult(strDecodeChk, optimizeDecodeString(iIndex, strDecodeCheck));
	}else{
		LOGE("normalAnalysis(), invalid iIndex:%d, strDecodeCheck.length():%d \n", iIndex, strDecodeCheck.length());
	}

	mFreqRecordList.clear();

}

string FreqAnalyzer::optimizeDecodeString(int iIndex, string strDecodeCheck){
	//string strDecode = msbDecode.substr(0, msbDecode.length()-((-1 < msbDecode.rfind(POSTFIX_DECODE) )?POSTFIX_DECODE.length():1));
	string strDecodeUnmark;
	//string strDecodeCheck(msbDecode.str());

	LOGE("optimizeDecodeString(), strDecodeCheck:[%s], index is %d\n", strDecodeCheck.c_str(), iIndex);

	if(0 <= iIndex && iIndex < strDecodeCheck.length()){
		string strDecode = strDecodeCheck.substr(0, iIndex);
		strDecodeUnmark = replaceInvalidChar(strDecode);

		int iLen = strDecodeUnmark.length();

		if(0 < iLen){
			if(SoundPair_Config::getMultiplyByFFTYPE() > 1 && 0 != iLen%SoundPair_Config::getMultiplyByFFTYPE()){
				if(0 < getMsgLength(iLen+1)){
					//try to find correct missing char

					string strCheck;
					for(int i = 0; i < iLen; i++){
						if(i == 0){
							strCheck = SoundPair_Config::SoundPair_Config::sCodeTable.front()+strDecodeUnmark;
						}else if(i == (iLen-1)){
							strCheck = strDecodeUnmark+SoundPair_Config::SoundPair_Config::sCodeTable.front();
						}else{
							strCheck = strDecodeUnmark.substr(0, i)+SoundPair_Config::SoundPair_Config::sCodeTable.front()+strDecodeUnmark.substr(i, iLen-i);
						}
						string strRet = decodeRSEC(strCheck);

						int iFound = strRet.find("error");
						//LOGE("optimizeDecodeString(), strRet %s, iFound:%d\n",strRet.c_str(), iFound);
						if(0 > iFound){
							strDecodeUnmark = strCheck;
							LOGE("optimizeDecodeString(), add dummy char at %d because index is %d\n",i, iIndex);
							break;
						}
					}
		//			strDecode = SoundPair_Config::SoundPair_Config::sCodeTable.front()+strDecode;
		//			LOGE("optimizeDecodeString(), add dummy char at head because index is %d\n", iIndex);
				}else if(0 < getMsgLength(iLen-1)){
					strDecodeUnmark = strDecodeUnmark.substr(0, iLen -1);
					LOGE("optimizeDecodeString(), remove dummy char at tail because index is %d\n", iIndex);
				}else{
					LOGE("optimizeDecodeString(), can not optimize, index is %d\n", iIndex);
				}
			}
		}else{
			LOGE("optimizeDecodeString(), invalid iLen:%d \n", iLen);
		}
	}else{
		LOGE("optimizeDecodeString(), invalid iIndex:%d, strDecodeCheck.length():%d \n", iIndex, strDecodeCheck.length());
	}
	return strDecodeUnmark;
}

int FreqAnalyzer::checkPostfix(){
	if(DEBUG_MODE){
		LOGD("checkPostfix()++\n");
	}
	string strDecodeCheck(msbDecode.str());
	int iRet = strDecodeCheck.rfind(SoundPair_Config::POSTFIX_DECODE);
	if(-1 >= iRet){
//			string strFstPostfix = SoundPair_Config::POSTFIX_DECODE.substr(0,1);
//			string strSndPostfix = SoundPair_Config::POSTFIX_DECODE.substr(1,2);
//			iRet = msbDecode.rfind(strSndPostfix+strSndPostfix);
//			if(0 < iRet){
//				LOGE("checkPostfix(), detect "+strSndPostfix+strSndPostfix+" +++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
//			}else{
//				iRet = msbDecode.rfind(strFstPostfix+strFstPostfix);
//				if(0 < iRet){
//					LOGE("checkPostfix(), detect "+strFstPostfix+strFstPostfix+" +++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
//				}
//			}
	}else{
		if(iRet < SoundPair_Config::MIN_PAIRING_MSG_LEN){
			LOGI("checkPostfix(), detect SoundPair_Config::POSTFIX_DECODE , but pos(%d) < SoundPair_Config::MIN_PAIRING_MSG_LEN (%d)\n", iRet , SoundPair_Config::MIN_PAIRING_MSG_LEN);
			iRet = -1;
		}else{
			LOGE("checkPostfix(), detect SoundPair_Config::POSTFIX_DECODE , and pos(%d) < SoundPair_Config::MIN_PAIRING_MSG_LEN (%d), +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", iRet , SoundPair_Config::MIN_PAIRING_MSG_LEN);
			if(mIFreqAnalyzeResultCBListener){
				mIFreqAnalyzeResultCBListener->onDetectPostFix();
			}
		}
	}
	if(DEBUG_MODE){
		LOGD("checkPostfix()--\n");
	}
	return iRet;
}

void FreqAnalyzer::checkFirstCharOfPrefix(Ref<CodeRecord> rec){
	if(DEBUG_MODE){
		LOGE("checkFirstCharOfPrefix()+\n");
	}
	mSessionBeginTs = rec->lStartTs;
	if(DEBUG_MODE){
		LOGE("checkFirstCharOfPrefix(), mSessionBeginTs=%lld\n", mSessionBeginTs);
	}
	mSessionBeginBufIdx = rec->mlstFreqRec.front()->miBufIndex;
	//mSessionOffset = segmentCheckOnFirst(false);

	if(DEBUG_MODE){
		LOGE("checkFirstCharOfPrefix(), sSessionBeginTs:%lld, sSessionBeginBufIdx:%d,\n sesFreqList:XXX\n", mSessionBeginTs, mSessionBeginBufIdx/*, rec->mlstFreqRec*/);
	}
	//check next char
	if( 0 < mFreqRecordList.size()){
		Ref<FreqRecord> fr = mFreqRecordList.front();
		//FreqRecord frBeginOfFirstChar = sesFreqList.front();
		Ref<FreqRecord> frEndOfFirstChar = rec->mlstFreqRec.back();

		//if only 2 consecutive digits and next is second tone
		if((SoundPair_Config::TONE_FRAME_COUNT-1) == rec->mlstFreqRec.size() && (frEndOfFirstChar->mlTs + SoundPair_Config::FRAME_TS) == fr->mlTs){
			if((0==SoundPair_Config::PREFIX_DECODE_C2.compare(fr->mstrCode))){
				LOGE("checkFirstCharOfPrefix(), need to shift one digit backward\n");
				mSessionBeginTs -= SoundPair_Config::FRAME_TS;
				rec->mlstFreqRec.insert(rec->mlstFreqRec.begin(), Ref<FreqRecord>(new FreqRecord(mSessionBeginTs, -1.0f, SoundPair_Config::MISSING_CHAR, -1, NULL)));
			}else{
				LOGE("checkFirstCharOfPrefix(), need to fill CodeRecord\n");
				rec->mlstFreqRec.push_back(mFreqRecordList.front());
				mFreqRecordList.erase(mFreqRecordList.begin());
				rec->lEndTs += SoundPair_Config::FRAME_TS;
			}
		}
		else if((frEndOfFirstChar->mlTs + SoundPair_Config::FRAME_TS) == fr->mlTs && (0==SoundPair_Config::PREFIX_DECODE_C1.compare(fr->mstrCode))){
			LOGE("checkFirstCharOfPrefix(), shift one frame_ts\n");
			if(rec->mlstFreqRec.size() == SoundPair_Config::TONE_FRAME_COUNT){
				//LOGE("checkFirstCharOfPrefix(), shift one frame_ts 0\n");
				rec->mlstFreqRec.erase(rec->mlstFreqRec.begin());
			}
			rec->mlstFreqRec.push_back(fr);
			mFreqRecordList.erase(mFreqRecordList.begin());
			mSessionBeginTs = rec->lStartTs += SoundPair_Config::FRAME_TS;
			rec->lEndTs += SoundPair_Config::FRAME_TS;
			mSessionBeginBufIdx = (++mSessionBeginBufIdx)%AudioBufferMgr::MAX_QUEUE_SIZE;
		}

		LOGE("checkFirstCharOfPrefix(), rec:%s\n", rec->toString().c_str());
		mCodeRecordList.push_back(rec);
	}
}

void FreqAnalyzer::setSessionOffset(int iOffset){
	mSessionOffset = iOffset;
}

int FreqAnalyzer::getSessionOffset(){
	return mSessionOffset;
}

void FreqAnalyzer::amplitudeTest(int iBufIndex){
//	LOGE("amplitudeTest(), on iBufIndex:%d-------------------------------------\n", (iBufIndex-1));
//	AudioBufferMgr::getInstance()->getBufByIndex(iBufIndex-1, 0, bufSegment);
//	for(int idx =0; idx< SoundPair_Config::FRAME_SIZE_REC;idx++)
//		LOGE("amplitudeTest(), bufSegment[:"+idx+"]="+std::abs(bufSegment[idx]/32767.0f));
//
//	LOGE("amplitudeTest(), on iBufIndex:"+iBufIndex+"-------------------------------------\n");
//	AudioBufferMgr::getInstance()->getBufByIndex(iBufIndex, 0, bufSegment);
//	for(int idx =0; idx< SoundPair_Config::FRAME_SIZE_REC;idx++)
//		LOGE("amplitudeTest(), bufSegment[:"+idx+"]="+std::abs(bufSegment[idx]/32767.0f));
//
//	LOGE("amplitudeTest(), on iBufIndex:"+(iBufIndex+1)+"-------------------------------------\n");
//	AudioBufferMgr::getInstance()->getBufByIndex(iBufIndex+1, 0, bufSegment);
//	for(int idx =0; idx< SoundPair_Config::FRAME_SIZE_REC;idx++)
//		LOGE("amplitudeTest(), bufSegment[:"+idx+"]="+std::abs(bufSegment[idx]/32767.0f));
}

int FreqAnalyzer::segmentCheckOnFirst(bool bForcePerform){
	int iRet = 0;
	//LOGE("segmentCheckOnFirst(), bForcePerform:%d-------------------------------------\n", bForcePerform);
	if((SoundPair_Config::SEGMENT_OFFSET_FEATURE && mbNeedToAutoCorrection) || bForcePerform){
		//msec_t lTsBegin = time_ms();
		Ref<FreqRange> fr = SoundPair_Config::findFreqRange(SoundPair_Config::PREFIX_DECODE_C1);
		int iBufIdxTOCheck = mSessionBeginBufIdx;

		const int MAX_SILENCE_SMAPLE = 13;
		const short sSilence = (short) (32767.0f * SoundPair_Config::SILENCE_CRITERIA);
		bool bCapture = false;
		int idxFirstDetect = -1, idxLastDetect = -1, iSilenceSample = 0;

		if(DEBUG_MODE){
			LOGE("segmentCheckOnFirst(), on iBufIndex:%d-------------------------------------\n", iBufIdxTOCheck);
		}
		AudioBufferMgr::getInstance()->getBufByIndex(iBufIdxTOCheck, 0, bufSegment);
		for(int idx =0; idx< SoundPair_Config::FRAME_SIZE_REC;idx++){
			if(sSilence < std::abs(bufSegment[idx])){
				if(-1 == idxFirstDetect){
					idxFirstDetect = idx;
					LOGE("segmentCheckOnFirst(), idxFirstDetect:%d\n", idxFirstDetect);
				}else{
					idxLastDetect = idx;
					if((idxLastDetect - idxFirstDetect) > SoundPair_Config::SILENCE_DETECTION_SAMPLE){
						LOGE("segmentCheckOnFirst(), idxLastDetect:%d\n",idxLastDetect);
						break;
					}
				}
				iSilenceSample = 0;
			}else{
				iSilenceSample++;
				if(MAX_SILENCE_SMAPLE < iSilenceSample && -1 < idxFirstDetect && -1 == idxLastDetect){
					LOGE("segmentCheckOnFirst(), reset idxFirstDetect\n");
					idxFirstDetect = -1;
					iSilenceSample = 0;
				}
			}
			//LOGI("segmentCheckOnFirst(), bufSegment[:"+idx+"]="+std::abs(bufSegment[idx]/32767.0f));
		}

		if(-1 < idxFirstDetect && (SoundPair_Config::FRAME_SIZE_REC - idxFirstDetect) < SoundPair_Config::SILENCE_DETECTION_SAMPLE){
			if(DEBUG_MODE){
				LOGE("segmentCheckOnFirst(), continue to check iBufIndex:%d-------------------------------------\n", (iBufIdxTOCheck+1));
			}
			AudioBufferMgr::getInstance()->getBufByIndex(iBufIdxTOCheck+1, 0, bufSegment);

			for(int idx = 0; idx < SoundPair_Config::FRAME_SIZE_REC; idx++){
				if(sSilence < std::abs(bufSegment[idx])){
					if(-1 == idxFirstDetect){
						idxFirstDetect = idx + SoundPair_Config::FRAME_SIZE_REC;
						LOGE("segmentCheckOnFirst(), idxFirstDetect:%d\n",idxFirstDetect);
					}else{
						idxLastDetect = idx + SoundPair_Config::FRAME_SIZE_REC;
						if((idxLastDetect - idxFirstDetect) > SoundPair_Config::SILENCE_DETECTION_SAMPLE){
							LOGE("segmentCheckOnFirst(), idxLastDetect:%d\n",idxLastDetect);
							break;
						}
					}
					iSilenceSample = 0;
				}else{
					iSilenceSample++;
					if(MAX_SILENCE_SMAPLE < iSilenceSample && -1 < idxFirstDetect && -1 == idxLastDetect){
						LOGE("segmentCheckOnFirst(), reset idxFirstDetect\n");
						idxFirstDetect = -1;
						iSilenceSample = 0;
					}
				}
				//LOGI("segmentCheckOnFirst(), bufSegment[:"+idx+"]="+std::abs(bufSegment[idx]/32767.0f));
			}
		}

		if((idxLastDetect - idxFirstDetect) > SoundPair_Config::SILENCE_DETECTION_SAMPLE){
			bCapture = true;
			iRet = idxFirstDetect;

			AudioBufferMgr::getInstance()->getBufByIndex(iBufIdxTOCheck, idxFirstDetect, bufSegment);
			if(mIFreqAnalyzeResultCBListener){
				mIFreqAnalyzeResultCBListener->onBufCheck(bufSegment, 0, false, NULL);
				float freq = mIFreqAnalyzeResultCBListener->onBufCheck(bufSegment, 0, false, NULL);
				LOGE("segmentCheckOnFirst(), iBufIdxTOCheck:%d, freq:%f, iRet:%d\n",iBufIdxTOCheck, freq, iRet);
			}
		}

		if(bCapture == false){
			if(DEBUG_MODE){
				LOGE("segmentCheckOnFirst(), on iBufIndex:%d-------------------------------------\n", (iBufIdxTOCheck+1));
			}
			AudioBufferMgr::getInstance()->getBufByIndex(iBufIdxTOCheck+1, 0, bufSegment);
			idxFirstDetect = -1;
			idxLastDetect = -1;
			iSilenceSample = 0;

			for(int idx =0; idx< SoundPair_Config::FRAME_SIZE_REC;idx++){
				if(sSilence < std::abs(bufSegment[idx])){
					if(-1 == idxFirstDetect){
						idxFirstDetect = idx;
						LOGE("segmentCheckOnFirst(), idxFirstDetect:%d\n",idxFirstDetect);
					}else{
						idxLastDetect = idx;
						if((idxLastDetect - idxFirstDetect) > SoundPair_Config::SILENCE_DETECTION_SAMPLE){
							LOGE("segmentCheckOnFirst(), idxLastDetect:%d\n",idxLastDetect);
							break;
						}
					}
					iSilenceSample = 0;
				}else{
					iSilenceSample++;
					if(MAX_SILENCE_SMAPLE < iSilenceSample && -1 < idxFirstDetect && -1 == idxLastDetect){
						LOGE("segmentCheckOnFirst(), reset idxFirstDetect\n");
						idxFirstDetect = -1;
						iSilenceSample = 0;
					}
				}

				//LOGI("segmentCheckOnFirst(), bufSegment[:"+idx+"]="+std::abs(bufSegment[idx]/32767.0f));
			}

			if((idxLastDetect - idxFirstDetect) > SoundPair_Config::SILENCE_DETECTION_SAMPLE){
				bCapture = true;
				iRet = idxFirstDetect;
			}
		}

		LOGE("segmentCheckOnFirst(), iBufIdxTOCheck:%d, iRet:%d, bCapture:%d\n", iBufIdxTOCheck,iRet,bCapture);
//			if(iRet > (SoundPair_Config::FRAME_SIZE_REC - SoundPair_Config::SILENCE_DETECTION_SAMPLE)){
//				LOGE("segmentCheckOnFirst(), iBufIdxTOCheck:"+iBufIdxTOCheck+\n", iRet:"+iRet+\n", bCapture:"+bCapture);
//			}

//		LOGD("segmentCheckOnFirst(), takes %lld ms at %d\n", (time_ms() - lTsBegin), mSessionBeginBufIdx);
	}
	return iRet;
}


int FreqAnalyzer::segmentCheck(bool bForcePerform){
	int iRet = 0;
	if(SoundPair_Config::SEGMENT_OFFSET_FEATURE || bForcePerform){
//		msec_t lTsBegin = time_ms();
		Ref<FreqRange> firstFR = SoundPair_Config::findFreqRange(SoundPair_Config::PREFIX_DECODE_C1);
		if(DEBUG_MODE){
			LOGE("segmentCheck(), mCodeRecordList.size():%d\n", mCodeRecordList.size());
		}
		if(0 < mCodeRecordList.size()){
			Ref<CodeRecord> rec = mCodeRecordList.front();

			//LOGE("segmentCheck(), rec:%s\n", (rec)?rec->toString().c_str():"null");
			vector<Ref<FreqRecord> > fRecLst = rec->mlstFreqRec;
			int iBufIdxTOCheck = mSessionBeginBufIdx;
			int iLen = fRecLst.size();
			for(int i = 1; i < iLen; i++){
				if((0==fRecLst.at(i)->mstrCode.compare(firstFR->mstrCode))){
					iBufIdxTOCheck++;
				}
			}

			bool bWithinSecondRange = false;
			//Check if there is first code within second range
			vector<Ref<FreqRecord> > fRecLst2 = mCodeRecordList[1]->mlstFreqRec;
			int iLen2 = fRecLst2.size();
			for(int i = 0; i < iLen2; i++){
				if((0==fRecLst2.at(i)->mstrCode.compare(firstFR->mstrCode))){
					LOGI("segmentCheck(), there is first code within second range\n");
					iBufIdxTOCheck++;
					bWithinSecondRange =true;
					break;
				}
			}

			//workaround on cam
			iBufIdxTOCheck= (0 == iBufIdxTOCheck)?(AudioBufferMgr::MAX_QUEUE_SIZE-1):(iBufIdxTOCheck-1);

			int iPart = SoundPair_Config::FRAME_SIZE_REC/10;
			for(int i = 0; i <= 10; i++){
				int iOffset = ( (i*iPart) > SoundPair_Config::FRAME_SIZE_REC-1)?SoundPair_Config::FRAME_SIZE_REC-1:i*iPart ;// fmin((int) (i*iPart),SoundPair_Config::FRAME_SIZE_REC-1);

				AudioBufferMgr::getInstance()->getBufByIndex(iBufIdxTOCheck, iOffset, bufSegment);
				//LOGE("segmentCheck(), bufSegment[iOffset]:%d", bufSegment[std::abs(iOffset)]);
				if(mIFreqAnalyzeResultCBListener){
					float freq = mIFreqAnalyzeResultCBListener->onBufCheck(bufSegment, 0, 0 == i, NULL);
					freq = mIFreqAnalyzeResultCBListener->onBufCheck(bufSegment, 0, 0, NULL);
					if(DEBUG_MODE){
						LOGE("segmentCheck(), iBufIdxTOCheck:%d, freq:%f, iOffset:%d\n",iBufIdxTOCheck,freq, iOffset);
					}
					if(0.0 >= freq || false == firstFR->withinFreqRange(freq)){
						iRet= iOffset - SoundPair_Config::SEG_SES_OFFSET*iPart;//Math.max(iOffset - 3*iPart, 0)+(bWithinSecondRange?SoundPair_Config::FRAME_SIZE_REC:0);//fmin(iOffset + iPart, 0);
						LOGE("segmentCheck(), iBufIdxTOCheck:%d, freq:%f, iOffset:%d, iRet:[%d]\n",iBufIdxTOCheck, freq, iOffset, iRet);
						break;
					}
				}
			}
//			LOGD("segmentCheck(), takes %lld ms at %d\n", (time_ms() - lTsBegin), mSessionBeginBufIdx);
		}else{
			LOGE("segmentCheck(), empty mCodeRecordList\n");
		}
	}

	return iRet;
}

string FreqAnalyzer::replaceInvalidChar(string strDecode){
	const int iLenPrefix = SoundPair_Config::PREFIX_DECODE.length();
	//const int POS_FST_DIVIDER = 12;
	stringstream strRet;

	int iLen = strDecode.length();
	int iMaxIndex = SoundPair_Config::getDivisionByFFTYPE();
	const string strDefReplaced = SoundPair_Config::sCodeTable[0];

//	//there is redundant digit check it from end
//	if(0 != strDecode.length()%SoundPair_Config::getMultiplyByFFTYPE()){
//		for(int i = iLen -1; i >=0; i--){
//			string strCode = strDecode.substr(i, 1);
//			if(iMaxIndex <= SoundPair_Config::findIdxFromCodeTable(strCode)){
//				strDecode = ((i > 0)?strDecode.substr(0, i):"")+strDefReplaced+((i+1 < iLen)?strDecode.substr(i+1):"");
//				if(i+iLenPrefix < mCodeRecordList.size()){
//					mCodeRecordList.erase(mCodeRecordList.begin()+i+iLenPrefix);
//				}
//
//				LOGE("replaceInvalidChar(), remove last illegal char %s at %d, strDecode = %s\n", strCode.c_str(), i, strDecode.c_str());
//			}
//		}
//	}

//	if(iLen <= POS_FST_DIVIDER){
//		LOGE("replaceInvalidChar(), iLen is %d, return\n");
//		return strRet.str();
//	}

	//Remove 2 dividers
	int iLenNoDivider = iLen - 2;
	int iPosSndDividerShouldBe = (iLenNoDivider*2/3)-12;//minus (token len + purpose + sec + reserved)

	LOGE("replaceInvalidChar(), iLenNoDivider is %d, iPosSndDividerShouldBe = %d\n", iLenNoDivider, iPosSndDividerShouldBe);

	int iPosFstDivider = strDecode.find(SoundPair_Config::PAIRING_DIVIDER);
	int iPosSndDivider = -1;
	LOGE("replaceInvalidChar(), iPosFstDivider is at %d, strDecode = %s\n", iPosFstDivider, strDecode.c_str());
	if(0 <= iPosFstDivider){
		//iPosSndDivider = strDecode.find(SoundPair_Config::PAIRING_DIVIDER, iPosFstDivider+1);
		iPosSndDivider = strDecode.rfind(SoundPair_Config::PAIRING_DIVIDER);

//		int iPosFstDividerDelta = iPosFstDivider-POS_FST_DIVIDER;
//
//		//if( 2 >= abs(iPosFstDividerDelta)){
//			if(0 < iPosFstDividerDelta && iPosFstDividerDelta < iLen){
//				strDecode = strDecode.substr(iPosFstDividerDelta);
//			}else if(0 > iPosFstDividerDelta){
//				int iDumbToAdd=abs(iPosFstDividerDelta);
//				int i = 0;
//				for(; i < iDumbToAdd ;i++){
//					strDecode = strDefReplaced+strDecode;
//				}
//			}
//
//			//iLen +=iPosFstDividerDelta;
//			//iLenNoDivider+=iPosFstDividerDelta;
//			iPosSndDividerShouldBe = (iLenNoDivider*2/3)-10;//minus token len
//			iPosFstDivider-=iPosFstDividerDelta;
//
//			iPosSndDivider = strDecode.find(SoundPair_Config::PAIRING_DIVIDER, iPosFstDivider+1);
			LOGE("replaceInvalidChar(), iPosSndDivider is at %d, strDecode = %s\n", iPosSndDivider, strDecode.c_str());
			if(0 < iPosSndDivider && iPosSndDivider+1 < iLen){
				strDecode = strDecode.substr(0, iPosSndDivider)+strDecode.substr(iPosSndDivider+1);
			}else if(0 < iPosSndDividerShouldBe && iPosSndDividerShouldBe+1 < iLen){
				strDecode = strDecode.substr(0, iPosSndDividerShouldBe)+strDecode.substr(iPosSndDividerShouldBe+1);
			}

			if(0 < iPosFstDivider && iPosFstDivider+1 < strDecode.length()){
				strDecode = strDecode.substr(0, iPosFstDivider)+strDecode.substr(iPosFstDivider+1);
			}else{
				LOGE("replaceInvalidChar(), invalid iPosFstDivider %d\n", iPosFstDivider);
			}
		//}
//		else{
//			LOGE("replaceInvalidChar(), iPosFstDividerDelta is too large (%d)\n", iPosFstDividerDelta);
//			if(0 < iPosFstDividerDelta && iPosFstDividerDelta+1 < strDecode.length()){
//				strDecode = strDecode.substr(0, iPosFstDividerDelta)+strDecode.substr(iPosFstDividerDelta+1);
//				if(0 < POS_FST_DIVIDER && POS_FST_DIVIDER+1 < strDecode.length()){
//					strDecode = strDecode.substr(0, POS_FST_DIVIDER)+strDecode.substr(POS_FST_DIVIDER+1);
//				}
//			}else if(0 < iPosSndDividerShouldBe && iPosSndDividerShouldBe+1 < strDecode.length()){
//				strDecode = strDecode.substr(0, iPosSndDividerShouldBe)+strDecode.substr(iPosSndDividerShouldBe+1);
//				if(0 < POS_FST_DIVIDER && POS_FST_DIVIDER+1 < strDecode.length()){
//					strDecode = strDecode.substr(0, POS_FST_DIVIDER)+strDecode.substr(POS_FST_DIVIDER+1);
//				}
//			}
//		}
	}else{
		LOGE("replaceInvalidChar(), can not fnd divider\n");
		if(0 < iPosSndDividerShouldBe && iPosSndDividerShouldBe+1 < strDecode.length()){
			strDecode = strDecode.substr(0, iPosSndDividerShouldBe)+strDecode.substr(iPosSndDividerShouldBe+1);
		}
//		if(0 < POS_FST_DIVIDER && POS_FST_DIVIDER+1 < strDecode.length()){
//			strDecode = strDecode.substr(0, POS_FST_DIVIDER)+strDecode.substr(POS_FST_DIVIDER+1);
//		}
	}

	iLen = strDecode.length();

	for(int i = iLen -1; i >=0; i--){
		string strCode = strDecode.substr(i, 1);
		if(iMaxIndex <= SoundPair_Config::findIdxFromCodeTable(strCode)){
			strDecode = ((i > 0)?strDecode.substr(0, i):"")+strDefReplaced+((i+1 < iLen)?strDecode.substr(i+1):"");
			if(i+iLenPrefix < mCodeRecordList.size()){
				mCodeRecordList.erase(mCodeRecordList.begin()+i+iLenPrefix);
			}

			LOGE("replaceInvalidChar(), remove last illegal char %s at %d, strDecode = %s\n", strCode.c_str(), i, strDecode.c_str());
		}
	}

	iLen = strDecode.length();

	for(int i = 0; i< iLen;i++){
		string strCode = strDecode.substr(i, 1);
		if(iMaxIndex > SoundPair_Config::findIdxFromCodeTable(strCode)){
			strRet<<strCode;
		}else{
			string strReplace = strDefReplaced;
			if(i+iLenPrefix < mCodeRecordList.size()){
				Ref<CodeRecord> cr = mCodeRecordList.at(i+iLenPrefix);
				if(NULL != cr){
					vector<Ref<FreqRecord> > lst = cr->mlstFreqRec;
					int iLenLst = lst.size();
					for(int idx = iLenLst - 1; idx >=0; idx--){
						if(iMaxIndex > SoundPair_Config::findIdxFromCodeTable(lst.at(idx)->mstrCode)){
							strReplace = lst.at(idx)->mstrCode;
						}
					}
				}
				LOGI("replaceInvalidChar(), replace [%d] char %s to %s\n",i,strCode.c_str(),strReplace.c_str());
				mCodeRecordList.at(i+iLenPrefix)->strCdoe = strReplace;
			}
			strRet<<strReplace;
		}
	}
	LOGE("replaceInvalidChar(), strDecode = %s, \n                         strRet = %s\n", strDecode.c_str(), strRet.str().c_str());
	return strRet.str();
}

bool FreqAnalyzer::checkEndPoint(){
	//detect first char first
	int iStartPos = 0;
	int iIndex = -1;
	string strDecodeCheck(msbDecode.str());
	do{
		if(0 <= iIndex){
			iStartPos = iIndex+1;
			LOGE("checkEndPoint(), retry iStartPos:%d\n", iStartPos);
		}
		iIndex = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C1, iStartPos);
		if(0 > iIndex){
			LOGE("checkEndPoint(), can not detect first char of SoundPair_Config::POSTFIX_DECODE\n");
			//detect second char first
			iIndex = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C2, (0==iStartPos)?1:iStartPos+1);
			if(0 <= iIndex){
				iIndex -=1;
				LOGE("checkEndPoint(), detect second char of SoundPair_Config::POSTFIX_DECODE, iIndex:%d, +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", iIndex);
			}
		}else{
			LOGE("checkEndPoint(), detect first char of SoundPair_Config::POSTFIX_DECODE, iIndex:%d, +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", iIndex);
			int iDxSndC = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C2, (0==iStartPos)?1:iStartPos+1);
			if(0 <= iDxSndC && iDxSndC < iIndex){
				iIndex = iDxSndC - 1;
				LOGE("checkEndPoint(), detect second char is prior to first char, iDxSndC:%d, +++++++++++++++++++++++++++++++++++++++++++++++++++++\n", iDxSndC);
			}
		}

	}while(0 <= iIndex && iIndex < SoundPair_Config::MIN_PAIRING_MSG_LEN);


	/*if(iIndex < SoundPair_Config::MIN_PAIRING_MSG_LEN){
		LOGI("checkEndPoint(), detect one of SoundPair_Config::POSTFIX_DECODE , but iIndex(%d) < SoundPair_Config::MIN_PAIRING_MSG_LEN (%d)\n", iIndex , SoundPair_Config::MIN_PAIRING_MSG_LEN);
	}else*/
	{
		if(-1 < iIndex){
			mbStartAppend = false;
			int iShift = checkFrameBySessionAndAutoCorrection();

			if(0 != iShift){
				LOGE("checkEndPoint(), redetect index, iShift = %d\n",iShift);
				//We may get postfix after shift
				int iNewIndex = -1;
				do{
					iNewIndex = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE, iNewIndex+1);
				}while(0 <= iNewIndex && iNewIndex < SoundPair_Config::MIN_PAIRING_MSG_LEN);

				LOGE("checkEndPoint(), redetect index, iShift = %d, iNewIndex=%d\n", iShift, iNewIndex);

				int iDxFstC = -1;
				do{
					iDxFstC = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C1, iDxFstC+1);
				}while(0 <= iDxFstC && iDxFstC < SoundPair_Config::MIN_PAIRING_MSG_LEN);

				int iDxSndC = -1;
				do{
					iDxSndC = strDecodeCheck.find(SoundPair_Config::POSTFIX_DECODE_C2, iDxSndC+1);
				}while(0 <= iDxSndC && iDxSndC < SoundPair_Config::MIN_PAIRING_MSG_LEN);

				if(0 <= iNewIndex){
					if(0 < iDxFstC && iDxFstC < iNewIndex && iDxFstC >= SoundPair_Config::MIN_PAIRING_MSG_LEN && (0 == iNewIndex%2)){//special case 1: ...H...HI, && (0 == iNewIndex%2) due to H should be at odd pos
						LOGE("checkEndPoint(), special case 1, iDxFstC=%d\n",iDxFstC);
						iNewIndex = iDxFstC;
					}else if(0 < iDxSndC && iDxSndC < iNewIndex+1 && iDxSndC -1 >= SoundPair_Config::MIN_PAIRING_MSG_LEN && (0 == iNewIndex%2)){//special case 2: ...I...HI
						LOGE("checkEndPoint(), special case 2, iDxSndC=%d\n",iDxSndC);
						iNewIndex = iDxSndC - 1;
					}
				}else{
					if(-1 == iDxFstC){
						LOGE("checkEndPoint(), can't detect first char of POSTFIX_DECODE, redetect index by second char \n");
						if(-1 == iDxSndC){
							LOGE("checkEndPoint(), can't detect second char of POSTFIX_DECODE, redetect index by shift\n");
							//iNewIndex = (iIndex-iShift);
						}else{
							iNewIndex = iDxSndC - 1;
						}
					}else{
						iNewIndex = iDxFstC;
					}
				}

				if(iNewIndex < SoundPair_Config::MIN_PAIRING_MSG_LEN){
					LOGI("checkEndPoint(), redetect one of SoundPair_Config::POSTFIX_DECODE , but iNewIndex(%d) < SoundPair_Config::MIN_PAIRING_MSG_LEN (%d)\n", iIndex , SoundPair_Config::MIN_PAIRING_MSG_LEN);
					iIndex = -1;
				}else{
					if(-1 < iNewIndex && iNewIndex != iIndex){
						LOGE("checkEndPoint(), change index from %d to %d\n",iIndex,iNewIndex);
						iIndex = iNewIndex;
					}
				}
			}

			if(0 <= iIndex && iIndex < strDecodeCheck.length()){
				string strDecodeChk = strDecodeCheck.substr(0, iIndex);
				checkResult(strDecodeChk, optimizeDecodeString(iIndex, strDecodeCheck));
				return true;
			}else{
				LOGE("checkEndPoint(), invalid iIndex:%d, strDecodeCheck.length():%d \n", iIndex, strDecodeCheck.length());
			}
		}else{
			LOGE("checkEndPoint(), can not detect first char of POSTFIX_DECODE +++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
		}
	}
	return false;
}

void FreqAnalyzer::checkResult(string strDecode, string strDecodeUnmark){
//		strDecode = replaceInvalidChar(strDecode);
//		string strDecodeUnmark = removeDividerAndUnmark(strDecode);

	//sIFreqAnalyzeResultCBListener.onSetResult();

	//string strDecodeUnmark = strDecode;//replaceInvalidChar(strDecode);
	msbDecode.str("");
	msbDecode.clear();
	if(CAM_TIME_TRACE && mCodeRecordList.size() > 0){
		//sCodeRecordList.remove(sCodeRecordList.size()-1);
//			if(-1 < ssbDecode.rfind(POSTFIX_DECODE)){
//				sCodeRecordList.remove(sCodeRecordList.size()-1);
//			}
		int iSize = mCodeRecordList.size();
		for(int idx = 0;idx < iSize;idx++){
			if(DEBUG_MODE){
				LOGI("checkResult(),===========>>>>>>>>>>>>>> rec= %s\n", mCodeRecordList[idx]->toString().c_str());
			}
		}
	}

	string strRet = decodeRSEC(strDecodeUnmark);

	//int iLenByTime = getMsgLengthByTime();
//		string strRet = decodeRSEC((SoundPair_Config::getMultiplyByFFTYPE() == 1)?strDecodeUnmark
//				                                                               :((strDecodeUnmark.length()%SoundPair_Config::getMultiplyByFFTYPE() == 0)?strDecodeUnmark
//				                                                            		                                                                  :correctErrByDelta(1)));//Need to modify!!!!
//		if(strRet.startsWith("error")){
//			int iTrial = 0;
//			while(++iTrial <= 3 && strRet.startsWith("error")){
//				strRet = decodeRSEC(correctErrByDelta(iTrial));
//			}
//
//			iTrial = 0;
//			while(++iTrial <= 3 && strRet.startsWith("error")){
//				strRet = decodeRSEC(correctErrByDelta(-iTrial));
//			}
//		}

	/*if(strRet.startsWith("error") && canPerformAutoCorrection()){
		LOGI("checkResult(), strRet:"+strRet+\n", mstrCodeTrace:"+mstrCodeTrace);
		performAutoCorrection();
	}else*/{
		if(mIFreqAnalyzeResultCBListener)
			mIFreqAnalyzeResultCBListener->onSetResult(strRet, strDecode, strDecodeUnmark, !mbNeedToAutoCorrection, mprevMatchRet);
		if(false == canPerformAutoCorrection()){
			//set mIFreqAnalyzeResultCBListener as NULL, if it's one self analyzer
			mIFreqAnalyzeResultCBListener = NULL;
		}
	}
}

int FreqAnalyzer::checkFrameBySessionAndAutoCorrection(){
	int iRet = 0;
	int iNumOfBias = getNumOfBias(mCodeRecordList);
	if(0 == iNumOfBias){
		LOGI("checkFrameBySessionAndAutoCorrection(), iNumOfBias = 0, no need to adjust\n");
	}else{
		vector<Ref<CodeRecord> > lstCodeRecordBackward = getLstCodeRecordByOffset(mCodeRecordList, 1);
		int iNumOfBiasShiftBackward = getNumOfBias(lstCodeRecordBackward);

		vector<Ref<CodeRecord> > lstCodeRecordForward = getLstCodeRecordByOffset(mCodeRecordList, -1);
		int iNumOfBiasShiftForward = getNumOfBias(lstCodeRecordForward);

		LOGE("checkFrameBySessionAndAutoCorrection(), iNumOfBias:%d, iNumOfBiasShiftBackward:%d, iNumOfBiasShiftForward:%d\n", iNumOfBias,iNumOfBiasShiftBackward,iNumOfBiasShiftForward);

		if(iNumOfBiasShiftBackward <= iNumOfBiasShiftForward){
			if(iNumOfBiasShiftBackward< iNumOfBias){
				if(DEBUG_MODE){
					LOGE("checkFrameBySessionAndAutoCorrection(), pick lstCodeRecordBackward **********************************************\n");
				}
				mCodeRecordList.clear();
				mCodeRecordList.insert(mCodeRecordList.begin(),lstCodeRecordBackward.begin(), lstCodeRecordBackward.end());
				regenDecode();
				iRet = 1;
			}else{
				LOGI("checkFrameBySessionAndAutoCorrection(), keep origianl\n");
			}
		}else{
			if(iNumOfBiasShiftForward< iNumOfBias){
				if(DEBUG_MODE){
					LOGE("checkFrameBySessionAndAutoCorrection(), pick lstCodeRecordForward ***************************************************\n");
				}
				mCodeRecordList.clear();
				mCodeRecordList.insert(mCodeRecordList.begin(),lstCodeRecordForward.begin(), lstCodeRecordForward.end());
				regenDecode();
				iRet = -1;
			}else{
				LOGI("checkFrameBySessionAndAutoCorrection(), keep origianl\n");
			}
		}
	}
	return iRet;
}

void FreqAnalyzer::regenDecode(){
	msbDecode.str("");
	msbDecode.clear();
	int iSize = mCodeRecordList.size();
	for(int idx = SoundPair_Config::PREFIX_DECODE.length(); idx < iSize;idx++){
		msbDecode<<mCodeRecordList.at(idx)->strCdoe;
	}
}

vector<Ref<CodeRecord> > FreqAnalyzer::getLstCodeRecordByOffset(vector<Ref<CodeRecord> > lstCodeRecord, int iOffset){
	vector<Ref<CodeRecord> > retLst;
	if(DEBUG_MODE){
		LOGD("getLstCodeRecordByOffset(), iOffset:%d\n", iOffset);
	}
	int iSize = lstCodeRecord.size();
	if(DEBUG_MODE){
		LOGD("getLstCodeRecordByOffset(),iSize:%d\n", iSize);
	}
	for(int idx = 0; idx + 1 < iSize;idx++){
		int iCurSize = retLst.size();
		LOGD("getLstCodeRecordByOffset(), idx:%d, iCurSize:%d, iSize:%d\n", idx, iCurSize, iSize);
		if(0 < iOffset){
			retLst.push_back(CodeRecord::combineNewCodeRecord(lstCodeRecord.at(idx), lstCodeRecord.at(idx+1), iOffset, getToneIdxByCode((0 < iCurSize)?retLst.at(iCurSize-1)->strCdoe:"")));
		}else if(0 > iOffset){
			retLst.push_back(CodeRecord::combineNewCodeRecord(lstCodeRecord.at(idx), lstCodeRecord.at(idx+1), iOffset, getToneIdxByCode((0 < iCurSize)?retLst.at(iCurSize-1)->strCdoe:"")));
		}
	}

	LOGD("getLstCodeRecordByOffset(), iOffset:%d --- 1\n\n");
	if(0 < iOffset){
		retLst.push_back(CodeRecord::combineNewCodeRecord(lstCodeRecord.at(iSize-1), Ref<CodeRecord>(NULL), iOffset, -1));
	}else if(0 > iOffset){
		retLst.insert(retLst.begin(), CodeRecord::combineNewCodeRecord(Ref<CodeRecord>(NULL), lstCodeRecord.at(0), iOffset, -1));
	}

	LOGD("getLstCodeRecordByOffset(), iOffset:%d\n",iOffset);

	return retLst;
}

int FreqAnalyzer::getNumOfBias(vector<Ref<CodeRecord> > lstCodeRecord){
	int iRet = 0;
	int iSize = lstCodeRecord.size();
	for(int idx = SoundPair_Config::PREFIX_DECODE.length(); idx < iSize;idx++){
		if(false == lstCodeRecord.at(idx)->isSameCode()){
			iRet++;
		}
	}

	return iRet;
}

bool FreqAnalyzer::canPerformAutoCorrection(){
	return mbNeedToAutoCorrection;
}

bool FreqAnalyzer::performAutoCorrection(MatchRetSet* prevMatchRet){
	if(mbNeedToAutoCorrection){
		autoCorrection(prevMatchRet);
		return true;
	}
	return false;
}

void FreqAnalyzer::autoCorrection(MatchRetSet* prevMatchRet){
	LOGE("autoCorrection(),++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
	int iSize = mCodeRecordList.size();
	if(0 < iSize){
		if(NULL == selfFreqAnalyzer)
			selfFreqAnalyzer = new FreqAnalyzer(false);

		selfFreqAnalyzer->mprevMatchRet = prevMatchRet;
		selfFreqAnalyzer->setIFreqAnalyzeResultCB(mIFreqAnalyzeResultCBListener);
		int iOffset = mSessionOffsetForAmp;//segmentCheckOnFirst(true);//segmentCheck(true);
		selfFreqAnalyzer->setSessionOffset(iOffset);

		bool bFirstTime = true;
		struct timespec yieldtime = {0};
		yieldtime.tv_sec  = 0;
		yieldtime.tv_nsec = 10000000;//10 ms

		for(int idx = 0; idx < iSize ;idx++){
			Ref<CodeRecord> rec = mCodeRecordList[idx];
			if(NULL != rec){
				vector<Ref<FreqRecord> > frRecLst = rec->mlstFreqRec;
				int iSizeFrRec = frRecLst.size();
				if(0 < iSizeFrRec){
					for(int idxRec = 0; idxRec < iSizeFrRec ;idxRec++){
						Ref<FreqRecord> fr = frRecLst[idxRec];
						if(fr && fr->miBufIndex >=0){
							AudioBufferMgr::getInstance()->getBufByIndex(fr->miBufIndex, iOffset, bufSegment);
							if(bFirstTime){
								//Workaround
								if(mIFreqAnalyzeResultCBListener)
									mIFreqAnalyzeResultCBListener->onBufCheck(bufSegment, 0, true, NULL);
								bFirstTime = false;
							}

							if(mIFreqAnalyzeResultCBListener){
								float freq = mIFreqAnalyzeResultCBListener->onBufCheck(bufSegment, fr->mlTs, false, fr->miFFTValues);
								selfFreqAnalyzer->analyze(fr->mlTs, freq, fr->miBufIndex, fr->miFFTValues);
							}
						}else{
							selfFreqAnalyzer->analyze(fr->mlTs, 0.0, -1, fr->miFFTValues);
						}
					}
				}else{
					LOGE("autoCorrection(), frRecLst is NULL for %s, at %lld\n",rec->strCdoe.c_str(), rec->lStartTs);
				}
			}

			if(0 == mlTraceTs){
				LOGE("autoCorrection(), break due to trace end\n");
				break;
			}

			nanosleep(&yieldtime, NULL);
		}

		//to simulate timeout case
		if(selfFreqAnalyzer->mbStartAppend){
			//if not detect postfix code, force to complete it
			if(false == selfFreqAnalyzer->checkEndPoint()){
				selfFreqAnalyzer->triggerTimeout();
			}
		}else{
			//if not get one result, force to timeout
			if(NULL != selfFreqAnalyzer->mIFreqAnalyzeResultCBListener){
				LOGE("selfFreqAnalyzer->mbStartAppend = false, trigger timeout\n" );
				selfFreqAnalyzer->triggerTimeout();
			}
		}

		selfFreqAnalyzer->setIFreqAnalyzeResultCB(NULL);
		selfFreqAnalyzer->reset();
		selfFreqAnalyzer->endToTrace();
	}else{
		LOGE("autoCorrection(), iSize= 0\n");
		if(mIFreqAnalyzeResultCBListener){
			mIFreqAnalyzeResultCBListener->onTimeout(this, true, mprevMatchRet);
		}
	}
	LOGE("autoCorrection(),------------------------------------------------------------------------------------------------------\n");
}

string FreqAnalyzer::removeDividerAndUnmark(string strDecode){
//		string[] strSplit = strDecode.split(DIVIDER);
	string strDecodeUnmark;
//		if(NULL != strSplit && strSplit.length == 2){
//			LOGE("removeDivider(), strSplit[0] = ["+strSplit[0]+"], strSplit[1] = ["+strSplit[1]+"]\n");
//			strDecodeUnmark = SoundPair_Config::decodeConsecutiveDigits(strSplit[0])+decodeConsecutiveDigits(strSplit[1]);
//		}else{
//			LOGE("removeDivider(), can not find DIVIDER\n");
		strDecodeUnmark = SoundPair_Config::decodeConsecutiveDigits(strDecode);
//		}
	return strDecodeUnmark;
}

string FreqAnalyzer::correctErrByDelta(int iDelta){
	if(DEBUG_MODE){
		LOGI("correctErrByDelta(),===========>>>>>>>>>>>>>>iDelta = %d\n",iDelta);
	}
	stringstream strRet;
//	if(0 < iDelta){
//		int iTime = iDelta;
//		while(iTime > 0){
//			vector<Ref<CodeRecord>> pairRec = findMaxDelta();
//			LOGI("correctErrByDelta(),===========>>>>>>>>>>>>>>findMaxDelta, pairRec[0]= %s, pairRec[1]=%s\n", pairRec[0]->toString().c_str(), pairRec[1]->toString().c_str());
//			if(NULL != pairRec[0] && NULL != pairRec[1]){
//
//				vector<Ref<CodeRecord>>::iterator it = std::find(mCodeRecordList.begin(), mCodeRecordList.end(), pairRec[1]);
//				if(mCodeRecordList.end() != it){
//					mCodeRecordList.insert(it, Ref<CodeRecord>(new CodeRecord((pairRec[1]->lStartTs + pairRec[0]->lStartTs)/2, (pairRec[1]->lEndTs + pairRec[0]->lEndTs)/2, "0")));
//				}else{
//					LOGE("correctErrByDelta(), cannot find pairRec[1]\n");
//				}
//			}
//			iTime--;
//		}
//	}else if(0 > iDelta){
//		int iTime = std::abs(iDelta);
//		while(iTime > 0){
//			Ref<CodeRecord> removeRec = findMinDelta();
//			LOGI("correctErrByDelta(),===========>>>>>>>>>>>>>>findMinDelta, removeRec= %s\n", removeRec->toString().c_str());
//			if(NULL == removeRec)
//				break;
//			vector<Ref<CodeRecord>>::iterator it = std::find(mCodeRecordList.begin(), mCodeRecordList.end(), removeRec);
//			if(it != mCodeRecordList.end())
//				mCodeRecordList.erase(it);
//			iTime--;
//		}
//	}
//
//	for(int i = 1; i < mCodeRecordList.size()-1;i++){
//		strRet<<(mCodeRecordList.at(i)->strCdoe);
//	}
//	LOGI("correctErrByDelta(),===========>>>>>>>>>>>>>> try to fix result to strRet= %s\n", strRet.str().c_str());

	return removeDividerAndUnmark(strRet.str());
}

string FreqAnalyzer::analyzeCodeRecordList(){
	stringstream strRet;
//	int iSize = mCodeRecordList.size();
//	if(iSize > 0){
//		Ref<CodeRecord> prevRec;
//		vector<Ref<CodeRecord>> pairRec(2);
//
//		msec_t lStartTs = 0, lEndTs, lMaxDelta = -1;;
//		for(int idx = 0; idx < iSize ;idx++){
//			Ref<CodeRecord> rec = mCodeRecordList[idx];
//			if(NULL == prevRec){
//				lStartTs = rec->lEndTs;
//			}else{
//				if(-1 == lMaxDelta){
//					lMaxDelta = rec->lEndTs -prevRec->lEndTs;
//					pairRec[0] = prevRec;
//					pairRec[1] = rec;
//				}else{
//					if(lMaxDelta < rec->lEndTs -prevRec->lEndTs){
//						lMaxDelta = rec->lEndTs -prevRec->lEndTs;
//						pairRec[0] = prevRec;
//						pairRec[1] = rec;
//					}
//				}
//			}
//
//			prevRec = rec;
//		}
//		lEndTs = prevRec->lStartTs;
//
//		msec_t lTotalPeriod = lEndTs - lStartTs;
//
//		LOGI("analyzeCodeRecordList(),===========>>>>>>>>>>>>>> lTotalPeriod= %lld, lMaxDelta=%lld\n",lTotalPeriod,lMaxDelta);
//		LOGI("analyzeCodeRecordList(),===========>>>>>>>>>>>>>> pairRec[0]= %s, pairRec[1]=%s\n", pairRec[0]->toString().c_str(), pairRec[1]->toString().c_str());
//		vector<Ref<CodeRecord>>::iterator it = std::find(mCodeRecordList.begin(), mCodeRecordList.end(), pairRec[1]);
//		if(mCodeRecordList.end() != it){
//			mCodeRecordList.insert(it, Ref<CodeRecord>(new CodeRecord((pairRec[1]->lStartTs + pairRec[0]->lStartTs)/2, (pairRec[1]->lEndTs + pairRec[0]->lEndTs)/2, "0")));
//		}else{
//			LOGE("analyzeCodeRecordList(), cannot find pairRec[1]\n");
//		}
//
//		for(int i = 1; i < mCodeRecordList.size()-1;i++){
//			strRet<<(mCodeRecordList.at(i)->strCdoe);
//		}
//		LOGI("analyzeCodeRecordList(),===========>>>>>>>>>>>>>> try to fix result to strRet= %s\n", strRet.str().c_str());
//	}

	return strRet.str();
}

vector<Ref<CodeRecord> > FreqAnalyzer::findMaxDelta(){
	vector<Ref<CodeRecord> > pairRec(2);
	int iSize = mCodeRecordList.size();
	if(iSize > 0){
		Ref<CodeRecord> prevRec;

		msec_t lMaxDelta = -1;;
		for(int idx = 0; idx < iSize ;idx++){
			Ref<CodeRecord> rec = mCodeRecordList[idx];
			if(NULL != prevRec){
				msec_t lDleta=((rec->lEndTs -prevRec->lEndTs)+(rec->lStartTs -prevRec->lStartTs))/2;
				if(-1 == lMaxDelta){
					lMaxDelta = lDleta;
					pairRec[0] = prevRec;
					pairRec[1] = rec;
				}else{
					if(lMaxDelta < lDleta){
						lMaxDelta = lDleta;
						pairRec[0] = prevRec;
						pairRec[1] = rec;
					}
				}
			}

			prevRec = rec;
		}
	}
	return pairRec;
}

Ref<CodeRecord> FreqAnalyzer::findMinDelta(){
	int pairRec[2];
	int iSize = mCodeRecordList.size();
	if(iSize > 0){
		int prevRec = -1;

		msec_t lMinDelta = LONG_MAX;
		for(int i = 0; i< iSize; i++){
			if(-1 != prevRec){
				msec_t lDleta=((mCodeRecordList.at(i)->lEndTs -mCodeRecordList.at(prevRec)->lEndTs)+(mCodeRecordList.at(i)->lEndTs -mCodeRecordList.at(prevRec)->lEndTs))/2;
				if(LONG_MAX == lMinDelta){
					lMinDelta = lDleta;
					pairRec[0] = 0;
					pairRec[1] = i;
				}else{
					if(lMinDelta > lDleta){
						lMinDelta = lDleta;
						pairRec[0] = prevRec;
						pairRec[1] = i;
					}
				}
			}
			prevRec = i;
		}
	}

	if(0 == pairRec[0] && mCodeRecordList.size() > 1){
		return mCodeRecordList.at(pairRec[1]);
	}else if((mCodeRecordList.size()-1) == pairRec[1]){
		return mCodeRecordList.at(pairRec[0]);
	}else{
		if(mCodeRecordList.size() > (pairRec[1]+1)){
			msec_t lDeltaPrev = ((mCodeRecordList.at(pairRec[0])->lEndTs - mCodeRecordList.at(pairRec[0] -1 )->lEndTs) + (mCodeRecordList.at(pairRec[0])->lStartTs - mCodeRecordList.at(pairRec[0] -1 )->lStartTs))/2;
			msec_t lDeltaNext = ((mCodeRecordList.at(pairRec[1]+1)->lEndTs - mCodeRecordList.at(pairRec[1])->lEndTs) + (mCodeRecordList.at(pairRec[1]+1)->lStartTs - mCodeRecordList.at(pairRec[1] )->lStartTs))/2;
			if(lDeltaPrev < lDeltaNext){
				return mCodeRecordList.at(pairRec[0]);
			}else{
				return mCodeRecordList.at(pairRec[1]);
			}
		}

	}
	return Ref<CodeRecord>(NULL);
}
//
//	static private int getMsgLengthByTime(){
//		int iRet = -1;
//		if(sCodeRecordList.size() > 1){
//			msec_t lDelta = (sCodeRecordList.get(sCodeRecordList.size()-1).lStartTs +sCodeRecordList.get(sCodeRecordList.size()-2).lEndTs)/2 - (sCodeRecordList.get(1).lStartTs+sCodeRecordList.get(0).lEndTs)/2;
//			iRet = (int) (lDelta/SoundPair_Config::TONE_PERIOD+((lDelta%SoundPair_Config::TONE_PERIOD > SoundPair_Config::TONE_PERIOD/2)?1:0));
//		}
//		return iRet;
//	}

int FreqAnalyzer::exceedToneCheckPeriod(){
	int iRet = -1;

	if(2 <= mFreqRecordList.size()){
		Ref<FreqRecord> firstRec = mFreqRecordList.front();
		Ref<FreqRecord> lastRec = mFreqRecordList.back();

		if(firstRec->mlTs > lastRec->mlTs){
			LOGE("exceedToneCheckPeriod(), abnormal sequence, firstRec:[%s], lastRec:[%s]\n", firstRec->toString().c_str(), lastRec->toString().c_str());
			mFreqRecordList.erase(mFreqRecordList.begin());
		}else if((SoundPair_Config::SEGMENT_FEATURE && -1 < mSessionBeginTs && (SoundPair_Config::TONE_FRAME_COUNT-1)*SoundPair_Config::FRAME_TS <= (lastRec->mlTs - firstRec->mlTs)) ||
		         (SoundPair_Config::TONE_PERIOD <= (lastRec->mlTs - firstRec->mlTs))){
			iRet = mFreqRecordList.size()-1;
		}
	}
	//LOGI("exceedToneCheckPeriod(), mFreqRecordList.size():%d, iRet:%d\n", mFreqRecordList.size(), iRet);
	return iRet;
}

int FreqAnalyzer::getMsgLength(int iDataLength){
	int iMultiply = SoundPair_Config::getMultiplyByFFTYPE();

	int iRealLen = (iMultiply > 1)?((iDataLength+1)/iMultiply):iDataLength;
	int iRet = -1;
	for(int i = 1;;i++){
		int iSum = 2*((int) ceil(SoundPair_Config::EC_RATIO*i))+i;
		if(iRealLen == iSum){
			iRet = i;
			break;
		}else if(iDataLength < iSum){
			break;
		}
	}

	return iRet*iMultiply;
}

int FreqAnalyzer::getMeaningfulMsgLength(int iDataLength, bool bAbove){
	const int MAX_TRIAL = 10;
	int iTrial = 0;
	int iNumTrial = iDataLength;
	int iRet = -1;
	while(iTrial++ < MAX_TRIAL){
		iNumTrial += (bAbove?1:-1);
		if(-1 < (iRet = getMsgLength(iNumTrial))){
			break;
		}
	}
	return iRet;
}

string FreqAnalyzer::decodeRSEC(string content){
	if(0 == content.length()){
		LOGI("decodeRSEC(), content is NULL\n");
		return "";
	}

	//string content = SoundPair_Config::decodeConsecutiveDigits(strDecode);

	int iMultiply = SoundPair_Config::getMultiplyByFFTYPE();
	int iPower = SoundPair_Config::getPowerByFFTYPE();

	stringstream ret;
	int numDataBytes = content.length();
	if(DEBUG_MODE){
		LOGI("decodeRSEC(),===========>>>>>>>>>>>>>> content= %s, numDataBytes=%d\n", content.c_str(), numDataBytes );
	}

	int numMsgBytes= getMsgLength(numDataBytes);//(int) Math.ceil((2*numDataBytes-3)/3.0f);
	if(0 > numMsgBytes){
		return "error1";
	}

	int numEcBytesInBlock = (1 == iMultiply)?(numDataBytes - numMsgBytes):((numDataBytes - numMsgBytes + 1)/iMultiply)*iMultiply;

	if(DEBUG_MODE){
		LOGI("decodeRSEC(), ===========>>>>>>>>>>>>>> numMsgBytes=%d , numEcBytesInBlock= %d\n",numMsgBytes ,numEcBytesInBlock );
	}

	//int* toDecode = NULL;
	int toDecodeSize = 0;
//	if(1 == SounPair_Config::getMultiplyByFFTYPE()){
//		toDecodeSize = numDataBytes;
//		toDecode = new int[numDataBytes];
//		int iLen = content.length();
//		for(int i =0;i < iLen;i++){
//			string strCode = content.substr(i, i+1);
//			for(int iIndex = 0; iIndex < SoundPair_Config::sFreqRangeTable.size(); iIndex++){
//				if((0!=strCode.compare(SoundPair_Config::sFreqRangeTable.at(iIndex)->mstrCode))){
//					toDecode[i] = iIndex;
//					break;
//				}
//			}
//		}
//	}else{
		toDecodeSize = numDataBytes/iMultiply;
		ArrayRef<int> toDecode(new Array<int>(toDecodeSize));
		//toDecode = new int[toDecodeSize];
		//memset(toDecode,0,toDecodeSize*sizeof(int));
		//int iLen = content.length()/iMultiply;
		//LOGD("decodeRSEC(), iLen = %d\n",iLen);
		for(int i =0;i < toDecodeSize;i++){
			toDecode[i] = 0;
			for(int j = 0;j < iMultiply;j++){
				string strCode = content.substr(i*iMultiply+j, 1);
				for(int iIndex = 0; iIndex < SoundPair_Config::sFreqRangeTable.size(); iIndex++){
					if((0==strCode.compare(SoundPair_Config::sFreqRangeTable.at(iIndex)->mstrCode))){
						toDecode[i] <<= iPower;
						toDecode[i] += iIndex;
						break;
					}
				}
			}
			if(DEBUG_MODE){
				LOGD("decodeRSEC(), toDecode[%d] = %d\n",i,toDecode[i]);
			}
		}
//	}

	//try {
		if(numMsgBytes < 0){
			return "error5_numMsgBytes<0";
		}
		//LOGI("decodeRSEC(), begin to decode\n\n");
		RS_DECODE_ERROR err = FreqAnalyzer::rsDecoder->decode(toDecode, numEcBytesInBlock/iMultiply);

		if(err != 0){
			LOGE("decodeRSEC(), decode err:%d\n", err);
			return "error2_ChecksumException";
		}else{
			for(int idx = 0; idx < toDecodeSize; idx++){
	//			if(1 == SoundPair_Config::getMultiplyByFFTYPE()){
	//				ret.append(SoundPair_Config::sFreqRangeTable.at(toDecode[idx])->mstrCode);
	//			}else{
					string tmp("");
					for(int j = 0;j < iMultiply;j++){
						tmp = SoundPair_Config::sFreqRangeTable.at(toDecode[idx] & ((0x1<<iPower) -1))->mstrCode + tmp;
						//ret.insert(idx*iMultiply, SoundPair_Config::sFreqRangeTable.at(toDecode[idx] & ((0x1<<iPower) -1))->mstrCode);
						toDecode[idx] >>= iPower;
					}
					ret << tmp;
	//			}
			}
			LOGI("decodeRSEC(), ret= %s\n", ret.str().c_str());
		}


//	  }
/*catch (ReedSolomonException ignored) {
		try {
			throw ChecksumException.getChecksumInstance();
		} catch (ChecksumException e) {
			e.printStackTrace();
			return "error2_ChecksumException";
		}
	  }*/
//	  catch(ArrayIndexOutOfBoundsException e){
//		  LOGI("decodeRSEC(), r= "+e.toString() );
//		  return "error3_ArrayIndexOutOfBoundsException";
//	  } catch(IllegalStateException e4){
//		  LOGI("decodeRSEC(), r= "+e4.toString() );
//		  return "error4_IllegalStateException";
//	  }
	return (ret.str().length() >= numEcBytesInBlock)?ret.str().substr(0, ret.str().length()-(numEcBytesInBlock)):"";
}

//audio pre-process and FFT

int FreqAnalyzer::sSampleRate = 16000;
int FreqAnalyzer::sFrameSize  = 512;
int FreqAnalyzer::sHalfFrameSize  = 0;
float FreqAnalyzer::sBinSize = 0;
int FreqAnalyzer::sNSIndex  = 0;
float FreqAnalyzer::sAGCLevel = 0;
float FreqAnalyzer::sFFTTol = 0.5f;
bool	FreqAnalyzer::sEnableDeverb = false;
float FreqAnalyzer::sDeverbDecay = 0.0f;
float FreqAnalyzer::sDeverbLevel = 0.0f;
int FreqAnalyzer::LOW_PASS_CRITERIA_FREQ = 1000;
int FreqAnalyzer::sLowPassIndex = 0;

int FreqAnalyzer::HIGH_PASS_CRITERIA_FREQ = 3300;
int FreqAnalyzer::sHighPassIndex = 0;

void FreqAnalyzer::initAnalysisParams(int iSampleRate, int iFrameSize, int iNSIndex, float iAGCLevel, bool bDeverb, float fDeverbDecay, float dDeverbLevel){
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

	initAudacity();

	if(DEBUG_MODE){
		LOGE("startRecord()+, sSampleRate:%d, sFrameSize:%d, sLowPassIndex:%d, sHighPassIndex:%d, sBinSize:%f\n", sSampleRate, sFrameSize, sLowPassIndex, sHighPassIndex, sBinSize);
	}
}

void FreqAnalyzer::setSpeexPreprocess(SpeexPreprocessState* sps){
	if(NULL != sps){
		if(sNSIndex < 0){
			int denoise = 1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DENOISE, &denoise);

			//int iRet = 0;
			//speex_preprocess_ctl(sps, SPEEX_PREPROCESS_GET_NOISE_SUPPRESS, &iRet);
			//LOGI("recordAudio+, SPEEX_PREPROCESS_GET_NOISE_SUPPRESS:%d\n", iRet);

//			iRet = sNSIndex;
//			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &iRet);
		}

		//Not used in FIXED-POINT
		if(sAGCLevel >0){
			int i=1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_AGC, &i);
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_AGC_LEVEL, &sAGCLevel);
		}

		if(sEnableDeverb){
			int i=1;
			speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB, &i);
//			if(0 < sDeverbDecay){
//				speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB_DECAY, &sDeverbDecay);
//			}
//
//			if(0 < sDeverbLevel){
//				speex_preprocess_ctl(sps, SPEEX_PREPROCESS_SET_DEREVERB_LEVEL, &sDeverbLevel);
//			}
		}
	}
}

void FreqAnalyzer::runSpeexPreprocess(SpeexPreprocessState* sps, short *bytes){
	if(NULL != sps && NULL != bytes){
		if((sNSIndex < 0 || sAGCLevel >0 || sEnableDeverb)){
			/*int iRet = */speex_preprocess_run(sps, bytes);
			//LOGI("runSpeexPreprocess+, speex_preprocess_run:%d\n", iRet);
		}
	}
}

void FreqAnalyzer::performSpeexPreprocess(short * bytes, bool bReset, SpeexPreprocessState** speexPrep){
	if(NULL == *speexPrep && (sNSIndex < 0 || sAGCLevel >0 || sEnableDeverb)){
		*speexPrep = speex_preprocess_state_init(sFrameSize, sSampleRate);
		setSpeexPreprocess(*speexPrep);
	}

	runSpeexPreprocess(*speexPrep, bytes);
}

void FreqAnalyzer::runAudioPreprocess(short * array, bool bReset){
	if(DEBUG_MODE){
		LOGD("runAudioPreprocess+\n");
	}
	performSpeexPreprocess(array, bReset, &stPreprocess);
}

void FreqAnalyzer::runAudioPreprocessAC(short * array, bool bReset){
	if(DEBUG_MODE){
		LOGD("runAudioPreprocessAC+\n");
	}
	performSpeexPreprocess(array, bReset, &stPreprocessAC);
}

//Audacity FFT analysis
pthread_mutex_t FreqAnalyzer::sSyncFFTObj;
float *FreqAnalyzer::inBuffer = NULL;
float *FreqAnalyzer::outBuffer = NULL;
float *FreqAnalyzer::win = NULL;
double FreqAnalyzer::wss ;
int FreqAnalyzer::windowFunc = 3;//hannings

//#define FFTW_TRIAL

#ifdef FFTW_TRIAL
float *inBuffer2 = NULL;
float *outBuffer2 = NULL;
//fftw_complex *outBuffer2 = NULL;
static fftwf_plan plan_forward;
#endif

void FreqAnalyzer::initAudacity(){
	if(DEBUG_MODE){
		LOGD("initAudacity(), ++\n");
	}
	if(NULL == inBuffer){
		inBuffer = (float*)malloc(sFrameSize*sizeof(float));
	}

	if(NULL == outBuffer){
		outBuffer = (float*)malloc(sFrameSize*sizeof(float));//new float[sFrameSize];
	}

#ifdef FFTW_TRIAL
	if(NULL == inBuffer2){
		inBuffer2 = (float*)malloc(sFrameSize*sizeof(float));
	}

	if(NULL == outBuffer2){
		//outBuffer2 = fftw_alloc_complex(sFrameSize);//
		outBuffer2 = (float*)malloc(sFrameSize*sizeof(float));
		//memset((void *)outBuffer2, 0, sizeof(float) * sFrameSize);
		//LOGE("initAudacity(), fftw_plan_r2r_1d+\n");
		plan_forward = fftwf_plan_r2r_1d ( sFrameSize, inBuffer2, outBuffer2, FFTW_R2HC, FFTW_ESTIMATE);
		//LOGE("initAudacity(), fftw_plan_r2r_1d-\n");
		//plan_forward = fftw_plan_r2r_1d ( sFrameSize, inBuffer2, outBuffer2, FFTW_R2HC, FFTW_ESTIMATE );
		//L
		//plan_forward = fftw_plan_dft_r2c_1d ( sFrameSize, inBuffer2, outBuffer2, FFTW_ESTIMATE );
		//
	}

#endif

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

	pthread_mutex_init(&sSyncFFTObj, NULL);


	if(DEBUG_MODE){
		LOGD("initAudacity(), --\n");
	}
}

void FreqAnalyzer::deinitAudacity(){
	pthread_mutex_destroy(&sSyncFFTObj);

	if(NULL != inBuffer){
		free(inBuffer);
		inBuffer = NULL;
	}

	if(NULL == outBuffer){
		free(outBuffer);
		outBuffer = NULL;
	}

#ifdef FFTW_TRIAL
	fftwf_destroy_plan ( plan_forward );

	if(NULL != inBuffer2){
		free(inBuffer2);
		inBuffer2 = NULL;
	}

	if(NULL != outBuffer2){
		free(outBuffer2);
		//fftw_free(outBuffer2);
		outBuffer2 = NULL;
	}
#endif

	if(NULL == win){
		free(win);
		win = NULL;	}
}

void FreqAnalyzer::performWindowFunc(float *winBuf){
	if(NULL != winBuf){
		int i=0;
		for(i=0; i < sFrameSize; i++)
			winBuf[i] = 1.0;
		WindowFunc(windowFunc, sFrameSize, winBuf);
	}
}
#define FIXED_POINT
#include <kiss_fft.h>

struct KissFFT{
    kiss_fft_cfg forwardConfig;
    kiss_fft_cpx* spectrumIn;
    kiss_fft_cpx* spectrumOut;
    int numSamples;
    int spectrumSize;
};

static KissFFT* fft = NULL;

float FreqAnalyzer::analyzeAudioViaKiss(ArrayRef<short> array, int iBufSize, int* iDxValues){
	if(!fft){
		if(DEBUG_MODE){
			LOGE("0\n");
		}
		fft = new KissFFT();
		//LOGE("0-1\n");

		//LOGE("0-2\n");
		fft->numSamples = iBufSize;
		fft->spectrumSize = iBufSize/2+1;

		fft->forwardConfig = kiss_fft_alloc(iBufSize,0,NULL,NULL);

		fft->spectrumIn = (kiss_fft_cpx*)malloc(sizeof(kiss_fft_cpx) * iBufSize);
		//LOGE("0-3\n");
		fft->spectrumOut = (kiss_fft_cpx*)malloc(sizeof(kiss_fft_cpx) * iBufSize);
		if(DEBUG_MODE){
			LOGE("0-4\n");
		}
	}

	//LOGE("1\n");
	long lTickCount = getTickCount();

	for(int i=0;i<fft->spectrumSize-1;i++) {
		fft->spectrumIn[i].r = array[2*i];
		fft->spectrumIn[i].i = array[2*i+1];
	}

	//LOGE("2\n");
	kiss_fft(fft->forwardConfig, fft->spectrumIn, fft->spectrumOut);

	//LOGE("3\n");

	int iDx = 0;
	int iDx2 = 0;
	int iDx3 = 0;
	int iDx4 = 0;
	int iDx5 = 0;

	//LOGE("4\n");
	int iRet = fft->spectrumOut[0].r*fft->spectrumOut[0].r + fft->spectrumOut[0].i*fft->spectrumOut[0].i;
	for(int i=1;i<fft->spectrumSize-1;i++) {
		int iTmp = fft->spectrumOut[i].r*fft->spectrumOut[i].r + fft->spectrumOut[i].i*fft->spectrumOut[i].i;
		if(iTmp > iRet){
			//LOGE("analyzeAudioViaAudacity+, fRetTmp[%d] = %f\n", i, fRetTmp);
			iDx5 = iDx4;
			iDx4 = iDx3;
			iDx3 = iDx2;
			iDx2 = iDx;
			iDx  = i;
			iRet = iTmp;
		}
	}

	//LOGE("5\n");
	if(NULL != iDxValues){
		iDxValues[0] = iDx;
		iDxValues[1] = iDx2;
		iDxValues[2] = iDx3;
		iDxValues[3] = iDx4;
		iDxValues[4] = iDx5;
	}

	float fRet = sBinSize*iDx;
	//long lDelta = (getTickCount() - lTickCount);
	//LOGE(" takes [%ld] ms, fRet:[%f]\n", lDelta, fRet);

	return fRet;
}

float FreqAnalyzer::performAudacityFFT(ArrayRef<short> bytes, bool bReset, SpeexPreprocessState** speexPrep, int iLastDet, int* iDxValues){
	//LOGE("performAudacityFFT+\n");
	static ArrayRef<short> bufCheck = ArrayRef<short>(new Array<short>(SoundPair_Config::FRAME_SIZE_REC));
	float fRet = 0.0;
//	if(bReset){
//		deinitAudacity();
//		initAudacity();
//	}

	if(NULL == inBuffer){
		initAudacity();
	}

	static long lTotalTime = 0, lCount = 1;

	long lTickCount = getTickCount();
	pthread_mutex_lock(&sSyncFFTObj);

	memcpy(&bufCheck[0], &bytes[0], SoundPair_Config::FRAME_SIZE_REC*sizeof(short));

	performSpeexPreprocess(&bufCheck[0], bReset, speexPrep);

	long lDelta = (getTickCount() - lTickCount);
	lTotalTime+=lDelta;
	//LOGE("performAudacityFFT(), performSpeexPreprocess takes %ld ms, average: %ld ms\n", lDelta, lTotalTime/(++lCount));


//	lTickCount = getTickCount();
//	performWindowFunc(win);
//	LOGE("performAudacityFFT(), performWindowFunc takes %ld ms\n", (getTickCount() - lTickCount));

	int iDx = 0;
	int iDx2 = 0;
	int iDx3 = 0;
	int iDx4 = 0;
	int iDx5 = 0;

	if(0 < bufCheck->size()){
		int i=0;
		static long sTotalTime1=0, sTotalTime2=0, sTotalTime2_1=0, sTotalTime2_2=0;
#ifdef FFTW_TRIAL

		for (i = 0; i < sFrameSize; i++)
			inBuffer2[i] =  (bufCheck[i]/32767.9)* win[i];

		lTickCount = getTickCount();
		memset((void *)outBuffer2, 0, sizeof(float) * sFrameSize);
		fftwf_execute ( plan_forward );
		//LOGE("performAudacityFFT(), fftw_execute takes %ld ms\n", (getTickCount() - lTickCount));

		fRet = outBuffer2[0] * outBuffer2[0] + outBuffer2[sHalfFrameSize + 1] * outBuffer2[sHalfFrameSize + 1];//outBuffer[0];//fabs(outBuffer2[0]);
		for (i = sLowPassIndex; i < sHalfFrameSize && i <= sHighPassIndex; i++){
			float fRetTmp = outBuffer2[i] * outBuffer2[i] + outBuffer2[sHalfFrameSize + 1 - i] * outBuffer2[sHalfFrameSize + 1 - i];
			if(fRetTmp > fRet){
				//LOGE("analyzeAudioViaAudacity+, fRetTmp[%d] = %f\n", i, fRetTmp);
				iDx5 = iDx4;
				iDx4 = iDx3;
				iDx3 = iDx2;
				iDx2 = iDx;
				iDx  = i;
				fRet = fRetTmp;
			}
		}
		sTotalTime1+=(getTickCount() - lTickCount);

#else
		lTickCount = getTickCount();
		for (i = 0; i < sFrameSize; i++)
			inBuffer[i] = win[i] * bufCheck[i];

		sTotalTime2_1 += (getTickCount() - lTickCount);
		lTickCount = getTickCount();

		//LOGE("performAudacityFFT(), inBuffer takes %ld ms\n", (getTickCount() - lTickCount));


		PowerSpectrum(sFrameSize, inBuffer, outBuffer);
		//LOGE("performAudacityFFT(), PowerSpectrum takes %ld ms\n", (getTickCount() - lTickCount));
		sTotalTime2 += (getTickCount() - lTickCount);
		lTickCount = getTickCount();

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

		sTotalTime2_2 += (getTickCount() - lTickCount);

#endif
		if(CAM_TIME_TRACE){
			if(DEBUG_MODE){
				LOGE("performAudacityFFT(), Spectrum and Preprocess takes [%ld, %ld, %ld, %ld, %ld] ms average\n", sTotalTime1/lCount, sTotalTime2_1/lCount, sTotalTime2/lCount, sTotalTime2_2/lCount, lTotalTime/(lCount));
			}
		}

		lCount++;
		if(NULL != iDxValues){
			iDxValues[0] = iDx;
			iDxValues[1] = iDx2;
			iDxValues[2] = iDx3;
			iDxValues[3] = iDx4;
			iDxValues[4] = iDx5;
		}

//		if(0 < iLastDet){
//			//LOGE("performAudacityFFT()-------------------------, iLastDetTone = [%.2f]=>%d, iDx0~5 = [%d, %d, %d, %d, %d]\n", iLastDet*sBinSize, iLastDet, iDx, iDx2, iDx3, iDx4, iDx5);
//			if(iDx > 0 && iDx2 >0 && iLastDet > 0 && (iDx - iLastDet <=1 && iDx - iLastDet >= -1) && (iDx - iDx2 >=2 || iDx - iDx2 <= -2)){
//				LOGE("performAudacityFFT()^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^, iLastDetTone=%.2f, change from %.2f to %.2f \n", iLastDet*sBinSize, iDx*sBinSize,  iDx2*sBinSize);
//				iDx = iDx2;
//			}
//		}
	}else{
		LOGE("performAudacityFFT(), bufCheck is null\n");
	}

	pthread_mutex_unlock(&sSyncFFTObj);

	return sBinSize*iDx;
}

float FreqAnalyzer::analyzeAudioViaAudacity(ArrayRef<short> array, int iBufSize, bool bReset ,int iLastDetect, int* iFFTValues){
	return performAudacityFFT(array, bReset, &stPreprocess, iLastDetect, iFFTValues);
}

float FreqAnalyzer::analyzeAudioViaAudacityAC(ArrayRef<short> array, int iBufSize, bool bReset,int iLastDetect, int* iFFTValues){
	return performAudacityFFT(array, bReset, &stPreprocessAC, iLastDetect, iFFTValues);
}
