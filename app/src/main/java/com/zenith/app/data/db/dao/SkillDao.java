package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.zenith.app.data.db.entity.SkillEntryEntity;

import java.util.List;

@Dao
public interface SkillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SkillEntryEntity entry);

    /** All distinct skill names the user has ever used. */
    @Query("SELECT DISTINCT skillName FROM skill_entry ORDER BY skillName ASC")
    LiveData<List<String>> getAllSkillNames();

    /** Total minutes per skill (lifetime). */
    @Query("SELECT skillName, SUM(minutesLogged) as minutesLogged, " +
           "MIN(sessionStart) as sessionStart, MAX(date) as date, 0 as id " +
           "FROM skill_entry GROUP BY skillName ORDER BY minutesLogged DESC")
    LiveData<List<SkillEntryEntity>> getLifetimeTotalPerSkill();

    /** Sessions for a skill in the last 7 days. */
    @Query("SELECT * FROM skill_entry WHERE skillName = :skill AND date >= :fromDate ORDER BY date ASC")
    LiveData<List<SkillEntryEntity>> getSkillSessionsSince(String skill, String fromDate);

    /** Total minutes for a skill on a specific date. */
    @Query("SELECT COALESCE(SUM(minutesLogged), 0) FROM skill_entry WHERE skillName = :skill AND date = :date")
    long getTodayMinutesForSkill(String skill, String date);

    /** Weekly summary: total minutes per skill since a date. */
    @Query("SELECT skillName, SUM(minutesLogged) as minutesLogged, 0 as sessionStart, :week as date, 0 as id " +
           "FROM skill_entry WHERE date >= :fromDate GROUP BY skillName ORDER BY minutesLogged DESC")
    List<SkillEntryEntity> getWeeklySkillSummarySync(String fromDate, String week);

    @Query("DELETE FROM skill_entry WHERE date < :cutoff")
    void deleteOlderThan(String cutoff);
}
