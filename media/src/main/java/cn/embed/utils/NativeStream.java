package cn.embed.utils;

public class NativeStream {
    private static byte[] data = {0x11, 0x22};
    private static final int NET_INIT = 0;
    private static final int SEND_VIDEIO_STREAM = 1;
    private static final int SEND_AUDIO_STREAM = 2;
    private static final int CLOSE_STREAM = 3;

    public static final int NETINIT_ERROR = -2;
    public static final int SEND_H264_ERROR = -3;
    public static final int SEND_ACC_ERROR = -4;
    public static final int NETCONN_ERROR = -5;

    public static boolean isBusy = false;

    static {
        System.loadLibrary("rtmp");
        System.loadLibrary("sffstreamer");
    }

    public static native synchronized int stream(byte[] inputurl, String outputurl, int len, int type);

    /**
     * @param outputurl
     * @return 0 正常  -2 失败
     */
    public static int open(String outputurl) {
        close();
        int result = NativeStream.stream(new byte[2], outputurl, 2, NET_INIT);
        return result;
    }

    /**
     * @return 0 正常  -3 失败
     */
    public static int sendVideo(byte[] data, int len) {
        if (len < 4) {
            return 0;
        }
        isBusy = true;
        int result = NativeStream.stream(data, "abc", len, SEND_VIDEIO_STREAM);
        isBusy = false;
        return result;

    }

    /**
     * @return 0 正常  -4 失败
     */
    public static int sendAudio(byte[] data, int len) {
        if (len < 7) {
            return 0;
        }
        isBusy = true;
        int result = NativeStream.stream(data, "abc", len, SEND_AUDIO_STREAM);
        isBusy = false;
        return result;

    }

    public static int close() {
        int result = NativeStream.stream(data, "abc", 2, CLOSE_STREAM);
        return result;

    }


}
