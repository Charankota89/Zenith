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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.zenith.app.databinding.FragmentScreenBinding;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.TimeUtils;
import java.util.ArrayList;
import java.util.List;

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

        setupWeeklyTrendChart();
        vm.weeklyTrend.observe(getViewLifecycleOwner(), this::renderWeeklyTrend);

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

    private void setupWeeklyTrendChart() {
        BarChart chart = b.weeklyTrendChart;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setDrawValueAboveBar(true);
        chart.setExtraBottomOffset(4f);
        chart.setNoDataText("Not enough data yet");

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(android.graphics.Color.parseColor("#14636AF1"));
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setTextColor(android.graphics.Color.parseColor("#94A3B8"));
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisLeft().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return TimeUtils.millisToMinutes((long) value) + "m";
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setTextColor(android.graphics.Color.parseColor("#94A3B8"));
        chart.getXAxis().setTextSize(10f);
        chart.getXAxis().setGranularity(1f);
    }

    private void renderWeeklyTrend(List<ScreenViewModel.DayUsagePoint> points) {
        if (points == null || points.isEmpty()) return;

        List<BarEntry> entries = new ArrayList<>();
        List<String>   labels  = new ArrayList<>();
        long total = 0;
        long max   = 0;
        int  maxIndex = 0;

        for (int i = 0; i < points.size(); i++) {
            ScreenViewModel.DayUsagePoint p = points.get(i);
            float minutes = p.millis / 60000f;
            entries.add(new BarEntry(i, minutes));
            labels.add(p.label);
            total += p.millis;
            if (p.millis > max) { max = p.millis; maxIndex = i; }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Screen Time");
        dataSet.setColor(android.graphics.Color.parseColor("#6366F1"));
        dataSet.setHighLightColor(android.graphics.Color.parseColor("#4F46E5"));
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(android.graphics.Color.parseColor("#64748B"));
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value < 1 ? "" : Math.round(value) + "m";
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);

        b.weeklyTrendChart.setData(data);
        b.weeklyTrendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        b.weeklyTrendChart.animateY(500);
        b.weeklyTrendChart.invalidate();

        b.tvTrendInsight.setText(buildWeeklyInsight(points, total, maxIndex));
    }

    /** A short, human insight about the week's pattern — the kind of thing
     *  that makes the data feel like it's actually telling you something,
     *  not just a chart for its own sake. */
    private String buildWeeklyInsight(List<ScreenViewModel.DayUsagePoint> points, long totalMillis, int maxIndex) {
        int daysWithData = 0;
        for (ScreenViewModel.DayUsagePoint p : points) if (p.millis > 0) daysWithData++;
        if (daysWithData == 0) return "Start using your phone to see your weekly trend here.";

        long avgMillis = totalMillis / points.size();
        ScreenViewModel.DayUsagePoint today = points.get(points.size() - 1);

        if (points.size() >= 2 && today.millis > 0) {
            ScreenViewModel.DayUsagePoint yesterday = points.get(points.size() - 2);
            if (yesterday.millis > 0) {
                long diffPct = ((today.millis - yesterday.millis) * 100) / yesterday.millis;
                if (diffPct <= -10) {
                    return "📉 Down " + Math.abs(diffPct) + "% from yesterday — nice work.";
                } else if (diffPct >= 10) {
                    return "📈 Up " + diffPct + "% from yesterday.";
                }
            }
        }

        if (avgMillis > 0) {
            return "Averaging " + TimeUtils.formatDuration(avgMillis) + "/day this week • Highest: " + points.get(maxIndex).label;
        }
        return "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
