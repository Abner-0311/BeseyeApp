#include "sp_config.h"

using namespace std;

std::map<string, double> SoundPair_Config::sAlphabetTable;
std::vector<Ref<FreqRange> > SoundPair_Config::sFreqRangeTable;
std::vector<string> SoundPair_Config::sCodeTable;
Ref<GenericGF> SoundPair_Config::gf = GenericGF::QR_CODE_FIELD_256;

float SoundPair_Config::AMP_BASE_RATIO[] = {1.00f, 1.00f, 1.10f, 1.10f, // 1093.75, 1187.50, 1781.25, 1968.75,
											1.04f, 1.00f, 1.00f, 1.05f, // 1281.25, 1468.75, 1593.75, 1687.5,
											0.60f, 0.98f, 1.02f, 1.00f, // 2843.75, 2093.75, 2187.50, 2375.0,
											1.00f, 0.90f, 0.80f, 0.68f, // 2468.75, 2562.50, 2656.25, 2750.0,
											0.80f, 1.00f, 0.40f      }; // 1406.25, 2281.25, 3218.75,

const string SoundPair_Config::BT_MSG_ACK = "BT_MSG_ACK";
const string SoundPair_Config::BT_MSG_PURE = "?P?";
const string SoundPair_Config::BT_BINDING_MAC = "D4:20:6D:EA:0F:3F";// XL  "BC:CF:CC:E2:C3:AC";// -> Butterfly  //
const string SoundPair_Config::BT_BINDING_MAC_SENDER = "7C:61:93:BF:32:3D";// FLYER
const long SoundPair_Config::TONE_PERIOD     = (long)(SoundPair_Config::TONE_DURATION*1000);
const float SoundPair_Config::BIN_SIZE       = (float)SoundPair_Config::SAMPLE_RATE_REC/(float)SoundPair_Config::FRAME_SIZE_REC;
string SoundPair_Config::PREFIX_DECODE = "w~";
string SoundPair_Config::POSTFIX_DECODE = "~w";
string SoundPair_Config::POSTFIX_DECODE_C1 = "~w";
string SoundPair_Config::POSTFIX_DECODE_C2 = "~w";
string SoundPair_Config::PEER_SIGNAL = "}#";
string SoundPair_Config::CONSECUTIVE_MARK = "";
string SoundPair_Config::DIVIDER = "";
string SoundPair_Config::BT_MSG_DIVIDER;
string SoundPair_Config::BT_MSG_FORMAT;
string SoundPair_Config::BT_MSG_SET_VOLUME = "BTVOL_";
string SoundPair_Config::BT_MSG_SET_VOLUME_END = "_VOLBT";
string SoundPair_Config::MISSING_CHAR = "%";

void SoundPair_Config::normalizeRatio(){
	int iLen = TONE_TYPE;//sizeof(AMP_BASE_RATIO)/sizeof(AMP_BASE_RATIO[0]);
	float fMax = AMP_BASE_RATIO[0];
	for(int i =1; i < iLen; i++){
		if(fMax < AMP_BASE_RATIO[i]){
			fMax = AMP_BASE_RATIO[i];
		}
	}

	for(int i =0; i < iLen; i++){
		AMP_BASE_RATIO[i]/=fMax;
		//LOGE(TAG, "normalizeRatio(), AMP_BASE_RATIO["+i+"] = "+AMP_BASE_RATIO[i]);
	}
}

string SoundPair_Config::getNDigits(string strCode, int iNumDigits){
	string strRet = "";
	for(int i =0; i < iNumDigits; i++){
		strRet+=strCode;
	}
	return strRet;
}

void SoundPair_Config::resolveFreqRangeConflict(){
	int iSize = sFreqRangeTable.size();
	if(AUBIO_FFT){
		for(int i =0; i < iSize - 1; i++){
			Ref<FreqRange> fr = sFreqRangeTable[i];
			std::vector<Ref<FreqRangeData> > lstFRD = fr->getLstFreqRangeData();
			int iSizeFRD = lstFRD.size();
			for(int idx = iSizeFRD -1; idx > 0; idx--){
				Ref<FreqRangeData> frd = lstFRD[idx];
				if(NULL != frd){
					bool bConflict = false;
					for(int iChkIdx = i+1; iChkIdx < iSize - 1;iChkIdx++){
						Ref<FreqRange> frChk = sFreqRangeTable[iChkIdx];
						//if(frChk.withinFreqRange(frd.mdLowerBound) || frChk.withinFreqRange(frd.mdUpperBound)){
						if(frChk->isOverlap(frd)){
							bConflict = true;
							break;
						}
					}
					if(bConflict){
						//LOGE("resolveFreqRangeConflict(), remove frd = %d",frd);
						lstFRD.erase(lstFRD.begin()+idx);
					}
				}
			}
		}
	}else{
		for(int i =iSize - 1; i >= 0 ; i--){
			Ref<FreqRange> fr = sFreqRangeTable[i];
			std::vector<Ref<FreqRangeData> > lstFRD = fr->getLstFreqRangeData();
			int iSizeFRD = lstFRD.size();
			for(int idx = iSizeFRD -1; idx > 0; idx--){
				Ref<FreqRangeData> frd = lstFRD[idx];
				if(NULL != frd){
					bool bConflict = false;
					if(i == 0){
						for(int iChkIdx = 1; iChkIdx < iSize; iChkIdx++){
							Ref<FreqRange> frChk = sFreqRangeTable[iChkIdx];
							//if(frChk.withinFreqRange(frd.mdLowerBound) || frChk.withinFreqRange(frd.mdUpperBound)){
							if(frChk->isOverlap(frd)){
								bConflict = true;
								break;
							}
						}
					}else{
						for(int iChkIdx = i-1; iChkIdx >=0;iChkIdx--){
							Ref<FreqRange> frChk = sFreqRangeTable[iChkIdx];
							//if(frChk.withinFreqRange(frd.mdLowerBound) || frChk.withinFreqRange(frd.mdUpperBound)){
							if(frChk->isOverlap(frd)){
								bConflict = true;
								break;
							}
						}
					}

					if(bConflict){
						//LOGE("resolveFreqRangeConflict(), remove frd = %d",frd);
						lstFRD.erase(lstFRD.begin()+idx);
					}
				}
			}
		}
	}
}

void SoundPair_Config::init(){
	if(!sFreqRangeTable.empty()){
		LOGE("init(), have init, return");
		return;
	}
	double dDelta = (dEndValue - dStartValue)/(16+3);//Plus additional 5 char for special use
	double dValue = dStartValue;

	double freqs[] = {
					1093.75,
					1187.50,
					1781.25,
					1968.75,//2843.75,//2000.0,//influence 1777

					1281.25,
					1468.75,
					1593.75,
					1687.5,

					2843.75,//1875.0,
					2093.75,
					2187.50,
					2375.00,

					2468.75,
					2562.50,
					2656.25,
					2750.00,

					1375.75,
					2281.25,
					3218.75,
			  };

	int iDx = 0;

	//0~9
	for(char i = 0x30; i<= 0x39 ; i++, dValue+=dDelta, iDx++){
		string strCode;
		strCode.push_back(i);
		sCodeTable.push_back(strCode);
		sAlphabetTable.insert(std::pair<string, double>(strCode, freqs[iDx]));
		sFreqRangeTable.push_back(Ref<FreqRange>(new FreqRange(freqs[iDx], (AUBIO_FFT?BIN_SIZE/*0.5*(freqs[iDx]/600.0f)*/:BIN_SIZE), strCode)));
	}

	//A~I
	for(char i = 0x61; i<= 0x69 ; i++, dValue+=dDelta, iDx++){
		string strCode;
		strCode.push_back(i);
		sCodeTable.push_back(strCode);
		sAlphabetTable.insert(std::pair<string, double>(strCode, freqs[iDx]));
		sFreqRangeTable.push_back(Ref<FreqRange>(new FreqRange(freqs[iDx], (AUBIO_FFT?BIN_SIZE/*0.5*(freqs[iDx]/600.0f)*/:BIN_SIZE), strCode)));
	}

	int iCodeTblSIze = sCodeTable.size();

	PREFIX_DECODE 	 = sCodeTable[iCodeTblSIze-3]+sCodeTable[iCodeTblSIze-1];//IK

	POSTFIX_DECODE_C1= sCodeTable[iCodeTblSIze-2];
	POSTFIX_DECODE_C2= sCodeTable[iCodeTblSIze-1];

	POSTFIX_DECODE 	 = POSTFIX_DECODE_C1 + POSTFIX_DECODE_C2;//KI

	//AubioTestConfig.PEER_SIGNAL 	 = sCodeTable.get(iCodeTblSIze-4)+sCodeTable.get(iCodeTblSIze-1);
	CONSECUTIVE_MARK = sCodeTable[iCodeTblSIze-3];
	//AubioTestConfig.DIVIDER			 = sCodeTable.get(iCodeTblSIze-5);

	BT_MSG_DIVIDER = sCodeTable[iCodeTblSIze-2]+sCodeTable[iCodeTblSIze-1];
	BT_MSG_FORMAT = "%s"+BT_MSG_DIVIDER+"%s";

	resolveFreqRangeConflict();
	normalizeRatio();
}

void SoundPair_Config::uninit(){
	while(!sAlphabetTable.empty()){
		sAlphabetTable.clear();
	}

//	while(!sFreqRangeTable.empty()){
//		Ref<FreqRange> data = sFreqRangeTable.back();
//		sFreqRangeTable.pop_back();
//		if(data){
//			delete data;
//			data = NULL;
//		}
//	}
	sFreqRangeTable.clear();

	while(!sCodeTable.empty()){
		sCodeTable.clear();
	}
}

int SoundPair_Config::getDivisionByFFTYPE(){
	return 16;
}

int SoundPair_Config::getMultiplyByFFTYPE(){
	return 2;
}

int SoundPair_Config::getPowerByFFTYPE(){
	return 4;
}

string SoundPair_Config::decodeConsecutiveDigits(string strCode){
	return strCode;
}

int SoundPair_Config::findIdxFromCodeTable(string strCode){
	int iRet = -1;
	vector<string>::iterator findit = find(SoundPair_Config::sCodeTable.begin(),SoundPair_Config::sCodeTable.end(),strCode);
	if(SoundPair_Config::sCodeTable.end() != findit){
		iRet = std::distance(SoundPair_Config::sCodeTable.begin(), findit);
	}
	//LOGE("findIdxFromCodeTable(), strCode=%s, iRet = %d", strCode.c_str(), iRet);
	return iRet;
}

Ref<FreqRange> SoundPair_Config::findFreqRange(string strCode){
	Ref<FreqRange> ret;
	int idx = findIdxFromCodeTable(strCode);
	if(0 <= idx){
		ret = SoundPair_Config::sFreqRangeTable.at(idx);
	}
	return ret;
}

FreqRangeData::FreqRangeData(double dValue, double dDelta) {
	init(dValue, dValue - fabs(dDelta), dValue + fabs(dDelta));
}

FreqRangeData::FreqRangeData(double dValue, double dLowerBound, double dUpperBound) {
	init(dValue, dLowerBound, dUpperBound);
}

FreqRangeData::~FreqRangeData(){}

void FreqRangeData::init(double dValue, double dLowerBound, double dUpperBound){
	this->mdValue = dValue;
	this->mdUpperBound = dUpperBound;
	this->mdLowerBound = dLowerBound;
}

string FreqRangeData::toString() {
	std::stringstream s;
	s<<"FreqRangeData [mdValue=" << mdValue << ", mdLowerBound="
			<< mdLowerBound << ", mdUpperBound=" << mdUpperBound << "]\n";
	return s.str();
}

bool FreqRangeData::withinFreqRange(double dFreq){
	return largeEqualThan(dFreq, mdLowerBound) && lessEqualThan(dFreq, mdUpperBound);
}


FreqRange::FreqRange(double dFreq, string strCode){
	init(dFreq, 50.0, strCode);
}

FreqRange::FreqRange(double dFreq, double dDelta, string strCode){
	init(dFreq, dDelta, strCode);
}

FreqRange::FreqRange(double dFreq, double dUpperBound, double dLowerBound, string strCode){
	mlstFreqRangeData.push_back(Ref<FreqRangeData>(new FreqRangeData(dFreq, dLowerBound, dUpperBound)));
	this->mstrCode = strCode;
}

FreqRange::~FreqRange(){
	mlstFreqRangeData.clear();
//	while(!mlstFreqRangeData.empty()){
//		Ref<FreqRangeData> data = mlstFreqRangeData.back();
//		mlstFreqRangeData.pop_back();
//		if(data){
//			delete data;
//			data = NULL;
//		}
//	}
}

void FreqRange::init(double dFreq, double dDelta, string strCode){
	mlstFreqRangeData.push_back(Ref<FreqRangeData>(new FreqRangeData(dFreq, dDelta)));
	this->mstrCode = strCode;
}

bool FreqRange::withinFreqRange(double dFreq){
	bool bRet = false;
	for (int idx = 0; idx < mlstFreqRangeData.size();idx++){
		Ref<FreqRangeData> it = mlstFreqRangeData[idx];
		if(it->withinFreqRange(dFreq)){
			bRet = true;
			break;
		}
	}
	return bRet;
}

bool FreqRange::isOverlap(Ref<FreqRangeData> frdChk){
	bool bRet = false;
	for (int idx = 0; idx < mlstFreqRangeData.size();idx++){
		Ref<FreqRangeData> frd = mlstFreqRangeData[idx];
		if(!(largeThan(frd->mdLowerBound, frdChk->mdUpperBound) || lessThan(frd->mdUpperBound, frdChk->mdLowerBound) )){
			bRet = true;
			break;
		}
	}
	return bRet;
}

std::vector<Ref<FreqRangeData> > FreqRange::getLstFreqRangeData(){
	return mlstFreqRangeData;
}

#ifndef GEN_TONE_ONLY

string findToneCodeByFreq(double dFreq){
	string strCode = "";
	int iSize = SoundPair_Config::sFreqRangeTable.size();
	for(int idx = 0; idx < iSize; idx++){
	//for (std::vector<FreqRange>::iterator fr = SoundPair_Config::sFreqRangeTable.begin() ; fr != SoundPair_Config::sFreqRangeTable.end(); ++fr){
		Ref<FreqRange> fr = SoundPair_Config::sFreqRangeTable[idx];
		if(fr->withinFreqRange(dFreq)){
			strCode = fr->mstrCode;
			break;
		}
	}
	return strCode;
}

FreqRecord::FreqRecord(msec_t lTs, double dFreq, string strCode, int iBufIndex, int iFFTValues[]){
	init(lTs, dFreq, strCode, iBufIndex, iFFTValues);
}

FreqRecord::FreqRecord(Ref<FreqRecord> copyObj){
	if(!copyObj)
		return;

	init(copyObj->mlTs, copyObj->mdFreq, copyObj->mstrCode, copyObj->miBufIndex, copyObj->miFFTValues);
}

FreqRecord::~FreqRecord(){}

void FreqRecord::init(msec_t lTs, double dFreq, string strCode, int iBufIndex, int iFFTValues[]){
		mlTs = lTs;
		mdFreq = dFreq;
		mstrCode = strCode;
		miBufIndex = iBufIndex;
		//LOGE("FreqRecord::init(), iFFTValues:%d", iFFTValues);
		if(NULL != iFFTValues)
			std::copy(iFFTValues, iFFTValues+SoundPair_Config::FFT_ANALYSIS_COUNT, miFFTValues);
		//LOGE("FreqRecord::init(), iFFTValues:%d--", iFFTValues);
	}

Ref<FreqRecord> FreqRecord::recheckToneCode(int iLastTone){
	Ref<FreqRecord> frRet = Ref<FreqRecord>(this);
	if(0 < iLastTone && !miFFTValues){
		if(miFFTValues[0] > 0 && miFFTValues[1] > 0 && (miFFTValues[0] - iLastTone <= 1 && miFFTValues[0] - iLastTone >= -1) && (miFFTValues[0] - miFFTValues[1] >= 2 || miFFTValues[0] - miFFTValues[1] <= -2)){
			double newFreq = miFFTValues[1]*SoundPair_Config::BIN_SIZE;
			if(!sameValue(mdFreq, newFreq)){
				string strCode = findToneCodeByFreq(newFreq);
				if(0!=strCode.compare("")){
					frRet = Ref<FreqRecord>(new FreqRecord(Ref<FreqRecord>(this)));

					LOGE("recheckToneCode()---------------------------------->, change from %s to %s", mstrCode.c_str(), strCode.c_str());

					frRet->mdFreq = newFreq;
					frRet->mstrCode = strCode;
				}
			}
		}
	}
	return frRet;
}

string FreqRecord::toString() {
	std::stringstream s;
	s<<"FreqRecord [miBufIndex=" << miBufIndex << ", mlTs=" << mlTs
			<< ", mdFreq=" << mdFreq << ", mstrCode=" << mstrCode
			<< ", miFFTValues=[ ";// << Arrays.toString(miFFTValues) << "]";
	if(miFFTValues){
		for(int i = 0; i < SoundPair_Config::FFT_ANALYSIS_COUNT;i++){
			s<<miFFTValues[i]<<" ";
		}
		s<<"]";
	}
	return s.str();
}

CodeRecord::CodeRecord() {
}

CodeRecord::CodeRecord(std::vector<Ref<FreqRecord> > lstFreqRec, string strReplaced){
	this->strReplaced = strReplaced;
	mlstFreqRec = lstFreqRec;
	inferFreq();
}

CodeRecord::CodeRecord(msec_t lStartTs, msec_t lEndTs, string strCdoe) {
	setParameters(lStartTs, lEndTs, strCdoe);
}

CodeRecord::CodeRecord(msec_t lStartTs, msec_t lEndTs, string strCode, std::vector<Ref<FreqRecord> > lstFreqRec){
	setParameters(lStartTs, lEndTs, strCode);
	mlstFreqRec = lstFreqRec;
}

CodeRecord::~CodeRecord(){
//	while(!mlstFreqRec.empty()){
//		Ref<FreqRecord> record = mlstFreqRec.back();
//		mlstFreqRec.pop_back();
//		if(record){
//			delete record;
//			record = NULL;
//		}
//	}
	mlstFreqRec.clear();
}

void CodeRecord::setParameters(msec_t lStartTs, msec_t lEndTs, string strCdoe){
	this->lStartTs = lStartTs;
	this->lEndTs = lEndTs;
	this->strCdoe = strCdoe;
}

void CodeRecord::inferFreq(){
	if(0 == mlstFreqRec.size()){
		LOGE("inferFreq()**, NULL mlstFreqRec");
		return;
	}
	int iSize = mlstFreqRec.size();
	for(int i =0; i< iSize;i++){

	}
	Ref<FreqRecord> frFirst = mlstFreqRec[0];
	Ref<FreqRecord> frLast = mlstFreqRec[iSize-1];

	//infer the freq
	bool bDiff = false;
	string* strLastCheck = NULL;
	string* curCode = NULL;
	for(int iIdx = 0; iIdx < iSize; iIdx++){
		//LOGE("inferFreq(), mlstFreqRec[%d] = %s",iIdx, mlstFreqRec[iIdx]->toString().c_str());
		curCode = &(mlstFreqRec[iIdx]->mstrCode);
		if(NULL == strLastCheck){
			strLastCheck = curCode;
			continue;
		}

		if(0 != curCode->compare(*strLastCheck)){
			//LOGE("inferFreq(), [%s] and [%s] are diff",curCode->c_str(), strLastCheck->c_str());
			bDiff = true;
		}

		strLastCheck = curCode;
	}

	if(false == bDiff){
		if(0!=SoundPair_Config::MISSING_CHAR.compare(*curCode)){
			LOGD("inferFreq()**, all the same, code = [%s] ", curCode->c_str());
			setParameters(frFirst->mlTs, frLast->mlTs, *curCode);
		}else{
			LOGD("inferFreq()**, all the same, code = [%s], replace it", curCode->c_str());
			setParameters(frFirst->mlTs, frLast->mlTs, strReplaced);
		}
	}else{
		int iMedIdx = iSize/2;
		Ref<FreqRecord> frMiddle = mlstFreqRec[iMedIdx];
		if(0 != SoundPair_Config::MISSING_CHAR.compare(frMiddle->mstrCode)){
			//Check back-half items first
			bDiff = false;
			curCode = strLastCheck = &frMiddle->mstrCode;

			for(int idx = iMedIdx +1; idx < iSize; idx++){
				curCode = &mlstFreqRec[idx]->mstrCode;
				if(!strLastCheck && (0!=curCode->compare(*strLastCheck))){
					bDiff = true;
				}
				strLastCheck = curCode;
			}

			if(false == bDiff && !curCode && (0!=SoundPair_Config::MISSING_CHAR.compare(*curCode))){
				LOGD("inferFreq()**, all the same of back-half, code = [%s]", curCode->c_str());
				setParameters(frFirst->mlTs, frLast->mlTs, *curCode);
			}else{
				//Check front-half items
				bDiff = false;
				curCode = strLastCheck = &frMiddle->mstrCode;

				for(int idx = iMedIdx -1; idx >= 0; idx--){
					curCode = &mlstFreqRec[idx]->mstrCode;
					if(!strLastCheck && !strLastCheck && (0!=curCode->compare(*strLastCheck))){
						bDiff = true;
					}
					strLastCheck = curCode;
				}

				if(false == bDiff && !curCode && (0!=SoundPair_Config::MISSING_CHAR.compare(*curCode))){
					LOGD("inferFreq()**, all the same of first-half, code = [%s]", curCode->c_str());
					setParameters(frFirst->mlTs, frLast->mlTs, *curCode);
				}else{
					if((0==mlstFreqRec[0]->mstrCode.compare(mlstFreqRec[iSize-1]->mstrCode)) && (0!=mlstFreqRec[0]->mstrCode.compare(SoundPair_Config::MISSING_CHAR))){
						LOGD("inferFreq()**, pick side items, code = [%s] ", mlstFreqRec[0]->mstrCode.c_str());
						setParameters(frFirst->mlTs, frLast->mlTs, mlstFreqRec[0]->mstrCode);
					}else{
						LOGD("inferFreq()**, pick middle items, code = [%s] ", frMiddle->mstrCode.c_str());
						setParameters(frFirst->mlTs, frLast->mlTs, frMiddle->mstrCode);
					}
				}
			}
		}else{
			//if middle is missing char
			string* strCodeInfer = NULL;
			for(int idx = iSize - 1; idx >= 0; idx--){
				curCode = &mlstFreqRec[idx]->mstrCode;
				if((0!=curCode->compare(SoundPair_Config::MISSING_CHAR))){
					strCodeInfer = curCode;
					break;
				}
			}
			LOGD("inferFreq()**, pick non MISSING_CHAR one, code = [%s]", strCodeInfer->c_str());
			setParameters(frFirst->mlTs, frLast->mlTs, (NULL == strCodeInfer)?strReplaced:*strCodeInfer);
		}
	}
}


string CodeRecord::toString() {
	std::stringstream s;
	s<<"CodeRecord [lStartTs=" << lStartTs << ", lEndTs=" << lEndTs
			<< ", strCdoe=" << strCdoe << ", mlstFreqRec=["/* << mlstFreqRec << "]\n"*/;
	int iSize = mlstFreqRec.size();
	if(0 < iSize){
		for(int i = 0; i< iSize;i++){
			Ref<FreqRecord> fRec = mlstFreqRec.at(i);
			if(fRec){
				s<<"\n["<<fRec->toString()<<"]";
			}
		}
		s<<"\n]";
	}else
		s<<"null]";
	return s.str();
}

bool CodeRecord::isSameCode(){
	bool bRet = true;
	int iSize = mlstFreqRec.size();
	for(int idx = 0; idx+1 < iSize; idx++){
		if((NULL != mlstFreqRec[idx]) && (0==mlstFreqRec[idx]->mstrCode.compare(SoundPair_Config::MISSING_CHAR)) || (NULL != mlstFreqRec[idx+1] && 0!=mlstFreqRec[idx]->mstrCode.compare(mlstFreqRec[idx+1]->mstrCode))){
			bRet = false;
		}
	}
	return bRet;
}

Ref<CodeRecord> CodeRecord::combineNewCodeRecord(Ref<CodeRecord> cr1, Ref<CodeRecord> cr2, int iOffset, int iLastTone){
	Ref<CodeRecord> crRet;
	//LOGI("combineNewCodeRecord()++");
	std::vector<Ref<FreqRecord> > lstFreqRec;
	if(0 < iOffset){
		if(NULL != cr1){
			for(int i =  iOffset; i < cr1->mlstFreqRec.size();i++){
				lstFreqRec.push_back(cr1->mlstFreqRec[i]->recheckToneCode(iLastTone));
			}
		}

		if(NULL != cr2){
			for(int i =  0; i < iOffset && i < cr2->mlstFreqRec.size();i++){
				lstFreqRec.push_back(cr2->mlstFreqRec[i]->recheckToneCode(iLastTone));
			}
		}

		crRet = Ref<CodeRecord>(new CodeRecord(lstFreqRec, cr1->strReplaced));
	}else if(0 > iOffset){
		if(NULL != cr1){
			for(int i =  cr1->mlstFreqRec.size() + iOffset; 0 <= i && i < cr1->mlstFreqRec.size();i++){
				lstFreqRec.push_back(cr1->mlstFreqRec[i]->recheckToneCode(iLastTone));
			}
		}

		if(NULL != cr2){
			for(int i =  0; i < (cr2->mlstFreqRec.size()+iOffset) && i < cr2->mlstFreqRec.size();i++){
				lstFreqRec.push_back(cr2->mlstFreqRec[i]->recheckToneCode(iLastTone));
			}
		}
		crRet = Ref<CodeRecord>(new CodeRecord(lstFreqRec, cr2->strReplaced));
	}else{
		lstFreqRec = cr1->mlstFreqRec;
		crRet = cr1;
	}
	//LOGI("combineNewCodeRecord()1");

	//Fill the missing items
	if(lstFreqRec.size()  < SoundPair_Config::TONE_FRAME_COUNT){
		int iSize = lstFreqRec.size();
		if(0 < iSize){
			if(NULL == cr1){
				msec_t lSesBeginTs = lstFreqRec[0]->mlTs;
				for(int idx = 0; idx < SoundPair_Config::TONE_FRAME_COUNT-iSize; idx++){
					msec_t lTsByIdx = lSesBeginTs - (idx+1)*SoundPair_Config::FRAME_TS;
					lstFreqRec.insert(lstFreqRec.begin(),  Ref<FreqRecord>(new FreqRecord(lTsByIdx, -1.0f, SoundPair_Config::MISSING_CHAR, -1, NULL)));
					LOGI("combineNewCodeRecord()**, ====>>>> add missing char to lstFreqRec to head at %d",(lTsByIdx));
				}
			}else if(NULL == cr2){
				msec_t lSesTailTs = lstFreqRec[iSize-1]->mlTs;
				for(int idx = 0; idx < SoundPair_Config::TONE_FRAME_COUNT-iSize; idx++){
					msec_t lTsByIdx = lSesTailTs + (idx+1)*SoundPair_Config::FRAME_TS;
					lstFreqRec.push_back( Ref<FreqRecord>(new FreqRecord(lTsByIdx, -1.0f, SoundPair_Config::MISSING_CHAR, -1, NULL)));
					LOGI("combineNewCodeRecord()**, ====>>>> add missing char to lstFreqRec to tail at %d",(lTsByIdx));
				}
			}
		}
	}
	//LOGI("combineNewCodeRecord()--");
	return crRet;
}

#endif
