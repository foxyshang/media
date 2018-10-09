package cn.embed.media.surfaceEncoder.filter.BlurFilter;

import android.content.Context;

import cn.embed.media.surfaceEncoder.filter.CameraFilter;
import cn.embed.media.surfaceEncoder.filter.FilterGroup;

public class ImageFilterGaussianBlur extends FilterGroup<CameraFilter> {

    public ImageFilterGaussianBlur(Context context, float blur) {
        super();
        addFilter(new ImageFilterGaussianSingleBlur(context, blur, false));
        addFilter(new ImageFilterGaussianSingleBlur(context, blur, true));
    }
}
