package com.zenith.app.ui.screen;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zenith.app.databinding.ItemBrowserVisitBinding;
import com.zenith.app.db.entity.BrowserVisitEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrowserVisitAdapter extends RecyclerView.Adapter<BrowserVisitAdapter.VH> {

    private List<BrowserVisitEntity> items = new ArrayList<>();

    public void setItems(List<BrowserVisitEntity> list) {
        items = list == null ? new ArrayList<>() : list;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemBrowserVisitBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BrowserVisitEntity v = items.get(pos);
        h.b.tvVisitDomain.setText(extractDomain(v.url));
        h.b.tvVisitUrl.setText(v.url);
        h.b.tvVisitTime.setText(formatTime(v.visitedAt));
        // Show browser icon based on package
        h.b.tvBrowserIcon.setText(iconForBrowser(v.browserPackage));
    }

    @Override public int getItemCount() { return items.size(); }

    private String extractDomain(String url) {
        if (url == null) return "";
        try {
            String d = url.replaceFirst("https?://", "").replaceFirst("www\\.", "");
            int s = d.indexOf('/');
            return s > 0 ? d.substring(0, s) : d;
        } catch (Exception e) { return url; }
    }

    private String formatTime(long epochMs) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(new Date(epochMs));
    }

    private String iconForBrowser(String pkg) {
        if (pkg == null) return "🌐";
        switch (pkg) {
            case "com.android.chrome":         return "🟦"; // Chrome
            case "org.mozilla.firefox":        return "🦊"; // Firefox
            case "com.microsoft.emmx":         return "🔷"; // Edge
            case "com.brave.browser":          return "🦁"; // Brave
            case "com.opera.browser":          return "🔴"; // Opera
            default:                           return "🌐";
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBrowserVisitBinding b;
        VH(ItemBrowserVisitBinding b) { super(b.getRoot()); this.b = b; }
    }
}
