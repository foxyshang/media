package cn.embed.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.embed.media.surfaceEncoder.filter.CameraFilter;
import cn.embed.media.surfaceEncoder.filter.FilterGroup;
import cn.embed.media.surfaceEncoder.filter.FilterTypeBean;
import cn.embed.media.surfaceEncoder.filter.IFilter;
import cn.embed.media.surfaceEncoder.gles.FullFrameRect;
import cn.embed.media.surfaceEncoder.gles.GlUtil;
import cn.embed.media.surfaceEncoder.video.EncoderConfig;
import cn.embed.media.surfaceEncoder.video.TextureMovieEncoder;
import cn.embed.media.surfaceEncoder.widget.CameraSurfaceView;

/**
 * GLSurfaceView 的渲染类
 * 1、view完毕后打开摄像机
 * 2、将摄像机的textureid 传过来
 * 3、在onDrawFrame 将textureID的内容渲染到GLSurfaceView上，同时渲染到encodec上，完成编码
 */

public class CameraRecordRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRecordRenderer";

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    public static final int RENDERER_TYPE_CAMERA = 0;                   //摄像头录制视频
    public static final int RENDERER_TYPE_PLAYER = 1;                   //视频播放器播放
    public static final int RENDERER_TYPE_VIDEO_EDIT = 2;               //视频编辑
    private int type;

    private final Context mApplicationContext;
    private final CameraSurfaceView.CameraHandler mCameraHandler;
    private int mTextureId = GlUtil.NO_TEXTURE;
    private FullFrameRect mFullScreen;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private final float[] mSTMatrix = new float[16];

    private List<FilterTypeBean> filterTypeBeanList;
    private IFilter mCurrentFilterType;
    private boolean isFilterChange = false;


    private TextureMovieEncoder mVideoEncoder;

    private boolean mRecordingEnabled = false;
    private int mRecordingStatus;
    private EncoderConfig mEncoderConfig;
    private float mMvpScaleX = 1f, mMvpScaleY = 1f;
    private int mSurfaceWidth, mSurfaceHeight;
    private int mIncomingWidth, mIncomingHeight;
    private GLSurfaceView.Renderer rendererListener;
    private TextureTrans textureTrans;

    private boolean isTakePicture = false;
    GLBitmapListener glBitmapListener;


    public CameraRecordRenderer(Context applicationContext, CameraSurfaceView.CameraHandler cameraHandler, int type) {
        mApplicationContext = applicationContext;
        mCameraHandler = cameraHandler;
        isFilterChange = false;
        mVideoEncoder = TextureMovieEncoder.getInstance();
        this.type = type;
        filterTypeBeanList = new ArrayList<>();

    }


    public void setEncoderConfig(EncoderConfig encoderConfig) {
        mEncoderConfig = encoderConfig;
    }

    public void setRecordingEnabled(boolean recordingEnabled) {
        mRecordingEnabled = recordingEnabled;
    }

    public void setCameraPreviewSize(int width, int height) {
        mIncomingWidth = width;
        mIncomingHeight = height;
        if (textureTrans == null) {
            setPreviewSize(mIncomingWidth, mIncomingHeight);
        } else {
            mFullScreen.scaleMVPMatrix(1, 1);
            mFullScreen.scaleM(textureTrans.getScaleX(), textureTrans.getScaleY());
            mFullScreen.translateM(textureTrans.getTransX(), textureTrans.getTransY());
        }

    }


    public void setPreviewSize(int width, int height) {
        this.mIncomingWidth = width;
        this.mIncomingHeight = height;

        float scaleHeight = mSurfaceWidth / (mIncomingWidth * 1f / mIncomingHeight * 1f);
        float surfaceHeight = mSurfaceHeight;

        Log.d(TAG, "mSurfaceWidth:" + mSurfaceWidth + "mSurfaceHeight:" + mSurfaceHeight + "mIncomingWidth:" + mIncomingWidth + "mIncomingHeight:" + mIncomingHeight);

        if (mFullScreen != null) {
            mMvpScaleX = 1f;
            mMvpScaleY = scaleHeight / surfaceHeight;
            mFullScreen.scaleMVPMatrix(1, 1);


            if (mMvpScaleY > 1.0f) {
                mFullScreen.scaleMVPMatrix(mMvpScaleX, mMvpScaleY);
                mFullScreen.translateM(0, -(mMvpScaleY - 1) / mMvpScaleY);
                textureTrans = new TextureTrans(mMvpScaleX, mMvpScaleY, 0, -(mMvpScaleY - 1) / mMvpScaleY);
            } else {
                mFullScreen.scaleMVPMatrix(1f / mMvpScaleY, 1.0f);
                textureTrans = new TextureTrans(1f / mMvpScaleY, 1.0f, 0, 0);
            }
            // mFullScreen.translateM((-mMvpScaleX + 1.0f) / 2.0f, (-mMvpScaleY + 1.0f) / 2.0f);


        }
        Log.d("foxyshang", "setCameraPreviewSize:" + "mMvpScaleY_" + mMvpScaleY);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        IFilter filterGroup = getFilterGroup();
        mCurrentFilterType = filterGroup;
        mCurrentFilterType.init();
        Matrix.setIdentityM(mSTMatrix, 0);
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;

        }
        mFullScreen = new FullFrameRect(mCurrentFilterType);
        mTextureId = mFullScreen.createTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        if (rendererListener != null) {
            rendererListener.onSurfaceCreated(gl, config);
        }
        mSurface = new Surface(mSurfaceTexture);
    }

    @NonNull
    public IFilter getFilterGroup() {

        List<IFilter> filters = new ArrayList<>();
        filters.add(new CameraFilter(mApplicationContext));
        if (filterTypeBeanList.size() > 0) {
            for (FilterTypeBean filterTypeBean : filterTypeBeanList) {
                filters.add(filterTypeBean.getFilter(mApplicationContext));
            }
        }
        FilterGroup<IFilter> filterGroup = new FilterGroup(filters);

        isFilterChange = false;
        return filterGroup;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        mSurfaceWidth = width;
        mSurfaceHeight = height;

        if (gl != null) {
            gl.glViewport(0, 0, width, height);
        }

        if (rendererListener != null) {
            rendererListener.onSurfaceChanged(gl, width, height);
        }
        //开启摄像机
        if (mCameraHandler == null) {
            return;
        }
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraSurfaceView.CameraHandler.SETUP_CAMERA, width, height, mSurfaceTexture));


    }

    @Override
    public void onDrawFrame(final GL10 gl) {

        /*if (type == RENDERER_TYPE_VIDEO_EDIT){
            videoOnDrawFrame(mTextureId, mSTMatrix, lastEncoderTimestamp);
            return;
        }*/

        //更新缓存文件
        mSurfaceTexture.updateTexImage();
        //TODO
        if (isFilterChange || mFullScreen == null || mCurrentFilterType == null) {
            IFilter filterGroup = getFilterGroup();
            filterGroup.init();
            mFullScreen.changeProgram(filterGroup);
            mCurrentFilterType = filterGroup;
            //mFullScreen.getFilter().setTextureSize(mSurfaceWidth, mSurfaceHeight);
        }
        mFullScreen.getFilter().setTextureSize(mSurfaceWidth, mSurfaceHeight);

        mSurfaceTexture.getTransformMatrix(mSTMatrix);

        if (type != RENDERER_TYPE_VIDEO_EDIT) {
            mFullScreen.drawFrame(mTextureId, mSTMatrix);
        }


        if (type != RENDERER_TYPE_PLAYER) {
            videoOnDrawFrame(mTextureId, mSTMatrix, mSurfaceTexture.getTimestamp());// mSurfaceTexture.getTimestamp());
        }
        if (isTakePicture) {
            isTakePicture = false;

            final Bitmap bmp = createBitmapFromGLSurface(0, 0, mSurfaceWidth, mSurfaceHeight, gl);
            if (glBitmapListener != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        glBitmapListener.onBitmap(bmp);
                    }
                }).start();

            }

        }

        if (rendererListener != null) {
            rendererListener.onDrawFrame(gl);
        }
    }

    private void videoOnDrawFrame(int textureId, float[] texMatrix, long timestamp) {
        if (mRecordingEnabled && mEncoderConfig != null) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    mEncoderConfig.updateEglContext(EGL14.eglGetCurrentContext());
                    mVideoEncoder.startRecording(mEncoderConfig);           //开始录制视频
                    mVideoEncoder.setTextureId(textureId);                  //设置纹理id
                    if (textureTrans == null) {
                        mVideoEncoder.scaleMVPMatrix(new TextureTrans(1, 1, 0, 0));
                    } else {
                        mVideoEncoder.scaleMVPMatrix(textureTrans);
                    }
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mVideoEncoder.setTextureId(textureId);
                    if (textureTrans == null) {
                        mVideoEncoder.scaleMVPMatrix(new TextureTrans(1, 1, 0, 0));
                    } else {
                        mVideoEncoder.scaleMVPMatrix(textureTrans);
                    }
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }


        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }

    /*    if (recordFilter == null || isRecordFilterChange) {
            recordFilter = getFilterGroup();
            mVideoEncoder.updateFilter(recordFilter);
        }*/


        // mVideoEncoder.updateFilter(recordFilter);
        //   mVideoEncoder.updateFilter(new CameraFilter(mApplicationContext));
        // isRecordFilterChange = true;


        mVideoEncoder.frameAvailable(texMatrix, timestamp);
    }


    public void setrendererListener(GLSurfaceView.Renderer rendererListener) {
        this.rendererListener = rendererListener;
    }

    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);     // assume the GLSurfaceView EGL context is about
            mFullScreen = null;             // to be destroyed
        }
    }

    public void addFilter(FilterTypeBean filterTypeBean) {
          mVideoEncoder.addFilter(filterTypeBean);
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
        mVideoEncoder.cleanFilter(type);
        for (Iterator<FilterTypeBean> it = filterTypeBeanList.iterator(); it.hasNext(); ) {
            FilterTypeBean filterType = it.next();
            if (filterType.getFilterType() == type) {
                it.remove();
            }
        }

        isFilterChange = true;
    }

    public IFilter getFilter(int type) {
        for (Iterator<FilterTypeBean> it = filterTypeBeanList.iterator(); it.hasNext(); ) {
            FilterTypeBean filterType = it.next();
            if (filterType.getFilterType() == type) {
                return filterType.getThisFilter();
            }
        }
        return null;

    }


    public void setFilterTypeBeanList(List<FilterTypeBean> filterTypeBeanList) {
        mVideoEncoder.setFilterTypeBeanList(filterTypeBeanList);
        this.filterTypeBeanList = filterTypeBeanList;
        isFilterChange = true;
    }

    public SurfaceTexture getmSurfaceTexture() {
        return mSurfaceTexture;
    }

    public int getmSurfaceHeight() {
        return mSurfaceHeight;
    }

    public int getmSurfaceWidth() {
        return mSurfaceWidth;
    }


    public TextureTrans getTextureTrans() {
        return textureTrans;
    }

    public void setTextureTrans(TextureTrans textureTrans) {
        this.textureTrans = textureTrans;
    }

    /**
     * 获取surface
     *
     * @return
     */
    public Surface getmSurface() {
        return mSurface;
    }


    /**
     * 获取opengl的截图
     *
     * @param x  起点
     * @param y
     * @param w
     * @param h
     * @param gl
     * @return
     */
    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl) {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);

        intBuffer.position(0);
        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                    intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }


    public void getBitmap(GLBitmapListener glBitmapListener) {
        isTakePicture = true;
        this.glBitmapListener = glBitmapListener;
    }

    public static interface GLBitmapListener {
        void onBitmap(Bitmap bitmap);
    }
}
