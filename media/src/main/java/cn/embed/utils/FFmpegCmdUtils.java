package cn.embed.utils;

import android.content.Context;
import android.os.Environment;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Created by shangdongzhou on 2017/7/11.
 */

public class FFmpegCmdUtils {
    /**
     * 链接视频
     *
     * @param paths
     */
    public static void videoConcat(Context context, ExecuteBinaryResponseHandler handler, List<String> paths, String outPutPath) {
        if (paths.size() > 0) {
            String txtfilePath, finalFileContent;
            txtfilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/picwall/tmp/" + UUID.randomUUID().toString()
                    + "mimiconcat.txt";
            File file = new File(txtfilePath);
            finalFileContent = "";
            for (String path : paths) {
                finalFileContent = finalFileContent + " file " + path + "\n";
            }
            try {
                CommonUtils.writeTxtFile(finalFileContent, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String cmd1 = "ffmpeg -f concat -safe 0 -i " + txtfilePath + " -c copy " + outPutPath;
            execute(context, handler, cmd1);
        }

    }

    /**
     * 裁剪视频
     *
     * @param inputPath
     * @param outputPath
     * @param startTime
     * @param endTime
     */
    public static void videoCut(Context context, ExecuteBinaryResponseHandler handler, String inputPath, String outputPath, int startTime, int endTime) {
        String cmd1 = "ffmpeg -y -ss " + startTime + " -t " + endTime + " -accurate_seek -i " + inputPath + " -codec copy -avoid_negative_ts 1 " + outputPath;
        execute(context, handler, cmd1);
    }

    /**
     * 裁剪音频
     *
     * @param inputPath
     * @param outputPath
     * @param startTime
     * @param endTime
     */
    public static void musicCut(Context context, ExecuteBinaryResponseHandler handler, String inputPath, String outputPath, int startTime, int endTime) {
        String cmd = "ffmpeg -i " + inputPath + " -ss " + startTime + " -t " + endTime + " -vn -y -acodec copy " + outputPath;
        execute(context, handler, cmd);


    }

    public static void getAudioFromVideo(Context context, ExecuteBinaryResponseHandler handler, String inVideoPath, String audioOutputPath) {
        String cmd = "ffmpeg -i " + inVideoPath + " -vn -y -acodec copy " + audioOutputPath;
        execute(context, handler, cmd);
    }

    public static void videoCut(Context context, final String inputPath, String outputPath, int startTime, int endTime, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        String cmd1 = "-y -ss " + startTime + " -t " + endTime + " -accurate_seek -i " + inputPath + " -codec copy -avoid_negative_ts 1 " + outputPath;
        final String[] command = cmd1.split(" ");
        execute(context, executeBinaryResponseHandler, command);
    }

    public static void execute(Context context, ExecuteBinaryResponseHandler executeBinaryResponseHandler, String[] command) {
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.execute(command, executeBinaryResponseHandler);
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }


    public static void execute(Context context, ExecuteBinaryResponseHandler executeBinaryResponseHandler, String command) {
        command = command.replace("ffmpeg", "");
        String[] commands = command.split(" ");
        execute(context, executeBinaryResponseHandler, commands);

    }


    public static void mixMusic(Context context, List<String> musics, String outputPath, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("-y ");
        for (String music : musics) {
            cmd.append("-i ");
            cmd.append(music);
            cmd.append(" ");
        }
        cmd.append("-filter_complex ");
        cmd.append("amix=inputs=");
        cmd.append(String.valueOf(musics.size()));
        cmd.append(":duration=first:dropout_transition=");
        cmd.append(String.valueOf(musics.size()));
        cmd.append(" ");
        cmd.append(outputPath);
        String cmd1 = cmd.toString();

        final String[] command = cmd1.split(" ");

        execute(context, executeBinaryResponseHandler, command);
    }


}
