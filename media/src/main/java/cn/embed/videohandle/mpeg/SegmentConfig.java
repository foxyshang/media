package cn.embed.videohandle.mpeg;

import android.graphics.Bitmap;

import cn.embed.media.surfaceEncoder.filter.FilterManager;


/**
 * 视频分段配置信息
 */

public class SegmentConfig {
    private int startTime;                              //本段的开始时间  单位S          小于0，从0开始
    private int endTime;                                //本段的结束时间  单位 s         如果大于视频长度，为-1
    private int speed;                                  //编辑视频的播放速度             需大于0，小于0时自动调整为1
    private FilterManager.FilterType filterType;        //视频滤镜类型                   滤镜
    private Bitmap filterBitmap;

    //视频滤镜的覆盖的位图


    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public FilterManager.FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterManager.FilterType filterType) {
        this.filterType = filterType;
    }

    public Bitmap getFilterBitmap() {
        return filterBitmap;
    }

    public void setFilterBitmap(Bitmap filterBitmap) {
        this.filterBitmap = filterBitmap;
    }


    public SegmentConfig(int startTime, int endTime, int speed) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.speed = speed;
    }

    public SegmentConfig(int startTime, int endTime, int speed, FilterManager.FilterType filterType) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.speed = speed;
        this.filterType = filterType;
    }

    public SegmentConfig() {
        this.startTime = 0;
        this.endTime = -1;
        this.speed = 1;
        this.filterType = FilterManager.FilterType.Normal;
    }
}
