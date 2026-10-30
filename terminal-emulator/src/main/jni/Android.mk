LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= liblift
LOCAL_SRC_FILES:= lift.c
include $(BUILD_SHARED_LIBRARY)
