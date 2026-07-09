package com.zenith.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zenith.app.databinding.FragmentSettingsBinding;
import com.zenith.app.service.UsageMonitorService;
import com.zenith.app.util.AppConstants;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext()
            .getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);

        // Career mode toggle
        boolean careerActive = prefs.getBoolean(AppConstants.PREF_CAREER_ACTIVE, false);
        binding.switchCareerMode.setChecked(careerActive);
        binding.switchCareerMode.setOnCheckedChangeListener((btn, isChecked) ->
            prefs.edit().putBoolean(AppConstants.PREF_CAREER_ACTIVE, isChecked).apply());

        // Daily goal
        int goalMins = prefs.getInt(AppConstants.PREF_DAILY_GOAL_MIN, 120);
        binding.tvDailyGoalValue.setText(goalMins + " min");

        binding.btnDecreaseGoal.setOnClickListener(v -> {
            int cur = prefs.getInt(AppConstants.PREF_DAILY_GOAL_MIN, 120);
            if (cur > 15) {
                cur -= 15;
                prefs.edit().putInt(AppConstants.PREF_DAILY_GOAL_MIN, cur).apply();
                binding.tvDailyGoalValue.setText(cur + " min");
            }
        });

        binding.btnIncreaseGoal.setOnClickListener(v -> {
            int cur = prefs.getInt(AppConstants.PREF_DAILY_GOAL_MIN, 120);
            cur += 15;
            prefs.edit().putInt(AppConstants.PREF_DAILY_GOAL_MIN, cur).apply();
            binding.tvDailyGoalValue.setText(cur + " min");
        });

        // PIN setup
        binding.btnSetPin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PinSetupActivity.class);
            startActivity(intent);
        });

        // Restart service
        binding.btnRestartService.setOnClickListener(v -> {
            requireContext().startForegroundService(
                new Intent(requireContext(), UsageMonitorService.class));
            Toast.makeText(requireContext(),
                "Usage Monitor restarted", Toast.LENGTH_SHORT).show();
        });

        // App version
        binding.tvAppVersion.setText("Zenith v1.0  •  Focus. Control. Your Peak.");
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
