package com.zenith.app.ui.screen;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.zenith.app.databinding.ItemAppUsageBinding;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.TimeUtils;

public class AppUsageAdapter extends ListAdapter<AppUsageEntity, AppUsageAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(AppUsageEntity entity);
        void onFocusBlockToggle(AppUsageEntity entity, boolean isBlocked);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AppUsageAdapter() { super(DIFF); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AppUsageEntity item = getItem(position);
        holder.bind(item, position, listener);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                listener.onItemClick(item);
            }
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemAppUsageBinding b;
        VH(ItemAppUsageBinding b) { super(b.getRoot()); this.b = b; }

        void bind(AppUsageEntity e, int pos, OnItemClickListener listener) {
            b.tvAppName.setText(e.appName);
            b.tvUsageTime.setText(TimeUtils.formatDuration(e.usageTimeMillis));
            b.tvRank.setText(String.valueOf(pos + 1));

            long pct = e.limitMillis > 0 ? (e.usageTimeMillis * 100L) / e.limitMillis : 0;
            b.progressUsage.setProgress((int) Math.min(100, pct));

            try {
                Drawable icon = b.getRoot().getContext()
                    .getPackageManager().getApplicationIcon(e.packageName);
                b.ivAppIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException ex) {
                b.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            b.switchFocusBlock.setOnCheckedChangeListener(null);
            b.switchFocusBlock.setChecked(!e.isFocusWhitelisted);

            b.switchFocusBlock.setOnCheckedChangeListener((btn, isChecked) -> {
                if (listener != null) {
                    listener.onFocusBlockToggle(e, isChecked);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<AppUsageEntity> DIFF =
        new DiffUtil.ItemCallback<AppUsageEntity>() {
            @Override public boolean areItemsTheSame(@NonNull AppUsageEntity a, @NonNull AppUsageEntity b) {
                return a.id == b.id;
            }
            @Override public boolean areContentsTheSame(@NonNull AppUsageEntity a, @NonNull AppUsageEntity b) {
                return a.usageTimeMillis == b.usageTimeMillis && a.isLocked == b.isLocked;
            }
        };
}
