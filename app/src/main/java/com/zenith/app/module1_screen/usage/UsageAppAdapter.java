package com.zenith.app.module1_screen.usage;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zenith.app.data.db.entity.AppUsageEntity;
import com.zenith.app.databinding.ItemAppUsageBinding;
import com.zenith.app.utils.DateUtils;

/**
 * RecyclerView adapter for the per-app usage list.
 * Uses ListAdapter with DiffCallback for efficient updates.
 * App icons are loaded via PackageManager — no external image-loading library needed.
 */
public class UsageAppAdapter extends ListAdapter<AppUsageEntity, UsageAppAdapter.ViewHolder> {

    /** 2-hour reference: reaching 120 min = 100% on the progress bar */
    private static final long MAX_MINUTES_REFERENCE = 120L;

    public UsageAppAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppUsageBinding binding = ItemAppUsageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAppUsageBinding binding;

        ViewHolder(ItemAppUsageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppUsageEntity entity, int position) {
            binding.tvAppName.setText(entity.appName);
            binding.tvUsageTime.setText(DateUtils.formatMinutes(entity.usageMinutes));

            // Progress bar — capped at 100%
            int progress = (int) Math.min(100L,
                    (entity.usageMinutes * 100L) / MAX_MINUTES_REFERENCE);
            binding.progressUsage.setProgress(progress);

            // Rank number (1-indexed)
            binding.tvRank.setText(String.valueOf(position + 1));

            // App icon via PackageManager
            try {
                PackageManager pm = binding.getRoot().getContext().getPackageManager();
                Drawable icon = pm.getApplicationIcon(entity.packageName);
                binding.ivAppIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            // Color the progress bar based on position (top 1 = red, 2-3 = amber, rest = green)
            int color;
            if (position == 0) {
                color = binding.getRoot().getContext()
                        .getColor(com.zenith.app.R.color.usage_high);
            } else if (position < 3) {
                color = binding.getRoot().getContext()
                        .getColor(com.zenith.app.R.color.usage_medium);
            } else {
                color = binding.getRoot().getContext()
                        .getColor(com.zenith.app.R.color.usage_low);
            }
            binding.progressUsage.setIndicatorColor(color);
        }
    }

    private static final DiffUtil.ItemCallback<AppUsageEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppUsageEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppUsageEntity a,
                                               @NonNull AppUsageEntity b) {
                    return a.packageName.equals(b.packageName) && a.date.equals(b.date);
                }

                @Override
                public boolean areContentsTheSame(@NonNull AppUsageEntity a,
                                                  @NonNull AppUsageEntity b) {
                    return a.usageMinutes == b.usageMinutes;
                }
            };
}
