package com.zenith.app.ui.settings;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.zenith.app.databinding.ActivityPinSetupBinding;
import com.zenith.app.util.AppConstants;

public class PinSetupActivity extends AppCompatActivity {

    private ActivityPinSetupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPinSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSavePin.setOnClickListener(v -> {
            String pin     = binding.etPin.getText().toString().trim();
            String confirm = binding.etConfirmPin.getText().toString().trim();

            if (pin.length() < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pin.equals(confirm)) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
                .edit().putString(AppConstants.PREF_PIN, pin).apply();
            Toast.makeText(this, "PIN saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
