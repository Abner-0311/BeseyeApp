// standard stuff

#include "rt_config.h"

static int siDebugMode = 1;

void setSPDebugMode(int iDebug){//0:off, 1:on
	siDebugMode = iDebug;
	//LOGI("setSPDebugMode(), siDebugMode:(%d)", siDebugMode);
}

int isSPDebugMode(){
	return siDebugMode;
}
