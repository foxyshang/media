package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;


/**
 * Created by Administrator on 2016/8/17.
 */
public class CameraFilterBilateral extends CameraFilter {
    private float mDistanceNormalizationFactor;
    private int mDisFactorLocation;
    private int mSingleStepOffsetLocation;

    private static final float offset_array[] = {
            1, 1,
    };

    public CameraFilterBilateral(Context context) {
        super(context);
        offset_array[0] = offset_array[0] / 1;
        offset_array[1] = offset_array[1] / 1;
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader_bilateral,
                R.raw.fragment_shader_ext_bilateral);
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();
        mDisFactorLocation = GLES20.glGetUniformLocation(mProgramHandle, "distanceNormalizationFactor");
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(mProgramHandle, "singleStepOffset");
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);

        GLES20.glUniform2fv(mSingleStepOffsetLocation, 1, FloatBuffer.wrap(new float[]{1.0f / mIncomingWidth, 1.0f / mIncomingHeight}));
        GLES20.glUniform1f(mDisFactorLocation, 0.5f);
    }
}
