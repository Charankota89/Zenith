package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.zenith.app.db.entity.SkillEntity;
import java.util.List;

@Dao
public interface SkillDao {
    @Insert
    void insert(SkillEntity skill);

    @Update
    void update(SkillEntity skill);

    @Delete
    void delete(SkillEntity skill);

    @Query("SELECT * FROM skills ORDER BY totalMillis DESC")
    LiveData<List<SkillEntity>> getAllSkills();

    @Query("SELECT * FROM skills WHERE id = :id LIMIT 1")
    SkillEntity getSkillById(int id);
}
