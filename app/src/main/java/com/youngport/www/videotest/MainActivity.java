package com.youngport.www.videotest;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private SurfaceView mSurfaceView;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private PLMediaPlayer mMediaPlayer;
    private LinearLayout loadingLl_videoPlay;
    private AVOptions mAVOptions;
    // Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_SEEK = 3;
    private int mTouchAction;
    private int mSurfaceYDisplayRange;
    private float mTouchY, mTouchX, mVol;
    private AudioManager audioManager;
    private int maxVolume, currentVolume;
    private boolean mIsFirstBrightnessGesture = true;
    private Toast mToast;
    private TextView timeTv, totalTv;
    private ImageButton playBtn;
    private SeekBar seekBar;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mSurfaceView = (SurfaceView) findViewById(R.id.SurfaceView);
        mSurfaceView.getHolder().addCallback(mCallback);
        loadingLl_videoPlay = (LinearLayout) findViewById(R.id.loadingLl_videoPlay);
        timeTv = (TextView) findViewById(R.id.timeTv);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        totalTv = (TextView) findViewById(R.id.totalTv);
        playBtn = (ImageButton) findViewById(R.id.playBtn);
        mAVOptions = new AVOptions();
        mAVOptions.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        mAVOptions.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        mAVOptions.setInteger(AVOptions.KEY_LIVE_STREAMING, 0);
        mAVOptions.setInteger(AVOptions.KEY_MEDIACODEC, 0);
        mAVOptions.setInteger(AVOptions.KEY_START_ON_PREPARED, 1);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 获取系统最大音量
        currentVolume = maxVolume * 2 / 3;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
        getAudioValue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null) {
            play();
        }
    }

    private void play() {
        if (!mMediaPlayer.isPlaying()) {
            playBtn.setImageResource(R.drawable.ic_pause);
            mMediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause();
    }

    private void pause() {
        if (mMediaPlayer.isPlaying()) {
            playBtn.setImageResource(R.drawable.ic_play);
            mMediaPlayer.pause();
        }
    }

    public void onClickPlay(View v) {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                pause();
            } else {
                play();
            }
        }
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            prepare();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mSurfaceWidth = width;
            mSurfaceHeight = height;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            release();
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                timeTv.setText(millisToString(mMediaPlayer.getCurrentPosition()));
                seekBar.setProgress((int) mMediaPlayer.getCurrentPosition());
            }
            mHandler.postDelayed(this, 1000);
        }
    };

    private PLMediaPlayer.OnPreparedListener mOnPreparedListener = new PLMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(PLMediaPlayer mp) {
            if (mMediaPlayer != null) {
                play();
                mHandler.post(runnable);
                timeTv.setText(millisToString(mMediaPlayer.getCurrentPosition()));
                totalTv.setText(millisToString(mMediaPlayer.getDuration()));
                seekBar.setMax((int) mMediaPlayer.getDuration());
                seekBar.setProgress((int) mMediaPlayer.getCurrentPosition());
            }
        }
    };

    private PLMediaPlayer.OnInfoListener mOnInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer mp, int what, int extra) {
            switch (what) {
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    loadingLl_videoPlay.setVisibility(View.VISIBLE);
                    break;
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_END:
                case PLMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    loadingLl_videoPlay.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    private void prepare() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            return;
        }
        try {
            mMediaPlayer = new PLMediaPlayer(mAVOptions);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnInfoListener(mOnInfoListener);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            seekBar.setOnSeekBarChangeListener(mSeekListener);
            // set replay if completed
            // mMediaPlayer.setLooping(true);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setDataSource("http://");
//            mMediaPlayer.setDataSource("file://");
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mMediaPlayer.seekTo(progress);
                timeTv.setText(millisToString(progress));
            }
        }
    };

    private PLMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new PLMediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(PLMediaPlayer mp, int width, int height) {
            // resize the display window to fit the screen
            if (width != 0 && height != 0) {
                float ratioW = (float) width / (float) mSurfaceWidth;
                float ratioH = (float) height / (float) mSurfaceHeight;
                float ratio = Math.max(ratioW, ratioH);
                width = (int) Math.ceil((float) width / ratio);
                height = (int) Math.ceil((float) height / ratio);
                FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(width, height);
                layout.gravity = Gravity.CENTER;
                mSurfaceView.setLayoutParams(layout);
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        DisplayMetrics screen = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screen);
        if (mSurfaceYDisplayRange == 0)
            mSurfaceYDisplayRange = Math.min(screen.widthPixels,
                    screen.heightPixels);
        float y_changed = event.getRawY() - mTouchY;
        float x_changed = event.getRawX() - mTouchX;

        // coef is the gradient's move to determine a neutral zone
        float coef = Math.abs(y_changed / x_changed);
        float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Audio
                mTouchY = event.getRawY();
                mVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mTouchAction = TOUCH_NONE;
                // Seek
                mTouchX = event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                // No volume/brightness action if coef < 2
                if (coef > 2) {
                    // Volume (Up or Down - Right side)
                    if (mTouchX > (screen.widthPixels / 2)) {
                        doVolumeTouch(y_changed);
                    }
                    // Brightness (Up or Down - Left side)
                    if (mTouchX < (screen.widthPixels / 2)) {
                        doBrightnessTouch(y_changed);
                    }
                    // Extend the overlay for a little while, so that it doesn't
                    // disappear on the user if more adjustment is needed. This
                    // is because on devices with soft navigation (e.g. Galaxy
                    // Nexus), gestures can't be made without activating the UI.
                }
                // Seek (Right or Left move)
                doSeekTouch(coef, xgesturesize, false);
                break;
            case MotionEvent.ACTION_UP:
                // Seek
                doSeekTouch(coef, xgesturesize, true);
                break;
        }
        return mTouchAction != TOUCH_NONE;
    }

    private void doSeekTouch(float coef, float gesturesize, boolean seek) {
        // No seek action if coef > 0.5 and gesturesize < 1cm
        if (coef > 0.5 || Math.abs(gesturesize) < 1)
            return;
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK)
            return;
        mTouchAction = TOUCH_SEEK;
        long length = mMediaPlayer.getDuration();
        long time = mMediaPlayer.getCurrentPosition();

        // Size of the jump, 10 minutes max (600000), with a bi-cubic
        // progression, for a 8cm gesture
        int jump = (int) (Math.signum(gesturesize) * ((600000 * Math.pow(
                (gesturesize / 8), 4)) + 3000));

        // Adjust the jump
        if ((jump > 0) && ((time + jump) > length))
            jump = (int) (length - time);
        if ((jump < 0) && ((time + jump) < 0))
            jump = (int) -time;

        // Jump !
        if (seek && length > 0)
            mMediaPlayer.seekTo(time + jump);

        if (length > 0)
            // Show the jump's size
            toastData(String.format("%s%s (%s)", jump >= 0 ? "+" : "", millisToString(jump), millisToString(time + jump)));
    }

    private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME)
            return;
        int delta = -(int) ((y_changed / mSurfaceYDisplayRange) * maxVolume);
        int vol = (int) Math.min(Math.max(mVol + delta, 0), maxVolume);
        if (delta != 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
            getAudioValue();
        }
    }

    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
        if (mIsFirstBrightnessGesture)
            initBrightnessTouch();
        mTouchAction = TOUCH_BRIGHTNESS;

        // Set delta : 0.07f is arbitrary for now, it possibly will change in
        // the future
        float delta = -y_changed / mSurfaceYDisplayRange * 0.07f;

        // Estimate and adjust Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = Math.min(
                Math.max(lp.screenBrightness + delta, 0.01f), 1);

        // Set Brightness
        getWindow().setAttributes(lp);
        toastData(getString(R.string.brightness) + '\u00A0' + Math.round(lp.screenBrightness * 15));
    }

    private void initBrightnessTouch() {
        float brightnessTemp = 0.01f;
        // Initialize the layoutParams screen brightness
        try {
            brightnessTemp = android.provider.Settings.System.getInt(
                    getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightnessTemp;
        getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }

    private String millisToString(long millis) {
        millis = Math.abs(millis);
        millis /= 1000;
        int sec = (int) (millis % 60);
        millis /= 60;
        int min = (int) (millis % 60);
        millis /= 60;
        int hours = (int) millis;
        String time;
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        format.applyPattern("00");
        if (millis > 0)
            time = hours + ":" + format.format(min) + ":" + format.format(sec);
        else
            time = min + ":" + format.format(sec);
        return time;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // 音量减小
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getAudioValue();
                    }
                }, 100);
                return false;

            // 音量增大
            case KeyEvent.KEYCODE_VOLUME_UP:
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getAudioValue();
                    }
                }, 100);
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void getAudioValue() {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // 获取当前值
        toastData(getString(R.string.volume) + '\u00A0' + currentVolume * 100 / maxVolume + " %");
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void toastData(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        } else {
            // 当toast对象存在时，修改文本和显示的时间，不要重新创建对象，多个Toast只显示最后的个toast的时
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_LONG);
        }
        mToast.show();
    }
}
