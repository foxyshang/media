package cn.embed.media.surfaceEncoder.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by shangdongzhou on 2017/9/17.
 */

public class FitCameraSurfaceView extends CameraSurfaceView {
    public FitCameraSurfaceView(Context context) {
        super(context);
        isSquare = false;
    }

    public FitCameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        isSquare = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


}
