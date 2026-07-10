package com.zenith.app.ui.settings;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.zenith.app.databinding.FragmentSettingsBinding;
import com.zenith.app.service.UsageMonitorService;
import com.zenith.app.util.AppConstants;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private final SimpleDateFormat fileDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final ActivityResultLauncher<String> createFileLauncher = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("text/plain"),
        uri -> {
            if (uri != null) {
                writeReportToUri(uri);
            }
        }
    );

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Staggered entering animations for settings cards
        android.view.animation.Animation a1 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), com.zenith.app.R.anim.slide_up_fade_in);
        android.view.animation.Animation a2 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), com.zenith.app.R.anim.slide_up_fade_in);
        android.view.animation.Animation a3 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), com.zenith.app.R.anim.slide_up_fade_in);
        android.view.animation.Animation a4 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), com.zenith.app.R.anim.slide_up_fade_in);
        android.view.animation.Animation a5 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), com.zenith.app.R.anim.slide_up_fade_in);

        a1.setStartOffset(50);
        a2.setStartOffset(100);
        a3.setStartOffset(150);
        a4.setStartOffset(200);
        a5.setStartOffset(250);

        binding.cardGrowthMode.startAnimation(a1);
        binding.cardDailyGoal.startAnimation(a2);
        binding.cardSecurity.startAnimation(a3);
        binding.cardExport.startAnimation(a4);
        binding.cardService.startAnimation(a5);

        SharedPreferences prefs = requireContext()
            .getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);

        // Career mode toggle
        boolean careerActive = prefs.getBoolean(AppConstants.PREF_CAREER_ACTIVE, false);
        binding.switchCareerMode.setChecked(careerActive);
        binding.switchCareerMode.setOnCheckedChangeListener((btn, isChecked) ->
            prefs.edit().putBoolean(AppConstants.PREF_CAREER_ACTIVE, isChecked).apply());

        // Daily goal
        int goalMins = prefs.getInt(AppConstants.PREF_DAILY_GOAL_MIN, 120);
        binding.tvDailyGoalValue.setText(goalMins + " min");

        binding.btnDecreaseGoal.setOnClickListener(v -> {
            int cur = prefs.getInt(AppConstants.PREF_DAILY_GOAL_MIN, 120);
            if (cur > 15) {
                cur -= 15;
                prefs.edit().putInt(AppConstants.PREF_DAILY_GOAL_MIN, cur).apply();
                binding.tvDailyGoalValue.setText(cur + " min");
            }
        });

        binding.btnIncreaseGoal.setOnClickListener(v -> {
            int cur = prefs.getInt(AppConstants.PREF_DAILY_GOAL_MIN, 120);
            cur += 15;
            prefs.edit().putInt(AppConstants.PREF_DAILY_GOAL_MIN, cur).apply();
            binding.tvDailyGoalValue.setText(cur + " min");
        });

        // PIN setup
        binding.btnSetPin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PinSetupActivity.class);
            startActivity(intent);
        });

        // Set default dates for report
        binding.tvStartDateValue.setText(fileDateFormatter.format(startCal.getTime()));
        binding.tvEndDateValue.setText(fileDateFormatter.format(endCal.getTime()));

        binding.btnSelectStartDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (dp, year, month, dayOfMonth) -> {
                startCal.set(Calendar.YEAR, year);
                startCal.set(Calendar.MONTH, month);
                startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                binding.tvStartDateValue.setText(fileDateFormatter.format(startCal.getTime()));
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        binding.btnSelectEndDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (dp, year, month, dayOfMonth) -> {
                endCal.set(Calendar.YEAR, year);
                endCal.set(Calendar.MONTH, month);
                endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                binding.tvEndDateValue.setText(fileDateFormatter.format(endCal.getTime()));
            }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        binding.btnExportReport.setOnClickListener(v -> {
            if (startCal.after(endCal)) {
                Toast.makeText(requireContext(), "Start date cannot be after End date", Toast.LENGTH_SHORT).show();
                return;
            }
            String startStr = fileDateFormatter.format(startCal.getTime());
            String endStr = fileDateFormatter.format(endCal.getTime());
            createFileLauncher.launch("zenith_activity_report_" + startStr + "_to_" + endStr + ".txt");
        });

        // Restart service
        binding.btnRestartService.setOnClickListener(v -> {
            requireContext().startForegroundService(
                new Intent(requireContext(), UsageMonitorService.class));
            Toast.makeText(requireContext(),
                "Usage Monitor restarted", Toast.LENGTH_SHORT).show();
        });

        // App version
        binding.tvAppVersion.setText("Zenith v1.0  •  Focus. Control. Your Peak.");
    }

    private void writeReportToUri(Uri uri) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String report = generateReportContent(startCal.getTime(), endCal.getTime());
                try (android.os.ParcelFileDescriptor pfd = requireContext().getContentResolver().openFileDescriptor(uri, "w");
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(pfd.getFileDescriptor())) {
                    fos.write(report.getBytes());
                }
                
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Report exported successfully!", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to export report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private String generateReportContent(Date start, Date end) {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("          ZENITH DAY-TO-DAY ACTIVITY REPORT       \n");
        sb.append("==================================================\n");
        sb.append("Period: ").append(fileDateFormatter.format(start)).append(" to ").append(fileDateFormatter.format(end)).append("\n");
        sb.append("Generated on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("==================================================\n\n");

        com.zenith.app.db.AppDatabase db = com.zenith.app.db.AppDatabase.getInstance(requireContext().getApplicationContext());
        Calendar current = Calendar.getInstance();
        current.setTime(start);
        current.set(Calendar.HOUR_OF_DAY, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);

        Calendar endLimit = Calendar.getInstance();
        endLimit.setTime(end);
        endLimit.set(Calendar.HOUR_OF_DAY, 0);
        endLimit.set(Calendar.MINUTE, 0);
        endLimit.set(Calendar.SECOND, 0);
        endLimit.set(Calendar.MILLISECOND, 0);

        while (!current.after(endLimit)) {
            String dateStr = fileDateFormatter.format(current.getTime());
            sb.append("Date: ").append(dateStr).append(" (").append(new SimpleDateFormat("EEEE", Locale.getDefault()).format(current.getTime())).append(")\n");
            sb.append("--------------------------------------------------\n");

            // 1. App Usage & Screen Time
            long totalUsage = db.appUsageDao().getTotalUsageForDate(dateStr);
            sb.append("📱 Total Screen Time: ").append(com.zenith.app.util.TimeUtils.formatDuration(totalUsage)).append("\n");
            
            java.util.List<com.zenith.app.db.entity.AppUsageEntity> appList = db.appUsageDao().getUsageForDateSync(dateStr);
            if (appList != null && !appList.isEmpty()) {
                sb.append("   App Details:\n");
                for (com.zenith.app.db.entity.AppUsageEntity app : appList) {
                    if (app.usageTimeMillis > 0) {
                        sb.append("   - ").append(app.appName)
                          .append(": ").append(com.zenith.app.util.TimeUtils.formatDuration(app.usageTimeMillis));
                        if (app.isLocked) {
                            sb.append(" (LOCKED)");
                        }
                        sb.append("\n");
                    }
                }
            } else {
                sb.append("   No app usage tracked.\n");
            }
            sb.append("\n");

            // 2. Study / Focus sessions
            java.util.List<com.zenith.app.db.entity.StudySessionEntity> studyList = db.studySessionDao().getSessionsForDateSync(dateStr);
            long totalStudy = db.studySessionDao().getTotalStudyTimeForDate(dateStr);
            sb.append("📚 Total Study/Focus Time: ").append(com.zenith.app.util.TimeUtils.formatDuration(totalStudy)).append("\n");
            if (studyList != null && !studyList.isEmpty()) {
                sb.append("   Study Sessions:\n");
                for (com.zenith.app.db.entity.StudySessionEntity ses : studyList) {
                    sb.append("   - ").append(ses.subject).append(": ")
                      .append(com.zenith.app.util.TimeUtils.formatDuration(ses.durationMillis))
                      .append(" (")
                      .append(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(ses.startTime)))
                      .append(")\n");
                }
            } else {
                sb.append("   No study sessions recorded.\n");
            }
            sb.append("\n");

            // 3. Pomodoro completed
            com.zenith.app.db.entity.PomodoroEntity pomo = db.pomodoroDao().getPomodoroForDate(dateStr);
            if (pomo != null && (pomo.sessionsCompleted > 0 || pomo.totalFocusMillis > 0)) {
                sb.append("🎯 Pomodoro Sessions: ")
                  .append(pomo.sessionsCompleted).append(" sessions completed (")
                  .append(com.zenith.app.util.TimeUtils.formatDuration(pomo.totalFocusMillis)).append(" focus)\n\n");
            }

            // 4. Habits completed
            java.util.List<com.zenith.app.db.entity.HabitEntity> habitList = db.habitDao().getHabitsCompletedOnDateSync(dateStr);
            if (habitList != null && !habitList.isEmpty()) {
                sb.append("✅ Habits Completed:\n");
                for (com.zenith.app.db.entity.HabitEntity hab : habitList) {
                    sb.append("   - ").append(hab.habitName).append(" (Streak: ").append(hab.currentStreak).append(" days)\n");
                }
            } else {
                sb.append("✅ Habits Completed: None\n");
            }
            sb.append("\n");

            // 5. Skills practiced
            java.util.List<com.zenith.app.db.entity.SkillEntity> skillList = db.skillDao().getSkillsPracticedOnDateSync(dateStr);
            if (skillList != null && !skillList.isEmpty()) {
                sb.append("🎸 Skills Practiced:\n");
                for (com.zenith.app.db.entity.SkillEntity sk : skillList) {
                    sb.append("   - ").append(sk.skillName).append(" (Total practice: ")
                      .append(com.zenith.app.util.TimeUtils.formatDuration(sk.totalMillis)).append(")\n");
                }
                sb.append("\n");
            }

            // 6. Browser Visits
            java.util.List<com.zenith.app.db.entity.BrowserVisitEntity> visits = db.browserVisitDao().getVisitsForDateSync(dateStr);
            if (visits != null && !visits.isEmpty()) {
                sb.append("🌐 Web Activity (").append(visits.size()).append(" visits):\n");
                for (com.zenith.app.db.entity.BrowserVisitEntity visit : visits) {
                    sb.append("   - [")
                      .append(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(visit.visitedAt)))
                      .append("] ").append(visit.url).append("\n");
                }
            } else {
                sb.append("🌐 Web Activity: No websites visited.\n");
            }
            sb.append("\n");

            // 7. Mood Logs
            com.zenith.app.db.entity.MoodEntity mood = db.moodDao().getMoodForDate(dateStr);
            if (mood != null) {
                String moodEmoji = "😐";
                if (mood.moodScore == 1) moodEmoji = "😞";
                else if (mood.moodScore == 2) moodEmoji = "😕";
                else if (mood.moodScore == 3) moodEmoji = "😐";
                else if (mood.moodScore == 4) moodEmoji = "🙂";
                else if (mood.moodScore == 5) moodEmoji = "😄";
                sb.append("🎭 Mood Check-in: ").append(moodEmoji);
                if (mood.note != null && !mood.note.isEmpty()) {
                    sb.append(" - \"").append(mood.note).append("\"");
                }
                sb.append("\n");
            } else {
                sb.append("🎭 Mood Check-in: No check-in\n");
            }
            sb.append("\n==================================================\n\n");

            current.add(Calendar.DATE, 1);
        }
        return sb.toString();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
