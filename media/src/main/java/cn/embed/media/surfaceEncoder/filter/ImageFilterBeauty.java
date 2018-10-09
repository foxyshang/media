package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.FloatBuffer;

import cn.embed.media.R;
import cn.embed.media.surfaceEncoder.gles.GlUtil;


/**
 * Created by foxy on 2016/8/17.
 */
public class ImageFilterBeauty extends CameraFilterBeauty {

    public static float toneLevel;
    public static float beautyLevel;
    public static float brightLevel;
    public static float texelOffset;

    private int paramsLocation;
    private int brightnessLocation;


    public ImageFilterBeauty(Context context) {
        super(context);
        toneLevel = -0.5f;
        beautyLevel = 1.2f;
        brightLevel = 0.47f;
        texelOffset = 2.0f;
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader,
                R.raw.fragment_shader_2d_beauty);
    }

    @Override
    public int getTextureTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();
        paramsLocation = GLES20.glGetUniformLocation(mProgramHandle, "params");
        brightnessLocation = GLES20.glGetUniformLocation(mProgramHandle, "brightness");


    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);


        setParams();
        setBrightLevel();
        setTexelOffset(texelOffset);

    }

    public void setParams() {

        float[] vector = new float[4];
        vector[0] = 1.0f - 0.6f * beautyLevel;
        vector[1] = 1.0f - 0.3f * beautyLevel;
        vector[2] = 0.1f + 0.3f * toneLevel;
        vector[3] = 0.1f + 0.3f * toneLevel;
        Log.d("media111", "vector[0]:" + vector[0] + ",vector[1]:" + vector[1] + ",vector[2]:" + vector[2] + ",vector[3]:" + vector[3]);
        setFloatVec4(paramsLocation, vector);
    }

    public void setTexelOffset(float texelOffset) {
        this.texelOffset = texelOffset;
        offset_array[0] = texelOffset / mIncomingWidth;
        offset_array[1] = texelOffset / mIncomingWidth;
        GLES20.glUniform2fv(singleStepOffset, 1, FloatBuffer.wrap(offset_array));
    }


    protected void setFloatVec4(final int location, final float[] arrayValue) {
        GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
    }

    public void setBrightLevel() {
        setFloat(brightnessLocation, 0.6f * (-0.5f + brightLevel));
    }

    protected void setFloat(final int location, final float floatValue) {
        GLES20.glUniform1f(location, floatValue);
    }


    public void setParams(float beauty, float tone) {
        this.beautyLevel = beauty;
        this.toneLevel = tone;
    }

    public void setBrightLevel(float brightLevel) {
        this.brightLevel = brightLevel;
    }

    public float getToneLevel() {
        return toneLevel;
    }

    public void setToneLevel(float toneLevel) {
        this.toneLevel = toneLevel;
    }

    public float getBeautyLevel() {
        return beautyLevel;
    }

    public void setBeautyLevel(float beautyLevel) {
        this.beautyLevel = beautyLevel;
    }

    public float getBrightLevel() {
        return brightLevel;
    }
}
