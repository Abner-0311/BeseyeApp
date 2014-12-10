#ifndef __UTILS_H__
#define __UTILS_H__

#include <cmath>
#include <limits>
#ifdef __cplusplus
extern "C" {
#endif
#include "Common.h"

typedef uint64_t uint64;
typedef unsigned char byte;

const char* getCamBEUrl();

bool sameValue(double a, double b);
bool largeThan(double a, double b);
bool largeEqualThan(double a, double b);
bool lessThan(double a, double b);
bool lessEqualThan(double a, double b);

long getTickCount();

void getTimeSpecByDelay(struct timespec &spec, long lDelayInMS);

//Need to free result
char* readFromFile(const char* filePath);
int saveToFile(const char* filePath, const char* content);
int deleteFile(const char* filePath);
bool isFileExist(const char* filePath);

int saveRegionId(unsigned char cRegId);
int saveOldRegionId();
int restoreRegionId();
int removeOldRegionId();

int saveLogFile(const char* filePath);

int strCmpEndWith(const char* toCmp, const char* strCmp);

int saveOldWifi();
int setWifi(const char* macAddr, const char* pw);
int setWifiBySSID(const char* ssid, const char* pw, const char cSecType);
int setWifiBySSIDHash(uint64 ssidHash, int iLenOfSSID, const char* pw, const char cSecType);
int restoreWifi();

void deleteOldWiFiFile();

char *ultostr(uint64 ulong_value);



#include "time_utils.h"

//template<typename T, int size>
//int getArrLength(T(&)[size]){return size;}
#ifdef __cplusplus
}
#endif
#endif
