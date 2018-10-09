package cn.embed.media.surfaceEncoder.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class AutoFitGLSurfaceView extends GLSurfaceView {

    protected int mRatioWidth = 0;
    protected int mRatioHeight = 0;
    protected boolean isSquare = true;

    public AutoFitGLSurfaceView(Context context) {
        super(context);
    }

    public AutoFitGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (isSquare) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            height = MeasureSpec.getSize(widthMeasureSpec);
        }

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    public boolean isSquare() {
        return isSquare;
    }


    public void setSquare(boolean isSquare) {
        this.isSquare = isSquare;
    }
}
