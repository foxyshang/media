package cn.embed.videohandle.audioMix;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by cj on 2017/6/26 .
 */

public class Constants {

    public static String getBaseFolder(Context context) {
        String baseFolder = Environment.getExternalStorageDirectory() + "/Codec/";
        File f = new File(baseFolder);
        if (!f.exists()) {
            boolean b = f.mkdirs();
            if (!b) {
                baseFolder = context.getExternalFilesDir(null).getAbsolutePath() + "/";
            }
        }
        return baseFolder;
    }

    //获取VideoPath
    public static String getPath(Context context, String path, String fileName) {
        String p = getBaseFolder(context) + path;
        File f = new File(p);
        if (!f.exists() && !f.mkdirs()) {
            return getBaseFolder(context) + fileName;
        }
        return p + fileName;
    }
}
