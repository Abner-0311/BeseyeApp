#include "AudioBufferMgr.h"

int AudioBufferMgr::MAX_QUEUE_SIZE = (int) (SoundPair_Config::MAX_RECORDING_TIME*SoundPair_Config::SAMPLE_RATE_REC/SoundPair_Config::FRAME_SIZE_REC);//30;
AudioBufferMgr* AudioBufferMgr::sAudioBufferMgr = NULL;

BufRecord::BufRecord(msec_t mlTs, ArrayRef<short> mbBuf, int miSampleRead){
	this->mlTs = mlTs;
	this->mbBuf = mbBuf;
	this->miSampleRead = miSampleRead;
	miIndex = AudioBufferMgr::getInstance()->getBufIndex(mbBuf);
	//LOGE("BufRecord(), miIndex:%d, mbBuf[99]:%d\n", miIndex, mbBuf[99]);
}

BufRecord::~BufRecord() {
	this->mbBuf = NULL;
}

string BufRecord::toString() {
	std::stringstream s;
	s<<"BufRecord [mlTs=" << mlTs << ", miSampleRead=" << miSampleRead << "]";
	return s.str();
}

AudioBufferMgr* AudioBufferMgr::getInstance(){
	if(NULL == sAudioBufferMgr){
		sAudioBufferMgr = new AudioBufferMgr();
	}
	return sAudioBufferMgr;
}

AudioBufferMgr::AudioBufferMgr():
miBufSize(0),
miPivotRecording(0),
miPivotAnalysis(0){
	for(int i =0; i < AudioBufferMgr::MAX_QUEUE_SIZE ;i++){
		mAvailalbeBufList.push_back(ArrayRef<short>(new Array<short>(SoundPair_Config::FRAME_SIZE_REC)));
	}
	pthread_mutex_init(&mSrcBufMux, NULL);
	pthread_mutex_init(&mDataBufMux, NULL);
	pthread_cond_init (&mSyncObjCond, NULL);
}

AudioBufferMgr::~AudioBufferMgr(){
	recycleAllBuffer();
	pthread_mutex_lock(&mSrcBufMux);
	mAvailalbeBufList.clear();
//	for(int i =0; i < AudioBufferMgr::MAX_QUEUE_SIZE ;i++){
//		ArrayRef<short> buf = mAvailalbeBufList.front();
//		if(buf){
//			delete[] buf;
//		}
//		mAvailalbeBufList.erase(mAvailalbeBufList.begin());
//	}
	pthread_mutex_unlock(&mSrcBufMux);

	pthread_cond_destroy(&mSyncObjCond);
	pthread_mutex_destroy(&mDataBufMux);
	pthread_mutex_destroy(&mSrcBufMux);
}

void AudioBufferMgr::cleanRecordingBuf(){
	pthread_mutex_lock(&mSrcBufMux);
	miPivotRecording = 0;
	miPivotAnalysis = 0;
	pthread_mutex_unlock(&mSrcBufMux);
}

int AudioBufferMgr::getBufIndex(ArrayRef<short> buf){
	int iIndex = -1;
	pthread_mutex_lock(&mSrcBufMux);
	vector<ArrayRef<short> >::iterator findit = find(mAvailalbeBufList.begin(),mAvailalbeBufList.end(),buf);
	if(mAvailalbeBufList.end() != findit){
		iIndex = std::distance(mAvailalbeBufList.begin(), findit);
	}
	pthread_mutex_unlock(&mSrcBufMux);
	return iIndex;
}

void AudioBufferMgr::setBufferSize(int iSize){
	miBufSize = iSize;
}

int AudioBufferMgr::getBufferSize(){
	return miBufSize;
}

ArrayRef<short> AudioBufferMgr::getBufByIndex(int iBufIndexInput, int iOffset, ArrayRef<short> bufReturn){
	//Log.d(TAG, "getBufByIndex(), iBufIndexInput:"+iBufIndexInput+\n", iOffset:"+iOffset);
	LOGD("getBufByIndex()+\n");
	pthread_mutex_lock(&mSrcBufMux);
	int iBufIndex = iBufIndexInput;
	while(iOffset > SoundPair_Config::FRAME_SIZE_REC){
		iOffset -= SoundPair_Config::FRAME_SIZE_REC;
		iBufIndex = (iBufIndex+1)%AudioBufferMgr::MAX_QUEUE_SIZE;
		LOGE("getBufByIndex(), shift iBufIndex because iOffset > SoundPair_Config::FRAME_SIZE_REC\n");
	}

	int iLenBuf = /*(NULL == bufReturn)?0:*/SoundPair_Config::FRAME_SIZE_REC;//bufReturn.length;
	ArrayRef<short> curBuf = (0 <= iBufIndex && iBufIndex < AudioBufferMgr::MAX_QUEUE_SIZE)?mAvailalbeBufList[iBufIndex]:ArrayRef<short>(new Array<short>(0));
	if(0 == curBuf->size()){
		LOGE("getBufByIndex(), curBuf is NULL, iBufIndex:%d,AudioBufferMgr::MAX_QUEUE_SIZEL%d \n", iBufIndex, AudioBufferMgr::MAX_QUEUE_SIZE);
	}else if(SoundPair_Config::FRAME_SIZE_REC != iLenBuf){
		LOGE("getBufByIndex(), length doesn't match iLenBuf:%d\n",iLenBuf);
	}else{
		if(0 == iOffset){
			memcpy(&bufReturn[0], &curBuf[0], iLenBuf);
		}else{
			int iDelta = std::abs(iOffset);
			if(iDelta > SoundPair_Config::FRAME_SIZE_REC){
				LOGE("getBufByIndex(), too large iOffset:%d\n", iOffset);
			}else if(0 < iOffset){
				ArrayRef<short> nextBuf = mAvailalbeBufList[(iBufIndex+1)%AudioBufferMgr::MAX_QUEUE_SIZE];
				if(0 < nextBuf->size()){
					//memset(bufReturn, 0, iLenBuf);
					memcpy(&bufReturn[0]			   , &curBuf[iDelta], iLenBuf - iDelta);
					memcpy(&bufReturn[iLenBuf - iDelta], &nextBuf[0]	, iDelta);
				}else{
					LOGE("getBufByIndex(), nextBuf is NULL, iBufIndex:%d,AudioBufferMgr::MAX_QUEUE_SIZEL%d \n", iBufIndex, AudioBufferMgr::MAX_QUEUE_SIZE);
				}
			}else{
				int idxPrev = (0 == iBufIndex)?(AudioBufferMgr::MAX_QUEUE_SIZE-1):(iBufIndex-1);
				ArrayRef<short> prevBuf = mAvailalbeBufList[idxPrev];
				//LOGE("getBufByIndex(), iBufIndex:"+iBufIndex+\n", idxPrev:"+idxPrev+\n", iOffset:"+iOffset+\n", iLenBuf:"+iLenBuf);
				if(0 < prevBuf->size()){
					//memset(bufReturn, 0, iLenBuf);
					memcpy(&bufReturn[0]	 , &prevBuf[iLenBuf - iDelta], iDelta);
					memcpy(&bufReturn[iDelta], &curBuf[0]	  		     , iLenBuf - iDelta);
				}else{
					LOGE("getBufByIndex(), prevBuf is NULL, iBufIndex:%d,AudioBufferMgr::MAX_QUEUE_SIZEL%d \n", iBufIndex, AudioBufferMgr::MAX_QUEUE_SIZE);
				}
			}
		}
	}
	//Log.i(TAG, "getBufByIndex(), bufReturn[0]:"+bufReturn[0]);
	pthread_mutex_unlock(&mSrcBufMux);
	//LOGD("getBufByIndex(), bufReturn:%d\n", bufReturn);
	return bufReturn;
}

ArrayRef<short> AudioBufferMgr::getAvailableBuf(){
	ArrayRef<short> buf = NULL;
	LOGD("getAvailableBuf()+\n");
	pthread_mutex_lock(&mSrcBufMux);
//#ifdef ANDROID
		static int iHaveShowWarning = 0;
		if(-1 == miPivotRecording){
				if(0==iHaveShowWarning)
						LOGE("getAvailableBuf(), buffer is out, need to wait for reset\n");
				iHaveShowWarning = 1;
		}else{
				iHaveShowWarning = 0;
				buf = mAvailalbeBufList[miPivotRecording];
				//Log.i(TAG, "getAvailableBuf(), get buffer at pos "+miPivotRecording);
				if(miPivotRecording == AudioBufferMgr::MAX_QUEUE_SIZE -1){
						LOGE("getAvailableBuf(), buffer is out\n");
						miPivotRecording = -1;
				}else
						miPivotRecording = (++miPivotRecording)%AudioBufferMgr::MAX_QUEUE_SIZE;

				if(miPivotRecording == miPivotAnalysis){
						//Log.w(TAG, "getAvailableBuf(), meet non-analyzed buf, push it\n");
						miPivotAnalysis = (++miPivotAnalysis)%AudioBufferMgr::MAX_QUEUE_SIZE;
				}
		}
//#else
//      buf = mAvailalbeBufList[miPivotRecording];
//      miPivotRecording = (++miPivotRecording)%AudioBufferMgr::MAX_QUEUE_SIZE;
//#endif
	pthread_mutex_unlock(&mSrcBufMux);
	LOGD("getAvailableBuf()-\n");
	return buf;
}

Ref<BufRecord> AudioBufferMgr::getDataBuf(){
	return getDataBuf(0);
}

Ref<BufRecord> AudioBufferMgr::getDataBuf(int iNumToRest){
	Ref<BufRecord> br;
	LOGD("getDataBuf()+\n");
	pthread_mutex_lock(&mDataBufMux);
	int iSize = mDataBufList.size();
	if((iNumToRest + 1) <= iSize){
		br = mDataBufList.front();
		mDataBufList.erase(mDataBufList.begin());
	}
	pthread_mutex_unlock(&mDataBufMux);
	LOGD("getDataBuf()-\n");
	return br;
}

void AudioBufferMgr::addToAvailableBuf(Ref<BufRecord> buf){
//	if(buf)
//		delete buf;
}

void AudioBufferMgr::recycleAllBuffer(){
	LOGI("recycleAllBuffer()+\n");
	pthread_mutex_lock(&mDataBufMux);
	cleanRecordingBuf();
	mDataBufList.clear();
//	Ref<BufRecord> br = NULL;
//	while(mDataBufList.size()){
//		br = mDataBufList.front();
//		mDataBufList.erase(mDataBufList.begin());
//		delete br;
//	}
	pthread_mutex_unlock(&mDataBufMux);
	LOGI("recycleAllBuffer()-\n");
}

void AudioBufferMgr::addToDataBuf(msec_t lTs, ArrayRef<short> buf, int iSampleRead){
	LOGD("addToDataBuf(), lTs:%lld\n", lTs);

	pthread_mutex_lock(&mDataBufMux);
	//LOGD("addToDataBuf(), push_back\n");
	mDataBufList.push_back(Ref<BufRecord>(new BufRecord(lTs, buf,iSampleRead)));

	//LOGD("addToDataBuf(), signal\n");
	pthread_cond_signal(&mSyncObjCond);
	pthread_mutex_unlock(&mDataBufMux);

	LOGD("addToDataBuf()-\n");
}

void AudioBufferMgr::waitForDataBuf(long lWaitTime){
	struct timespec outtime;
	getTimeSpecByDelay(outtime, lWaitTime);

	pthread_mutex_lock(&mDataBufMux);
	LOGD("waitForDataBuf(), wait++++\n");
	int iRet = pthread_cond_timedwait(&mSyncObjCond, &mDataBufMux, &outtime);
//	if(ETIMEDOUT == iRet)
//		LOGI("waitForDataBuf(), iRet:ETIMEDOUT\n");
//	else
		LOGD("waitForDataBuf(), iRet:%d\n", iRet);
	pthread_mutex_unlock(&mDataBufMux);
	LOGD("waitForDataBuf()-\n");
}
