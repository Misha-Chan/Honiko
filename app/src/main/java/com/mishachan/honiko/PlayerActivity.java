package com.mishachan.honiko;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.mishachan.honiko.data.ApiClient;
import com.mishachan.honiko.data.CardItem;
import com.mishachan.honiko.ui.Ui;

import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "video_id";
    private static final String EXTRA_TITLE = "video_title";
    private static final String EXTRA_URL = "video_url";
    private static final String EXTRA_POSITION = "video_position";
    private static final String EXTRA_DURATION = "video_duration";
    private static final String EXTRA_CODEC = "video_codec";
    private static final String EXTRA_HEIGHT = "video_height";

    private static final long SEEK_STEP_MS = 10_000L;
    private static final long SAVE_INTERVAL_MS = 5_000L;
    private static final long CONTROLS_TIMEOUT_MS = 3_500L;

    private PlayerView playerView;
    private ExoPlayer player;
    private LinearLayout controls;
    private TextView titleView;
    private TextView positionView;
    private TextView durationView;
    private TextView codecView;
    private ProgressBar seekBar;
    private ProgressBar bufferingView;

    private Handler handler;
    private String videoId = "";
    private String streamUrl = "";
    private String videoCodec;
    private double fallbackDuration;
    private long lastSaveAt;
    private boolean errorDialogShown;
    private boolean released;

    private final Runnable hideControls = () -> {
        if (player != null && !player.isPlaying()) {
            return;
        }
        controls.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> controls.setVisibility(View.INVISIBLE))
                .start();
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateUi();
            if (player != null && player.isPlaying()) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastSaveAt >= SAVE_INTERVAL_MS) {
                    lastSaveAt = now;
                    saveProgress(false);
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    public static void start(Activity activity, CardItem card) {
        Intent intent = new Intent(activity, PlayerActivity.class);
        intent.putExtra(EXTRA_ID, card.video.id);
        intent.putExtra(EXTRA_TITLE, card.video.title);
        intent.putExtra(EXTRA_URL, card.streamUrl);
        intent.putExtra(EXTRA_POSITION, card.position);
        intent.putExtra(EXTRA_DURATION, card.video.duration);
        intent.putExtra(EXTRA_CODEC, card.video.videoCodec);
        intent.putExtra(EXTRA_HEIGHT, card.video.height == null ? 0 : card.video.height);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handler = new Handler(Looper.getMainLooper());
        playerView = findViewById(R.id.player_view);
        controls = findViewById(R.id.controls);
        titleView = findViewById(R.id.title);
        positionView = findViewById(R.id.position);
        durationView = findViewById(R.id.duration);
        codecView = findViewById(R.id.codec);
        seekBar = findViewById(R.id.seek);
        bufferingView = findViewById(R.id.buffering);

        videoId = getIntent().getStringExtra(EXTRA_ID);
        if (videoId == null) {
            videoId = "";
        }
        streamUrl = getIntent().getStringExtra(EXTRA_URL);
        if (streamUrl == null) {
            streamUrl = "";
        }
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        titleView.setText(title == null ? "" : title);
        videoCodec = getIntent().getStringExtra(EXTRA_CODEC);
        fallbackDuration = getIntent().getDoubleExtra(EXTRA_DURATION, 0d);
        int height = getIntent().getIntExtra(EXTRA_HEIGHT, 0);

        StringBuilder codecLabel = new StringBuilder();
        if (videoCodec != null && !videoCodec.isEmpty()) {
            codecLabel.append(videoCodec.toUpperCase(Locale.US));
        }
        if (height > 0) {
            if (codecLabel.length() > 0) {
                codecLabel.append(" • ");
            }
            codecLabel.append(height).append("p");
        }
        codecView.setText(codecLabel.toString());

        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setShutterBackgroundColor(Color.BLACK);

        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), true);
        player.setHandleAudioBecomingNoisy(true);
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                bufferingView.setVisibility(
                        state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                if (state == Player.STATE_ENDED) {
                    saveCompleted();
                    showControls();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    showControls();
                } else {
                    showControls();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                bufferingView.setVisibility(View.GONE);
                showPlaybackError(error);
            }
        });

        double initialPosition = getIntent().getDoubleExtra(EXTRA_POSITION, 0d);
        long startMs = (long) Math.max(0d, initialPosition * 1000d);
        if (startMs < 5000L) {
            startMs = 0L;
        }
        if (fallbackDuration > 0 && initialPosition > fallbackDuration - 15d) {
            startMs = 0L;
        }

        lastSaveAt = SystemClock.elapsedRealtime();
        prepare(startMs);
        showControls();
        handler.post(ticker);
        hideSystemUi();
    }

    private void prepare(long startMs) {
        if (player == null) {
            return;
        }
        errorDialogShown = false;
        MediaItem item = MediaItem.fromUri(streamUrl);
        player.setMediaItem(item, Math.max(0L, startMs));
        player.prepare();
        player.setPlayWhenReady(true);
        bufferingView.setVisibility(View.VISIBLE);
        showControls();
    }

    private void updateUi() {
        if (player == null) {
            return;
        }
        long position = Math.max(0L, player.getCurrentPosition());
        long duration = player.getDuration();
        if (duration <= 0L) {
            duration = (long) (fallbackDuration * 1000d);
        }
        if (duration > 0L) {
            int progress = (int) Math.min(1000L, position * 1000L / duration);
            seekBar.setProgress(Math.max(0, progress));
        } else {
            seekBar.setProgress(0);
        }
        positionView.setText(Ui.time(position / 1000d));
        durationView.setText(Ui.time(duration / 1000d));
    }

    private void showControls() {
        if (controls == null) {
            return;
        }
        handler.removeCallbacks(hideControls);
        controls.animate().cancel();
        controls.setVisibility(View.VISIBLE);
        controls.setAlpha(1f);
        if (player != null && player.isPlaying()) {
            handler.postDelayed(hideControls, CONTROLS_TIMEOUT_MS);
        }
    }

    private void togglePlay() {
        if (player == null) {
            return;
        }
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0L);
            player.play();
        } else if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        showControls();
    }

    private void seekBy(long deltaMs) {
        if (player == null) {
            return;
        }
        long duration = player.getDuration();
        if (duration <= 0L) {
            duration = (long) (fallbackDuration * 1000d);
        }
        long target = player.getCurrentPosition() + deltaMs;
        target = Math.max(0L, target);
        if (duration > 0L) {
            target = Math.min(duration, target);
        }
        player.seekTo(target);
        showControls();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    seekBy(-SEEK_STEP_MS);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    seekBy(SEEK_STEP_MS);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    showControls();
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_SPACE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    togglePlay();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    if (player != null) {
                        player.play();
                    }
                    showControls();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    if (player != null) {
                        player.pause();
                    }
                    showControls();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    saveProgress(true);
                    finish();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    seekBy(30_000L);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    seekBy(-30_000L);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    saveProgress(true);
                    finish();
                    return true;
                default:
                    showControls();
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void saveProgress(boolean force) {
        if (player == null || videoId.isEmpty()) {
            return;
        }
        long duration = player.getDuration();
        if (duration <= 0L) {
            duration = (long) (fallbackDuration * 1000d);
        }
        if (duration <= 0L) {
            return;
        }
        long position = Math.max(0L, player.getCurrentPosition());
        if (!force && position < 5000L) {
            return;
        }
        ApiClient.saveProgress(this, videoId, position / 1000d, duration / 1000d);
    }

    private void saveCompleted() {
        if (videoId.isEmpty() || player == null) {
            return;
        }
        long duration = player.getDuration();
        if (duration <= 0L) {
            duration = (long) (fallbackDuration * 1000d);
        }
        if (duration <= 0L) {
            return;
        }
        ApiClient.saveProgress(this, videoId, duration / 1000d, duration / 1000d);
    }

    private void showPlaybackError(PlaybackException error) {
        if (errorDialogShown || isFinishing() || released) {
            return;
        }
        errorDialogShown = true;
        String codec = videoCodec == null || videoCodec.isEmpty()
                ? "?" : videoCodec.toUpperCase(Locale.US);

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_error_title)
                .setMessage(getString(R.string.player_error_message, codec))
                .setPositiveButton(R.string.open_external, (dialog, which) -> openExternal())
                .setNeutralButton(R.string.retry, (dialog, which) -> {
                    errorDialogShown = false;
                    long position = player != null ? player.getCurrentPosition() : 0L;
                    prepare(position);
                })
                .setNegativeButton(R.string.close, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void openExternal() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(streamUrl), "video/*");
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.no_external_player, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
            saveProgress(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveProgress(true);
    }

    @Override
    protected void onDestroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        saveProgress(true);
        released = true;
        if (player != null) {
            player.release();
            player = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
