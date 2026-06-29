package com.zenith.app.ui.screen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zenith.app.databinding.FragmentScreenBinding;

public class ScreenFragment extends Fragment {

    private FragmentScreenBinding binding;
    private ScreenViewModel       vm;
    private AppUsageAdapter       adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this,
            new ScreenViewModelFactory(requireContext())).get(ScreenViewModel.class);

        adapter = new AppUsageAdapter();
        binding.rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvApps.setAdapter(adapter);

        vm.usageList.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            if (list != null && !list.isEmpty()) {
                long totalMillis = list.stream()
                    .mapToLong(e -> e.usageTimeMillis).sum();
                long hours   = totalMillis / 3600000;
                long minutes = (totalMillis % 3600000) / 60000;
                binding.tvTotalTime.setText("Total: " + hours + "h " + minutes + "m");
            }
        });

        binding.btnSync.setOnClickListener(v -> vm.syncUsage());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
