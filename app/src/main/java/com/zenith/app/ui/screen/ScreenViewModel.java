package com.zenith.app.ui.screen;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.db.entity.BrowserVisitEntity;
import com.zenith.app.repository.UsageRepository;
import com.zenith.app.util.TimeUtils;
import java.util.List;

public class ScreenViewModel extends ViewModel {

    public final LiveData<List<AppUsageEntity>>    usageList;
    public final LiveData<List<BrowserVisitEntity>> browserList;
    private final UsageRepository repo;

    public ScreenViewModel(Context context) {
        repo        = new UsageRepository(context);
        usageList   = AppDatabase.getInstance(context)
            .appUsageDao()
            .getUsageForDate(TimeUtils.getTodayDate());
        browserList = AppDatabase.getInstance(context)
            .browserVisitDao()
            .getVisitsForDate(TimeUtils.getTodayDate());
    }

    public void syncUsage() {
        repo.syncTodayUsage();
    }

    public void setLimit(String pkg, long limitMillis) {
        repo.setAppLimit(pkg, limitMillis);
    }
}
