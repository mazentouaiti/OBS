package com.example.acceuil;

import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar loadingProgress;
    private TextView titleView;
    private TextView uploaderView;
    private boolean isFullscreen = false;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Garder l'écran allumé pendant la lecture
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialiser les vues
        videoView = findViewById(R.id.video_view);
        loadingProgress = findViewById(R.id.loading_progress);
        titleView = findViewById(R.id.video_title);
        uploaderView = findViewById(R.id.uploader_name);

        // Récupérer les données de l'intent
        String videoTitle = getIntent().getStringExtra("VIDEO_TITLE");
        String uploaderName = getIntent().getStringExtra("UPLOADER_NAME");
        videoUrl = getIntent().getStringExtra("VIDEO_URL");
        int views = getIntent().getIntExtra("VIEWS", 0);
        int duration = getIntent().getIntExtra("DURATION", 0);

        // Afficher les infos
        titleView.setText(videoTitle);
        uploaderView.setText("By: " + uploaderName + " • " + views + " views");

        // Configurer le VideoView
        setupVideoPlayer();

        // Bouton retour
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Bouton plein écran
        findViewById(R.id.btn_fullscreen).setOnClickListener(v -> toggleFullscreen());
    }

    private void setupVideoPlayer() {
        if (videoUrl == null || videoUrl.isEmpty()) {
            showError("No video URL provided");
            return;
        }

        // Afficher le loading
        loadingProgress.setVisibility(View.VISIBLE);

        // Configurer les contrôleurs média
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // Préparer l'URI vidéo
        Uri videoUri = Uri.parse(videoUrl);
        videoView.setVideoURI(videoUri);

        // Listeners
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                loadingProgress.setVisibility(View.GONE);
                videoView.start();

                // Ajuster l'aspect ratio
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                loadingProgress.setVisibility(View.GONE);
                showError("Error playing video");
                return true;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Vidéo terminée
                videoView.seekTo(0);
            }
        });
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            // Quitter le mode plein écran
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getSupportActionBar().show();
        } else {
            // Entrer en mode plein écran
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getSupportActionBar().hide();
        }
        isFullscreen = !isFullscreen;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        loadingProgress.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && !videoView.isPlaying()) {
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}