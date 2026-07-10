package com.zenith.app.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.zenith.app.databinding.FragmentHomeBinding;
import com.zenith.app.R;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.ui.settings.SettingsFragment;
import com.zenith.app.util.TimeUtils;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel       vm;

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
        });

        vm.habitsDoneToday.observe(getViewLifecycleOwner(), count ->
            binding.tvHabitCount.setText(count + " habits done today"));

        vm.studyTimeToday.observe(getViewLifecycleOwner(), millis ->
            binding.tvStudyTime.setText(TimeUtils.formatDuration(millis) + " studied"));
    }

    @Override
    public void onResume() {
        super.onResume();
        checkSystemPermissions();
    }

    private void checkSystemPermissions() {
        boolean accessEnabled = isAccessibilityServiceEnabled();
        boolean overlayEnabled = isOverlayPermissionEnabled();

        if (!accessEnabled || !overlayEnabled) {
            binding.cardPermissionWarning.setVisibility(View.VISIBLE);
            if (!accessEnabled && !overlayEnabled) {
                binding.tvPermissionDesc.setText("Zenith needs Accessibility Service to track active apps and Overlay permission to display lock screens. Tap below to configure.");
                binding.btnGrantPermissions.setOnClickListener(v -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    android.widget.Toast.makeText(getContext(), "Please enable Draw Overlays for Zenith, then enable Accessibility.", android.widget.Toast.LENGTH_LONG).show();
                });
            } else if (!accessEnabled) {
                binding.tvPermissionDesc.setText("Zenith Accessibility Service is disabled. Please enable it in Settings to track and lock apps.");
                binding.btnGrantPermissions.setOnClickListener(v -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                });
            } else {
                binding.tvPermissionDesc.setText("Zenith Overlay permission is disabled. Please allow drawing over other apps.");
                binding.btnGrantPermissions.setOnClickListener(v -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                });
            }
        } else {
            binding.cardPermissionWarning.setVisibility(View.GONE);
        }
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
