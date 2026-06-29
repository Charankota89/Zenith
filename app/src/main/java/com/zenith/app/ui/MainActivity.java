package com.zenith.app.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zenith.app.R;
import com.zenith.app.databinding.ActivityMainBinding;
import com.zenith.app.ui.career.CareerFragment;
import com.zenith.app.ui.focus.FocusFragment;
import com.zenith.app.ui.home.HomeFragment;
import com.zenith.app.ui.screen.ScreenFragment;
import com.zenith.app.ui.wellbeing.WellbeingFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadFragment(new HomeFragment());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if      (id == R.id.nav_home)     f = new HomeFragment();
            else if (id == R.id.nav_screen)   f = new ScreenFragment();
            else if (id == R.id.nav_focus)    f = new FocusFragment();
            else if (id == R.id.nav_wellbeing) f = new WellbeingFragment();
            else if (id == R.id.nav_career)   f = new CareerFragment();
            else                              f = new HomeFragment();
            loadFragment(f);
            return true;
        });
    }

    private void loadFragment(Fragment f) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .commit();
    }
}
