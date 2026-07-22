package com.zenith.app.ui.home;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.zenith.app.databinding.FragmentHomeBinding;
import com.zenith.app.R;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.ui.settings.SettingsFragment;
import com.zenith.app.util.TimeUtils;
import com.zenith.app.util.GradientTextUtil;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel       vm;

    // ── Guided one-tap permission setup ─────────────────────────────
    // Android will never let an app silently grant Accessibility, Usage
    // Access, or "Draw over other apps" — those legally require the user
    // to flip a toggle themselves in Settings, every time, for every app.
    // What we CAN do is chain the requests: one tap starts the flow, and
    // each time the user comes back from granting one, we immediately
    // advance to the next missing one automatically instead of making
    // them hunt for the button again.
    private boolean guidedFlowActive  = false;
    private String  lastRequestedStep = "";

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (guidedFlowActive) requestNextMissingPermission();
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this,
            new HomeViewModelFactory(requireContext())).get(HomeViewModel.class);

        binding.tvGreeting.setText("Good day — " + TimeUtils.getDayOfWeek());
        binding.tvTagline.setText("Rise above. Stay focused.");

        // Staggered entering animations for cards
        android.view.animation.Animation anim1 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);
        android.view.animation.Animation anim2 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);
        android.view.animation.Animation anim3 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);

        anim1.setStartOffset(100);
        anim2.setStartOffset(200);
        anim3.setStartOffset(300);

        binding.cardScreenTime.startAnimation(anim1);
        binding.cardHabits.startAnimation(anim2);
        binding.cardStudyTime.startAnimation(anim3);

        binding.btnSettings.setOnClickListener(v -> {
            applyClickBounce(v, () -> {
                getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
            });
        });

        binding.cardScreenTime.setOnClickListener(v -> {
            applyClickBounce(v, () -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_screen);
                }
            });
        });

        binding.cardHabits.setOnClickListener(v -> {
            applyClickBounce(v, () -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_career);
                }
            });
        });

        binding.cardStudyTime.setOnClickListener(v -> {
            applyClickBounce(v, () -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_focus);
                }
            });
        });

        vm.totalScreenTime.observe(getViewLifecycleOwner(), millis -> {
            long hours   = millis / 3600000;
            long minutes = (millis % 3600000) / 60000;
            binding.tvScreenTime.setText(hours + "h " + minutes + "m");
            GradientTextUtil.applyGradient(binding.tvScreenTime,
                Color.parseColor("#A78BFA"), Color.parseColor("#5B21B6"));
        });

        vm.habitsDoneToday.observe(getViewLifecycleOwner(), count -> {
            binding.tvHabitCount.setText(count + " habits done today");
            GradientTextUtil.applyGradient(binding.tvHabitCount,
                Color.parseColor("#34D399"), Color.parseColor("#059669"));
        });

        vm.studyTimeToday.observe(getViewLifecycleOwner(), millis -> {
            binding.tvStudyTime.setText(TimeUtils.formatDuration(millis) + " studied");
            GradientTextUtil.applyGradient(binding.tvStudyTime,
                Color.parseColor("#D97706"), Color.parseColor("#92400E"));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        checkSystemPermissions();
    }

    private void checkSystemPermissions() {
        boolean accessEnabled      = isAccessibilityServiceEnabled();
        boolean overlayEnabled     = isOverlayPermissionEnabled();
        boolean usageEnabled       = isUsageAccessEnabled();
        boolean notificationsOk    = isNotificationPermissionGranted();
        boolean batteryOk          = isBatteryOptimizationDisabled();

        int totalSteps   = 5;
        int missingCount = (accessEnabled ? 0 : 1) + (overlayEnabled ? 0 : 1)
                          + (usageEnabled ? 0 : 1) + (notificationsOk ? 0 : 1)
                          + (batteryOk ? 0 : 1);

        if (missingCount > 0) {
            binding.cardPermissionWarning.setVisibility(View.VISIBLE);

            StringBuilder sb = new StringBuilder(
                "Zenith needs a few system settings enabled to monitor screen time and protect focus. " +
                "Tap below once — we'll guide you through each one automatically:\n");
            if (!notificationsOk)  sb.append("• Notifications\n");
            if (!usageEnabled)     sb.append("• Usage Access (Stats tracking)\n");
            if (!overlayEnabled)   sb.append("• Draw Over Other Apps (Overlay)\n");
            if (!accessEnabled)    sb.append("• Accessibility Service (App lock)\n");
            if (!batteryOk)        sb.append("• Unrestricted Battery Usage (keeps tracking accurate in the background)\n");
            sb.append("\nNote: If greyed out in settings, go to Android Settings -> Apps -> Zenith, tap top-right 3-dots and choose 'Allow restricted settings' to unlock it.");
            binding.tvPermissionDesc.setText(sb.toString());

            int stepsDone = totalSteps - missingCount;
            binding.btnGrantPermissions.setText(
                guidedFlowActive
                    ? "Continue Setup (" + stepsDone + "/" + totalSteps + ")"
                    : "Set Up Zenith Protector");

            binding.btnGrantPermissions.setOnClickListener(v -> {
                guidedFlowActive = true;
                requestNextMissingPermission();
            });

            // Auto-advance: if we're mid-flow and the step we just sent the
            // user to settings for is now granted, immediately continue to
            // the next one without waiting for another tap. If it's still
            // missing, they backed out without toggling it — stop the
            // auto-chain so we don't trap them in a settings-screen loop.
            if (guidedFlowActive && !lastRequestedStep.isEmpty()) {
                boolean lastStepNowGranted =
                    ("notifications".equals(lastRequestedStep) && notificationsOk) ||
                    ("usage".equals(lastRequestedStep) && usageEnabled) ||
                    ("overlay".equals(lastRequestedStep) && overlayEnabled) ||
                    ("accessibility".equals(lastRequestedStep) && accessEnabled) ||
                    ("battery".equals(lastRequestedStep) && batteryOk);

                if (lastStepNowGranted) {
                    requestNextMissingPermission();
                } else {
                    guidedFlowActive = false;
                }
            }
        } else {
            binding.cardPermissionWarning.setVisibility(View.GONE);
            guidedFlowActive  = false;
            lastRequestedStep = "";
        }
    }

    /** Requests whichever permission is next in priority order, one step
     *  of the guided flow. Called once from the button tap, then again
     *  automatically from onResume/the notification callback each time a
     *  step completes, until everything is granted. */
    private void requestNextMissingPermission() {
        if (!isNotificationPermissionGranted()) {
            lastRequestedStep = "notifications";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            // Below API 33 notifications don't need a runtime prompt at
            // all — fall through immediately to the next missing step.
            else {
                requestNextMissingPermission();
            }
            return;
        }
        if (!isUsageAccessEnabled()) {
            lastRequestedStep = "usage";
            startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));
            android.widget.Toast.makeText(getContext(), "Step: enable Usage Access for Zenith, then come back.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (!isOverlayPermissionEnabled()) {
            lastRequestedStep = "overlay";
            startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName())));
            android.widget.Toast.makeText(getContext(), "Step: enable Draw Overlays for Zenith, then come back.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (!isAccessibilityServiceEnabled()) {
            lastRequestedStep = "accessibility";
            startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
            android.widget.Toast.makeText(getContext(), "Next step: turn on Zenith Protector under Accessibility, then come back.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (!isBatteryOptimizationDisabled()) {
            lastRequestedStep = "battery";
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                // Some OEMs (especially heavily customized ones) don't support
                // this intent directly — fall back to the general battery
                // optimization list where the user can find Zenith manually.
                startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
            android.widget.Toast.makeText(getContext(), "Last step: choose 'Allow' or 'Don't optimize' for Zenith, then come back.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        // Everything granted.
        guidedFlowActive  = false;
        lastRequestedStep = "";
        android.widget.Toast.makeText(getContext(), "All set! Zenith is fully protected. 🎉", android.widget.Toast.LENGTH_LONG).show();
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        android.content.Context ctx = getContext();
        if (ctx == null) return true;
        return androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private boolean isUsageAccessEnabled() {
        android.content.Context ctx = getContext();
        if (ctx == null) return true;
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) ctx.getSystemService(android.content.Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), ctx.getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }

    private boolean isAccessibilityServiceEnabled() {
        android.content.Context ctx = getContext();
        if (ctx == null) return false;
        int accessibilityEnabled = 0;
        final String service = ctx.getPackageName() + "/" + com.zenith.app.service.GuardianAccessibilityService.class.getName();
        try {
            accessibilityEnabled = android.provider.Settings.Secure.getInt(
                ctx.getApplicationContext().getContentResolver(),
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (android.provider.Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        android.text.TextUtils.SimpleStringSplitter mStringColonSplitter = new android.text.TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = android.provider.Settings.Secure.getString(
                ctx.getApplicationContext().getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOverlayPermissionEnabled() {
        android.content.Context ctx = getContext();
        if (ctx == null) return true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return android.provider.Settings.canDrawOverlays(ctx);
        }
        return true;
    }

    /** Without this, Doze mode and OEM battery managers (especially
     *  Xiaomi/Samsung/Oppo/Vivo) routinely suspend or throttle the
     *  accessibility service in the background — the tracking loop and
     *  screen-on/off receiver can silently stop running for stretches at a
     *  time, producing exactly the kind of gaps and "not working
     *  efficiently" behavior this step is meant to prevent. */
    private boolean isBatteryOptimizationDisabled() {
        android.content.Context ctx = getContext();
        if (ctx == null) return true;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true;
        android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(android.content.Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
    }

    private void applyClickBounce(View view, Runnable action) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(80)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .withEndAction(action)
                    .start();
            })
            .start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
