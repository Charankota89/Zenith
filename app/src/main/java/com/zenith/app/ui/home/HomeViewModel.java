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

    public HomeViewModel(Context context) {
        AppDatabase db    = AppDatabase.getInstance(context);
        String      today = TimeUtils.getTodayDate();

        // These are reactive Room LiveData queries: they automatically
        // re-fire whenever the underlying tables change, so the Home
        // dashboard stays live and in sync with what the accessibility
        // service is recording in the background — no more stale numbers
        // that only update if you leave and re-open the Home tab.
        totalScreenTime.setValue(0L);
        LiveData<Long> usageSource = db.appUsageDao().observeTotalUsageForDate(today);
        totalScreenTime.addSource(usageSource, value ->
            totalScreenTime.setValue(value != null ? value : 0L));

        habitsDoneToday.setValue(0);
        LiveData<Integer> habitsSource = db.habitDao().observeCompletedCountToday();
        habitsDoneToday.addSource(habitsSource, value ->
            habitsDoneToday.setValue(value != null ? value : 0));

        studyTimeToday.setValue(0L);
        LiveData<Long> studySource = db.studySessionDao().observeTotalStudyTimeForDate(today);
        studyTimeToday.addSource(studySource, value ->
            studyTimeToday.setValue(value != null ? value : 0L));
    }
}
