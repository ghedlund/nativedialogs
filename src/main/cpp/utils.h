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
