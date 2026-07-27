package com.zenith.app.ui.screen;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenViewModel extends ViewModel {

    /** One day's worth of usage, ready for the weekly bar chart. */
    public static class DayUsagePoint {
        public final String date;    // yyyy-MM-dd — needed to look up that day's detail on tap
        public final String label;   // e.g. "Mon"
        public final long   millis;
        public DayUsagePoint(String date, String label, long millis) {
            this.date = date;
            this.label = label;
            this.millis = millis;
        }
    }

    public final LiveData<List<AppUsageEntity>>     usageList;
    public final LiveData<List<BrowserVisitEntity>> browserList;
    public final MediatorLiveData<List<DayUsagePoint>> weeklyTrend = new MediatorLiveData<>();

    /** Populated when a chart bar is tapped — the selected day's per-app
     *  breakdown, so "tap a past day" actually shows something instead of
     *  doing nothing. */
    public final MutableLiveData<List<AppUsageEntity>> selectedDayDetail = new MutableLiveData<>();
    public final MutableLiveData<String> selectedDayLabel = new MutableLiveData<>();

    private final UsageRepository repo;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int TREND_DAYS = 7;

    public ScreenViewModel(Context context) {
        repo        = new UsageRepository(context);
        db          = AppDatabase.getInstance(context);
        usageList   = db.appUsageDao().getUsageForDate(TimeUtils.getTodayDate());
        browserList = db.browserVisitDao().getVisitsForDate(TimeUtils.getTodayDate());

        AppUsageDao dao = db.appUsageDao();
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
                points.add(new DayUsagePoint(date, TimeUtils.getShortDayLabel(date), millis));
            }
            weeklyTrend.setValue(points);
        });
    }

    /** Loads the per-app breakdown for a specific past day (used when a
     *  chart bar is tapped). Past days are finalized/frozen, so a one-shot
     *  query is fine — no need for a LiveData that stays live forever. */
    public void loadDayDetail(String date, String humanLabel) {
        executor.execute(() -> {
            List<AppUsageEntity> apps = db.appUsageDao().getUsageForDateSync(date);
            selectedDayLabel.postValue(humanLabel);
            selectedDayDetail.postValue(apps);
        });
    }

    public void syncUsage() {
        repo.syncTodayUsage();
    }

    public void setLimit(String pkg, long limitMillis) {
        repo.setAppLimit(pkg, limitMillis);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
