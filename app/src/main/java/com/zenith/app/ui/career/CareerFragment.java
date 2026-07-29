package com.zenith.app.ui.career;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

        // Long-press → confirm delete — a confirmation dialog prevents
        // accidental deletion of a habit with an active streak.
        adapter.setOnDeleteListener(habit -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Delete habit?")
                .setMessage("\"" + habit.habitName + "\" and its " + habit.currentStreak
                    + "-day streak will be permanently deleted.")
                .setPositiveButton("Delete", (d, w) -> vm.deleteHabit(habit))
                .setNegativeButton("Cancel", null)
                .show();
        });

        vm.habits.observe(getViewLifecycleOwner(), habits -> {
            adapter.submitList(habits);
            boolean isEmpty = habits == null || habits.isEmpty();
            binding.emptyHabitsState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

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
