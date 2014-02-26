TOP_LOCAL_PATH := $(call my-dir)

#include $(TOP_LOCAL_PATH)/../../SDL2-2.0.1/Android.mk
include $(TOP_LOCAL_PATH)/main/Android.mk
#include $(call all-subdir-makefiles)
