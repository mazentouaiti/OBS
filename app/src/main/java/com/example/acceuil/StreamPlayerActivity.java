package com.example.acceuil;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StreamPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_player);

        // Get data from intent
        String streamTitle = getIntent().getStringExtra("STREAM_TITLE");
        String streamerName = getIntent().getStringExtra("STREAMER_NAME");

        // Display data
        TextView titleView = findViewById(R.id.stream_title);
        TextView streamerView = findViewById(R.id.streamer_name);

        if (titleView != null) titleView.setText(streamTitle);
        if (streamerView != null) streamerView.setText("Streamer: " + streamerName);
    }
}