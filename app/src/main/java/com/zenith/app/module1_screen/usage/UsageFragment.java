package com.zenith.app.module1_screen.usage;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.zenith.app.R;
import com.zenith.app.data.db.dao.AppUsageDao;
import com.zenith.app.databinding.FragmentUsageBinding;
import com.zenith.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment — Usage Tracker Screen (Module 1, Feature 1).
 * Shows:
 *  - Weekly bar chart of daily total screen time
 *  - RecyclerView of today's per-app usage
 */
public class UsageFragment extends Fragment {

    private FragmentUsageBinding binding;
    private UsageViewModel viewModel;
    private UsageAppAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUsageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(UsageViewModel.class);

        setupRecyclerView();
        setupBarChart();
        observeViewModel();
        setupSwipeRefresh();

        // Permission banner action
        binding.btnGrantUsage.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.checkPermission();
        viewModel.syncUsage();
    }

    // ─── Setup ───────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new UsageAppAdapter();
        binding.recyclerUsage.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsage.setAdapter(adapter);
        binding.recyclerUsage.setHasFixedSize(true);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.syncUsage());
        binding.swipeRefresh.setColorSchemeColors(
                requireContext().getColor(R.color.zenith_purple),
                requireContext().getColor(R.color.zenith_blue)
        );
    }

    private void setupBarChart() {
        BarChart chart = binding.chartWeeklyUsage;

        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setFitBars(true);
        chart.setExtraBottomOffset(10f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#B0BEC5"));
        xAxis.setTextSize(11f);
        xAxis.setAxisLineColor(Color.parseColor("#2A2D3E"));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#1A1D2E"));
        leftAxis.setTextColor(Color.parseColor("#B0BEC5"));
        leftAxis.setTextSize(10f);
        leftAxis.setAxisLineColor(Color.parseColor("#2A2D3E"));
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateUtils.formatMinutes((long) value);
            }
        });

        chart.getAxisRight().setEnabled(false);
    }

    // ─── Observe ─────────────────────────────────────────────

    private void observeViewModel() {
        // Permission state
        viewModel.getHasPermission().observe(getViewLifecycleOwner(), granted -> {
            binding.bannerNoPermission.setVisibility(granted ? View.GONE : View.VISIBLE);
            binding.contentGroup.setVisibility(granted ? View.VISIBLE : View.GONE);
        });

        // Loading state
        viewModel.isSyncing().observe(getViewLifecycleOwner(), syncing -> {
            binding.swipeRefresh.setRefreshing(syncing);
        });

        // Today's app list
        viewModel.todayUsage.observe(getViewLifecycleOwner(), usageList -> {
            adapter.submitList(usageList);
            binding.tvNoData.setVisibility(
                    usageList == null || usageList.isEmpty() ? View.VISIBLE : View.GONE
            );

            // Update today total card
            if (usageList != null) {
                long totalMinutes = 0;
                for (com.zenith.app.data.db.entity.AppUsageEntity u : usageList) totalMinutes += u.usageMinutes;
                binding.tvTodayTotal.setText(DateUtils.formatMinutes(totalMinutes));
                binding.tvAppCount.setText(usageList.size() + " apps tracked");
            }
        });

        // Weekly bar chart
        viewModel.weeklyDailyTotals.observe(getViewLifecycleOwner(), dailyTotals -> {
            if (dailyTotals == null || dailyTotals.isEmpty()) return;
            updateBarChart(dailyTotals);
        });
    }

    // ─── Chart population ────────────────────────────────────

    private void updateBarChart(List<AppUsageDao.DailyTotalUsage> dailyTotals) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < dailyTotals.size(); i++) {
            AppUsageDao.DailyTotalUsage total = dailyTotals.get(i);
            entries.add(new BarEntry(i, total.totalMinutes));
            labels.add(DateUtils.dayLabel(total.date));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Screen Time");
        dataSet.setColor(requireContext().getColor(R.color.zenith_purple));
        dataSet.setValueTextColor(Color.parseColor("#B0BEC5"));
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value < 1f) return "";
                return DateUtils.formatMinutes((long) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        BarChart chart = binding.chartWeeklyUsage;
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setLabelCount(labels.size());
        chart.setData(barData);
        chart.invalidate();
        chart.animateY(600);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
