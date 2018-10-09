package cn.embed.playmedia;

import android.app.Service;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;


import java.io.IOException;

import cn.embed.utils.CommonUtils;
import cn.embed.utils.Size;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by shangdongzhou on 2017/9/25.
 */

public class TextureViewPlayer implements TextureView.SurfaceTextureListener {
    public static final int TOP_CROP = 0;               //从视频的上部裁减，填充慢视频
    public static final int FIT_XY = 1;                 //将内容放大或缩小至控件大小 view大小不变
    public static final int CENTER_INSIDE = 2;          //将内容完全居中显示
    private TextureView textureView;
    private MediaPlayer mediaPlayer;
    private Surface mSurface;
    private Context context;
    private boolean isSettedPlay = false;
    private String videoPath;
    private Size videoSize;                 //视频大小
    private int degree;                     //视频角度
    boolean isPlaying = false;

    private int resId = -1;

    private int inputType = CENTER_INSIDE;

    private int viewWidth;
    private int viewHeight;
    private int videoType;
    public static final int PLAY_RESID_VIDEO = 0;           //播放
    public static final int PLAY_PATH_VIDEO = 1;


    public TextureViewPlayer(TextureView textureView) {
        this.context = textureView.getContext();
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
        inputType = CENTER_INSIDE;
    }

    /**
     * 视频播放器构造方式
     *
     * @param textureView
     * @param inputType   TOP_CROP = 0;               //从视频的上部裁减，填充慢视频
     *                    FIT_XY = 1;                 //将内容放大或缩小至控件大小 view大小不变
     *                    CENTER_INSIDE = 2;          //将内容完全居中显示
     */
    public TextureViewPlayer(TextureView textureView, int inputType) {
        this.context = textureView.getContext();
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
        this.inputType = inputType;

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        mSurface = new Surface(surface);
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        if (isSettedPlay) {
            switch (videoType) {
                case PLAY_RESID_VIDEO:
                    play(resId);
                    break;
                case PLAY_PATH_VIDEO:
                    play(videoPath);
                    break;
            }

        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stop();
        release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {


    }

    public void play(final String url) {
        videoType = PLAY_PATH_VIDEO;
        if (isPlaying) {
            if (url.equals(videoPath) && !isSettedPlay) {
                return;
            }
        }
        videoPath = url;
        if (mediaPlayer == null) {
            if (textureView.isAvailable()) {
                mediaPlayer = new MediaPlayer();
            } else {
                isSettedPlay = true;
                return;
            }
        }


        isSettedPlay = false;
        if (CommonUtils.isEmpty(url)) {
            return;
        }

        Observable<Size> observable = Observable.create(new ObservableOnSubscribe<Size>() {
            @Override
            public void subscribe(final ObservableEmitter<Size> emitter) throws Exception {
                try {

                    isPlaying = true;
                    mediaPlayer.reset();
                    mediaPlayer.setSurface(mSurface);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setScreenOnWhilePlaying(true);
                    if (url.contains("http:")) {
                        mediaPlayer.setDataSource(context, Uri.parse(url));
                    } else {
                        mediaPlayer.setDataSource(url);
                    }

                    try {
                        mediaPlayer.setLooping(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //  mediaPlayer.prepare();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            //准备完成后播放
                            mediaPlayer.start();
                            videoSize = new Size(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                            emitter.onNext(videoSize);
                        }
                    });

                    mediaPlayer.prepareAsync();

                } catch (IOException e) {
                    isPlaying = false;
                    e.printStackTrace();
                }
            }
        });

        Consumer<Size> consumer = new Consumer<Size>() {
            @Override
            public void accept(Size size) throws Exception {
                switch (inputType) {
                    case TOP_CROP:
                        if (mediaPlayer != null) {
                            topCrop(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                        }
                        break;
                    case FIT_XY:
                        break;
                    case CENTER_INSIDE:
                        if (mediaPlayer != null) {
                            adjustAspectRatio(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                        }
                        break;
                }
            }
        };


        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(consumer);


    }

    /**
     * 播放资源文件
     *
     * @param resId
     */
    public void play(final int resId) {
        videoType = PLAY_RESID_VIDEO;

        if (resId == this.resId && !isSettedPlay) {
            return;
        }
        this.resId = resId;
        this.videoPath = "";
        if (mediaPlayer == null) {
            if (textureView.isAvailable()) {
                mediaPlayer = new MediaPlayer();
            } else {
                isSettedPlay = true;
                return;
            }
        }
        isSettedPlay = false;

        Observable<Size> observable = Observable.create(new ObservableOnSubscribe<Size>() {
            @Override
            public void subscribe(final ObservableEmitter<Size> emitter) throws Exception {
                try {
                    AssetFileDescriptor file = textureView.getContext().getResources().openRawResourceFd(resId);
                    isPlaying = true;
                    mediaPlayer.reset();
                    mediaPlayer.setSurface(mSurface);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setScreenOnWhilePlaying(true);
                    mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(),
                            file.getLength());

                    //mediaPlayer.setLooping(true);
                    //  mediaPlayer.prepare();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            //准备完成后播放
                            mediaPlayer.start();
                            videoSize = new Size(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                            emitter.onNext(videoSize);
                        }
                    });
                    mediaPlayer.prepareAsync();

                } catch (IOException e) {
                    isPlaying = false;
                    e.printStackTrace();
                }
            }
        });

        Consumer<Size> consumer = new Consumer<Size>() {
            @Override
            public void accept(Size size) throws Exception {
                switch (inputType) {
                    case TOP_CROP:
                        topCrop(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                        break;
                    case FIT_XY:
                        break;
                    case CENTER_INSIDE:
                        adjustAspectRatio(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                        break;
                }
            }
        };


        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(consumer);


    }


    public void stop() {
        isPlaying = false;
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void closeVolume() {
        mediaPlayer.setVolume(0, 0);
    }

    public void OpenVolume() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
        float max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(current / max, current / max);
    }

    public void release() {
        isSettedPlay = false;
        videoPath = "";
    }

    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     * 按照原比例显示
     */
    public void adjustAspectRatio(int videoWidth, int videoHeight) {
       /* int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Matrix txform = new Matrix();
        textureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        textureView.setTransform(txform);*/
        int mSurfaceWidth = textureView.getWidth();
        int mSurfaceHeight = textureView.getHeight();
        setPreviewSize(videoWidth, videoHeight, mSurfaceWidth, mSurfaceHeight);
    }


    public void adjustAspectRatio(Size size, int videoWidth, int videoHeight) {
       /* int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Matrix txform = new Matrix();
        textureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        textureView.setTransform(txform);*/
        int mSurfaceWidth = size.getWidth();
        int mSurfaceHeight = size.getHeight();
        setPreviewSize(videoWidth, videoHeight, mSurfaceWidth, mSurfaceHeight);
    }

    public void setPreviewSize(int width, int height, int mSurfaceWidth, int mSurfaceHeight) {
        int mIncomingWidth = width;
        int mIncomingHeight = height;


        float scaleHeight = mSurfaceWidth / (mIncomingWidth * 1f / mIncomingHeight * 1f);
        float surfaceHeight = mSurfaceHeight;


        Matrix txform = new Matrix();

        float mMvpScaleX = 1f;
        float mMvpScaleY = scaleHeight / surfaceHeight;
        txform.setScale(1, 1);


        if (mMvpScaleY > 1.0f) {
            txform.setScale(mMvpScaleX, mMvpScaleY);
            txform.postTranslate(0, -(mMvpScaleY - 1) / mMvpScaleY);
        } else {
            txform.setScale(1f / mMvpScaleY, 1.0f);
        }
        textureView.setTransform(txform);
    }


    public Size getVideoSize() {
        return videoSize;
    }

    public int getDegree() {
        return degree;
    }

    public void play() {
        switch (videoType) {
            case PLAY_RESID_VIDEO:
                if (resId != -1) {
                    play(resId);
                }
                break;
            case PLAY_PATH_VIDEO:
                if (CommonUtils.isNotEmpty(videoPath) && !isPlaying) {
                    play(videoPath);
                }
                break;
        }

    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;

        }
    }

    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
        }
    }

    private void topCrop(int videoWidth, int videoHeight) {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        //
        double aspectRatio = (double) videoHeight / videoWidth;
        if (aspectRatio < 1) {
            adjustAspectRatio(videoWidth, videoHeight);
            return;
        }

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }

        Matrix txform = new Matrix();
        textureView.getTransform(txform);
        //  txform.setScale(1, (float) viewHeight / newHeight);
        txform.setScale(1, (float) aspectRatio);
        textureView.setTransform(txform);

    }

    public int getViewWidth() {
        return viewWidth;
    }

    public void setViewWidth(int viewWidth) {
        this.viewWidth = viewWidth;
    }

    public int getViewHeight() {
        return viewHeight;
    }

    public void setViewHeight(int viewHeight) {
        this.viewHeight = viewHeight;
    }
}
