package cn.embed.videohandle.mpeg;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import cn.embed.utils.CommonUtils;
import cn.embed.renderer.CameraRecordRenderer;
import cn.embed.renderer.TextureTrans;
import cn.embed.media.surfaceEncoder.filter.FilterTypeBean;
import cn.embed.media.surfaceEncoder.video.EglCore;
import cn.embed.media.surfaceEncoder.video.EglSurfaceBase;
import cn.embed.media.surfaceEncoder.video.EncoderConfig;
import cn.embed.utils.Size;
import cn.embed.utils.GetMP4Info;
import cn.embed.videohandle.muxer.TrackIndex;

import static cn.embed.codec.VideoEncoderCore.outputNumber;


/**
 * Created by foxy on 2017/5/31.
 * 视频编辑操作
 */
public class MovieEditOperate implements Runnable {
    String TAG = "foxy";
    private boolean VERBOSE = true;
    private volatile static MovieEditOperate sInstance;
    private Context mContext;
    private boolean mRunning;
    private volatile MovieHandler mHandler;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private static final int MAX_SLEEP_NUMBER = 500;
    private static final boolean SUCCESS = true;
    private static final boolean FAILURE = false;

    private static final int MSG_CREATE_GL = 1;                     //创建OPENGL 环境  视频硬解码时使用
    private static final int MSG_SET_VIDEO = 2;                     //设置要写入的video
    private static final int MSG_SET_AUDIO = 3;                     //设置要写入的声音
    private static final int MSG_START_WRITR_VIDEO = 4;             //开始写入video
    private static final int MSG_START_WRITE_AUDIO = 5;             //开始写入音频
    private static final int MSG_WRITE_VIDEO_END = 6;               //写入视频结束
    private static final int MSG_WRITE_AUDIO_END = 7;               //写入音频结束
    private static final int MSG_END = 8;                           //结束对视频的操作，可以是结束，也可以是中断
    private static final int MSG_SET_VIDEO_CONFIG = 9;              //设置视频输出配置
    private static final int MSG_START_HANDLE = 10;                 //开始处理音视频
    private static final int ON_DRAW_FRAME = 12;                    //编译帧
    private static final int STOP_RECOEDING = 11;                   //结束编码
    private static final int MSG_SEGMENT_CONFIG = 13;               //设置视频片段参数
    private static final int MSG_EDIT_MEDIA_COMPLETE = 14;          //文件处理完成
    private static final int MSG_COMPRESS_FAIL = 15;                //硬解码编码失败
    private static final int MSG_AUDIO_SEGMENT_CONFIGS = 16;        //设置音频的播放
    private static final int MSG_SET_NEED_COMPRESS = 17;            //设置
    private static final int MSG_CANCEL = 18;                       //取消操作


    private Surface surface;
    private CameraRecordRenderer mVideoRecordRender;                   //视频处理的需要
    private EncoderConfig encoderConfig = null;                     //视频输出参数设置
    private String videoPath;                                       //视频文件地址
    private String audioPath;                                       //音频数据
    //  public static FeMuxer mMuxer = null;
    private List<SegmentConfig> segmentConfigs;                     //视频分段操作的设置组
    private List<SegmentConfig> audioSegmentConfigs;                //音乐分段操作的配置

    public static TrackIndex audioTrackIndex;                       //
    MediaExtractor videoExtractor = null;                           //负责将源数据分离成音视频数据
    MediaExtractor audioExtractor = null;
    private long totalFrame;
    private ProgressListener progressListener = null;

    private int videoDuration;                                      //视频时长 单位秒，默认是为0，如果为-1，则表示视频文件的长度

    //视频编辑完成后释放
    EglSurfaceBase eglSurfaceBase;
    EglCore mEglCore;
    //是否需要压缩，1、如果输出码率大于输入码率，不需要压缩。2、如果手机不支持压缩，则直接进行裁剪
    private boolean NEED_COMPRESS = true;
    private String outputPath = "";
    private int videoQuality = MediaProfile.VIDEO_QUALITY_HIGH4;
    private int videoEncodingSize = MediaProfile.VIDEO_ENCODING_HEIGHT_720;

    private boolean isMp4a = true;                //传入的音频是否为aac的格式
    private MediaCodec audioDecoder;            //音频解码器
    private AudioEncoder audioEncoder;          //音频编码器

    private boolean isCancel = false;        //取消上传
    private static boolean isBusy = false;
    private List<FilterTypeBean> filterTypeBeanList;
    private boolean isFilterChange = false;

    private TextureTrans textureTrans = null;
    private Size videoRealSize;

    float videoAsp = -1;

    //初始化，获取单例
    public static void initialize(Context applicationContext) {
        isBusy = false;
        if (sInstance == null) {
            synchronized (MovieEditOperate.class) {
                if (sInstance == null) {
                    sInstance = new MovieEditOperate(applicationContext);
                }
            }
        }
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static MovieEditOperate getInstance() {
        return sInstance;
    }

    public TextureTrans getTextureTrans() {
        return textureTrans;
    }

    public void setTextureTrans(TextureTrans textureTrans) {
        this.textureTrans = textureTrans;
    }

    /**
     * 启动线程
     */
    public void startOperate() {
        if (mRunning) {
            return;
        }
        mRunning = true;
        new Thread(this, "MovieEditOperate").start();
    }


    public void setVideoQuality(int videoQuality) {
        this.videoQuality = videoQuality;
    }

    public void setVideoEncodingSize(int videoEncodingSize) {
        this.videoEncodingSize = videoEncodingSize;
    }

    public MovieEditOperate(Context mContext) {
        this.mContext = mContext;
        filterTypeBeanList = new ArrayList<>();
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new MovieHandler(this);
        Looper.loop();
    }

    public boolean isRecording() {
        return mRunning;
    }

    /**
     * 设置视频输出设置
     *
     * @param encoderConfig
     */
    public void setVideoConfig(EncoderConfig encoderConfig) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_VIDEO_CONFIG, encoderConfig));
    }

    /**
     * 设置输入的视频地址
     *
     * @param path 视频的本地地址
     */
    public void setVideo(String path) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_VIDEO, path));
    }

    /**
     * 设置视频片段参数
     *
     * @param segmentConfigs
     */
    public void setSegmentConfigs(List<SegmentConfig> segmentConfigs) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SEGMENT_CONFIG, segmentConfigs));
    }

    public void setAudioSegmentConfigs(List<SegmentConfig> segmentConfigs) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_AUDIO_SEGMENT_CONFIGS, segmentConfigs));
    }

    /**
     * 设置输入音频的地址
     *
     * @param path 音频的本地地址
     */
    public void setAudio(String path) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_AUDIO, path));
    }

    /**
     * 音频写入完成
     */
    public void setAudioEnd() {
        mHandler.sendEmptyMessage(MSG_WRITE_AUDIO_END);
    }

    /**
     * 设置监听器
     *
     * @param progressListener
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * 开始
     *
     * @param outputPath 文件输出地址
     */
    public void startHandle(String outputPath) {
        if (!isBusy) {
            this.outputPath = outputPath;
            mHandler.sendMessage(mHandler.obtainMessage(MSG_START_HANDLE, outputPath));
            isBusy = true;
        }
    }

    /**
     * 取消操作
     */
    public void cancel() {
        mHandler.sendEmptyMessage(MSG_CANCEL);
    }

    /**
     * 设置是否需要压缩
     *
     * @param needCompress
     */
    public void setMsgSetNeedCompress(boolean needCompress) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_NEED_COMPRESS, needCompress));
    }


    private static class MovieHandler extends Handler {
        private WeakReference<MovieEditOperate> mWeakEdit;

        public MovieHandler(MovieEditOperate editOperate) {
            this.mWeakEdit = new WeakReference<MovieEditOperate>(editOperate);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Object object = msg.obj;
            MovieEditOperate movieEditOperate = mWeakEdit.get();
            //被引用对象不能为空
            if (movieEditOperate == null) {
                return;
            }
            //线程处于非运行状态
            if (!movieEditOperate.mRunning) {
                return;
            }
            switch (what) {
                //设置输入视频
                case MSG_SET_VIDEO:
                    movieEditOperate.handlerSetVideo((String) object);
                    break;
                //视频输出设置
                case MSG_SET_VIDEO_CONFIG:
                    movieEditOperate.handlerSetVideoConfig((EncoderConfig) object);
                    break;
                //开始处理
                case MSG_START_HANDLE:
                    movieEditOperate.handlerStart((String) object);
                    break;
                //设置输入音频
                case MSG_SET_AUDIO:
                    movieEditOperate.handlerSetAudio((String) object);
                    break;
                //开始写入视频
                case MSG_START_WRITR_VIDEO:
                    movieEditOperate.handlerWriteVideo();
                    break;
                //视频写入完成
                case MSG_WRITE_VIDEO_END:
                    movieEditOperate.handlerVideoEnd();
                    break;
                //开始写入音频
                case MSG_START_WRITE_AUDIO:
                    movieEditOperate.handlerWriteAudio();
                    break;
                //音频写入完成
                case MSG_WRITE_AUDIO_END:
                    movieEditOperate.handlerAudioEnd();
                    break;
                //绘制farme
                case ON_DRAW_FRAME:
                    movieEditOperate.handlerOnDraw();
                    break;
                //停止录制
                case STOP_RECOEDING:
                    movieEditOperate.handlerStop();
                    break;
                //视频的配置信息
                case MSG_SEGMENT_CONFIG:
                    movieEditOperate.handlerSetSegmentConfig((List<SegmentConfig>) object);
                    break;
                case MSG_AUDIO_SEGMENT_CONFIGS:
                    movieEditOperate.handlerSetAudioSegmentConfig((List<SegmentConfig>) object);
                    break;
                //文件处理完成
                case MSG_EDIT_MEDIA_COMPLETE:
                    movieEditOperate.handlerEditMediaComplete();
                    break;
                //硬编码码失败，采用不压缩的方式处理
                case MSG_COMPRESS_FAIL:
                    movieEditOperate.handleCompressFail();
                    break;
                //设置是否需要压缩
                case MSG_SET_NEED_COMPRESS:
                    movieEditOperate.handlerSetNeedCompress((boolean) object);
                    break;
                //取消操作
                case MSG_CANCEL:
                    movieEditOperate.handlerCancle();
                    break;
            }
        }
    }


    private void handlerOnDraw() {
        mVideoRecordRender.onDrawFrame(null);
    }

    private void handlerStop() {
        mVideoRecordRender.setRecordingEnabled(false);
        mVideoRecordRender.onDrawFrame(null);
    }

    /**
     * 设置视频地址
     *
     * @param videoPath
     */
    private void handlerSetVideo(String videoPath) {
        this.videoPath = videoPath;
    }


    /**
     * 设置音频地址
     *
     * @param audioPath
     */
    private void handlerSetAudio(String audioPath) {
        this.audioPath = audioPath;
    }

    private void handlerAudioEnd() {
        mHandler.sendEmptyMessage(MSG_EDIT_MEDIA_COMPLETE);
    }

    /**
     * 处理完成
     */
    private void handlerEditMediaComplete() {
        if (!isCancel) {
            if (progressListener != null) {
                progressListener.progress(100);
            }
        } else {
            CommonUtils.delFile(outputPath);
        }
        release();
    }

    private void release() {
        writeAudioTask = null;
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (mVideoRecordRender != null) {
            mVideoRecordRender = null;
        }
        encoderConfig = null;                               //视频输出参数设置
        videoPath = "";                                     //视频文件地址
        audioPath = "";                                     //音频数据
     /*   if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }*/
        audioTrackIndex = null;                             //
        videoExtractor = null;                              //负责将源数据分离成音视频数据
        audioExtractor = null;
        totalFrame = 0;
        videoDuration = 0;                                  //视频时长 单位秒，默认是为0，如果为-1，则表示视频文件的长度
        //视频编辑完成后释放
        if (eglSurfaceBase != null) {
            eglSurfaceBase.releaseEglSurface();
            eglSurfaceBase = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        //是否需要压缩，1、如果输出码率大于输入码率，不需要压缩。2、如果手机不支持压缩，则直接进行裁剪
        NEED_COMPRESS = true;
        isBusy = false;
    }

    private void handleCompressFail() {
        writeAudioTask = null;
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (mVideoRecordRender != null) {
            mVideoRecordRender = null;
        }
        encoderConfig = null;                 //视频输出参数设置
       /* if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }*/
        audioTrackIndex = null;                   //
        //负责将源数据分离成音视频数据
        if (videoExtractor != null) {
            videoExtractor.release();
            videoExtractor = null;
        }
        //负责将源数据分离成音视频数据
        if (audioExtractor != null) {
            audioExtractor.release();
            audioExtractor = null;
        }
        totalFrame = 0;
        videoDuration = 0;              //视频时长 单位秒，默认是为0，如果为-1，则表示视频文件的长度
        //视频编辑完成后释放
        if (eglSurfaceBase != null) {
            eglSurfaceBase.releaseEglSurface();
            eglSurfaceBase = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        //是否需要压缩，1、如果输出码率大于输入码率，不需要压缩。2、如果手机不支持压缩，则直接进行裁剪
        NEED_COMPRESS = false;
        isBusy = false;
        //  mHandler.
        startHandle(outputPath);
    }


    /**
     * 设置音频输出参数
     *
     * @param encoderConfig
     */
    private void handlerSetVideoConfig(EncoderConfig encoderConfig) {
        this.encoderConfig = encoderConfig;
    }

    /**
     * @param outPath
     */
    private void handlerStart(String outPath) {
        isCancel = false;
        //编码前的准备
        editPrepare(outPath);
        //视频波特率小于等于输出的波特率
        long fileBitRate = GetMP4Info.getBitRate(videoPath, videoExtractor);
        long dskBitRate = getmBitRateByQuality(videoQuality);
       /* if (fileBitRate <= dskBitRate) {
            NEED_COMPRESS = false;
        }*/
        //开始处理视频
        mHandler.sendEmptyMessage(MSG_START_WRITR_VIDEO);
    }

    private void handlerSetNeedCompress(boolean needCompress) {
        this.NEED_COMPRESS = needCompress;
    }

    private void handlerVideoEnd() {
        if (CommonUtils.isNotEmpty(audioPath)) {
            mHandler.sendEmptyMessage(MSG_START_WRITE_AUDIO);
        } else {
            mHandler.sendEmptyMessage(MSG_EDIT_MEDIA_COMPLETE);
        }
    }

    /**
     * 写入音频
     */
    private void handlerWriteAudio() {
        writeAudio();
    }

    /**
     * 设置片段参数信息，按照从小到大的顺序，将配置数据进行排序
     *
     * @param segmentConfigs
     */
    private void handlerSetSegmentConfig(List<SegmentConfig> segmentConfigs) {
        Comparator comp = new SegmentConfigSortComparator();
        Collections.sort(segmentConfigs, comp);
        this.segmentConfigs = segmentConfigs;
    }

    /**
     * 取消上传
     */
    private void handlerCancle() {
        isCancel = true;
        if (writeAudioTask != null) {
            writeAudioTask.setCancle(isCancel);
        }


    }


    /**
     * 设置片段参数信息，按照从小到大的顺序，将配置数据进行排序
     *
     * @param segmentConfigs
     */
    private void handlerSetAudioSegmentConfig(List<SegmentConfig> segmentConfigs) {
        Comparator comp = new SegmentConfigSortComparator();
        Collections.sort(segmentConfigs, comp);
        this.audioSegmentConfigs = segmentConfigs;
    }

    private void handlerWriteVideo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    play();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public CameraRecordRenderer getmVideoRecordRender() {
        return mVideoRecordRender;
    }

    public void setmVideoRecordRender(CameraRecordRenderer mVideoRecordRender) {
        this.mVideoRecordRender = mVideoRecordRender;
    }

    /**
     * 编码前的准备
     *
     * @param outPath
     */
    private void editPrepare(String outPath) {
        //视频相关内容
        if (videoExtractor == null) {
            try {
                videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(videoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Size videoSize = GetMP4Info.getVideoSize(videoExtractor);
        int rotation = GetMP4Info.getVideoRotation(videoExtractor);
        if (((rotation / 90) % 2) == 1) {
            videoSize.exchange();
        }
        videoRealSize = new Size(videoSize.getWidth(), videoSize.getHeight());
        // videoRealSize = videoSize;
        if (videoAsp > 0) {
            videoSize.setmHeight((int) (videoAsp * videoSize.getWidth()));

        } else if (textureTrans != null || videoRealSize.getHeight() > videoRealSize.getWidth()) {
            videoSize.setmHeight(videoSize.getWidth());
        }
        //硬件编码器测试
        if (encoderConfig == null) {
            int mBitRate;
            int width = 0;
            int height = 0;
            mBitRate = getmBitRateByQuality(videoQuality);
            switch (videoEncodingSize) {
                case MediaProfile.VIDEO_ENCODING_HEIGHT_240:
                    width = 240;
                    break;
                case MediaProfile.VIDEO_ENCODING_HEIGHT_360:
                    width = 360;
                    break;
                case MediaProfile.VIDEO_ENCODING_HEIGHT_480:
                    width = 480;
                    break;
                case MediaProfile.VIDEO_ENCODING_HEIGHT_540:
                    width = 540;
                    break;
                case MediaProfile.VIDEO_ENCODING_HEIGHT_720:
                    width = 720;
                    break;
                case MediaProfile.VIDEO_ENCODING_HEIGHT_1080:
                    width = 1080;
                    break;
            }
            //高宽比
            float aspect = ((float) videoSize.getHeight()) / ((float) videoSize.getWidth());
            if (width > videoSize.getWidth()) {
                width = videoSize.getWidth();
            }
           /* //细长的矩形
            if (aspect >= 1.0f) {
                if (videoSize.getWidth() > width) {
                    height = (videoSize.getHeight() * width) / videoSize.getWidth();
                } else {
                    height = videoSize.getHeight();
                }
            }
            //扁矩形
            else {
                height = width;
                if (videoSize.getHeight() > height) {
                    width = (videoSize.getWidth() * height) / videoSize.getHeight();
                } else {
                    width = videoSize.getWidth();
                }
            }*/
            if (videoAsp > 0f) {
                height = (int) (videoAsp * width);
            } else {
                height = (int) (aspect * width);
            }
            height = height - height % 2;
            width = width - width % 2;
            encoderConfig = new EncoderConfig(width, height, mBitRate, new File(outPath));
        }


        mVideoRecordRender = new CameraRecordRenderer(mContext, null, CameraRecordRenderer.RENDERER_TYPE_VIDEO_EDIT);
        if (isFilterChange) {
            mVideoRecordRender.setFilterTypeBeanList(filterTypeBeanList);
            isFilterChange = false;
        }
        if (textureTrans != null) {
            mVideoRecordRender.setTextureTrans(textureTrans);
        }


        mVideoRecordRender.setEncoderConfig(encoderConfig);
        surface = setGLEnv(videoSize.getWidth(), videoSize.getHeight());
        mVideoRecordRender.setRecordingEnabled(true);
        //音频相关内容
        if (CommonUtils.isNotEmpty(audioPath)) {
            try {
                audioExtractor = new MediaExtractor();
                audioExtractor.setDataSource(audioPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //  MovieEditOperate.mMuxer = new FeMuxer(outPath);
       /* if (CommonUtils.isNotEmpty(audioPath)) {
            MediaFormat audioFormat = GetMP4Info.getAudioFormat(audioExtractor);
            String mime = audioFormat.getString(MediaFormat.KEY_MIME);

            if (mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                isMp4a = true;
                MediaFormat format = GetMP4Info.getAudioFormat(audioExtractor);

                //  audioFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
                audioTrackIndex = MovieEditOperate.mMuxer.addMediaFormat(format);
            } else {
                isMp4a = false;
                MediaFormat audioInputFormat = GetMP4Info.getAudioFormat(audioExtractor);
                int simpleRate = audioInputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int bitRate = 2 * 64 * 1024;
                int channelCount = audioInputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                audioEncoder = new AudioEncoder(simpleRate, bitRate, channelCount);
                MediaFormat audioOutputFormat = audioEncoder.getEncoderFormat();
                audioTrackIndex = MovieEditOperate.mMuxer.addMediaFormat(audioOutputFormat);
                audioEncoder.setTrackIndexAudio(audioTrackIndex);
                try {
                    audioDecoder = getAndStartAudioMediaCodec(audioExtractor);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }*/
        //  if ()


    }

    private int getmBitRateByQuality(int quality) {
        int bitRate = 512 * 1024;
        switch (quality) {
            case MediaProfile.VIDEO_QUALITY_LOW1:
                bitRate = 128 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_LOW2:
                bitRate = 256 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_LOW3:
                bitRate = 384 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_MEDIUM1:
                bitRate = 512 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_MEDIUM2:
                bitRate = 768 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_MEDIUM3:
                bitRate = 1024 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_HIGH1:
                bitRate = 1280 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_HIGH2:
                bitRate = 1536 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_HIGH3:
                bitRate = 2048 * 1024;
                break;
            case MediaProfile.VIDEO_QUALITY_HIGH4:
                bitRate = 4096 * 1024;
                break;
        }
        return bitRate;
    }

    /**
     * 设置opengl环境
     *
     * @param width
     * @param height
     * @return
     */

    private Surface setGLEnv(int width, int height) {

        //  mEglCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
        mEglCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
        eglSurfaceBase = new EglSurfaceBase(mEglCore);
        eglSurfaceBase.createOffscreenSurface(width, height);
        eglSurfaceBase.makeCurrent();

        mVideoRecordRender.onSurfaceCreated(null, null);
        mVideoRecordRender.onSurfaceChanged(null, width, height);
        if (videoRealSize == null) {
            mVideoRecordRender.setCameraPreviewSize(width, height);
        } else {
            mVideoRecordRender.setCameraPreviewSize(videoRealSize.getWidth(), videoRealSize.getHeight());
        }

        return mVideoRecordRender.getmSurface();
    }


    public void play() throws IOException {
        MediaCodec decoder = null;
        //视频中的总帧数
        try {
            int trackIndex = GetMP4Info.selectVideoTrack(videoExtractor);            //选择的视频的那个流   track表示文件中的源数，多条音轨算多个
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + videoPath);
            }
            // 需要压缩的视频
            if (NEED_COMPRESS) {
                //初始化视频解码器
                decoder = getAndStartVideoMediaCodec(videoExtractor);
                //提取解析
                boolean state = doExtract(videoExtractor, trackIndex, decoder);
                if (!state) {
                    return;
                }
            }
            //不需要压缩的视频
            else {
                addVideoStream(videoExtractor);
            }
        } finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
           /* if (videoExtractor != null) {
                videoExtractor.release();
                videoExtractor = null;
            }*/
        }
        mHandler.sendEmptyMessage(MSG_WRITE_VIDEO_END);

    }

    /**
     * 开启视频解码器
     *
     * @param extractor
     * @return
     * @throws IOException
     */
    @NonNull
    private MediaCodec getAndStartVideoMediaCodec(MediaExtractor extractor) throws IOException {
        MediaCodec decoder = null;
        MediaFormat format = GetMP4Info.getVideoFormat(extractor);
        String mime = format.getString(MediaFormat.KEY_MIME);
        decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(format, surface, null, 0);
        decoder.start();
        return decoder;
    }

    /**
     * 开启音频解码器
     *
     * @param extractor
     * @return
     * @throws IOException
     */
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

    /**
     * 解码方式
     *
     * @param extractor
     * @param trackIndex
     * @param decoder
     * @return
     */
    private boolean doExtract(MediaExtractor extractor, int trackIndex, MediaCodec decoder) {
        extractor.selectTrack(trackIndex);
        final int TIMEOUT_USEC = 0;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        long firstInputTimeNsec = -1;
        boolean outputDone = false;                 //解压输出完成
        boolean inputDone = false;                  //解压输入完成
        int inputChunk = 0;                         //传输的数量
        int outputChunk = 0;                        //传出的数量
        int program = 0;                            //进度
        //视频分段处理信息
        getVideoDuration(extractor);
        totalFrame = GetMP4Info.getSliceVideoFrameNumber(videoExtractor, videoDuration);

        int sleepNumber = 0;

        for (int i = 0; i < segmentConfigs.size(); i++) {
            SegmentConfig segmentConfig = segmentConfigs.get(i);
            long starttime = ((long) segmentConfig.getStartTime()) * 1000000L;

            extractor.seekTo(starttime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            while (!outputDone) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //计算进度
                program = getProgram(inputChunk, program, outputNumber);

                Log.d("current", "totalFrame" + totalFrame + "inputChunk" + inputChunk + "program" + program + "outputNumber" + outputNumber + "");

                //编解码同步,拿到已解码的数据和编码完成的数据个数进行对比，如果差值超过4，就进行等待，否则会造成内存溢出
                if ((outputChunk - outputNumber) > 2) {
                    try {
                        if (sleepNumber > MAX_SLEEP_NUMBER) {
                            mHandler.sendEmptyMessage(MSG_COMPRESS_FAIL);
                            return FAILURE;
                        }
                        Thread.sleep(10);
                        Log.d(TAG, "sleep");
                        sleepNumber++;

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    sleepNumber = 0;
                    if (!inputDone) {
                        int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (inputBufIndex >= 0) {
                            if (firstInputTimeNsec == -1) {
                                firstInputTimeNsec = System.nanoTime();
                            }
                            if (((segmentConfig.getEndTime() != -1) && extractor.getSampleTime() > (segmentConfig.getEndTime() * 1000000L)) || isCancel) {
                                if ((i == segmentConfigs.size() - 1) || isCancel) {
                                    decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                } else {
                                    break;
                                }
                            } else {
                                ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                                int chunkSize = extractor.readSampleData(inputBuf, 0);
                                if (chunkSize < 0) {
                                    decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                } else {
                                    if (extractor.getSampleTrackIndex() != trackIndex) {
                                        Log.w(TAG, "WEIRD: got sample from track " +
                                                extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                                    }
                                    long presentationTimeUs = extractor.getSampleTime();


                                    decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                                            presentationTimeUs - getSegmentConfigStartTime(segmentConfigs, i) * 1000000L, 0 /*flags*/);
                                    if (VERBOSE) {
                                        Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                                chunkSize);
                                    }
                                    inputChunk++;
                                    extractor.advance();
                                }
                            }
                        } else {
                            if (VERBOSE) Log.d(TAG, "input buffer not available");
                        }
                    }
                    if (!outputDone) {
                        int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            if (VERBOSE) Log.d(TAG, "no output from decoder available");
                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = decoder.getOutputFormat();
                            if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
                        } else if (decoderStatus < 0) {
                            throw new RuntimeException(
                                    "unexpected result from decoder.dequeueOutputBuffer: " +
                                            decoderStatus);
                        } else { // decoderStatus >= 0
                            if (firstInputTimeNsec != 0) {
                                long nowNsec = System.nanoTime();
                                Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                                firstInputTimeNsec = 0;
                            }
                            boolean doLoop = false;
                            if (VERBOSE)
                                Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                                        " (size=" + mBufferInfo.size + ")");
                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true;
                                mHandler.sendEmptyMessage(STOP_RECOEDING);
                            }
                            boolean doRender = (mBufferInfo.size != 0);

                            // if (MainActivity.isEncoder) {
                            mHandler.sendEmptyMessage(ON_DRAW_FRAME);
                            //  }
                            if (VERBOSE) {
                                Log.d(TAG, "decoder frame " + outputChunk + " to dec, size="
                                );
                            }
                            outputChunk++;
                            decoder.releaseOutputBuffer(decoderStatus, doRender);

                        }
                    }
                }
            }
        }
        return SUCCESS;

    }

    /**
     * 获取剪切视频的长度
     *
     * @param extractor
     */
    private void getVideoDuration(MediaExtractor extractor) {
        int duration = GetMP4Info.getDuration(extractor);
        if (segmentConfigs == null) {
            segmentConfigs = new ArrayList<SegmentConfig>();
            segmentConfigs.add(new SegmentConfig());
            videoDuration = duration;
        } else if (segmentConfigs.size() == 0) {
            segmentConfigs.add(new SegmentConfig());
            videoDuration = duration;
        } else {
            for (SegmentConfig segmentConfig : segmentConfigs) {
                if (segmentConfig.getStartTime() >= duration) {
                    continue;
                }
                if (segmentConfig.getEndTime() >= duration) {
                    segmentConfig.setEndTime(duration);
                }
                if (segmentConfig.getEndTime() <= 0) {
                    segmentConfig.setEndTime(duration);
                }
                if (segmentConfig.getEndTime() > 0) {
                    int segmentDuration = segmentConfig.getEndTime() - segmentConfig.getStartTime();
                    videoDuration += segmentDuration;
                }
            }
        }
    }

    /**
     * 获取视频进度
     */
    private int getProgram(int inputChunk, int program, int outputNumber) {
        if (progressListener != null) {
            if (totalFrame > 0 && inputChunk > 0) {
                if (program != inputChunk) {
                    program = inputChunk;
                    long quotient = totalFrame / 99;
                    if (quotient == 0) {
                        quotient = 1;
                    }
                    if (inputChunk % quotient == 0) {
                        int percent = (int) ((outputNumber * 99) / totalFrame);
                        if (percent >= 99) {
                            percent = 99;
                        }
                        progressListener.progress(percent);
                    }
                }
            }
        }
        return program;
    }


    WriteAudioTask writeAudioTask = null;

    /**
     * 向mp4文件中写入音频
     * 1、首先检查是否是mp4文件 ，mp4直接写文件，非mp4文件需要设置音频的编解码器
     * 2、音视频是否是同文件，同文件间隔直接写入视频的时间配置，不同文件需要音频的时间设置
     */
    private void writeAudio() {
       /* if (mMuxer != null) {
            if (writeAudioTask == null) {
                //是否是m4a文件
                if (isMp4a) {
                    writeAudioTask = new WriteAudioTask(audioExtractor, mMuxer, audioTrackIndex);
                } else {
                    writeAudioTask = new WriteAudioTask(audioExtractor, mMuxer, audioTrackIndex, audioDecoder, audioEncoder);
                }
                //audio video 是否为同文件
                //同文件 时间配置信息一致
                if (audioPath.equals(videoPath)) {
                    writeAudioTask.setSegmentConfigs(segmentConfigs, true);
                }
                //不同文件
                else {
                    if (audioSegmentConfigs != null && audioSegmentConfigs.size() > 0) {
                        writeAudioTask.setSegmentConfigs(audioSegmentConfigs, false);
                    } else {
                        List<SegmentConfig> segments = new ArrayList<>();
                        segments.add(new SegmentConfig(0, videoDuration, 1));
                        writeAudioTask.setSegmentConfigs(segments, false);
                    }
                }
                new Thread(writeAudioTask).start();
            }
        }*/
    }

    /**
     * 获取到帧的时间差
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


    /**
     * 直接写入视频文件
     *
     * @param videoExtractor
     */
    private void addVideoStream(MediaExtractor videoExtractor) {

        TrackIndex videoTrackIndex = null;         //写入视频的音轨
        int videoIndex;
        int videoMaxInputSize;
        boolean isWritedVideo = false;
        long lasttime = 0;

        int inputChunk = 0;
        int program = 0;
        getVideoDuration(videoExtractor);
        totalFrame = GetMP4Info.getSliceVideoFrameNumber(videoExtractor, videoDuration);


        if (CommonUtils.isNotEmpty(videoPath)) {
            MediaFormat mediaFormat = GetMP4Info.getVideoFormat(videoExtractor);
          /*  videoTrackIndex = MovieEditOperate.mMuxer.addMediaFormat(mediaFormat);
            mMuxer.setOrientationHint(GetMP4Info.getVideoRotation(videoExtractor));
            MovieEditOperate.mMuxer.start();*/
        }

        videoMaxInputSize = GetMP4Info.getVideoFormat(videoExtractor).getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        videoIndex = GetMP4Info.selectVideoTrack(videoExtractor);
        videoExtractor.selectTrack(videoIndex);
        // MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer inputBuf = ByteBuffer.allocate(videoMaxInputSize);
        //同文件的视频切割
        for (int i = 0; i < segmentConfigs.size(); i++) {
            SegmentConfig segmentConfig = segmentConfigs.get(i);
            long starttime = ((long) segmentConfig.getStartTime()) * 1000000L;

            videoExtractor.seekTo(starttime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            while (!isWritedVideo) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                program = getProgram(inputChunk, program, inputChunk);
                inputChunk++;
                int sampleSize = videoExtractor.readSampleData(inputBuf, 0);
                long sampleTime1 = videoExtractor.getSampleTime();
                long endtime = ((long) segmentConfig.getEndTime()) * 1000000L;
                if ((segmentConfig.getEndTime() != -1) && sampleTime1 > endtime) {
                    if (i == (segmentConfigs.size() - 1)) {
                        isWritedVideo = true;
                        //mMuxer.closed(videoTrackIndex);
                        break;
                    } else {
                        break;
                    }
                } else {
                    if (sampleSize < 0) {
                        isWritedVideo = true;
                        // mMuxer.closed(videoTrackIndex);
                    } else {
                        int flags = videoExtractor.getSampleFlags();
                        info.offset = 0;
                        info.size = sampleSize;
                        info.flags = flags;


                        long sampleTime = videoExtractor.getSampleTime();
                        //   long presentationTimeUs = videoExtractor.getSampleTime() - getSegmentConfigStartTime(segmentConfigs, i) * 1000000;


                        long presentationTimeUs = sampleTime - getSegmentConfigStartTime(segmentConfigs, i) * 1000000L;

                        if (presentationTimeUs < 0) {
                            presentationTimeUs = 0;
                        }
                        if (presentationTimeUs >= lasttime) {
                            info.presentationTimeUs = presentationTimeUs;
                          /*  if (mMuxer != null) {
                                mMuxer.writeSampleData(videoTrackIndex, inputBuf, info);
                            }*/
                            lasttime = presentationTimeUs;
                        }


                        videoExtractor.advance();
                    }

                }


            }
            // mMuxer.closed(videoTrackIndex);
        }
    }

    /**
     * 是否处于忙碌状态
     *
     * @return
     */
    public boolean isBusy() {
        return isBusy;
    }

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

    public float getVideoAsp() {
        return videoAsp;
    }

    public void setVideoAsp(float videoAsp) {
        this.videoAsp = videoAsp;
    }
}
