package cn.embed.media.surfaceEncoder.filter;

import java.nio.FloatBuffer;

import android.content.Context;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;



/**
 * Created by Administrator on 2016/8/18.
 */
public class CameraFilterGray extends CameraFilter {

    public CameraFilterGray(Context context) {
        super(context);
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader,
                R.raw.fragment_shader_ext_gray);
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);

    }
}
