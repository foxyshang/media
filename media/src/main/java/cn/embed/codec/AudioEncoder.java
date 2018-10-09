package cn.embed.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder {
    private final static String MINE_TYPE = "audio/mp4a-latm";
    private MediaCodec mediaCodec;
    ByteBuffer[] inputBuffers = null;
    ByteBuffer[] outputBuffers = null;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private int sampleRate = 44100;
    private int bit_rate = 2 * 64 * 1024;
    private static final boolean isSaveFile = false;

    public static FileOutputStream fos;

    public AudioEncoder() {
        initialize(this.sampleRate, this.bit_rate);
    }

    public AudioEncoder(int sampleRate, int bit_rate) {
        this.bit_rate = bit_rate;
        this.sampleRate = sampleRate;
        initialize(this.sampleRate, this.bit_rate);
    }

    //初始化编码器
    private void initialize(int sampleRate, int bit_rate) {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MINE_TYPE);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MINE_TYPE, sampleRate, 2);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit_rate);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            inputBuffers = mediaCodec.getInputBuffers();
            outputBuffers = mediaCodec.getOutputBuffers();

            if (isSaveFile) {
                String mRecordFileName = Environment.getExternalStorageDirectory().toString() + "/leAAC.aac";
                try {
                    fos = new FileOutputStream(mRecordFileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 关闭编码器
     */
    public void close() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
            if (isSaveFile) {
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * codec实现硬编码
     *
     * @param input 需要编码的pcm数据
     * @return output 编译完成的aac数据
     */
    public synchronized byte[] offerEncord(byte[] input) {

        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            int outBitsSize = bufferInfo.size;
            int outPacketSize = outBitsSize + 7; // 7 is ADTS size
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + outBitsSize);

            //byte[] outData = new byte[outPacketSize];
            byte[] outData = new byte[outPacketSize];
            addADTStoPacket(outData, outPacketSize);

            //outputBuffer.get(outData);
            outputBuffer.get(outData, 7, outBitsSize);
            outputBuffer.position(bufferInfo.offset);
            try {
                outputStream.write(outData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        byte[] ret = outputStream.toByteArray();
        outputStream.reset();
        if (isSaveFile) {
            try {
                fos.write(ret);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return ret;
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet. This is
     * needed as MediaCodec encoder generates a packet of raw AAC data.
     * <p>
     * Note the packetLen must count in the ADTS header itself.
     **/
    public void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        // 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        // fill in ADTS data
        /**
         adts_fixed_header(){
         syncword; 					12 	bslbf	同步头 总是0xFFF, all bits must be 1，代表着一个ADTS帧的开始
         ID; 						1 	bslbf	MPEG Version: 0 for MPEG-4, 1 for MPEG-2
         layer;						2 	uimsbf	always: '00'
         protection_absent;			1 	bslbf

         profile;					2 	uimsbf
         sampling_frequency_index;	4 	uimsbf
         private_bit;				1 	bslbf
         channel_configuration;		3 	uimsbf
         original_copy;				1 	bslbf
         home;						1 	bslbf
         }
         */
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


}
