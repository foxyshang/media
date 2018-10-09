package cn.embed.videohandle.mpeg;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import cn.embed.utils.GetMP4Info;
import cn.embed.videohandle.muxer.FeMuxer;
import cn.embed.videohandle.muxer.TrackIndex;


/**
 * Created by foxy on 2017/5/25.
 * 向文件中写入音频数据的线程
 */
public class WriteAudioTask implements Runnable {
    String TAG = "decoder audio";
    boolean VERBOSE = true;
    MediaExtractor extractor;
    int audioIndex;
    int audioMaxInputSize;
    boolean isWritedAudio;
    private TrackIndex trackIndexAudio;
    private FeMuxer mMuxer;
    private MovieEditOperate movieEditOperate;
    private List<SegmentConfig> segmentConfigs;
    private boolean isSameFile;
    private boolean isFormatConversion = false;         //需要转码
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec decoder = null;
    private AudioEncoder audioEncoder;
    private boolean isCancle=false;

    /**
     * 写入音频方法的构造函数
     *
     * @param extractor       mediaextractor   通过视频文件可得
     * @param mMuxer          文件中的最大帧大小，通过mediaformat可得
     * @param trackIndexAudio 写入的index音轨
     */
    public WriteAudioTask(MediaExtractor extractor, FeMuxer mMuxer, TrackIndex trackIndexAudio) {
        this.extractor = extractor;
        this.audioIndex = GetMP4Info.selectAudioTrack(extractor);
        try {
            this.audioMaxInputSize = GetMP4Info.getAudioFormat(extractor).getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } catch (Exception e) {
            this.audioMaxInputSize = 8192;
        }
        this.isWritedAudio = false;
        this.mMuxer = mMuxer;
        this.trackIndexAudio = trackIndexAudio;
        this.movieEditOperate = MovieEditOperate.getInstance();
        isFormatConversion = false;
        isCancle=false;
    }

    public WriteAudioTask(MediaExtractor extractor, FeMuxer mMuxer, TrackIndex trackIndexAudio, MediaCodec decoder, AudioEncoder audioEncoder) {
        this.extractor = extractor;
        this.audioIndex = GetMP4Info.selectAudioTrack(extractor);
        try {
            this.audioMaxInputSize = GetMP4Info.getAudioFormat(extractor).getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } catch (Exception e) {
            this.audioMaxInputSize = 8192;
        }
        this.isWritedAudio = false;
        this.mMuxer = mMuxer;
        this.trackIndexAudio = trackIndexAudio;
        this.movieEditOperate = MovieEditOperate.getInstance();
        isFormatConversion = true;
        this.decoder = decoder;
        this.audioEncoder = audioEncoder;
        isCancle=false;
    }

    /**
     * 设置片段的配置信息
     *
     * @param segmentConfigs
     * @param isSameFile
     */
    public void setSegmentConfigs(List<SegmentConfig> segmentConfigs, boolean isSameFile) {
        this.segmentConfigs = segmentConfigs;
        this.isSameFile = isSameFile;
    }

    @Override
    public void run() {
        this.addAudioStream(extractor);
    }

    /**
     * 向文件中写入音频流
     *
     * @param extractor
     */
    private void addAudioStream(MediaExtractor extractor) {
        extractor.selectTrack(audioIndex);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer inputBuf = ByteBuffer.allocate(audioMaxInputSize);
        long lasttime = 0;
        //格式不对，需要转换格式
        if (this.trackIndexAudio == null) {
            isFormatConversion = true;
        }
        //同文件的视频切割
        if (isSameFile) {
            for (int i = 0; i < segmentConfigs.size(); i++) {
                SegmentConfig segmentConfig = segmentConfigs.get(i);
                long starttime=((long)segmentConfig.getStartTime()) * 1000000L;
                extractor.seekTo(starttime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                while (!isWritedAudio) {
                    if (isCancle){
                        isWritedAudio = true;
                        mMuxer.closed(trackIndexAudio);
                        break;
                    }
                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    long endtime=segmentConfig.getEndTime() * 1000000L;
                    if ((segmentConfig.getEndTime() != -1) && extractor.getSampleTime() > endtime) {
                        if (i == segmentConfigs.size() - 1) {
                            isWritedAudio = true;
                            mMuxer.closed(trackIndexAudio);
                            break;
                        } else {
                            break;
                        }
                    } else {
                        if (sampleSize < 0) {
                            isWritedAudio = true;
                            mMuxer.closed(trackIndexAudio);
                        } else {
                            int flag=extractor.getSampleFlags();
                            info.offset = 0;
                            info.size = sampleSize;
                            info.flags = flag;
                            long sampleTime = extractor.getSampleTime();
                            long presentationTimeUs = extractor.getSampleTime() - getSegmentConfigStartTime(segmentConfigs, i) * 1000000L;
                            if (presentationTimeUs < 0) {
                                presentationTimeUs = 0;
                            }
                            if (presentationTimeUs >= lasttime) {
                                info.presentationTimeUs = presentationTimeUs;
                                mMuxer.writeSampleData(trackIndexAudio, inputBuf, info);
                                lasttime = presentationTimeUs;
                            }

                            extractor.advance();
                        }
                    }
                }
            }
        }
        //不同文件的音频切割
        else {
            SegmentConfig segmentConfig = segmentConfigs.get(0);
            long audioDuration = 0;         //音频写入的simple的时间戳
            long lastSample = 0;            //上次simple的时间
            MediaFormat audioInputFormat = null;
            MediaFormat audioOutputFormat = null;


            final int TIMEOUT_USEC = 10000;
            ByteBuffer[] decoderInputBuffers = null;
            ByteBuffer[] decoderOutputBuffers = null;
            long firstInputTimeNsec = -1;
            boolean outputDone = false;
            boolean inputDone = false;
            int outputChunk = 0;
            boolean isFirst=true;

            if (isFormatConversion) {
                decoderInputBuffers = decoder.getInputBuffers();
                decoderOutputBuffers = decoder.getOutputBuffers();
            }


            while (!isWritedAudio) {
                if (isCancle){
                    isWritedAudio = true;
                    mMuxer.closed(trackIndexAudio);
                    break;
                }
                if (isFirst){
                    extractor.seekTo(segmentConfig.getStartTime() * 1000000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    isFirst=false;
                }
                //不需要转码
                if (!isFormatConversion) {
                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    if (audioDuration > ((segmentConfig.getEndTime() - segmentConfig.getStartTime()) * 1000000L)) {
                        isWritedAudio = true;
                        mMuxer.closed(trackIndexAudio);
                        break;
                    } else {
                        if (sampleSize < 0) {
                            extractor.seekTo(segmentConfig.getStartTime() * 1000000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                        } else {
                            int flag=extractor.getSampleFlags();
                            info.offset = 0;
                            info.size = flag;
                            info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                            long sampleTime = extractor.getSampleTime() - getSegmentConfigStartTime(segmentConfigs, 0) * 1000000L;            //当前的值
                            if (sampleTime < 0) {
                                sampleTime = 0;
                            }
                            long difference = 0;
                            if (sampleTime > lastSample) {
                                difference = sampleTime - lastSample;
                            } else {
                                difference = sampleTime;
                            }
                            lastSample = sampleTime;
                            audioDuration += difference;
                            info.presentationTimeUs = audioDuration;
                            mMuxer.writeSampleData(trackIndexAudio, inputBuf, info);
                            extractor.advance();
                        }
                    }
                }
                //需要转码
                else {
                    if ((outputChunk - AudioEncoder.outputNumber) > 4) {
                        try {
                            Thread.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        extractor.selectTrack(this.audioIndex);
                        if (!inputDone) {
                            int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                            if (inputBufIndex >= 0) {
                                if (firstInputTimeNsec == -1) {
                                    firstInputTimeNsec = System.nanoTime();
                                }
                                ByteBuffer deInputBuf = decoderInputBuffers[inputBufIndex];

                                int chunkSize = extractor.readSampleData(deInputBuf, 0);
                                if (chunkSize < 0) {
                                    decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                } else {
                                    if (extractor.getSampleTrackIndex() != this.audioIndex) {
                                        Log.w(TAG, "WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", expected " + audioIndex);
                                    }
                                    long presentationTimeUs = extractor.getSampleTime();
                                    decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0 /*flags*/);
                                    extractor.advance();
                                }
                            } else {
                                if (VERBOSE) Log.d(TAG, "input buffer not available");
                            }
                        }
                        if (!outputDone) {
                            int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

                            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                if (VERBOSE) {
                                    Log.d(TAG, "no output from decoder available");
                                }
                            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                if (VERBOSE) {
                                    Log.d(TAG, "decoder output buffers changed");
                                }
                            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat newFormat = decoder.getOutputFormat();
                                if (VERBOSE) {
                                    Log.d(TAG, "decoder output format changed: " + newFormat);
                                }
                            } else if (decoderStatus < 0) {
                                throw new RuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                            } else {
                                if (firstInputTimeNsec != 0) {
                                    // Log the delay from the first buffer of input to the first buffer
                                    long nowNsec = System.nanoTime();
                                    Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                                    firstInputTimeNsec = 0;
                                }
                                if (VERBOSE) {
                                    Log.d(TAG, "surface decoder given buffer " + decoderStatus + " (size=" + mBufferInfo.size + ")");
                                }
                                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    if (VERBOSE) {
                                        Log.d(TAG, "output EOS");
                                    }
                                    outputDone = true;
                                    //  MainActivity.mHandler.sendEmptyMessage(MainActivity.STOP_RECOEDING);
                                    isWritedAudio = true;
                                    mMuxer.closed(trackIndexAudio);
                                    audioEncoder.close();

                                }

                                boolean doRender = (mBufferInfo.size != 0);

                                // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                                // to SurfaceTexture to convert to a texture.  We can't control when it
                                // appears on-screen, but we can manage the pace at which we release
                                // the buffers.

                                //   while (decoderStatus >= 0) {
                                ByteBuffer decodedData = decoderOutputBuffers[decoderStatus];
                                if (decodedData == null) {
                                    throw new RuntimeException("encoderOutputBuffer " + decoderStatus +
                                            " was null");
                                }

                                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    if (VERBOSE)
                                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                                    mBufferInfo.size = 0;
                                }

                                if (mBufferInfo.size != 0) {

                                    decodedData.position(mBufferInfo.offset);
                                    decodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                                    //    MovieEditOperate.mMuxer.writeSampleData(videoIndex, encodedData, mBufferInfo);
                                    audioEncoder.offerEncord(decodedData);
                                    outputChunk++;

                                    if (VERBOSE) {
                                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                                mBufferInfo.presentationTimeUs);
                                    }
                                }

                                decoder.releaseOutputBuffer(decoderStatus, doRender);
                                //  }


                                //    if (MainActivity.isEncoder) {
                                //  MainActivity.mHandler.sendEmptyMessage(MainActivity.ON_DRAW_FRAME);
                                //    }
                                if (VERBOSE) {
                                    Log.d(TAG, "decoder frame " + outputChunk + " to dec, size="
                                    );
                                }

                                //  decoder.releaseOutputBuffer(decoderStatus, doRender);


                            }
                        }
                    }


                }
            }


        }
        movieEditOperate.setAudioEnd();
    }

    /**
     * 或许时间间隔差
     *
     * @param segmentConfigs
     * @param index
     * @return
     */
    private int getSegmentConfigStartTime(List<SegmentConfig> segmentConfigs, int index) {
        int startTime = 0;
        for (int i = 0; i < index + 1; i++) {
            if (i == 0) {
                startTime = segmentConfigs.get(i).getStartTime();
            } else {
                startTime += segmentConfigs.get(i).getStartTime() - segmentConfigs.get(i - 1).getEndTime();
            }
        }
        return startTime;
    }

    @NonNull
    private MediaCodec getAndStartAudioMediaCodec(MediaExtractor extractor) throws IOException {
        MediaCodec decoder = null;
        MediaFormat format = GetMP4Info.getAudioFormat(extractor);
        String mime = format.getString(MediaFormat.KEY_MIME);
        decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(format, null, null, 0);
        decoder.start();
        return decoder;
    }

    public void setCancle(boolean isCancle){
        this.isCancle=isCancle;
    }

}
