package com.example.acceuil;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

public class StreamPlayerActivity extends AppCompatActivity {

    private StyledPlayerView playerView;
    private ExoPlayer player;
    private ProgressBar loadingProgress;
    private TextView titleView;
    private TextView streamerView;
    private TextView viewerCountView;
    private boolean isFullscreen = false;
    private String streamUrl; // STOCKER L'URL ICI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_player);

        // Garder l'écran allumé
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialiser les vues
        playerView = findViewById(R.id.player_view);
        loadingProgress = findViewById(R.id.loading_progress);
        titleView = findViewById(R.id.stream_title);
        streamerView = findViewById(R.id.streamer_name);
        viewerCountView = findViewById(R.id.viewer_count);

        // Récupérer les données DE L'INTENT
        String streamTitle = getIntent().getStringExtra("STREAM_TITLE");
        String streamerName = getIntent().getStringExtra("STREAMER_NAME");
        int viewerCount = getIntent().getIntExtra("VIEWER_COUNT", 0);
        streamUrl = getIntent().getStringExtra("STREAM_URL"); // RÉCUPÉRER L'URL

        // Afficher les infos
        titleView.setText(streamTitle);
        streamerView.setText("Streamer: " + streamerName);
        viewerCountView.setText(viewerCount + " viewers");

        // Initialiser ExoPlayer
        initializePlayer();

        // Charger le stream AVEC L'URL DE L'INTENT
        if (streamUrl != null && !streamUrl.isEmpty()) {
            loadStream(streamUrl); // UTILISER LA VRAIE URL
        } else {
            // Fallback si pas d'URL
            Toast.makeText(this, "No stream URL provided", Toast.LENGTH_LONG).show();
            loadStream("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8");
        }

        // Boutons
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_fullscreen).setOnClickListener(v -> toggleFullscreen());
        findViewById(R.id.btn_chat).setOnClickListener(v -> openChat());
    }

    private void initializePlayer() {
        // Créer le player
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Listeners
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        loadingProgress.setVisibility(View.VISIBLE);
                        break;
                    case Player.STATE_READY:
                        loadingProgress.setVisibility(View.GONE);
                        break;
                    case Player.STATE_ENDED:
                        Toast.makeText(StreamPlayerActivity.this, "Stream ended", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(StreamPlayerActivity.this,
                        "Error loading stream: " + error.getMessage(), Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }
        });
    }

    private void loadStream(String streamUrl) {
        Toast.makeText(this, "Loading: " + streamUrl, Toast.LENGTH_SHORT).show();

        if (streamUrl == null || streamUrl.isEmpty()) {
            Toast.makeText(this, "No stream URL available", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // Détecter le type de stream
            if (streamUrl.toLowerCase().endsWith(".m3u8") || streamUrl.contains("m3u8")) {
                // HLS Stream
                loadHlsStream(streamUrl);
            } else if (streamUrl.startsWith("rtmp://") || streamUrl.startsWith("rtmps://")) {
                // RTMP Stream (nécessite conversion)
                loadRtmpStream(streamUrl);
            } else if (streamUrl.startsWith("rtsp://")) {
                // RTSP Stream
                loadRtspStream(streamUrl);
            } else {
                // Essayer comme HLS par défaut
                loadHlsStream(streamUrl);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();

            // Fallback to test stream
            Toast.makeText(this, "Using fallback stream", Toast.LENGTH_SHORT).show();
            loadHlsStream("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8");
        }
    }

    private void loadHlsStream(String hlsUrl) {
        try {
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(hlsUrl)));

            player.setMediaSource(hlsMediaSource);
            player.prepare();
            player.setPlayWhenReady(true);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load HLS stream", e);
        }
    }

    private void loadRtmpStream(String rtmpUrl) {
        // RTMP direct n'est pas supporté par ExoPlayer
        // On peut essayer de convertir RTMP en HLS via un proxy
        // Pour l'instant, on utilise un fallback
        Toast.makeText(this,
                "RTMP streams need a proxy server. Using test stream instead.",
                Toast.LENGTH_LONG).show();

        loadHlsStream("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8");
    }

    private void loadRtspStream(String rtspUrl) {
        try {
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            RtspMediaSource rtspMediaSource = new RtspMediaSource.Factory()
                    .createMediaSource(MediaItem.fromUri(Uri.parse(rtspUrl)));

            player.setMediaSource(rtspMediaSource);
            player.prepare();
            player.setPlayWhenReady(true);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load RTSP stream", e);
        }
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            // Sortir du plein écran
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        } else {
            // Entrer en plein écran
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        }
        isFullscreen = !isFullscreen;
    }

    private void openChat() {
        // TODO: Implémenter le chat
        Toast.makeText(this, "Chat feature coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}