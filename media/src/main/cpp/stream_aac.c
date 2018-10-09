#include "stream_aac.h"

int SendAacStream(unsigned char *cdata, int nlen)
{
    int bread, bRet;
    int samplerate;
    int fileread;
    int bitrate;
    float length;
    aac_buffer b;
    adts_header adts;
    int dlen = nlen;
    memset(&b, 0, sizeof(aac_buffer));
    //   b.indata = fopen("cuc_ieschool.aac", "rb");
// 	b.indata = (char *)malloc(dlen);
//  if (b.indata == NULL)
//	{
    /* unable to open file */
// 	  fprintf(stderr, "Error opening the input file");
//}
//    fseek(b.indata, 0, SEEK_END);
//    fileread = ftell(b.indata);
//    fseek(b.indata, 0, SEEK_SET);
    /*buffer*/
//    if ( !(b.buffer = (unsigned char*)malloc(dlen)) )
//    {
// 	   fprintf(stderr, "Memory allocation error\n");
//	}

//    memset(b.buffer, 0, dlen);
    b.buffer = cdata;
    /*read*/
//    bread = fread(b.buffer, 1, FAAD_MIN_STREAMSIZE*MAX_CHANNELS, b.indata);
//		bread = dlen;
    b.bytes_into_buffer = dlen;
    b.bytes_consumed = 0;
//    b.file_offset = 0;
//	b.bits_had_read = 0;

//	if (bread != FAAD_MIN_STREAMSIZE*MAX_CHANNELS)
//        b.at_eof = 1;
    if ((b.buffer[0] == 0xFF) && ((b.buffer[1] & 0xF6) == 0xF0))
    {
        if ( !g_nAacHeadFlag )
        {
            if (!(bRet = first_adts_analysis(b.buffer, &adts)) )
            {
                return bRet;
            }
            g_nAacHeadFlag++;
            //b.bits_had_read = 74;
        }
        if (!(bRet = adts_parse(&b, &bitrate, &length)) )
        {
            return bRet;
        }
        //fseek(b.indata, 0, SEEK_SET);
        //bread = fread(b.buffer, 1, FAAD_MIN_STREAMSIZE*MAX_CHANNELS, b.indata);
//       if (bread != FAAD_MIN_STREAMSIZE*MAX_CHANNELS)
//            b.at_eof = 1;
//        else
//            b.at_eof = 0;
//        b.bytes_into_buffer = bread;
//        b.bytes_consumed = 0;
//        b.file_offset = 0;
    }

    /*close*/
//    if (b.buffer)
//    {
//    	free(b.buffer);
//    	b.buffer = 0x0;
//    }
//    fclose(b.indata);
}

int first_adts_analysis(unsigned char *buffer, adts_header* adts)
{
    int bRet;
    int protection;
    int profile ;
    int sampleIndex ;
    int channelConfiguration ;

    if ((buffer[0] == 0xFF)&&((buffer[1] & 0xF6) == 0xF0))
    {
        adts->id = ((unsigned short) buffer[1] & 0x8) >> 3;
        fprintf(stderr, "adts:id  %d\n",adts->id);
        adts->layer = ((unsigned short) buffer[1] & 0x6) >> 1;
        fprintf(stderr, "adts:layer  %d\n",adts->layer);
        adts->protection_absent = (unsigned short) buffer[1] & 0x1;
        fprintf(stderr, "adts:protection_absent  %d\n",adts->protection_absent);
        adts->profile = ((unsigned short) buffer[2] & 0xc0) >> 6;
        fprintf(stderr, "adts:profile  %d\n",adts->profile);
        adts->sf_index = ((unsigned short) buffer[2] & 0x3c) >> 2;
        fprintf(stderr, "adts:sf_index  %d\n",adts->sf_index);
        adts->private_bit = ((unsigned short) buffer[2] & 0x2) >> 1;
        fprintf(stderr, "adts:pritvate_bit  %d\n",adts->private_bit);
        adts->channel_configuration = ((((unsigned short) buffer[2] & 0x1) << 3) |
                                       (((unsigned short) buffer[3] & 0xc0) >> 6));
        fprintf(stderr, "adts:channel_configuration  %d\n",adts->channel_configuration);
        adts->original = ((unsigned short) buffer[3] & 0x30) >> 5;
        fprintf(stderr, "adts:original  %d\n",adts->original);
        adts->home = ((unsigned short) buffer[3] & 0x10) >> 4;
        fprintf(stderr, "adts:home  %d\n",adts->home);
        adts->emphasis = ((unsigned short) buffer[3] & 0xc) >> 2;
        fprintf(stderr, "adts:emphasis  %d\n",adts->emphasis);
        adts->copyright_identification_bit = ((unsigned short) buffer[3] & 0x2) >> 1;
        fprintf(stderr, "adts:copyright_identification_bit  %d\n",adts->copyright_identification_bit);
        adts->copyright_identification_start = (unsigned short) buffer[3] & 0x1;
        fprintf(stderr, "adts:copyright_identification_start  %d\n",adts->copyright_identification_start);
        adts->aac_frame_length = ((((unsigned short) buffer[4]) << 5) |
                                  (((unsigned short) buffer[5] & 0xf8) >> 3));
        fprintf(stderr, "adts:aac_frame_length  %d\n",adts->aac_frame_length);
        adts->adts_buffer_fullness = (((unsigned short) buffer[5] & 0x7) |
                                      ((unsigned short) buffer[6]));
        fprintf(stderr, "adts:adts_buffer_fullness  %d\n",adts->adts_buffer_fullness);
        adts->no_raw_data_blocks_in_frame = ((unsigned short) buffer[7] & 0xc0) >> 6;
        fprintf(stderr, "adts:no_raw_data_blocks_in_frame  %d\n",adts->no_raw_data_blocks_in_frame);

        // The protection bit indicates whether or not the header contains the two extra bytes
        protection = (buffer[1]&0x01)>0 ? 1 : 0;

        if(!protection)
        {
            adts->crc_check = ((((unsigned short) buffer[7] & 0x3c) << 10) |
                               (((unsigned short) buffer[8]) << 2) |
                               (((unsigned short) buffer[9] & 0xc0) >> 6));
            fprintf(stderr, "adts:crc_check  %d\n",adts->crc_check);
        }

        /*
        frameLength = (buffer[3]&0x03) << 11 |
                (buffer[4]&0xFF) << 3 |
                (buffer[5]&0xFF) >> 5 ;

        frameLength -= (protection ? 7 : 9);

        // Read CRS if any
        if (!protection) is.read(buffer,0,2);
        */

        profile = ( (buffer[2]&0xC0) >> 6 ) + 1 ;
        sampleIndex = (buffer[2]&0x3C) >> 2;
        channelConfiguration = ((buffer[2] & 0x01) << 2) + (buffer[3] >> 6);

        bRet = sendAACSequenceHeaderPacket(profile, sampleIndex, channelConfiguration);
    }
}

int sendAACSequenceHeaderPacket(int aac_type, int sample_index, int channel_conf)
{
    int bRet;
#if 1
    unsigned char body[4];
    body[0]=0xAF;
    body[1]=0x00;

    unsigned char tmp = 0;
    tmp |= (((unsigned char)aac_type) << 3);
    tmp |= ((((unsigned char)sample_index) >> 1) & 0x07);

    body[2]=tmp;

    tmp = 0;
    tmp |= (((unsigned char)sample_index) << 7);
    tmp |= (((unsigned char)channel_conf) << 3) & 0x78;

    body[3]=tmp;

    bRet = SendPacket(RTMP_PACKET_TYPE_AUDIO, body, 4, GetTickCount());
#else

    typedef unsigned short uint16_t;

	unsigned char body[4]={0};
	int i=0;
	//AACdecoderSpecificInfo
/*
	body[i++]= (2 << 4) |        // soundformat "10 == AAC"
				(3 << 2) |        // soundrate   "3  == 44-kHZ"
				(1 << 1) |        // soundsize   "1  == 16bit"
				1;                // soundtype   "1  == Stereo"

	body[i++]=0x00;
	//AudioSpecificConfig
	uint16_t audio_specific_config = 0;
	audio_specific_config |= ((2<<11)&0xF800);  // 2: AACLC(Low Complexity)
	audio_specific_config |= ((4<<7)&0x0780); 	//4: 44KHz
	audio_specific_config |= ((2<<3)&0x78);   	//2: Stereo
	audio_specific_config |= (0&0x07);        	//Padding:000

	body[i++] = (audio_specific_config>>8) & 0xFF;
	body[i++] = audio_specific_config & 0xFF;
*/
	body[0]=0xAF;
	body[1]=0x00;
	body[2]=0x12;
	body[3]=0x10;

	SendPacket(RTMP_PACKET_TYPE_AUDIO, body, 4, timestamp);
#endif
    return bRet;
}

/**

 */

static int SendPacket(unsigned int nPacketType, unsigned char *data, unsigned int size, unsigned int nTimestamp)
{
    int nRet =0;
    RTMPPacket* packet=NULL;
    packet = (RTMPPacket *)malloc(RTMP_HEAD_SIZE+size);
    memset(packet,0,RTMP_HEAD_SIZE);
    packet->m_body = (char *)packet + RTMP_HEAD_SIZE;
    packet->m_nBodySize = size;
    memcpy(packet->m_body, data, size);
    packet->m_hasAbsTimestamp = 0;
    packet->m_packetType = nPacketType;
    packet->m_nInfoField2 = m_pRtmp->m_stream_id;
    packet->m_nChannel = 0x04;
    //if (RTMP_PACKET_TYPE_AUDIO ==nPacketType && size !=4)
    {
        packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    }
    //packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nTimeStamp = nTimestamp;
    //����
    if (RTMP_IsConnected(m_pRtmp))
    {
        nRet = RTMP_SendPacket(m_pRtmp, packet, FALSE); //
    }
    else
    {
        nRet = NETCONN_ERROR;
    }
    if (packet)
    {
        free(packet);
        packet = 0x0;
    }
    return nRet;
}

int adts_parse(aac_buffer *b, int *bitrate, float *length)
{
    int frames, frame_length;
    int t_framelength = 0;
    int nRet;
    int samplerate;
    float frames_per_sec, bytes_per_frame;

    /* Read all frames to ensure correct time and bitrate */
    for (frames = 0; /* */; frames++)
    {
        fill_buffer(b);

        if (b->bytes_into_buffer > 7)
        {
            /* check syncword */
            if (!((b->buffer[0] == 0xFF)&&((b->buffer[1] & 0xF6) == 0xF0)))
                break;

            if (frames == 0)
                samplerate = adts_sample_rates[(b->buffer[2]&0x3c)>>2];

            frame_length = ((((unsigned int)b->buffer[3] & 0x3)) << 11)
                           | (((unsigned int)b->buffer[4]) << 3) | (b->buffer[5] >> 5);

            t_framelength += frame_length;

            if (frame_length > b->bytes_into_buffer)
                break;

            fprintf(stderr, "size:  %d\n",frame_length);
            if ( (nRet = sendAACPacket(b->buffer, frame_length)) <= 0 )
            {
                return nRet;
            }
            //msleep(1);
            //------------------
            advance_buffer(b, frame_length);
        } else {
            break;
        }
    }

    frames_per_sec = (float)samplerate/1024.0f;
    if (frames != 0)
        bytes_per_frame = (float)t_framelength/(float)(frames*1000);
    else
        bytes_per_frame = 0;
    *bitrate = (int)(8. * bytes_per_frame * frames_per_sec + 0.5);
    fprintf(stderr, "bitrate:  %d\n",*bitrate);
    if (frames_per_sec != 0)
        *length = (float)frames/frames_per_sec;
    else
        *length = 1;
    fprintf(stderr, "length  %f\n",*length);

    return 1;
}

void advance_buffer(aac_buffer *b, int bytes)
{
//    b->file_offset += bytes;
    b->bytes_consumed = bytes;
    b->bytes_into_buffer -= bytes;
}

int fill_buffer(aac_buffer *b)
{
    int bread;
    if (b->bytes_consumed > 0)
    {
        if (b->bytes_into_buffer)
        {
            memmove((void*)b->buffer, (void*)(b->buffer + b->bytes_consumed),
                    b->bytes_into_buffer*sizeof(unsigned char));
        }
        b->bytes_consumed = 0;
        if (b->bytes_into_buffer > 3)
        {
            if (memcmp(b->buffer, "TAG", 3) == 0)
                b->bytes_into_buffer = 0;
        }
        if (b->bytes_into_buffer > 11)
        {
            if (memcmp(b->buffer, "LYRICSBEGIN", 11) == 0)
                b->bytes_into_buffer = 0;
        }
        if (b->bytes_into_buffer > 8)
        {
            if (memcmp(b->buffer, "APETAGEX", 8) == 0)
                b->bytes_into_buffer = 0;
        }
    }
    return 1;
}
int sendAACPacket(unsigned char *data, int acc_raw_data_length)
{
    int i=0;
    int body_size = 2+acc_raw_data_length-7;
    unsigned char *body=(unsigned char *)malloc(body_size*sizeof(unsigned char));
    if (!body)
    {
        return 0;
    }
    //AACdecoderSpecificInfo
    body[i++] = 0xAF;
    body[i++] = 0x01;
    //Aac raw data without 7bytes frame header
    memcpy(&body[i],data+7, acc_raw_data_length-7);
    int bRet = SendPacket(RTMP_PACKET_TYPE_AUDIO,body,body_size, GetTickCount()-startTime);
    if (body)
    {
        free(body);
        body = 0x0;
    }
    return bRet;
}
unsigned long GetTickCount()
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (ts.tv_sec * 1000 + ts.tv_nsec / 1000000);
}
