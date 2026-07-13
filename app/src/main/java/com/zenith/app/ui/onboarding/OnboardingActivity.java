package com.zenith.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.zenith.app.databinding.ActivityOnboardingBinding;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.util.AppConstants;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private int                       currentPage = 0;
    private GoogleSignInClient        mGoogleSignInClient;
    private static final int          RC_SIGN_IN = 9001;

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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnNext.setOnClickListener(v -> {
            if (currentPage < titles.length - 1) {
                showPage(currentPage + 1);
            }
            // On the final page, btnNext is hidden in favor of btnGoogleSignIn
            // (see updatePageData below), so there's no fake-completion path here.
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null || account.getEmail() == null) {
                showSignInError("Couldn't retrieve your Google account details. Please try again.");
                return;
            }
            finishOnboarding(account.getEmail());
        } catch (ApiException e) {
            e.printStackTrace();
            // Status code 10 (DEVELOPER_ERROR) almost always means the app's
            // SHA-1 fingerprint isn't registered for this OAuth client yet.
            String message = (e.getStatusCode() == com.google.android.gms.common.api.CommonStatusCodes.DEVELOPER_ERROR)
                ? "Google Sign-In isn't configured for this build yet."
                : "Google Sign-In was cancelled or failed. Please try again.";
            showSignInError(message);
        }
    }

    private void showSignInError(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    private void showPage(int page) {
        currentPage = page;
        
        // If it's the first page load, set the content without slide-out animation
        if (page == 0) {
            updatePageData(page);
            return;
        }

        // Page change transitions: slide out to the left, reset to the right, slide in
        binding.tvTitle.animate().alpha(0f).translationX(-50f).setDuration(150).withEndAction(() -> {
            updatePageData(page);
            binding.tvTitle.setTranslationX(50f);
            binding.tvTitle.animate().alpha(1f).translationX(0f).setDuration(250).start();
        }).start();

        binding.tvDesc.animate().alpha(0f).translationX(-50f).setDuration(150).withEndAction(() -> {
            binding.tvDesc.setTranslationX(50f);
            binding.tvDesc.animate().alpha(1f).translationX(0f).setDuration(250).start();
        }).start();
    }

    private void updatePageData(int page) {
        binding.tvTitle.setText(titles[page]);
        binding.tvDesc.setText(descs[page]);
        if (page == titles.length - 1) {
            binding.btnNext.setVisibility(View.GONE);
            binding.btnGoogleSignIn.setVisibility(View.VISIBLE);
            binding.btnGoogleSignIn.setAlpha(0f);
            binding.btnGoogleSignIn.animate().alpha(1f).setDuration(250).start();
        } else {
            binding.btnNext.setVisibility(View.VISIBLE);
            binding.btnNext.setText("Next");
            binding.btnGoogleSignIn.setVisibility(View.GONE);
        }

        // Scale and fade dot indicators
        animateDot(binding.dot1, page == 0);
        animateDot(binding.dot2, page == 1);
        animateDot(binding.dot3, page == 2);
    }

    private void animateDot(View dot, boolean active) {
        float targetAlpha = active ? 1.0f : 0.3f;
        float targetScale = active ? 1.3f : 1.0f;
        dot.animate()
            .alpha(targetAlpha)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(200)
            .start();
    }

    private void finishOnboarding(String email) {
        getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(AppConstants.PREF_ONBOARDED, true)
            .putString("user_email", email)
            .apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
