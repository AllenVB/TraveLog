package com.example.travelog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean onboarded = getSharedPreferences(
                    OnboardingActivity.PREFS, MODE_PRIVATE)
                    .getBoolean(OnboardingActivity.KEY_ONBOARDED, false);

            Class<?> target = onboarded ? MainActivity.class : OnboardingActivity.class;
            startActivity(new Intent(SplashActivity.this, target));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
