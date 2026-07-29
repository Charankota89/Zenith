package com.zenith.app.ui.focus;

import android.content.Context;
import android.os.CountDownTimer;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.PomodoroEntity;
import com.zenith.app.db.entity.StudySessionEntity;
import com.zenith.app.util.NotificationHelper;
import com.zenith.app.util.TimeUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FocusViewModel extends ViewModel {

    public enum TimerState { IDLE, RUNNING, PAUSED, BREAK }

    private static final long WORK_MILLIS  = 25 * 60 * 1000L;  // 25 min
    private static final long BREAK_MILLIS =  5 * 60 * 1000L;  //  5 min

    public final MutableLiveData<Long>        timeLeftMillis    = new MutableLiveData<>(WORK_MILLIS);
    public final MutableLiveData<Integer>     sessionsCompleted = new MutableLiveData<>(0);
    public final MutableLiveData<TimerState>  timerState        = new MutableLiveData<>(TimerState.IDLE);
    public final MutableLiveData<String>      currentSubject    = new MutableLiveData<>("Study");

    private CountDownTimer     countDownTimer;
    private long               pausedMillisLeft  = WORK_MILLIS;
    private long               sessionStartTime  = 0;
    // Track total time spent paused during a session so it can be
    // subtracted from the final durationMs. Without this, a 25-minute
    // work session where you pause for 5 minutes gets logged as 25 minutes
    // of focus — inflating real study time.
    private long               pauseStartTime    = 0;
    private long               totalPausedMillis = 0;

    private final AppDatabase  db;
    private final Context      appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FocusViewModel(Context context) {
        db         = AppDatabase.getInstance(context);
        appContext  = context.getApplicationContext();

        // Without this, "X sessions completed today" always showed 0 on a
        // fresh app open/ViewModel recreation, even if you'd already done
        // several Pomodoros earlier today — the DB record was correct, the
        // UI just never read it back.
        executor.execute(() -> {
            PomodoroEntity pomo =
                db.pomodoroDao().getPomodoroForDate(TimeUtils.getTodayDate());
            if (pomo != null) {
                sessionsCompleted.postValue(pomo.sessionsCompleted);
            }
        });
    }

    public void startTimer() {
        TimerState state = timerState.getValue();
        if (state == TimerState.RUNNING) return;

        if (state == TimerState.PAUSED) {
            // Resuming from pause — accumulate the paused duration so it can
            // be subtracted from the total when the session completes.
            if (pauseStartTime > 0) {
                totalPausedMillis += System.currentTimeMillis() - pauseStartTime;
                pauseStartTime = 0;
            }
        } else {
            // Fresh start — reset all tracking state.
            sessionStartTime  = System.currentTimeMillis();
            totalPausedMillis = 0;
            pauseStartTime    = 0;
        }

        long duration = (state == TimerState.PAUSED) ? pausedMillisLeft : WORK_MILLIS;
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
        pauseStartTime = System.currentTimeMillis(); // record when pause began
        timerState.setValue(TimerState.PAUSED);
    }

    public void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        pausedMillisLeft  = WORK_MILLIS;
        totalPausedMillis = 0;
        pauseStartTime    = 0;
        timeLeftMillis.setValue(WORK_MILLIS);
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
        // Subtract any paused time so the logged duration reflects actual
        // focused work, not wall-clock time including pauses.
        long durationMs = (endTime - sessionStartTime) - totalPausedMillis;
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
        totalPausedMillis = 0; // reset for next session

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
                pausedMillisLeft = WORK_MILLIS;
                timeLeftMillis.setValue(WORK_MILLIS);
                timerState.setValue(TimerState.IDLE);
            }
        }.start();
    }

    public androidx.lifecycle.LiveData<java.util.List<StudySessionEntity>> getRecentSessions() {
        return db.studySessionDao().getRecentSessions();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) countDownTimer.cancel();
        executor.shutdown();
    }
}
