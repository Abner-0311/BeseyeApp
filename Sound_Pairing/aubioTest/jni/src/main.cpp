#include "AudioTest.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <getopt.h>
#include <signal.h>

static int was_closed;
static int force_exit = 0;

void sighandler(int sig){
    force_exit = 1;
    AudioTest::getInstance()->stopAutoTest();
}

int main(int argc, char** argv) {
	int iDigitalToTest = 24;
	signal(SIGINT, sighandler);
	SoundPair_Config::init();
	AudioTest::getInstance()->setReceiverMode();
	AudioTest::getInstance()->startAutoTest("", iDigitalToTest);
    return 0;
}

int Delegate_OpenAudioDevice(int sampleRate, int is16Bit, int channelCount, int desiredBufferFrames){
	return 0;
}

void * Delegate_GetAudioBuffer(){
	return 0;
}

int Delegate_GetAudioBufferSize(int sampleRate){
	return 0;
}

void Delegate_WriteAudioBuffer(){
	return;
}

void Delegate_CloseAudioDevice(){
	return;
}

int Delegate_OpenAudioRecordDevice(int sampleRate, int is16Bit){
	return 0;
}

int Delegate_getAudioRecordBuf(ArrayRef<short> buf, int iLen){
	return 0;
}

void Delegate_CloseAudioRecordDevice(){
	return;
}

void Delegate_UpdateFreq(msec_t lts, float freq){
	return;
}

void Delegate_ResetData(){
	return;
}

void Delegate_FeedbackMatchResult(string strCode, string strECCode, string strEncodeMark, string strDecode, string strDecodeUnmark, string strDecodeMark, int iMatchDesc, bool bFromAutoCorrection){
	return;
}

void Delegate_SendMsgByBT(string strCode){
	return;
}

void Delegate_detachCurrentThread(){
	return;
}
