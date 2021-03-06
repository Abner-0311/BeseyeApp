#include "AudioBufferMgr.h"
#include "time_utils_cpp.h"

unsigned int AudioBufferMgr::MAX_QUEUE_SIZE = (int) (SoundPair_Config::MAX_RECORDING_TIME*SoundPair_Config::SAMPLE_RATE_REC/SoundPair_Config::FRAME_SIZE_REC);//30;
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
	LOGE("AudioBufferMgr(), AudioBufferMgr::MAX_QUEUE_SIZE:%d\n", AudioBufferMgr::MAX_QUEUE_SIZE);
	for(unsigned int i =0; i < AudioBufferMgr::MAX_QUEUE_SIZE ;i++){
		mAvailalbeBufList.push_back(ArrayRef<short>(new Array<short>(SoundPair_Config::FRAME_SIZE_REC)));
	}
	pthread_mutex_init(&mSrcBufMux, NULL);
	pthread_mutex_init(&mDataBufMux, NULL);
	pthread_cond_init (&mSyncObjCond, NULL);
}

AudioBufferMgr::~AudioBufferMgr(){
	recycleAllBuffer();
	acquireSrcBufMux();
	mAvailalbeBufList.clear();
//	for(int i =0; i < AudioBufferMgr::MAX_QUEUE_SIZE ;i++){
//		ArrayRef<short> buf = mAvailalbeBufList.front();
//		if(buf){
//			delete[] buf;
//		}
//		mAvailalbeBufList.erase(mAvailalbeBufList.begin());
//	}
	releaseSrcBufMux();

	pthread_cond_destroy(&mSyncObjCond);
	pthread_mutex_destroy(&mDataBufMux);
	pthread_mutex_destroy(&mSrcBufMux);
}

void AudioBufferMgr::cleanRecordingBuf(){
	acquireSrcBufMux();
	miPivotRecording = 0;
	miPivotAnalysis = 0;
	releaseSrcBufMux();
}

int AudioBufferMgr::getBufIndex(ArrayRef<short> buf){
	int iIndex = -1;
	acquireSrcBufMux();
	vector<ArrayRef<short> >::iterator findit = find(mAvailalbeBufList.begin(),mAvailalbeBufList.end(),buf);
	if(mAvailalbeBufList.end() != findit){
		iIndex = std::distance(mAvailalbeBufList.begin(), findit);
	}
	releaseSrcBufMux();
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
	if(DEBUG_MODE){
		LOGD("getBufByIndex()+\n");
	}
	acquireSrcBufMux();
	unsigned int iBufIndex = iBufIndexInput;
	while(iOffset > SoundPair_Config::FRAME_SIZE_REC){
		iOffset -= SoundPair_Config::FRAME_SIZE_REC;
		iBufIndex = (iBufIndex+1)%AudioBufferMgr::MAX_QUEUE_SIZE;
		if(DEBUG_MODE){
			LOGE("getBufByIndex(), shift iBufIndex because iOffset > SoundPair_Config::FRAME_SIZE_REC\n");
		}
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

			if(DEBUG_MODE){
				LOGE("getBufByIndex(), curBuf[0]:%d, bufReturn[0]:%d, curBuf[99]:%d, bufReturn[99]:%d\n", curBuf[0], bufReturn[0], curBuf[99], bufReturn[99]);
			}
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
	releaseSrcBufMux();
	//LOGD("getBufByIndex(), bufReturn:%d\n", bufReturn);
	return bufReturn;
}

void AudioBufferMgr::setRecordMode(bool isRecordMode){
	mbRecordMode = isRecordMode;
	if(DEBUG_MODE){
		LOGD("setRecordMode(), mbRecordMode:%d\n", mbRecordMode);
	}
}

ArrayRef<short> AudioBufferMgr::getAvailableBuf(){
	ArrayRef<short> buf = NULL;
	if(DEBUG_MODE){
		LOGD("getAvailableBuf()+\n");
	}
	acquireSrcBufMux();
	static bool bShowWarning = false;
//#ifdef ANDROID
	if(-1 == miPivotRecording){
//		if(!bShowWarning)
//			LOGE("getAvailableBuf(), buffer is out, need to wait for reset\n");
		bShowWarning = true;
	}else{
		bShowWarning = false;

		buf = mAvailalbeBufList[miPivotRecording];
		//Log.i(TAG, "getAvailableBuf(), get buffer at pos "+miPivotRecording);
//		if(miPivotRecording == AudioBufferMgr::MAX_QUEUE_SIZE -1){
//			LOGE("getAvailableBuf(), buffer is out\n");
//			miPivotRecording = -1;
//		}else
			miPivotRecording = (++miPivotRecording)%AudioBufferMgr::MAX_QUEUE_SIZE;

		if(!mbRecordMode && miPivotRecording == miPivotAnalysis){
			LOGE("getAvailableBuf(), meet non-analyzed buf, push it, [%d, %d]\n", miPivotRecording, miPivotAnalysis);
			//miPivotAnalysis = (++miPivotAnalysis)%AudioBufferMgr::MAX_QUEUE_SIZE;
			miPivotRecording = -1;
		}
	}
//#else
//	buf = mAvailalbeBufList[miPivotRecording];
//	miPivotRecording = (++miPivotRecording)%AudioBufferMgr::MAX_QUEUE_SIZE;
//#endif
	releaseSrcBufMux();
	if(DEBUG_MODE){
		LOGD("getAvailableBuf()-\n");
	}
	return buf;
}

Ref<BufRecord> AudioBufferMgr::getDataBuf(){
	return getDataBuf(0);
}

Ref<BufRecord> AudioBufferMgr::getDataBuf(int iNumToRest){
	Ref<BufRecord> br;
	if(DEBUG_MODE){
		LOGD("getDataBuf()+\n");
	}
	acquireDataBufMux();
	int iSize = mDataBufList.size();
	if((iNumToRest + 1) <= iSize){
		br = mDataBufList.front();
		mDataBufList.erase(mDataBufList.begin());
	}
	releaseDataBufMux();
	if(DEBUG_MODE){
		LOGD("getDataBuf()-\n");
	}
	return br;
}

void AudioBufferMgr::addToAvailableBuf(Ref<BufRecord> buf){
//	if(buf)
//		delete buf;
}

void AudioBufferMgr::recycleAllBuffer(){
	if(DEBUG_MODE){
		LOGI("recycleAllBuffer()+\n");
	}
	acquireDataBufMux();
	cleanRecordingBuf();
	mDataBufList.clear();
//	Ref<BufRecord> br = NULL;
//	while(mDataBufList.size()){
//		br = mDataBufList.front();
//		mDataBufList.erase(mDataBufList.begin());
//		delete br;
//	}
	releaseDataBufMux();
	if(DEBUG_MODE){
		LOGI("recycleAllBuffer()-\n");
	}
}

void AudioBufferMgr::addToDataBuf(msec_t lTs, ArrayRef<short> buf, int iSampleRead){
	if(DEBUG_MODE){
		LOGD("addToDataBuf(), lTs:%lld\n", lTs);
	}

	acquireDataBufMux();
	//LOGD("addToDataBuf(), push_back\n");
	mDataBufList.push_back(Ref<BufRecord>(new BufRecord(lTs, buf,iSampleRead)));
	if(mDataBufList.size() > AudioBufferMgr::MAX_QUEUE_SIZE){
		mDataBufList.erase(mDataBufList.begin());
	}
	//LOGD("addToDataBuf(), signal\n");
	pthread_cond_signal(&mSyncObjCond);
	releaseDataBufMux();

	if(DEBUG_MODE){
		LOGD("addToDataBuf()-\n");
	}
}

int AudioBufferMgr::getLastDataBufIndex(){
	int iRet = -1;
	acquireDataBufMux();
	int iSize = mDataBufList.size();
	if(0 < iSize){
		iRet = mDataBufList[iSize-1]->miIndex;
	}
	releaseDataBufMux();
	return iRet;
}

int AudioBufferMgr::getIndexFromPosition(int iPosIdx, int iShift){
	int iRet = iPosIdx + iShift;
	if(AudioBufferMgr::MAX_QUEUE_SIZE <= iRet){
		iRet -= AudioBufferMgr::MAX_QUEUE_SIZE;
	}else if(0 > iRet){
		iRet += AudioBufferMgr::MAX_QUEUE_SIZE;
	}
	if(DEBUG_MODE){
		LOGI("iPosIdx:[%d]->iShift:[%d] = iRet:[%d]\n", iPosIdx, iShift, iRet);
	}
	return iRet;
}

void AudioBufferMgr::trimAvailableBuf(unsigned int iRestCount){
	//LOGI("trimAvailableBuf(), iRestCount:%d\n", iRestCount);

	acquireDataBufMux();
	if(DEBUG_MODE){
		LOGI("trimAvailableBuf(), iRestCount:%d, mDataBufList.size() :%d\n", iRestCount, mDataBufList.size());
	}
	unsigned int iCountToErase = (mDataBufList.size() >= iRestCount)?(mDataBufList.size() - iRestCount):0;
	while(0 < iCountToErase--){
		mDataBufList.erase(mDataBufList.begin());
	}
	if(0 < mDataBufList.size()){
		miPivotAnalysis = mDataBufList[0]->miIndex;
		LOGI("trimAvailableBuf(), miPivotAnalysis:%d, miPivotRecording :%d\n", miPivotAnalysis, miPivotRecording);
	}
	releaseDataBufMux();

}

void AudioBufferMgr::waitForDataBuf(long lWaitTime){
	struct timespec outtime;
	getTimeSpecByDelay(outtime, lWaitTime);

	acquireDataBufMux();
	if(DEBUG_MODE){
		LOGD("waitForDataBuf(), wait++++\n");
	}
	int iRet = pthread_cond_timedwait(&mSyncObjCond, &mDataBufMux, &outtime);
//	if(ETIMEDOUT == iRet)
//		LOGI("waitForDataBuf(), iRet:ETIMEDOUT\n");
//	else
	if(DEBUG_MODE){
		LOGD("waitForDataBuf(), iRet:%d\n", iRet);
	}

	releaseDataBufMux();
	if(DEBUG_MODE){
		LOGD("waitForDataBuf()-\n");
	}
}

static volatile int siSrcBufMuxCount = 0;
void AudioBufferMgr::acquireSrcBufMux(){
	siSrcBufMuxCount++;
//	if(1 < siSrcBufMuxCount)
//		LOGE("Error, siSrcBufMuxCount:%d\n", siSrcBufMuxCount);
	pthread_mutex_lock(&mSrcBufMux);
}

void AudioBufferMgr::releaseSrcBufMux(){
	pthread_mutex_unlock(&mSrcBufMux);
	siSrcBufMuxCount--;
	//LOGI("siSrcBufMuxCount:%d\n", siSrcBufMuxCount);
}

static volatile int siDataBufMuxCount = 0;
void AudioBufferMgr::acquireDataBufMux(){
//	siDataBufMuxCount++;
//	if(1 < siDataBufMuxCount)
//		LOGE("Error, siDataBufMuxCount:%d\n", siDataBufMuxCount);
	pthread_mutex_lock(&mDataBufMux);
}

void AudioBufferMgr::releaseDataBufMux(){
	pthread_mutex_unlock(&mDataBufMux);
	siDataBufMuxCount--;
	//LOGI("siDataBufMuxCount:%d\n", siDataBufMuxCount);
}
