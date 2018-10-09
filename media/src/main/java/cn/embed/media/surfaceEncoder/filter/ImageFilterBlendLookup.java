package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.support.annotation.DrawableRes;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;


public class ImageFilterBlendLookup extends ImageFilterBlend {

    public ImageFilterBlendLookup(Context context, Bitmap drawableId) {
        super(context, drawableId);
    }

    @Override
    protected int createProgram(Context applicationContext) {

        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader_two_input,
                R.raw.fragment_shader_2d_lookup);
    }

    @Override
    public int getTextureTarget() {
        return GLES10.GL_TEXTURE_2D;
    }
}