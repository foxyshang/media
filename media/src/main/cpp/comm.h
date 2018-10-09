#ifndef __COMMON_H_
#define __COMMON_H_

#include "stream_data.h"

#define NETINIT_ERROR   -2
#define SEND_H264_ERROR -3
#define SEND_ACC_ERROR  -4
#define NETCONN_ERROR   -5

#define NET_INIT	0
#define SEND_VIDEIO_STREAM 1
#define SEND_AUDIO_STREAM  2
#define CLOSE_STREAM  3

//extern RTMP* m_pRtmp;
//extern unsigned char* m_pFileBuf;
//extern unsigned int  m_nFileBufSize;
//extern unsigned int  m_nCurPos;
extern int g_nInitFlag;
extern int g_nSpsPpsFlag;
extern int g_nAacHeadFlag;
extern unsigned int g_ntick;
extern RTMP* m_pRtmp;
extern long startTime;
extern char push_url[512];
int Net_Init(const char* url);
void Close();

#endif
