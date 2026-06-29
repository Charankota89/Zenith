package com.zenith.app.repository;

import android.content.Context;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.HabitEntity;
import com.zenith.app.util.TimeUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HabitRepository {

    private final AppDatabase     db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HabitRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    public void addHabit(String name) {
        executor.execute(() -> {
            HabitEntity h    = new HabitEntity();
            h.habitName      = name;
            h.currentStreak  = 0;
            h.longestStreak  = 0;
            h.completedToday = false;
            db.habitDao().insert(h);
        });
    }

    public void completeHabit(int habitId) {
        executor.execute(() -> {
            HabitEntity h = db.habitDao().getHabitById(habitId);
            if (h == null || h.completedToday) return;
            h.completedToday    = true;
            h.currentStreak    += 1;
            h.lastCompletedDate = TimeUtils.getTodayDate();
            if (h.currentStreak > h.longestStreak)
                h.longestStreak = h.currentStreak;
            db.habitDao().update(h);
        });
    }

    public void deleteHabit(HabitEntity habit) {
        executor.execute(() -> db.habitDao().delete(habit));
    }
}
