package com.zenith.app.ui.home;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.util.TimeUtils;

public class HomeViewModel extends ViewModel {

    public final MutableLiveData<String> currentDate = new MutableLiveData<>(TimeUtils.getTodayDate());
    public final LiveData<Long>    totalScreenTime;
    public final LiveData<Integer> habitsDoneToday;
    public final LiveData<Long>    studyTimeToday;

    public HomeViewModel(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        totalScreenTime = androidx.lifecycle.Transformations.switchMap(currentDate, date ->
            db.appUsageDao().observeTotalUsageForDate(date));

        habitsDoneToday = androidx.lifecycle.Transformations.switchMap(currentDate, date ->
            db.habitDao().observeCompletedCountToday());

        studyTimeToday = androidx.lifecycle.Transformations.switchMap(currentDate, date ->
            db.studySessionDao().observeTotalStudyTimeForDate(date));
    }

    public void refreshTodayDate() {
        String today = TimeUtils.getTodayDate();
        if (!today.equals(currentDate.getValue())) {
            currentDate.setValue(today);
        }
    }
}
