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
import com.zenith.app.R;
import com.zenith.app.ui.settings.SettingsFragment;
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

        // Staggered entering animations for cards
        android.view.animation.Animation anim1 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);
        android.view.animation.Animation anim2 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);
        android.view.animation.Animation anim3 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);

        anim1.setStartOffset(100);
        anim2.setStartOffset(200);
        anim3.setStartOffset(300);

        binding.cardScreenTime.startAnimation(anim1);
        binding.cardHabits.startAnimation(anim2);
        binding.cardStudyTime.startAnimation(anim3);

        binding.btnSettings.setOnClickListener(v -> {
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new SettingsFragment())
                .addToBackStack(null)
                .commit();
        });

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
