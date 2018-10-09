package cn.embed.media.surfaceEncoder.filter.BlurFilter;

import android.content.Context;

import cn.embed.media.surfaceEncoder.filter.CameraFilter;
import cn.embed.media.surfaceEncoder.filter.FilterGroup;

public class CameraFilterGaussianBlur extends FilterGroup<CameraFilter> {

    public CameraFilterGaussianBlur(Context context, float blur) {
        super();
        addFilter(new CameraFilterGaussianSingleBlur(context, blur, false));
        addFilter(new CameraFilterGaussianSingleBlur(context, blur, true));
    }
}
