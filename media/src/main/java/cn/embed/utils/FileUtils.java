package cn.embed.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by shangdongzhou on 2017/9/25.
 */

public class FileUtils {
    //在应用文件夹下创建文件夹
    public static File CreatFolderInAPP(String FolderName) {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            System.out.println("SD存在");
            File file = new File(FolderName);
            if (file.exists()) {
                System.out.println("文件已经存在");
            } else {
                file.mkdirs();
            }
            return file;
        } else {
            System.out.println("没有发现SD卡");
        }
        return null;
    }


    public String CreatFileInAPP(File Folder, String FilserName) {
        String file = Folder.getPath() + Folder.separator + FilserName;
        return file;
    }


}
