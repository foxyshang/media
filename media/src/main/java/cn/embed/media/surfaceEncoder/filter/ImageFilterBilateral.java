package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.opengl.GLES20;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;


public class ImageFilterBilateral extends CameraFilterBilateral {

    public ImageFilterBilateral(Context applicationContext) {
        super(applicationContext);
    }

    @Override
    public int getTextureTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader_bilateral,
                R.raw.fragment_shader_2d_bilateral);
    }
}
