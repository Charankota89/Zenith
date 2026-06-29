package com.zenith.app.ui.career;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.HabitEntity;
import com.zenith.app.repository.HabitRepository;
import java.util.List;

public class CareerViewModel extends ViewModel {

    public final LiveData<List<HabitEntity>> habits;
    private final HabitRepository repo;

    public CareerViewModel(Context context) {
        repo   = new HabitRepository(context);
        habits = AppDatabase.getInstance(context).habitDao().getAllHabits();
    }

    public void addHabit(String name) { repo.addHabit(name); }
    public void completeHabit(int id) { repo.completeHabit(id); }
    public void deleteHabit(HabitEntity h) { repo.deleteHabit(h); }
}
