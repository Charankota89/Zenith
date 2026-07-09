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

        vm = new ViewModelProvider(this,
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
        });

        vm.sessionsCompleted.observe(getViewLifecycleOwner(), count ->
            binding.tvSessionCount.setText(count + " session" + (count == 1 ? "" : "s") + " completed today"));

        vm.timerState.observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case RUNNING:
                    binding.btnStart.setText("⏸ Pause");
                    binding.btnStop.setEnabled(true);
                    binding.tvTimerLabel.setText("Focus Time");
                    break;
                case PAUSED:
                    binding.btnStart.setText("▶ Resume");
                    binding.btnStop.setEnabled(true);
                    binding.tvTimerLabel.setText("Paused");
                    break;
                case BREAK:
                    binding.btnStart.setEnabled(false);
                    binding.tvTimerLabel.setText("Break Time 🌿");
                    break;
                case IDLE:
                default:
                    binding.btnStart.setText("▶ Start");
                    binding.btnStart.setEnabled(true);
                    binding.btnStop.setEnabled(false);
                    binding.tvTimerLabel.setText("Pomodoro Timer");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
