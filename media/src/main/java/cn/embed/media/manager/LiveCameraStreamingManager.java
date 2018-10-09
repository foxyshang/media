package cn.embed.media.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import cn.embed.audio.LiveAudio;
import cn.embed.codec.AudioEncoder;
import cn.embed.media.surfaceEncoder.camera.CameraController;
import cn.embed.renderer.CameraRecordRenderer;
import cn.embed.media.surfaceEncoder.filter.FilterManager;
import cn.embed.media.surfaceEncoder.video.EncoderConfig;
import cn.embed.media.surfaceEncoder.video.TextureMovieEncoder;
import cn.embed.media.surfaceEncoder.widget.CameraSurfaceView;
import cn.embed.utils.NativeStream;


public class LiveCameraStreamingManager {
    private static final int NET_CONNECTED = 1;
    private static final int NET_DISCONNECTED = 2;
    private static final int NET_CONNECT_ERROR = 3;
    private static final int NET_URL_ERROR = 4;

    private Context context;
    public static AudioEncoder audioEncoder = null;                             // 音频编码器
    private static boolean isStreaming = false;                                // 推流状态
    private boolean firstIn = true;
    public static String phone_model = "";
    private boolean isMute = false;                                        // 是否进行静音操作，只发送视频，不发送音频，缺省值为非静音情况
    private StreamingProfile profile;
    private CameraSurfaceView glSurface;
    public static Queue<byte[]> sendDataQueue = new LinkedList<byte[]>();    // 队列，用于缓存音频和视频数据
    private Thread sendThread = null;
    private boolean isFirstPacket = true;
    private static boolean isTexture = false;
    private boolean isEndStream = false;
    private NetInterface netInterface = null;                                // 网络状态监控接口
    private boolean netListenerEnable = false;                                // 网络状态接口使能
    private boolean isNetConnect = false;                                    // 查看网络是否处于网络连接状态
    private Handler handler;
    private String url = "";
    // 负责发送队列中的视频和音频数据
    Runnable sendRunable = new Runnable() {
        @Override
        public void run() {
            while (isStreaming) {
                if (isEndStream) {
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isNetConnect) {
                    if (!NativeStream.isBusy) {

                        byte[] encData = null;
                        synchronized (LiveCameraStreamingManager.sendDataQueue) {
                            if (sendDataQueue.size() > 2) {
                                if ((encData = sendDataQueue.poll()) != null) {
                                    //判断video
                                    if ((encData[0] == 0x00) && (encData[1] == 0x00) && (encData[2] == 0x00) && (encData[3] == 0x01)) {
                                        //判断长度
                                        if (encData.length >= 4) {
                                            if (isFirstPacket) {
                                                if ((encData[4] & 0x0f) == 7) {
                                                    int result = NativeStream.sendVideo(encData, encData.length);
                                                    if (result == -5) {
                                                        isNetConnect = false;
                                                        setHandleMsg(NET_DISCONNECTED);
                                                    } else {
                                                        isFirstPacket = false;
                                                    }

                                                }

                                            } else {
                                                int result = NativeStream.sendVideo(encData, encData.length);
                                                if (result == -5) {
                                                    isNetConnect = false;
                                                    setHandleMsg(NET_DISCONNECTED);
                                                }
                                            }
                                        }

                                    } else {
                                        if (encData.length > 7) {

                                            if (!isFirstPacket) {
                                                int result = NativeStream.sendAudio(encData, encData.length);
                                                if (result == -5) {
                                                    isNetConnect = false;
                                                    setHandleMsg(NET_DISCONNECTED);
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }

                }

            }

        }
    };

    /**
     * 是否为后置摄像头
     *
     * @return true 后置 false 为前置
     */
    public boolean ismIsCameraBackForward() {
        return glSurface.isCameraBackForward();
    }

    /**
     * 重新启动设定摄像机
     *
     * @return
     */
    public void resetCamera(boolean ismIsCameraBackForward) {
        glSurface.resetCamera(ismIsCameraBackForward);
    }

    public static boolean isStreaming() {
        return isStreaming;
    }

    public void init() {

        audioEncoder = null; // 音频编码器
        isStreaming = false; // 推流状态
        sendDataQueue.clear();
        isTexture = false;
    }

    /**
     * 管理管理构造函数
     *
     * @param context
     * @param glSurface
     */
    @SuppressLint("HandlerLeak")
    public LiveCameraStreamingManager(Context context,
                                      CameraSurfaceView glSurface) {
        super();
        this.context = context;
        this.glSurface = glSurface;

        audioEncoder = null; // 音频编码器
        isStreaming = false; // 推流状态
        sendDataQueue.clear();
        isTexture = false;
        handler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    // 推流连接断开
                    case NET_DISCONNECTED:
                        NativeStream.close();
                        isFirstPacket = true;
                        if (netInterface != null) {
                            netInterface.disconnected();
                        }
                        break;
                    // 推流连接成功
                    case NET_CONNECTED:
                        if (netInterface != null) {
                            netInterface.connected();
                        }
                        break;
                    // 网络连接失败
                    case NET_CONNECT_ERROR:
                        if (netInterface != null) {
                            netInterface.connectError();
                        }
                        break;
                    // url 错误
                    case NET_URL_ERROR:
                        if (netInterface != null) {
                            netInterface.urlError();
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 推流准备
     *
     * @param profile
     */
    public void onPrepare(StreamingProfile profile) {
        this.profile = profile;
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        glSurface.switchCamera();
    }

    /**
     * 开启闪光灯
     *
     * @param isOpen true 打开 ；false 关闭
     *               <p>
     *               参数为 Camera.Parameters.FLASH_MODE_AUTO;
     *               Camera.Parameters.FLASH_MODE_OFF;
     *               Camera.Parameters.FLASH_MODE_ON;
     *               Camera.Parameters.FLASH_MODE_RED_EYE
     *               Camera.Parameters.FLASH_MODE_TORCH 等
     */
    public void setLightOpen(boolean isOpen) {
        if (isOpen) {
            glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_OFF);
        }

    }

    /**
     * 打开闪光灯
     */
    public void turnLightOn() {
        glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_TORCH);
    }

    /**
     * 关闭闪光灯
     */
    public void turnLightOff() {
        glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_OFF);
    }

    /**
     * 查询目前是否为静音状态
     *
     * @return
     */
    public boolean isMute() {
        return isMute;
    }

    /**
     * 设置当前收否为静音状态 true 静音 false，不静音
     *
     * @param isMute
     */
    public void setMute(boolean isMute) {
        this.isMute = isMute;
    }

    /**
     * 开始推流
     */
    public boolean startStreaming() {
        glSurface.getRenderer().setRecordingEnabled(true);
        isStreaming = true;
        url = profile.getStream().getUrl();
        if (url.equals("") || url == null || url.equals("null")) {
            Log.i("foxy", "start stream error,please set stream's url");
            setHandleMsg(NET_URL_ERROR);
            return false;
        }

        int result = NativeStream.open(url);
        if (result < 0) {
            Log.i("foxy", "start stream failure,please check phone's net");
            setHandleMsg(NET_CONNECT_ERROR);
            return false;
        }
        isNetConnect = true;
        setHandleMsg(NET_CONNECTED);

        if (sendThread == null) {
            sendThread = new Thread(sendRunable);
        } else {
            sendThread = null;
            sendThread = new Thread(sendRunable);
        }
        sendThread.start();
        return true;
    }

    public boolean reconnectStream() {
        if (isNetConnect) {
            return true;
        }
        url = profile.getStream().getUrl();
        if (url.equals("") || url == null || url.equals("null")) {
            Log.i("foxy", "start stream error,please set stream's url");
            setHandleMsg(NET_URL_ERROR);
            return false;
        }
        int result = NativeStream.open(url);
        if (result < 0) {
            setHandleMsg(NET_CONNECT_ERROR);
            NativeStream.close();
            return false;
        }
        isNetConnect = true;
        setHandleMsg(NET_CONNECTED);
        return true;
    }

    /**
     * 停止推流
     *
     * @return
     */
    public boolean stopStreaming() {
        if (sendThread != null) {
            sendThread.interrupted();
            sendRunable = null;
        }
        isEndStream = true;
        isStreaming = false;
        isNetConnect = false;
        NativeStream.close();
        if (TextureMovieEncoder.getInstance().isRecording()) {
            TextureMovieEncoder.getInstance().stopRecording();
            isTexture = false;
        }
        closeDevice();
        closeEncode();
        return true;
    }

    public void closeDevice() {
        // 关闭录音机
        LiveAudio.getInstance().start();
        // 关闭相机

    }

    /**
     * 设置设备参数，-1为设备缺省值
     *
     * @return
     */
    public boolean openDevice() {
        initEncode();
        LiveAudio.getInstance().setConfig(profile.getAudioProfile());
        LiveAudio.getInstance().setDateListener(new AudioDataSend());
        LiveAudio.getInstance().start();


        return true;
    }

    /**
     * 初始化编码器
     */
    private void initEncode() {
        int width, height;
        // 初始化视频编码器
        // if (!isTexture) {
        CameraRecordRenderer renderer = glSurface.getRenderer();
        renderer.setEncoderConfig(new EncoderConfig(profile.getVideoProfile().getWidth(), profile.getVideoProfile().getHeight(), profile.getVideoProfile().getReqBitrate()));


        isTexture = true;
        // }
        if (audioEncoder == null) {
            audioEncoder = new AudioEncoder(
                    profile.getAudioProfile().getSampleRate(),
                    profile.getAudioProfile().getSampleRate());
        } else {
            audioEncoder = null;
            audioEncoder = new AudioEncoder(
                    profile.getAudioProfile().getSampleRate(),
                    profile.getAudioProfile().getSampleRate());
        }

    }

    /**
     * 释放编码器
     */
    private void closeEncode() {
        /*
         * if (videoEncoder!=null) { videoEncoder=null; }
         */
        if (audioEncoder != null) {
            audioEncoder = null;
        }

    }

    public class AudioDataSend implements LiveAudio.IPCMData {
        /**
         * 音频数据进行硬编并进行发送
         *
         * @param data
         */
        @Override
        public void audioData(byte[] data) {
            // TODO 编码并发送音频数据
            if (isStreaming) {
                // 非静音下处理音频数据
                if (!isMute) {
                    if (data.length > 7) {
                        byte[] outData = audioEncoder.offerEncord(data);
                        if (outData.length > 7) {
                            if (LiveCameraStreamingManager.sendDataQueue.size() < 100) {
                                synchronized (LiveCameraStreamingManager.sendDataQueue) {
                                    LiveCameraStreamingManager.sendDataQueue
                                            .offer(outData);
                                }
                            }
                        }

                    }

                }

            }

        }

    }

    /**
     * 界面进入后台时调用
     */
    public void onPause() {
        CameraController.getInstance().stopCamera();
        glSurface.onPause();
        closeDevice();

    }

    /**
     * activity 执行onResume()时调用
     */
    public void onResume() {
        if (firstIn) {
            firstIn = false;
        } else {
            openDevice();
            glSurface.onResume();
            // glSurface.resumePreview();
        }
    }

    /**
     * activity执行onDestory时调用
     */
    public void onDestory() {
        stopStreaming();
        CameraController.getInstance().stopCamera();
    }


    public void changeFilter(FilterManager.FilterType filterType) {
        // glSurface.changeFilter(filterType);
    }


    /**
     * 设置网络状态的监听事件
     *
     * @param netInterface
     */
    public void setNetListener(NetInterface netInterface) {
        if (netInterface != null) {
            this.netInterface = netInterface;
            netListenerEnable = true;
        } else {
            netListenerEnable = false;
        }

    }

    public boolean isNetConnect() {
        return isNetConnect;
    }

    public void setNetConnect(boolean isNetConnect) {
        this.isNetConnect = isNetConnect;
    }

    private void setHandleMsg(int what) {
        if (netListenerEnable && netInterface != null) {
            Message message = new Message();
            message.what = what;
            handler.sendMessage(message);
        }
    }

}
