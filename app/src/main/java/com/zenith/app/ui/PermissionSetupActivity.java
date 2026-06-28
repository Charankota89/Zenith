package com.zenith.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Guides the user through granting all required permissions:
 *  1. Usage Access (PACKAGE_USAGE_STATS)
 *  2. Draw Over Apps (SYSTEM_ALERT_WINDOW)
 *  3. Accessibility Service (BIND_ACCESSIBILITY_SERVICE)
 *
 * Full UI implementation will be added after all features are scaffolded.
 * Required now to satisfy Manifest + accessibility_service_config.xml references.
 */
public class PermissionSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Will be implemented in a dedicated step
        finish();
    }
}
