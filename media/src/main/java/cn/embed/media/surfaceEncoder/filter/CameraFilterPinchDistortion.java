package cn.embed.media.surfaceEncoder.filter;

import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;



/**
 * Created by foxy on 2016/8/19.
 * 收缩失真，凹面镜效果
 */
public class CameraFilterPinchDistortion extends CameraFilter {
    private float aspectRatio;
    private float radius;
    private float scale;
    private int mAspectRatio;
    private int mRadius;
    private int mScale;
    private int mCenter;


    private static final float center[] = {
            0.5f, 0.5f,
    };

    public CameraFilterPinchDistortion(Context context) {
        super(context);
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader,
                R.raw.fragment_shader_ext_pinchdistortion);
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();
        mAspectRatio = GLES20.glGetUniformLocation(mProgramHandle, "aspectRatio");
        mRadius = GLES20.glGetUniformLocation(mProgramHandle, "radius");
        mCenter = GLES20.glGetUniformLocation(mProgramHandle, "center");
        mScale = GLES20.glGetUniformLocation(mProgramHandle, "scale");
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);
        GLES20.glUniform2fv(mCenter, 1, center, 0);
        GLES20.glUniform1f(mAspectRatio, (float) mIncomingHeight / (float) mIncomingWidth);
        GLES20.glUniform1f(mRadius, 1.0f);
        GLES20.glUniform1f(mScale, 0.5f);
    }
}
