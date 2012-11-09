#include <string.h>

#include "com_example_retroshare_remote_bitdht.h"

#include "example/bssdht.h"
#include <iostream>

jstring Java_com_example_retroshare_remote_bitdht_getIp
(JNIEnv* env, jclass cl, jstring s){
	std::cerr << "Java_com_example_retroshare_remote_bitdht_getIp()" << std::endl;
	//const char* temp = env->GetStringUTFChars(dir, NULL);
	//std::string stringDir(temp);

	jboolean b;
    const char* str = env->GetStringUTFChars(s, &b);
    std::string stdstr(str);
    env->ReleaseStringUTFChars(s, str);
	dowork(stdstr);

	std::cerr << "Java_com_example_retroshare_remote_bitdht_getIp() returning ..." << std::endl;

    return env->NewStringUTF("Hello from JNI !");
}

/*
jstring
Java_com_example_hellojni_HelloJni_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
    return (*env)->NewStringUTF(env, "Hello from JNI !");
}
*/
