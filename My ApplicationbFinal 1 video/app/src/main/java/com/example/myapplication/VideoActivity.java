package com.example.myapplication;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public class VideoActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_VIDEO = 1;
    private static final long HIDE_DELAY = 5000; // 5 seconds

    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private ImageButton btnPlayPause;
    private ImageButton btnSpeed;
    private SeekBar seekBar;
    private View speedBarLayout;
    private SeekBar speedSeekBar;
    private EditText customSpeedInput;
    private TextView currentSpeedText;
    private boolean isSpeedBarVisible = false;
    private boolean isFullscreen = false;
    private float defaultSpeed = 1.0f;
    private Handler hideSpeedBarHandler = new Handler();
    private Runnable hideSpeedBarRunnable = new Runnable() {
        @Override
        public void run() {
            hideSpeedBar();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        videoView = findViewById(R.id.videoView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnSpeed = findViewById(R.id.btnSpeed);
        seekBar = findViewById(R.id.seekBar);
        speedBarLayout = findViewById(R.id.speedBarLayout);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        customSpeedInput = findViewById(R.id.customSpeedInput);
        currentSpeedText = findViewById(R.id.currentSpeedText);

        // Start activity to pick video
        startActivityForResult(new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI), REQUEST_PICK_VIDEO);

        // Play/Pause button click listener
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        // Speed button click listener
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSpeedBar();
            }
        });

        // Submit button click listener
        findViewById(R.id.btnSubmitSpeed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String speedString = customSpeedInput.getText().toString();
                if (!speedString.isEmpty()) {
                    try {
                        float speed = Float.parseFloat(speedString);
                        if (speed >= 0.1f && speed <= 30.0f) { // Adjust the range as needed
                            speedSeekBar.setProgress((int) (speed * 100));
                            currentSpeedText.setText(String.format("%.2fx", speed));
                            changeSpeed(speed);
                        } else {
                            Toast.makeText(VideoActivity.this, "Please enter a speed between 0.1x and 30x", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(VideoActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(VideoActivity.this, "Please enter a speed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // SeekBar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set up listeners for video playback
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer = mp;
                seekBar.setMax(videoView.getDuration());
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                    }
                }, 0, 1000);
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(VideoActivity.this, "Video completed", Toast.LENGTH_SHORT).show();
            }
        });

        // Speed SeekBar change listener
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update current speed text
                float speed = progress / 100f;
                currentSpeedText.setText(String.format("%.2fx", speed));
                // Change playback speed
                changeSpeed(speed);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri selectedVideoUri = data.getData();
            if (selectedVideoUri != null) {
                videoView.setVideoURI(selectedVideoUri);
            }
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
        }
    }

    private void toggleSpeedBar() {
        if (isSpeedBarVisible) {
            hideSpeedBar();
        } else {
            showSpeedBar();
        }
    }

    private void showSpeedBar() {
        isSpeedBarVisible = true;
        speedBarLayout.setVisibility(View.VISIBLE);
        // Set a timer to hide the speed bar after HIDE_DELAY milliseconds
        hideSpeedBarHandler.removeCallbacks(hideSpeedBarRunnable);
        hideSpeedBarHandler.postDelayed(hideSpeedBarRunnable, HIDE_DELAY);
    }

    private void hideSpeedBar() {
        isSpeedBarVisible = false;
        speedBarLayout.setVisibility(View.GONE);
    }

    private void changeSpeed(float speed) {
        if (mediaPlayer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PlaybackParams params = mediaPlayer.getPlaybackParams();
                if (params == null) {
                    params = new PlaybackParams();
                }
                params.setSpeed(speed);
                mediaPlayer.setPlaybackParams(params);
                Toast.makeText(this, "Speed changed to " + speed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Speed control not supported on this device", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
