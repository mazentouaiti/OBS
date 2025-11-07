package com.obs.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity - Entry point of OBS Mobile application
 *
 * Displays splash screen for 2.5 seconds then navigates to Main Menu
 * This is the launcher activity defined in AndroidManifest.xml
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500; // 2.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide action bar for fullscreen splash
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Navigate to Main Menu after delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainMenuActivity.class);
                startActivity(intent);
                finish(); // Close splash so user can't go back to it
            }
        }, SPLASH_DURATION);
    }
}

