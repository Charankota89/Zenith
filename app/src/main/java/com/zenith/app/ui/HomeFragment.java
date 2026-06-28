package com.zenith.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.zenith.app.R;
import com.zenith.app.databinding.FragmentHomeBinding;
import com.zenith.app.module1_screen.usage.UsageViewModel;
import com.zenith.app.utils.DateUtils;

/**
 * Home / Dashboard Fragment.
 * Shows a quick summary: today's total screen time, study time, habit streaks.
 * Tapping any card navigates to its respective detail screen.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private UsageViewModel usageViewModel;

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

        usageViewModel = new ViewModelProvider(this).get(UsageViewModel.class);

        // Date header
        binding.tvDate.setText(DateUtils.friendlyDate(DateUtils.today()));

        // Navigate to Screen tab on card tap
        binding.cardScreenTime.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.usageFragment));

        observeUsage();
    }

    @Override
    public void onResume() {
        super.onResume();
        usageViewModel.syncUsage();
    }

    private void observeUsage() {
        usageViewModel.todayUsage.observe(getViewLifecycleOwner(), usageList -> {
            if (usageList == null) return;
            long totalMinutes = 0;
            for (com.zenith.app.data.db.entity.AppUsageEntity u : usageList) totalMinutes += u.usageMinutes;
            binding.tvScreenTimeValue.setText(DateUtils.formatMinutes(totalMinutes));
        });

        usageViewModel.getHasPermission().observe(getViewLifecycleOwner(), granted -> {
            binding.tvPermWarning.setVisibility(granted ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
