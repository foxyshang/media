package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;


/**
 * Created by foxy on 2016/8/17.
 */
public class CameraFilterBeauty extends CameraFilter {
     int singleStepOffset;

     static final float offset_array[] = {
            2, 2,
    };

    public CameraFilterBeauty(Context context) {
        super(context);
        offset_array[0] = offset_array[0] / 90;
        offset_array[1] = offset_array[1] / 160;
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader,
                R.raw.fragment_shader_beauty);
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();

        singleStepOffset = GLES20.glGetUniformLocation(mProgramHandle, "singleStepOffset");
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);


        offset_array[0] = offset_array[0] / mIncomingWidth;
        offset_array[1] = offset_array[1] / mIncomingHeight;
        GLES20.glUniform2fv(singleStepOffset, 1, FloatBuffer.wrap(offset_array));
    }

    @Override
    protected void unbindTexture() {
        super.unbindTexture();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


}
