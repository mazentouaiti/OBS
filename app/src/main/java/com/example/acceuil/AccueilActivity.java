package com.example.acceuil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AccueilActivity extends AppCompatActivity {

    // We'll use these to make buttons work
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tell Android: "Use the layout file we just made"
        setContentView(R.layout.activity_accueil);

        // Find the search bar
        searchBar = findViewById(R.id.et_search);

        // Make the OBS button work
        findViewById(R.id.btn_obs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccueilActivity.this,
                        "Starting OBS Camera...",
                        Toast.LENGTH_SHORT).show();

                // Later you'll add code to open camera here
            }
        });

        // Make the Profile button work
        findViewById(R.id.btn_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccueilActivity.this,
                        "Opening Profile...",
                        Toast.LENGTH_SHORT).show();

                // Later you'll add code to open profile here
            }
        });

        // Make Live Stream 1 clickable
        findViewById(R.id.stream_card_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccueilActivity.this,
                        "Watching Gaming Tournament",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Make Live Stream 2 clickable
        findViewById(R.id.stream_card_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccueilActivity.this,
                        "Watching Coding Session",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}