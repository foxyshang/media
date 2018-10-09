package cn.embed.videohandle.mpeg;

/**
 * Created by shangdongzhou on 2017/6/14.
 */

public class MediaProfile {
    //视频的码率
    public static final int VIDEO_QUALITY_LOW1 = 0;             //128kbps
    public static final int VIDEO_QUALITY_LOW2 = 1;              //256
    public static final int VIDEO_QUALITY_LOW3 = 2;             //384
    public static final int VIDEO_QUALITY_MEDIUM1 = 10;         //512
    public static final int VIDEO_QUALITY_MEDIUM2 = 11;         //768
    public static final int VIDEO_QUALITY_MEDIUM3 = 12;         //1024
    public static final int VIDEO_QUALITY_HIGH1 = 20;           //1280
    public static final int VIDEO_QUALITY_HIGH2 = 21;           //1536
    public static final int VIDEO_QUALITY_HIGH3 = 22;           //2048
    public static final int VIDEO_QUALITY_HIGH4 = 4096;           //4096

    public static final int VIDEO_ENCODING_HEIGHT_240 = 0;       //424 x 240   320 x 240
    public static final int VIDEO_ENCODING_HEIGHT_360 = 1;       //640 x 360
    public static final int VIDEO_ENCODING_HEIGHT_480 = 2;       //840 x 480   640 x 480
    public static final int VIDEO_ENCODING_HEIGHT_540 = 3;       //960 x 540   720 x 540
    public static final int VIDEO_ENCODING_HEIGHT_720 = 4;       //1280 x 720  960 x 720
    public static final int VIDEO_ENCODING_HEIGHT_1080 = 5;      //1920 x 1080 1440 x 1080

}
