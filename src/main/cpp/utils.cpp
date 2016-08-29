/*
 * This file is part of nativedialogs for java
 * Copyright (C) 2016 Gregory Hedlund &lt;ghedlund@mun.ca&gt;
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
