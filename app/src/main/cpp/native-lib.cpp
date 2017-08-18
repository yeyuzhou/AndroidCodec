#include <jni.h>
#include <string>


jobject g_object;

extern "C"
{


    /*
     * Class:     com_beryl_jni_CodecJniHelp
     * Method:    Init
     * Signature: (Ljava/lang/Object;)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_beryl_jni_CodecJniHelp_Init
            (JNIEnv *env, jclass cls , jobject obj)
    {
        if(NULL != obj )
        {
            g_object = env->NewGlobalRef(obj);

        }

        return true;

    }


    /*
     * Class:     com_beryl_jni_CodecJniHelp
     * Method:    setEncodeData
     * Signature: ([BII)V
     */
    JNIEXPORT void JNICALL Java_com_beryl_jni_CodecJniHelp_SetEncodeData
            (JNIEnv *env , jclass cls ,jbyteArray data , jint len ,jint tag) {

        jclass classtemp = env->GetObjectClass(g_object);
        if (NULL != classtemp)
        {
            jmethodID  method= env->GetMethodID(classtemp, "onDecodeDataCallback", "([BII)V");
            if(NULL != method){
                env->CallVoidMethod(g_object, method,data,len ,tag);
            }
        }
    }


    JNIEXPORT void JNICALL Java_com_beryl_jni_CodecJniHelp_changeNV21toYUV420SemiPlanar
            (JNIEnv *env, jclass cls, jbyteArray inputArray, jbyteArray outputArray, jint len, jint w,
             jint h , jboolean jFrontCamera) {

        jbyte* byData = (jbyte*)env->GetByteArrayElements(inputArray, NULL);
        if (NULL != byData)
        {

            for (int i = len *2 /3; i + 2 < len; i+=2) {
                jbyte tmp = byData[i];
                byData[i] = byData[i+1];
                byData[i+1] = tmp;
            }


        }
        env->ReleaseByteArrayElements(inputArray,byData,0);


    }

}



