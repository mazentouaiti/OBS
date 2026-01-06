package com.obs.mobile;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.obs.mobile.streaming.ChatManager;
import com.obs.mobile.streaming.StreamDatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * StreamPlaybackActivity - Simple playback activity that plays a local MP4 and shows chat replay.
 * Expects intent extras: "stream_id", "stream_path", "stream_title".
 */
public class StreamPlaybackActivity extends AppCompatActivity {
    private static final String TAG = "StreamPlaybackActivity";

    private VideoView videoView;
    private SeekBar seekBar;
    private Button btnPlayPause;
    private TextView tvTitle, tvTime, tvDuration;
    private RecyclerView rvChat;

    private StreamDatabaseHelper dbHelper;
    private ChatReplayAdapter chatAdapter;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private String streamId;
    private String streamPath;

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (videoView != null && videoView.isPlaying()) {
                int pos = videoView.getCurrentPosition();
                seekBar.setProgress(pos);
                tvTime.setText(formatTime(pos));
            }
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read intent extras
        streamId = getIntent().getStringExtra("stream_id");
        streamPath = getIntent().getStringExtra("stream_path");
        String title = getIntent().getStringExtra("stream_title");

        // Simple programmatic layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvTitle.setText(title != null ? title : "Playback");
        tvTitle.setPadding(16,16,16,8);
        root.addView(tvTitle);

        videoView = new VideoView(this);
        videoView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 500));
        root.addView(videoView);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        seekBar = new SeekBar(this);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        controls.addView(seekBar);

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        times.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tvTime = new TextView(this);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvTime.setText("00:00");
        times.addView(tvTime);

        tvDuration = new TextView(this);
        tvDuration.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvDuration.setText("00:00");
        times.addView(tvDuration);

        controls.addView(times);

        btnPlayPause = new Button(this);
        btnPlayPause.setText("Play");
        controls.addView(btnPlayPause);

        root.addView(controls);

        rvChat = new RecyclerView(this);
        rvChat.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        root.addView(rvChat);

        setContentView(root);

        // Initialize helpers
        dbHelper = new StreamDatabaseHelper(this);
        chatAdapter = new ChatReplayAdapter(new ArrayList<>());
        rvChat.setAdapter(chatAdapter);

        // Wire controls
        btnPlayPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlayPause.setText("Play");
            } else {
                videoView.start();
                btnPlayPause.setText("Pause");
                uiHandler.post(progressUpdater);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser = false;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean isFromUser) {
                fromUser = isFromUser;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (fromUser && videoView != null) {
                    videoView.seekTo(seekBar.getProgress());
                }
            }
        });

        // Load video
        if (streamPath == null || streamPath.isEmpty()) {
            Toast.makeText(this, "Invalid stream path", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No stream_path provided in intent");
        } else {
            try {
                videoView.setVideoURI(Uri.parse(streamPath));
                videoView.setOnPreparedListener(mp -> {
                    int duration = mp.getDuration();
                    seekBar.setMax(duration);
                    tvDuration.setText(formatTime(duration));
                });
                videoView.setOnCompletionListener(mp -> {
                    btnPlayPause.setText("Play");
                    seekBar.setProgress(0);
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load video", e);
                Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
            }
        }

        // Load chat messages from DB
        loadChatMessages();
    }

    private void loadChatMessages() {
        new Thread(() -> {
            try {
                if (streamId != null) {
                    List<ChatManager.ChatMessage> messages = dbHelper.getChatMessages(streamId);
                    runOnUiThread(() -> chatAdapter.setChatMessages(messages));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading chat messages", e);
            }
        }).start();
    }

    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int mins = seconds / 60;
        int hrs = mins / 60;
        seconds = seconds % 60;
        mins = mins % 60;
        if (hrs > 0) return String.format(Locale.US, "%d:%02d:%02d", hrs, mins, seconds);
        return String.format(Locale.US, "%02d:%02d", mins, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) videoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        if (dbHelper != null) dbHelper.close();
    }
}

