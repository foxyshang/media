package cn.pli.media;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.embed.media.manager.LiveCameraStreamingManager;
import cn.embed.media.manager.StreamingProfile;
import cn.embed.media.surfaceEncoder.filter.FilterTypeBean;
import cn.embed.media.surfaceEncoder.filter.ImageFilterBeauty;
import cn.embed.media.surfaceEncoder.widget.CameraSurfaceView;


public class CameraActivity extends Activity implements OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "foxy";
    CameraSurfaceView glSurfaceView = null;
    ImageButton shutterBtn;
    float previewRate = -1f;
    boolean openLight = false;
    private LiveCameraStreamingManager cameraManeger = null;
    private boolean isNet = false;
    int i = 0;
    private TextView tvNum;
    private boolean beauti = false;

    public static final String pushUrl = "rtmp://testpush.xiannu.tv/puti/123456?vhost=testlive.xiannu.tv&auth_key=1535442215-0-0-fda2d7c07d9788af875380ab8a83c0bd";


    private SeekBar sb_tone, sb_beauty, sb_bright;
    private static float minstepoffset = -10;
    private static float maxstepoffset = 10;
    private static float minToneValue = -5;
    private static float maxToneValue = 5;
    private static float minbeautyValue = 0;
    private static float maxbeautyValue = 2.5f;
    private static float minbrightValue = 0;
    private static float maxbrightValue = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        initUI();
        initViewParams();
        shutterBtn.setOnClickListener(new BtnListeners());

    }

    private void initUI() {
        glSurfaceView = (CameraSurfaceView) findViewById(R.id.camera_textureview);
        tvNum = (TextView) this.findViewById(R.id.tv_test);
        shutterBtn = (ImageButton) findViewById(R.id.btn_shutter);
        //glSurfaceView.presetCameraForward(false);
        sb_tone = (SeekBar) findViewById(R.id.sb_tone);
        sb_tone.setOnSeekBarChangeListener(this);
        sb_beauty = (SeekBar) findViewById(R.id.sb_beauty);
        sb_beauty.setOnSeekBarChangeListener(this);
        sb_bright = (SeekBar) findViewById(R.id.sb_bright);
        sb_bright.setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.sb_step)).setOnSeekBarChangeListener(this);


    }

    private void initViewParams() {

        int result = 0;
        int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = this.getResources().getDimensionPixelSize(resourceId);
        }
        int streamWidth = 360;
        float streamHeiht = 360 / ((App.screenWidth * 1f) / ((App.screenHeight - result) * 1f));

        int height = Math.round(streamHeiht);
        height = height - height % 4;

        StreamingProfile.Stream stream = new StreamingProfile.Stream("rtmp://www.opengl.cn:1935/live/foxy1", "");
        StreamingProfile.AudioProfile audioProfile = new StreamingProfile.AudioProfile(44100, 2 * 64 * 1024, null);


        StreamingProfile.VideoProfile videoProfile = new StreamingProfile.VideoProfile(25, 512 * 1024, 3, Camera.CameraInfo.CAMERA_FACING_FRONT, (int) height, streamWidth);


        StreamingProfile profile = new StreamingProfile();
        profile.setStream(stream);
        profile.setAudioProfile(audioProfile);
        profile.setVideoProfile(videoProfile);

        cameraManeger = new LiveCameraStreamingManager(CameraActivity.this, glSurfaceView);
        cameraManeger.onPrepare(profile);
        cameraManeger.openDevice();


        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                cameraManeger.startStreaming();

            }
        }).start();


    }

    int filterIndex = 3;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ImageFilterBeauty iFilter = (ImageFilterBeauty) glSurfaceView.getmCameraRenderer().getFilter(FilterTypeBean.FILTER_TYPE_BEAUTY);
        if (iFilter == null) {
            glSurfaceView.getmCameraRenderer().addFilter(new FilterTypeBean(FilterTypeBean.FILTER_TYPE_BEAUTY, true));
            return;
        }


        switch (seekBar.getId()) {
            case R.id.sb_step:
                iFilter.setTexelOffset(range(progress, minstepoffset, maxstepoffset));
                break;
            case R.id.sb_tone:
                iFilter.setToneLevel(range(progress, minToneValue, maxToneValue));
                break;
            case R.id.sb_beauty:
                iFilter.setBeautyLevel(range(progress, minbeautyValue, maxbeautyValue));
                break;
            case R.id.sb_bright:
                iFilter.setBrightLevel(range(progress, minbrightValue, maxbrightValue));
                break;
        }


    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class BtnListeners implements OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_shutter:
                    //	cameraManeger.reconnectStream();
                    beauti = !beauti;
                    if (beauti) {

                        glSurfaceView.getmCameraRenderer().addFilter(new FilterTypeBean(FilterTypeBean.FILTER_TYPE_BEAUTY, true));

                    } else {
                        glSurfaceView.getmCameraRenderer().cleanFilter(FilterTypeBean.FILTER_TYPE_BEAUTY);

                    }
                    /*if (beauti) {
                        glSurfaceView.changeFilter(FilterManager.FilterType.Beauty);
                    } else {
                        glSurfaceView.changeFilter(FilterManager.FilterType.Normal);
                    }*/
//				cameraManeger.switchCamera();
                    //cameraManeger.startStreaming();
                    break;
                default:
                    break;
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManeger.onResume();
    }

    @Override
    protected void onPause() {
        cameraManeger.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cameraManeger.onDestory();
        cameraManeger = null;
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

    }

    private String getPushStreamUrl(String streamName, long interval, String privateKey) {
        long time = System.currentTimeMillis() / 1000 + interval;
        String sstring = "/puti/" + streamName + "-" + time + "-0-0-" + privateKey;
        // return "rtmp://testpush.xiannu.tv/puti/" + streamName + "?vhost=testlive.xiannu.tv&auth_key=" + time + "-0-0-" + getMd5(sstring);
        return "rtmp://47.52.199.84:1935/live/foxy";

    }

    public static String getMd5(String str) {
        String result = "";
        if (str != null) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
                md5.update(str.getBytes());
                byte[] d = md5.digest();
                //转字符串
                String hs = "";
                String stmp = "";
                for (int n = 0; n < d.length; n++) {
                    stmp = (Integer.toHexString(d[n] & 0XFF));
                    if (stmp.length() == 1) {
                        hs = hs + "0" + stmp;
                    } else {
                        hs = hs + stmp;
                    }
                    if (n < d.length - 1) {
                        hs = hs + "";
                    }
                }
                result = hs.toLowerCase();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public Bitmap getFilter(int filterPath) {
        Bitmap filter;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;    // No pre-scaling
        filter = BitmapFactory.decodeResource(getResources(), filterPath, options);
        return filter;
    }

    protected float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 100.0f + start;
    }


}
