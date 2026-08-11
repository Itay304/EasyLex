package com.example.easylex.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.easylex.R;
import com.example.easylex.data.SchoolClass;
import com.example.easylex.data.UserRoleManager;
import com.example.easylex.data.Word;
import com.example.easylex.data.WordRepository;
import com.example.easylex.ui.assignments.StudentAssignmentsViewModel;
import com.example.easylex.ui.auth.SplashActivity;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.example.easylex.ui.mywords.MyWordsViewModel;
import com.example.easylex.ui.teacher.TeacherDashboardViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * =====================================================================
 * ProfileFragment — מסך הפרופיל האישי
 * =====================================================================
 *
 * מה מוצג במסך?
 * --------------
 * ● שם המשתמש ותמונת פרופיל (נטענת מ-Firebase Auth בעזרת Glide)
 * ● כתובת אימייל
 * ● כפתור הגדרות → מנווט ל-SettingsFragment
 * ● כפתור התנתקות → signOut() + חזרה ל-SplashActivity
 * ● כפתור מנהל (גלוי רק למנהלים) → AdminEditFragment
 *
 * כיצד מזוהה מנהל?
 * ------------------
 * הפונקציה checkIfAdmin() משתמשת ב-UserRoleManager (רזולוור תפקיד מרכזי,
 * ר' data/UserRoleManager.java) במקום לקרוא מ-Firestore ישירות.
 * אם התפקיד הוא "superadmin" (או "admin" הישן — תאימות זמנית עד שסקריפט
 * המיגרציה admin-tool/migrate-admin-role.js ירוץ בכל הסביבות) — כפתור
 * המנהל הופך גלוי.
 *
 * תוכן תלוי-role (שלב 2, חלק 4):
 * -------------------------------
 * מעבר לבסיס הקבוע (תמונה/שם/אימייל/הגדרות/התנתקות), applyRoleUi() מוסיף:
 *   • תלמיד עצמאי       — streak/רמה/XP/תג הישג (llStudentStats)
 *   • תלמיד מוסדי        — כל מה שלמעלה + כיתה/דירוג/משימות שהושלמו (llInstitutionalStudentExtra)
 *   • מורה/מנהל          — מוסד/כיתות/תלמידים/משימות פעילות (llTeacherStats), בלי XP/רמה/streak
 *   • superadmin         — בסיס בלבד, ללא תוספות
 * ProfileViewModel (חדש) אחראי על שליפות Firestore הנוספות (דירוג/משימות
 * מורה); הגמיפיקציה המקומית (XP/רמה/streak) נשלפת ישירות מ-GamificationEngine
 * כמו ב-StatisticsFragment, בלי ViewModel נוסף.
 *
 * ארכיטקטורה:
 * -----------
 * אין ViewModel לפרופיל הבסיסי עצמו (Auth read חד-פעמי) — ProfileViewModel
 * משמש רק לתוספות תלויות-role שמעל הבסיס.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * nav_graph.xml / institutional_nav_graph.xml / teacher_nav_graph.xml — navigation_profile
 */
public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView tvName, tvEmail;
    private MaterialButton btnLogout, btnAdmin, btnSettings;

    // ── תלמיד (עצמאי + מוסדי) ────────────────────────────────────────────────
    private LinearLayout llStudentStats;
    private TextView tvStreakLarge, tvBadgeTitle, tvLevelXp;
    private LinearProgressIndicator progressLevelProfile;

    // ── תלמיד מוסדי בלבד ─────────────────────────────────────────────────────
    private LinearLayout llInstitutionalStudentExtra;
    private TextView tvClassName, tvLeaderboardRank, tvAssignmentsCompleted;

    // ── מורה/מנהל בלבד ───────────────────────────────────────────────────────
    private LinearLayout llTeacherStats;
    private TextView tvInstitutionName, tvTeacherCount, tvClassCount, tvActiveStudentCount, tvActiveAssignmentCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        // אתחול Views
        profileImage = root.findViewById(R.id.profileImage);
        tvName = root.findViewById(R.id.tvUserName);
        tvEmail = root.findViewById(R.id.tvUserEmail);
        btnLogout = root.findViewById(R.id.btnLogout);
        btnAdmin = root.findViewById(R.id.btnAdminPanel);
        btnSettings = root.findViewById(R.id.btnSettings);

        llStudentStats = root.findViewById(R.id.llStudentStats);
        tvStreakLarge = root.findViewById(R.id.tvStreakLarge);
        tvBadgeTitle = root.findViewById(R.id.tvBadgeTitle);
        tvLevelXp = root.findViewById(R.id.tvLevelXp);
        progressLevelProfile = root.findViewById(R.id.progressLevelProfile);

        llInstitutionalStudentExtra = root.findViewById(R.id.llInstitutionalStudentExtra);
        tvClassName = root.findViewById(R.id.tvClassName);
        tvLeaderboardRank = root.findViewById(R.id.tvLeaderboardRank);
        tvAssignmentsCompleted = root.findViewById(R.id.tvAssignmentsCompleted);

        llTeacherStats = root.findViewById(R.id.llTeacherStats);
        tvInstitutionName = root.findViewById(R.id.tvInstitutionName);
        tvTeacherCount = root.findViewById(R.id.tvTeacherCount);
        tvClassCount = root.findViewById(R.id.tvClassCount);
        tvActiveStudentCount = root.findViewById(R.id.tvActiveStudentCount);
        tvActiveAssignmentCount = root.findViewById(R.id.tvActiveAssignmentCount);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // תצוגת פרטים בסיסיים
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "משתמש EasyLex");
            tvEmail.setText(user.getEmail());

            // טעינת תמונה מגוגל/פייסבוק אם קיימת
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(profileImage);
            }

            // בדיקה האם המשתמש הוא מנהל
            checkIfAdmin(user.getUid());
        }

        // כפתור הגדרות
        btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_profile_to_navigation_settings));

        // כפתור התנתקות
        btnLogout.setOnClickListener(v -> {
            // תיקון באג: ניקוי Room מלא (מילים + סטטיסטיקה) וגמיפיקציה מקומית
            // (XP/רמה/רצף/lastActiveDate) לפני signOut — אחרת משתמש הבא
            // שיתחבר על אותו מכשיר רואה את נתוני המשתמש הקודם.
            new WordRepository(requireActivity().getApplication()).deleteAll();
            MyWordsViewModel.resetSyncThrottle(requireContext());
            GamificationEngine.getInstance(requireContext()).clearLocalData();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // כפתור מעבר לניהול
        btnAdmin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_admin);
        });

        return root;
    }

    private void checkIfAdmin(String uid) {
        UserRoleManager.getInstance().refresh(() -> {
            // בדיקה שהפרגמנט עדיין מחובר לפני עדכון ה-UI — מונע zombie callback
            if (!isAdded()) return;
            String role = UserRoleManager.getInstance().getRole();
            // "admin" נתמך כאן זמנית לתאימות לאחור, עד שסקריפט המיגרציה
            // (admin-tool/migrate-admin-role.js) ירוץ בכל סביבה. להסיר בהמשך.
            if (UserRoleManager.ROLE_SUPERADMIN.equals(role) || "admin".equals(role)) {
                btnAdmin.setVisibility(View.VISIBLE);
            }
            applyRoleUi(role);
        });
    }

    // ── תוכן תלוי-role (שלב 2, חלק 4) ────────────────────────────────────────

    /**
     * applyRoleUi — קובע אילו קטעים נוספים מוצגים לפי role, בלי לגעת בבסיס
     * הקבוע (תמונה/שם/אימייל/הגדרות/התנתקות/admin) שכבר קיים למעלה.
     */
    private void applyRoleUi(String role) {
        boolean isTeacherOrPrincipal = UserRoleManager.ROLE_TEACHER.equals(role)
                || UserRoleManager.ROLE_PRINCIPAL.equals(role);
        boolean isSuperadmin = UserRoleManager.ROLE_SUPERADMIN.equals(role) || "admin".equals(role);
        boolean isStudent = !isTeacherOrPrincipal && !isSuperadmin;
        boolean isInstitutionalStudent = isStudent && UserRoleManager.getInstance().getInstitutionId() != null;

        if (isStudent) {
            llStudentStats.setVisibility(View.VISIBLE);
            setupStudentGamification();
            if (isInstitutionalStudent) {
                llInstitutionalStudentExtra.setVisibility(View.VISIBLE);
                setupInstitutionalStudentExtras();
            }
        } else if (isTeacherOrPrincipal) {
            llTeacherStats.setVisibility(View.VISIBLE);
            if (UserRoleManager.ROLE_PRINCIPAL.equals(role)) {
                tvTeacherCount.setVisibility(View.VISIBLE);
                setupPrincipalStats();
            } else {
                setupTeacherStats();
            }
        }
        // superadmin — בסיס בלבד, שני הקטעים נשארים gone (ברירת המחדל ב-XML)
    }

    /** streak/רמה/XP + תג הישג — לכל תלמיד (עצמאי או מוסדי). */
    private void setupStudentGamification() {
        GamificationEngine gamification = GamificationEngine.getInstance(requireContext());
        gamification.syncFromFirestore(() -> {
            if (!isAdded()) return;
            int xp = gamification.getTotalXp();
            int level = gamification.getCurrentLevel();
            int streak = gamification.getStreak();
            tvStreakLarge.setText("🔥 " + streak + " ימים ברצף");
            tvLevelXp.setText("רמה " + level + " · " + xp + " XP");
            progressLevelProfile.setMax(Math.max(gamification.getXpRangeOfLevel(), 1));
            progressLevelProfile.setProgress(gamification.getXpWithinLevel());
        });

        // תג הישג — לפי אחוז המילים שנכבשו (Word.isMastered, חלק 2), אותו סולם תוארים כמו StatisticsFragment.
        new ViewModelProvider(this).get(MyWordsViewModel.class).getAllWords()
                .observe(getViewLifecycleOwner(), words -> {
                    if (words == null || words.isEmpty() || !isAdded()) return;
                    int mastered = 0;
                    for (Word w : words) if (w.isMastered()) mastered++;
                    float pct = mastered * 100f / words.size();
                    tvBadgeTitle.setText(rankBadge(pct));
                });
    }

    /** כיתה/דירוג בטבלת המובילים/משימות שהושלמו — תלמיד מוסדי בלבד. */
    private void setupInstitutionalStudentExtras() {
        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.getStudentExtras().observe(getViewLifecycleOwner(), extras -> {
            if (extras == null || !isAdded()) return;
            tvClassName.setText(extras.className != null ? "הכיתה שלי: " + extras.className : "");
            tvLeaderboardRank.setText(extras.leaderboardRank > 0
                    ? "מקום #" + extras.leaderboardRank + " בטבלת המובילים" : "");
        });
        profileViewModel.loadStudentExtras();

        StudentAssignmentsViewModel assignmentsViewModel =
                new ViewModelProvider(this).get(StudentAssignmentsViewModel.class);
        assignmentsViewModel.getAssignments().observe(getViewLifecycleOwner(), items -> {
            if (items == null || !isAdded()) return;
            int completed = 0;
            for (StudentAssignmentsViewModel.AssignmentWithProgress item : items) {
                if (item.total > 0 && item.completed >= item.total) completed++;
            }
            tvAssignmentsCompleted.setText("הושלמו " + completed + " מתוך " + items.size() + " משימות");
        });
        assignmentsViewModel.loadAssignments();
    }

    /** מוסד/כיתות/תלמידים/משימות פעילות — מורה/מנהל בלבד. */
    private void setupTeacherStats() {
        TeacherDashboardViewModel teacherViewModel =
                new ViewModelProvider(this).get(TeacherDashboardViewModel.class);
        teacherViewModel.getInstitutionName().observe(getViewLifecycleOwner(), name -> {
            if (isAdded() && name != null) tvInstitutionName.setText(name);
        });
        teacherViewModel.getClasses().observe(getViewLifecycleOwner(), classes -> {
            if (classes == null || !isAdded()) return;
            tvClassCount.setText(classes.size() + " כיתות");
            long totalStudents = 0;
            for (SchoolClass c : classes) totalStudents += c.getStudentCount();
            tvActiveStudentCount.setText(totalStudents + " תלמידים פעילים");
        });
        // TeacherDashboardViewModel טוען אוטומטית בקונסטרקטור (loadDashboard) — אין צורך לקרוא שוב.

        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.getActiveAssignmentCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && isAdded()) tvActiveAssignmentCount.setText(count + " משימות פעילות");
        });
        profileViewModel.loadTeacherActiveAssignmentCount();
    }

    /**
     * מוסד/מורים/כיתות/תלמידים-פעילים/משימות — מנהל מוסד בלבד (role=="principal").
     * בכוונה *לא* TeacherDashboardViewModel.getClasses()/ProfileViewModel.loadTeacherActiveAssignmentCount —
     * שניהם מסוננים לפי teacherId/createdBy של המשתמש המחובר, נתונים שגויים
     * למנהל (צריך את כל המוסד, לא רק את מה שהוא אישית יצר/מלמד). ר' getPrincipalStats
     * (Cloud Function) ו-PrincipalDashboardFragment לאותם נתונים בדיוק, במסך נפרד.
     */
    private void setupPrincipalStats() {
        TeacherDashboardViewModel teacherViewModel =
                new ViewModelProvider(this).get(TeacherDashboardViewModel.class);
        teacherViewModel.getInstitutionName().observe(getViewLifecycleOwner(), name -> {
            if (isAdded() && name != null) tvInstitutionName.setText(name);
        });

        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.getPrincipalStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null || !isAdded()) return;
            tvTeacherCount.setText(stats.teacherCount + " מורים");
            tvClassCount.setText(stats.classCount + " כיתות");
            tvActiveStudentCount.setText(stats.activeStudentsThisWeek + " תלמידים פעילים השבוע");
            tvActiveAssignmentCount.setText(stats.activeAssignmentCount + " משימות פעילות");
        });
        profileViewModel.loadPrincipalStats();
    }

    /** rankBadge — אותו סולם תוארים בדיוק כמו StatisticsFragment.updateRank, לתצוגה כתג הישג. */
    private static String rankBadge(float pct) {
        if (pct < 5)  return "🌱 מתחיל";
        if (pct < 20) return "📖 תלמיד";
        if (pct < 40) return "⭐ מתקדם";
        if (pct < 60) return "🎖 אלוף";
        if (pct < 80) return "👑 נסיך";
        if (pct < 95) return "🏆 מלך";
        return "💎 אגדה";
    }
}