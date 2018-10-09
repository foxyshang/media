package cn.embed.videohandle.muxer;

/**
 * Created by shangdongzhou on 2017/5/23.
 * 视频写入时的轨
 */

public class TrackIndex {
    public TrackIndex() {
        this.trackIndex = -1;
        this.isOver = false;
    }

    private int trackIndex = -1;              //视频轨
    private boolean isOver = false;           //该轨的数据是否已经写完

    public int getTrackIndex() {
        return trackIndex;
    }

    public void setTrackIndex(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public boolean isOver() {
        return isOver;
    }

    public void setOver(boolean over) {
        isOver = over;
    }
}
