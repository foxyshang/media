#ifndef __STREAM_DATA__
#define __STREAM_DATA__

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>


#include "amf.h"
#include "rtmp.h"
#include "rtmp_sys.h"
#include "comm.h"

#ifdef ANDROID
#include <jni.h>
#include <android/log.h>

#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif



#define BUFFER_SIZE (32768)       //

enum
{
	FLV_CODECID_H264 = 7,
};

// NALU��Ԫ
typedef struct _NaluUnit
{
	int type;
	int size;
	unsigned char *data;
}NaluUnit;

typedef struct _RTMPMetadata
{
	// video, must be h264 type
	unsigned int	nWidth;
	unsigned int	nHeight;
	unsigned int	nFrameRate;		// fps
	unsigned int	nVideoDataRate;	// bps
	unsigned int	nSpsLen;
	unsigned char	Sps[1024];
	unsigned int	nPpsLen;
	unsigned char	Pps[1024];

	// audio, must be aac type
	int	        bHasAudio;
	unsigned int	nAudioDatarate;
	unsigned int	nAudioSampleRate;
	unsigned int	nAudioSampleSize;
	int				nAudioFmt;
	unsigned int	nAudioChannels;
	char		    pAudioSpecCfg;
	unsigned int	nAudioSpecCfgLen;

} RTMPMetadata, *LPRTMPMetadata;

extern unsigned char* m_pFileBuf;
extern unsigned int  m_nFileBufSize;
extern unsigned int  m_nCurPos;
//extern int g_nInitFlag;
//extern int g_nSpsPpsFlag;
//extern int g_nAacHeadFlag;
//extern unsigned int g_ntick;
#endif
