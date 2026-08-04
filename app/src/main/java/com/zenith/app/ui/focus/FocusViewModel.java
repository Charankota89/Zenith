package com.zenith.app.ui.focus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.PomodoroEntity;
import com.zenith.app.db.entity.StudySessionEntity;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.NotificationHelper;
import com.zenith.app.util.TimeUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FocusViewModel extends ViewModel {

    public enum TimerState { IDLE, RUNNING, PAUSED, BREAK }

    private static final long DEFAULT_WORK_MIN = 25;
    private static final long BREAK_MILLIS = 5 * 60 * 1000L;  //  5 min

    /** User's chosen study duration, in minutes. Defaults to 25, but is
     *  now fully adjustable and persisted, instead of being permanently
     *  fixed at 25 like before. */
    public final MutableLiveData<Integer> selectedDurationMin = new MutableLiveData<>((int) DEFAULT_WORK_MIN);

    public final MutableLiveData<Long>        timeLeftMillis    = new MutableLiveData<>(DEFAULT_WORK_MIN * 60000L);
    public final MutableLiveData<Integer>     sessionsCompleted = new MutableLiveData<>(0);
    public final MutableLiveData<TimerState>  timerState        = new MutableLiveData<>(TimerState.IDLE);
    public final MutableLiveData<String>      currentSubject    = new MutableLiveData<>("Study");

    private CountDownTimer     countDownTimer;
    private long               pausedMillisLeft;
    private long               sessionStartTime = 0;
    private final AppDatabase  db;
    private final Context      appContext;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FocusViewModel(Context context) {
        db         = AppDatabase.getInstance(context);
        appContext  = context.getApplicationContext();
        prefs       = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);

        int savedMin = prefs.getInt(AppConstants.PREF_STUDY_DURATION_MIN, (int) DEFAULT_WORK_MIN);
        selectedDurationMin.setValue(savedMin);
        pausedMillisLeft = savedMin * 60000L;
        timeLeftMillis.setValue(pausedMillisLeft);

        // Without this, "X sessions completed today" always showed 0 on a
        // fresh app open/ViewModel recreation, even if you'd already done
        // several Pomodoros earlier today — the DB record was correct, the
        // UI just never read it back.
        executor.execute(() -> {
            PomodoroEntity pomo = db.pomodoroDao().getPomodoroForDate(TimeUtils.getTodayDate());
            if (pomo != null) {
                sessionsCompleted.postValue(pomo.sessionsCompleted);
            }
        });
    }

    /** Change how long a study session should be. Only takes effect on the
     *  next fresh timer start — if a session is already running or paused,
     *  changing this doesn't yank time out from under it mid-session. */
    public void setDurationMinutes(int minutes) {
        if (minutes < 1) minutes = 1;
        if (minutes > 180) minutes = 180; // sane upper bound
        selectedDurationMin.setValue(minutes);
        prefs.edit().putInt(AppConstants.PREF_STUDY_DURATION_MIN, minutes).apply();

        if (timerState.getValue() == TimerState.IDLE) {
            pausedMillisLeft = minutes * 60000L;
            timeLeftMillis.setValue(pausedMillisLeft);
        }
    }

    private long currentWorkMillis() {
        Integer min = selectedDurationMin.getValue();
        return (min != null ? min : DEFAULT_WORK_MIN) * 60000L;
    }

    public void startTimer() {
        TimerState state = timerState.getValue();
        if (state == TimerState.RUNNING) return;

        long duration = (state == TimerState.PAUSED) ? pausedMillisLeft : currentWorkMillis();
        if (state != TimerState.PAUSED) sessionStartTime = System.currentTimeMillis();

        timerState.setValue(TimerState.RUNNING);
        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                pausedMillisLeft = millisUntilFinished;
                timeLeftMillis.setValue(millisUntilFinished);
            }
            @Override public void onFinish() {
                timeLeftMillis.setValue(0L);
                onWorkSessionComplete();
            }
        }.start();
    }

    public void pauseTimer() {
        if (timerState.getValue() != TimerState.RUNNING) return;
        if (countDownTimer != null) countDownTimer.cancel();
        timerState.setValue(TimerState.PAUSED);
    }

    public void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        pausedMillisLeft = currentWorkMillis();
        timeLeftMillis.setValue(pausedMillisLeft);
        timerState.setValue(TimerState.IDLE);
        // Note: sessionsCompleted is intentionally left untouched here — it
        // reflects sessions actually finished today (persisted in the DB),
        // not the in-progress one being abandoned. Resetting it to 0 would
        // have wiped a legitimate earlier session's count off the screen.
    }

    public void setSubject(String subject) {
        currentSubject.setValue(subject);
    }

    private void onWorkSessionComplete() {
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - sessionStartTime;
        String subject = currentSubject.getValue() != null ? currentSubject.getValue() : "Study";
        String today   = TimeUtils.getTodayDate();

        executor.execute(() -> {
            // Save study session
            StudySessionEntity session = new StudySessionEntity();
            session.subject        = subject;
            session.startTime      = sessionStartTime;
            session.endTime        = endTime;
            session.durationMillis = durationMs;
            session.date           = today;
            db.studySessionDao().insert(session);

            // Update or create pomodoro log
            PomodoroEntity pomo = db.pomodoroDao().getPomodoroForDate(today);
            if (pomo == null) {
                pomo = new PomodoroEntity();
                pomo.date              = today;
                pomo.sessionsCompleted = 1;
                pomo.totalFocusMillis  = durationMs;
                db.pomodoroDao().insert(pomo);
            } else {
                pomo.sessionsCompleted++;
                pomo.totalFocusMillis += durationMs;
                db.pomodoroDao().update(pomo);
            }
        });

        int done = sessionsCompleted.getValue() != null ? sessionsCompleted.getValue() : 0;
        done++;
        sessionsCompleted.setValue(done);

        // 🔔 Fire notification → opens Focus tab
        NotificationHelper.notifyPomodoroSessionDone(appContext, done);

        // Start break countdown
        startBreak();
    }

    private void startBreak() {
        timerState.setValue(TimerState.BREAK);
        timeLeftMillis.setValue(BREAK_MILLIS);
        countDownTimer = new CountDownTimer(BREAK_MILLIS, 1000) {
            @Override public void onTick(long ms) { timeLeftMillis.setValue(ms); }
            @Override public void onFinish() {
                // Break done — 🔔 notify and reset to ready state
                NotificationHelper.notifyPomodoroBreakDone(appContext);
                pausedMillisLeft = currentWorkMillis();
                timeLeftMillis.setValue(pausedMillisLeft);
                timerState.setValue(TimerState.IDLE);
            }
        }.start();
    }

    public LiveData<List<StudySessionEntity>> getRecentSessions() {
        return db.studySessionDao().getRecentSessions();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) countDownTimer.cancel();
        executor.shutdown();
    }
}
