TOP_LOCAL_PATH := $(call my-dir)
  
LOCAL_PATH := $(call my-dir)
  
include $(CLEAR_VARS)

LOCAL_PATH := ./

SOUNDPAIRING_PATH 	   := $(TOP_LOCAL_PATH)/soundpairing
AUBIO_SRC_ROOT  	   := $(SOUNDPAIRING_PATH)/src

LOCAL_MODULE           := soundpairing

LOCAL_CPPFLAGS 		   += -fexceptions

LOCAL_SRC_FILES        := $(SOUNDPAIRING_PATH)/zxing/Exception.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/IllegalArgumentException.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/GenericGF.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/GenericGFPoly.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/ReedSolomonEncoder.cpp \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon/ReedSolomonException.cpp \
						  $(AUBIO_SRC_ROOT)/utils.cpp \
						  $(AUBIO_SRC_ROOT)/sp_config.cpp \
						  $(AUBIO_SRC_ROOT)/FreqGenerator.cpp \
						  $(AUBIO_SRC_ROOT)/native.cpp \

LOCAL_C_INCLUDES 	   := $(SOUNDPAIRING_PATH)\
						  $(SOUNDPAIRING_PATH)/include \
						  $(SOUNDPAIRING_PATH)/zxing \
						  $(SOUNDPAIRING_PATH)/zxing/common \
						  $(SOUNDPAIRING_PATH)/zxing/common/reedsolomon

LOCAL_LDLIBS           := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib \
						  -L$(SOUNDPAIRING_PATH) \
						  -lm \
					      -llog \
					      -lz \
					      -ldl \
					      -lgcc 
 
include $(BUILD_SHARED_LIBRARY) 
  
include $(CLEAR_VARS)

LOCAL_PATH := ./
SDL_PATH := ../SDL

STREAMING_PATH := $(TOP_LOCAL_PATH)/streaming
SRC_PATH  := $(STREAMING_PATH)/source

LOCAL_MODULE    := ffmpegutils
LOCAL_SRC_FILES := $(SRC_PATH)/ffmpeg_ext.c \
				   $(SRC_PATH)/librtmp_ext.c \
				   $(SRC_PATH)/utils.cpp \
				   $(SRC_PATH)/cmdutils.c \
				   $(SRC_PATH)/ffmpeg.c \
				   $(SRC_PATH)/ffplay.c \
				   $(SRC_PATH)/pkt_queue.c \
				   $(SRC_PATH)/beseye_rtmp_observer.cpp \
				   $(SRC_PATH)/beseyeplayer.cpp \
				   $(SRC_PATH)/beseye_audio_streamer.cpp \
				   $(SRC_PATH)/http_cgi.c \
				   $(STREAMING_PATH)/main.cpp \
 
#LOCAL_SHARE_LIBRARIES := libSDL2


LOCAL_C_INCLUDES := $(STREAMING_PATH) \
					$(STREAMING_PATH)/header \
					$(STREAMING_PATH)/include \
					$(STREAMING_PATH)/$(SDL_PATH)/include
					
LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES \
				-D__STDC_CONSTANT_MACROS=1 \
				-D__STDC_LIMIT_MACROS \
				-D__STDC_FORMAT_MACROS

LOCAL_STATIC_LIBRARIES := SDL2
					
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib \
				-L$(STREAMING_PATH) \
				-L$(STREAMING_PATH)/../../obj/local/armeabi \
				-lswresample \
				-lavformat \
				-lavcodec \
				-lavfilter \
				-lswscale \
				-lavutil \
				-lrtmp \
				-lSDL2 \
				-lssl \
				-lcrypto \
				-lcurl \
				-ljson-c \
				-ljson \
				-llog \
				-ljnigraphics \
				-lz \
				-ldl \
				-lEGL \
				-lGLESv1_CM \
				-lGLESv2 \
				-landroid \
				-lgcc

#LOCAL_SHARED_LIBRARIES := \
	libssl \
	libcrypto \

include $(BUILD_SHARED_LIBRARY)
$(call import-module,SDL2-2.0.1)LOCAL_PATH := $(call my-dir)

 
