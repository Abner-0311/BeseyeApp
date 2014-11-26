#include "AudioTest.h"
#include "simple_websocket_mgr.h"
#include "delegate/account_mgr.h"
#include "delegate/cam_controller.h"
#include "delegate/led_controller.h"
#include "cgi_attr.h"
#include "json_utils.h"
#include "utils.h"
#include "ws_attr.h"
#include "cmd_error.h"

#ifdef CAM_ENV
#include "http_cgi.h"
#endif

#include <dirent.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>

#include <signal.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/wait.h>

#define MONITOR_PROCESS_FLAG "/tmp/beseye_sp_monitor_process"
#define MONITOR_PROCESS_RET "/tmp/beseye_sp_monitor_process_ret"

static const int  MAX_TIME_TO_INVOKE_SYSTEM = 10;//10 sec
static int  siTimeoutValue = MAX_TIME_TO_INVOKE_SYSTEM;

static pid_t pid_system = -1;
static msec_t lTimeInvodeSystem = 0;
static pid_t intermediate_pid = -1;
//static int iRetSystemCall = 0;

//To avoid system call/fork blocking issue, we need to monitor it and kill it when timeout
int invokeSystemWithTimeout(const char* cmd, int iTimeoutInSec){
	siTimeoutValue = MAX_TIME_TO_INVOKE_SYSTEM;
	if(0 < iTimeoutInSec && iTimeoutInSec < 120){
		siTimeoutValue = iTimeoutInSec;
	}

	//return system(cmd);
	int iRetSystemCall = -1;
	//LOGE( "invokeSystem(), time_ms:%lld .............++++\n", time_ms());
	saveToFile(MONITOR_PROCESS_FLAG,"monitor");
	saveToFile(MONITOR_PROCESS_RET,"-1");

	intermediate_pid = fork();
	if (intermediate_pid == 0) {
		LOGE( "invokeSystem(), intermediate_pid fork successfully time_ms:%lld .............+++++++++++++++++\n", time_ms());
		int iRet = system(cmd);
		char cRet[32]={0};
		sprintf(cRet, "%d", iRet);
		saveToFile(MONITOR_PROCESS_RET,cRet);
		deleteFile(MONITOR_PROCESS_FLAG);
		LOGE( "invokeSystem(), MONITOR_PROCESS_FLAG end time_ms:%lld,cmd:[%s] cRet:[%s] .............---------------\n", time_ms(),cmd, cRet);
		exit (0);
	}

	LOGE( "invokeSystem(), intermediate_pid:%d \n", intermediate_pid);
	if(0 < intermediate_pid){
		lTimeInvodeSystem = time_ms();
		//LOGE( "invokeSystem(), waitpid begin\n");
		waitpid(intermediate_pid, 0, 0);
		//LOGE( "invokeSystem(), waitpid end\n");
		deleteFile(MONITOR_PROCESS_FLAG);
		intermediate_pid = -1;
	}else{
		LOGE( "invokeSystem(), invalid intermediate_pid:%d \n", intermediate_pid);
	}

	char* cRet = readFromFile(MONITOR_PROCESS_RET);
	if(cRet){
		iRetSystemCall = atoi(cRet);
	}
	FREE(cRet)
	LOGE( "invokeSystem(), iRetSystemCall:%d, time_ms:%lld .............----\n", iRetSystemCall, time_ms());
	return iRetSystemCall;

}

int invokeSystem(const char* cmd){
	return invokeSystemWithTimeout(cmd, -1);
}

bool isSystemProcessExist(){
	bool bRet = false;
	char* stopSSFlag = readFromFile(MONITOR_PROCESS_FLAG);
	if(stopSSFlag){
		bRet = true;
	}
	FREE(stopSSFlag)
	return bRet;
}

void killSystemProcess(){
	if(0 < intermediate_pid){
		if(0 > kill(intermediate_pid, SIGKILL)){
			LOGE( "failed tp kill intermediate_pid\n");
		}
		intermediate_pid = -1;
		lTimeInvodeSystem = 0;
	}
}

void checkSystemProcess(){
	if(0 < intermediate_pid){
		if(isSystemProcessExist()){
			if((0 != lTimeInvodeSystem) && ((time_ms() - lTimeInvodeSystem) > (siTimeoutValue*1000))){
				LOGE( "stop monitor flag is on over %d sec\n", siTimeoutValue);
				killSystemProcess();
			}
		}else{
			LOGE( "stop monitor flag is off\n");
			lTimeInvodeSystem = 0;
			intermediate_pid = -1;
		}
	}
}

static const char* SP_ENABLED_FLAG			= "/beseye/config/sp_enabled";

//void setInvalidWifi(){
//	int iRet = 0;
//	int iTrial = 0;
//	char jsonData[BUF_SIZE]={0};
//	sprintf(jsonData, "{\"sec\":3,\"key\":\"88888888\",\"ssid\":\"test\"}");
//	char jsonData2[BUF_SIZE]={0};
//	do{
//		strcpy(jsonData2, jsonData);
//		//LOGI( "ssid:%s, jsonData2:[%s]\n", ssid, jsonData2);
//		if(0 < iTrial){
//			sleep(1);
//		}
//		iRet = setWiFiSetting(jsonData2);
//	}while((iRet == RET_CODE_NETWORK_ERR || iRet == CMD_RET_CODE_WEB_API_ERR) && 3 >iTrial++);
//}

int checkSpEnabled(){
	int iRet = RET_CODE_OK;
	BOOL sp_enabled = isFileExist(SP_ENABLED_FLAG);

	if(FALSE == sp_enabled){
		char wifiInfo[BUF_SIZE]={0};
		int iTrials = 0;
		do{
			if(0 < iTrials){
				sleep(1);
			}
			iRet = getWiFiSetting(wifiInfo);
		}while(iTrials++ > 3 && RET_CODE_OK != iRet);

		if(RET_CODE_OK == iRet){
			//LOGE( "wifiInfo:%s\n", wifiInfo?wifiInfo:"");
			struct json_object *wifi_obj = json_tokener_parse(wifiInfo);
			if(!is_error(wifi_obj)){
				json_object *Data_obj = json_object_object_get(wifi_obj, WS_ATTR_INTERNAL_DATA);
				if(!is_error(Data_obj)){
					json_object *ssid_obj = json_object_object_get(Data_obj, API_PARAM_SSID);
					if(!is_error(ssid_obj)){
						const char *ssid = json_object_to_json_string(ssid_obj);
						//LOGE( "ssid:[%s]\n", ssid?ssid:"");
						if(ssid && 0 == strcmp(ssid, "\"beseye_ap1_cam\"")){
							//LOGE( "ssid is matched\n");
							json_object *pw_obj = json_object_object_get(Data_obj, API_PARAM_KEY);
							if(!is_error(pw_obj)){
								const char *pw = json_object_to_json_string(pw_obj);
								//LOGE( "pw:[%s]\n", pw?pw:"");
								if(pw && 0 == strcmp(pw, "\"12345678\"")){
									//LOGE( "Found match AP info........................\n");
									iRet = CMD_RET_CODE_SP_DISABLED;
								}
							}
						}
					}
				}
				FREE_JSON_OBJ(wifi_obj)
			}
		}
	}else{
		LOGE( "sp_enabled is on........................\n");
	}

	return iRet;
}

static ArrayRef<short> shortsRecBuf=NULL;
static int iCurIdx = 0;

static timespec sleepValue = {0};
static msec_t lTsRec = 0;
static int iAudioFrameSize = 4;
static const int MAX_TRIAL = 1;//10;


//Check audio activity
static long  ANALYSIS_THRESHHOLD_MONITOR			=0;
static int 	 ANALYSIS_THRESHHOLD_MONITOR_CNT		=0;

static const short ANALYSIS_MAX_AUDIO_VALUE_G25 	= 2000;//audio max value for gain =25
static const short ANALYSIS_START_THRESHHOLD_MIN_G25= 450;//audio value
static const short ANALYSIS_START_THRESHHOLD_MAX_G25= 1400;//audio value

static const short ANALYSIS_MAX_AUDIO_VALUE_G35 	= 5500;//audio max value for gain =35
static const short ANALYSIS_START_THRESHHOLD_MIN_G35= 900;//audio value
static const short ANALYSIS_START_THRESHHOLD_MAX_G35= 3500;//audio value

static short ANALYSIS_MAX_AUDIO_VALUE 				= ANALYSIS_MAX_AUDIO_VALUE_G35;//audio max value
static short ANALYSIS_START_THRESHHOLD_MIN 			= ANALYSIS_START_THRESHHOLD_MIN_G35;//audio value
static short ANALYSIS_START_THRESHHOLD_MAX 			= ANALYSIS_START_THRESHHOLD_MAX_G35;//audio value

static short ANALYSIS_START_THRESHHOLD 				= 15000;//audio value
static short ANALYSIS_END_THRESHHOLD   				= 15000;//audio value

//after detect prefix
static long  ANALYSIS_THRESHHOLD_MONITOR_DETECT		= 0;
static int 	 ANALYSIS_THRESHHOLD_MONITOR_DETECT_CNT	= 0;
static short ANALYSIS_END_THRESHHOLD_DETECT	   		= -1;//audio value

static const int   ANALYSIS_THRESHHOLD_CK_LEN 		= 1600;//sample size , about 0.1 sec
static const int   ANALYSIS_AB_THRESHHOLD_CK_CNT 	= 8;
static const int   ANALYSIS_UN_THRESHHOLD_CK_CNT 	= 3;

static short sMaxValue = 0;
static int siAboveThreshHoldCount = 0;
static int siUnderThreshHoldCount = 0;
static int siRefCount = 0;

static const int   ANALYSIS_LED_UPDATE_PERIOD = 8000;//sample size , about 0.5 sec
static bool sbLEDOn = false;

static msec_t lLastTimeToBufRec = 0;

//Check Network and token
static const msec_t TIME_TO_CHECK_TOKEN = 30000;//30 seconds
static const msec_t TIME_TO_CHECK_TOKEN_ANALYSIS_PERIOD = 6000000;//600 seconds
static const long TIME_TO_CHECK_LED = 1;//1 seconds
static msec_t slLastTimeCheckToken = -1;

static const char* LOG_SOURCE = "/tmp/beseye_boot.log";
static const char* LOG_DEST = "%s/sp_failed_%s.log";
static const char* LOG_DIR = "/beseye/sp_log";

static const char* RAYLIOS_VER_ENV	= "RAYLIOS_VER";
static const char* RAYLIOS_VER_18	= "1.0-rc18";
static const char* GAIN_25 			= "25";
static const char* GAIN_35 			= "35";//after rc-18
static int siRayliosVerAbove18 = -1;//-1:unknown, 0:no, 1:yes

const char* getSPGain(){
	if(-1 == siRayliosVerAbove18){
		siRayliosVerAbove18 = 0;
		ANALYSIS_MAX_AUDIO_VALUE 		= ANALYSIS_MAX_AUDIO_VALUE_G25;
		ANALYSIS_START_THRESHHOLD_MIN 	= ANALYSIS_START_THRESHHOLD_MIN_G25;
		ANALYSIS_START_THRESHHOLD_MAX 	= ANALYSIS_START_THRESHHOLD_MAX_G25;

		char * raylios_ver = getenv(RAYLIOS_VER_ENV);
		LOGI( "raylios_ver:[%s]\n", raylios_ver?raylios_ver:"");
		if(raylios_ver && strcmp(raylios_ver, RAYLIOS_VER_18) >= 0){
			siRayliosVerAbove18 = 1;
			ANALYSIS_MAX_AUDIO_VALUE 		= ANALYSIS_MAX_AUDIO_VALUE_G35;
			ANALYSIS_START_THRESHHOLD_MIN 	= ANALYSIS_START_THRESHHOLD_MIN_G35;
			ANALYSIS_START_THRESHHOLD_MAX 	= ANALYSIS_START_THRESHHOLD_MAX_G35;
		}
	}

	return (1 <= siRayliosVerAbove18)?GAIN_35:GAIN_25;
}

int setMicrophoneGain(const char* gain){
	char jsonData[BUF_SIZE]={0};
	int iRet = RET_CODE_OK;
	if(gain){
		sprintf(jsonData, "{\"gain\":%s}", gain);
		iRet = setMicGain(jsonData);
		LOGI( "iRet of setMicGain():%d\n", iRet);
	}else{
		return CMD_RET_CODE_INVALID_INPUT_ERR;
	}
	return iRet;
}

int checkSPEnv(){
	char jsonData[BUF_SIZE]={0};
	int iRet = setMicrophoneGain(getSPGain());

	if(RET_CODE_OK == getEventByName(jsonData, "mEvent")){
		memset(jsonData, 0 , BUF_SIZE);
		strcpy(jsonData, "{\"name\":\"mEvent\"}");
		iRet = httpPostViaCGI(API_DEL_EVENT, jsonData);
		memset(jsonData, 0 , BUF_SIZE);
		strcpy(jsonData, "{\"name\":\"LocalMotion\"}");
		iRet = httpPostViaCGI(API_DEL_EVENT_SEVER, jsonData);
		//system("/beseye/util/curl --basic -u admin:password -X POST -H \"Content-Type: application/json\" -d '{\"name\":\"mEvent\"}' http://localhost/sray/deleteEvent.cgi");
		//system("/beseye/util/curl --basic -u admin:password -X POST -H \"Content-Type: application/json\" -d '{\"name\":\"LocalMotion\"}' http://localhost/sray/deleteEventServer.cgi");
		//iRet = CMD_RET_CODE_NEED_REBOOT;
	}
	LOGI( "final iRet:%d\n", iRet);
	return iRet;
}

int checkLogFiles ()
{
    DIR * d = NULL;
    int iMatchBegin = -1;
    int iMatchEnd = -1;
    int idx = 0;
    int iKillFileEnd = -1;
    //char * dir_name = ".";

    /* Open the current directory. */

    d = opendir (LOG_DIR);

    if (! d) {
    	LOGE ("Cannot open directory '%s': %s\n", LOG_DIR, strerror (errno));
    	mkdir(LOG_DIR, 755);
    	goto $EXIT;
    }

    while (1) {
        struct dirent * entry;

        entry = readdir (d);
        if (! entry) {
            break;
        }
        //LOGE ("%s\n", entry->d_name);
        if(NULL != entry->d_name && 0 == strncmp(entry->d_name, "sp_failed_", 10)){
        	if(-1 == iMatchBegin){
        		iMatchBegin = iMatchEnd = idx;
        	}else{
        		iMatchEnd = idx;
        	}
        }
        idx++;
    }

    //LOGE ("iMatchBegin:%d, iMatchEnd:%d\n", iMatchBegin, iMatchEnd);

    if(0 <= iMatchBegin && iMatchEnd - iMatchBegin > 2){
    	rewinddir(d);

    	iKillFileEnd = iMatchEnd - 3;

    	LOGE ("iMatchBegin:%d, iKillFileEnd:%d\n", iMatchBegin, iKillFileEnd);
    	idx = 0;
    	if(iKillFileEnd >= iMatchBegin){
    		if(d){
				if (closedir (d)) {
					LOGE ("Could not close '%s': %s\n", LOG_DIR, strerror (errno));
				}
				d=NULL;
			}
    		d = opendir (LOG_DIR);
    	    if (! d) {
    	    	LOGE ("Cannot open directory '%s': %s\n", LOG_DIR, strerror (errno));
    	    	mkdir(LOG_DIR, 755);
    	    	goto $EXIT;
    	    }
    		while (1) {
				struct dirent * entry;

				entry = readdir (d);
				if (! entry) {
					break;
				}
				//LOGE ("%s at %d\n", entry->d_name, idx);
				if(iMatchBegin<=idx && idx <= iKillFileEnd && NULL != entry->d_name && 0 == strncmp(entry->d_name, "sp_failed_", 10)){
					char logFilePath[1024] = {0};
					sprintf(logFilePath, "%s/%s", LOG_DIR, entry->d_name);
					int iRet = remove(logFilePath);

					LOGE ("del dir %s, iRet:%d\n", entry->d_name, iRet);
				}
				idx++;
				if(idx > iKillFileEnd){
					break;
				}
			}
    	}
    }

$EXIT:
    /* Close the directory. */
    if(d){
        if (closedir (d)) {
            LOGE ("Could not close '%s': %s\n", LOG_DIR, strerror (errno));
        }
    }
    return 0;
}

//static std::string UTF8_To_string(const std::string & str){
//	int nwLen = MultiByteToWideChar(CP_UTF8, 0, str.c_str(), -1, NULL, 0);
//
//	wchar_t * pwBuf = new wchar_t[nwLen + 1];//
//	memset(pwBuf, 0, nwLen * 2 + 2);
//
//	MultiByteToWideChar(CP_UTF8, 0, str.c_str(), str.length(), pwBuf, nwLen);
//
//	int nLen = WideCharToMultiByte(CP_ACP, 0, pwBuf, -1, NULL, NULL, NULL, NULL);
//
//	char * pBuf = new char[nLen + 1];
//	memset(pBuf, 0, nLen + 1);
//
//	WideCharToMultiByte(CP_ACP, 0, pwBuf, nwLen, pBuf, nLen, NULL, NULL);
//
//	std::string retStr = pBuf;
//
//	delete []pBuf;
//	delete []pwBuf;
//
//	pBuf = NULL;
//	pwBuf = NULL;
//
//	return retStr;
//}

static void copyLogFile(){
	char ch;
	FILE *source = NULL, *target = NULL;
	source = fopen(LOG_SOURCE, "r");

	if( source == NULL ){
		LOGE( "cannot open file[%s]\n", LOG_SOURCE);
		return;
	}

	char date[20]={0};
	struct timeval tv;
	gettimeofday(&tv, NULL);
	strftime(date, sizeof(date) / sizeof(*date), "%Y-%m-%dT%H:%M:%S", gmtime(&tv.tv_sec));
	char logFilePath[1024] = {0};
	sprintf(logFilePath, LOG_DEST, LOG_DIR, date);

	target = fopen(logFilePath, "w");
	if( target == NULL ){
	    fclose(source);
	    LOGE( "cannot open file[%s]\n", (logFilePath)?logFilePath:"");
	    return;
	}

	while( ( ch = fgetc(source) ) != EOF )
	  fputc(ch, target);

	LOGE("File copied to %s successfully.\n", logFilePath);

	fclose(source);
	fclose(target);
}

//Check if SoundPairing is disabled
static bool sForceDisabledSp = false;

//Check if SoundPairing Error log is enabled
static bool sSpErrLogEnabled = false;

AudioTest* AudioTest::sAudioTest=NULL;

#ifdef ANDROID
void soundpairSenderCb(const char* cb_type, void* data){
	AudioTest::getInstance()->soundpairSenderCallback(cb_type, data);
}

void AudioTest::soundpairSenderCallback(const char* cb_type, void* data){
#ifdef AUTO_TEST
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
#endif
}
#endif

void soundpairReceiverCb(const char* cb_type, void* data){
	AudioTest::getInstance()->soundpairReceiverCallback(cb_type, data);
}

void AudioTest::soundpairReceiverCallback(const char* cb_type, void* data){//cam ws server side
#ifdef AUTO_TEST
	LOGE( "cb_type:[%s]\n", (cb_type)?cb_type:"");
	if(NULL != cb_type){
		string strMsg(cb_type);
		if(0 == strMsg.compare(SoundPair_Config::MSG_AUTO_TEST_BEGIN)){
			tmpRet.str("");
			tmpRet.clear();
			FreqAnalyzer::getInstance()->endToTrace();
			FreqAnalyzer::getInstance()->reset();
			resetBuffer();
			acquireAutoTestCtrlObj();
			LOGI("soundpairReceiverCallback(), broadcast, mbAutoTestBeginOnReceiver=[true]\n");

			mbAutoTestBeginOnReceiver = true;
#ifdef CAM_ENV
			Delegate_BeginToSaveResult();
#endif
			pthread_cond_broadcast(&mAutoTestCtrlObjCond);
			releaseAutoTestCtrlObj();
		}else if((0 == strMsg.compare(SoundPair_Config::MSG_AUTO_TEST_END) || 0 == strMsg.compare(MSG_WS_CLOSED)) && mbAutoTestBeginOnReceiver){
			FreqAnalyzer::getInstance()->endToTrace();
			acquireAutoTestCtrlObj();
			LOGI("soundpairReceiverCallback(),  mbAutoTestBeginAnalyzeOnReceiver=[false]\n");
#ifdef CAM_ENV
			Delegate_EndToSaveResult();
#endif
			setAutoTestBeginAnalyzeOnReceiver(false);
			mbAutoTestBeginOnReceiver = false;
			mbSenderAcked = true;
			releaseAutoTestCtrlObj();

			acquireSyncObj();
			pthread_cond_broadcast(&mSyncObjCond);
			releaseSyncObj();
		}else{
			if(mbAutoTestBeginOnReceiver){
				std::vector<std::string> msg = split(strMsg, SoundPair_Config::BT_MSG_DIVIDER);
				if(msg.size() == 3){
					LOGI( "soundpairReceiverCallback(), bIsSenderMode, msg[0] = [%s], msg[1] = [%s], msg[2] = [%s]\n",msg[0].c_str(), msg[1].c_str(), msg[2].c_str());
					if(0 == msg[0].compare(SoundPair_Config::BT_MSG_ACK) && 0 == mstrCurTransferTs.compare(msg[1])){
						curECCode = msg[2];
						curEncodeMark = curCode+curECCode;

						acquireAutoTestCtrlObj();
						setAutoTestBeginAnalyzeOnReceiver(true);
						LOGI("soundpairReceiverCallback(), broadcast, mbAutoTestBeginAnalyzeOnReceiver=[true]\n");
						pthread_cond_broadcast(&mAutoTestCtrlObjCond);
						releaseAutoTestCtrlObj();

						char msgSent[1024]={0};
						sprintf(msgSent, SoundPair_Config::BT_MSG_FORMAT.c_str(), SoundPair_Config::BT_MSG_ACK.c_str(), mstrCurTransferTs.c_str());
						int iRet = send_msg_to_client(msgSent);
						LOGI("soundpairReceiverCallback(), send_msg_to_client, iRet=[%d]\n", iRet);

						acquireSendPairingCodeObj();
						LOGI("soundpairReceiverCallback(), broadcast, mstrCurTransferCode=[%s]\n", mstrCurTransferCode.c_str());
						mbSenderAcked = true;
						pthread_cond_broadcast(&mSendPairingCodeObjCond);
						releaseSendPairingCodeObj();
					}
				}
			}else{
				LOGI("soundpairReceiverCallback(), broadcast, mbAutoTestBeginOnReceiver=[false]\n");
			}
		}
	}
#endif
}

void AudioTest::sendPlayPairingCode(string strCode){
#ifdef AUTO_TEST
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

			acquireSendPairingCodeObj();
			getTimeSpecByDelay(outtime, 5000);
			LOGI("sendPlayPairingCode(), begin wait, mstrCurTransferCode=[%s]\n", mstrCurTransferCode.c_str());
			pthread_cond_timedwait(&mSendPairingCodeObjCond, &mSendPairingCodeObj, &outtime);
			LOGI("sendPlayPairingCode(), exit wait, mbSenderAcked=%d\n", mbSenderAcked);
			if(mbSenderAcked){
				AudioBufferMgr::getInstance()->recycleAllBuffer();
			}
			releaseSendPairingCodeObj();
		}while(!mbSenderAcked && is_websocket_server_inited() && !mbStopControlThreadFlag);

		mstrCurTransferCode = "";
		mstrCurTransferTs = "";
		mbSenderAcked = false;
	}
#endif
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
mbAboveThreshold(false),
miSyncObjInvokeCount(0),
miSendPairingCodeObjInvokeCount(0),
miAutoTestCtrlObjInvokeCount(0),
miThresholdCtrlObjInvokeCount(0),
miStopAnalysisBufIdx(-1),
mbDetectStartFlag(false){
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
#ifdef AUTO_TEST
	deinit_websocket_server();
#endif
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
#ifdef AUTO_TEST
	if(bAutoTest)
		init_websocket_server(soundpairReceiverCb);
#endif

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
	sForceDisabledSp = false;
	sSpErrLogEnabled = false;
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
#ifdef AUTO_TEST
		int iRet = send_msg_to_server(SoundPair_Config::MSG_AUTO_TEST_BEGIN.c_str());
		LOGE("startAutoTest(), send_msg_to_server, iRet:[%d]\n", iRet);
		bRet = true;
#endif
	}

#ifdef ANDROID
	LOGI("startAutoTest()+, bRet:%d, isReceiverMode():%d, isAutoTestMode():%d\n",bRet, isReceiverMode(), isAutoTestMode());
	if(bRet && (isReceiverMode() || isAutoTestMode()))
		bRet = startGenerateTone(strInitCode, iDigitalToTest);
#endif
//EXIT:
	LOGE("startAutoTest()--\n");
	return bRet;
}

bool AudioTest::startPairingAnalysis(){
	deinitTestRound();
	mbPairingAnalysisMode = true;
	bool bRet = false;
	if(false == isSenderMode()){
		sForceDisabledSp = false;
		sSpErrLogEnabled = false;

		bRet = startAnalyzeTone();
#ifndef ANDROID
		if(bRet){
			//checkLogFiles();
			//setInvalidWifi();
			deleteFile(MONITOR_PROCESS_FLAG);

			int iRet = checkSpEnabled();//system("/beseye/cam_main/cam-handler -chk_sp_enabled") >> 8;
			LOGE("startPairingAnalysis(),chk_sp_enabled, iRet:%d\n", iRet);
			if(CMD_RET_CODE_SP_DISABLED == iRet){
				sForceDisabledSp = true;
				LOGE("startPairingAnalysis(), chk_sp_enabled, disabled SP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
			}

			sSpErrLogEnabled = isFileExist("/beseye/config/sp_error_enabled");

			miPairingReturnCode = -1;
			slLastTimeCheckToken = time_ms();
			AudioBufferMgr::getInstance()->setRecordMode(true);
			FreqAnalyzer::getInstance()->setIFreqAnalyzeResultCB(this);

			LOGE("startPairingAnalysis(), begin join mBufRecordThread\n");
			pthread_join(mBufRecordThread, NULL);
			LOGE("startPairingAnalysis(), end join mBufRecordThread\n");
			//temp remove !!!!!!!!!!!!
//			LOGI("startAutoTest(), begin join mAnalysisThread\n");
//			pthread_join(mAnalysisThread, NULL);

			LOGE("startPairingAnalysis(), end join\n");
		}
#endif
	}

	LOGE("startPairingAnalysis()--\n");
	return bRet;
}

bool AudioTest::stopAutoTest(){
	bool bRet = false;
	if(isSenderMode()){
#ifdef AUTO_TEST
		int iRet = send_msg_to_server(SoundPair_Config::MSG_AUTO_TEST_END.c_str());
		LOGE("stopAutoTest(), send_msg_to_server, iRet:[%d]\n", iRet);
#endif
	}
	stopGenerateTone();
	stopAnalyzeTone();
	deinitTestRound();
	LOGE("----\n");
	return bRet;
}

bool AudioTest::playTone(string strCode, bool bNeedEncode){
#ifndef CAM_ENV
	FreqGenerator::getInstance()->setOnPlayToneCallback(this);
	return FreqGenerator::getInstance()->playCode2(strCode, bNeedEncode);
#else
	return false;
#endif
}

bool AudioTest::startGenerateTone(string strInitCode, int iDigitalToTest){
	bool bRet = false;
	int iErrno = 0;
	//LOGE("AudioTest::startGenerateTone(),  mControlThread,%d\n", mControlThread);
	if(!mControlThread){
		this->strInitCode = "0123456789abcdef";//strInitCode;
		miDigitalToTest = iDigitalToTest;
		if (0 != (iErrno = pthread_create(&mControlThread, NULL, AudioTest::runAutoTestControl, this))) {
			LOGE("AudioTest::startAutoTet(), error when create mControlThread,%d\n", iErrno);
		}else{
			LOGE("AudioTest::startAutoTet(), create mControlThread,%d\n", iErrno);
			bRet = true;
#ifdef ANDROID
			pthread_setname_np(mControlThread, "ControlThread");
#endif	
		}
	}
//EXIT:
	return bRet;
}

bool AudioTest::stopGenerateTone(){
	LOGE("stopGenerateTone()+\n");
	mbStopControlThreadFlag = true;
	return true;
}

bool AudioTest::startAnalyzeTone(){
	bool bRet = false;
	int iErrno = 0;
	if(!mBufRecordThread){
		if (0 != (iErrno = pthread_create(&mBufRecordThread, NULL, AudioTest::runAudioBufRecord, this))) {
			LOGE("AudioTest::startAutoTet(), error when create mBufRecordThread,%d\n", iErrno);
		}else{
			bRet = true;
#ifdef ANDROID
			pthread_setname_np(mBufRecordThread, "BufRecordThread");
#endif
		}
	}

	if(bRet && !mAnalysisThread){
		if (0 != (iErrno = pthread_create(&mAnalysisThread, NULL, AudioTest::runAudioBufAnalysis, this))) {
			LOGE("AudioTest::startAutoTet(), error when create mAnalysisThread,%d\n", iErrno);
		}else{
			bRet = true;
#ifdef ANDROID
			pthread_setname_np(mAnalysisThread, "AnalysisThread");
#endif	
		}
	}
//EXIT:
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
					FreqGenerator::getInstance()->playCode3(tester->curCode, false);
					//FreqGenerator::getInstance()->playCode2(tester->curCode, true);
	#endif
			}else{
	#ifndef CAM_ENV
				tester->curECCode = FreqGenerator::getECCode(tester->curCode);
				tester->curEncodeMark = /*SoundPair_Config::encodeConsecutiveDigits*/(tester->curCode+tester->curECCode);
	#endif

	#ifdef CAM_ENV
				while(bIsReceiverMode && !tester->mbAutoTestBeginOnReceiver && !tester->mbStopControlThreadFlag){
					LOGI("runAutoTestControl(), begin wait auto test\n");
					tester->acquireAutoTestCtrlObj();
					pthread_cond_wait(&tester->mAutoTestCtrlObjCond, &tester->mAutoTestCtrlObj);
					tester->releaseAutoTestCtrlObj();
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
			}
	#ifdef CAM_ENV
			else if(!tester->mbAutoTestBeginOnReceiver){
				LOGE("runAutoTestControl(), continue to rewait---\n");
				continue;
			}
    #endif

			LOGI("runAutoTestControl(), enter lock\n");
			tester->acquireSyncObj();
			//tester->tmpRet.str("");
			tester->tmpRet.clear();
			//LOGI("runAutoTestControl(), beginToTrace\n");
			FreqAnalyzer::getInstance()->beginToTrace(tester->curCode);
			LOGI("runAutoTestControl(), ----------------------------begin wait\n");
			pthread_cond_wait(&tester->mSyncObjCond, &tester->mSyncObj);
			LOGI("runAutoTestControl(), ----------------------------exit wait\n");
			tester->releaseSyncObj();
		}

		tester->mControlThread = 0;
	}else{
		LOGE("runAutoTestControl(), tester is null\n");
	}

	Delegate_detachCurrentThread();
	LOGE("runAutoTestControl()---\n");
//#endif
	return 0;
}

void AudioTest::setDetectStartFlag(bool flag){
	mbDetectStartFlag = flag;
}

bool AudioTest::getDetectStartFlag(){
	return mbDetectStartFlag;
}

typedef enum{
	PAIRING_NONE,
	PAIRING_INIT				,
    PAIRING_WAITING 			,
    PAIRING_ANALYSIS 			,
    PAIRING_ERROR				,
    PAIRING_DONE
}Pairing_Mode;

typedef enum{
	PAIRING_ERR_BASE,
	PAIRING_ERR_MAC_NOT_FOUND,
	PAIRING_ERR_COUNT
}Pairing_Err_Type;

static Pairing_Mode sPairingMode = PAIRING_INIT;
static Pairing_Mode sPedningPairingMode = PAIRING_INIT;
static Pairing_Err_Type sPairingErrType = PAIRING_ERR_BASE;
static const int ERROR_LED_PERIOD = 10;
static bool sbNeedToInitBuf = false;
static int sCurLEDCnt = 0;
static msec_t lTImeToSaveErrorLog = 0;

void changePairingMode(Pairing_Mode mode){
	if(sPairingMode != mode)
		LOGW("sPairingMode:%d, mode:%d\n", sPairingMode, mode);

	if(PAIRING_ERROR == sPairingMode && sCurLEDCnt <= ERROR_LED_PERIOD){
		LOGW("---sPedningPairingMode:%d\n", sPedningPairingMode);
		sPedningPairingMode = PAIRING_INIT;//mode;
	}else{
		if(PAIRING_ANALYSIS == sPairingMode && mode == PAIRING_ERROR){
			if(sSpErrLogEnabled){
				lTImeToSaveErrorLog = time_ms() + 10000;
				LOGE("Need to save error log at %u\n", lTImeToSaveErrorLog);
			}
			//try to restart audio
			stopReceiveAudioBuf();
		}

		if(mode == PAIRING_ERROR){
			sCurLEDCnt = 0;
		}

		if(PAIRING_ERROR == sPairingMode || (PAIRING_ANALYSIS == sPairingMode && PAIRING_INIT == mode)){
			sbNeedToInitBuf = true;
			sPairingMode = PAIRING_INIT;
			sPairingErrType = PAIRING_ERR_BASE;
			setLEDMode(LED_MODE_SOLID_GB);

//			sPairingMode = (PAIRING_NONE==sPedningPairingMode)?mode:sPedningPairingMode;
//			sPedningPairingMode = PAIRING_NONE;
		}else{
			if(PAIRING_INIT == mode){
				setLEDMode(LED_MODE_SOLID_GB);
			}else if(PAIRING_WAITING == mode){
				setLEDMode(LED_MODE_BLINK_G);
			}else if(PAIRING_ANALYSIS == mode){
				setLEDMode(LED_MODE_BLINK_B);
			}else if(PAIRING_ERROR == mode){
				if(sPairingErrType == PAIRING_ERR_MAC_NOT_FOUND){
					setLEDMode(LED_MODE_CYCLE_R_B);
				}else{
					setLEDMode(LED_MODE_SOLID_R);
				}
			}else if(PAIRING_DONE == mode){
				setLEDMode(LED_MODE_SOLID_G);
			}
			sPairingMode = mode;
		}
	}
	//LOGW("---sPairingMode:%d\n", sPairingMode);
}

void checkLEDByMode(){
	if(PAIRING_ERROR == sPairingMode && sCurLEDCnt > ERROR_LED_PERIOD){
		//sPairingMode = (PAIRING_NONE==sPedningPairingMode)?PAIRING_WAITING:sPedningPairingMode;
		changePairingMode(PAIRING_INIT);
		sPairingErrType = PAIRING_ERR_BASE;
		//setLEDMode(LED_MODE_SOLID_GB);

		//sPairingMode = PAIRING_INIT;
		//sPedningPairingMode = PAIRING_NONE;
	}
}

void setLedLight(int bRedOn, int bGreenOn, int bBlueOn){
#ifndef ANDROID
//	static char jsonData[BUF_SIZE]={0};
//	setLEDLight(jsonData, (bRedOn) | (bGreenOn<<1) | (bBlueOn<<2));
	static char cmd[BUF_SIZE]={0};
	//sprintf(cmd, "/beseye/cam_main/cam-handler -setled %d", ((bRedOn) | (bGreenOn<<1) | (bBlueOn<<2)));
	memset(cmd,0,BUF_SIZE);
	sprintf(cmd, "{\"index\":0, \"status\":%d}", bRedOn);
	postCGI("http://localhost/sray/setLEDSetting.cgi", cmd);

	memset(cmd,0,BUF_SIZE);
	sprintf(cmd, "{\"index\":1, \"status\":%d}", bGreenOn);
	postCGI("http://localhost/sray/setLEDSetting.cgi", cmd);

	memset(cmd,0,BUF_SIZE);
	sprintf(cmd, "{\"index\":2, \"status\":%d}", bBlueOn);
	postCGI("http://localhost/sray/setLEDSetting.cgi", cmd);
	//int iRet = system(cmd);
	//LOGW("cmd:[%s], iRet:%d\n", cmd, iRet);
#endif
}

static const char* SES_TOKEN_PATH 			= "/beseye/config/ses_token";
static pthread_t sThreadVerifyToken;

void* AudioTest::verifyToken(void* userdata){
	LOGE("+\n");
	AudioTest* tester = (AudioTest*)userdata;
	while(!tester->mbStopAnalysisThreadFlag){
		msec_t lDelta = time_ms() - slLastTimeCheckToken;
		if((PAIRING_ANALYSIS != sPairingMode && lDelta > TIME_TO_CHECK_TOKEN)/* || (PAIRING_ANALYSIS == sPairingMode && lDelta >TIME_TO_CHECK_TOKEN_ANALYSIS_PERIOD)*/){
			if(readFromFile(SES_TOKEN_PATH)){
				//if(0 == (system("/beseye/cam_main/beseye_network_check") >> 8)){
					if(0 == (invokeSystemWithTimeout("/beseye/cam_main/beseye_token_check", 40) >> 8)){
					//if(0 == checkTokenValid()){
						AudioTest::getInstance()->setPairingReturnCode(CMD_RET_CODE_TOKEN_STILL_VALID);
						setLedLight(0,1,0);
						AudioTest::getInstance()->stopAutoTest();
						exit(0);
					}
				//}
			}
			slLastTimeCheckToken = time_ms();

			char WifiLinkInfo[BUF_SIZE]={0};
			int iTrials = 0, iRet = RET_CODE_OK;
			do{
				iRet = getWiFiStatus(WifiLinkInfo);
				if(RET_CODE_OK == iRet){
					LOGE( "WifiLinkInfo:[%s]\n", WifiLinkInfo);
				}
			}while(RET_CODE_OK != iRet && iTrials++ < 3);
		}


		if(AudioTest::getInstance()->isPairingAnalysisMode()){
			if(PAIRING_ERROR == sPairingMode){
				sCurLEDCnt++;
				checkLEDByMode();
			}
		}

		if(0 < lTImeToSaveErrorLog && lTImeToSaveErrorLog < time_ms()){
			LOGE("Time to save error log\n");
			copyLogFile();
			checkLogFiles();
			lTImeToSaveErrorLog = 0;
		}
		sleep(TIME_TO_CHECK_LED);
	}
	sThreadVerifyToken = NULL;
	LOGE("-\n");
	return 0;
}

static int siOffset = 0;

void AudioTest::setOffset(int iOffset){
	LOGE("setOffset(), iOffset:%d\n", iOffset);
	siOffset=iOffset;
}

//#define SEG_PROFILE

#ifdef SEG_PROFILE
static int iHaveTest = 0;
static msec_t lTsToTest = 0;
static FILE *fp = NULL;
static unsigned char* charBufTmp = NULL;
#endif

//static int iOldLen = 0;
void writeBuf(unsigned char* charBuf, int iLen){

#ifdef SEG_PROFILE
	if(0 == lTsToTest){
		lTsToTest = time_ms();
		LOGE("writeBuf, lTsToTest:%lld\n", lTsToTest);
	}

	if(NULL == fp && 0 == iHaveTest && (time_ms() - lTsToTest) > 1000){
		LOGE("writeBuf, (%lld - %lld)\n", time_ms(), lTsToTest);

		iHaveTest = 1;
		char* filePath = "/beseye//beseye_audio_16k.pcm";
		fp=fopen(filePath, "r");
		if(!fp){
			LOGE("failed to %s\n", filePath);
		}else{
			LOGE("Succeed to %s\n", filePath);
		}

		shortsRecBuf = AudioBufferMgr::getInstance()->getAvailableBuf();

//		int iIdx = AudioBufferMgr::getInstance()->getBufIndex(shortsRecBuf);
//		LOGE("writeBuf, start idx = %d\n", iIdx);
		iCurIdx = siOffset;
	}


	if(fp){
		if(charBufTmp){
			free(charBufTmp);
			charBufTmp = NULL;
		}
		int iLenTmp = iLen/2;

		charBufTmp = (unsigned char*) malloc(iLenTmp*sizeof(char));
		memset(charBufTmp, 0, iLenTmp);

		fread(charBufTmp, sizeof(char), iLenTmp, fp);
		//LOGE("writeBuf, iLen:%d, iLenTmp:%d\n", iLen, iLenTmp);

		int iCountFrame = iLen/iAudioFrameSize;
		for(int i = 0 ; i < iCountFrame; i++){
			charBuf[iAudioFrameSize*i+3] = charBufTmp[2*i+1];
			charBuf[iAudioFrameSize*i+2] = charBufTmp[2*i];
//			charBufTmp[iAudioFrameSize*i+3] = charBuf[2*i+1];
//			charBufTmp[iAudioFrameSize*i+2] = charBuf[2*i];
		}

		//charBuf = charBufTmp;
		//iLen = iLen*2;

		if(feof(fp)){
			fclose(fp);
			fp = NULL;
			LOGE("Close due to eof\n");
		}
	 }
#endif

	checkSystemProcess();

	if(!AudioTest::getInstance()->isPairingAnalysisMode() && !AudioTest::getInstance()->isAutoTestBeginAnalyzeOnReceiver()){
		return;
	}

	//Check network and token here
	if(AudioTest::getInstance()->isPairingAnalysisMode()){
		if(NULL == sThreadVerifyToken){
			int iErrno = 0;
			if (0 != (iErrno = pthread_create(&sThreadVerifyToken, NULL,  AudioTest::verifyToken, AudioTest::getInstance()))) {
				LOGE("writeBuf, error when create thread to verify token,%d\n", iErrno);
			}
		}
	}

	if(sbNeedToInitBuf){
		LOGE("init buffer for entering!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
		shortsRecBuf = NULL;
		iCurIdx = 0;
		ANALYSIS_THRESHHOLD_MONITOR_CNT = 0;
		ANALYSIS_THRESHHOLD_MONITOR = 0;
		ANALYSIS_START_THRESHHOLD = 0;
		ANALYSIS_END_THRESHHOLD = 0;
		siAboveThreshHoldCount = 0;
		siUnderThreshHoldCount = 0;
		sbNeedToInitBuf = false;
	}

	int iIdxOffset = -iCurIdx;
	msec_t lTs1 = lTsRec;
	int iCountFrame = iLen/iAudioFrameSize;
	static bool bHaveShowNoBuf = false;
	for(int i = 0 ; i < iCountFrame; i++){
		if(NULL == shortsRecBuf || 0 == shortsRecBuf->size()){
			lTs1 = (lTsRec+=SoundPair_Config::FRAME_TS);//System.currentTimeMillis();
			int iCurTrial = 0;
			while(NULL == (shortsRecBuf = AudioBufferMgr::getInstance()->getAvailableBuf())){
				//nanosleep(&sleepValue, NULL);
				iCurTrial++;
				if(iCurTrial > MAX_TRIAL){
					if(!bHaveShowNoBuf){
						LOGW("Can not get available buf!!!!!!!!!!!!!!!!!!!!!!!!!\n");
						bHaveShowNoBuf = true;
					}
					//stop analysis
					AudioBufferMgr::getInstance()->setRecordMode(true);

					//AudioTest::getInstance()->resetBuffer();
					int iStopAnalysisIdx = AudioBufferMgr::getInstance()->getLastDataBufIndex();
					if(-1 == AudioTest::getInstance()->getStopAnalysisBufIdx()){
						if(-1 != iStopAnalysisIdx){
							AudioTest::getInstance()->setStopAnalysisBufIdx(iStopAnalysisIdx);
							LOGE("miStopAnalysisBufIdx:%d\n",iStopAnalysisIdx);
						}else{
							LOGE("reset because miStopAnalysisBufIdx:%d\n",iStopAnalysisIdx);
							AudioTest::getInstance()->deinitTestRound();
						}
					}else{
						//LOGE("miStopAnalysisBufIdx != -1\n");
					}

					FreqAnalyzer::getInstance()->setDetectLowSound(true);
					break;
				}
			}

			if(NULL != shortsRecBuf){
				memset(&shortsRecBuf[0], 0, sizeof(short)*SoundPair_Config::FRAME_SIZE_REC);
				bHaveShowNoBuf = false;
			}

			iIdxOffset = i;
			iCurIdx = 0;
			//LOGE("writeBuf(), get rec buf at %lld, iIdxOffset:%d, iCurIdx:%d, shortsRecBuf->size():%d\n", lTs1, iIdxOffset, iCurIdx, shortsRecBuf->size() );
		}

		if(NULL != shortsRecBuf){
			iCurIdx = i - iIdxOffset;
			shortsRecBuf[iCurIdx] = (((short)charBuf[iAudioFrameSize*i+3])<<8 | (charBuf[iAudioFrameSize*i+2]));

			/*if(AudioTest::getInstance()->isPairingAnalysisMode())*/
			if(0 == siRefCount%4){
				short val = abs(shortsRecBuf[iCurIdx]);
				if(val > sMaxValue){
					sMaxValue = val;
				}

				if(0 == siRefCount%ANALYSIS_THRESHHOLD_CK_LEN){
					if(ANALYSIS_MAX_AUDIO_VALUE < sMaxValue){
						LOGW("-------------------------------------------------------->ANALYSIS_MAX_AUDIO_VALUE:%d < sMaxValue:%d, set mic gain\n", ANALYSIS_MAX_AUDIO_VALUE, sMaxValue);
						//system("/beseye/cam_main/cam-handler -setgain 25") >> 8;
						setMicrophoneGain(getSPGain());
						saveLogFile("/beseye/sp-gain-set");
					}

					//LOGW("-------------------------------------------------------->sMaxValue:%d\n", sMaxValue);

					if(PAIRING_INIT == sPairingMode){
						ANALYSIS_THRESHHOLD_MONITOR = ((ANALYSIS_THRESHHOLD_MONITOR*(ANALYSIS_THRESHHOLD_MONITOR_CNT))+sMaxValue)/(++ANALYSIS_THRESHHOLD_MONITOR_CNT);
						LOGW("-------------------------------------------------------->ANALYSIS_THRESHHOLD_MONITOR:%d, ANALYSIS_THRESHHOLD_MONITOR_CNT:%d\n", ANALYSIS_THRESHHOLD_MONITOR, ANALYSIS_THRESHHOLD_MONITOR_CNT);
						if(ANALYSIS_THRESHHOLD_MONITOR_CNT >= 10){
							if(ANALYSIS_THRESHHOLD_MONITOR < ANALYSIS_START_THRESHHOLD_MIN){
								ANALYSIS_START_THRESHHOLD = ANALYSIS_START_THRESHHOLD_MIN;
							}else{
								ANALYSIS_START_THRESHHOLD = ((ANALYSIS_THRESHHOLD_MONITOR) < ANALYSIS_START_THRESHHOLD_MAX)?(ANALYSIS_THRESHHOLD_MONITOR):ANALYSIS_START_THRESHHOLD_MAX;
							}
							ANALYSIS_END_THRESHHOLD = (ANALYSIS_THRESHHOLD_MONITOR + ANALYSIS_START_THRESHHOLD)/2;
							LOGW("-------------------------------------------------------->ANALYSIS_START_THRESHHOLD:%d, ANALYSIS_END_THRESHHOLD:%d\n", ANALYSIS_START_THRESHHOLD, ANALYSIS_END_THRESHHOLD);
							//sPairingMode = PAIRING_WAITING;
							changePairingMode(PAIRING_WAITING);
							ANALYSIS_END_THRESHHOLD_DETECT = -1;
						}
					}else{
						if(0 > ANALYSIS_END_THRESHHOLD_DETECT && AudioTest::getInstance()->getDetectStartFlag()){
							ANALYSIS_THRESHHOLD_MONITOR_DETECT = ((ANALYSIS_THRESHHOLD_MONITOR_DETECT*(ANALYSIS_THRESHHOLD_MONITOR_DETECT_CNT))+sMaxValue)/(++ANALYSIS_THRESHHOLD_MONITOR_DETECT_CNT);
							if(ANALYSIS_THRESHHOLD_MONITOR_CNT >= 25){
								if(ANALYSIS_THRESHHOLD_MONITOR_DETECT > ANALYSIS_END_THRESHHOLD){
									ANALYSIS_END_THRESHHOLD_DETECT = ANALYSIS_THRESHHOLD_MONITOR_DETECT;
								}else{
									ANALYSIS_END_THRESHHOLD_DETECT = 0;
								}
								LOGW("-------------------------------------------------------->ANALYSIS_THRESHHOLD_MONITOR_DETECT:%d, ANALYSIS_END_THRESHHOLD_DETECT:%d\n", ANALYSIS_THRESHHOLD_MONITOR_DETECT, ANALYSIS_END_THRESHHOLD_DETECT);
							}
						}

						if(ANALYSIS_START_THRESHHOLD < sMaxValue){
							LOGW("-------------------------------------------------------->sMaxValue:%d, siAboveThreshHoldCount:%d, siUnderThreshHoldCount:%d\n", sMaxValue, siAboveThreshHoldCount, siUnderThreshHoldCount);
							if(0 == siAboveThreshHoldCount){
								FreqAnalyzer::getInstance()->setSessionOffsetForAmp(-iCurIdx);
							}

							siAboveThreshHoldCount++;
							if(false == AudioTest::getInstance()->getAboveThresholdFlag() && siAboveThreshHoldCount >= ANALYSIS_AB_THRESHHOLD_CK_CNT && PAIRING_WAITING == sPairingMode){
								LOGE("trigger analysis-----\n");
								//trigger analysis
								if(AudioTest::getInstance()->isPairingAnalysisMode()){
									AudioTest::getInstance()->setPairingReturnCode(-1);
									changePairingMode(PAIRING_ANALYSIS);
									FreqAnalyzer::getInstance()->setDetectLowSound(false);
									AudioBufferMgr::getInstance()->trimAvailableBuf((((ANALYSIS_THRESHHOLD_CK_LEN*ANALYSIS_AB_THRESHHOLD_CK_CNT)/SoundPair_Config::FRAME_SIZE_REC)*2));
									AudioBufferMgr::getInstance()->setRecordMode(false);
								}
								AudioTest::getInstance()->setAboveThresholdFlag(true);
								siAboveThreshHoldCount = 0;
							}
							siUnderThreshHoldCount = 0;
						}else if(ANALYSIS_END_THRESHHOLD > sMaxValue || (0 < ANALYSIS_END_THRESHHOLD_DETECT && ANALYSIS_END_THRESHHOLD_DETECT > sMaxValue && PAIRING_WAITING < sPairingMode)){
							siUnderThreshHoldCount++;
							if((PAIRING_ANALYSIS ==  sPairingMode/* || AudioTest::getInstance()->getAboveThresholdFlag()*/) && siUnderThreshHoldCount >= ANALYSIS_UN_THRESHHOLD_CK_CNT){
								LOGE("trigger stop analysis-----\n");
								//stop analysis
								if(AudioTest::getInstance()->isPairingAnalysisMode()){
									AudioBufferMgr::getInstance()->setRecordMode(true);
								}
								int iStopAnalysisIdx = AudioBufferMgr::getInstance()->getBufIndex(shortsRecBuf);
								if(-1 == AudioTest::getInstance()->getStopAnalysisBufIdx()){
									AudioTest::getInstance()->setStopAnalysisBufIdx(iStopAnalysisIdx);
									LOGE("miStopAnalysisBufIdx:%d\n",iStopAnalysisIdx);
								}else{
									LOGE("miStopAnalysisBufIdx != -1\n");
								}

								//AudioTest::getInstance()->setAboveThresholdFlag(true);
								FreqAnalyzer::getInstance()->setDetectLowSound(true);

								siUnderThreshHoldCount = 0;
							}
							siAboveThreshHoldCount = 0;
						}else{
							siUnderThreshHoldCount = 0;
							siAboveThreshHoldCount = 0;
						}
					}

					//siRefCount = 0;
					sMaxValue = 0;
				}
			}
			siRefCount++;

			//if(PAIRING_INIT != sPairingMode){
				if(iCurIdx == shortsRecBuf->size()-1){
					//LOGE("writeBuf(), add rec buf at %lld, iIdxOffset:%d, iCurIdx:%d, shortsRecBuf->size():%d\n", lTs1, iIdxOffset, iCurIdx, shortsRecBuf->size() );
					AudioBufferMgr::getInstance()->addToDataBuf(lTs1, shortsRecBuf, shortsRecBuf->size());
					shortsRecBuf = NULL;
				}
			//}
		}else{
			//LOGW("shortsRecBuf is NULL\n");
		}
	}
	iCurIdx++;
	lLastTimeToBufRec = time_ms();
	//Delegate_WriteAudioBuffer2();
}

void AudioTest::setAutoTestBeginAnalyzeOnReceiver(bool flag){
	LOGI("setAutoTestBeginAnalyzeOnReceiver(), ++, flag:%d\n", flag);
	mbAutoTestBeginAnalyzeOnReceiver = flag;
}

void AudioTest::setAboveThresholdFlag(bool flag){
	LOGI("setAboveThresholdFlag(), ++, flag:%d\n", flag);
	acquireThresholdCtrlObj();
	bool oldflag = mbAboveThreshold;
	mbAboveThreshold=flag;
	if(!oldflag && mbAboveThreshold){
		LOGI("setAboveThresholdFlag(), broadcast\n");
		pthread_cond_broadcast(&mThresholdCtrlObjCond);
	}
	releaseThresholdCtrlObj();
	LOGI("setAboveThresholdFlag()--\n");

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

	int iRet = checkSPEnv();//system("/beseye/cam_main/cam-handler -setspenv 25") >> 8;
	LOGE("runAudioBufRecord(), check sp env, iRet:%d\n", iRet);
	if(CMD_RET_CODE_NEED_REBOOT == iRet){
		LOGE("runAudioBufRecord(), need to reboot due to change config\n");
		saveLogFile("/beseye/reboot-pairing");
		char jsonData[BUF_SIZE]={0};
		httpGetViaCGI(API_REBOOT_DEV, jsonData);
		//system("reboot");
	}else{
		//char* session = "0e4bba41bef24f009337727ce44008cd";//[SESSION_SIZE];
		do{
			if(sPairingMode == PAIRING_INIT){
				char session[SESSION_SIZE];
				memset(session, 0, sizeof(session));
				int iTrial = 0;
				int iRet = 0;
				do{
					if(0 < iTrial++){
						LOGE("Get session failed, iTrial:%d", iTrial);
						sleep(iTrial);
						memset(session, 0, sizeof(session));
					}
					iRet = GetSession(HOST_NAME, session);
				}while(0 != iRet && iTrial < 5);

				if(iRet != 0){
					LOGE("Get session failed.");
				}else{
					LOGE("runAudioBufRecord(), begin to GetAudioBufCGI\n");
					changePairingMode(PAIRING_INIT);

					int res = GetAudioBufCGI(HOST_NAME_AUDIO, "receiveRaw", session, writeBuf);
					LOGE("GetAudioBufCGI:res(%d), mbStopBufRecordFlag:%d\n",res, tester->mbStopBufRecordFlag);
					//Delegate_CloseAudioDevice2();
				}
			}else{
				LOGE("sPairingMode is not PAIRING_INIT, wait a while\n");
				sleep(2);
			}
		}while(false == tester->mbStopBufRecordFlag);
	}
#endif
	LOGE("runAudioBufRecord()-\n");
	tester->mBufRecordThread = 0;
	Delegate_detachCurrentThread();
	return 0;
}

void* AudioTest::runAudioBufAnalysis(void* userdata){
	LOGE("runAudioBufAnalysis()+\n");
	AudioTest* tester = (AudioTest*)userdata;
	tester->mbStopAnalysisThreadFlag = false;
	Ref<BufRecord> buf;

	struct timespec yieldtime;
	yieldtime.tv_nsec = 1000000;//1 ms
	//getTimeSpecByDelay(yieldtime, 1);

	while(!tester->mbStopAnalysisThreadFlag){
		while(!tester->isPairingAnalysisMode() && tester->isReceiverMode() && !tester->isAutoTestBeginAnalyzeOnReceiver() && !tester->mbStopAnalysisThreadFlag){
			LOGI("runAudioBufAnalysis(), begin wait auto test, [%d, %d, %d, %d]\n", tester->isPairingAnalysisMode(), tester->isReceiverMode(), tester->mbAutoTestBeginAnalyzeOnReceiver, tester->mbStopAnalysisThreadFlag);
			tester->acquireAutoTestCtrlObj();
			pthread_cond_wait(&tester->mAutoTestCtrlObjCond, &tester->mAutoTestCtrlObj);
			tester->releaseAutoTestCtrlObj();
			LOGI("runAudioBufAnalysis(), exit wait auto test, [%d, %d, %d, %d]\n", tester->isPairingAnalysisMode(), tester->isReceiverMode(), tester->mbAutoTestBeginAnalyzeOnReceiver, tester->mbStopAnalysisThreadFlag);
		}

		while(tester->isPairingAnalysisMode() && !tester->getAboveThresholdFlag() && !tester->mbStopAnalysisThreadFlag){
			LOGI("runAudioBufAnalysis(), begin wait threshold\n");
			tester->acquireThresholdCtrlObj();
			pthread_cond_wait(&tester->mThresholdCtrlObjCond, &tester->mThresholdCtrlObj);
			tester->releaseThresholdCtrlObj();
			LOGI("runAudioBufAnalysis(), exit wait threshold\n");
		}

		if(tester->mbStopAnalysisThreadFlag){
			LOGI("runAudioBufAnalysis(), break loop\n");
			break;
		}

		//if(FreqAnalyzer::getInstance()->isDetectLowSound()){
		//	LOGI("runAudioBufAnalysis(), isDetectLowSound is true\n");
		/*if(-1 < miStopAnalysisBufIdx)
			FreqAnalyzer::getInstance()->triggerTimeout();
			AudioTest::getInstance()->setAboveThresholdFlag(false);
			changePairingMode(PAIRING_WAITING);
		}else*/{
			//LOGE("runAudioBufAnalysis()+1\n");
			int iSessionOffset = FreqAnalyzer::getInstance()->getSessionOffset();
			LOGD("runAudioBufAnalysis(), iSessionOffset:%d\n", iSessionOffset);

			if(iSessionOffset > 0)
				buf = tester->getBuf((iSessionOffset/SoundPair_Config::FRAME_SIZE_REC)+1);
			else
				buf = tester->getBuf();

			LOGD("runAudioBufAnalysis(), get buf\n");
			int iCheckIdx = tester->getStopAnalysisBufIdx();
			if(-1 < iCheckIdx && buf->miIndex == iCheckIdx){
				LOGE("runAudioBufAnalysis(), meet miStopAnalysisBufIdx:%d\n", iCheckIdx);
				FreqAnalyzer::getInstance()->triggerTimeout();
				AudioTest::getInstance()->setAboveThresholdFlag(false);
				//changePairingMode(PAIRING_WAITING);
				tester->setStopAnalysisBufIdx(-1);
			}else{
				ArrayRef<short> bufShort = buf->mbBuf;

				if(false == sForceDisabledSp){
					if(0 != iSessionOffset){
						bufShort = AudioBufferMgr::getInstance()->getBufByIndex(buf->miIndex, iSessionOffset, tester->bufSegment);
					}

					//LOGE("runAudioBufAnalysis(), idx:%d, bufShort[0]:%d, bufShort[99]:%d\n", buf->miIndex, bufShort[0], bufShort[99]);

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
				}
			}
			AudioBufferMgr::getInstance()->addToAvailableBuf(buf);

			//to avoid blocking buf record thread
			if(-1 != AudioTest::getInstance()->getStopAnalysisBufIdx()){
				yieldtime.tv_nsec = 30000000;//30 ms
				nanosleep(&yieldtime, NULL);
			}else{
				msec_t lDleta = time_ms() - lLastTimeToBufRec;
				if(lDleta >= 5000){
					LOGI("runAudioBufAnalysis(), lDleta > 5000\n");
					yieldtime.tv_nsec = 10000000;//10 ms
					nanosleep(&yieldtime, NULL);
				}else if(lDleta >= 3000){
					LOGI("runAudioBufAnalysis(), lDleta > 3000\n");
					yieldtime.tv_nsec = 5000000;//5 msO
					nanosleep(&yieldtime, NULL);
				}else if(lDleta >= 1000){
					LOGI("runAudioBufAnalysis(), lDleta > 1000\n");
					yieldtime.tv_nsec = 2000000;//2 ms
					nanosleep(&yieldtime, NULL);
				}else{
					//yieldtime.tv_nsec = 1000000;//1 ms
				}
			}
		}
	}

	LOGE("runAudioBufAnalysis()-\n");
	tester->mAnalysisThread=0;
	Delegate_detachCurrentThread();
	return 0;
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
	setDetectStartFlag(true);
	Delegate_ResetData();
}

void AudioTest::onDetectPostFix(){
	LOGI("onDetectPostFix()\n");
	if(isPairingAnalysisMode())
		setAboveThresholdFlag(false);

	setDetectStartFlag(false);

	acquireAutoTestCtrlObj();
	setAutoTestBeginAnalyzeOnReceiver(false);
	releaseAutoTestCtrlObj();
}

void AudioTest::onAppendResult(string strCode){
	tmpRet<<strCode;
}

#ifndef ANDROID
#define CAM_ENV
#endif

//#include "delegate/account_mgr.h"
void checkPairingResult(string strCode, string strDecodeUnmark){
#ifdef CAM_ENV
	LOGE("++, strCode:[%s]\n", strCode.c_str());

	int iMultiply = SoundPair_Config::getMultiplyByFFTYPE();
	int iPower = SoundPair_Config::getPowerByFFTYPE();

	//const int MAC_LEN = 12;
	const int SSID_MIN_LEN = 2;
	const int SSID_HASH_LEN = 8;
	const int SSID_MAX_LEN = 32*2;
	const int TOKEN_LEN = 4;
	const int PURPOSE_LEN = 6;
	const int MIN_PW_LEN = 0;

	string /*strMAC*/strSSID, strSSIDHash, strUserNum, strPurposeSeg, strPW;

	stringstream retSSID, retPW;
	unsigned char cPurpose = 0;
	unsigned char cSecType = 0;
	unsigned int iReserved = 0;
	uint64 lSSIDHash = 0;
	bool bGuess = false;
	int iSSIDLen = 0;

	if(0 == strCode.find("error")){
		LOGE("Error, trying to get pairing code!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");

		int iRetLen = strDecodeUnmark.length();
		if(iRetLen < (SSID_MIN_LEN + TOKEN_LEN + PURPOSE_LEN +2 /*+ MIN_PW_LEN +2*/)){
			LOGE("iRetLen:[%d] < min len\n",iRetLen, (SSID_MIN_LEN + TOKEN_LEN + PURPOSE_LEN +2/*+ MIN_PW_LEN +2*/));
			return;
		}

		int iFirstDiv = strDecodeUnmark.find(SoundPair_Config::PAIRING_DIVIDER);
		LOGE("iFirstDiv:%d\n", iFirstDiv);
		if(SSID_MIN_LEN <= iFirstDiv){
			int iSecondDiv = strDecodeUnmark.find(SoundPair_Config::PAIRING_DIVIDER, iFirstDiv+1);
			LOGE("iSecondDiv:%d\n", iSecondDiv);
			if(iSecondDiv > iFirstDiv){
				//string ssidSeg = strDecodeUnmark.substr(0, iFirstDiv);

				strSSID = strDecodeUnmark.substr(SSID_MIN_LEN, iFirstDiv - SSID_MIN_LEN);

				strPW = strDecodeUnmark.substr(iFirstDiv+1, (iSecondDiv - (iFirstDiv+1)));

				if(iRetLen >= (iSecondDiv+1+TOKEN_LEN)){
					strUserNum = strDecodeUnmark.substr((iSecondDiv+1), TOKEN_LEN);
				}

				if(iRetLen >= (iSecondDiv+1+TOKEN_LEN +PURPOSE_LEN)){
					strPurposeSeg = strDecodeUnmark.substr((iSecondDiv+TOKEN_LEN+1), PURPOSE_LEN);
				}

				LOGE("possible bundle [%s, %s, %s, %s]\n",strSSID.c_str(),strPW.c_str(),strUserNum.c_str(),strPurposeSeg.c_str());
				bGuess = true;
			}
		}
	}else{
		int iRetLen = strCode.length();
		if(iRetLen < (SSID_MIN_LEN + TOKEN_LEN + PURPOSE_LEN /*+ MIN_PW_LEN +2*/)){
			LOGE("iRetLen:[%d] < min len\n",iRetLen, (SSID_MIN_LEN + TOKEN_LEN + PURPOSE_LEN/*+ MIN_PW_LEN +2*/));
			return;
		}

		string strSSIDLen = strCode.substr(0, SSID_MIN_LEN);
		for(int idx = 0;idx < SSID_MIN_LEN;idx++){
			iSSIDLen <<= iPower;
			string strTmp = strSSIDLen.substr(idx, 1);
			int iVal = SoundPair_Config::findIdxFromCodeTable(strTmp.c_str());
			iSSIDLen += (unsigned char) iVal;
		}

		iSSIDLen*=2;

		LOGI("iSSIDLen:[%d]\n",iSSIDLen);

		if(iSSIDLen > SSID_MAX_LEN){
			LOGE("iSSIDLen:[%d] > SSID_MAX_LEN:[%d] \n",iSSIDLen, SSID_MAX_LEN);
			if(iSSIDLen > 2*SSID_MAX_LEN){
				LOGE("iSSIDLen:[%d] > 2*SSID_MAX_LEN:[%d], return \n",iSSIDLen, 2*SSID_MAX_LEN);
				return;
			}else{
				iSSIDLen -=SSID_MAX_LEN;
				if((SSID_HASH_LEN+SSID_MIN_LEN) >= iRetLen){
					LOGE("(SSID_HASH_LEN+SSID_MIN_LEN) >= iRetLen:[%d], return \n",(SSID_HASH_LEN+SSID_MIN_LEN), iRetLen);
					return;
				}

				strSSIDHash = strCode.substr(SSID_MIN_LEN, SSID_HASH_LEN);
			}
		}else if((iSSIDLen+SSID_MIN_LEN) >= iRetLen){
			LOGE("(iSSIDLen+SSID_MIN_LEN) >= iRetLen:[%d], return \n",(iSSIDLen+SSID_MIN_LEN), iRetLen);
			return;
		}

		if(0 == strSSIDHash.length()){
			strSSID = strCode.substr(SSID_MIN_LEN, iSSIDLen);
			strPW = strCode.substr((SSID_MIN_LEN+iSSIDLen), ( iRetLen - ((SSID_MIN_LEN+iSSIDLen)+TOKEN_LEN+PURPOSE_LEN)));
		}else{
			strPW = strCode.substr((SSID_MIN_LEN+SSID_HASH_LEN), ( iRetLen - ((SSID_MIN_LEN+SSID_HASH_LEN)+TOKEN_LEN+PURPOSE_LEN)));
		}

		strUserNum = strCode.substr(iRetLen - (TOKEN_LEN+PURPOSE_LEN), TOKEN_LEN);
		strPurposeSeg = strCode.substr(iRetLen - (PURPOSE_LEN));

		LOGE("[%s, %s, %s, %s, %s]\n",strSSID.c_str(), strSSIDHash.c_str(), strPW.c_str(),strUserNum.c_str(),strPurposeSeg.c_str());
	}

	if((0 < strSSID.length() || 0 < strSSIDHash.length()) && 0 < strUserNum.length() && 0 < strPurposeSeg.length()){
		LOGE("strSSIDHash.length():[%d]\n",strSSIDHash.length());
		string strSSIDFinal, strAPIChk;
		if(0 == strSSIDHash.length()){
			int iLenSSID = strSSID.length()/iMultiply;
			for(int i =0;i < iLenSSID;i++){
				unsigned char c = 0;
				for(int j = 0;j < iMultiply;j++){
					c <<= iPower;
					string strTmp = strSSID.substr(i*iMultiply+j, 1);
					int iVal = SoundPair_Config::findIdxFromCodeTable(strTmp.c_str());
					c += (unsigned char) iVal;
					//LOGI("iVal:[%d]\n",iVal);
				}
				//LOGI("c:[%u]\n",c);
				retSSID << c;
			}

			LOGE("retSSID:[%s]\n",retSSID.str().c_str());

			strAPIChk = strSSIDFinal = retSSID.str();//UTF8_To_string(retSSID.str());

			LOGE("strSSIDFinal:[%s]\n",strSSIDFinal.c_str());
		}else{
			for(int i =0;i < SSID_HASH_LEN;i++){
				lSSIDHash <<= iPower;
				string strTmp = strSSIDHash.substr(i, 1);
				int iVal = SoundPair_Config::findIdxFromCodeTable(strTmp.c_str());
				lSSIDHash |= iVal;
				LOGE("strTmp:[%s], iVal:[%d], lSSIDHash:[%llu]\n",strTmp.c_str(), iVal, lSSIDHash);
			}

			strAPIChk = ultostr(lSSIDHash);
			LOGE("lSSIDHash:[%llu], strAPIChk:[%s]\n",lSSIDHash, strAPIChk.c_str());
		}

		int iLenPW = strPW.length()/iMultiply;
		for(int i =0;i < iLenPW;i++){
			unsigned char c = 0;
			for(int j = 0;j < iMultiply;j++){
				c <<= iPower;
				string strTmp = strPW.substr(i*iMultiply+j, 1);
				int iVal = SoundPair_Config::findIdxFromCodeTable(strTmp.c_str());
				c += (unsigned char) iVal;
				//LOGI("iVal:[%d]\n",iVal);
			}
			//LOGI("c:[%u]\n",c);
			retPW << c;
		}

		LOGE("retPW:[%s]\n",retPW.str().c_str());

		int iLenPurpose = strPurposeSeg.length();///iMultiply;
		cPurpose =SoundPair_Config::findIdxFromCodeTable(strPurposeSeg.substr(0, 1).c_str());
		cSecType =SoundPair_Config::findIdxFromCodeTable(strPurposeSeg.substr(1, 1).c_str());
		for(int i =2;i < iLenPurpose;i++){
			iReserved << iPower;
			int iVal = SoundPair_Config::findIdxFromCodeTable(strPurposeSeg.substr(i, 1).c_str());
			iReserved+=iVal;
//			for(int j = 0;j < iMultiply;j++){
//				string strTmp = strPurposeSeg.substr(i*iMultiply+j, 1);
//				int iVal = SoundPair_Config::findIdxFromCodeTable(strTmp.c_str());
//				cPurpose += (unsigned char) iVal;
//				//LOGI("iVal:[%d]\n",iVal);
//			}
		}

		LOGE("cPurpose:%u, cSecType:%u, iReserved:%d, strPurposeSeg:[%s]\n", cPurpose, cSecType, iReserved, strPurposeSeg.c_str());

		char cmd[BUF_SIZE]={0};
//		sprintf(cmd, "/beseye/cam_main/cam-handler -setwifi %s %s", strMAC.c_str(), retPW.str().c_str());
//		LOGE("wifi set cmd:[%s]\n", cmd);
		int iRet = 0;

		if(0 == strSSIDHash.length()){
			iRet = setWifiBySSID((const char*)strSSIDFinal.c_str(), (const char*)retPW.str().c_str(), cSecType);//setWifi((const char*)strMAC.c_str(), (const char*)retPW.str().c_str());//system(cmd) >> 8;
		}else{
			iRet = setWifiBySSIDHash(lSSIDHash, iSSIDLen/2, (const char*)retPW.str().c_str(), cSecType);//setWifi((const char*)strMAC.c_str(), (const char*)retPW.str().c_str());//system(cmd) >> 8;
		}
		//
		if(0 == iRet){
			stopReceiveAudioBuf();
			LOGE("wifi set OK\n");
			//long lCheckTime = time_ms();
			//long lDelta;
			int iNetworkRet = 0;
			int iTrials = 0;
			msec_t lTimeToChkNetwork = time_ms();
			msec_t lTimeDelta = 0;

			LOGE("wifi connection check begin.............\n");
			do{
				if(0 < iTrials){
					//LOGE("wifi connection check , trial %d failed, ret:%d, sleep.............\n", iTrials, iNetworkRet);
					sleep(1);
				}
				++iTrials;
				LOGE("wifi connection check , trial %d.............\n", iTrials);
				//iNetworkRet = checkInternetStatus(NETWORK_CHECK_HOST);

				iNetworkRet = invokeSystem("/beseye/util/curl --connect-timeout 5 --max-time 5 www.beseye.com") >> 8;

				if(iNetworkRet != 0)
					iNetworkRet = invokeSystem("/beseye/util/curl --connect-timeout 5 --max-time 5 www.alibaba.com.cn") >> 8;

				lTimeDelta = time_ms() - lTimeToChkNetwork;
				LOGE("wifi connection check, trial: %d ,iNetworkRet:%d, lTimeDelta:%lld\n", iTrials, iNetworkRet, lTimeDelta);
			}while((iTrials < 15 && lTimeDelta < 40000L) && 0 != iNetworkRet);

				//LOGE("wifi check ret:%d, iTrials:%ld\n", iNetworkRet, iTrials));
			//}while( (15 > iTrials) && (iNetworkRet != 0));

			LOGE("network checking complete, iNetworkRet:%d, iTrials:%ld\n", iNetworkRet, iTrials);

			if(0 == iNetworkRet){
				LOGE("network connected\n");
				iRet = invokeSystem("/beseye/cam_main/beseye_token_check") >> 8;
				//iRet = checkTokenValid();
				if(0 == iRet){
					LOGE("Token is already existed, check tmp token\n");
					if(1 == cPurpose){
						sprintf(cmd, "/beseye/cam_main/cam-util -verToken \"%s\" %s", strAPIChk.c_str(), strUserNum.c_str());
						LOGE("verToken cmd:[%s]\n", cmd);
						iRet = invokeSystem(cmd) >> 8;
						//iRet = verifyUserToken(strMAC.c_str(), strUserNum.c_str());
						if(0 == iRet){
							LOGE("Tmp User Token verification OK\n");
							AudioTest::getInstance()->setPairingReturnCode(0);
						}else{
							LOGE("Tmp User Token verification failed\n");
							//roll back wifi settings
							iRet = restoreWifi();//system("/beseye/cam_main/cam-handler -restoreWifi") >> 8;
						}
					}else{
						LOGE("Wrong cPurpose\n");
						//roll back wifi settings
						iRet = restoreWifi();//system("/beseye/cam_main/cam-handler -restoreWifi") >> 8;
					}

				}else{
					if(0 == cPurpose){
						LOGE("Token is invalid, try to attach\n");

						sprintf(cmd, "/beseye/cam_main/cam-util -attach \"%s\" %s", strAPIChk.c_str(), strUserNum.c_str());
						LOGE("attach cmd:[%s]\n", cmd);
						iRet = invokeSystem(cmd) >> 8;

						//iRet = attachCam(strMAC.c_str(), strUserNum.c_str());
						if(0 == iRet){
							LOGE("Cam attach OK\n");
							invokeSystem("/beseye/cam_main/beseye_token_check");
							AudioTest::getInstance()->setPairingReturnCode(0);
						}else{
							LOGE("Cam attach failed\n");
							iRet = restoreWifi();
						}
					}else{
						LOGE("Wrong cPurpose for attach\n");
						iRet = restoreWifi();
					}
				}
			}else{
				LOGE("network disconnected\n");
				iRet = restoreWifi();
			}
		}else{
			LOGE("wifi set failed, iRet = &d\n", CMD_RET_CODE_MAC_NOT_FOUND);
			if(CMD_RET_CODE_MAC_NOT_FOUND == iRet){
				sPairingErrType = PAIRING_ERR_MAC_NOT_FOUND;
			}
		}
	}
#endif
}

void AudioTest::onSetResult(string strCode, string strDecodeMark, string strDecodeUnmark, bool bFromAutoCorrection, MatchRetSet* prevMatchRet){
	LOGI("onSetResult(), strCode:%s, strDecodeMark = %s\n", strCode.c_str(), strDecodeMark.c_str());
#ifdef CAM_ENV
	if(mbPairingAnalysisMode && 0 < strCode.length()){
		checkPairingResult(strCode, strDecodeMark);
		if(0 <= miPairingReturnCode){
			LOGE("miPairingReturnCode:[%d], close sp\n",miPairingReturnCode);
			changePairingMode(PAIRING_DONE);
			saveToFile("/beseye/config/sp_enabled", "");
			setLedLight(0,1,0);
			stopAutoTest();
			return;
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
	LOGE("onTimeout(), bFromAutoCorrection:%d\n", bFromAutoCorrection);
	FreqAnalyzer* freqAnalyzer = (FreqAnalyzer*)freqAnalyzerRef;
	stringstream strLog;
	/*if(NULL == getDecodeRet())*/{
		if(false == freqAnalyzer->checkEndPoint()){
			string strDecodeUnmark = SoundPair_Config::decodeConsecutiveDigits(tmpRet.str());
#ifdef CAM_ENV
			if(mbPairingAnalysisMode && 0 < strDecodeUnmark.length()){
				checkPairingResult("error", strDecodeUnmark);
				if(0 <= miPairingReturnCode){
					LOGE("miPairingReturnCode:[%d], close sp\n",miPairingReturnCode);
					changePairingMode(PAIRING_DONE);
					setLedLight(0,1,0);
					stopAutoTest();
					return;
				}
			}
#endif
			if(0 == strDecodeUnmark.find(curCode)){
				if(0 == strDecodeUnmark.find(curCode+curECCode)){
					strLog << "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 7 ===>>> detection timeout but msg+errCode matched, bFromAutoCorrection:"<<bFromAutoCorrection<<"\n" <<
							"curCode          = ["<<curCode<<"], \n" <<
							"curECCode        = ["<<curECCode<<"], \n" <<
							"curEncodeMark    = ["<<curEncodeMark<<"], \n" <<
							"tmpRet           = ["<<tmpRet.str()<<"]\n" <<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"]";
					LOGE("%s\n", strLog.str().c_str());
					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.str(), DESC_TIMEOUT_MSG_EC, bFromAutoCorrection);
						}
						if(0 > miPairingReturnCode){
							if(getDetectStartFlag()){
								changePairingMode(PAIRING_ERROR);
							}else{
								changePairingMode(PAIRING_INIT);
							}
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
							"tmpRet           = ["<<tmpRet.str()<<"]\n" <<
							"strDecodeUnmark  = ["<<strDecodeUnmark<<"]";
					LOGE("%s\n", strLog.str().c_str());

					if(bFromAutoCorrection){
						if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
							adaptPrevMatchRet(prevMatchRet);
						}else{
							Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.str(), DESC_TIMEOUT_MSG, bFromAutoCorrection);
						}

						if(0 > miPairingReturnCode){
							if(getDetectStartFlag()){
								changePairingMode(PAIRING_ERROR);
							}else{
								changePairingMode(PAIRING_INIT);
							}
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
						"tmpRet           = ["<<tmpRet.str()<<"]\n" <<
						"strDecodeUnmark  = ["<<strDecodeUnmark<<"]";
				LOGE("%s\n", strLog.str().c_str());

				if(bFromAutoCorrection){
					if(NULL != prevMatchRet && prevMatchRet->prevMatchRetType <= DESC_MATCH_MSG){
						adaptPrevMatchRet(prevMatchRet);
					}else{
						Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.str(), DESC_TIMEOUT, bFromAutoCorrection);
					}

					if(0 > miPairingReturnCode){
						if(getDetectStartFlag()){
							changePairingMode(PAIRING_ERROR);
						}else{
							changePairingMode(PAIRING_INIT);
						}
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
			if(bFromAutoCorrection){
				deinitTestRound();
				if(0 > miPairingReturnCode){
					if(getDetectStartFlag()){
						changePairingMode(PAIRING_ERROR);
					}else{
						changePairingMode(PAIRING_INIT);
					}
				}
			}
			return;
		}
	}
	deinitTestRound();
	LOGE("onTimeout()--, bFromAutoCorrection:%d\n", bFromAutoCorrection);
}

float AudioTest::onBufCheck(ArrayRef<short> buf, msec_t lBufTs, bool bResetFFT, int* iFFTValues){
	//FreqAnalyzer::getInstance()->analyzeAudioViaAudacityAC(buf, SoundPair_Config::FRAME_SIZE_REC, bResetFFT, 0, NULL);
	return FreqAnalyzer::getInstance()->analyzeAudioViaAudacityAC(buf, SoundPair_Config::FRAME_SIZE_REC, bResetFFT, FreqAnalyzer::getInstance()->getLastDetectedToneIdx(lBufTs), iFFTValues);
}

void AudioTest::adaptPrevMatchRet(MatchRetSet* prevMatchRet){
	LOGI("adaptPrevMatchRet(), previous result is better,\n prevMatchRet = %s", prevMatchRet?prevMatchRet->toString().c_str():"");
	Delegate_FeedbackMatchResult(curCode, curECCode, curEncodeMark, prevMatchRet->strCode, prevMatchRet->strDecodeUnmark, prevMatchRet->strDecodeMark, prevMatchRet->prevMatchRetType, false);
}

void AudioTest::decodeRSCode(int* data, int iCount, int iNumErr){

}

void AudioTest::resetBuffer(){
	mbNeedToResetFFT = true;
	AudioBufferMgr::getInstance()->recycleAllBuffer();
	//shortsRecBuf = NULL;
	lTsRec = 0;
	//deinitTestRound();
}

void AudioTest::deinitTestRound(){
	LOGE("deinitTestRound()++\n");
	tmpRet.str("");
	tmpRet.clear();
	FreqAnalyzer::getInstance()->endToTrace();
	FreqAnalyzer::getInstance()->reset();
	setStopAnalysisBufIdx(-1);
	setDetectStartFlag(false);
	resetBuffer();

	acquireThresholdCtrlObj();
	mbAboveThreshold=false;
	pthread_cond_broadcast(&mThresholdCtrlObjCond);
	releaseThresholdCtrlObj();

	acquireAutoTestCtrlObj();
	pthread_cond_broadcast(&mAutoTestCtrlObjCond);
	releaseAutoTestCtrlObj();

	acquireSyncObj();
	pthread_cond_broadcast(&mSyncObjCond);
	releaseSyncObj();
	LOGE("deinitTestRound()--\n");
}


string AudioTest::findDifference(string strSrc, string strDecode){
	stringstream strRet(strDecode);
	unsigned int iLenSrc = strSrc.length();

	for(unsigned int i =0; i < iLenSrc; i++){
		if(i >= strDecode.length())
			break;

		if(0 != strSrc.substr(i, 1).compare(strDecode.substr(i, 1))){
			strRet.str().replace(i, 1, "#");
			//break;
		}
	}
	return strRet.str();
}

void AudioTest::acquireSyncObj(){
	miSyncObjInvokeCount++;
	if(1 < miSyncObjInvokeCount)
		LOGE("Error, miSyncObjInvokeCount:%d\n",miSyncObjInvokeCount);
	pthread_mutex_lock(&mSyncObj);
}

void AudioTest::releaseSyncObj(){
	pthread_mutex_unlock(&mSyncObj);
	miSyncObjInvokeCount--;
	//LOGI("miSyncObjInvokeCount:%d\n",miSyncObjInvokeCount);
}

void AudioTest::acquireSendPairingCodeObj(){
	miSendPairingCodeObjInvokeCount++;
	if(1 < miSendPairingCodeObjInvokeCount)
		LOGE("Error, miSendPairingCodeObjInvokeCount:%d\n",miSendPairingCodeObjInvokeCount);
	pthread_mutex_lock(&mSendPairingCodeObj);
}

void AudioTest::releaseSendPairingCodeObj(){
	pthread_mutex_unlock(&mSendPairingCodeObj);
	miSendPairingCodeObjInvokeCount--;
	//LOGI("miSendPairingCodeObjInvokeCount:%d\n",miSendPairingCodeObjInvokeCount);
}

void AudioTest::acquireAutoTestCtrlObj(){
	miAutoTestCtrlObjInvokeCount++;
	if(1 < miAutoTestCtrlObjInvokeCount)
		LOGE("Error, miAutoTestCtrlObjInvokeCount:%d\n",miAutoTestCtrlObjInvokeCount);
	pthread_mutex_lock(&mAutoTestCtrlObj);
}

void AudioTest::releaseAutoTestCtrlObj(){
	pthread_mutex_unlock(&mAutoTestCtrlObj);
	miAutoTestCtrlObjInvokeCount--;
	//LOGI("miAutoTestCtrlObjInvokeCount:%d\n",miAutoTestCtrlObjInvokeCount);
}

void AudioTest::acquireThresholdCtrlObj(){
	miThresholdCtrlObjInvokeCount++;
	if(1 < miThresholdCtrlObjInvokeCount)
		LOGE("Error, miThresholdCtrlObjInvokeCount:%d\n",miThresholdCtrlObjInvokeCount);
	pthread_mutex_lock(&mThresholdCtrlObj);
}

void AudioTest::releaseThresholdCtrlObj(){
	pthread_mutex_unlock(&mThresholdCtrlObj);
	miThresholdCtrlObjInvokeCount--;
	//LOGI("miThresholdCtrlObjInvokeCount:%d\n",miThresholdCtrlObjInvokeCount);
}

#ifdef ANDROID
void AudioTest::setCamCamWSServerInfo(string strHost, int iPort){
	mstrCamWSServerIP = strHost;
	miCamWSServerPort = iPort;
}

int AudioTest::connectCamCamWSServer(){
#ifdef AUTO_TEST
	return init_websocket_client(mstrCamWSServerIP.c_str(), miCamWSServerPort, soundpairSenderCb);
#else
	return false;
#endif
}

int AudioTest::disconnectCamCamWSServer(){
#ifdef AUTO_TEST
	return deinit_websocket_client();
#else
	return false;
#endif
}

bool AudioTest::isCamCamWSServerConnected(){
#ifdef AUTO_TEST
	return is_websocket_client_inited();
#else
	return false;
#endif
}
#endif
