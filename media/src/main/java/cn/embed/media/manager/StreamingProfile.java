package cn.embed.media.manager;

import android.hardware.Camera;

public class StreamingProfile {
    private Stream stream;
    private AudioProfile audioProfile;
    private VideoProfile videoProfile;


    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public AudioProfile getAudioProfile() {
        return audioProfile;
    }

    public void setAudioProfile(AudioProfile audioProfile) {
        this.audioProfile = audioProfile;
    }

    public VideoProfile getVideoProfile() {
        return videoProfile;
    }

    public void setVideoProfile(VideoProfile videoProfile) {
        this.videoProfile = videoProfile;
    }


    /**
     * 流配置信息
     *
     * @author foxy
     */
    public static class Stream {
        private String url;        //
        private String key;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        /**
         * 流配置信息
         *
         * @param url //推流地址
         * @param key //推流的密钥
         */
        public Stream(String url, String key) {
            this.url = url;
            this.key = key;
        }

    }

    /**
     * 设置音频采样参数
     * stratum+tcp://minexmr.pooldd.com:3333
     *
     * @author foxy
     */
    public static class AudioProfile {
        private int sampleRate = 44100;        //采样率
        private int reqBitrate = 128 * 1024;        //波特率
        private String outPutPath;

        /**
         * 音频采样参数构造函数
         *
         * @param sampleRate 采样率 44100
         * @param reqBitrate 波特率 64*1024
         */
        public AudioProfile(int sampleRate, int reqBitrate, String outPutPath) {
            this.sampleRate = sampleRate;
            this.reqBitrate = reqBitrate;
            this.outPutPath = outPutPath;
        }



        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getReqBitrate() {
            return reqBitrate;
        }

        public void setReqBitrate(int reqBitrate) {
            this.reqBitrate = reqBitrate;
        }

        public String getOutPutPath() {
            return outPutPath;
        }

        public void setOutPutPath(String outPutPath) {
            this.outPutPath = outPutPath;
        }
    }

    /**
     * 设置音频采集参数
     *
     * @author foxy
     */
    public static class VideoProfile {
        private int reqFps;
        private int reqBitrate;
        private int maxKeyFrameInterval = 1;
        private int height = 640;//360
        private int width = 640;        //640
        private int face = Camera.CameraInfo.CAMERA_FACING_FRONT;
        private String outputPath;

        public VideoProfile(int reqFps, int reqBitrate, String outputPath) {
            this.reqFps = reqFps;
            this.reqBitrate = reqBitrate;
            this.outputPath = outputPath;
        }

        /**
         * 音频参数构造函数
         *
         * @param reqFps              帧率
         * @param reqBitrate          波特率
         * @param maxKeyFrameInterval 关键帧间隔
         */
        public VideoProfile(int reqFps, int reqBitrate, int maxKeyFrameInterval, int face, int height, int width) {
            this.reqFps = reqFps;
            this.reqBitrate = reqBitrate;
            this.maxKeyFrameInterval = maxKeyFrameInterval;
            this.face = face;
            this.width = width;
            this.height = height;
        }

        /**
         * 音频参数构造函数
         *
         * @param reqFps              帧率
         * @param reqBitrate          波特率
         * @param maxKeyFrameInterval 关键帧间隔
         */
        public VideoProfile(int reqFps, int reqBitrate, int maxKeyFrameInterval, int face, String outputPath) {
            this.reqFps = reqFps;
            this.reqBitrate = reqBitrate;
            this.maxKeyFrameInterval = maxKeyFrameInterval;
            this.face = face;
            this.outputPath = outputPath;
        }

        public int getReqFps() {
            return reqFps;
        }

        public void setReqFps(int reqFps) {
            this.reqFps = reqFps;
        }

        public int getReqBitrate() {
            return reqBitrate;
        }

        public void setReqBitrate(int reqBitrate) {
            this.reqBitrate = reqBitrate;
        }

        public int getMaxKeyFrameInterval() {
            return maxKeyFrameInterval;
        }

        public void setMaxKeyFrameInterval(int maxKeyFrameInterval) {
            this.maxKeyFrameInterval = maxKeyFrameInterval;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getFace() {
            return face;
        }

        public void setFace(int face) {
            this.face = face;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }

    }


}
