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

AUBIO_SRC_ROOT 		   := src

LOCAL_MODULE           := soundpairing

LOCAL_CPPFLAGS 		   += -fexceptions

LOCAL_SRC_FILES        := http_cgi_lib/src/http_cgi.c \
						  cam-handler/src/network/event.c \
						  cam-handler/src/network/network.c \
						  cam-handler/src/main.c \
						  cam-handler/src/delegate/cam_controller.cpp \
						  cam-handler/src/event_queue/event_queue_handler.cpp \
						  cam-handler/src/event_queue/event_queue.cpp \
						  cam-handler/src/cam_event_mgr.c \
						  src/FFT.cpp \
						  zxing/Exception.cpp \
						  zxing/common/IllegalArgumentException.cpp \
						  zxing/common/reedsolomon/GenericGF.cpp \
						  zxing/common/reedsolomon/GenericGFPoly.cpp \
						  zxing/common/reedsolomon/ReedSolomonEncoder.cpp \
						  zxing/common/reedsolomon/ReedSolomonDecoder.cpp \
						  zxing/common/reedsolomon/ReedSolomonException.cpp \
						  src/utils.cpp \
						  src/sp_config.cpp \
						  src/AudioBufferMgr.cpp \
						  src/FreqAnalyzer.cpp \
						  src/FreqGenerator.cpp \
						  src/AudioTest.cpp \
						  src/native.cpp \

LOCAL_C_INCLUDES 	   := $(LOCAL_PATH)\
						  $(LOCAL_PATH)/include \
						  $(LOCAL_PATH)/include/speex \
						  $(LOCAL_PATH)/include/curl \
						  $(LOCAL_PATH)/include/json-c \
						  $(LOCAL_PATH)/zxing \
						  $(LOCAL_PATH)/zxing/common \
						  $(LOCAL_PATH)/zxing/common/reedsolomon \
						  $(LOCAL_PATH)/http_cgi_lib/inc \
						  $(LOCAL_PATH)/cam-handler/inc \
						  $(LOCAL_PATH)/cam-handler/inc/network \
						  
#LOCAL_STATIC_LIBRARIES := audacityFFT

LOCAL_LDLIBS           := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib \
						  -L$(LOCAL_PATH) \
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
  