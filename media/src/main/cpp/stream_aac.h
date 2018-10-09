#ifndef __STREAM_AAC_H
#define __STREAM_AAC_H

#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <string.h>
#include <time.h>

#include "amf.h"
#include "rtmp.h"
#include "rtmp_sys.h"
#include "stream_data.h"

#define RTMP_HEAD_SIZE   (sizeof(RTMPPacket)+RTMP_MAX_HEADER_SIZE)

typedef struct
{
    short syncword;
    short id;
    short layer;
    short protection_absent;
    short profile;
    short sf_index;
    short private_bit;
    short channel_configuration;
    short original;
    short home;
    short emphasis;
    short copyright_identification_bit;
    short copyright_identification_start;
    short aac_frame_length;
    short adts_buffer_fullness;
    short no_raw_data_blocks_in_frame;
    short crc_check;

    /* control param */
    short old_format;
} adts_header;

typedef struct
{
    short adts_header_present;
    short sf_index;
    short object_type;
    short channelConfiguration;
    short frameLength;
}faacDecHandle;

#define MAX_CHANNELS        64
#define FAAD_MIN_STREAMSIZE 768 /* 6144 bits/channel */
static int adts_sample_rates[] = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350, 0, 0, 0};

typedef struct
{
    long bytes_into_buffer;
    long bytes_consumed;
    long file_offset;
    long bits_had_read;
    unsigned char *buffer;
    int at_eof;
    char *indata;
} aac_buffer;


int SendAacStream(unsigned char *cdata, int nlen);
int adts_parse(aac_buffer *b, int *bitrate, float *length);
int first_adts_analysis(unsigned char *buffer,adts_header* adts);
void advance_buffer(aac_buffer *b, int bytes);
int fill_buffer(aac_buffer *b);
int sendAACPacket(unsigned char *data, int acc_raw_data_length);
int sendAACSequenceHeaderPacket(int aac_type,int sample_index,int channel_conf);
static int SendPacket(unsigned int nPacketType, unsigned char *data, unsigned int size, unsigned int nTimestamp);
unsigned long GetTickCount();

#endif
