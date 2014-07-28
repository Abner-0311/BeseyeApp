TOP_LOCAL_PATH := $(call my-dir)

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

#Build Sound Pairing module  
LOCAL_PATH := ./
  
include $(CLEAR_VARS)

AUBIO_SRC_ROOT 		   := src

LOCAL_MODULE           := soundpairing

LOCAL_CPPFLAGS 		   += -fexceptions
LOCAL_CFLAGS    := -DLWS_BUILTIN_GETIFADDRS -DCMAKE_BUILD
LWS_LIB_PATH	:= $(TOP_LOCAL_PATH)/libwebsockets/lib


LOCAL_SRC_FILES        := $(TOP_LOCAL_PATH)/common/src/utils.cpp \
						  $(TOP_LOCAL_PATH)/common/src/json_utils.cpp \
						  $(TOP_LOCAL_PATH)/src/preprocess/kiss_fft.c \
						  $(TOP_LOCAL_PATH)/src/preprocess/kiss_fftr.c \
						  $(TOP_LOCAL_PATH)/src/preprocess/mdf.c \
						  $(TOP_LOCAL_PATH)/src/preprocess/smallft.c \
						  $(TOP_LOCAL_PATH)/src/preprocess/fftwrap.c \
						  $(TOP_LOCAL_PATH)/src/preprocess/filterbank.c \
						  $(TOP_LOCAL_PATH)/src/preprocess/preprocess.c \
						  $(TOP_LOCAL_PATH)/http_cgi_lib/src/http_cgi.c \
						  $(TOP_LOCAL_PATH)/cam-handler/src/network/event.c \
						  $(TOP_LOCAL_PATH)/cam-handler/src/network/network.c \
						  $(TOP_LOCAL_PATH)/cam-handler/src/network/network_observer.cpp \
						  $(TOP_LOCAL_PATH)/cam-handler/src/main.c \
						  $(TOP_LOCAL_PATH)/cam-handler/src/delegate/cam_controller.cpp \
						  $(TOP_LOCAL_PATH)/cam-handler/src/delegate/led_controller.cpp \
						  $(TOP_LOCAL_PATH)/cam-handler/src/event_queue/event_queue_handler.cpp \
						  $(TOP_LOCAL_PATH)/cam-handler/src/event_queue/event_queue.cpp \
						  $(TOP_LOCAL_PATH)/cam-handler/src/cam_event_mgr.c \
						  $(TOP_LOCAL_PATH)/src/FFT.cpp \
						  $(TOP_LOCAL_PATH)/zxing/Exception.cpp \
						  $(TOP_LOCAL_PATH)/zxing/common/IllegalArgumentException.cpp \
						  $(TOP_LOCAL_PATH)/zxing/common/reedsolomon/GenericGF.cpp \
						  $(TOP_LOCAL_PATH)/zxing/common/reedsolomon/GenericGFPoly.cpp \
						  $(TOP_LOCAL_PATH)/zxing/common/reedsolomon/ReedSolomonEncoder.cpp \
						  $(TOP_LOCAL_PATH)/zxing/common/reedsolomon/ReedSolomonDecoder.cpp \
						  $(TOP_LOCAL_PATH)/zxing/common/reedsolomon/ReedSolomonException.cpp \
						  $(TOP_LOCAL_PATH)/src/sp_config.cpp \
						  $(TOP_LOCAL_PATH)/src/AudioBufferMgr.cpp \
						  $(TOP_LOCAL_PATH)/src/FreqAnalyzer.cpp \
						  $(TOP_LOCAL_PATH)/src/FreqGenerator.cpp \
						  $(TOP_LOCAL_PATH)/src/AudioTest.cpp \
						  $(TOP_LOCAL_PATH)/src/native.cpp \
						  $(LWS_LIB_PATH)/base64-decode.c \
							$(LWS_LIB_PATH)/client.c \
							$(LWS_LIB_PATH)/client-handshake.c \
							$(LWS_LIB_PATH)/client-parser.c \
							$(LWS_LIB_PATH)/daemonize.c \
							$(LWS_LIB_PATH)/extension.c \
							$(LWS_LIB_PATH)/extension-deflate-frame.c \
							$(LWS_LIB_PATH)/extension-deflate-stream.c \
							$(LWS_LIB_PATH)/getifaddrs.c \
							$(LWS_LIB_PATH)/handshake.c \
							$(LWS_LIB_PATH)/server.c \
							$(LWS_LIB_PATH)/server-handshake.c \
							$(LWS_LIB_PATH)/libwebsockets.c \
							$(LWS_LIB_PATH)/output.c \
							$(LWS_LIB_PATH)/parsers.c \
							$(LWS_LIB_PATH)/sha-1.c \
							$(TOP_LOCAL_PATH)/libwebsockets/src/simple_websocket_mgr.c 

LOCAL_C_INCLUDES 	   := $(TOP_LOCAL_PATH)\
						  $(TOP_LOCAL_PATH)/src \
						  $(TOP_LOCAL_PATH)/src/preprocess \
						  $(TOP_LOCAL_PATH)/include \
						  $(TOP_LOCAL_PATH)/include/speex \
						  $(TOP_LOCAL_PATH)/include/curl \
						  $(TOP_LOCAL_PATH)/include/json-c \
						  $(TOP_LOCAL_PATH)/zxing \
						  $(TOP_LOCAL_PATH)/zxing/common \
						  $(TOP_LOCAL_PATH)/zxing/common/reedsolomon \
						  $(TOP_LOCAL_PATH)/common/inc \
						  $(TOP_LOCAL_PATH)/http_cgi_lib/inc \
						  $(TOP_LOCAL_PATH)/cam-handler/inc \
						  $(TOP_LOCAL_PATH)/ws-client/inc \
						  $(TOP_LOCAL_PATH)/cam-handler/inc/network \
						  $(TOP_LOCAL_PATH)/libwebsockets/inc \
						  $(LWS_LIB_PATH)/
						  
#LOCAL_STATIC_LIBRARIES := audacityFFT

LOCAL_LDLIBS           := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib \
						  -L$(TOP_LOCAL_PATH) \
						  -lm \
						  -lcurl \
						  -ljson-c \
					      -ljson \
					      -lssl \
						  -lcrypto \
					      -lfftw3f \
					      -llog \
					      -lz \
					      -ldl \
					      -lgcc 
#LOCAL_SHARED_LIBRARIES := aubio
 
include $(BUILD_SHARED_LIBRARY) 
