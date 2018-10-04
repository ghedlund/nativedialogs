/*
 * Copyright (C) 2012-2018 Gregory Hedlund
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef UTILS_H
#define UTILS_H

#include <jni.h>

#define RESULT_OK 0x01
#define RESULT_CANCEL 0x02
#define ANSWER_YES 0x01
#define ANSWER_NO 0x03
#define RESULT_UNKNOWN 0x04

/**
* Create a new dialog result
*/
jobject CreateDialogResult(JNIEnv *env, int result, jobject data);

/**
 * Fire dialog result to listener
 */
void SendDialogResult(JNIEnv *env, jobject listener, jobject result);

/**
 * Get named property from java property map
 */
jobject GetProperty(JNIEnv* env, jobject props, jstring propName);

bool GetBool(JNIEnv *env, jobject obj);
jobject ToBool(JNIEnv* env, bool val);

#endif
