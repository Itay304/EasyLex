package com.example.easylex;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.easylex.data.UserRoleManager;
import com.example.easylex.databinding.ActivityMainBinding;

/**
 * =====================================================================
 * MainActivity — המסך הראשי של האפליקציה (Shell Activity)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * MainActivity היא "המעטפת" של האפליקציה.
 * היא עצמה לא מציגה תוכן — היא מכילה:
 *   1. NavHostFragment — מיכל שבו Fragment אחד מוצג בכל פעם
 *   2. BottomNavigationView — סרגל ניווט תחתון עם 5 לשוניות:
 *      בית | מילים | תרגול | סריקה | פרופיל
 *
 * ניווט:
 * ------
 * NavigationUI.setupWithNavController() מחבר את ה-BottomNav לניווט.
 * לחיצה על לשונית → ה-NavController מחליף את ה-Fragment הנוכחי.
 *
 * ViewBinding:
 * ------------
 * ActivityMainBinding.inflate() — ניגשים ל-Views ישירות דרך binding
 * (ללא findViewById).
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * SplashActivity, LoginActivity, RegisterActivity — כולן מנווטות אליה.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * EXTRA_OPEN_JOIN_INSTITUTION — משימה 0.8: LoginActivity/RegisterActivity
     * מעבירות boolean=true כשהמשתמש לחץ "אני מורה — יש לי קוד מוסד" ואז התחבר/נרשם
     * בהצלחה. MainActivity מנווט ל-navigation_join_institution במקום להישאר
     * במסך הבית הרגיל. לא נוגע בזרימת הניווט הרגילה כשההרחבה הזו לא קיימת.
     */
    public static final String EXTRA_OPEN_JOIN_INSTITUTION = "open_join_institution";

    /**
     * EXTRA_OPEN_JOIN_CLASS — משימה 0.10: LoginActivity/RegisterActivity מעבירות
     * boolean=true כשהמשתמש לחץ "יש לי קוד כיתה" ואז התחבר/נרשם בהצלחה.
     * MainActivity מנווט ל-navigation_join_class במקום להישאר במסך הבית הרגיל.
     * אותו דפוס בדיוק כמו EXTRA_OPEN_JOIN_INSTITUTION, נתיב עצמאי משלו.
     */
    public static final String EXTRA_OPEN_JOIN_CLASS = "open_join_class";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ה-Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. הגדרת הניווט (NavController)
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // משימה 0.8: אם הגענו הנה עם כוונה להצטרף כמורה — נווט ישר למסך ההצטרפות.
            // עדיין בגרף/בתפריט של התלמיד (הצטרפות עצמה נגישה משם — הוא עוד
            // לא מורה עד שהקריאה ל-joinAsTeacher מצליחה).
            if (getIntent().getBooleanExtra(EXTRA_OPEN_JOIN_INSTITUTION, false)) {
                NavigationUI.setupWithNavController(binding.bottomNavView, navController);
                binding.roleCheckOverlay.setVisibility(View.GONE);
                navController.navigate(R.id.navigation_join_institution);
            } else if (getIntent().getBooleanExtra(EXTRA_OPEN_JOIN_CLASS, false)) {
                // משימה 0.10: אותו דפוס בדיוק כמו EXTRA_OPEN_JOIN_INSTITUTION —
                // המשתמש עדיין תלמיד (role לא משתנה ב-joinClass), אז נשארים
                // בגרף/בתפריט של התלמיד ורק מנווטים למסך ההצטרפות לכיתה.
                NavigationUI.setupWithNavController(binding.bottomNavView, navController);
                binding.roleCheckOverlay.setVisibility(View.GONE);
                navController.navigate(R.id.navigation_join_class);
            } else {
                // תיקון באג: role נטען לפני שמציגים/מחברים משהו אינטראקטיבי —
                // לא אחרי. roleCheckOverlay (גלוי כברירת מחדל ב-XML) חוסם את
                // דשבורד התלמיד עד שה-role נקבע סופית, ורק אז מוסר עצמו.
                // בלי זה, דשבורד התלמיד האינטראקטיבי היה גלוי/לחיץ למשך כל
                // זמן בדיקת ה-role (יכול לקחת כמה שניות ברשת איטית) — מורה
                // שרואה/נוגע בו בחלון הזה חווה את זה כאילו הניתוב "לא עובד".
                // עדיין לא מנווטים/מחברים bottom-nav לפני שהטעינה הסתיימה —
                // זה בדיוק הלקח מההערה למטה (checkAdminStatus גרם לקריסה
                // כשנבדק מוקדם מדי ב-lifecycle) — רק שעכשיו גם לא *רואים*
                // את מה שמתחתיו בזמן ההמתנה.
                UserRoleManager.getInstance().refresh(() -> {
                    String role = UserRoleManager.getInstance().getRole();
                    boolean isTeacherOrPrincipal = UserRoleManager.ROLE_TEACHER.equals(role)
                            || UserRoleManager.ROLE_PRINCIPAL.equals(role);
                    // שינוי ארכיטקטוני — 4 עולמות לפי role: תלמיד מוסדי (student
                    // עם institutionId) מקבל גרף נפרד משלו (institutional_nav_graph.xml,
                    // 4 טאבים), לא את nav_graph.xml הרגיל (5 טאבים) ולא את
                    // teacher_nav_graph.xml. superadmin נשאר זמנית על nav_graph.xml
                    // הרגיל (5 טאבים) — אין עדיין ממשק ניהול ייעודי, ר' הערה בסוף.
                    boolean isInstitutionalStudent = UserRoleManager.ROLE_STUDENT.equals(role)
                            && UserRoleManager.getInstance().getInstitutionId() != null;
                    if (isTeacherOrPrincipal) {
                        switchToTeacherNavigation(navController);
                    } else if (isInstitutionalStudent) {
                        switchToInstitutionalNavigation(navController);
                    }
                    // מחברים את ה-BottomNavigationView לניווט רק עכשיו — אחרי
                    // שהוחלט (ואם צריך, הוחלף) איזה גרף/תפריט רלוונטי. תלמיד
                    // עצמאי / superadmin / ללא role: הגרף/התפריט המקוריים מה-XML
                    // (5 טאבים) נשארים ללא שינוי — navigation_home כבר מוצג מתחת
                    // ל-overlay.
                    NavigationUI.setupWithNavController(binding.bottomNavView, navController);
                    binding.roleCheckOverlay.setVisibility(View.GONE);
                });
            }
        }

        // הסרנו מכאן את checkAdminStatus כי הוא גרם לקריסה!
        // עכשיו הבדיקה מתבצעת בתוך ה-ProfileFragment (ובמסכים חדשים — UserRoleManager).
    }

    /**
     * switchToTeacherNavigation — מחליף את גרף הניווט ואת תפריט ה-bottom
     * navigation לגרסה הייעודית למורה/מנהל (teacher_nav_graph.xml, 3 טאבים:
     * כיתות/פרופיל/עוד) במקום ברירת המחדל של התלמיד (nav_graph.xml, 5 טאבים).
     *
     * למה זה נחוץ?
     * -------------
     * בלי זה, למורה לא הייתה דרך לחזור לדשבורד שלו אחרי שמנווט למסך אחר —
     * navigation_teacher_dashboard לא היה חלק מאף bottom-nav menu בכלל.
     *
     * setGraph() מחליף את הגרף כולו (כולל back stack) ומנווט אוטומטית
     * ל-startDestination של הגרף החדש (navigation_teacher_dashboard) —
     * לכן לא צריך עוד navigate()/popUpTo נפרד כמו בגרסה הקודמת.
     */
    private void switchToTeacherNavigation(NavController navController) {
        navController.setGraph(R.navigation.teacher_nav_graph);
        binding.bottomNavView.getMenu().clear();
        binding.bottomNavView.inflateMenu(R.menu.teacher_bottom_nav_menu);
    }

    /**
     * switchToInstitutionalNavigation — מחליף את גרף הניווט ואת תפריט ה-bottom
     * navigation לגרסה הייעודית לתלמיד מוסדי (institutional_nav_graph.xml,
     * 4 טאבים: בית/משימות/סטטיסטיקות/פרופיל) במקום ברירת המחדל של התלמיד
     * העצמאי (nav_graph.xml, 5 טאבים). אותו דפוס בדיוק כמו switchToTeacherNavigation.
     */
    private void switchToInstitutionalNavigation(NavController navController) {
        navController.setGraph(R.navigation.institutional_nav_graph);
        binding.bottomNavView.getMenu().clear();
        binding.bottomNavView.inflateMenu(R.menu.institutional_bottom_nav_menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            return navHostFragment.getNavController().navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}