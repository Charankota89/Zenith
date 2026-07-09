package com.zenith.app.ui.wellbeing;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.MoodEntity;
import com.zenith.app.util.TimeUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WellbeingViewModel extends ViewModel {

    private final AppDatabase    db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public final LiveData<List<MoodEntity>> lastSevenMoods;

    public WellbeingViewModel(Context context) {
        db              = AppDatabase.getInstance(context);
        lastSevenMoods  = db.moodDao().getLastSevenDaysMood();
    }

    /** Save today's mood (overwrites if already logged today). */
    public void saveMood(int score, String note) {
        executor.execute(() -> {
            String today = TimeUtils.getTodayDate();
            // Check if already logged today — if yes, skip (one entry per day)
            MoodEntity existing = db.moodDao().getMoodForDate(today);
            if (existing != null) return;

            MoodEntity mood = new MoodEntity();
            mood.moodScore = score;
            mood.date      = today;
            mood.note      = note != null ? note : "";
            db.moodDao().insert(mood);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
