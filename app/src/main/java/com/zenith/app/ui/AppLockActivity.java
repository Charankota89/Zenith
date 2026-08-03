package com.zenith.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
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

        // Modern back pressed handler: send to home screen if user backs out of lock screen
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE);
        storedPinHash = prefs.getString(AppConstants.PREF_PIN, null);

        // If somehow no PIN is actually set, don't strand the user on a
        // lock screen they can never pass — just let them through.
        if (storedPinHash == null || storedPinHash.trim().isEmpty()) {
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
        try {
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
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Do not finish the activity; user can still enter PIN.
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // Do not finish the activity; user can try again or enter PIN.
                    }
                });

            binding.tvUseBiometric.setOnClickListener(v -> {
                try { biometricPrompt.authenticate(promptInfo); } catch (Exception ignored) {}
            });

            // Offer biometric immediately on open, since it's faster than typing
            biometricPrompt.authenticate(promptInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void proceedToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
