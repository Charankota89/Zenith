package com.zenith.app.ui.focus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.zenith.app.databinding.FragmentFocusBinding;
import com.zenith.app.util.AppConstants;

public class FocusFragment extends Fragment {

    private FragmentFocusBinding binding;
    private FocusViewModel       vm;
    private android.animation.AnimatorSet timerPulsingAnimator;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFocusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(requireActivity(),
            new FocusViewModelFactory(requireContext())).get(FocusViewModel.class);

        // Focus mode switch
        SharedPreferences prefs = requireContext()
            .getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        boolean active = prefs.getBoolean(AppConstants.PREF_FOCUS_ACTIVE, false);
        binding.switchFocusMode.setChecked(active);
        binding.switchFocusMode.setOnCheckedChangeListener((btn, isChecked) ->
            prefs.edit().putBoolean(AppConstants.PREF_FOCUS_ACTIVE, isChecked).apply());

        // Observe timer state
        vm.timeLeftMillis.observe(getViewLifecycleOwner(), ms -> {
            long mins = ms / 60000;
            long secs = (ms % 60000) / 1000;
            binding.tvTimerDisplay.setText(String.format("%02d:%02d", mins, secs));

            FocusViewModel.TimerState state = vm.timerState.getValue();
            long totalMs;
            if (state == FocusViewModel.TimerState.BREAK) {
                totalMs = 5 * 60 * 1000L;
            } else {
                // Was hardcoded to 25 minutes here regardless of the actual
                // selected duration — the progress ring would fill up at
                // the wrong rate for any non-default duration.
                Integer durationMin = vm.selectedDurationMin.getValue();
                totalMs = (durationMin != null ? durationMin : 25) * 60000L;
            }
            int progress = totalMs > 0 ? (int) ((ms * 100L) / totalMs) : 0;
            binding.focusProgressIndicator.setProgress(progress);
        });

        vm.selectedDurationMin.observe(getViewLifecycleOwner(), this::highlightSelectedChip);

        vm.sessionsCompleted.observe(getViewLifecycleOwner(), count ->
            binding.tvSessionCount.setText(count + " session" + (count == 1 ? "" : "s") + " completed today"));

        vm.timerState.observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case RUNNING:
                    binding.btnStart.setText("⏸ Pause");
                    binding.btnStop.setEnabled(true);
                    binding.tvTimerLabel.setText("Focus Time");
                    startTimerPulsing();
                    break;
                case PAUSED:
                    binding.btnStart.setText("▶ Resume");
                    binding.btnStop.setEnabled(true);
                    binding.tvTimerLabel.setText("Paused");
                    stopTimerPulsing();
                    break;
                case BREAK:
                    binding.btnStart.setEnabled(false);
                    binding.tvTimerLabel.setText("Break Time 🌿");
                    stopTimerPulsing();
                    break;
                case IDLE:
                default:
                    binding.btnStart.setText("▶ Start");
                    binding.btnStart.setEnabled(true);
                    binding.btnStop.setEnabled(false);
                    binding.tvTimerLabel.setText("Pomodoro Timer");
                    stopTimerPulsing();
                    break;
            }
        });

        vm.currentSubject.observe(getViewLifecycleOwner(), subject ->
            binding.tvSubjectName.setText(subject));

        // Button listeners
        binding.btnStart.setOnClickListener(v -> {
            FocusViewModel.TimerState state = vm.timerState.getValue();
            if (state == FocusViewModel.TimerState.RUNNING) {
                vm.pauseTimer();
            } else {
                vm.startTimer();
            }
        });

        binding.btnStop.setOnClickListener(v -> vm.stopTimer());

        binding.tvSubjectName.setOnClickListener(v -> showSubjectDialog());

        binding.chip15min.setOnClickListener(v -> vm.setDurationMinutes(15));
        binding.chip25min.setOnClickListener(v -> vm.setDurationMinutes(25));
        binding.chip45min.setOnClickListener(v -> vm.setDurationMinutes(45));
        binding.chip60min.setOnClickListener(v -> vm.setDurationMinutes(60));
        binding.chipCustomMin.setOnClickListener(v -> showCustomDurationDialog());
    }

    private void highlightSelectedChip(int selectedMin) {
        if (binding == null) return;
        int[] presetValues = {15, 25, 45, 60};
        android.widget.TextView[] chips = {
            binding.chip15min, binding.chip25min, binding.chip45min, binding.chip60min
        };
        boolean matchedPreset = false;
        for (int i = 0; i < presetValues.length; i++) {
            boolean isSelected = presetValues[i] == selectedMin;
            if (isSelected) matchedPreset = true;
            chips[i].setTextColor(isSelected
                ? getResources().getColor(com.zenith.app.R.color.zenith_purple)
                : getResources().getColor(com.zenith.app.R.color.text_secondary));
            chips[i].setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        // Custom chip shows the actual chosen value once it doesn't match a preset
        if (!matchedPreset) {
            binding.chipCustomMin.setText(selectedMin + "m");
            binding.chipCustomMin.setTextColor(getResources().getColor(com.zenith.app.R.color.zenith_purple));
            binding.chipCustomMin.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            binding.chipCustomMin.setText("Custom");
            binding.chipCustomMin.setTextColor(getResources().getColor(com.zenith.app.R.color.text_secondary));
            binding.chipCustomMin.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void showCustomDurationDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Minutes (1–180)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        Integer current = vm.selectedDurationMin.getValue();
        if (current != null) input.setText(String.valueOf(current));

        new AlertDialog.Builder(requireContext())
            .setTitle("Custom study duration")
            .setView(input)
            .setPositiveButton("Set", (d, w) -> {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        int minutes = Integer.parseInt(text);
                        vm.setDurationMinutes(minutes);
                    } catch (NumberFormatException ignored) { }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showSubjectDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("e.g. Mathematics, Coding...");
        String current = vm.currentSubject.getValue();
        if (current != null) input.setText(current);

        new AlertDialog.Builder(requireContext())
            .setTitle("What are you studying?")
            .setView(input)
            .setPositiveButton("Set", (d, w) -> {
                String s = input.getText().toString().trim();
                if (!s.isEmpty()) vm.setSubject(s);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void startTimerPulsing() {
        if (timerPulsingAnimator != null && timerPulsingAnimator.isRunning()) return;
        
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(binding.tvTimerDisplay, "scaleX", 1.0f, 1.04f, 1.0f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(binding.tvTimerDisplay, "scaleY", 1.0f, 1.04f, 1.0f);
        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        
        timerPulsingAnimator = new android.animation.AnimatorSet();
        timerPulsingAnimator.playTogether(scaleX, scaleY);
        timerPulsingAnimator.setDuration(1200);
        timerPulsingAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        timerPulsingAnimator.start();
    }

    private void stopTimerPulsing() {
        if (timerPulsingAnimator != null) {
            timerPulsingAnimator.cancel();
            timerPulsingAnimator = null;
        }
        if (binding != null && binding.tvTimerDisplay != null) {
            binding.tvTimerDisplay.setScaleX(1.0f);
            binding.tvTimerDisplay.setScaleY(1.0f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimerPulsing();
        binding = null;
    }
}
