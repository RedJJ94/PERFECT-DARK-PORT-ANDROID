#ifndef ANDROID_SYSTEM_H
#define ANDROID_SYSTEM_H

#ifdef ANDROID

#include <jni.h>
#include <android/log.h>

// Android-specific system functions
const char* sysGetDataPath(void);
bool sysIsAndroid(void);

// Virtual touch-controller state (defined in input.c, written by JNI in main.c)
#include <PR/ultratypes.h>
extern volatile u32 g_androidButtons;
extern volatile s8  g_androidStickX;
extern volatile s8  g_androidStickY;
extern volatile s8  g_androidCamX;
extern volatile s8  g_androidCamY;

// Input functions for Android
void inputGetStickInput(int stick, float* x, float* y);
bool inputGetButtonState(int button);

// JNI function declarations
JNIEXPORT void JNICALL Java_com_perfectdark_port_MainActivity_nativeInit(JNIEnv* env, jobject thiz, jstring dataPath);
JNIEXPORT void JNICALL Java_com_perfectdark_port_MainActivity_nativeDestroy(JNIEnv* env, jobject thiz);
JNIEXPORT void JNICALL Java_com_perfectdark_port_GameView_nativeSurfaceCreated(JNIEnv* env, jobject thiz);
JNIEXPORT void JNICALL Java_com_perfectdark_port_GameView_nativeSurfaceChanged(JNIEnv* env, jobject thiz, jint width, jint height);
JNIEXPORT void JNICALL Java_com_perfectdark_port_GameView_nativeDrawFrame(JNIEnv* env, jobject thiz);
JNIEXPORT void JNICALL Java_com_perfectdark_port_GameView_nativeKeyDown(JNIEnv* env, jobject thiz, jint keyCode);
JNIEXPORT void JNICALL Java_com_perfectdark_port_GameView_nativeKeyUp(JNIEnv* env, jobject thiz, jint keyCode);
JNIEXPORT void JNICALL Java_com_perfectdark_port_TouchControls_nativeStickInput(JNIEnv* env, jobject thiz, jint stick, jfloat x, jfloat y);
JNIEXPORT void JNICALL Java_com_perfectdark_port_TouchControls_nativeButtonDown(JNIEnv* env, jobject thiz, jint button);
JNIEXPORT void JNICALL Java_com_perfectdark_port_TouchControls_nativeButtonUp(JNIEnv* env, jobject thiz, jint button);

// Settings application function
JNIEXPORT void JNICALL Java_com_perfectdark_port_SettingsActivity_nativeApplySettings(
    JNIEnv* env, jobject thiz,
    jboolean fullscreen, jboolean vsync, jboolean displayFPS, jboolean detailTextures, jboolean texFilter2D,
    jint framerateLimit, jfloat screenShake,
    jboolean disableMpDeathMusic,
    jboolean uncapTickrate, jboolean geMuzzleFlashes, jint fieldOfView, jint hudCenter,
    jboolean showControls, jfloat leftStickSensitivity, jfloat rightStickSensitivity,
    jfloat leftStickDeadzone, jfloat rightStickDeadzone, jboolean vibration, jfloat vibrationStrength,
    jboolean texDumpEnabled, jboolean texLoadEnabled
);

#endif // ANDROID

#endif // ANDROID_SYSTEM_H