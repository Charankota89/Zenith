package com.zenith.app.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zenith.app.R;
import com.zenith.app.databinding.ActivityMainBinding;
import com.zenith.app.service.UsageMonitorService;
import com.zenith.app.ui.career.CareerFragment;
import com.zenith.app.ui.focus.FocusFragment;
import com.zenith.app.ui.home.HomeFragment;
import com.zenith.app.ui.screen.ScreenFragment;
import com.zenith.app.ui.settings.SettingsFragment;
import com.zenith.app.ui.wellbeing.WellbeingFragment;
import com.zenith.app.util.NotificationHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Start background usage monitor (idempotent — service handles duplicate starts)
        startForegroundService(new Intent(this, UsageMonitorService.class));

        // Default tab
        currentTabIndex = 0;
        loadFragment(new HomeFragment());
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        // Handle notification deep-link tap (if launched from a notification)
        handleNotificationIntent(getIntent());
        handleEmailDeepLink(getIntent());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int newIndex = getTabIndex(item.getItemId());
            if (newIndex != currentTabIndex) {
                loadFragmentWithAnimation(fragmentForId(item.getItemId()), newIndex);
            }
            return true;
        });
    }

    /**
     * Called when the activity is already running and a new intent arrives
     * (e.g., user taps another notification while app is open).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
        handleEmailDeepLink(intent);
    }

    private void handleEmailDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        android.net.Uri data = intent.getData();
        if ("zenith".equals(data.getScheme()) && "unlock".equals(data.getHost())) {
            String pkg = data.getQueryParameter("pkg");
            String durationStr = data.getQueryParameter("duration");
            String reason = data.getQueryParameter("reason");

            if (pkg != null && durationStr != null) {
                try {
                    long durationMin = Long.parseLong(durationStr);
                    long durationMs = durationMin * 60000;
                    
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        com.zenith.app.db.AppDatabase db = com.zenith.app.db.AppDatabase.getInstance(getApplicationContext());
                        String today = com.zenith.app.util.TimeUtils.getTodayDate();
                        com.zenith.app.db.entity.AppUsageEntity ue = db.appUsageDao().getUsageForApp(pkg, today);
                        if (ue != null) {
                            ue.isLocked = false;
                            ue.unlockReason = reason != null ? reason : "Verified via Email Link";
                            ue.unlockExpiresAt = System.currentTimeMillis() + durationMs;
                            db.appUsageDao().update(ue);
                        }
                    });

                    android.widget.Toast.makeText(this, "Email verified! App unlocked successfully.", android.widget.Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reads EXTRA_TARGET_TAB from the intent and navigates to the correct tab.
     * This is set by NotificationHelper so every notification taps into the right screen.
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;
        String tab = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_TAB);
        if (tab == null) return;

        int menuId;
        switch (tab) {
            case NotificationHelper.TAB_SCREEN:    menuId = R.id.nav_screen;    break;
            case NotificationHelper.TAB_FOCUS:     menuId = R.id.nav_focus;     break;
            case NotificationHelper.TAB_WELLBEING: menuId = R.id.nav_wellbeing; break;
            case NotificationHelper.TAB_CAREER:    menuId = R.id.nav_career;    break;
            default:                               menuId = R.id.nav_home;      break;
        }
        binding.bottomNav.setSelectedItemId(menuId);
    }

    private int currentTabIndex = 0;

    private int getTabIndex(int id) {
        if      (id == R.id.nav_home)      return 0;
        else if (id == R.id.nav_screen)    return 1;
        else if (id == R.id.nav_focus)     return 2;
        else if (id == R.id.nav_wellbeing) return 3;
        else if (id == R.id.nav_career)    return 4;
        return 0;
    }

    private Fragment fragmentForId(int id) {
        if      (id == R.id.nav_home)     return new HomeFragment();
        else if (id == R.id.nav_screen)   return new ScreenFragment();
        else if (id == R.id.nav_focus)    return new FocusFragment();
        else if (id == R.id.nav_wellbeing) return new WellbeingFragment();
        else if (id == R.id.nav_career)   return new CareerFragment();
        else                              return new HomeFragment();
    }

    private void loadFragment(Fragment f) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .commit();
    }

    private void loadFragmentWithAnimation(Fragment f, int newIndex) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (newIndex > currentTabIndex) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (newIndex < currentTabIndex) {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        currentTabIndex = newIndex;
        transaction.replace(R.id.fragmentContainer, f).commit();
    }

    public void navigateToTab(int navItemId) {
        binding.bottomNav.setSelectedItemId(navItemId);
    }
}
