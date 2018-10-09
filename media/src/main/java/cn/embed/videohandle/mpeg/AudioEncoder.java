package cn.embed.videohandle.mpeg;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import cn.embed.videohandle.muxer.TrackIndex;

public class AudioEncoder {
    private final static String MINE_TYPE = "audio/mp4a-latm";
    private MediaCodec mediaCodec;
    private ByteBuffer[] inputBuffers = null;
    private ByteBuffer[] outputBuffers = null;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private int sampleRate = 44100;             //采样率
    private int bit_rate = 2 * 64 * 1024;       //码率
    private int channelCount=2;                 //通道数

    private static final boolean isSaveFile = false;
    private static FileOutputStream fos;

    private TrackIndex trackIndexAudio;
    public static  volatile int outputNumber;

    /**
     * 构造函数如果不设置则默认
     */
    public AudioEncoder() {
        initialize(this.sampleRate, this.bit_rate,this.channelCount);
    }

    /**
     * 构造函数
     * @param sampleRate
     * @param bit_rate
     * @param channelCount 采样通道
     */
    public AudioEncoder(int sampleRate, int bit_rate,int channelCount) {
        this.bit_rate = bit_rate;
        this.sampleRate = sampleRate;
        this.channelCount=channelCount;
        initialize(this.sampleRate, this.bit_rate ,this.channelCount);
    }

    //初始化编码器
    private void initialize(int sampleRate, int bit_rate,int channelCount) {
        outputNumber=0;
        try {
            mediaCodec = MediaCodec.createEncoderByType(MINE_TYPE);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MINE_TYPE, sampleRate, channelCount);
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
     * 获取meidaFormat
     */
    public MediaFormat getEncoderFormat(){
        return mediaCodec.getOutputFormat();
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

            byte[] outData = new byte[outPacketSize];
            addADTStoPacket(outData, outPacketSize);

            outputBuffer.get(outData, 7, outBitsSize);
            outputBuffer.position(bufferInfo.offset);

           /* MovieEditOperate.mMuxer.writeSampleData(trackIndexAudio,outputBuffer,bufferInfo);*/


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
     * codec实现硬编码
     *
     * @param inputBytebuffer 需要编码的pcm数据
     * @return output 编译完成的aac数据
     */
    public  void offerEncord(ByteBuffer inputBytebuffer) {
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(10000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(inputBytebuffer);
            inputBuffer.flip();
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, inputBytebuffer.limit(), 10000, 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            int outBitsSize = bufferInfo.size;
            int outPacketSize = outBitsSize + 7; // 7 is ADTS size
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + outBitsSize);

            byte[] outData = new byte[outPacketSize];
            addADTStoPacket(outData, outPacketSize);
            outputBuffer.get(outData, 7, outBitsSize);
            outputBuffer.position(bufferInfo.offset);
            bufferInfo.size=outputBuffer.limit();
            outputBuffer.flip();
          /*  MovieEditOperate.mMuxer.writeSampleData(trackIndexAudio,outputBuffer,bufferInfo);*/
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        outputNumber++;

    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet. This is
     * needed as MediaCodec encoder generates a packet of raw AAC data.
     * <p>
     * Note the packetLen must count in the ADTS header itself.
     **/
    public void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    public TrackIndex getTrackIndexAudio() {
        return trackIndexAudio;
    }

    public void setTrackIndexAudio(TrackIndex trackIndexAudio) {
        this.trackIndexAudio = trackIndexAudio;
    }
}
