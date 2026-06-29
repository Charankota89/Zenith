package com.zenith.app.ui.career;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zenith.app.databinding.FragmentCareerBinding;

public class CareerFragment extends Fragment {

    private FragmentCareerBinding binding;
    private HabitAdapter          adapter;
    private CareerViewModel       vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCareerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this,
            new CareerViewModelFactory(requireContext())).get(CareerViewModel.class);

        adapter = new HabitAdapter(habit -> vm.completeHabit(habit.id));
        binding.rvHabits.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHabits.setAdapter(adapter);

        vm.habits.observe(getViewLifecycleOwner(), adapter::submitList);

        binding.fabAddHabit.setOnClickListener(v -> {
            String name = binding.etHabitName.getText().toString().trim();
            if (!name.isEmpty()) {
                vm.addHabit(name);
                binding.etHabitName.setText("");
            }
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
