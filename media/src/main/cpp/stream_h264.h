#ifndef __STREAM_H264__
#define __STREAM_H264__

#include "stream_data.h"
#include "sps_decode.h"
#include "comm.h"


int SendMetadata(LPRTMPMetadata lpMetaData);
//void init();
//int Connect(char* url);
//int Net_Init(const char* url);
//void Close();
int ReadOneNaluFromBuf(NaluUnit *nalu);
static int SendPacket(unsigned int nPacketType, unsigned char *data, unsigned int size, unsigned int nTimestamp);
int SendH264Packet(unsigned char *data, unsigned int size, int bIsKeyFrame, unsigned int nTimeStamp);
int SendH264Stream(unsigned char *pStreamData);
int KMP_search(const char *buf,  unsigned int datalen, const char *pattern, unsigned int dlen,  int pos, int next[]);
void get_next(const char *pattern, int next[]);
int Net_Init(const char* url);
int SendSPSPPSPacke(NaluUnit *naluUnit);

#endif
