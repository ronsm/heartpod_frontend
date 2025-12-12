#include <android/log.h>
#include <jni.h>

#define LOG_TAG "HealthHubNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL Java_org_hwu_care_healthub_NativeLib_getVersion(
    JNIEnv *env, jobject /* this */) {

  LOGI("Native library loaded");
  return env->NewStringUTF("HealthHub Native v1.0");
}

} // extern "C"
