package com.example.acceuil;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;
import android.widget.ImageView;
import android.widget.FrameLayout;

public class AccueilActivity extends AppCompatActivity {

    private EditText searchBar;
    private FirebaseFirestore db;
    private LinearLayout liveStreamsContainer;
    private LinearLayout videosContainer;
    private LinearLayout categoryChipsContainer;
    private SwipeRefreshLayout swipeRefresh;

    // Loading and empty states
    private ProgressBar streamsLoading;
    private ProgressBar videosLoading;
    private TextView streamsEmpty;
    private TextView videosEmpty;
    private HorizontalScrollView streamsScroll;
    private HorizontalScrollView videosScroll;

    // Sort buttons
    private TextView sortPopular;
    private TextView sortNewest;

    // Data storage
    private List<Stream> allStreams = new ArrayList<>();
    private List<Video> allVideos = new ArrayList<>();
    private String selectedCategory = "All";
    private String currentSort = "popular";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accueil);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Find views
        searchBar = findViewById(R.id.et_search);
        liveStreamsContainer = findViewById(R.id.live_streams_container);
        videosContainer = findViewById(R.id.videos_container);
        categoryChipsContainer = findViewById(R.id.category_chips_container);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        streamsLoading = findViewById(R.id.streams_loading);
        videosLoading = findViewById(R.id.videos_loading);
        streamsEmpty = findViewById(R.id.streams_empty);
        videosEmpty = findViewById(R.id.videos_empty);
        streamsScroll = findViewById(R.id.streams_scroll);
        videosScroll = findViewById(R.id.videos_scroll);

        sortPopular = findViewById(R.id.sort_popular);
        sortNewest = findViewById(R.id.sort_newest);

        // Setup category chips
        setupCategoryChips();

        // Setup sort buttons
        setupSortButtons();

        // Setup search functionality
        setupSearch();

        // Setup pull to refresh
        swipeRefresh.setOnRefreshListener(() -> {
            loadLiveStreams();
            loadRecommendedVideos();
        });

        // Buttons
        findViewById(R.id.btn_obs).setOnClickListener(v -> {
            Toast.makeText(this, "Starting OBS Camera...", Toast.LENGTH_SHORT).show();
            // TODO: Open OBSActivity
        });

        findViewById(R.id.btn_profile).setOnClickListener(v -> {
            Toast.makeText(this, "Opening Profile...", Toast.LENGTH_SHORT).show();
            // TODO: Open ProfileActivity
        });

        // Load data from Firebase
        loadLiveStreams();
        loadRecommendedVideos();
    }

    // ================== INTENT METHODS ==================
    private void openStreamPlayer(Stream stream) {
        String streamUrl = stream.getStreamUrl();

        // Check if it's YouTube/Twitch for WebView
        if (streamUrl != null && (streamUrl.contains("youtube.com") ||
                streamUrl.contains("youtu.be") ||
                streamUrl.contains("twitch.tv"))) {

            Intent intent = new Intent(AccueilActivity.this, WebViewStreamActivity.class);
            intent.putExtra("STREAM_TITLE", stream.getTitle() != null ? stream.getTitle() : "Live Stream");
            intent.putExtra("STREAMER_NAME", stream.getStreamerName() != null ? stream.getStreamerName() : "Unknown");
            intent.putExtra("STREAM_URL", streamUrl);

            if (streamUrl.contains("youtube.com") || streamUrl.contains("youtu.be")) {
                intent.putExtra("PLATFORM", "youtube");
            } else if (streamUrl.contains("twitch.tv")) {
                intent.putExtra("PLATFORM", "twitch");
            }

            startActivity(intent);

        } else {
            // Use ExoPlayer for RTMP/HLS
            Intent intent = new Intent(AccueilActivity.this, StreamPlayerActivity.class);
            intent.putExtra("STREAM_TITLE", stream.getTitle() != null ? stream.getTitle() : "Live Stream");
            intent.putExtra("STREAMER_NAME", stream.getStreamerName() != null ? stream.getStreamerName() : "Unknown");
            intent.putExtra("VIEWER_COUNT", stream.getViewerCount());
            intent.putExtra("STREAM_URL", streamUrl);
            startActivity(intent);
        }
    }

    private void openVideoPlayer(Video video) {
        Intent intent = new Intent(AccueilActivity.this, VideoPlayerActivity.class);
        intent.putExtra("VIDEO_TITLE", video.getTitle() != null ? video.getTitle() : "Video");
        intent.putExtra("UPLOADER_NAME", video.getUploaderName() != null ? video.getUploaderName() : "Unknown");
        intent.putExtra("VIEWS", video.getViews());
        intent.putExtra("DURATION", video.getDurationSeconds());
        intent.putExtra("VIDEO_URL", video.getVideoUrl());
        startActivity(intent);
    }

    // ================== CARD CREATION METHODS ==================
    private View createStreamCard(Stream stream) {
        // Main container
        FrameLayout cardFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                dpToPx(280),
                dpToPx(180)
        );
        frameParams.setMarginEnd(dpToPx(16));
        cardFrame.setLayoutParams(frameParams);

        // ImageView for thumbnail
        ImageView thumbnail = new ImageView(this);
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        thumbnail.setLayoutParams(thumbParams);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Load image with Glide - WITH NULL CHECKS
        String thumbnailUrl = stream.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            int fallbackColor = getFallbackColor(stream);
            Glide.with(this)
                    .load(thumbnailUrl)
                    .placeholder(getColorDrawable(fallbackColor))
                    .error(getColorDrawable(fallbackColor))
                    .into(thumbnail);
        } else {
            thumbnail.setBackgroundColor(getFallbackColor(stream));
        }

        cardFrame.addView(thumbnail);

        // Overlay container for text
        LinearLayout overlay = new LinearLayout(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlay.setLayoutParams(overlayParams);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        overlay.setBackgroundColor(Color.parseColor("#40000000"));

        // LIVE badge
        LinearLayout liveBadge = new LinearLayout(this);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.setMargins(0, 0, 0, dpToPx(8));
        liveBadge.setLayoutParams(badgeParams);
        liveBadge.setBackgroundColor(Color.parseColor("#FF4757"));
        liveBadge.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));

        TextView liveText = new TextView(this);
        liveText.setText("LIVE");
        liveText.setTextColor(Color.WHITE);
        liveText.setTextSize(12);
        liveText.setTypeface(null, android.graphics.Typeface.BOLD);
        liveBadge.addView(liveText);

        // Spacer
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
        );
        spacerParams.weight = 1;
        spacer.setLayoutParams(spacerParams);

        // Stream title
        TextView titleText = new TextView(this);
        titleText.setText(stream.getTitle() != null ? stream.getTitle() : "Untitled Stream");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setShadowLayer(4, 0, 0, Color.BLACK);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, dpToPx(4));
        titleText.setLayoutParams(titleParams);

        // Streamer name
        TextView streamerText = new TextView(this);
        streamerText.setText(stream.getStreamerName() != null ? stream.getStreamerName() : "Unknown");
        streamerText.setTextColor(Color.parseColor("#CCCCCC"));
        streamerText.setTextSize(14);
        streamerText.setShadowLayer(4, 0, 0, Color.BLACK);
        LinearLayout.LayoutParams streamerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        streamerParams.setMargins(0, 0, 0, dpToPx(4));
        streamerText.setLayoutParams(streamerParams);

        // Viewer count
        TextView viewerText = new TextView(this);
        viewerText.setText(stream.getFormattedViewerCount());
        viewerText.setTextColor(Color.WHITE);
        viewerText.setTextSize(14);
        viewerText.setTypeface(null, android.graphics.Typeface.BOLD);
        viewerText.setShadowLayer(4, 0, 0, Color.BLACK);

        // Add all elements to overlay
        overlay.addView(liveBadge);
        overlay.addView(spacer);
        overlay.addView(titleText);
        overlay.addView(streamerText);
        overlay.addView(viewerText);

        // Add overlay on top of thumbnail
        cardFrame.addView(overlay);

        // Click listener
        cardFrame.setOnClickListener(v -> {
            openStreamPlayer(stream);
        });

        return cardFrame;
    }

    private int getFallbackColor(Stream stream) {
        try {
            String color = stream.getThumbnailColor();
            if (color != null && !color.isEmpty()) {
                return Color.parseColor(color);
            }
        } catch (Exception e) {
            // Ignore
        }
        return Color.parseColor("#FF6B8B");
    }

    // ================== HELPER METHOD FOR GLIDE ==================
    private android.graphics.drawable.ColorDrawable getColorDrawable(int color) {
        return new android.graphics.drawable.ColorDrawable(color);
    }

    private View createVideoCard(Video video) {
        // Main container
        FrameLayout cardFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                dpToPx(180),
                dpToPx(180)
        );
        frameParams.setMarginEnd(dpToPx(16));
        cardFrame.setLayoutParams(frameParams);

        // Background ImageView for thumbnail
        ImageView thumbnail = new ImageView(this);
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        thumbnail.setLayoutParams(thumbParams);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Load image with Glide - WITH NULL CHECKS
        String thumbnailUrl = video.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            String colorHex = video.getThumbnailColor() != null ? video.getThumbnailColor() : "#2A2A3E";
            int fallbackColor = Color.parseColor(colorHex);
            Glide.with(this)
                    .load(thumbnailUrl)
                    .placeholder(getColorDrawable(fallbackColor))
                    .error(getColorDrawable(fallbackColor))
                    .into(thumbnail);
        } else {
            String color = video.getThumbnailColor() != null ? video.getThumbnailColor() : "#2A2A3E";
            thumbnail.setBackgroundColor(Color.parseColor(color));
        }

        cardFrame.addView(thumbnail);

        // Overlay container
        LinearLayout overlay = new LinearLayout(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlay.setLayoutParams(overlayParams);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        overlay.setBackgroundColor(Color.parseColor("#40000000"));

        // Duration badge
        TextView durationBadge = new TextView(this);
        durationBadge.setText(video.getFormattedDuration());
        durationBadge.setTextColor(Color.WHITE);
        durationBadge.setTextSize(12);
        durationBadge.setBackgroundColor(Color.parseColor("#80000000"));
        durationBadge.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        durationParams.gravity = Gravity.END;
        durationBadge.setLayoutParams(durationParams);

        // Spacer
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
        );
        spacerParams.weight = 1;
        spacer.setLayoutParams(spacerParams);

        // Video title
        TextView titleText = new TextView(this);
        titleText.setText(video.getTitle() != null ? video.getTitle() : "Untitled Video");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(14);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setShadowLayer(4, 0, 0, Color.BLACK);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, dpToPx(4));
        titleText.setLayoutParams(titleParams);

        // Uploader name
        TextView uploaderText = new TextView(this);
        uploaderText.setText(video.getUploaderName() != null ? video.getUploaderName() : "Unknown");
        uploaderText.setTextColor(Color.parseColor("#CCCCCC"));
        uploaderText.setTextSize(12);
        uploaderText.setShadowLayer(4, 0, 0, Color.BLACK);
        LinearLayout.LayoutParams uploaderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        uploaderParams.setMargins(0, 0, 0, dpToPx(4));
        uploaderText.setLayoutParams(uploaderParams);

        // Views count
        TextView viewsText = new TextView(this);
        viewsText.setText(video.getFormattedViews());
        viewsText.setTextColor(Color.parseColor("#CCCCCC"));
        viewsText.setTextSize(12);
        viewsText.setShadowLayer(4, 0, 0, Color.BLACK);

        // Add all elements
        overlay.addView(durationBadge);
        overlay.addView(spacer);
        overlay.addView(titleText);
        overlay.addView(uploaderText);
        overlay.addView(viewsText);

        cardFrame.addView(overlay);

        // Click listener
        cardFrame.setOnClickListener(v -> {
            openVideoPlayer(video);
        });

        return cardFrame;
    }

    // ================== FIREBASE DATA LOADING ==================
    private void loadLiveStreams() {
        streamsLoading.setVisibility(View.VISIBLE);
        streamsEmpty.setVisibility(View.GONE);
        streamsScroll.setVisibility(View.GONE);

        db.collection("streams")
                .whereEqualTo("isLive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStreams.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Stream stream = document.toObject(Stream.class);
                        stream.setDocumentId(document.getId());
                        allStreams.add(stream);
                    }

                    filterData();

                    streamsLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (allStreams.isEmpty()) {
                        streamsEmpty.setVisibility(View.VISIBLE);
                    } else {
                        streamsScroll.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error loading streams: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    streamsLoading.setVisibility(View.GONE);
                    streamsEmpty.setVisibility(View.VISIBLE);
                    swipeRefresh.setRefreshing(false);
                });
    }

    private void loadRecommendedVideos() {
        videosLoading.setVisibility(View.VISIBLE);
        videosEmpty.setVisibility(View.GONE);
        videosScroll.setVisibility(View.GONE);

        db.collection("videos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allVideos.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Video video = document.toObject(Video.class);
                        video.setDocumentId(document.getId());
                        allVideos.add(video);
                    }

                    filterData();

                    videosLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (allVideos.isEmpty()) {
                        videosEmpty.setVisibility(View.VISIBLE);
                    } else {
                        videosScroll.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error loading videos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    videosLoading.setVisibility(View.GONE);
                    videosEmpty.setVisibility(View.VISIBLE);
                    swipeRefresh.setRefreshing(false);
                });
    }

    // ================== HELPER METHODS ==================
    private void setupCategoryChips() {
        String[] categories = {"All", "Gaming", "Music", "Coding", "Art", "Sports", "Talk"};

        for (String category : categories) {
            TextView chip = createCategoryChip(category);
            categoryChipsContainer.addView(chip);
        }
    }

    private TextView createCategoryChip(String category) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(dpToPx(8));
        chip.setLayoutParams(params);

        chip.setText(category);
        chip.setTextSize(14);
        chip.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        if (category.equals(selectedCategory)) {
            chip.setBackgroundColor(Color.parseColor("#4F8BF9"));
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundColor(Color.parseColor("#2A2A3E"));
            chip.setTextColor(Color.parseColor("#888888"));
        }

        chip.setClickable(true);
        chip.setFocusable(true);

        chip.setOnClickListener(v -> {
            selectedCategory = category;
            updateCategoryChips();
            filterData();
        });

        return chip;
    }

    private void updateCategoryChips() {
        categoryChipsContainer.removeAllViews();
        setupCategoryChips();
    }

    private void setupSortButtons() {
        sortPopular.setOnClickListener(v -> {
            currentSort = "popular";
            updateSortButtons();
            sortAndDisplayData();
        });

        sortNewest.setOnClickListener(v -> {
            currentSort = "newest";
            updateSortButtons();
            sortAndDisplayData();
        });
    }

    private void updateSortButtons() {
        if (currentSort.equals("popular")) {
            sortPopular.setBackgroundColor(Color.parseColor("#2A2A3E"));
            sortPopular.setTextColor(Color.WHITE);
            sortNewest.setBackgroundColor(Color.parseColor("#1A1A2E"));
            sortNewest.setTextColor(Color.parseColor("#888888"));
        } else {
            sortNewest.setBackgroundColor(Color.parseColor("#2A2A3E"));
            sortNewest.setTextColor(Color.WHITE);
            sortPopular.setBackgroundColor(Color.parseColor("#1A1A2E"));
            sortPopular.setTextColor(Color.parseColor("#888888"));
        }
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterData() {
        String searchQuery = searchBar.getText().toString().toLowerCase();

        // Filter streams
        List<Stream> filteredStreams = new ArrayList<>();
        for (Stream stream : allStreams) {
            String title = stream.getTitle() != null ? stream.getTitle().toLowerCase() : "";
            String streamerName = stream.getStreamerName() != null ? stream.getStreamerName().toLowerCase() : "";

            boolean matchesSearch = title.contains(searchQuery) || streamerName.contains(searchQuery);

            String streamCategory = stream.getCategory() != null ? stream.getCategory() : "All";
            boolean matchesCategory = selectedCategory.equals("All") || streamCategory.equals(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredStreams.add(stream);
            }
        }

        // Filter videos
        List<Video> filteredVideos = new ArrayList<>();
        for (Video video : allVideos) {
            String title = video.getTitle() != null ? video.getTitle().toLowerCase() : "";
            String uploaderName = video.getUploaderName() != null ? video.getUploaderName().toLowerCase() : "";

            boolean matchesSearch = title.contains(searchQuery) || uploaderName.contains(searchQuery);

            String videoCategory = video.getCategory() != null ? video.getCategory() : "All";
            boolean matchesCategory = selectedCategory.equals("All") || videoCategory.equals(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredVideos.add(video);
            }
        }

        // Sort and display
        sortAndDisplayStreams(filteredStreams);
        sortAndDisplayVideos(filteredVideos);
    }

    private void sortAndDisplayData() {
        filterData();
    }

    private void sortAndDisplayStreams(List<Stream> streams) {
        if (currentSort.equals("popular")) {
            streams.sort((s1, s2) -> Integer.compare(s2.getViewerCount(), s1.getViewerCount()));
        }
        displayStreams(streams);
    }

    private void sortAndDisplayVideos(List<Video> videos) {
        if (currentSort.equals("popular")) {
            videos.sort((v1, v2) -> Integer.compare(v2.getViews(), v1.getViews()));
        }
        displayVideos(videos);
    }

    private void displayStreams(List<Stream> streams) {
        liveStreamsContainer.removeAllViews();

        for (Stream stream : streams) {
            View streamCard = createStreamCard(stream);
            liveStreamsContainer.addView(streamCard);
        }

        if (streams.isEmpty()) {
            streamsEmpty.setVisibility(View.VISIBLE);
            streamsScroll.setVisibility(View.GONE);
        } else {
            streamsEmpty.setVisibility(View.GONE);
            streamsScroll.setVisibility(View.VISIBLE);
        }
    }

    private void displayVideos(List<Video> videos) {
        videosContainer.removeAllViews();

        for (Video video : videos) {
            View videoCard = createVideoCard(video);
            videosContainer.addView(videoCard);
        }

        if (videos.isEmpty()) {
            videosEmpty.setVisibility(View.VISIBLE);
            videosScroll.setVisibility(View.GONE);
        } else {
            videosEmpty.setVisibility(View.GONE);
            videosScroll.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}