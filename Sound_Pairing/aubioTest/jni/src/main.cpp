#include "AudioTest.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <getopt.h>
#include <signal.h>
#include "simple_websocket_mgr.h"

static int was_closed;
static int force_exit = 0;

void sighandler(int sig){
    force_exit = 1;
    AudioTest::getInstance()->stopAutoTest();
}

int main(int argc, char** argv) {
	LOGE("+++++\n");
	int iDigitalToTest = 24;
	signal(SIGINT, sighandler);
	SoundPair_Config::init();
	AudioTest::getInstance()->setReceiverMode();
	AudioTest::getInstance()->startAutoTest("", iDigitalToTest);
	LOGE("-----\n");
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
static const char* FORMAT_LINE_RANDOM  = "%s\t%s\t%s\t%s\t%s\t%s\t%s\n";
static const char* filePath = "/beseye/ss_ret.log";
static char* buf = NULL;
static FILE *fp = NULL;
static const int AUTO_CORRECTION_TYPE = 2;
static int mTypeCounter [AUTO_CORRECTION_TYPE][DESC_RESULT_TYPE];

void Delegate_BeginToSaveResult(){
	if(NULL != fp){
		fclose(fp);
		fp = NULL;

		if(buf){
			free(buf);
			buf = NULL;
		}
	}

	fp=fopen(filePath, "wb");
	if(!fp){
		LOGE("failed to %s\n", filePath);
	}else{
		LOGE("Succeed to %s\n", filePath);
		//buf = (char*)malloc(2048);
		for(int i = 0;i<AUTO_CORRECTION_TYPE;i++){
			for(int j = 0;j<DESC_RESULT_TYPE;j++){
				mTypeCounter[i][j]=0;
			}
		}
	}
}

void Delegate_EndToSaveResult(){
	if(NULL != fp){
		fclose(fp);
		fp = NULL;
		if(buf){
			free(buf);
			buf = NULL;
		}
	}
}

static char msgStatistics[256]={0};
static char msgSent[2048]={0};

void Delegate_FeedbackMatchResult(string strCode, string strECCode, string strEncodeMark, string strDecode, string strDecodeUnmark, string strDecodeMark, int iMatchDesc, bool bFromAutoCorrection){
	if(fp){
		string ret = strCode+"\t"+strECCode+"\t"+strEncodeMark+"\t"+strDecode+"\t"+strDecodeUnmark+"\t"+strDecodeMark+"\t"+MATCH_RESULTStoString((MATCH_RESULTS)iMatchDesc)+(bFromAutoCorrection?"_AC":"")+"\n";
		fwrite(ret.c_str(), sizeof(char), ret.length(), fp);
		fflush(fp);

		int iAC_TYPE = bFromAutoCorrection?1:0;
		mTypeCounter[iAC_TYPE][iMatchDesc]++;

		sprintf(msgStatistics, "[%d, %d, %d, %d, %d, %d, %d][%d, %d, %d, %d, %d, %d, %d]\n",  mTypeCounter[0][0],  mTypeCounter[0][1],  mTypeCounter[0][2],  mTypeCounter[0][3],  mTypeCounter[0][4],  mTypeCounter[0][5],  mTypeCounter[0][6]
																						         ,  mTypeCounter[1][0],  mTypeCounter[1][1],  mTypeCounter[1][2],  mTypeCounter[1][3],  mTypeCounter[1][4],  mTypeCounter[1][5],  mTypeCounter[1][6]);
		sprintf(msgSent,
				SoundPair_Config::BT_MSG_FORMAT_SENDER.c_str(),
				SoundPair_Config::MSG_TEST_ROUND_RESULT.c_str(),
				ret.c_str(),
				msgStatistics);

		int iRet = send_msg_to_client(msgSent);
		LOGE("Delegate_FeedbackMatchResult(), send_msg_to_client, iRet:[%d]\n", iRet);
	}
	return;
}

void Delegate_TestRoundBegin(){}

void Delegate_TestRoundEnd(string strMatchRet, string strStatistics){}

void Delegate_SendMsgByBT(string strCode){}

void Delegate_detachCurrentThread(){}

//For ws client callback
void Delegate_WSConnecting(string strHost){}
void Delegate_WSConnected(string strHost){}
void Delegate_WSClosed(string strHost){}
