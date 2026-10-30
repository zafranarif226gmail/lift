LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := liblift-bootstrap
LOCAL_SRC_FILES := lift-bootstrap-zip.S lift-bootstrap.c
include $(BUILD_SHARED_LIBRARY)
