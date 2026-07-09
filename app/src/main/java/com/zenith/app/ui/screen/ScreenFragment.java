package com.zenith.app.ui.screen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
