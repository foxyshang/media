#include "comm.h"
#include "stream_aac.h"


RTMP* m_pRtmp = 0x0;
int g_nInitFlag = 0;
int g_nSpsPpsFlag = 0;
int g_nAacHeadFlag = 0;
long startTime = 0;
char push_url[512] = {0};

int Net_Init(const char* url)
{
	g_nInitFlag = 0;
	g_nSpsPpsFlag = 0;
	g_nAacHeadFlag = 0;
	memset(push_url, 0x0, sizeof(push_url));
	memcpy(push_url, url, strlen(url)+1);
	startTime = GetTickCount();
	m_pRtmp = RTMP_Alloc();
	if (!m_pRtmp)
	{
		return -1;
	}
	RTMP_Init(m_pRtmp);
	if (RTMP_SetupURL(m_pRtmp,(char*)url) == FALSE)
	{
		RTMP_Free(m_pRtmp);
		m_pRtmp = 0x0;
		return -1;
	}
	RTMP_EnableWrite(m_pRtmp);
	if (RTMP_Connect(m_pRtmp, NULL) == FALSE)
	{
		RTMP_Free(m_pRtmp);
		m_pRtmp = 0x0;
		return -1;
	}
	//
	if (RTMP_ConnectStream(m_pRtmp,0) == FALSE)
	{
		RTMP_Close(m_pRtmp);
		RTMP_Free(m_pRtmp);
		m_pRtmp = 0x0;
		return -1;
	}
	//LOGE( "3--------------------------Net_Init--------------\n");
	return 0;
}
//
void Close()
{
	if(m_pRtmp)
	{
		RTMP_Close(m_pRtmp);
		RTMP_Free(m_pRtmp);
		m_pRtmp = 0x0;
	}
	if (m_pFileBuf)
	{
		free(m_pFileBuf);
		m_pFileBuf = 0x0;
	}
	g_nInitFlag = 0;
	g_nSpsPpsFlag = 0;
	g_nAacHeadFlag = 0;
	g_ntick = 0;
	//LOGE( "-------=====---66666-------------------Close--------------\n");
}
