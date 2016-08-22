#include <jni.h>

#include "utils.h"

jobject CreateDialogResult(JNIEnv *env, int result, jobject data) {
	const char *szClassName = "ca/phon/ui/nativedialogs/NativeDialogEvent";
	const char *szCstrSig = "(ILjava/lang/Object;)V";
	jobject retVal = NULL;

	jclass NativeDialogEvent = env->FindClass(szClassName);
	jmethodID cstr = env->GetMethodID(NativeDialogEvent, "<init>", szCstrSig);

	retVal = env->NewObject(NativeDialogEvent, cstr, result, data);

	return retVal;
}

void SendDialogResult(JNIEnv *env, jobject listener, jobject result) {
	const char *szClassName = "ca/phon/ui/nativedialogs/NativeDialogListener";
	const char *szMethodName = "nativeDialogEvent";
	const char *szMethodSig = "(Lca/phon/ui/nativedialogs/NativeDialogEvent;)V";

	jclass NativeDialogListener = env->FindClass(szClassName);
	jmethodID methodId = env->GetMethodID(NativeDialogListener, szMethodName, szMethodSig);

	env->CallVoidMethod(listener, methodId, result);
}

jobject GetProperty(JNIEnv* env, jobject props, jstring propName) {
	const char *szMethodName = "get";
	const char *szMethodSig = "(Ljava/lang/Object;)Ljava/lang/Object;";

	jclass clazz = env->GetObjectClass(props);
	jmethodID get = env->GetMethodID(clazz, szMethodName, szMethodSig);

	return env->CallObjectMethod(props, get, propName);
}

bool GetBool(JNIEnv *env, jobject obj) {
	const char *szMethodName = "booleanValue";
	const char *szMethodSig = "()Z";

	jclass clazz = env->GetObjectClass(obj);
	jmethodID methodID = env->GetMethodID(clazz, szMethodName, szMethodSig);

	return env->CallBooleanMethod(obj, methodID);
}

jobject ToBool(JNIEnv* env, bool val) {
	const char *szClassName = "java/lang/Boolean";
	const char *szCstrSig = "(Z)V";

	jclass clazz = env->FindClass(szClassName);
	jmethodID cstrID = env->GetMethodID(clazz, "<init>", szCstrSig);

	return env->NewObject(clazz, cstrID, val);
}
