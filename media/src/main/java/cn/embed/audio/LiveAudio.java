package cn.embed.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/***
 *直播打开microphone 获取音频流类
 */

public class LiveAudio extends ABAudio {
    private AudioRecord audioRecord;
    private static LiveAudio instance;
    private int bufferSize;
    private byte[] audioData;
    private Thread thread = null;
    private IPCMData dateListener;


    /**
     * 获取单例
     *
     * @return
     */
    public static synchronized LiveAudio getInstance() {
        if (instance == null) {
            instance = new LiveAudio();
        }
        return instance;
    }


    @Override
    public boolean start() {
        doStartAudioRecord();
        return true;
    }

    @Override
    public void end() {
        doStopRecorder();

    }


    public void setDateListener(IPCMData dateListener) {
        this.dateListener = dateListener;
    }


    /**
     * 停止录音机
     */
    private synchronized void doStopRecorder() {
        isRunning = false;
        if (audioRecord != null) {
            if (thread != null) {
                thread.interrupted();
                thread.isInterrupted();
                thread = null;
            }
            audioRecord.release();
            audioRecord = null;
        }
    }

    /**
     * 启动录音机
     */
    private void doStartAudioRecord() {
        doStopRecorder();
        bufferSize = AudioRecord.getMinBufferSize(audioProfile.getSampleRate(), AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, audioProfile.getSampleRate(), AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioData = new byte[bufferSize];
        audioRecord.startRecording();
        isRunning = true;
        if (thread != null) {
            thread = null;
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    int bufferReadResult = audioRecord.read(audioData, 0, bufferSize);
                    if (bufferReadResult > 0) {
                        if (dateListener != null) {
                            dateListener.audioData(audioData);
                        }
                    }
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        thread.start();
    }

    public static interface IPCMData {
        public void audioData(byte[] data);

    }
}
