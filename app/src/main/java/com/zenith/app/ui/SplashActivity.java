package com.zenith.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.zenith.app.databinding.ActivitySplashBinding;

/**
 * Splash screen — shown for 2 seconds then routes to MainActivity.
 * Uses WindowInsetsController (API 30+) to achieve fullscreen.
 */
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge fullscreen using WindowCompat (replaces deprecated FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide system bars for true immersive splash
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Fade in animation
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setFillAfter(true);
        binding.ivLogo.startAnimation(fadeIn);
        binding.tvAppName.startAnimation(fadeIn);
        binding.tvTagline.startAnimation(fadeIn);

        new Handler(Looper.getMainLooper()).postDelayed(() ->
                startActivity(new Intent(SplashActivity.this, MainActivity.class)), 2000);
    }
}
