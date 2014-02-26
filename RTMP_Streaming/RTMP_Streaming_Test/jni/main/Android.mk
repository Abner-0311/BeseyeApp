LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PATH := ./
SDL_PATH := ../SDL

MAIN_PATH := $(TOP_LOCAL_PATH)/main
SRC_PATH  := $(MAIN_PATH)/source

LOCAL_MODULE    := ffmpegutils
LOCAL_SRC_FILES := $(SRC_PATH)/ffmpeg_ext.c \
				   $(SRC_PATH)/librtmp_ext.c \
				   $(SRC_PATH)/cmdutils.c \
				   $(SRC_PATH)/ffmpeg.c \
				   $(SRC_PATH)/pkt_queue.c \
				   $(SRC_PATH)/beseye_rtmp_observer.cpp \
				   $(SRC_PATH)/beseyeplayer.cpp \
				   $(SRC_PATH)/beseye_audio_streamer.cpp \
				   $(MAIN_PATH)/main.cpp \
 
#LOCAL_SHARE_LIBRARIES := libSDL2

LOCAL_C_INCLUDES := $(MAIN_PATH) \
					$(MAIN_PATH)/header \
					$(MAIN_PATH)/include \
					$(MAIN_PATH)/$(SDL_PATH)/include
					
LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES \
				-D__STDC_CONSTANT_MACROS=1 \
				-D__STDC_LIMIT_MACROS \
				-D__STDC_FORMAT_MACROS

LOCAL_STATIC_LIBRARIES := SDL2
					
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib \
				-L$(MAIN_PATH) \
				-L$(MAIN_PATH)/../../obj/local/armeabi \
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

 