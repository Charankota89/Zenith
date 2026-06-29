package com.zenith.app.ui.career;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.zenith.app.databinding.ItemHabitBinding;
import com.zenith.app.db.entity.HabitEntity;

public class HabitAdapter extends ListAdapter<HabitEntity, HabitAdapter.VH> {

    public interface OnHabitClick { void onComplete(HabitEntity habit); }

    private final OnHabitClick listener;

    public HabitAdapter(OnHabitClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemHabitBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        HabitEntity e = getItem(position);
        h.b.tvHabitName.setText(e.habitName);
        h.b.tvStreak.setText("🔥 " + e.currentStreak + " day streak");
        h.b.btnComplete.setEnabled(!e.completedToday);
        h.b.btnComplete.setText(e.completedToday ? "✓ Done" : "Complete");
        h.b.btnComplete.setOnClickListener(v -> listener.onComplete(e));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemHabitBinding b;
        VH(ItemHabitBinding b) { super(b.getRoot()); this.b = b; }
    }

    private static final DiffUtil.ItemCallback<HabitEntity> DIFF =
        new DiffUtil.ItemCallback<HabitEntity>() {
            @Override public boolean areItemsTheSame(@NonNull HabitEntity a, @NonNull HabitEntity b) {
                return a.id == b.id;
            }
            @Override public boolean areContentsTheSame(@NonNull HabitEntity a, @NonNull HabitEntity b) {
                return a.completedToday == b.completedToday && a.currentStreak == b.currentStreak;
            }
        };
}
