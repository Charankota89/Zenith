package com.zenith.app.ui.focus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zenith.app.databinding.FragmentFocusBinding;
import com.zenith.app.util.AppConstants;

public class FocusFragment extends Fragment {

    private FragmentFocusBinding binding;

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
        SharedPreferences prefs = requireContext()
            .getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        boolean active = prefs.getBoolean(AppConstants.PREF_FOCUS_ACTIVE, false);

        binding.switchFocusMode.setChecked(active);
        updateFocusUI(active);

        binding.switchFocusMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(AppConstants.PREF_FOCUS_ACTIVE, isChecked).apply();
            updateFocusUI(isChecked);
        });
    }

    private void updateFocusUI(boolean active) {
        binding.tvFocusStatus.setText(active ? "Focus Mode: ON" : "Focus Mode: OFF");
        binding.tvFocusDesc.setText(active
            ? "All non-whitelisted apps will be blocked."
            : "Turn on to block distracting apps during study time.");
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
