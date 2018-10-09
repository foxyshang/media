package cn.embed.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.embed.media.surfaceEncoder.filter.FilterTypeBean;
import cn.embed.media.surfaceEncoder.filter.IFilter;
import cn.embed.media.surfaceEncoder.video.EncoderConfig;
import cn.embed.media.surfaceEncoder.video.TextureMovieEncoder;

/**
 * GLSurfaceView 的渲染类
 * 1、view完毕后打开摄像机
 * 2、将摄像机的textureid 传过来
 * 3、在onDrawFrame 将textureID的内容渲染到GLSurfaceView上，同时渲染到encodec上，完成编码
 */

public class VideoEditRenderer extends BaseRecorderer {
    private static final String TAG = "VideoEditRenderer";
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private TextureMovieEncoder mVideoEncoder;

    private boolean mRecordingEnabled = false;
    private int mRecordingStatus;
    private EncoderConfig mEncoderConfig;


    private boolean isTakePicture = false;
    CameraRecordRenderer.GLBitmapListener glBitmapListener;


    public VideoEditRenderer(Context applicationContext) {
        init(applicationContext);
        mVideoEncoder = TextureMovieEncoder.getInstance();


    }

    public void setEncoderConfig(EncoderConfig encoderConfig) {
        mEncoderConfig = encoderConfig;
    }

    public void setRecordingEnabled(boolean recordingEnabled) {
        mRecordingEnabled = recordingEnabled;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;

        }
    }


    @Override
    public void onDrawFrame(final GL10 gl) {

        super.onDrawFrame(gl);
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        videoOnDrawFrame(mTextureId, mSTMatrix, mSurfaceTexture.getTimestamp());
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
        this.glBitmapListener = (CameraRecordRenderer.GLBitmapListener) glBitmapListener;
    }

    public static interface GLBitmapListener {
        void onBitmap(Bitmap bitmap);
    }


    public IFilter getFilter(Context context) {
        return filterManager.getFilter(context);
    }


    public void addFilter(FilterTypeBean filterTypeBean) {
        mVideoEncoder.addFilter(filterTypeBean);
        filterManager.addFilter(filterTypeBean);
    }

    public void cleanFilter(int type) {
        mVideoEncoder.cleanFilter(type);
        filterManager.cleanFilterByType(type);
    }


}

