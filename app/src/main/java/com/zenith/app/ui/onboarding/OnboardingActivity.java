package com.zenith.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.zenith.app.databinding.ActivityOnboardingBinding;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.util.AppConstants;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private int                       currentPage = 0;

    private final String[] titles = {
        "Take Back Your Time",
        "Set Limits. Lock Apps.",
        "Focus. Grow. Rise."
    };

    private final String[] descs = {
        "Zenith tracks every minute you spend on social media and apps — with no judgment, just clarity.",
        "Set daily time limits. When you hit them, the app locks itself — automatically.",
        "Pomodoro sessions, habit tracking, skill timer, mood check-ins — everything you need to be your best."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        showPage(0);

        binding.btnNext.setOnClickListener(v -> {
            if (currentPage < titles.length - 1) {
                showPage(currentPage + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void showPage(int page) {
        currentPage = page;
        binding.tvTitle.setText(titles[page]);
        binding.tvDesc.setText(descs[page]);
        binding.btnNext.setText(page == titles.length - 1 ? "Get Started" : "Next");

        // Simple dot indicators
        binding.dot1.setAlpha(page == 0 ? 1f : 0.3f);
        binding.dot2.setAlpha(page == 1 ? 1f : 0.3f);
        binding.dot3.setAlpha(page == 2 ? 1f : 0.3f);
    }

    private void finishOnboarding() {
        getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
            .edit().putBoolean(AppConstants.PREF_ONBOARDED, true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
