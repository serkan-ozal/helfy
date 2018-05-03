#include <jvmti.h>
#include <iostream>
#include "helfy.h"

extern "C" JNIEXPORT jobject JNICALL
Java_one_helfy_HelfyNative_getObject(JNIEnv* env, jclass clazz, jlong addressLocation) {
    return (jobject) addressLocation;
}
