package com.zenith.app.ui.screen;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.dao.AppUsageDao;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.db.entity.BrowserVisitEntity;
import com.zenith.app.repository.UsageRepository;
import com.zenith.app.util.TimeUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenViewModel extends ViewModel {

    /** One day's worth of usage, ready for the weekly bar chart. */
    public static class DayUsagePoint {
        public final String label;   // e.g. "Mon"
        public final long   millis;
        public DayUsagePoint(String label, long millis) {
            this.label = label;
            this.millis = millis;
        }
    }

    public final LiveData<List<AppUsageEntity>>     usageList;
    public final LiveData<List<BrowserVisitEntity>> browserList;
    public final MediatorLiveData<List<DayUsagePoint>> weeklyTrend = new MediatorLiveData<>();
    private final UsageRepository repo;

    private static final int TREND_DAYS = 7;

    public ScreenViewModel(Context context) {
        repo        = new UsageRepository(context);
        usageList   = AppDatabase.getInstance(context)
            .appUsageDao()
            .getUsageForDate(TimeUtils.getTodayDate());
        browserList = AppDatabase.getInstance(context)
            .browserVisitDao()
            .getVisitsForDate(TimeUtils.getTodayDate());

        AppUsageDao dao = AppDatabase.getInstance(context).appUsageDao();
        String startDate = TimeUtils.getDateDaysAgo(TREND_DAYS - 1);
        LiveData<List<AppUsageDao.DailyUsageTotal>> rawTrend = dao.observeWeeklyTrend(startDate);

        weeklyTrend.addSource(rawTrend, totals -> {
            Map<String, Long> byDate = new HashMap<>();
            if (totals != null) {
                for (AppUsageDao.DailyUsageTotal t : totals) {
                    byDate.put(t.date, t.totalMillis);
                }
            }
            List<DayUsagePoint> points = new ArrayList<>();
            for (int i = TREND_DAYS - 1; i >= 0; i--) {
                String date = TimeUtils.getDateDaysAgo(i);
                long millis = byDate.containsKey(date) ? byDate.get(date) : 0L;
                points.add(new DayUsagePoint(TimeUtils.getShortDayLabel(date), millis));
            }
            weeklyTrend.setValue(points);
        });
    }

    public void syncUsage() {
        repo.syncTodayUsage();
    }

    public void setLimit(String pkg, long limitMillis) {
        repo.setAppLimit(pkg, limitMillis);
    }
}
