package com.zenith.app.ui.home;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.util.TimeUtils;

public class HomeViewModel extends ViewModel {

    public final MediatorLiveData<Long>    totalScreenTime = new MediatorLiveData<>();
    public final MediatorLiveData<Integer> habitsDoneToday  = new MediatorLiveData<>();
    public final MediatorLiveData<Long>    studyTimeToday   = new MediatorLiveData<>();

    private final AppDatabase db;
    private String trackedDate;
    private LiveData<Long> usageSource;
    private LiveData<Long> studySource;

    public HomeViewModel(Context context) {
        db = AppDatabase.getInstance(context);

        // habitsDoneToday is driven by a boolean flag the midnight worker
        // resets directly in the database, not a date string, so it isn't
        // affected by the same staleness issue and only needs wiring once.
        habitsDoneToday.setValue(0);
        LiveData<Integer> habitsSource = db.habitDao().observeCompletedCountToday();
        habitsDoneToday.addSource(habitsSource, value ->
            habitsDoneToday.setValue(value != null ? value : 0));

        pointDateSourcesAtToday();
    }

    private void pointDateSourcesAtToday() {
        String today = TimeUtils.getTodayDate();
        trackedDate = today;

        // These are reactive Room LiveData queries: they automatically
        // re-fire whenever the underlying tables change, so the Home
        // dashboard stays live and in sync with what the accessibility
        // service is recording in the background. But the query itself is
        // still bound to whatever date string was passed in when it was
        // created — Room doesn't know to start looking at a new date on
        // its own. Since this ViewModel can stay alive across midnight
        // (MainActivity keeps fragments around between tab switches),
        // that was the actual cause of "showing previous day timing" on
        // Home too: the underlying data was correct, this screen just
        // never asked about today again once it rolled over.
        if (usageSource != null) totalScreenTime.removeSource(usageSource);
        usageSource = db.appUsageDao().observeTotalUsageForDate(today);
        totalScreenTime.addSource(usageSource, value ->
            totalScreenTime.setValue(value != null ? value : 0L));

        if (studySource != null) studyTimeToday.removeSource(studySource);
        studySource = db.studySessionDao().observeTotalStudyTimeForDate(today);
        studyTimeToday.addSource(studySource, value ->
            studyTimeToday.setValue(value != null ? value : 0L));
    }

    /** Call from the Fragment's onResume(). */
    public void refreshIfNewDay() {
        String today = TimeUtils.getTodayDate();
        if (!today.equals(trackedDate)) {
            pointDateSourcesAtToday();
        }
    }
}
