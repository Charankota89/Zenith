package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.zenith.app.db.entity.MoodEntity;
import java.util.List;

@Dao
public interface MoodDao {
    @Insert
    void insert(MoodEntity mood);

    @Query("SELECT * FROM mood_log WHERE date = :date LIMIT 1")
    MoodEntity getMoodForDate(String date);

    @Query("SELECT * FROM mood_log ORDER BY date DESC LIMIT 7")
    LiveData<List<MoodEntity>> getLastSevenDaysMood();
}
