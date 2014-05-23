#ifndef __AUDIOBUFFERMGR_H__
#define __AUDIOBUFFERMGR_H__

#include <map>
#include <vector>
#include <string>
#include <iostream>
#include <pthread.h>
#include <cmath>
#include "sp_config.h"
#include "utils.h"

#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>
using zxing::Ref;
using zxing::Array;
using zxing::Counted;
using zxing::ArrayRef;

class BufRecord : public Counted{
public:
	BufRecord(msec_t mlTs, ArrayRef<short> mbBuf, int miSampleRead);
	virtual ~BufRecord();

	string toString();

	ArrayRef<short> mbBuf;
	int miIndex;
	msec_t mlTs;
	int miSampleRead;
	int miFFTValues[SoundPair_Config::FFT_ANALYSIS_COUNT];
};

class AudioBufferMgr : public Counted{
public:
	static unsigned int MAX_QUEUE_SIZE;

	static AudioBufferMgr* getInstance();
	void cleanRecordingBuf();
	int getBufIndex(ArrayRef<short> buf);
	void setBufferSize(int iSize);
	int getBufferSize();
	ArrayRef<short> getBufByIndex(int iBufIndexInput, int iOffset, ArrayRef<short> bufReturn);
	ArrayRef<short> getAvailableBuf();
	void addToDataBuf(msec_t lTs, ArrayRef<short> buf, int iSampleRead);
	Ref<BufRecord> getDataBuf();
	Ref<BufRecord> getDataBuf(int iNumToRest);
	void addToAvailableBuf(Ref<BufRecord> buf);
	void trimAvailableBuf(unsigned int iRestCount);

	void recycleAllBuffer();
	void waitForDataBuf(long lWaitTime);

	void setRecordMode(bool isRecordMode);
private:
	static AudioBufferMgr* sAudioBufferMgr;
	AudioBufferMgr();
	~AudioBufferMgr();

	vector<ArrayRef<short> > mAvailalbeBufList;
	vector<Ref<BufRecord> > mDataBufList;
	int miBufSize;
	//private Object mSyncObj = new Object();
	pthread_mutex_t mSrcBufMux;
	pthread_mutex_t mDataBufMux;
	pthread_cond_t mSyncObjCond;
	int miPivotRecording;
	int miPivotAnalysis;

	bool mbRecordMode;
};

#endif
