/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.embed.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import cn.embed.media.manager.LiveCameraStreamingManager;


/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
public class LiveVideoEncoderCore implements IVideoEncoder {
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = false;

    // TODO: these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25;               // 30fps
    private static final int IFRAME_INTERVAL = 3;           // 5 seconds between I-frames

    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    private boolean isSaveFile = false;

    //TODO
    byte[] spsPpsInfo = null;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    //TODO
    public static FileOutputStream fos;

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public LiveVideoEncoderCore(int width, int height, int bitRate, File file)
            throws IOException {
        //TODO
        if (isSaveFile) {
            String mRecordFileName = Environment.getExternalStorageDirectory().toString() + "/huawei.h264";
            try {
                fos = new FileOutputStream(mRecordFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (isSaveFile) {
            try {
                fos.flush();
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");
        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = encoderOutputBuffers[outputBufferIndex];
            byte[] outData = new byte[mBufferInfo.size];
            outputBuffer.get(outData);
            if (spsPpsInfo == null) {
                ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                if (spsPpsBuffer.getInt() == 0x00000001) {
                    spsPpsInfo = new byte[outData.length];
                    System.arraycopy(outData, 0, spsPpsInfo, 0, outData.length);
                } else {
                    return;
                }
            } else {
                try {
                    outputStream.write(outData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mEncoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            byte[] ret11 = outputStream.toByteArray();
//            Log.i("byte", Arrays.toString(ret11));

        }
        byte[] ret = outputStream.toByteArray();

        if (ret.length > 5 && (ret[4] & 0x0f) == 0x05) //key frame need to add sps pps
        {

            try {
                outputStream.reset();
                outputStream.write(spsPpsInfo);
                outputStream.write(ret);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //TODO
        byte[] ret1 = outputStream.toByteArray();
        outputStream.reset();
        if (isSaveFile) {
            try {
                if (ret.length > 4) {
                    fos.write(ret1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (LiveCameraStreamingManager.isStreaming() && (LiveCameraStreamingManager.sendDataQueue.size() < 100)) {
            synchronized (LiveCameraStreamingManager.sendDataQueue) {
                if (ret1.length > 4) {

                    LiveCameraStreamingManager.sendDataQueue.offer(ret1);
                }
            }

        }

    }
}
