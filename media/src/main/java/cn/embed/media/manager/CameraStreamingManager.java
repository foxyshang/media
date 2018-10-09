package cn.embed.media.manager;

import android.content.Context;
import android.hardware.Camera;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

import java.io.File;

import cn.embed.audio.RecorderAudio;
import cn.embed.utils.CommonUtils;
import cn.embed.utils.FFmpegCmdUtils;
import cn.embed.media.surfaceEncoder.camera.CameraController;
import cn.embed.renderer.CameraRecordRenderer;
import cn.embed.media.surfaceEncoder.video.EncoderConfig;
import cn.embed.media.surfaceEncoder.widget.CameraSurfaceView;
import cn.embed.utils.FileUtils;

public class CameraStreamingManager {
    private boolean isRecording = false;        // 录制状态
    private boolean isMute = false; // 是否进行静音操作，只发送视频，不发送音频，缺省值为非静音情况
    private StreamingProfile profile;
    private CameraSurfaceView glSurface;
    private RecorderAudio audioRecorder;
    private String outDir;
    private String outName;

    public CameraStreamingManager(CameraSurfaceView glSurface) {
        super();
        this.glSurface = glSurface;
        audioRecorder = RecorderAudio.getInstance();
    }


    /**
     * 推流准备
     */
    public void onPrepare(String dir, String name) {
        boolean isSquare = this.glSurface.isSquare();
        this.outDir = dir;
        this.outName = CommonUtils.getFilename(name);
        String audioTepName = outName + "tmp.m4a";
        String videoTepName = outName + "tmp.mp4";
        this.outName = name;

        FileUtils.CreatFolderInAPP(dir);


        StreamingProfile.AudioProfile audioProfile = new StreamingProfile.AudioProfile(44100, 64 * 1024, this.outDir + "/" + audioTepName);
        StreamingProfile.VideoProfile videoProfile =
                new StreamingProfile.VideoProfile(25, 2 * 1024 * 1024, 3, Camera.CameraInfo.CAMERA_FACING_FRONT, this.outDir + "/" + videoTepName);
        if (isSquare) {
            videoProfile.setHeight(640);
            videoProfile.setWidth(640);
        } else {
            videoProfile.setHeight(1280);
            videoProfile.setWidth(720);
        }
        StreamingProfile profile = new StreamingProfile();
        profile.setAudioProfile(audioProfile);
        profile.setVideoProfile(videoProfile);


        this.profile = profile;
        initEncode();
        audioRecorder.setConfig(profile.getAudioProfile());
    }


    public boolean ismIsCameraBackForward() {
        return glSurface.isCameraBackForward();
    }

    public void resetCamera(boolean ismIsCameraBackForward) {
        glSurface.resetCamera(ismIsCameraBackForward);
    }

    private void initEncode() {
        CameraRecordRenderer renderer = glSurface.getRenderer();
        renderer.setEncoderConfig(new EncoderConfig(profile.getVideoProfile().getWidth()
                , profile.getVideoProfile().getHeight(), profile.getVideoProfile().getReqBitrate()
                , new File(getOutputVideoPath())));
    }


    public void onPause() {
        glSurface.onPause();
    }


    public void onResume() {
        glSurface.onResume();
    }


    public void onDestory() {
        CameraController.getInstance().stopCamera();
    }


    public void setFilter(int index) {
        if (index > 97 || index < 0) {
            index = 0;
        }
        //	glSurface.setFilterWithConfig(FilterEffect.effectConfigs[index]);
    }


    public void switchCamera() {
        glSurface.switchCamera();
    }

    public void turnLightOn() {
        glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_TORCH);
    }

    public void turnLightOnTakePhoto() {
        glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_ON);
    }

    public void turnLightOff() {
        glSurface.setFlashLightMode(Camera.Parameters.FLASH_MODE_OFF);
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean isMute) {
        this.isMute = isMute;
    }

    public void setRecordingEnabled(Context context, ExecuteBinaryResponseHandler executeBinaryResponseHandler, boolean isRecording) {
        if (isRecording == this.isRecording) {
            return;
        }
        this.isRecording = isRecording;
        String videoPath = outDir + "/" + outName;
        glSurface.getRenderer().setRecordingEnabled(isRecording);
        if (isRecording){
            audioRecorder.start();
        }else{
            audioRecorder.end();
        }
        this.isRecording = isRecording;
        if (!isRecording) {
            FFmpegCmdUtils.execute(context, executeBinaryResponseHandler, "ffmpeg -y -i " + profile.getAudioProfile().getOutPutPath()
                    + " -i " + getOutputVideoPath() +
                    " -vcodec copy -acodec copy " +
                    videoPath);
        }
    }


    public String getOutputVideoPath() {
        return profile.getVideoProfile().getOutputPath();
    }

    public String getVideoPath() {
        return outDir + "/" + outName;
    }

    public void takePhoto(Camera.PictureCallback jpeg) {
        CameraController.getInstance().takePicture(null, null, jpeg);
    }


}
