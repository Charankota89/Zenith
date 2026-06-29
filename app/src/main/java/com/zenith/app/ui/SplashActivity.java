package com.zenith.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.zenith.app.databinding.ActivitySplashBinding;
import com.zenith.app.ui.onboarding.OnboardingActivity;
import com.zenith.app.util.AppConstants;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setFillAfter(true);
        binding.ivLogo.startAnimation(fadeIn);
        binding.tvAppName.startAnimation(fadeIn);
        binding.tvTagline.startAnimation(fadeIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs =
                getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE);
            boolean onboarded = prefs.getBoolean(AppConstants.PREF_ONBOARDED, false);
            Intent target = onboarded
                ? new Intent(SplashActivity.this, MainActivity.class)
                : new Intent(SplashActivity.this, OnboardingActivity.class);
            startActivity(target);
            finish();
        }, 2000);
    }
}
