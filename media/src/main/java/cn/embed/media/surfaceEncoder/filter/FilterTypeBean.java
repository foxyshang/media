package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.graphics.Bitmap;

import cn.embed.media.surfaceEncoder.filter.BlurFilter.ImageFilterGaussianSingleBlur;

public class FilterTypeBean {
    private int filterType;
    private Bitmap filterBitmap;
    private boolean isWidth;
    public static final int FILTER_TYPE_LOOKUP = 1;           //查表法滤镜
    public static final int FILTER_TYPE_BLEND = 2;            //添加贴图
    public static final int FILTER_TYPE_BUFFING = 3;//buffing
    public static final int FILTER_TYPE_BILATERAL = 4;
    public static final int FILTER_TYPE_BEAUTY = 5;
    public IFilter iFilter;

    public FilterTypeBean(int filterType, Bitmap filterBitmap) {
        this.filterType = filterType;
        this.filterBitmap = filterBitmap;
    }

    public FilterTypeBean(int filterType, boolean isWidth) {
        this.filterType = filterType;
        this.isWidth = isWidth;
    }

    public int getFilterType() {
        return filterType;
    }

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    public Bitmap getFilterBitmap() {
        return filterBitmap;
    }

    public void setFilterBitmap(Bitmap filterBitmap) {
        this.filterBitmap = filterBitmap;
    }

    public IFilter getFilter(Context context) {
        switch (this.filterType) {
            case FILTER_TYPE_LOOKUP:
                iFilter = new ImageFilterBlendLookup(context, filterBitmap);
                break;
            case FILTER_TYPE_BLEND:
                iFilter = new ImageFilterBlend(context, filterBitmap);
                break;
            case FILTER_TYPE_BUFFING:
                iFilter = new ImageFilterGaussianSingleBlur(context, 0.3f, isWidth);
                break;
            case FILTER_TYPE_BILATERAL:
                iFilter = new ImageFilterBilateral(context);
                break;
            case FILTER_TYPE_BEAUTY:
                iFilter = new ImageFilterBeauty(context);
                break;
        }
        return iFilter;
    }


    public IFilter getThisFilter() {
        return iFilter;
    }
}
