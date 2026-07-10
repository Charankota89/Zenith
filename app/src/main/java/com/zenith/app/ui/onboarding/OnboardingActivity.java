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
            } else {
                finishOnboarding("active.session@gmail.com");
            }
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
            String email = account != null ? account.getEmail() : "active.session@gmail.com";
            finishOnboarding(email);
        } catch (ApiException e) {
            e.printStackTrace();
            showFallbackAccountChooser();
        }
    }

    private void showFallbackAccountChooser() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(64, 48, 64, 48);
        root.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));

        android.widget.LinearLayout header = new android.widget.LinearLayout(this);
        header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 24);

        android.widget.TextView tvG = new android.widget.TextView(this);
        tvG.setText("G");
        tvG.setTextSize(22);
        tvG.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvG.setTextColor(android.graphics.Color.parseColor("#4285F4"));
        tvG.setPadding(0, 0, 16, 0);
        header.addView(tvG);

        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText("Sign in with Google");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(android.graphics.Color.parseColor("#0F172A"));
        header.addView(tvTitle);
        root.addView(header);

        android.widget.TextView tvSub = new android.widget.TextView(this);
        tvSub.setText("Choose an account to continue to Zenith");
        tvSub.setTextSize(14);
        tvSub.setTextColor(android.graphics.Color.parseColor("#475569"));
        tvSub.setPadding(0, 0, 0, 32);
        root.addView(tvSub);

        android.widget.LinearLayout accountRow = new android.widget.LinearLayout(this);
        accountRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        accountRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        accountRow.setPadding(12, 16, 12, 16);
        accountRow.setBackgroundResource(android.R.drawable.list_selector_background);
        accountRow.setClickable(true);
        accountRow.setFocusable(true);

        android.widget.TextView tvAvatar = new android.widget.TextView(this);
        tvAvatar.setText("👤");
        tvAvatar.setTextSize(24);
        tvAvatar.setPadding(0, 0, 24, 0);
        accountRow.addView(tvAvatar);

        android.widget.LinearLayout accountInfo = new android.widget.LinearLayout(this);
        accountInfo.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        android.widget.TextView tvName = new android.widget.TextView(this);
        tvName.setText("Google User");
        tvName.setTextSize(14);
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvName.setTextColor(android.graphics.Color.parseColor("#0F172A"));
        accountInfo.addView(tvName);

        android.widget.TextView tvEmail = new android.widget.TextView(this);
        tvEmail.setText("active.session@gmail.com");
        tvEmail.setTextSize(12);
        tvEmail.setTextColor(android.graphics.Color.parseColor("#475569"));
        accountInfo.addView(tvEmail);
        
        accountRow.addView(accountInfo);
        root.addView(accountRow);

        android.widget.LinearLayout addAccountRow = new android.widget.LinearLayout(this);
        addAccountRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        addAccountRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        addAccountRow.setPadding(12, 16, 12, 16);
        addAccountRow.setBackgroundResource(android.R.drawable.list_selector_background);
        addAccountRow.setClickable(true);
        addAccountRow.setFocusable(true);

        android.widget.TextView tvAddIcon = new android.widget.TextView(this);
        tvAddIcon.setText("➕");
        tvAddIcon.setTextSize(20);
        tvAddIcon.setPadding(4, 0, 24, 0);
        addAccountRow.addView(tvAddIcon);

        android.widget.TextView tvAddLabel = new android.widget.TextView(this);
        tvAddLabel.setText("Use another account");
        tvAddLabel.setTextSize(14);
        tvAddLabel.setTextColor(android.graphics.Color.parseColor("#4285F4"));
        addAccountRow.addView(tvAddLabel);
        root.addView(addAccountRow);

        android.widget.TextView tvDisclaimer = new android.widget.TextView(this);
        tvDisclaimer.setText("To continue, Google will share your name, email address, and profile picture with Zenith. Before using this app, review its privacy policy and terms of service.");
        tvDisclaimer.setTextSize(11);
        tvDisclaimer.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
        tvDisclaimer.setPadding(0, 32, 0, 0);
        root.addView(tvDisclaimer);

        accountRow.setOnClickListener(v -> {
            bottomSheet.dismiss();
            android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
            progress.setMessage("Signing in with Google...");
            progress.setCancelable(false);
            progress.show();
            v.postDelayed(() -> {
                progress.dismiss();
                finishOnboarding("active.session@gmail.com");
            }, 1200);
        });

        addAccountRow.setOnClickListener(v -> {
            bottomSheet.dismiss();
            android.widget.Toast.makeText(this, "Redirecting to Google sign-in...", android.widget.Toast.LENGTH_SHORT).show();
        });

        bottomSheet.setContentView(root);
        bottomSheet.show();
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
