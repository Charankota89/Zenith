package com.zenith.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.zenith.app.databinding.ActivityAppLockBinding;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.PinSecurityUtil;

/**
 * Shown on app launch whenever a PIN has been set. Without this activity,
 * the PIN feature under Settings did nothing at all — you could set a
 * PIN, but nothing in the app ever asked for it again.
 */
public class AppLockActivity extends AppCompatActivity {

    private ActivityAppLockBinding binding;
    private String storedPinHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE);
        storedPinHash = prefs.getString(AppConstants.PREF_PIN, null);

        // If somehow no PIN is actually set, don't strand the user on a
        // lock screen they can never pass — just let them through.
        if (storedPinHash == null || storedPinHash.isEmpty()) {
            proceedToMain();
            return;
        }

        // Anyone who set a PIN before this screen existed has a PLAINTEXT
        // value stored (that's literally the bug that got fixed) — typing
        // their real PIN would hash it and compare against the raw text,
        // which can never match, permanently locking them out with no way
        // back in. A real SHA-256 hash is always exactly 64 lowercase hex
        // characters; anything else can only be a legacy/corrupted value,
        // so treat it as "no PIN" and clear it rather than trap the user.
        if (!storedPinHash.matches("^[a-f0-9]{64}$")) {
            prefs.edit().remove(AppConstants.PREF_PIN).apply();
            proceedToMain();
            return;
        }

        binding.btnUnlock.setOnClickListener(v -> attemptPinUnlock());
        binding.etPinEntry.setOnEditorActionListener((v, actionId, event) -> {
            attemptPinUnlock();
            return true;
        });

        setupBiometricIfAvailable();
    }

    private void attemptPinUnlock() {
        String entered = binding.etPinEntry.getText() != null
            ? binding.etPinEntry.getText().toString().trim() : "";
        if (PinSecurityUtil.matches(entered, storedPinHash)) {
            proceedToMain();
        } else {
            binding.tvLockError.setText("Incorrect PIN, try again");
            binding.etPinEntry.setText("");
        }
    }

    private void setupBiometricIfAvailable() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            return; // No enrolled fingerprint/face on this device — PIN-only.
        }

        binding.tvUseBiometric.setVisibility(android.view.View.VISIBLE);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Zenith")
            .setSubtitle("Use your fingerprint or face to continue")
            .setNegativeButtonText("Use PIN instead")
            .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    proceedToMain();
                }
                // On error or failure, we simply do nothing — the user
                // stays on the PIN screen and can try again or type their
                // PIN instead. No need to show a scary error message for
                // a cancelled/failed biometric attempt.
            });

        binding.tvUseBiometric.setOnClickListener(v ->
            biometricPrompt.authenticate(promptInfo));

        // Offer biometric immediately on open, since it's faster than typing
        biometricPrompt.authenticate(promptInfo);
    }

    private void proceedToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Deliberately does nothing — you can't back out of the lock
        // screen without entering the correct PIN or biometric.
    }
}
