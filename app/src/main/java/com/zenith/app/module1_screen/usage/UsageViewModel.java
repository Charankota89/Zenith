package com.zenith.app.module1_screen.usage;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zenith.app.data.db.dao.AppUsageDao;
import com.zenith.app.data.db.entity.AppUsageEntity;
import com.zenith.app.data.repository.UsageRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for Usage Tracker screen.
 * Exposes LiveData observed by UsageFragment.
 */
public class UsageViewModel extends AndroidViewModel {

    private final UsageRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Observed by the fragment
    public final LiveData<List<AppUsageEntity>> todayUsage;
    public final LiveData<List<AppUsageEntity>> weeklyTopApps;
    public final LiveData<List<AppUsageDao.DailyTotalUsage>> weeklyDailyTotals;
    public final LiveData<List<AppUsageEntity>> topAppsToday;

    private final MutableLiveData<Boolean> hasPermission = new MutableLiveData<>();
    private final MutableLiveData<Boolean> syncing       = new MutableLiveData<>(false);

    public UsageViewModel(Application application) {
        super(application);
        repository = new UsageRepository(application);

        // Wire up LiveData straight from repo
        todayUsage       = repository.getTodayUsageLive();
        weeklyTopApps    = repository.getWeeklyTopApps();
        weeklyDailyTotals = repository.getWeeklyDailyTotals();
        topAppsToday     = repository.getTopAppsToday(5);

        // Check permission immediately
        checkPermission();
    }

    public LiveData<Boolean> getHasPermission() { return hasPermission; }
    public LiveData<Boolean> isSyncing()          { return syncing; }

    public void checkPermission() {
        hasPermission.setValue(repository.hasUsagePermission());
    }

    /**
     * Trigger a fresh sync from UsageStatsManager.
     * Called on pull-to-refresh or when screen is foregrounded.
     */
    public void syncUsage() {
        if (Boolean.TRUE.equals(syncing.getValue())) return;
        syncing.setValue(true);
        executor.execute(() -> {
            repository.syncTodayUsage();
            syncing.postValue(false);
        });
    }

    /**
     * Returns usage in minutes for a specific package today.
     * Used by AppTimerViewModel to check if limit is hit.
     */
    public long getUsageMinutesForPackage(String pkg) {
        return repository.getUsageMinutesForPackageToday(pkg);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
