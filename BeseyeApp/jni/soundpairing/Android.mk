LOCAL_PATH := $(call my-dir)

#include $(CLEAR_VARS)
# 
#
# FMOD Ex Shared Library
# 
#include $(CLEAR_VARS) 
#
#LOCAL_MODULE            := aubio
#LOCAL_SRC_FILES         := libaubio.so
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

#include $(PREBUILT_SHARED_LIBRARY)
  
include $(CLEAR_VARS)

LOCAL_PATH := ./

SOUNDPAIRING_PATH 	   := $(TOP_LOCAL_PATH)/soundpairing
AUBIO_SRC_ROOT  	   := $(SOUNDPAIRING_PATH)/src

#AUBIO_SRC_ROOT 		   := src

LOCAL_MODULE           := soundpairing

LOCAL_CPPFLAGS 		   += -fexceptions

LOCAL_SRC_FILES        := $(AUBIO_SRC_ROOT)/FFT.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/Exception.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/IllegalArgumentException.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/GenericGF.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/GenericGFPoly.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/ReedSolomonEncoder.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/ReedSolomonDecoder.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/ReedSolomonException.cpp \
						  $(AUBIO_SRC_ROOT)/utils.cpp \
						  $(AUBIO_SRC_ROOT)/sp_config.cpp \
						  $(AUBIO_SRC_ROOT)/AudioBufferMgr.cpp \
						  $(AUBIO_SRC_ROOT)/FreqAnalyzer.cpp \
						  $(AUBIO_SRC_ROOT)/FreqGenerator.cpp \
						  $(AUBIO_SRC_ROOT)/AudioTest.cpp \
						  $(AUBIO_SRC_ROOT)/http_cgi.cpp \
						  $(AUBIO_SRC_ROOT)/native.cpp \

LOCAL_C_INCLUDES 	   := $(SOUNDPAIRING_PATH)\
						  $(SOUNDPAIRING_PATH)/include \
						  $(SOUNDPAIRING_PATH)/include/speex \
						  $(SOUNDPAIRING_PATH)/include/curl \
						  $(SOUNDPAIRING_PATH)/include/json-c \
						  $(SOUNDPAIRING_PATH)/zxing \
						  $(SOUNDPAIRING_PATH)/zxing/common \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon
						  
#LOCAL_STATIC_LIBRARIES := audacityFFT

LOCAL_LDLIBS           := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib \
						  -L$(SOUNDPAIRING_PATH) \
						  -lspeexdsp \
						  -lm \
						  -lcurl \
						  -ljson-c \
					      -ljson \
					      -llog \
					      -lz \
					      -ldl \
					      -lgcc 
#LOCAL_SHARED_LIBRARIES := aubio
 
include $(BUILD_SHARED_LIBRARY) 
  