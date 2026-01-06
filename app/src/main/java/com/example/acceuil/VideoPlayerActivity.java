package com.example.acceuil;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Get data from intent
        String videoTitle = getIntent().getStringExtra("VIDEO_TITLE");
        String uploaderName = getIntent().getStringExtra("UPLOADER_NAME");

        // Display data
        TextView titleView = findViewById(R.id.video_title);
        TextView uploaderView = findViewById(R.id.uploader_name);

        if (titleView != null) titleView.setText(videoTitle);
        if (uploaderView != null) uploaderView.setText("By: " + uploaderName);
    }
}