package cn.embed.media.surfaceEncoder.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;

import cn.embed.media.surfaceEncoder.camera.CameraController;
import cn.embed.media.surfaceEncoder.camera.CameraHelper;
import cn.embed.renderer.CameraRecordRenderer;
import cn.embed.media.surfaceEncoder.camera.CommonHandlerListener;


public class CameraSurfaceView extends AutoFitGLSurfaceView implements CommonHandlerListener, SurfaceTexture.OnFrameAvailableListener {

    private CameraHandler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private CameraRecordRenderer mCameraRenderer;
    protected boolean mIsCameraBackForward = false;
    protected Context mContext;

    public CameraSurfaceView(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mHandlerThread = new HandlerThread("CameraHandlerThread");
        mHandlerThread.start();
        mBackgroundHandler = new CameraHandler(mHandlerThread.getLooper(), this);
        mCameraRenderer = new CameraRecordRenderer(context.getApplicationContext(), mBackgroundHandler, CameraRecordRenderer.RENDERER_TYPE_CAMERA);
        setRenderer(mCameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);           //设置为脏数据模式
    }


    @Override
    public void onPause() {
        mBackgroundHandler.removeCallbacksAndMessages(null);
        CameraController.getInstance().release();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraRenderer.notifyPausing();
                CameraController.getInstance().stopCamera();
            }
        });
        super.onPause();
    }

    public void onDestroy() {
        mBackgroundHandler.removeCallbacksAndMessages(null);
        if (!mHandlerThread.isInterrupted()) {
            try {
                mHandlerThread.quit();
                mHandlerThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }


    public synchronized void resetCamera(boolean isCameraBackForward) {
        mIsCameraBackForward = isCameraBackForward;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mBackgroundHandler.sendEmptyMessage(CameraHandler.STOP_CAMERA_PREVIEW);
                mBackgroundHandler.sendMessage(mBackgroundHandler.obtainMessage(
                        CameraHandler.SETUP_CAMERA
                        , mCameraRenderer.getmSurfaceWidth()
                        , mCameraRenderer.getmSurfaceHeight()
                        , mCameraRenderer.getmSurfaceTexture()));
            }
        });

    }


    public synchronized void switchCamera() {
        mIsCameraBackForward = !mIsCameraBackForward;
        resetCamera(mIsCameraBackForward);
    }


    public synchronized boolean setFlashLightMode(String mode) {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return false;
        }
        if (!mIsCameraBackForward) {
            return false;
        }
        Camera.Parameters parameters = CameraController.getInstance().getCameraParameters();
        if (parameters == null)
            return false;
        try {
            if (!parameters.getSupportedFlashModes().contains(mode)) {
                return false;
            }
            parameters.setFlashMode(mode);
            CameraController.getInstance().setCameraParameters(parameters);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    public static class CameraHandler extends Handler {
        public static final int SETUP_CAMERA = 1001;
        public static final int CONFIGURE_CAMERA = 1002;
        public static final int START_CAMERA_PREVIEW = 1003;
        public static final int STOP_CAMERA_PREVIEW = 1004;
        public static final int RESTART_CAMERA = 1006;        //重新启动摄像机
        public static final int SWITCH_CAMERA = 1007;            //切换摄像头
        public static final int SET_FLASH_LIGHT = 1008;        //设置闪光灯模式
        private CommonHandlerListener listener;

        public CameraHandler(Looper looper, CommonHandlerListener listener) {
            super(looper);
            this.listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            listener.handleMessage(msg);
        }
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            //设置相机
            case CameraHandler.SETUP_CAMERA: {
                final int width = msg.arg1;
                final int height = msg.arg2;
                final SurfaceTexture surfaceTexture = (SurfaceTexture) msg.obj;
                surfaceTexture.setOnFrameAvailableListener(this);

                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraController.getInstance().setupCamera(
                                surfaceTexture, getContext().getApplicationContext()
                                , width, mIsCameraBackForward);
                        mBackgroundHandler.sendMessage(mBackgroundHandler.obtainMessage(CameraHandler.CONFIGURE_CAMERA, width, height));
                    }
                });
            }
            break;
            //配置相机参数
            case CameraHandler.CONFIGURE_CAMERA: {
                final int width = msg.arg1;
                final int height = msg.arg2;
                Camera.Size previewSize = CameraHelper.getOptimalPreviewSize(CameraController.getInstance().getCameraParameters(), CameraController.getInstance().mCameraPictureSize, width);
                CameraController.getInstance().configureCameraParameters(previewSize);
                if (previewSize != null) {
                    mCameraRenderer.setCameraPreviewSize(previewSize.height, previewSize.width);
                }
                mBackgroundHandler.sendEmptyMessage(CameraHandler.START_CAMERA_PREVIEW);
            }
            break;
            //启动摄像机
            case CameraHandler.START_CAMERA_PREVIEW:
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraController.getInstance().startCameraPreview();
                    }
                });
                break;
            //停止相机预览
            case CameraHandler.STOP_CAMERA_PREVIEW:
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraController.getInstance().stopCameraPreview();
                    }
                });
                break;

            default:
                break;
        }
    }

    public CameraRecordRenderer getRenderer() {
        return mCameraRenderer;
    }

    public boolean isCameraBackForward() {
        return mIsCameraBackForward;
    }

    public CameraRecordRenderer getmCameraRenderer() {
        return mCameraRenderer;
    }
}