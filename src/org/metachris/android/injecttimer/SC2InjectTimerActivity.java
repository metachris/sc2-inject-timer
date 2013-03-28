package org.metachris.android.injecttimer;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SC2InjectTimerActivity extends Activity {
    private static String TAG = "SC2InjectTimer Main";
    protected static final int DOUBLE_CLICK_MAX_DELAY = 500;
    protected static final int TIMEOUT_SECONDS = 32;
    protected static final int WARNING_AHEAD_SECONDS = 4;
    protected static final String PREF_SOUND_TICKTOCK = "ticktock";
    protected static final String SOUND_TICKTOCK_DEFAULT = "content://media/internal/audio/media/6";

    // Helper for double tap
    private long firstTapTime = 0;

    // Bindings
    private LinearLayout ll1;
    private TextView tv_status;
    private TextView tv_timer;

    private Handler mHandler;
    final MediaPlayer mp = new MediaPlayer();

    // Status
    private boolean isRunning = false;
    private long seconds_remaining = TIMEOUT_SECONDS;
    private Uri sound_ticktock;

    /**
     * timerRunnable runs every minute
     */
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(timerRunnable, 1000);
            seconds_remaining -= 1;
            // Reset
            if (seconds_remaining == 0) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                long[] i = {0, 300, 50, 300};
                v.vibrate(i, -1);
                //v.vibrate(1000);
                seconds_remaining = TIMEOUT_SECONDS;
            } else if (seconds_remaining <= WARNING_AHEAD_SECONDS) {
                mp.start();
            }
            showTimer();
        }
    };

    private SharedPreferences sharedPref;
    private AudioManager audio;

    private void showTimer() {
        tv_timer.setText(String.valueOf(seconds_remaining));
        tv_timer.setTextColor((isRunning) ? Color.GREEN : Color.WHITE);
    }

    private void pause() {
        mHandler.removeCallbacks(timerRunnable);
        isRunning = false;
        showTimer();
    }

    private void resume() {
        mHandler.postDelayed(timerRunnable, 1000);
        isRunning = true;
        showTimer();
    }

    private void togglePause() {
        if (isRunning)
            pause();
        else
            resume();
    }

    private void onTap() {
        Log.v(TAG, "tap");
        if ((firstTapTime == 0) || ((SystemClock.uptimeMillis() - firstTapTime) > DOUBLE_CLICK_MAX_DELAY)) {
            firstTapTime = SystemClock.uptimeMillis();
            togglePause();
        } else {
            Log.v(TAG, "diff: " + (SystemClock.uptimeMillis() - firstTapTime));
            if ((SystemClock.uptimeMillis() - firstTapTime) <= DOUBLE_CLICK_MAX_DELAY) {
                Log.v(TAG, "double tap");
                seconds_remaining = TIMEOUT_SECONDS;
                showTimer();
                mHandler.removeCallbacks(timerRunnable);
                resume();
            }
            firstTapTime = 0;
        }
    }

    private void setupMediaPlayer() {
        try {
            mp.setDataSource(this, sound_ticktock);
            mp.prepare();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        tv_status = (TextView) findViewById(R.id.tvStatus);
        tv_timer = (TextView) findViewById(R.id.tvTimer);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mHandler = new Handler();
        sharedPref = getSharedPreferences("FileName",MODE_PRIVATE);
        sound_ticktock = Uri.parse(sharedPref.getString(PREF_SOUND_TICKTOCK, SOUND_TICKTOCK_DEFAULT));
        setupMediaPlayer();

        ll1 = (LinearLayout) findViewById(R.id.ll1);
        ll1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(TAG, "long press");
                //stopTimer();
                return false;
            }
        });

        ll1.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        onTap();
                        break;

                    default:
                        break;
                }

                return false;
            }
        });

        showTimer();

//        mHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_sound:
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALL);
                startActivityForResult(intent, 1);
                return true;
            case R.id.help:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            // Sound picker
            sound_ticktock = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putString(PREF_SOUND_TICKTOCK, String.valueOf(sound_ticktock));
            prefEditor.commit();

            setupMediaPlayer();
            Log.v(TAG, "sound picker result: " + sound_ticktock);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
            switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_UP) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    }
                return true;
            default:
                return super.dispatchKeyEvent(event);
            }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
//      setContentView(R.layout.myLayout);
      Log.v(TAG, "confchange");
    }
}

