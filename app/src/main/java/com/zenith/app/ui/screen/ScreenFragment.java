package com.zenith.app.ui.screen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zenith.app.databinding.FragmentScreenBinding;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.TimeUtils;

public class ScreenFragment extends Fragment {

    private FragmentScreenBinding b;
    private ScreenViewModel       vm;
    private AppUsageAdapter       usageAdapter;
    private BrowserVisitAdapter   browserAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle saved) {
        b = FragmentScreenBinding.inflate(inf, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);

        vm = new ViewModelProvider(this,
            new ScreenViewModelFactory(requireContext()))
            .get(ScreenViewModel.class);

        // App usage RecyclerView
        usageAdapter = new AppUsageAdapter();
        b.rvAppUsage.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvAppUsage.setAdapter(usageAdapter);
        b.rvAppUsage.addItemDecoration(
            new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        usageAdapter.setOnItemClickListener(new AppUsageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppUsageEntity entity) {
                long currentLimitMins = entity.limitMillis / 60000;
                int initialHour = (int) (currentLimitMins / 60);
                int initialMinute = (int) (currentLimitMins % 60);

                com.google.android.material.timepicker.MaterialTimePicker picker = 
                    new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                        .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                        .setHour(initialHour)
                        .setMinute(initialMinute)
                        .setTitleText("Set daily limit: " + entity.appName)
                        .setInputMode(com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK)
                        .build();

                picker.addOnPositiveButtonClickListener(v -> {
                    int hour = picker.getHour();
                    int minute = picker.getMinute();
                    long totalMins = (hour * 60L) + minute;
                    long newLimitMillis = totalMins * 60000;

                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        com.zenith.app.db.AppDatabase db = com.zenith.app.db.AppDatabase.getInstance(requireContext().getApplicationContext());
                        entity.limitMillis = newLimitMillis;
                        if (entity.usageTimeMillis < newLimitMillis || newLimitMillis == 0) {
                            entity.isLocked = false;
                            entity.unlockExpiresAt = 0;
                        }
                        db.appUsageDao().update(entity);
                        
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                vm.syncUsage();
                                Toast.makeText(requireContext(), "Limit updated successfully!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });

                picker.show(getChildFragmentManager(), "LIMIT_TIME_PICKER");
            }

            @Override
            public void onFocusBlockToggle(AppUsageEntity entity, boolean isBlocked) {
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    com.zenith.app.db.AppDatabase db = com.zenith.app.db.AppDatabase.getInstance(requireContext().getApplicationContext());
                    entity.isFocusWhitelisted = !isBlocked;
                    db.appUsageDao().update(entity);
                    
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            String status = isBlocked ? "restricted" : "allowed";
                            Toast.makeText(requireContext(), entity.appName + " is now " + status + " in Focus Mode.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        // Browser visits RecyclerView
        browserAdapter = new BrowserVisitAdapter();
        b.rvBrowserVisits.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvBrowserVisits.setAdapter(browserAdapter);

        // Observe app usage
        vm.usageList.observe(getViewLifecycleOwner(), list -> {
            usageAdapter.submitList(list);  // ListAdapter uses submitList
            long totalMs = 0;
            if (list != null) {
                for (AppUsageEntity e : list) totalMs += e.usageTimeMillis;
            }
            b.tvTotalTime.setText(TimeUtils.formatDuration(totalMs));
            b.tvAppCount.setText((list == null ? 0 : list.size()) + " apps tracked");
        });

        // Observe browser history
        vm.browserList.observe(getViewLifecycleOwner(), visits -> {
            browserAdapter.setItems(visits);
            int count = visits == null ? 0 : visits.size();
            b.tvBrowserCount.setText(count + " sites");
            b.tvBrowserEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            b.rvBrowserVisits.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });

        // Sync button
        b.btnSync.setOnClickListener(v -> vm.syncUsage());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
