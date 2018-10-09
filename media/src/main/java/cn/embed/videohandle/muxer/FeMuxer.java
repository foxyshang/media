package cn.embed.videohandle.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 视频合成 on 2017/5/23.
 */

public class FeMuxer {
    private MediaMuxer mediaMuxer;
    private List<TrackIndex> trackIndices;
    private String dstPath;
    private boolean finished = false;

    /**
     * dstPath 不能为空，必须为有效地址,不能重复
     *
     * @param dstPath
     */
    public FeMuxer(String dstPath) {
        this.dstPath = dstPath;
        try {
            mediaMuxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            finished = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        trackIndices = new ArrayList<>();
    }

    /**
     * 添加mediaFormat
     *
     * @param mediaFormat
     * @return
     */
    public TrackIndex addMediaFormat(MediaFormat mediaFormat) {
        TrackIndex trackIndex = new TrackIndex();
        if (mediaFormat != null) {
            trackIndex.setTrackIndex(mediaMuxer.addTrack(mediaFormat));
            trackIndex.setOver(false);
        }
        trackIndices.add(trackIndex);
        return trackIndex;
    }

    public void start() {
        if (mediaMuxer != null) {
            mediaMuxer.start();
        }
    }

    /**
     * 合成mp4
     *
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
    public void writeSampleData(TrackIndex trackIndex, @NonNull ByteBuffer byteBuf, @NonNull MediaCodec.BufferInfo bufferInfo) {
       // if (mediaMuxer!=null) {
            mediaMuxer.writeSampleData(trackIndex.getTrackIndex(), byteBuf, bufferInfo);
      //  }
    }

    /**
     * 流写入完成，关闭流信息
     *
     * @param trackIndex
     */
    public void closed(TrackIndex trackIndex) {
        trackIndex.setOver(true);
        for (TrackIndex track : trackIndices) {
            if (!track.isOver()) {
                return;
            }
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }

    }

    public void release(){
        trackIndices.clear();
        try {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
                mediaMuxer = null;
            }
        }catch (Exception e){
            mediaMuxer=null;
        }
      //  mediaMuxer = null;
    }

    public void setOrientationHint(int degrees) {
        mediaMuxer.setOrientationHint(degrees);
    }


}
