package com.zenith.app.repository;

import android.content.Context;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.StudySessionEntity;
import com.zenith.app.util.TimeUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudyRepository {

    private final AppDatabase     db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public StudyRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    public void saveSession(String subject, long startTime, long endTime) {
        executor.execute(() -> {
            StudySessionEntity session = new StudySessionEntity();
            session.subject        = subject;
            session.startTime      = startTime;
            session.endTime        = endTime;
            session.durationMillis = endTime - startTime;
            session.date           = TimeUtils.getTodayDate();
            db.studySessionDao().insert(session);
        });
    }
}
