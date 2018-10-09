package cn.embed.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.File;

/**
 * Created by shangdongzhou on 2017/5/24.
 * 读取输入文件的MP4信息
 */

public class GetMP4Info {

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no audio track is found.
     */
    public static int selectAudioTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);       //format 的类型
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    public static int selectVideoTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);       //format 的类型
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    public static int selectAudioTrack(String filePath) {
        MediaExtractor extractor = null;        //负责将源数据分离成音视频数据
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (extractor == null) {
            return -1;
        }
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);       //format 的类型
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    public static MediaExtractor getMediaExtractor(String filePath) {
        MediaExtractor extractor = null;        //负责将源数据分离成音视频数据
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return extractor;

    }

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    public static int selectVideoTrack(String filePath) {
        MediaExtractor extractor = null;        //负责将源数据分离成音视频数据
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);       //format 的类型
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }


    /**
     * 获取视频的Fromat
     *
     * @param extractor
     * @return
     */
    public static MediaFormat getVideoFormat(MediaExtractor extractor) {
        MediaFormat format = null;
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);       //format 的类型
            if (mime.startsWith("video/")) {
                return format;
            }
        }
        return format;
    }

    /**
     * 获取音频的Fromat
     *
     * @param extractor
     * @return
     */
    public static MediaFormat getAudioFormat(MediaExtractor extractor) {
        MediaFormat format = null;
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);       //format 的类型
            if (mime.startsWith("audio/")) {
                return format;
            }
        }
        return format;
    }


    /**
     * 获取视频大小
     *
     * @param extractor
     * @return
     */
    public static Size getVideoSize(MediaExtractor extractor) {
        Size size = new Size(-1, -1);
        MediaFormat format = getVideoFormat(extractor);
        size.setmWidth(format.getInteger(MediaFormat.KEY_WIDTH));
        size.setmHeight(format.getInteger(MediaFormat.KEY_HEIGHT));
        return size;
    }


    /**
     * 获取视频大小
     *
     * @param
     * @return
     */
    public static Size getVideoSize(String videoPath) {

        return getVideoSize(getExtractor(videoPath));
    }


    public static int getVideoRotation(MediaExtractor extractor) {
        int rotation = 0;
        MediaFormat format = getVideoFormat(extractor);
        try {
            rotation = format.getInteger(MediaFormat.KEY_ROTATION);
        } catch (Exception e) {
            rotation = 0;
        }
        return rotation;
    }

    public static int getVideoRotation(String videoPath) {
        return getVideoRotation(getExtractor(videoPath));
    }

    /**
     * 获取视频中的总帧数
     *
     * @return
     */
    public static long getVideoFrameNumber(MediaExtractor extractor) {
        MediaFormat format = GetMP4Info.getVideoFormat(extractor);
        int frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);     //帧率
        long duration = format.getLong(MediaFormat.KEY_DURATION);          //间隔时间
        return frameRate * (duration / 1000000);
    }

    /**
     * 获取一段时间的帧数
     */
    public static long getSliceVideoFrameNumber(MediaExtractor extractor, int interval) {
        MediaFormat format = GetMP4Info.getVideoFormat(extractor);
        int frameRate = 30;
        try {
            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);     //帧率
        } catch (Exception e) {
            frameRate = 30;
        }
        return frameRate * interval;
    }

    /**
     * 获取总时长
     *
     * @param extractor
     * @return
     */
    public static int getDuration(MediaExtractor extractor) {
        MediaFormat format = GetMP4Info.getVideoFormat(extractor);
        long duration = format.getLong(MediaFormat.KEY_DURATION);
        return (int) (duration / 1000000);
    }

    /**
     * 获取视频波特率
     *
     * @param videoExtractor //
     * @return
     */
    public static long getBitRate(String videoPath, MediaExtractor videoExtractor) {
        File file = new File(videoPath);
        long length = file.length();
        int duration = getDuration(videoExtractor);
        return (length / duration) * 8;
    }

    public static MediaExtractor getExtractor(String path) {
        MediaExtractor extractor = null;        //负责将源数据分离成音视频数据
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(path);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return extractor;
    }


}
