package com.zenith.app.module1_screen.locker;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * ZenithAccessibilityService — detects foreground app changes and
 * scroll events for reel counting.
 *
 * Feature 3 (AppLocker) will add overlay launching logic.
 * Feature 5 (ReelCounter) will add scroll event counting.
 *
 * This stub is required so the Manifest reference compiles.
 */
public class ZenithAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Full implementation in Feature 3 & 5
    }

    @Override
    public void onInterrupt() {
        // No-op
    }
}
