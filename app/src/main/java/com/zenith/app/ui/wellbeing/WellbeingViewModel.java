package com.zenith.app.ui.wellbeing;

import android.content.Context;
import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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

    private static final long EYE_BREAK_INTERVAL_MS = 1200000L; // 20 minutes
    public final MutableLiveData<Long> eyeBreakMillisRemaining = new MutableLiveData<>(EYE_BREAK_INTERVAL_MS);
    private CountDownTimer eyeBreakTimer;

    public WellbeingViewModel(Context context) {
        db              = AppDatabase.getInstance(context);
        lastSevenMoods  = db.moodDao().getLastSevenDaysMood();
        startEyeBreakTimer();
    }

    // Scoped to the Activity (not the Fragment) so this keeps counting down
    // in the background even while you're on a different tab — previously
    // this lived directly in WellbeingFragment and reset to a full 20:00
    // every single time you left and came back to the tab, since Fragment
    // recreation destroyed and recreated the CountDownTimer from scratch.
    private void startEyeBreakTimer() {
        if (eyeBreakTimer != null) eyeBreakTimer.cancel();
        eyeBreakTimer = new CountDownTimer(EYE_BREAK_INTERVAL_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                eyeBreakMillisRemaining.postValue(millisUntilFinished);
            }
            @Override
            public void onFinish() {
                startEyeBreakTimer(); // loop
            }
        }.start();
    }

    /** Save today's mood — updates the existing entry if you've already
     *  logged one today, so tapping a different mood later in the day
     *  actually changes what's recorded (previously it silently did
     *  nothing on a second tap, while the screen still showed new
     *  feedback text as if it had worked). */
    public void saveMood(int score, String note) {
        executor.execute(() -> {
            String today = TimeUtils.getTodayDate();
            MoodEntity existing = db.moodDao().getMoodForDate(today);

            if (existing != null) {
                existing.moodScore = score;
                existing.note      = note != null ? note : existing.note;
                db.moodDao().update(existing);
                return;
            }

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
        if (eyeBreakTimer != null) eyeBreakTimer.cancel();
    }
}
