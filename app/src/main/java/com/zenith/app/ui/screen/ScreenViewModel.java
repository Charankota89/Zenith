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

    public final MediatorLiveData<List<AppUsageEntity>>     usageList   = new MediatorLiveData<>();
    public final MediatorLiveData<List<BrowserVisitEntity>> browserList = new MediatorLiveData<>();
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

    // The date every LiveData source above is currently pointed at. This
    // ViewModel is Fragment-scoped, and MainActivity keeps fragments alive
    // across tab switches — so it's entirely possible for midnight to pass
    // while this ViewModel is still sitting in memory. Room's LiveData is
    // bound to whatever date string was used when the query was created;
    // it does not somehow know to start looking at a new date on its own.
    // That was the actual cause of "showing previous day timing" — the
    // underlying database was correct, this ViewModel just never asked it
    // about today again.
    private String trackedDate;

    private LiveData<List<AppUsageEntity>>     usageSource;
    private LiveData<List<BrowserVisitEntity>> browserSource;
    private LiveData<List<AppUsageDao.DailyUsageTotal>> trendSource;

    public ScreenViewModel(Context context) {
        repo = new UsageRepository(context);
        db   = AppDatabase.getInstance(context);
        pointAllSourcesAtToday();
    }

    private void pointAllSourcesAtToday() {
        String today = TimeUtils.getTodayDate();
        trackedDate = today;

        if (usageSource != null) usageList.removeSource(usageSource);
        usageSource = db.appUsageDao().getUsageForDate(today);
        usageList.addSource(usageSource, usageList::setValue);

        if (browserSource != null) browserList.removeSource(browserSource);
        browserSource = db.browserVisitDao().getVisitsForDate(today);
        browserList.addSource(browserSource, browserList::setValue);

        if (trendSource != null) weeklyTrend.removeSource(trendSource);
        String startDate = TimeUtils.getDateDaysAgo(TREND_DAYS - 1);
        trendSource = db.appUsageDao().observeWeeklyTrend(startDate);
        weeklyTrend.addSource(trendSource, totals -> {
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

    /** Call from the Fragment's onResume() — cheap no-op most of the time,
     *  but the moment midnight has actually passed since this ViewModel
     *  was created, this re-points every date-dependent query at the new
     *  "today" so the screen shows fresh data instead of yesterday's. */
    public void refreshIfNewDay() {
        String today = TimeUtils.getTodayDate();
        if (!today.equals(trackedDate)) {
            pointAllSourcesAtToday();
        }
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
