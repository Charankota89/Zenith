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

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Choose a Google Account");
            String[] accounts = {
                "Ram Charan (ramcharan@gmail.com)",
                "Add another account"
            };
            builder.setItems(accounts, (dialog, which) -> {
                if (which == 0) {
                    android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
                    progress.setMessage("Signing in with Google...");
                    progress.setCancelable(false);
                    progress.show();
                    
                    v.postDelayed(() -> {
                        progress.dismiss();
                        finishOnboarding();
                    }, 1500);
                } else {
                    android.widget.Toast.makeText(this, "Redirecting to Google login...", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            builder.create().show();
        });
    }

    private void showPage(int page) {
        currentPage = page;
        binding.tvTitle.setText(titles[page]);
        binding.tvDesc.setText(descs[page]);
        if (page == titles.length - 1) {
            binding.btnNext.setVisibility(View.GONE);
            binding.btnGoogleSignIn.setVisibility(View.VISIBLE);
        } else {
            binding.btnNext.setVisibility(View.VISIBLE);
            binding.btnNext.setText("Next");
            binding.btnGoogleSignIn.setVisibility(View.GONE);
        }

        // Simple dot indicators
        binding.dot1.setAlpha(page == 0 ? 1f : 0.3f);
        binding.dot2.setAlpha(page == 1 ? 1f : 0.3f);
        binding.dot3.setAlpha(page == 2 ? 1f : 0.3f);
    }

    private void finishOnboarding() {
        getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(AppConstants.PREF_ONBOARDED, true)
            .putString("user_email", "ramcharan@gmail.com")
            .apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
