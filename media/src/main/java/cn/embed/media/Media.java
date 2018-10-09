package cn.embed.media;

import android.content.Context;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import cn.embed.media.surfaceEncoder.video.TextureMovieEncoder;
import cn.embed.videohandle.mpeg.MovieEditOperate;

/**
 * Created by shangdongzhou on 2018/3/26.
 */

public class Media {
    public static void init(Context context) {
        //opengl选择
        TextureMovieEncoder.getInstance();
        TextureMovieEncoder.initialize(context);
        //视频编辑
        MovieEditOperate.initialize(context);
        MovieEditOperate.getInstance().startOperate();

        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

    }
}
