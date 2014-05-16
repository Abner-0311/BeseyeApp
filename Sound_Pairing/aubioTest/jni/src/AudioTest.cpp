#include "AudioTest.h"
#include "simple_websocket_mgr.h"

#ifdef CAM_ENV
#include "http_cgi.h"
#endif

AudioTest* AudioTest::sAudioTest=NULL;

std::vector<std::string> &split(const std::string &str, const std::string &strdelim, std::vector<std::string> &elems) {
	LOGI("strdelim:[%s]\n",(strdelim.c_str())?strdelim.c_str():"");
	std::stringstream ss(str);

    std::string item;
    std::string::size_type pos, lastPos = 0;

//    int iLen = str.length();
//    int i =0;
//    for(i = 0; i< iLen;i++){
//    	LOGI("str[%d] = 0x%x\n",i, str.c_str()[i]);
//    	if(str.c_str()[i] == delim){
//    		LOGI("find delim:[%d]\n",i);
//    	}
//    }
    int iLenDelim = strdelim.length();
    while(true){
	  pos = str.find(strdelim, lastPos);
	  if(pos == std::string::npos){
		 pos = str.length();

		 if(pos != lastPos){
//			tokens.push_back(ContainerT::value_type(str.data()+lastPos,
//				  (ContainerT::value_type::size_type)pos-lastPos ));
			 item = str.substr(lastPos, (pos-lastPos));
			 elems.push_back(item);
			 LOGI("item:[%s]\n",item.c_str());
		 }

		 break;
	  }else{
		 if(pos != lastPos){
//			tokens.push_back(ContainerT::value_type(str.data()+lastPos,
//				  (ContainerT::value_type::size_type)pos-lastPos ));
			 item = str.substr(lastPos, (pos-lastPos));
			 elems.push_back(item);
			 LOGI("item:[%s]\n",item.c_str());
		 }
	  }
	  lastPos = pos + iLenDelim;
	}
    return elems;
}

std::vector<std::string> split(const std::string &s, const std::string &strdelim) {
    std::vector<std::string> elems;
    split(s, strdelim, elems);
    return elems;
}

std::vector<std::string> split(const std::string &s, char delim) {
	LOGI("delim:[0x%x]\n",delim);
    std::vector<std::string> elems;
    std::stringstream ssDelim;
    ssDelim<<delim;
    split(s, ssDelim.str(), elems);
    return elems;
}
#ifdef ANDROID
void soundpairSenderCb(const char* cb_type, void* data){
	AudioTest::getInstance()->soundpairSenderCallback(cb_type, data);
}

void AudioTest::soundpairSenderCallback(const char* cb_type, void* data){
	LOGE( "cb_type:[%s]\n", (cb_type)?cb_type:"");
	if(NULL != cb_type){
		string strMsg(cb_type);
		int iVolStartIdx = strMsg.find(SoundPair_Config::BT_MSG_SET_VOLUME);
		if(0 == iVolStartIdx){
			int iEndIdx = strMsg.find(SoundPair_Config::BT_MSG_SET_VOLUME_END);
			if(0 < iEndIdx && iEndIdx > iVolStartIdx){
				string strVol = strMsg.substr(iVolStartIdx, (iEndIdx - iVolStartIdx));
				//parse it
				LOGI( "soundpairSenderCallback(), strVol:[%s]\n", (strVol.c_str())?strVol.c_str():"");
			}
		}

		if(0 == strMsg.compare(MSG_WS_CONNECTING)){
			Delegate_WSConnecting(mstrCamWSServerIP);
		}else if(0 == strMsg.compare(MSG_WS_CONNECTED)){
			Delegate_WSConnected(mstrCamWSServerIP);
		}else if(0 == strMsg.compare(MSG_WS_CLOSED)){
			Delegate_WSClosed(mstrCamWSServerIP);
		}else{
			std::vector<std::string> msg = split(strMsg, SoundPair_Config::BT_MSG_DIVIDER);
			if(msg.size() == 2){
				LOGI( "soundpairSenderCallback(), bIsSenderMode, msg[0] = [%s], msg[1] = [%s]",msg[0].c_str(), msg[1].c_str());
				if(0 == msg[0].compare(SoundPair_Config::BT_MSG_ACK) && 0 == mstrCurTransferTs.compare(msg[1])){

					if(0 == mstrCurTransferCode.find(SoundPair_Config::BT_MSG_PURE)){
						//FreqGenerator.getInstance().playCode2(mstrCurTransferCode.substring(BT_MSG_PURE.length()), false);
						//playCode(mstrCurTransferCode.substring(SoundPair_Config::BT_MSG_PURE.length()), false);

					}else{
						//FreqGenerator.getInstance().playCode2(mstrCurTransferCode, true);
						//playCode(mstrCurTransferCode, true);
						FreqGenerator::getInstance()->playCode2(mstrCurTransferCode, true);
					}
					mstrCurTransferCode = "";
					mstrCurTransferTs = "";
					mbSenderAcked = false;
					//resetBTParams();
				}else{
					Delegate_TestRoundBegin();
					mstrCurTransferTs = msg[0];
					mstrCurTransferCode = msg[1];
					char msgSent[1024]={0};
					sprintf(msgSent,
							SoundPair_Config::BT_MSG_FORMAT_SENDER.c_str(),
							SoundPair_Config::BT_MSG_ACK.c_str(),
							mstrCurTransferTs.c_str(),
							FreqGenerator::getECCode(mstrCurTransferCode).c_str());

					int iRet = send_msg_to_server(msgSent);
					LOGE("soundpairSenderCallback(), send_msg_to_server, iRet:[%d]\n", iRet);
					//sendBTMsg(String.format(BT_MSG_FORMAT, BT_MSG_ACK, mstrCurTransferTs));
				}
			}else if(msg.size() == 3){
				//LOGI( "soundpairSenderCallback(), msg[0] = [%s], msg[1] = [%s], msg[2] = [%s]",msg[0].c_str(), msg[1].c_str(), msg[2].c_str());
				if(0 == SoundPair_Config::MSG_TEST_ROUND_RESULT.compare(msg[0])){
					Delegate_TestRoundEnd(msg[1],msg[2]);
				}
			}
		}
	}
}
#endif

void soundpairReceiverCb(const char* cb_type, void* data){
	AudioTest::getInstance()->soundpairReceiverCallback(cb_type, data);
}

void AudioTest::soundpairReceiverCallback(const char* cb_type, void* data){//cam ws server side
	LOGE( "cb_type:[%s]\n", (cb_type)?cb_type:"");
	if(NULL != cb_type){
		string strMsg(cb_type);
		if(0 == strMsg.compare(SoundPair_Config::MSG_AUTO_TEST_BEGIN)){
			tmpRet.str("");
			tmpRet.clear();
			FreqAnalyzer::getInstance()->endToTrace();
			FreqAnalyzer::getInstance()->reset();
			resetBuffer();
			pthread_mutex_lock(&mAutoTestCtrlObj);
			LOGI("soundpairReceiverCallback(), broadcast, mbAutoTestBeginOnReceiver=[true]\n");

			mbAutoTestBeginOnReceiver = true;
#ifdef CAM_ENV
			Delegate_BeginToSaveResult();
#endif
			pthread_cond_broadcast(&mAutoTestCtrlObjCond);
			pthread_mutex_unlock(&mAutoTestCtrlObj);
		}else if((0 == strMsg.compare(SoundPair_Config::MSG_AUTO_TEST_END) || 0 == strMsg.compare(MSG_WS_CLOSED)) && mbAutoTestBeginOnReceiver){
			FreqAnalyzer::getInstance()->endToTrace();
			pthread_mutex_lock(&mAutoTestCtrlObj);
			LOGI("soundpairReceiverCallback(),  mbAutoTestBeginAnalyzeOnReceiver=[false]\n");
#ifdef CAM_ENV
			Delegate_EndToSaveResult();
#endif
			setAutoTestBeginAnalyzeOnReceiver(false);
			mbAutoTestBeginOnReceiver = false;
			mbSenderAcked = true;
			pthread_mutex_unlock(&mAutoTestCtrlObj);

			pthread_mutex_lock(&mSyncObj);
			pthread_cond_broadcast(&mSyncObjCond);
			pthread_mutex_unlock(&mSyncObj);
		}else{
			if(mbAutoTestBeginOnReceiver){
				std::vector<std::string> msg = split(strMsg, SoundPair_Config::BT_MSG_DIVIDER);
				if(msg.size() == 3){
					LOGI( "soundpairReceiverCallback(), bIsSenderMode, msg[0] = [%s], msg[1] = [%s], msg[2] = [%s]\n",msg[0].c_str(), msg[1].c_str(), msg[2].c_str());
					if(0 == msg[0].compare(SoundPair_Config::BT_MSG_ACK) && 0 == mstrCurTransferTs.compare(msg[1])){
						curECCode = msg[2];
						curEncodeMark = curCode+curECCode;

						pthread_mutex_lock(&mAutoTestCtrlObj);
						setAutoTestBeginAnalyzeOnReceiver(true);
						LOGI("soundpairReceiverCallback(), broadcast, mbAutoTestBeginAnalyzeOnReceiver=[true]\n");
						pthread_cond_broadcast(&mAutoTestCtrlObjCond);
						pthread_mutex_unlock(&mAutoTestCtrlObj);

						char msgSent[1024]={0};
						sprintf(msgSent, SoundPair_Config::BT_MSG_FORMAT.c_str(), SoundPair_Config::BT_MSG_ACK.c_str(), mstrCurTransferTs.c_str());
						int iRet = send_msg_to_client(msgSent);
						LOGI("soundpairReceiverCallback(), send_msg_to_client, iRet=[%d]\n", iRet);

						pthread_mutex_lock(&mSendPairingCodeObj);
						LOGI("soundpairReceiverCallback(), broadcast, mstrCurTransferCode=[%s]\n", mstrCurTransferCode.c_str());
						mbSenderAcked = true;
						pthread_cond_broadcast(&mSendPairingCodeObjCond);
						pthread_mutex_unlock(&mSendPairingCodeObj);
					}
				}
			}else{
				LOGI("soundpairReceiverCallback(), broadcast, mbAutoTestBeginOnReceiver=[false]\n");
			}
		}
	}
}

void AudioTest::sendPlayPairingCode(string strCode){
	struct timespec outtime;

	LOGI("sendPlayPairingCode(), strCode=%s\n", strCode.c_str());
	if(0 == mstrCurTransferCode.length()){
		mbSenderAcked = false;
		do{
			mstrCurTransferCode = strCode;
			char tsSent[128]={0};
			sprintf(tsSent, "%u",time_ms());
			mstrCurTransferTs = tsSent;

			char msgSent[1024]={0};
			sprintf(msgSent, SoundPair_Config::BT_MSG_FORMAT.c_str(), mstrCurTransferTs.c_str(), mstrCurTransferCode.c_str());
			int iRet = send_msg_to_client(msgSent);
			LOGI("sendPlayPairingCode(), send_msg_to_client, iRet=[%d]\n", iRet);

			pthread_mutex_lock(&mSendPairingCodeObj);
			getTimeSpecByDelay(outtime, 5000);
			LOGI("sendPlayPairingCode(), begin wait, mstrCurTransferCode=[%s]\n", mstrCurTransferCode.c_str());
			pthread_cond_timedwait(&mSendPairingCodeObjCond, &mSendPairingCodeObj, &outtime);
			LOGI("sendPlayPairingCode(), exit wait, mbSenderAcked=%d\n", mbSenderAcked);
			if(mbSenderAcked){
				AudioBufferMgr::getInstance()->recycleAllBuffer();
			}
			pthread_mutex_unlock(&mSendPairingCodeObj);
		}while(!mbSenderAcked && is_websocket_server_inited() && !mbStopControlThreadFlag);

		mstrCurTransferCode = "";
		mstrCurTransferTs = "";
		mbSenderAcked = false;
	}
}

AudioTest::AudioTest():
mIsSenderMode(false),
mIsReceiverMode(false),
mbStopControlThreadFlag(false),
mbStopBufRecordFlag(false),
mbStopAnalysisThreadFlag(false),
mbNeedToResetFFT(false),
mbSenderAcked(false),
mbAutoTestBeginOnReceiver(false),
mbAutoTestBeginAnalyzeOnReceiver(false),
miDigitalToTest(0),
mControlThread(0),
mBufRecordThread(0),
mAnalysisThread(0),
mstrCamWSServerIP(CAM_URL),
miCamWSServerPort(CAM_WS_PORT),
miPairingReturnCode(-1),
mbPairingAnalysisMode(false),
mbAboveThreshold(false){
	pthread_mutex_init(&mSyncObj, NULL);
	pthread_cond_init(&mSyncObjCond, NULL);

	pthread_mutex_init(&mSendPairingCodeObj, NULL);
	pthread_cond_init(&mSendPairingCodeObjCond, NULL);

	pthread_mutex_init(&mAutoTestCtrlObj, NULL);
	pthread_cond_init(&mAutoTestCtrlObjCond, NULL);

	pthread_mutex_init(&mThresholdCtrlObj, NULL);
	pthread_cond_init(&mThresholdCtrlObjCond, NULL);


	FreqAnalyzer::initAnalysisParams(SoundPair_Config::SAMPLE_RATE_REC,
									SoundPair_Config::FRAME_SIZE_REC,
									SoundPair_Config::NOISE_SUPPRESS_INDEX,
									SoundPair_Config::AGC_LEVEL,
									SoundPair_Config::ENABLE_DEVERB,
									SoundPair_Config::DEVERB_DECAY,
									SoundPair_Config::DEVERB_LEVEL);
	bufSegment = ArrayRef<short>(new Array<short>(SoundPair_Config::FRAME_SIZE_REC));
}

AudioTest::~AudioTest(){
//	if(bufSegment){
//		delete[] bufSegment;
//		bufSegment = NULL;
//	}

	pthread_cond_destroy(&mThresholdCtrlObjCond);
	pthread_mutex_destroy(&mThresholdCtrlObj);

	pthread_cond_destroy(&mAutoTestCtrlObjCond);
	pthread_mutex_destroy(&mAutoTestCtrlObj);

	pthread_cond_destroy(&mSendPairingCodeObjCond);
	pthread_mutex_destroy(&mSendPairingCodeObj);

	pthread_cond_destroy(&mSyncObjCond);
	pthread_mutex_destroy(&mSyncObj);

	deinit_websocket_server();
}

AudioTest* AudioTest::getInstance(){
	if(!sAudioTest)
		sAudioTest = new AudioTest();
	return sAudioTest;
}
bool AudioTest::destroyInstance(){
	if(sAudioTest){
		delete sAudioTest;
		sAudioTest = NULL;
		return true;
	}
	return false;
}

bool AudioTest::setSenderMode(){
	LOGI("setSenderMode()+\n");
#ifdef ANDROID
	connectCamCamWSServer();
#endif
	stopAutoTest();
	mIsSenderMode = true;
	mIsReceiverMode = false;
	return true;
}

bool AudioTest::setReceiverMode(bool bAutoTest){
	LOGI("setReceiverMode()+\n");
	if(bAutoTest)
		init_websocket_server(soundpairReceiverCb);

	stopAutoTest();
	mIsSenderMode = false;
	mIsReceiverMode = true;
	return true;
}

bool AudioTest::setAutoTestMode(){
	LOGI("setAutoTestMode()+\n");
	stopAutoTest();
	mIsSenderMode = false;
	mIsReceiverMode = false;
	return true;
}

bool AudioTest::isSenderMode(){
	return mIsSenderMode && !mIsReceiverMode;
}

bool AudioTest::isReceiverMode(){
	return !mIsSenderMode && mIsReceiverMode;
}

bool AudioTest::isAutoTestMode(){
	return !mIsSenderMode && !mIsReceiverMode;
}

bool AudioTest::startAutoTest(string strInitCode, int iDigitalToTest){
	//LOGI("startAutoTest()+, strInitCode:%s\n", strInitCode.c_str());
	deinitTestRound();

	bool bRet = false;
	if(false == isSenderMode()){
		bRet = startAnalyzeTone();
#ifndef ANDROID
	if(bRet){

		FreqAnalyzer::getInstance()->setIFreqAnalyzeResultCB(this);

		LOGI("startAutoTest()+, bRet:%d, isReceiverMode():%d, isAutoTestMode():%d\n",bRet, isReceiverMode(), isAutoTestMode());
		if(bRet && (isReceiverMode() || isAutoTestMode()))
				bRet = startGenerateTone(strInitCode, iDigitalToTest);
		//sleep(1);
		LOGI("startAutoTest(), begin join mBufRecordThread\n");
		pthread_join(mBufRecordThread, NULL);
		LOGI("startAutoTest(), begin join mAnalysisThread\n");
		pthread_join(mAnalysisThread, NULL);
		LOGE("startAutoTest(), end join\n");
	}
#endif
	}else{
		int iRet = send_msg_to_server(SoundPair_Config::MSG_AUTO_TEST_BEGIN.c_str());
		LOGE("startAutoTest(), send_msg_to_server, iRet:[%d]\n", iRet);
		bRet = true;
	}

#ifdef ANDROID
	LOGI("startAutoTest()+, bRet:%d, isReceiverMode():%d, isAutoTestMode():%d\n",bRet, isReceiverMode(), isAutoTestMode());
	if(bRet && (isReceiverMode() || isAutoTestMode()))
		bRet = startGenerateTone(strInitCode, iDigitalToTest);
#endif
EXIT:
	LOGE("startAutoTest()--\n");
	return bRet;
}

bool AudioTest::startPairingAnalysis(){
	deinitTestRound();
	mbPairingAnalysisMode = true;
	bool bRet = false;
	if(false == isSenderMode()){
		bRet = startAnalyzeTone();
#ifndef ANDROID
		if(bRet){
			miPairingReturnCode = -1;
			AudioBufferMgr::getInstance()->setRecordMode(true);
			FreqAnalyzer::getInstance()->setIFreqAnalyzeResultCB(this);
			LOGI("startAutoTest(), begin join mBufRecordThread\n");
			pthread_join(mBufRecordThread, NULL);
			LOGI("startAutoTest(), begin join mAnalysisThread\n");
			pthread_join(mAnalysisThread, NULL);
			LOGE("startAutoTest(), end join\n");
		}
#endif
	}

	LOGE("startPairingAnalysis()--\n");
	return bRet;
}

bool AudioTest::stopAutoTest(){
	bool bRet = false;
	if(isSenderMode()){
		int iRet = send_msg_to_server(SoundPair_Config::MSG_AUTO_TEST_END.c_str());
		LOGE("stopAutoTest(), send_msg_to_server, iRet:[%d]\n", iRet);
	}
	stopGenerateTone();
	stopAnalyzeTone();
	deinitTestRound();
	return bRet;
}

bool AudioTest::playTone(string strCode, bool bNeedEncode){
#ifndef CAM_ENV
	FreqGenerator::getInstance()->setOnPlayToneCallback(this);
	FreqGenerator::getInstance()->playCode2(strCode, bNeedEncode);
#endif
}

bool AudioTest::startGenerateTone(string strInitCode, int iDigitalToTest){
	bool bRet = false;
	int errno = 0;
	//LOGE("AudioTest::startGenerateTone(),  mControlThread,%d\n", mControlThread);
	if(!mControlThread){
		this->strInitCode = "0123456789abcdef";//strInitCode;
		miDigitalToTest = iDigitalToTest;
		if (0 != (errno = pthread_create(&mControlThread, NULL, AudioTest::runAutoTestControl, this))) {
			LOGE("AudioTest::startAutoTet(), error when create mControlThread,%d\n", errno);
		}else{
			LOGE("AudioTest::startAutoTet(), create mControlThread,%d\n", errno);
			bRet = true;
#ifdef ANDROID
			pthread_setname_np(mControlThread, "ControlThread");
#endif	
		}
	}
EXIT:
	return bRet;
}

bool AudioTest::stopGenerateTone(){
	LOGE("stopGenerateTone()+\n");
	mbStopControlThreadFlag = true;
	return true;
}

bool AudioTest::startAnalyzeTone(){
	bool bRet = false;
	int errno = 0;
	if(!mBufRecordThread){
		if (0 != (errno = pthread_create(&mBufRecordThread, NULL, AudioTest::runAudioBufRecord, this))) {
			LOGE("AudioTest::startAutoTet(), error when create mBufRecordThread,%d\n", errno);
		}else{
			bRet = true;
#ifdef ANDROID
			pthread_setname_np(mBufRecordThread, "BufRecordThread");
#endif
		}
	}

	if(bRet && !mAnalysisThread){
		if (0 != (errno = pthread_create(&mAnalysisThread, NULL, AudioTest::runAudioBufAnalysis, this))) {
			LOGE("AudioTest::startAutoTet(), error when create mAnalysisThread,%d\n", errno);
		}else{
			bRet = true;
#ifdef ANDROID
			pthread_setname_np(mAnalysisThread, "AnalysisThread");
#endif	
		}
	}
EXIT:
	return bRet;
}

bool AudioTest::stopAnalyzeTone(){
	LOGD("stopAnalyzeTone()+\n");
	mbStopBufRecordFlag = true;
	mbStopAnalysisThreadFlag = true;
#ifdef CAM_ENV
	stopReceiveAudioBuf();
#endif
	return true;
}

#ifdef CAM_ENV
int getRandomNumDigit(int iMin, int iMax){
	return iMin + rand() % (iMax - iMin);
}
string genNextRandomData(int iMinDigit){
	//LOGI("genNextRandomData(), iMinDigi=%d", iMinDigit);
	stringstream strRet;
	int iDivision = SoundPair_Config::getDivisionByFFTYPE();
//
//	int iMaxDigit = min(SoundPair_Config::MAX_ENCODE_DATA_LEN*SoundPair_Config::getMultiplyByFFTYPE(), (int) ((pow(2.0, (double)(SoundPair_Config::getPowerByFFTYPE()*SoundPair_Config::getMultiplyByFFTYPE()) -1 ))* 0.6666666666666f));
//
//	//LOGI("genNextRandomData(), iDivision:%d, iMaxDigit=%d", iDivision, iMaxDigit);
//
//	int iLen = getRandomNumDigit(iMinDigit, iMaxDigit)*SoundPair_Config::getMultiplyByFFTYPE();
//
//	//LOGI("genNextRandomData(), iLen:%d, iMaxDigit=%d", iLen, iMaxDigit);
//	//Log.e(TAG, "genNextRandomData(), iMaxDigit= "+iMaxDigit+", iLen="+iLen );
//
//	for(int i =0;i<iLen;i++){
//		strRet<<(SoundPair_Config::sCodeTable.at(rand() % (iDivision)));
//	}

	static const int MAC_ADDR_LEN = 12;
	for(int i =0;i<MAC_ADDR_LEN;i++){
		strRet<<(SoundPair_Config::sCodeTable.at(rand() % (iDivision)));
	}
	//strRet<<SoundPair_Config::PAIRING_DIVIDER;
	strRet<<"1b";

	LOGE("genNextRandomData()1, strRet:[%s]\n", strRet.str().c_str());

	//int iLenPW = getRandomNumDigit(8, 64)*iDivision;
	int iLenPW = getRandomNumDigit(8, 16)*SoundPair_Config::getMultiplyByFFTYPE();

	for(int i =0;i<iLenPW;i++){
		strRet<<(SoundPair_Config::sCodeTable.at(rand() % (iDivision)));
	}
	strRet<<"1b";

	LOGE("genNextRandomData()2, strRet:[%s]\n", strRet.str().c_str());

	static const int TOKEN_LEN = 4;
	for(int i =0;i<TOKEN_LEN;i++){
		strRet<<(SoundPair_Config::sCodeTable.at(rand() % (iDivision)));
	}
	LOGE("genNextRandomData()3, strRet:[%s]\n", strRet.str().c_str());
	LOGE("genNextRandomData(), iLenPW:[%d], length:%d\n", iLenPW, strRet.str().length());
	return strRet.str();
}
#endif

void* AudioTest::runAutoTestControl(void* userdata){
//#ifndef CAM_ENV
	LOGE("runAutoTestControl()+\n");
	AudioTest* tester = (AudioTest*)userdata;
	if(tester){
		const bool bIsSenderMode = tester->isSenderMode();
		const bool bIsReceiverMode = tester->isReceiverMode();
		const bool bIsAutoTestMode = !bIsSenderMode && !bIsReceiverMode;

		tester->mbStopControlThreadFlag = false;

		FreqAnalyzer::getInstance()->setSenderMode(/*isSenderMode*/false);
		FreqAnalyzer::getInstance()->setIFreqAnalyzeResultCB(tester);
	#ifndef CAM_ENV
		if(!bIsReceiverMode)
			FreqGenerator::getInstance()->setOnPlayToneCallback(tester);
	#endif
		//char *nativeString = (char *)jni_env->GetStringUTFChars( strCurCode, 0);
		tester->curCode = tester->strInitCode;
		LOGE("runAutoTestControl()+, strCurCode:%s\n", (tester->curCode.c_str())?tester->curCode.c_str():"null");
		//jni_env->ReleaseStringUTFChars( strCurCode, nativeString);

	#ifdef CAM_ENV
		while(0 < (tester->curCode = genNextRandomData(tester->miDigitalToTest)).length()){
	#else
		while(0 < (tester->curCode = FreqGenerator::genNextRandomData(tester->miDigitalToTest)).length()){
	#endif
			LOGE("runAutoTestControl+, tester->mbStopControlThreadFlag:%d\n", tester->mbStopControlThreadFlag);
			if(tester->mbStopControlThreadFlag){
				LOGE("runAutoTestControl(), break loop\n");
	#ifndef CAM_ENV
				FreqGenerator::getInstance()->stopPlay2();
	#endif
				break;
			}

			if(bIsAutoTestMode){
				//LOGE("runAutoTestControl+, FreqGenerator::getInstance() is %d\n", FreqGenerator::getInstance());

		//		if(SELF_TEST)
		//			FreqGenerator.getInstance().playCode3(/*lstTestData.get(i)*/curCode, true);
		//		else
	#ifndef CAM_ENV
					FreqGenerator::getInstance()->playCode2(tester->curCode, true);
	#endif
			}else{
	#ifndef CAM_ENV
				tester->curECCode = FreqGenerator::getECCode(tester->curCode);
				tester->curEncodeMark = /*SoundPair_Config::encodeConsecutiveDigits*/(tester->curCode+tester->curECCode);
	#endif

	#ifdef CAM_ENV
				while(bIsReceiverMode && !tester->mbAutoTestBeginOnReceiver && !tester->mbStopControlThreadFlag){
					LOGI("runAutoTestControl(), begin wait auto test\n");
					pthread_mutex_lock(&tester->mAutoTestCtrlObj);
					pthread_cond_wait(&tester->mAutoTestCtrlObjCond, &tester->mAutoTestCtrlObj);
					pthread_mutex_unlock(&tester->mAutoTestCtrlObj);
					LOGD("runAutoTestControl(), exit wait auto test\n");
				}


				tester->sendPlayPairingCode(tester->curCode);

	#else
				Delegate_SendMsgByBT(tester->curCode); // 990 digits max
	#endif
			}

			if(tester->mbStopControlThreadFlag){
				LOGE("runAutoTestControl(), break loop2\n");
	#ifndef CAM_ENV
				FreqGenerator::getInstance()->stopPlay2();
	#endif
				break;
			}else if(!tester->mbAutoTestBeginOnReceiver){
				LOGE("runAutoTestControl(), continue to rewait---\n");
				continue;
			}

			LOGI("runAutoTestControl(), enter lock\n");
			pthread_mutex_lock(&tester->mSyncObj);
			tester->tmpRet.str("");
			tester->tmpRet.clear();
			//LOGI("runAutoTestControl(), beginToTrace\n");
			FreqAnalyzer::getInstance()->beginToTrace(tester->curCode);
			LOGI("runAutoTestControl(), ----------------------------begin wait\n");
			pthread_cond_wait(&tester->mSyncObjCond, &tester->mSyncObj);
			LOGI("runAutoTestControl(), ----------------------------exit wait\n");
			pthread_mutex_unlock(&tester->mSyncObj);
		}

		tester->mControlThread = 0;
	}else{
		LOGE("runAutoTestControl(), tester is null\n");
	}

	Delegate_detachCurrentThread();
	LOGE("runAutoTestControl()---\n");
//#endif
}

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

static ArrayRef<short> shortsRecBuf=NULL;
static int iCurIdx = 0;

static timespec sleepValue = {0};
static msec_t lTsRec = 0;
static int iAudioFrameSize = 4;
static const int MAX_TRIAL = 10;

static const short ANALYSIS_START_THRESHHOLD = 20000;//audio value
static const short ANALYSIS_END_THRESHHOLD   = 15000;//audio value
static const int   ANALYSIS_THRESHHOLD_CK_LEN = 1600;//sample size , about 0.1 sec
static const int   ANALYSIS_AB_THRESHHOLD_CK_CNT = 10;
static const int   ANALYSIS_UN_THRESHHOLD_CK_CNT = 100;
static short sMaxValue = 0;
static int iAboveThreshHoldCount = 0;
static int iUnderThreshHoldCount = 0;
static int iRefCount = 0;

static const int   ANALYSIS_LED_UPDATE_PERIOD = 8000;//sample size , about 0.5 sec
static bool sbLEDOn = false;

typedef enum{
	PAIRING_NONE,
	PAIRING_INIT				,
    PAIRING_WAITING 			,
    PAIRING_ANALYSIS 			,
    PAIRING_ERROR					,
    PAIRING_DONE
}Pairing_Mode;

static Pairing_Mode sPairingMode = PAIRING_INIT;
static Pairing_Mode sPedningPairingMode = PAIRING_NONE;
static const int ERROR_LED_PERIOD = 6;
static int sCurLEDCnt = 0;

void changePairingMode(Pairing_Mode mode){
	if(sPairingMode != mode)
		LOGW("sPairingMode:%d, mode:%d\n", sPairingMode, mode);

	if(PAIRING_ERROR == sPairingMode && sCurLEDCnt <= ERROR_LED_PERIOD){
		LOGW("---sPedningPairingMode:%d\n", sPedningPairingMode);
		sPedningPairingMode = mode;
	}else{
		if(mode == PAIRING_ERROR){
			sCurLEDCnt = 0;
		}
		if(PAIRING_ERROR == sPairingMode){
			sPairingMode = (PAIRING_NONE==sPedningPairingMode)?mode:sPedningPairingMode;
			sPedningPairingMode = PAIRING_NONE;
		}else{
			sPairingMode = mode;
		}
	}

	//LOGW("---sPairingMode:%d\n", sPairingMode);
}

void setLedLight(int bRedOn, int bGreenOn, int bBlueOn){
	static char cmd[BUF_SIZE]={0};
	sprintf(cmd, "/beseye/cam_main/cam-handler -setled %d", ((bRedOn) | (bGreenOn<<1) | (bBlueOn<<2)));
	int iRet = system(cmd);
	//LOGW("cmd:[%s], iRet:%d\n", cmd, iRet);
}

void writeBuf(unsigned char* charBuf, int iLen){
	if(!AudioTest::getInstance()->isPairingAnalysisMode() && !AudioTest::getInstance()->isAutoTestBeginAnalyzeOnReceiver()){
		return;
	}
	//LOGW("writeBuf:%d, iCurIdx:%d", iLen, iCurIdx);
	//short* shortBuf = (short*)audioBufferPinned;
	int iIdxOffset = -iCurIdx;
	msec_t lTs1 = lTsRec;
	int iCountFrame = iLen/iAudioFrameSize;
	for(int i = 0 ; i < iCountFrame; i++){
		if(NULL == shortsRecBuf || 0 == shortsRecBuf->size()){
			lTs1 = (lTsRec+=SoundPair_Config::FRAME_TS);//System.currentTimeMillis();
			int iCurTrial = 0;
			while(NULL == (shortsRecBuf = AudioBufferMgr::getInstance()->getAvailableBuf())){
				nanosleep(&sleepValue, NULL);
				iCurTrial++;
				if(iCurTrial > MAX_TRIAL){
					LOGW("Can not get available buf\n");
					//stop analysis
					AudioTest::getInstance()->setAboveThresholdFlag(false);
					AudioTest::getInstance()->resetBuffer();
					AudioBufferMgr::getInstance()->setRecordMode(true);
					break;
				}
			}
			iIdxOffset = i;
			iCurIdx = 0;
			//LOGE("writeBuf(), get rec buf at %lld, iIdxOffset:%d, iCurIdx:%d, shortsRecBuf->size():%d\n", lTs1, iIdxOffset, iCurIdx, shortsRecBuf->size() );
		}

		if(NULL != shortsRecBuf){
			iCurIdx = i - iIdxOffset;
			//shortsRecBuf[iCurIdx] = ulaw2linear(charBuf[i]);
			shortsRecBuf[iCurIdx] = (((short)charBuf[iAudioFrameSize*i+3])<<8 | (charBuf[iAudioFrameSize*i+2]));

			/*if(AudioTest::getInstance()->isPairingAnalysisMode())*/{
				short val = abs(shortsRecBuf[iCurIdx]);
				if(val > sMaxValue){
					sMaxValue = val;
				}

				if(0 == iRefCount%ANALYSIS_THRESHHOLD_CK_LEN){
					//LOGW("--------------------------------------------------------------------------->sMaxValue:%d, iAboveThreshHoldCount:%d, iUnderThreshHoldCount:%d\n", sMaxValue, iAboveThreshHoldCount, iUnderThreshHoldCount);
					if(ANALYSIS_START_THRESHHOLD < sMaxValue){
						iAboveThreshHoldCount++;
						if(false == AudioTest::getInstance()->getAboveThresholdFlag() && iAboveThreshHoldCount >= ANALYSIS_AB_THRESHHOLD_CK_CNT){
							LOGW("trigger analysis-----\n");
							//trigger analysis
							if(AudioTest::getInstance()->isPairingAnalysisMode()){
								changePairingMode(PAIRING_ANALYSIS);

								AudioBufferMgr::getInstance()->trimAvailableBuf(((ANALYSIS_THRESHHOLD_CK_LEN*ANALYSIS_AB_THRESHHOLD_CK_CNT)/SoundPair_Config::FRAME_SIZE_REC)*3/2);
								AudioBufferMgr::getInstance()->setRecordMode(false);
							}
							AudioTest::getInstance()->setAboveThresholdFlag(true);
							iAboveThreshHoldCount = 0;
						}
						iUnderThreshHoldCount = 0;
					}else if(ANALYSIS_END_THRESHHOLD > sMaxValue){
						iUnderThreshHoldCount++;
						if(AudioTest::getInstance()->getAboveThresholdFlag() && iUnderThreshHoldCount >= ANALYSIS_UN_THRESHHOLD_CK_CNT){
							LOGW("trigger stop analysis-----\n");
							//stop analysis
							if(AudioTest::getInstance()->isPairingAnalysisMode()){
								AudioBufferMgr::getInstance()->setRecordMode(true);
							}
							AudioTest::getInstance()->setAboveThresholdFlag(false);
							FreqAnalyzer::getInstance()->triggerTimeout();
							changePairingMode(PAIRING_WAITING);
							//AudioTest::getInstance()->resetBuffer();
						}
						iAboveThreshHoldCount = 0;
					}else{
						iUnderThreshHoldCount = 0;
						iAboveThreshHoldCount = 0;
					}

					//iRefCount = 0;
					sMaxValue = 0;
				}

				if(AudioTest::getInstance()->isPairingAnalysisMode() && 0 == iRefCount%ANALYSIS_LED_UPDATE_PERIOD){
					//LOGW("sPairingMode is %d-----\n", sPairingMode);

					if(false == sbLEDOn){
						if(PAIRING_WAITING == sPairingMode){
							setLedLight(0,1,0);
						}else if(PAIRING_ANALYSIS == sPairingMode){
							setLedLight(0,0,1);
						}else if(PAIRING_ERROR == sPairingMode){
							setLedLight(1,0,0);
						}else if(PAIRING_DONE == sPairingMode){
							setLedLight(0,1,0);
						}
					}else if(sPairingMode != PAIRING_DONE){
						setLedLight(0,0,0);
					}

					if(PAIRING_ERROR == sPairingMode){
						sCurLEDCnt++;
					}
					sbLEDOn = !sbLEDOn;
				}
				iRefCount++;
			}

			if(iCurIdx == shortsRecBuf->size()-1){
				//LOGE("writeBuf(), add rec buf at %lld, iIdxOffset:%d, iCurIdx:%d, shortsRecBuf->size():%d\n", lTs1, iIdxOffset, iCurIdx, shortsRecBuf->size() );
				AudioBufferMgr::getInstance()->addToDataBuf(lTs1, shortsRecBuf, shortsRecBuf->size());
				shortsRecBuf = NULL;
			}
		}else{
			LOGW("shortsRecBuf is NULL");
		}
	}
	iCurIdx++;
	//Delegate_WriteAudioBuffer2();
}

void AudioTest::setAutoTestBeginAnalyzeOnReceiver(bool flag){
	LOGI("setAutoTestBeginAnalyzeOnReceiver(), ++, flag:%d\n", flag);
	mbAutoTestBeginAnalyzeOnReceiver = flag;
}

void AudioTest::setAboveThresholdFlag(bool flag){
	LOGI("setAboveThresholdFlag(), ++, flag:%d\n", flag);
	pthread_mutex_lock(&mThresholdCtrlObj);
	bool oldflag = mbAboveThreshold;
	mbAboveThreshold=flag;
	if(!oldflag && mbAboveThreshold){
		LOGI("setAboveThresholdFlag(), broadcast\n");
		pthread_cond_broadcast(&mThresholdCtrlObjCond);
	}
	pthread_mutex_unlock(&mThresholdCtrlObj);
	LOGI("setAboveThresholdFlag()\n");

}
bool AudioTest::getAboveThresholdFlag(){
	return mbAboveThreshold;
}

void* AudioTest::runAudioBufRecord(void* userdata){
	LOGE("runAudioBufRecord()+\n");
	AudioTest* tester = (AudioTest*)userdata;
	tester->mbStopBufRecordFlag = false;

	lTsRec = 0;
	sleepValue.tv_nsec = 100000;//0.1 ms

#ifndef CAM_ENV
	ArrayRef<short> shortsRec=NULL;

	//timespec sleepValue = {0};
	//sleepValue.tv_nsec = 100000;//0.1 ms
	Delegate_OpenAudioRecordDevice(SoundPair_Config::SAMPLE_RATE_REC, 0);

	while(!tester->mbStopBufRecordFlag){
		msec_t lTs1 = (lTsRec+=SoundPair_Config::FRAME_TS);//System.currentTimeMillis();
		while(NULL == (shortsRec = AudioBufferMgr::getInstance()->getAvailableBuf()) && !tester->mbStopBufRecordFlag){
			nanosleep(&sleepValue, NULL);
		}
		int samplesRead=0;
		if(samplesRead = Delegate_getAudioRecordBuf(shortsRec, SoundPair_Config::FRAME_SIZE_REC)){
			AudioBufferMgr::getInstance()->addToDataBuf(lTs1, shortsRec, samplesRead);
		}else{
			LOGI("runAudioBufRecord, AudioRecord.ERROR_INVALID_OPERATION");
		}

		msec_t lTs2 = (lTsRec+=SoundPair_Config::FRAME_TS);
		while(NULL == (shortsRec = AudioBufferMgr::getInstance()->getAvailableBuf())&& !tester->mbStopBufRecordFlag){
			nanosleep(&sleepValue, NULL);
		}

		//LOGI("record, samplesRead:"+samplesRead);
		if(samplesRead= Delegate_getAudioRecordBuf(shortsRec, SoundPair_Config::FRAME_SIZE_REC)){
			AudioBufferMgr::getInstance()->addToDataBuf(lTs2, shortsRec, samplesRead);
		}else{
			LOGI("runAudioBufRecord, AudioRecord.ERROR_INVALID_OPERATION");
		}
	}
	Delegate_CloseAudioRecordDevice();
#else

	//char* session = "0e4bba41bef24f009337727ce44008cd";//[SESSION_SIZE];
	char session[SESSION_SIZE];
	memset(session, 0, sizeof(session));

	if(GetSession(HOST_NAME, session) != 0) {
		LOGE("Get session failed.");
	}else{
		LOGE("runAudioBufRecord(), begin to GetAudioBufCGI\n");
		changePairingMode(PAIRING_WAITING);
		int res = GetAudioBufCGI(HOST_NAME_AUDIO, "receiveRaw", session, writeBuf);
		LOGE("GetAudioBufCGI:res(%d)\n%s",res);
		//Delegate_CloseAudioDevice2();
	}


#endif
	LOGE("runAudioBufRecord()-\n");
	tester->mBufRecordThread = 0;
	Delegate_detachCurrentThread();
}

void* AudioTest::runAudioBufAnalysis(void* userdata){
	LOGE("runAudioBufAnalysis()+\n");
	AudioTest* tester = (AudioTest*)userdata;
	tester->mbStopAnalysisThreadFlag = false;
	Ref<BufRecord> buf;

	while(!tester->mbStopAnalysisThreadFlag){
		while(!tester->isPairingAnalysisMode() && tester->isReceiverMode() && !tester->isAutoTestBeginAnalyzeOnReceiver() && !tester->mbStopAnalysisThreadFlag){
			LOGI("runAudioBufAnalysis(), begin wait auto test, [%d, %d, %d, %d]\n", tester->isPairingAnalysisMode(), tester->isReceiverMode(), tester->mbAutoTestBeginAnalyzeOnReceiver, tester->mbStopAnalysisThreadFlag);
			pthread_mutex_lock(&tester->mAutoTestCtrlObj);
			pthread_cond_wait(&tester->mAutoTestCtrlObjCond, &tester->mAutoTestCtrlObj);
			pthread_mutex_unlock(&tester->mAutoTestCtrlObj);
			LOGI("runAudioBufAnalysis(), exit wait auto test, [%d, %d, %d, %d]\n", tester->isPairingAnalysisMode(), tester->isReceiverMode(), tester->mbAutoTestBeginAnalyzeOnReceiver, tester->mbStopAnalysisThreadFlag);
		}

		while(tester->isPairingAnalysisMode() && !tester->getAboveThresholdFlag() && !tester->mbStopAnalysisThreadFlag){
			LOGI("runAudioBufAnalysis(), begin wait threshold\n");
			pthread_mutex_lock(&tester->mThresholdCtrlObj);
			pthread_cond_wait(&tester->mThresholdCtrlObjCond, &tester->mThresholdCtrlObj);
			pthread_mutex_unlock(&tester->mThresholdCtrlObj);
			LOGI("runAudioBufAnalysis(), exit wait threshold\n");
		}

		if(tester->mbStopAnalysisThreadFlag){
			LOGI("runAudioBufAnalysis(), break loop\n");
			break;
		}

		//LOGE("runAudioBufAnalysis()+1\n");
		int iSessionOffset = FreqAnalyzer::getInstance()->getSessionOffset();
		LOGD("runAudioBufAnalysis(), iSessionOffset:%d\n", iSessionOffset);

		if(iSessionOffset > 0)
			buf = tester->getBuf((iSessionOffset/SoundPair_Config::FRAME_SIZE_REC)+1);
		else
			buf = tester->getBuf();

		LOGD("runAudioBufAnalysis(), get buf\n");
		ArrayRef<short> bufShort = buf->mbBuf;

		if(0 != iSessionOffset){
			bufShort = AudioBufferMgr::getInstance()->getBufByIndex(buf->miIndex, iSessionOffset, tester->bufSegment);
		}

		float ret = FreqAnalyzer::getInstance()->analyzeAudioViaAudacity(bufShort,
																		 buf->miSampleRead,
																		 tester->mbNeedToResetFFT,
																		 FreqAnalyzer::getInstance()->getLastDetectedToneIdx(buf->mlTs),
																		 buf->miFFTValues);
		LOGD("runAudioBufAnalysis(), iFFTValues=[%d,%d,%d,%d,%d]", buf->miFFTValues[0], buf->miFFTValues[1], buf->miFFTValues[2], buf->miFFTValues[3], buf->miFFTValues[4]);
		msec_t lTs = buf->mlTs;

		FreqAnalyzer::getInstance()->analyze(lTs, ret, buf->miIndex, buf->miFFTValues);
		//LOGE("runAudioBufAnalysis(), analyze out\n");
		Delegate_UpdateFreq(lTs, ret);

		AudioBufferMgr::getInstance()->addToAvailableBuf(buf);
	}

	LOGE("runAudioBufAnalysis()-\n");
	tester->mAnalysisThread=0;
	Delegate_detachCurrentThread();
}

Ref<BufRecord> AudioTest::getBuf(){
	return getBuf(0);
}

Ref<BufRecord> AudioTest::getBuf(int iNumToRest){
	Ref<BufRecord> buf;
	while( !mbStopAnalysisThreadFlag && NULL == (buf=AudioBufferMgr::getInstance()->getDataBuf(iNumToRest))){
		//for self test
		//FreqGenerator::getInstance()->notifySelfTestCond();deadlock

		AudioBufferMgr::getInstance()->waitForDataBuf(2000);//2 seconds
	}
	return buf;
}

void AudioTest::onStartGen(string strCode){
	LOGI("onStartGen() strCode:%s\n",strCode.c_str());
}

void AudioTest::onStopGen(string strCode){
	LOGI("onStopGen() strCode:%s\n",strCode.c_str());
}

void AudioTest::onCurFreqChanged(double dFreq){
	LOGI("onCurFreqChanged() dFreq:%f\n",dFreq);
}

void AudioTest::onErrCorrectionCode(string strCode, string strEC, string strEncodeMark){
	LOGI("onErrCorrectionCode() strCode:%s, strEC:%s, strEncodeMark:%s\n",strCode.c_str(), strEC.c_str(), strEncodeMark.c_str());
	curECCode = strEC;
	curEncodeMark = strEncodeMark;
}

void AudioTest::onDetectStart(){
	Delegate_ResetData();
}

void AudioTest::onDetectPostFix(){
	LOGI("onDetectPostFix()\n");
	if(isPairingAnalysisMode())
		setAboveThresholdFlag(false);

	pthread_mutex_lock(&mAutoTestCtrlObj);
	setAutoTestBeginAnalyzeOnReceiver(false);
	pthread_mutex_unlock(&mAutoTestCtrlObj);
}

void AudioTest::onAppendResult(string strCode){
	tmpRet<<strCode;
}

//#include "delegate/account_mgr.h"
void checkPairingResult(string strCode){
#ifdef CAM_ENV
	LOGE("++, strCode:[%s]\n", strCode.c_str());

	int iMultiply = SoundPair_Config::getMultiplyByFFTYPE();
	int iPower = SoundPair_Config::getPowerByFFTYPE();

	if(0 == strCode.find("error")){
		LOGE("Error\n");
	}else{

		int toDecodeSize = strCode.length()/iMultiply;
		LOGE("toDecodeSize:%d\n", toDecodeSize);
		stringstream retS;
		for(int i =0;i < toDecodeSize;i++){
			unsigned char c = 0;
			for(int j = 0;j < iMultiply;j++){
				c <<= iPower;
				string strTmp = strCode.substr(i*iMultiply+j, 1);
				int iVal = SoundPair_Config::findIdxFromCodeTable(strTmp.c_str());
				c += (unsigned char) iVal;
				//LOGI("iVal:[%d]\n",iVal);
			}
			//LOGI("c:[%u]\n",c);
			retS << c;
		}

		LOGI("retS:[%s]\n",retS.str().c_str());

		std::vector<std::string> ret = split(retS.str(), SoundPair_Config::PAIRING_DIVIDER);

		if(ret.size() == 3){
			string mac = ret[0];
			stringstream retMacAddr;
			int iLenMacAddr = mac.length();

			for(int idx = 0;idx < iLenMacAddr;idx++){
				string tmp("");
				char cDecode = mac.at(idx);
				for(int j = 0;j < iMultiply;j++){
					tmp = SoundPair_Config::sFreqRangeTable.at(cDecode & ((0x1<<iPower) -1))->getCode() + tmp;
					cDecode >>= iPower;
				}
				retMacAddr << tmp;
			}

			string strUserNum = ret[2];
			stringstream retUserToken;
			int iLenUserNum = strUserNum.length();
			for(int idx = 0; idx < iLenUserNum;idx++){
				string tmp("");
				char cDecode = strUserNum.at(idx);
				for(int j = 0;j < iMultiply;j++){
					tmp = SoundPair_Config::sFreqRangeTable.at(cDecode & ((0x1<<iPower) -1))->getCode() + tmp;
					cDecode >>= iPower;
				}
				retUserToken << tmp;
			}

			char cmd[BUF_SIZE]={0};
			sprintf(cmd, "/beseye/cam_main/cam-handler -setwifi %s %s", retMacAddr.str().c_str(), ret[1].c_str());
			LOGI("wifi set cmd:[%s]\n", cmd);
			int iRet = system(cmd) >> 8;
			if(0 == iRet){
				LOGI("wifi set OK\n");
				long lCheckTime = time_ms();
				long lDelta;
				int iNetworkRet = 0;
				do{
					sleep(1);
					iNetworkRet = system("/beseye/cam_main/beseye_network_check") >> 8;
					lDelta = (time_ms() - lCheckTime);
					LOGI("wifi check ret :%d, ts:%u, ccc:%d\n", iNetworkRet, lDelta, ((iNetworkRet != 0) && (15000 > lDelta)));
				}while((iNetworkRet != 0) && (15000 > lDelta));

				LOGI("network checking complete, iNetworkRet:%d, ts:%u\n", iNetworkRet, lDelta);

				if(0 == iNetworkRet){
					LOGI("network connected\n");
					iRet = system("/beseye/cam_main/beseye_token_check") >> 8;
					if(0 == iRet){
						LOGI("Token is already existed, check tmp token\n");
						sprintf(cmd, "/beseye/cam_main/cam-handler -verToken %s %s", retMacAddr.str().c_str(), retUserToken.str().c_str());
						LOGI("verToken cmd:[%s]\n", cmd);
						iRet = system(cmd) >> 8;
						if(0 == iRet){
							LOGI("Tmp User Token verification OK\n");
							AudioTest::getInstance()->setPairingReturnCode(0);
						}else{
							LOGI("Tmp User Token verification failed\n");
							//roll back wifi settings
							iRet = system("/beseye/cam_main/cam-handler -restoreWifi") >> 8;
						}
					}else{
						LOGI("Token is invalid, try to attach\n");
						sprintf(cmd, "/beseye/cam_main/cam-handler -attach %s %s", retMacAddr.str().c_str(), retUserToken.str().c_str());
						LOGI("attach cmd:[%s]\n", cmd);
						iRet = system(cmd) >> 8;
						if(0 == iRet){
							LOGI("Cam attach OK\n");
							iRet = system("/beseye/cam_main/beseye_token_check") >> 8;
							if(0 == iRet){
								LOGI("Token verification OK\n");
								AudioTest::getInstance()->setPairingReturnCode(0);
							}else{
								LOGI("Token verification failed\n");
							}
						}else{
							LOGI("Cam attach failed\n");
						}
					}
				}else{
					LOGI("network disconnected\n");
				}
			}else{
				LOGE("wifi set failed\n");
			}

//			LOGI("sUserId:[%u]\n",sUserId);
//			char testData[BUF_SIZE]={0};
//			if(RET_CODE_OK == bindUserAccount(testData, sUserId)){
//				LOGE("bindUserAccount OK:[%s]\n", testData);
//			}else{
//				LOGE("bindUserAccount Failed:[%s]\n", testData);
//			}
		}else{
			LOGI("failed to parse result, ret.size():[%d]\n",ret.size());
		}
	}
#endif
}

void AudioTest::onSetResult(string strCode, string strDecodeMark, string strDecodeUnmark, bool bFromAutoCorrection, MatchRetSet* prevMatchRet){
	LOGI("onSetResult(), strCode:%s, strDecodeMark = %s\n", strCode.c_str(), strDecodeMark.c_str());
#ifdef CAM_ENV
	if(mbPairingAnalysisMode){
		checkPairingResult(strCode);
		if(0 <= miPairingReturnCode){
			changePairingMode(PAIRING_DONE);
			setLedLight(0,1,0);
			stopAutoTest();
		}else if(bFromAutoCorrection){
			changePairingMode(PAIRING_ERROR);
		}else{
			changePairingMode(PAIRING_ANALYSIS);
		}
	}
#endif

	stringstream strLog;
	if(strCode.length() > 0 || strDecodeMark.length() >0){
		/*if(false == isSenderMode)*/{
			if(0 == strCode.compare(curCode)){
				if(0 == strDecodeUnmark.find(curCode)){
					strLog <<"runAutoTest(), Case 1 ===>>> Detection match before error correction, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"strDecodeMark    = ["<<strDecodeMark<<"]\n"<<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"] \n"<<
							"strCode          = ["<<strCode<<"]\n";
					LOGE("%s\n", strLog.str().c_str());

					Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, DESC_MATCH, bFromAutoCorrection);
				}else{

					strLog <<"runAutoTest(), Case 2 ===>>> Detection match after error correction, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"strDecodeMark    = ["<<strDecodeMark<<"]\n"<<
							"Difference       = ["<<findDifference(curEncodeMark, strDecodeMark)<<"]\n"<<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"] \n"<<
							"strCode          = ["<<strCode<<"]\n";
					LOGE("%s\n", strLog.str().c_str());
					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_EC){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, DESC_MATCH_EC, bFromAutoCorrection);
						}
					}else if(0 > miPairingReturnCode){
						MatchRetSet* matchRet = new MatchRetSet(DESC_MATCH_EC, strDecodeMark, strDecodeUnmark, strCode);
						tmpRet.str("");
						tmpRet.clear();
						FreqAnalyzer::getInstance()->performAutoCorrection(matchRet);
						return;
					}
				}
			}else{
				if(0 == strDecodeUnmark.find(curCode)){
					strLog <<"runAutoTest(), Case 3 ===>>> Detection mismatch but msg matched, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"strDecodeMark    = ["<<strDecodeMark<<"]\n"<<
							"Difference       = ["<<findDifference(curEncodeMark, strDecodeMark)<<"]\n"<<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"] \n"<<
							"strCode          = ["<<strCode<<"]\n";
					LOGE("%s\n", strLog.str().c_str());
					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_EC){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, DESC_MATCH_MSG, bFromAutoCorrection);
						}
					}else if(0 > miPairingReturnCode){
						MatchRetSet* matchRet = new MatchRetSet(DESC_MATCH_MSG, strDecodeMark, strDecodeUnmark, strCode);
						tmpRet.str("");
						tmpRet.clear();
						FreqAnalyzer::getInstance()->performAutoCorrection(matchRet);
						return;
					}
				}else{
					strLog <<"runAutoTest(), Case 4 ===>>> Detection mismatch, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"strDecodeMark    = ["<<strDecodeMark<<"]\n"<<
							"Difference       = ["<<findDifference(curEncodeMark, strDecodeMark)<<"]\n"<<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"] \n"<<
							"strCode          = ["<<strCode<<"]\n";
					LOGE("%s\n", strLog.str().c_str());
					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, DESC_MISMATCH, bFromAutoCorrection);
						}
					}else if(0 > miPairingReturnCode){
						MatchRetSet* matchRet = new MatchRetSet(DESC_MISMATCH, strDecodeMark, strDecodeUnmark, strCode);
						tmpRet.str("");
						tmpRet.clear();
						FreqAnalyzer::getInstance()->performAutoCorrection(matchRet);
						return;
					}
				}
			}
		}
		deinitTestRound();
	}
}

void AudioTest::onTimeout(void* freqAnalyzerRef, bool bFromAutoCorrection, MatchRetSet* prevMatchRet){
	LOGE("onTimeout(), bFromAutoCorrection:%d", bFromAutoCorrection);
	FreqAnalyzer* freqAnalyzer = (FreqAnalyzer*)freqAnalyzerRef;
	stringstream strLog;
	/*if(NULL == getDecodeRet())*/{
		if(false == freqAnalyzer->checkEndPoint()){
			string strDecodeUnmark = SoundPair_Config::decodeConsecutiveDigits(tmpRet.str());
			if(0 == strDecodeUnmark.find(curCode)){
				if(0 == strDecodeUnmark.find(curCode+curECCode)){
					strLog << "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 7 ===>>> detection timeout but msg+errCode matched, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"tmpRet           = ["<<tmpRet<<"]\n" <<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"]";
					LOGE("%s\n", strLog.str().c_str());
					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.str(), DESC_TIMEOUT_MSG_EC, bFromAutoCorrection);
						}
					}else if(0 > miPairingReturnCode){
						MatchRetSet* matchRet = new MatchRetSet(DESC_TIMEOUT_MSG_EC, tmpRet.str(), strDecodeUnmark, "XXX");
						tmpRet.str("");
						tmpRet.clear();
						freqAnalyzer->performAutoCorrection(matchRet);
						return;
					}
				}else{
					strLog << "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 6 ===>>> detection timeout but msg matched, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"tmpRet           = ["<<tmpRet<<"]\n" <<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"]";
					LOGE("%s\n", strLog.str().c_str());

					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.str(), DESC_TIMEOUT_MSG, bFromAutoCorrection);
						}
					}else if(0 > miPairingReturnCode){
						MatchRetSet* matchRet = new MatchRetSet(DESC_TIMEOUT_MSG, tmpRet.str(), strDecodeUnmark, "XXX");
						tmpRet.str("");
						tmpRet.clear();
						freqAnalyzer->performAutoCorrection(matchRet);
						return;
					}
				}
			}else{
				strLog << "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 5 ===>>> detection timeout and msg mismatched, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
						"curCode          = ["<<curCode<<"], \n" <<
						"curECCode        = ["<<curECCode<<"], \n" <<
						"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
						"tmpRet           = ["<<tmpRet<<"]\n" <<
						"strDecodeUnmark  = ["<<strDecodeUnmark<<"]";
				LOGE("%s\n", strLog.str().c_str());

				if(bFromAutoCorrection){
					if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
						adaptPrevMatchRet(prevMatchRet);
					}else{
						Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.str(), DESC_TIMEOUT, bFromAutoCorrection);
					}
				}else if(0 > miPairingReturnCode){
					MatchRetSet* matchRet = new MatchRetSet(DESC_TIMEOUT, tmpRet.str(), strDecodeUnmark, "XXX");
					tmpRet.str("");
					tmpRet.clear();
					freqAnalyzer->performAutoCorrection(matchRet);
					return;
				}
			}
		}else{
			//if end point is detected, the path will be redirect to onSetResult() callback
			LOGE("onTimeout(), checkEndPoint is true, bFromAutoCorrection:%d", bFromAutoCorrection);
			return;
		}
	}
	deinitTestRound();
}

float AudioTest::onBufCheck(ArrayRef<short> buf, msec_t lBufTs, bool bResetFFT, int* iFFTValues){
	//FreqAnalyzer::getInstance()->analyzeAudioViaAudacityAC(buf, SoundPair_Config::FRAME_SIZE_REC, bResetFFT, 0, NULL);
	return FreqAnalyzer::getInstance()->analyzeAudioViaAudacityAC(buf, SoundPair_Config::FRAME_SIZE_REC, bResetFFT, FreqAnalyzer::getInstance()->getLastDetectedToneIdx(lBufTs), iFFTValues);
}

void AudioTest::adaptPrevMatchRet(MatchRetSet* prevMatchRet){
	LOGI("adaptPrevMatchRet(), previous result is better,\n prevMatchRet = %s", prevMatchRet->toString().c_str());
	Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, prevMatchRet->strCode, prevMatchRet->strDecodeUnmark, prevMatchRet->strDecodeMark, prevMatchRet->prevMatchRetType, false);
}

void AudioTest::decodeRSCode(int* data, int iCount, int iNumErr){

}

void AudioTest::resetBuffer(){
	mbNeedToResetFFT = true;
	AudioBufferMgr::getInstance()->recycleAllBuffer();
	shortsRecBuf = NULL;
	lTsRec = 0;
	//deinitTestRound();
}

void AudioTest::deinitTestRound(){
	tmpRet.str("");
	tmpRet.clear();
	FreqAnalyzer::getInstance()->endToTrace();
	FreqAnalyzer::getInstance()->reset();
	resetBuffer();

	pthread_mutex_lock(&mThresholdCtrlObj);
	mbAboveThreshold=false;
	pthread_cond_broadcast(&mThresholdCtrlObjCond);
	pthread_mutex_unlock(&mThresholdCtrlObj);

	pthread_mutex_lock(&mAutoTestCtrlObj);
	pthread_cond_broadcast(&mAutoTestCtrlObjCond);
	pthread_mutex_unlock(&mAutoTestCtrlObj);

	pthread_mutex_lock(&mSyncObj);
	pthread_cond_broadcast(&mSyncObjCond);
	pthread_mutex_unlock(&mSyncObj);
}

string AudioTest::findDifference(string strSrc, string strDecode){
	stringstream strRet(strDecode);
	int iLenSrc = strSrc.length();
	for(int i =0; i < iLenSrc; i++){
		if(i >= strDecode.length())
			break;
		if(0 != strSrc.substr(i, 1).compare(strDecode.substr(i, 1))){
			strRet.str().replace(i, 1, "#");
			//break;
		}
	}
	return strRet.str();
}

#ifdef ANDROID
void AudioTest::setCamCamWSServerInfo(string strHost, int iPort){
	mstrCamWSServerIP = strHost;
	miCamWSServerPort = iPort;
}

int AudioTest::connectCamCamWSServer(){
	return init_websocket_client(mstrCamWSServerIP.c_str(), miCamWSServerPort, soundpairSenderCb);
}

int AudioTest::disconnectCamCamWSServer(){
	return deinit_websocket_client();
}

bool AudioTest::isCamCamWSServerConnected(){
	return is_websocket_client_inited();
}
#endif
