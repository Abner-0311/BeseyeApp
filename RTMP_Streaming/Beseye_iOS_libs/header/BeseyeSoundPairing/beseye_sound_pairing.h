#ifndef __SOUNDPAIRING_H__
#define __SOUNDPAIRING_H__
#include <string>
#include <zxing/common/Array.h>

using zxing::ArrayRef;
using namespace std;

#ifdef __cplusplus
 extern "C" {

int Delegate_OpenAudioDevice(int sampleRate, int is16Bit, int channelCount, int desiredBufferFrames);
void * Delegate_GetAudioBuffer();
int Delegate_GetAudioBufferSize(int sampleRate);
void Delegate_WriteAudioBuffer(int iLen);
//void Delegate_WriteAudioBuffer(byte* buf);
void Delegate_CloseAudioDevice();

int Delegate_OpenAudioRecordDevice(int sampleRate, int is16Bit);
int Delegate_getAudioRecordBuf(ArrayRef<short> buf, int iLen);
void Delegate_CloseAudioRecordDevice();

void Delegate_UpdateFreq(msec_t lts, float freq);
void Delegate_ResetData();
void Delegate_FeedbackMatchResult(string strCode, string strECCode, string strEncodeMark, string strDecode, string strDecodeUnmark, string strDecodeMark, int iMatchDesc, bool bFromAutoCorrection);

void Delegate_SendMsgByBT(string strCode);

void Delegate_detachCurrentThread();
}
#endif

#endif
