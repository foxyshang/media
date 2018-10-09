package cn.embed.media.surfaceEncoder.filter;

import android.content.Context;

import cn.embed.media.R;


public class FilterManager {

    private static int mCurveIndex;
    private static int[] mCurveArrays = new int[]{
            R.raw.cross_1, R.raw.cross_2, R.raw.cross_3, R.raw.cross_4, R.raw.cross_5,
            R.raw.cross_6, R.raw.cross_7, R.raw.cross_8, R.raw.cross_9, R.raw.cross_10,
            R.raw.cross_11, R.raw.cross_edit,
    };

    private FilterManager() {
    }

    public static IFilter getCameraFilter(FilterType filterType, Context context) {
        switch (filterType) {
            case Normal:
                return new CameraFilter(context);

            default:
                return new CameraFilter(context);
          /*  //混合
            case Blend:
                return new CameraFilterBlend(context, R.drawable.app_icon);
            //柔光混合
            case SoftLight:
                return new CameraFilterBlendSoftLight(context, R.drawable.mask);*/
            //色调曲线
           /* case ToneCurve:
                mCurveIndex++;
             //   mCurveIndex=0;
                if (mCurveIndex > 10) {
                    mCurveIndex = 0;
                }
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[mCurveIndex]));*/
            //美颜
            case Beauty:
                return new CameraFilterBeauty(context);
            //磨皮
            case Bilateral:
                return new CameraFilterBilateral(context);
            case Gray:
                return new CameraFilterGray(context);
            case PinchDistortion:
                return new CameraFilterPinchDistortion(context);
            case CROSS_1:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[1]));
            case CROSS_2:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[2]));
            case CROSS_3:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[3]));
            case CROSS_4:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[4]));
            case CROSS_5:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[5]));
            case CROSS_6:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[6]));
            case CROSS_7:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[7]));
            case CROSS_8:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[8]));
            case CROSS_9:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[9]));
            case CROSS_10:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[10]));
            case CROSS_11:
                return new CameraFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[0]));


        }

    }

    public static IFilter getImageFilter(FilterType filterType, Context context) {
        switch (filterType) {
            case Normal:
            default:
                return new ImageFilter(context);
           /* case Blend:
                return new ImageFilterBlend(context, R.drawable.mask);
            case SoftLight:
                return new ImageFilterBlendSoftLight(context, R.drawable.mask);*/
        /*    case ToneCurve:
                mCurveIndex++;
                if (mCurveIndex > 10) {
                    mCurveIndex = 0;
                }
                return new ImageFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[mCurveIndex]));*/
            case CROSS_1:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[1]));
            case CROSS_2:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[2]));
            case CROSS_3:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[3]));
            case CROSS_4:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[4]));
            case CROSS_5:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[5]));
            case CROSS_6:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[6]));
            case CROSS_7:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[7]));
            case CROSS_8:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[8]));
            case CROSS_9:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[9]));
            case CROSS_10:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[10]));
            case CROSS_11:
                return new ImageFilterToneCurve(context, context.getResources().openRawResource(mCurveArrays[11]));

        }
    }

    public enum FilterType {
        Normal, Blend, SoftLight,/* ToneCurve,*/Beauty, Bilateral, Gray, PinchDistortion, CROSS_1, CROSS_2, CROSS_3, CROSS_4, CROSS_5, CROSS_6, CROSS_7, CROSS_8, CROSS_9, CROSS_10, CROSS_11, CROSS_12
    }
}
