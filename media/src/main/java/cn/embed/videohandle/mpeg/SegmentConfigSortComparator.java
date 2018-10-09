package cn.embed.videohandle.mpeg;

import java.util.Comparator;

/**
 * Created by shangdongzhou on 2017/6/6.
 * 片段信息从小到大排序
 */

public class SegmentConfigSortComparator implements Comparator {
    @Override
    public int compare(Object object1, Object object2) {
        SegmentConfig sgementConfig1=(SegmentConfig) object1;
        SegmentConfig segmentConfig2=(SegmentConfig) object2;
        return sgementConfig1.getStartTime()-segmentConfig2.getStartTime();
    }
}
