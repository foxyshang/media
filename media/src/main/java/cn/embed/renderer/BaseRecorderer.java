package cn.embed.renderer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.embed.media.surfaceEncoder.filter.IFilter;
import cn.embed.media.surfaceEncoder.gles.FullFrameRect;
import cn.embed.media.surfaceEncoder.gles.GlUtil;

public class BaseRecorderer implements GLSurfaceView.Renderer {
    Context context = null;
    FullFrameRect mFullScreen;

    int mTextureId = GlUtil.NO_TEXTURE;
    SurfaceTexture mSurfaceTexture;
    Surface mSurface;
    final float[] mSTMatrix = new float[16];

    int mIncomingWidth, mIncomingHeight;
    int mSurfaceWidth, mSurfaceHeight;
    float mMvpScaleX = 1f, mMvpScaleY = 1f;
    GLSurfaceView.Renderer rendererListener;
    //滤镜相关
    IFilter mCurrentFilter;
    FilterManager filterManager;
    TextureTrans textureTrans;

    //纹理变换需求

    public void init(Context context) {
        this.context = context;
        filterManager = new FilterManager();
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Matrix.setIdentityM(mSTMatrix, 0);

        mCurrentFilter = filterManager.getFilter(context);
        mCurrentFilter.init();
        mFullScreen = new FullFrameRect(mCurrentFilter);
        mTextureId = mFullScreen.createTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurface = new Surface(mSurfaceTexture);

        if (rendererListener != null) {
            rendererListener.onSurfaceCreated(gl, config);
        }
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

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mSurfaceTexture.updateTexImage();

        if (filterManager.isFilterChange || mFullScreen == null || mCurrentFilter == null) {
            IFilter filterGroup = filterManager.getFilter(context);
            filterGroup.init();
            mFullScreen.changeProgram(filterGroup);
            mCurrentFilter = filterGroup;
        }

        mFullScreen.getFilter().setTextureSize(mSurfaceWidth, mSurfaceHeight);
        mSurfaceTexture.getTransformMatrix(mSTMatrix);

        mFullScreen.drawFrame(mTextureId, mSTMatrix);


        if (rendererListener != null) {
            rendererListener.onDrawFrame(gl);
        }

    }

    public void setRendererListener(GLSurfaceView.Renderer rendererListener) {
        this.rendererListener = rendererListener;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }

    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = filterManager;
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

        }
    }






}
