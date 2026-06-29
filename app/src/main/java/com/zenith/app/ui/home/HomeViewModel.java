package com.zenith.app.ui.home;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.util.TimeUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends ViewModel {

    public final MutableLiveData<Long>    totalScreenTime = new MutableLiveData<>(0L);
    public final MutableLiveData<Integer> habitsDoneToday  = new MutableLiveData<>(0);
    public final MutableLiveData<Long>    studyTimeToday   = new MutableLiveData<>(0L);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HomeViewModel(Context context) {
        AppDatabase db    = AppDatabase.getInstance(context);
        String      today = TimeUtils.getTodayDate();

        executor.execute(() -> {
            long usage = db.appUsageDao().getTotalUsageForDate(today);
            totalScreenTime.postValue(usage);

            int habits = db.habitDao().countCompletedToday();
            habitsDoneToday.postValue(habits);

            long study = db.studySessionDao().getTotalStudyTimeForDate(today);
            studyTimeToday.postValue(study);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
