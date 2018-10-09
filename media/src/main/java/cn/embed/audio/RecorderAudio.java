package cn.embed.audio;

import android.media.MediaRecorder;

import java.io.IOException;

/**
 * Android microphone 录制声音并将录制完成的文件保存到本地
 */

public class RecorderAudio extends ABAudio {
    private static RecorderAudio instance;
    private MediaRecorder mediaRecorder;

    public static synchronized RecorderAudio getInstance() {
        if (instance == null) {
            instance = new RecorderAudio();
        }
        return instance;
    }


    @Override
    public boolean start() {
        if (isRunning) {
            return true;
        }
        startRecording();
        isRunning = true;
        return true;

    }

    @Override
    public void end() {
        stopRecording();
    }

    private void startRecording() {
        if (mediaRecorder != null) {
            mediaRecorder = null;
        }
        if (audioProfile == null) {
            throw new IllegalArgumentException("audioProfile is empty ");

        }
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(2);
        mediaRecorder.setAudioEncodingBitRate(audioProfile.getReqBitrate());
        mediaRecorder.setAudioSamplingRate(audioProfile.getSampleRate());
        mediaRecorder.setOutputFile(audioProfile.getOutPutPath());
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void stopRecording() {

        try {

            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
