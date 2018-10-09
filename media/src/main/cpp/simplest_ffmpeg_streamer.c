
#include <stdio.h>
#include <time.h>


#include "stream_h264.h"
#include "stream_aac.h"


#ifdef ANDROID

#include <jni.h>
#include <android/log.h>

#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "media", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "media", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif


JNIEXPORT jint JNICALL Java_cn_embed_utils_NativeStream_stream
        (JNIEnv *env, jobject obj, jbyteArray input_jstr, jstring output_jstr, jint len,
         jint type) {
    int nRet = 0;
    char output_str[500] = {0};
    sprintf(output_str, "%s", (*env)->GetStringUTFChars(env, output_jstr, NULL));
    jbyte *m_temp = (*env)->GetByteArrayElements(env, input_jstr, 0);
    char *data = (char *) m_temp;
    m_nFileBufSize = len;
    m_pFileBuf = data;
    m_nCurPos = 0;
    signal(SIGPIPE, SIG_IGN);
    signal(SIGHUP, SIG_IGN);

    if (type == NET_INIT)  //0
    {
        LOGE("-----NET_INIT--------\n");
        if (Net_Init(output_str) < 0) {
            Close();
            return NETINIT_ERROR;
        }
    }

    if (type == SEND_VIDEIO_STREAM) //1
    {
        LOGE("-----SendVideo--------\n");
        if ((nRet = SendH264Stream(data)) <= 0) {
            if (!nRet) {
                return SEND_H264_ERROR;
            }
            return nRet;
        }
    }

    if (type == SEND_AUDIO_STREAM) //2
    {
        LOGE("-----SEND_AUDIO_STREAM--------\n");
        if ((nRet = SendAacStream(data, len)) <= 0) {
            if (!nRet) {
                return SEND_ACC_ERROR;
            }
            return nRet;
        }
    }
    if (type == CLOSE_STREAM) //3
    {
        LOGE("-----CLOSE_STREAM--------\n");
        Close();
    }

    return 0;
}
