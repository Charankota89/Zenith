package com.zenith.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.zenith.app.databinding.FragmentHomeBinding;
import com.zenith.app.util.TimeUtils;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel       vm;

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
        vm = new ViewModelProvider(this,
            new HomeViewModelFactory(requireContext())).get(HomeViewModel.class);

        binding.tvGreeting.setText("Good day — " + TimeUtils.getDayOfWeek());
        binding.tvTagline.setText("Rise above. Stay focused.");

        vm.totalScreenTime.observe(getViewLifecycleOwner(), millis -> {
            long hours   = millis / 3600000;
            long minutes = (millis % 3600000) / 60000;
            binding.tvScreenTime.setText(hours + "h " + minutes + "m");
        });

        vm.habitsDoneToday.observe(getViewLifecycleOwner(), count ->
            binding.tvHabitCount.setText(count + " habits done today"));

        vm.studyTimeToday.observe(getViewLifecycleOwner(), millis ->
            binding.tvStudyTime.setText(TimeUtils.formatDuration(millis) + " studied"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
