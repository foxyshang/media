#ifndef __SPS_DECODE_H_
#define __SPS_DECODE_H_

#include <stdio.h>
#include <math.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>

#ifdef ANDROID

#include <jni.h>
#include <android/log.h>

#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "media", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "media", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif

#include "amf.h"
#include "rtmp.h"
#include "rtmp_sys.h"

typedef unsigned int UINT;
typedef unsigned char BYTE;
typedef unsigned long DWORD;

void de_emulation_prevention(BYTE *buf, unsigned int *buf_size);

int h264_decode_sps(BYTE *buf, unsigned int nLen, int *width, int *height, int *fps);

char *put_amf_double(char *c, double d);

char *put_amf_string(char *c, const char *str);

char *put_be16(char *output, uint16_t nVal);

char *put_be24(char *output, uint32_t nVal);

char *put_be32(char *output, uint32_t nVal);

char *put_be24(char *output, uint32_t nVal);

char *put_be32(char *output, uint32_t nVal);

char *put_be64(char *output, uint64_t nVal);

char *put_byte(char *output, uint8_t nVal);

int e(BYTE *pBuff, UINT nLen, UINT *nStartBit);

DWORD u(UINT BitCount, BYTE *buf, UINT *nStart);

UINT Ue(BYTE *pBuff, UINT nLen, UINT *nStart);

#endif
