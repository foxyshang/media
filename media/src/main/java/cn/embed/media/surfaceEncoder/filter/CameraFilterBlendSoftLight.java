package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;


public class CameraFilterBlendSoftLight extends CameraFilterBlend {

    public CameraFilterBlendSoftLight(Context context, Bitmap drawable) {
        super(context, drawable);
    }

    @Override
    protected int createProgram(Context applicationContext) {

        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader_two_input,
                R.raw.fragment_shader_ext_blend_soft_light);
    }
}