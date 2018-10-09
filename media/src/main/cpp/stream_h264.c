#include "stream_h264.h"
#include "stream_aac.h"
#include <pthread.h>

unsigned char *m_pFileBuf = 0x0;
unsigned int m_nFileBufSize = BUFFER_SIZE;
unsigned int m_nCurPos = 0;
//RTMPMetadata metaData;
//int g_nInitFlag = 0;
//int g_nSpsPpsFlag = 0;
//int g_nAacHeadFlag = 0;
unsigned int g_ntick = 0;

int SendMetadata(LPRTMPMetadata lpMetaData) {
    if (lpMetaData == NULL) {
        return 0;
    }
    char body[1024] = {0};;
    char *p = (char *) body;
    p = put_byte(p, AMF_STRING);
    p = put_amf_string(p, "@setDataFrame");

    p = put_byte(p, AMF_STRING);
    p = put_amf_string(p, "onMetaData");

    p = put_byte(p, AMF_OBJECT);
    p = put_amf_string(p, "copyright");
    p = put_byte(p, AMF_STRING);
    p = put_amf_string(p, "5.cn");

    p = put_amf_string(p, "width");
    p = put_amf_double(p, lpMetaData->nWidth);

    p = put_amf_string(p, "height");
    p = put_amf_double(p, lpMetaData->nHeight);

    p = put_amf_string(p, "framerate");
    p = put_amf_double(p, lpMetaData->nFrameRate);

    p = put_amf_string(p, "videocodecid");
    p = put_amf_double(p, FLV_CODECID_H264);

    p = put_amf_string(p, "");
    p = put_byte(p, AMF_OBJECT_END);

    int index = p - body;

    SendPacket(RTMP_PACKET_TYPE_INFO, (unsigned char *) body, p - body, 0);

    int i = 0;
    body[i++] = 0x17; // 1:keyframe  7:AVC
    body[i++] = 0x00; // AVC sequence header

    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00; // fill in 0;

    // AVCDecoderConfigurationRecord.
    body[i++] = 0x01; // configurationVersion
    body[i++] = lpMetaData->Sps[1]; // AVCProfileIndication
    body[i++] = lpMetaData->Sps[2]; // profile_compatibility
    body[i++] = lpMetaData->Sps[3]; // AVCLevelIndication
    body[i++] = 0xff; // lengthSizeMinusOne

    // sps nums
    body[i++] = 0xE1; //&0x1f
    // sps data length
    body[i++] = lpMetaData->nSpsLen >> 8;
    body[i++] = lpMetaData->nSpsLen & 0xff;
    // sps data
    memcpy(&body[i], lpMetaData->Sps, lpMetaData->nSpsLen);
    i = i + lpMetaData->nSpsLen;

    // pps nums
    body[i++] = 0x01; //&0x1f
    // pps data length
    body[i++] = lpMetaData->nPpsLen >> 8;
    body[i++] = lpMetaData->nPpsLen & 0xff;
    // sps data
    memcpy(&body[i], lpMetaData->Pps, lpMetaData->nPpsLen);
    i = i + lpMetaData->nPpsLen;

    return SendPacket(RTMP_PACKET_TYPE_VIDEO, (unsigned char *) body, i, 0);

}

//void init()
//{
//	m_pFileBuf = (char *)malloc(BUFFER_SIZE);
//	memset(m_pFileBuf, 0, BUFFER_SIZE);
//	m_nFileBufSize = BUFFER_SIZE;
//	m_pRtmp = RTMP_Alloc();
//	RTMP_Init(m_pRtmp);
//}

//int Connect(char* url)
//{
//	if (RTMP_SetupURL(m_pRtmp, (char*)url) < 0)
//	{
//		return -1;
//	}
//	RTMP_EnableWrite(m_pRtmp);
//	if (RTMP_Connect(m_pRtmp, NULL) < 0)
//	{
//		return -1;
//	}
//	if (RTMP_ConnectStream(m_pRtmp, 0) < 0)
//	{
//		return -1;
//	}
//	return 0;
//}

//int Net_Init(const char* url)
//{
//	InitSockets();
//
//	m_pRtmp = RTMP_Alloc();
//	RTMP_Init(m_pRtmp);
//	//����URL
//	if (RTMP_SetupURL(m_pRtmp,(char*)url) == FALSE)
//	{
//		RTMP_Free(m_pRtmp);
//		return -1;
//	}
//	//���ÿ�д,��������,����������������ǰʹ��,������Ч
//	RTMP_EnableWrite(m_pRtmp);
//	//���ӷ�����
//	if (RTMP_Connect(m_pRtmp, NULL) == FALSE)
//	{
//		RTMP_Free(m_pRtmp);
//		return -1;
//	}
//
//	//������
//	if (RTMP_ConnectStream(m_pRtmp,0) == FALSE)
//	{
//		RTMP_Close(m_pRtmp);
//		RTMP_Free(m_pRtmp);
//		return -1;
//	}
//
//	return 0;
//}
////
//void Close()
//{
//	if(m_pRtmp)
//	{
//		RTMP_Close(m_pRtmp);
//		RTMP_Free(m_pRtmp);
//		m_pRtmp = NULL;
//	}
//	if (m_pFileBuf)
//	{
//		free(m_pFileBuf);
//		m_pFileBuf = 0x0;
//	}
//	g_nflag = 0;
//	g_ntick = 0;
//}

//int ReadOneNaluFromBuf(NaluUnit *nalu)
//{
//	int i = m_nCurPos;
//	while (i < m_nFileBufSize)
//	{
//		if(m_pFileBuf[i++] == 0x00 &&
//			m_pFileBuf[i++] == 0x00 &&
//			m_pFileBuf[i++] == 0x00 &&
//			m_pFileBuf[i++] == 0x01
//			)
//		{
//			int pos = i;
//			while (pos < m_nFileBufSize)
//			{
//				if(m_pFileBuf[pos++] == 0x00 &&
//					m_pFileBuf[pos++] == 0x00 &&
//					m_pFileBuf[pos++] == 0x00 &&
//					m_pFileBuf[pos++] == 0x01
//					)
//				{
//					break;
//				}
//			}
//			if(pos == m_nFileBufSize)
//			{
//				nalu->size = pos-i;
//			}
//			else
//			{
//				nalu->size = (pos-4)-i;
//			}
//			nalu->type = m_pFileBuf[i]&0x1f;
//			nalu->data = &m_pFileBuf[i];
//			m_nCurPos = pos-4;
//			return 1;
//		}
//	}
//	return 0;
//}

static int SendPacket(unsigned int nPacketType, unsigned char *data, unsigned int size,
                      unsigned int nTimestamp) {
    if (m_pRtmp == NULL) {
        return -1;
    }
    int nRet = -1;
    RTMPPacket packet;
    RTMPPacket_Reset(&packet);
    RTMPPacket_Alloc(&packet, size);

    packet.m_packetType = nPacketType;
    packet.m_nChannel = 0x04;
    packet.m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet.m_nTimeStamp = nTimestamp;
    packet.m_nInfoField2 = m_pRtmp->m_stream_id;
    packet.m_nBodySize = size;
    memcpy(packet.m_body, data, size);

    if (RTMP_IsConnected(m_pRtmp)) {
        nRet = RTMP_SendPacket(m_pRtmp, &packet, 0);//
    } else {
        nRet = NETCONN_ERROR;
    }
    RTMPPacket_Free(&packet);
    return nRet;
}

int
SendH264Packet(unsigned char *data, unsigned int size, int bIsKeyFrame, unsigned int nTimeStamp) {
    if (data == NULL) {
        return 0;
    }

    unsigned char *body = (char *) malloc(size + 9);

    int i = 0;
    if (bIsKeyFrame) {
        body[i++] = 0x17;// 1:Iframe  7:AVC
    } else {
        body[i++] = 0x27;// 2:Pframe  7:AVC
    }
    body[i++] = 0x01;// AVC NALU
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    // NALU size
    body[i++] = size >> 24;
    body[i++] = size >> 16;
    body[i++] = size >> 8;
    body[i++] = size & 0xff;;

    // NALU data
    memcpy(&body[i], data, size);
    int bRet = SendPacket(RTMP_PACKET_TYPE_VIDEO, body, i + size, nTimeStamp);
    free(body);
    body = 0x0;

    return bRet;
}

int SendH264Stream(unsigned char *pStreamData) {
    if (pStreamData == NULL) {
        return 0;
    }
//	FILE *fp = fopen(pStream, "rb");
//	if(!fp)
//	{
//		printf("ERROR:open file %s failed!", pStream);
//	}
//	fseek(fp, 0, SEEK_SET);
//	m_nFileBufSize = fread(m_pFileBuf, sizeof(unsigned char), FILEBUFSIZE, fp);
//	if(m_nFileBufSize >= FILEBUFSIZE)
//	{
//		printf("warning : File size is larger than BUFSIZE\n");
//	}
//	fclose(fp);
    LOGE("-----1--------\n");
    m_pFileBuf = pStreamData;
    NaluUnit naluUnit;
    LOGE("-----2--------\n");
    if (!g_nSpsPpsFlag) {
        LOGE("-----3--------\n");
        //init();
        g_ntick = 0;
        //Connect("rtmp://60.247.26.246:1935/live/testdf");
        RTMPMetadata metaData;
        memset(&metaData, 0, sizeof(RTMPMetadata));
        LOGE("-----4--------\n");
        ReadOneNaluFromBuf(&naluUnit);
        LOGE("-----41--------\n");
        metaData.nSpsLen = naluUnit.size;
        LOGE("-----42--------\n");
        memcpy(metaData.Sps, naluUnit.data, naluUnit.size);
        LOGE("-----5--------\n");
        ReadOneNaluFromBuf(&naluUnit);
        metaData.nPpsLen = naluUnit.size;
        memcpy(metaData.Pps, naluUnit.data, naluUnit.size);

        LOGE("-----6--------\n");
        int width = 0, height = 0, fps = 0;
        h264_decode_sps(metaData.Sps, metaData.nSpsLen, &width, &height, &fps);
        metaData.nWidth = width;
        metaData.nHeight = height;
        if (fps)
            metaData.nFrameRate = fps;
        else
            metaData.nFrameRate = 25;
        LOGE("-----7--------\n");
        SendMetadata(&metaData);
        LOGE("-----8--------\n");
        g_nSpsPpsFlag++;
    }
//	unsigned int tick = 0;
    memset(&naluUnit, 0x0, sizeof(NaluUnit));
    LOGE("-----while--------\n");
    while (ReadOneNaluFromBuf(&naluUnit)) {
        if (naluUnit.type == 0x07) {
            SendSPSPPSPacke(&naluUnit);
            continue;
        }
        int bKeyframe = (naluUnit.type == 0x05) ? 1 : 0;
        int bRet = SendH264Packet(naluUnit.data, naluUnit.size, bKeyframe,
                                  GetTickCount() - startTime);
        if (bRet <= 0) {
            return bRet;
        }
//		msleep(1);
//		g_ntick +=1;
    }

    return 1;
}

void get_next(const char *pattern, int next[]) {
    unsigned int i = 0;
    int j = -1;
    next[0] = -1;
    int len = strlen(pattern);
    while (i < len) {
        if (j == -1 || pattern[i] == pattern[j]) {
            ++i;
            ++j;
            LOGE("-----======----  ", i);
            LOGE("-----i----  %d  ---\n", i);
            LOGE("-----j----  %d  ---\n", j);
            if (i >= len)
                return;
            next[i] = j; //
        } else
            LOGE("-----!!!!!!!----  ", i);
        LOGE("-----i----  %d  ---\n", i);
        LOGE("-----j----  %d  ---\n", j);
        j = next[j];
    }
}

int
KMP_search(const char *buf, unsigned int datalen, const char *pattern, unsigned int dlen, int pos,
           int next[]) {
    unsigned int i = pos;
    int j = 0;
    if (datalen < dlen)
        return -2;
    for (i = pos; i < datalen; ++i) {
        while (j > 0 && buf[i] != (pattern[j] - 48))
            j = next[j];
        if (buf[i] == (pattern[j] - 48))
            ++j;
        if (j == dlen)
            return i - j + 1;
    }
    return -1;
}

int ReadOneNaluFromBuf(NaluUnit *nalu) {

    int total = m_nFileBufSize;
    if (!m_pFileBuf || (m_nCurPos >= total)) {
        return 0;
    }
    int next3[3] = {0};
    int next4[4] = {0};
    int i = -1, j = -1;
    int n_pos = m_nCurPos;
//	  unsigned int total = m_nFileBufSize;
    int flag = 0;

    LOGE("-----next3--------\n");
    get_next("001", next3);
    LOGE("-----next4--------\n");
    get_next("0001", next4);

    LOGE("-----a--------\n");
    i = KMP_search(m_pFileBuf, total, "0001", 4, n_pos, next4);
    j = KMP_search(m_pFileBuf, total, "001", 3, n_pos, next3);
    LOGE("-----b--------\n");
    if (i >= 0 && (i < j || j < 0)) {
        n_pos = i + 4;
    } else if (j >= 0 && (j < i || i < 0)) {
        n_pos = j + 3;
    } else if (j < 0 && i < 0) {
        return 0;
    }
    LOGE("-----c--------\n");
    int k = n_pos;
    nalu->type = m_pFileBuf[k] & 0x1f;
    nalu->data = &m_pFileBuf[k];
////////////////////////////////////////////////////////////
    p:
    i = KMP_search(m_pFileBuf, total, "0001", 4, n_pos, next4);
    j = KMP_search(m_pFileBuf, total, "001", 3, n_pos, next3);
////////////////////////////////////////////////////////////
    LOGE("-----d--------\n");
    if (i >= 0 && (i < j || j < 0)) {
        printf("m_pFileBuf[i+5]=%0x, nalu->type=%d, i=%d, j=%d\n", m_pFileBuf[i + 5], nalu->type, i,
               j);
        if (((m_pFileBuf[i + 5] & 0x80) != 0x80) && (nalu->type != 7 && nalu->type != 8)) {
            n_pos = i + 4;
            LOGE("-----goto p-------\n");
            goto p;
        }
        nalu->size = i - k;
        m_nCurPos = i;
        LOGE("-----bdhdh--------\n");
        LOGE("-----e--------\n");

        return 1;
    } else if (j >= 0 && (j < i || i < 0)) {

        if ((m_pFileBuf[j + 4] & 0x80 != 0x80) && (nalu->type != 7 && nalu->type != 8)) {
            n_pos = j + 3;
            goto p;
        }
        nalu->size = j - k;
        m_nCurPos = j;
//  		nalu->type = m_pFileBuf[k]&0x1f;
//			nalu->data = &m_pFileBuf[k];
        LOGE("-----f--------\n");
        return 1;
    } else if (i < 0 || j < 0) {
//  		nalu->type = m_pFileBuf[k]&0x1f;
//			nalu->data = &m_pFileBuf[k];
        nalu->size = total - k;
        m_nCurPos = total;
        LOGE("-----g--------\n");
        return 1;
    }

    return 0;
}

int SendSPSPPSPacke(NaluUnit *naluUnit) {
    char body[40960] = {0};
    int i = 0;
    body[i++] = 0x17; // 1:keyframe  7:AVC
    body[i++] = 0x00; // AVC sequence header

    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00; // fill in 0;

    // AVCDecoderConfigurationRecord.
    body[i++] = 0x01; // configurationVersion
    body[i++] = naluUnit->data[1]; // AVCProfileIndication
    body[i++] = naluUnit->data[2]; // profile_compatibility
    body[i++] = naluUnit->data[3]; // AVCLevelIndication
    body[i++] = 0xff; // lengthSizeMinusOne

    // sps nums
    body[i++] = 0xE1; //&0x1f
    // sps data length
    body[i++] = naluUnit->size >> 8;
    body[i++] = naluUnit->size & 0xff;
    // sps data
    memcpy(&body[i], naluUnit->data, naluUnit->size);
    i = i + naluUnit->size;
//	SendPacket(RTMP_PACKET_TYPE_VIDEO, (unsigned char*)body, i, GetTickCount()-start_time);
    // ��ȡPPS֡
    NaluUnit pps_naluUnit;
    ReadOneNaluFromBuf(&pps_naluUnit);
    // pps nums
    body[i++] = 0x01; //&0x1f
    // pps data length
    body[i++] = pps_naluUnit.size >> 8;
    body[i++] = pps_naluUnit.size & 0xff;
    // sps data
    memcpy(&body[i], pps_naluUnit.data, pps_naluUnit.size);
    i = i + pps_naluUnit.size;

    return SendPacket(RTMP_PACKET_TYPE_VIDEO, (unsigned char *) body, i,
                      0);//GetTickCount()-startTime
}


