package cn.pli.media;


import android.app.Application;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.logging.Logger;

import cn.embed.media.Media;


/**
 * Created by pc on 2017/9/19.
 */

public class App extends Application {

    public static int screenWidth;
    public static int screenHeight;
    public static float screenDensity;


    private static App app;

    public static App getApp() {
        return app;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Media.init(getApplicationContext());
        initScreenSize();
        this.app = this;

    }

    private void initScreenSize() {
        DisplayMetrics curMetrics = getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = curMetrics.widthPixels;
        screenHeight = curMetrics.heightPixels;
        screenDensity = curMetrics.density;
    }


}
