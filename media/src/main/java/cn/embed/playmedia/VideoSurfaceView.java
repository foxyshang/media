package cn.embed.playmedia;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.embed.renderer.CameraRecordRenderer;
import cn.embed.media.surfaceEncoder.widget.AutoFitGLSurfaceView;


public class VideoSurfaceView extends AutoFitGLSurfaceView implements SurfaceTexture.OnFrameAvailableListener, MediaPlayerWrapper.IMediaCallback {

    private CameraRecordRenderer mCameraRenderer;
    protected Context mContext;
    private MediaPlayerWrapper mMediaPlayer;
    /**
     * 视频播放状态的回调
     */
    private MediaPlayerWrapper.IMediaCallback callback;

    public VideoSurfaceView(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mCameraRenderer = new CameraRecordRenderer(context.getApplicationContext(), null, CameraRecordRenderer.RENDERER_TYPE_PLAYER);
        setRenderer(mCameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);           //设置为脏数据模式
        setPreserveEGLContextOnPause(false);
        setCameraDistance(100);


        mMediaPlayer = new MediaPlayerWrapper();
        mMediaPlayer.setOnCompletionListener(this);
        //不进行视频录制
        mCameraRenderer.setRecordingEnabled(false);
        setSquare(false);
    }

    /**
     * 设置视频的播放地址
     */
    public void setVideoPath(List<String> paths) {
        mMediaPlayer.setDataSource(paths);
    }

    public void playVideo(String path) {
        List<String> paths = new ArrayList<>();
        paths.add(path);
        mMediaPlayer.setDataSource(paths);
        playVideo();

    }

    public void playVideo() {
        SurfaceTexture surfaceTexture = mCameraRenderer.getmSurfaceTexture();
        surfaceTexture.setOnFrameAvailableListener(this);
        Surface surface = new Surface(surfaceTexture);
        mMediaPlayer.setSurface(surface);
        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.start();


    }


    @Override
    public void onPause() {
        super.onPause();
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }

    public void onDestroy() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();

    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }


    @Override
    public void onVideoPrepare() {
        if (callback != null) {
            callback.onVideoPrepare();
        }
    }

    @Override
    public void onVideoStart() {
        if (callback != null) {
            callback.onVideoStart();
        }
    }

    @Override
    public void onVideoPause() {
        if (callback != null) {
            callback.onVideoPause();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (callback != null) {
            callback.onCompletion(mp);
        }
    }


    /**
     * isPlaying now
     */
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    /**
     * pause play
     */
    public void pause() {
        mMediaPlayer.pause();
    }

    /**
     * start play video
     */
    public void start() {
        mMediaPlayer.start();
    }

    /**
     * 跳转到指定的时间点，只能跳到关键帧
     */
    public void seekTo(int time) {
        mMediaPlayer.seekTo(time);
    }

    @Override
    public void onVideoChanged(final VideoInfo info) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (info.rotation % 180 != 0) {
                    mCameraRenderer.setCameraPreviewSize(info.height, info.width);
                } else {
                    mCameraRenderer.setCameraPreviewSize(info.width, info.height);
                }


            }
        });
        if (callback != null) {
            callback.onVideoChanged(info);
        }
    }

    /**
     * 获取当前视频的长度
     */
    public int getVideoDuration() {
        return mMediaPlayer.getCurVideoDuration();
    }

    public void setIMediaCallback(MediaPlayerWrapper.IMediaCallback callback) {
        this.callback = callback;
    }

    public CameraRecordRenderer getmCameraRenderer() {
        return mCameraRenderer;
    }

    public MediaPlayerWrapper getmMediaPlayer() {
        return mMediaPlayer;
    }

    public void setmMediaPlayer(MediaPlayerWrapper mMediaPlayer) {
        this.mMediaPlayer = mMediaPlayer;
    }
}