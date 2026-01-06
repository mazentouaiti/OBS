package com.example.acceuil;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewStreamActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar loadingProgress;
    private TextView titleView;
    private TextView streamerView;
    private FrameLayout webViewContainer;
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_stream);

        // Garder l'écran allumé
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialiser les vues
        webView = findViewById(R.id.web_view);
        loadingProgress = findViewById(R.id.loading_progress);
        titleView = findViewById(R.id.stream_title);
        streamerView = findViewById(R.id.streamer_name);
        webViewContainer = findViewById(R.id.web_view_container);

        // Récupérer les données
        String streamTitle = getIntent().getStringExtra("STREAM_TITLE");
        String streamerName = getIntent().getStringExtra("STREAMER_NAME");
        String platform = getIntent().getStringExtra("PLATFORM");
        String channelId = getIntent().getStringExtra("CHANNEL_ID");
        String videoId = getIntent().getStringExtra("VIDEO_ID");
        String streamUrl = getIntent().getStringExtra("STREAM_URL");

        // Afficher les infos
        titleView.setText(streamTitle != null ? streamTitle : "Live Stream");
        streamerView.setText("Streamer: " + (streamerName != null ? streamerName : "Unknown"));

        // Configurer le WebView
        setupWebView();

        // Charger le stream
        if ("youtube".equalsIgnoreCase(platform) && videoId != null) {
            loadYouTubeStream(videoId);
        } else if ("twitch".equalsIgnoreCase(platform) && channelId != null) {
            loadTwitchStream(channelId);
        } else if (streamUrl != null) {
            loadDirectUrl(streamUrl);
        } else {
            Toast.makeText(this, "No stream URL provided", Toast.LENGTH_LONG).show();
            finish();
        }

        // Boutons
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_fullscreen).setOnClickListener(v -> toggleFullscreen());
        findViewById(R.id.btn_chat).setOnClickListener(v -> openChat());
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Activer JavaScript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Pour la lecture vidéo
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // Performance
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // WebView Client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("youtube.com") || url.contains("twitch.tv") || url.contains("facebook.com")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(WebViewStreamActivity.this,
                        "Error loading stream",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadingProgress.setVisibility(View.GONE);
            }
        });

        // WebChrome Client
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                loadingProgress.setProgress(newProgress);
                if (newProgress == 100) {
                    loadingProgress.setVisibility(View.GONE);
                } else {
                    loadingProgress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                enterFullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }
        });
    }

    private void loadYouTubeStream(String videoId) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                "<style>" +
                "body { margin: 0; padding: 0; background: #000; } " +
                "#player { width: 100%; height: 100vh; } " +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id='player'></div>" +
                "<script>" +
                "var tag = document.createElement('script');" +
                "tag.src = 'https://www.youtube.com/iframe_api';" +
                "var firstScriptTag = document.getElementsByTagName('script')[0];" +
                "firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);" +
                "" +
                "var player;" +
                "function onYouTubeIframeAPIReady() {" +
                "  player = new YT.Player('player', {" +
                "    height: '100%'," +
                "    width: '100%'," +
                "    videoId: '" + videoId + "'," +
                "    playerVars: {" +
                "      'autoplay': 1," +
                "      'controls': 1," +
                "      'rel': 0," +
                "      'showinfo': 0," +
                "      'modestbranding': 1," +
                "      'playsinline': 1" +
                "    }" +
                "  });" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
    }

    private void loadTwitchStream(String channelName) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                "<style>" +
                "body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #000; } " +
                "iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; } " +
                "</style>" +
                "</head>" +
                "<body>" +
                "<iframe " +
                "  src='https://player.twitch.tv/?channel=" + channelName +
                "&parent=localhost" +
                "&autoplay=true" +
                "&muted=false'" +
                "  allowfullscreen='true'" +
                "  scrolling='no'" +
                "  frameborder='0'>" +
                "</iframe>" +
                "</body>" +
                "</html>";

        webView.loadDataWithBaseURL("https://twitch.tv", html, "text/html", "UTF-8", null);
    }

    private void loadDirectUrl(String url) {
        webView.loadUrl(url);
    }

    private void enterFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (isFullscreen) return;

        isFullscreen = true;
        webViewContainer.setVisibility(View.GONE);

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void exitFullscreen() {
        if (!isFullscreen) return;

        isFullscreen = false;
        webViewContainer.setVisibility(View.VISIBLE);

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.removeAllViews();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            webView.evaluateJavascript("if(document.querySelector('video')) document.querySelector('video').requestFullscreen();", null);
        }
    }

    private void openChat() {
        Toast.makeText(this, "Chat coming soon...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            exitFullscreen();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.evaluateJavascript("if(document.querySelector('video')) document.querySelector('video').pause();", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.evaluateJavascript("if(document.querySelector('video')) document.querySelector('video').play();", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}