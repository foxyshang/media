/*
 * Copyright 2013 Google Inc. All rights reserved.
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

package cn.embed.media.surfaceEncoder.video;

import android.content.Context;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.embed.codec.IVideoEncoder;
import cn.embed.codec.LiveVideoEncoderCore;
import cn.embed.codec.VideoEncoderCore;
import cn.embed.media.surfaceEncoder.filter.CameraFilter;
import cn.embed.media.surfaceEncoder.filter.FilterGroup;
import cn.embed.media.surfaceEncoder.filter.FilterTypeBean;
import cn.embed.media.surfaceEncoder.filter.IFilter;
import cn.embed.media.surfaceEncoder.gles.FullFrameRect;
import cn.embed.renderer.TextureTrans;

/**
 * Encode a movie from frames rendered from an external texture image.
 * 通过外面纹理图像渲染来编码视频
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * 对象封装了一个编码器运行在一个专有的线程，不同的控制命令可以被发送给，丛各种线程。编码器线程管理两种编码。
 * 唯一的外部输入源是GL纹理
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * <p>
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object        //创建对象
 * <li>create an EncoderConfig                  //创建编码器配置信息
 * <li>call TextureMovieEncoder#startRecording() with the config      //调用
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 */
public class TextureMovieEncoder implements Runnable {
    private static final String TAG = "TextureMovieEncoder";
    private static final int MSG_START_RECORDING = 0;       //开始recording
    private static final int MSG_STOP_RECORDING = 1;        //停止recording
    private static final int MSG_SCALE_MVP_MATRIX = 2;  //缩放矩阵
    private static final int MSG_FRAME_AVAILABLE = 3;   //
    private static final int MSG_SET_TEXTURE_ID = 4;    //set texture id
    private static final int MSG_UPDATE_SHARED_CONTEXT = 6; //update context
    private static final int MSG_UPDATE_FILTER = 7;         //更新filter
    private static final int MSG_QUIT = 8;                  //quit

    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private FullFrameRect mFullScreen;
    private int mTextureId;
    private IVideoEncoder mVideoEncoder;
    private IFilter mCurrentFilterType;
    private boolean isFilterChange = false;


    private volatile EncoderHandler mHandler;
    private final Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private Context mContext;
    private volatile static TextureMovieEncoder sInstance;
    private List<FilterTypeBean> filterTypeBeanList;

    EncoderConfig config;

    //初始化，获取单例
    public static void initialize(Context applicationContext) {
        if (sInstance == null) {
            synchronized (TextureMovieEncoder.class) {
                if (sInstance == null) {
                    sInstance = new TextureMovieEncoder(applicationContext);
                }
            }
        }
    }

    public static TextureMovieEncoder getInstance() {
        return sInstance;
    }

    private TextureMovieEncoder(Context applicationContext) {
        mContext = applicationContext;
        filterTypeBeanList = new ArrayList<>();
    }


    public void startRecording(EncoderConfig config) {
        synchronized (mReadyFence) {
            if (mRunning) {
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                }
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * 缩放矩阵
     */
    public void scaleMVPMatrix(TextureTrans textureTrans) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SCALE_MVP_MATRIX, textureTrans));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void initFilter(IFilter filterType) {
        mCurrentFilterType = filterType;
        mCurrentFilterType.init();
    }

    public void updateFilter(IFilter filterType) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_FILTER, filterType));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(float[] texMatrix, long timestamp) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (timestamp == 0) {
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, (int) (timestamp >> 32),
                (int) timestamp, texMatrix));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;
            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                //开始硬编码
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                //结束硬编码
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                //计算mvp矩阵
                case MSG_SCALE_MVP_MATRIX:
                    encoder.handleSaleMVPMatrix((TextureTrans) obj);
                    break;
                //有效的帧
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) | (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                //设置纹理ID
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                //
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                //更新滤镜
                case MSG_UPDATE_FILTER:
                    encoder.handleUpdateFilter((IFilter) inputMessage.obj);
                    break;
                //退出消息
                case MSG_QUIT:
                    Looper looper = Looper.myLooper();
                    if (looper != null) {
                        looper.quit();
                    }
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Starts recording.
     * 开始编码，设置编码器配置参数
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        this.config = config;
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate, config.mOutputFile);
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     *
     * @param transform      The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(float[] transform, long timestampNanos) {


        if (isFilterChange || mFullScreen == null || mCurrentFilterType == null) {
            IFilter filterGroup = getFilterGroup();
            filterGroup.init();
            mFullScreen.changeProgram(filterGroup);
            mCurrentFilterType = filterGroup;
            mFullScreen.getFilter().setTextureSize(config.mWidth, config.mHeight);
        }


        mVideoEncoder.drainEncoder(false);
        //   mFullScreen.getFilter().setTextureSize(config.mWidth, config.mHeight);

        mFullScreen.drawFrame(mTextureId, transform);
        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    private void handleSaleMVPMatrix(TextureTrans textureTrans) {
        //  mFullScreen.getFilter().setTextureSize(config.mWidth, config.mHeight);
        mFullScreen.scaleMVPMatrix(1, 1);
        mFullScreen.scaleM(textureTrans.getScaleX(), textureTrans.getScaleY());
        mFullScreen.translateM(textureTrans.getTransX(), textureTrans.getTransY());
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        mTextureId = id;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mFullScreen.release(false);
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();


        // Create new programs and such for the new context.
        mFullScreen = new FullFrameRect(mCurrentFilterType);
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate, File file) {
        try {
            if (file == null) {
                mVideoEncoder = new LiveVideoEncoderCore(width, height, bitRate, file);
            } else {
                mVideoEncoder = new VideoEncoderCore(width, height, bitRate, file);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }


        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();
        IFilter filterGroup = getFilterGroup();
        mCurrentFilterType = filterGroup;
        mCurrentFilterType.init();
        mFullScreen = new FullFrameRect(mCurrentFilterType);
        mFullScreen.getFilter().setTextureSize(config.mWidth, config.mHeight);

    }

    private void handleUpdateFilter(IFilter filterType) {
        if (mFullScreen != null /*&& filterType != mCurrentFilterType*/) {
            filterType.init();
            mFullScreen.changeProgram(filterType);
            mCurrentFilterType = filterType;
        }
    }

    private void releaseEncoder() {
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);
            mFullScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

    }


    public void addFilter(FilterTypeBean filterTypeBean) {
        for (Iterator<FilterTypeBean> it = filterTypeBeanList.iterator(); it.hasNext(); ) {
            FilterTypeBean filterType = it.next();
            if (filterType.getFilterType() == filterTypeBean.getFilterType()) {
                it.remove();
            }
        }
        filterTypeBeanList.add(filterTypeBean);
        isFilterChange = true;
    }

    public void cleanFilter(int type) {

        for (Iterator<FilterTypeBean> it = filterTypeBeanList.iterator(); it.hasNext(); ) {
            FilterTypeBean filterType = it.next();
            if (filterType.getFilterType() == type) {
                it.remove();
            }
        }

        isFilterChange = true;
    }


    public void setFilterTypeBeanList(List<FilterTypeBean> filterTypeBeanList) {
        this.filterTypeBeanList = filterTypeBeanList;
        isFilterChange = true;
    }

    @NonNull
    public IFilter getFilterGroup() {

        List<IFilter> filters = new ArrayList<>();
        filters.add(new CameraFilter(mContext));
        if (filterTypeBeanList.size() > 0) {
            for (FilterTypeBean filterTypeBean : filterTypeBeanList) {
                filters.add(filterTypeBean.getFilter(mContext));
            }
        }
        FilterGroup<IFilter> filterGroup = new FilterGroup(filters);

        isFilterChange = false;
        return filterGroup;
    }

}
