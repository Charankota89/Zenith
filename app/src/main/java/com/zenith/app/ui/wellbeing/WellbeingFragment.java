package com.zenith.app.ui.wellbeing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.zenith.app.databinding.FragmentWellbeingBinding;

public class WellbeingFragment extends Fragment {

    private FragmentWellbeingBinding binding;
    private WellbeingViewModel       vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWellbeingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this,
            new WellbeingViewModelFactory(requireContext())).get(WellbeingViewModel.class);

        // Mood selector (1–5 emoji) — save to DB on tap
        int[] moodBtns = {
            com.zenith.app.R.id.btnMood1, com.zenith.app.R.id.btnMood2,
            com.zenith.app.R.id.btnMood3, com.zenith.app.R.id.btnMood4,
            com.zenith.app.R.id.btnMood5
        };
        for (int i = 0; i < moodBtns.length; i++) {
            int score = i + 1;
            binding.getRoot().findViewById(moodBtns[i]).setOnClickListener(v -> {
                animateBounce(v);
                binding.tvMoodFeedback.setText(getMoodFeedback(score));
                vm.saveMood(score, null);  // Save to Room DB
            });
        }
    }

    private void animateBounce(View view) {
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.25f, 1.0f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.25f, 1.0f);
        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(350);
        set.setInterpolator(new android.view.animation.OvershootInterpolator(2.0f));
        set.start();
    }

    private String getMoodFeedback(int score) {
        switch (score) {
            case 1: return "That's okay. Tomorrow is a new day.";
            case 2: return "Hang in there. You're stronger than you think.";
            case 3: return "Decent. Keep pushing forward.";
            case 4: return "Feeling good. Keep the momentum!";
            case 5: return "You're on fire! Ride this energy!";
            default: return "";
        }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
