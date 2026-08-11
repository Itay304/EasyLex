package com.example.easylex.ui.settings;

/**
 * =====================================================================
 * SettingsFragment — מסך ההגדרות
 * =====================================================================
 *
 * מה עושה המסך הזה?
 * ------------------
 * מסך ניהול ה"חשבון + הגדרות לימוד + נתונים" של המשתמש.
 * מחולק ל-4 אזורים:
 *   1. חשבון      — שם תצוגה, אימייל, שינוי שם
 *   2. לימוד      — יעד יומי (Slider), TTS, סריקה אישית בלבד
 *   3. נתונים     — סנכרן עכשיו, נקה מילים אישיות, איפוס התקדמות
 *   4. אודות      — גרסת האפליקציה
 *
 * שני מקורות/יעדי נתונים:
 * -------------------------
 *   • SharedPreferences ("settings_prefs") — לכל הגדרות הלימוד (מהיר, מקומי)
 *   • Firebase Auth + Firestore           — לפרטי המשתמש ונתוני Gamification
 *
 * למה ViewBinding?
 * -----------------
 * ViewBinding מייצר קוד Java אוטומטי מה-XML — כל View מקבל שדה בטוח
 * לפי שמו. אין צורך ב-findViewById() וללא חשש של ClassCastException.
 * חייב לאפס ל-null בonDestroyView() כדי למנוע Memory Leak.
 * =====================================================================
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.easylex.BuildConfig;
import com.example.easylex.data.WordRepository;
import com.example.easylex.databinding.FragmentSettingsBinding;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    // ── מפתחות SharedPreferences ────────────────────────────────────────────────
    // קבועים = public static final כך שמסכים אחרים יוכלו לגשת לאותם מפתחות.
    // למשל: SpellingFragment יכול לקרוא KEY_TTS_ENABLED ישירות.

    /** שם קובץ ההגדרות — מזהה את קובץ ה-SharedPreferences ב-Android */
    public static final String PREFS_SETTINGS    = "settings_prefs";

    /** מפתח ל"יעד יומי" — מספר מילים שהמשתמש רוצה לתרגל ביום */
    public static final String KEY_DAILY_GOAL    = "daily_goal_words";

    /** מפתח ל-TTS (Text-To-Speech) — האם לקרוא מילים בקול? */
    public static final String KEY_TTS_ENABLED   = "tts_enabled";

    /** יעד יומי ברירת מחדל — 10 מילים. */
    private static final int DEFAULT_DAILY_GOAL = 10;

    // ── שדות מחלקה ────────────────────────────────────────────────────────────

    /**
     * ViewBinding — גישה בטוחה לכל אלמנטי ה-XML ללא findViewById.
     * מוגדר כ-null ב-onDestroyView למניעת Memory Leak.
     */
    private FragmentSettingsBinding binding;

    /**
     * SharedPreferences — מחסן הגדרות מקומי ומהיר (key-value).
     * מאוחסן על מכשיר המשתמש — לא נמחק בסגירת האפליקציה.
     */
    private SharedPreferences settingsPrefs;

    // ── אתחול ────────────────────────────────────────────────────────────────

    /**
     * onCreateView — נקרא כשהמסך נוצר לראשונה.
     * מאתחל את כל 4 האזורים בסדר: Toolbar → חשבון → לימוד → נתונים → אודות.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        // פותחים את קובץ ההגדרות במצב MODE_PRIVATE — רק האפליקציה יכולה לגשת אליו
        settingsPrefs = requireContext().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);

        setupToolbar();        // כפתור חזרה
        loadAccountInfo();     // שם ואימייל מ-Firebase Auth
        loadLearningSettings(); // Slider, TTS, Personal Only
        setupDataActions();    // כפתורי סנכרון / מחיקה / איפוס
        setupAbout();          // גרסת האפליקציה

        return binding.getRoot();
    }

    /**
     * onDestroyView — ניקוי binding למניעת Memory Leak.
     * Fragment ממשיך לחיות גם אחרי שה-View נהרס (למשל בניווט למסך אחר).
     * אם binding לא מאופס, הוא ימנע מה-View להיאסף ע"י ה-Garbage Collector.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    /**
     * setupToolbar — מחבר כפתור חזרה לסרגל הכלים.
     * onBackPressed() מפעיל את מנגנון הניווט האחורה — חוזר למסך הקודם.
     */
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    // ── חשבון משתמש ──────────────────────────────────────────────────────────

    /**
     * loadAccountInfo — טוען פרטי המשתמש מ-Firebase Auth ומציגם.
     *
     * Firebase Auth שומר מידע על המשתמש המחובר כעת — FirebaseUser.
     * FirebaseUser מכיל:
     *   getDisplayName() — שם התצוגה (ניתן לעדכון)
     *   getEmail()       — אימייל (נקבע בהרשמה, לא ניתן לשינוי כאן)
     *   getUid()         — מזהה ייחודי של המשתמש (לא ניתן לשינוי)
     *
     * אם אין שם — מציגים "משתמש EasyLex" כברירת מחדל.
     */
    private void loadAccountInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            // הצגת שם או ברירת מחדל אם השם ריק
            binding.tvSettingsName.setText(
                    (name != null && !name.isEmpty()) ? name : "משתמש EasyLex");
            // הצגת אימייל
            binding.tvSettingsEmail.setText(
                    user.getEmail() != null ? user.getEmail() : "—");
        }
        // לחיצה על "ערוך שם" → דיאלוג שינוי שם
        binding.btnEditName.setOnClickListener(v -> showEditNameDialog());
    }

    /**
     * showEditNameDialog — מציג דיאלוג Material3 עם שדה טקסט לשינוי שם.
     *
     * במקום להגדיר XML נפרד לדיאלוג — בונים את ה-View מקוד Java:
     *   TextInputLayout = מעטפת Material3 עם כותרת (hint) ושפה
     *   TextInputEditText = שדה הקלט בפועל
     *   LinearLayout = מיכל שמסדר אותם אנכית עם padding
     *
     * ה-density ב-Android = כמה פיקסלים פיזיים יש ב-1dp (תלוי בצפיפות המסך).
     * כדי לחשב padding ב-px: dp × density = px.
     * padding = 20dp × density = pixels מדויקים לכל גודל מסך.
     */
    private void showEditNameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // חישוב padding ב-pixels (20dp) בהתאם לצפיפות המסך
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        TextInputLayout til = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint("שם תצוגה");

        TextInputEditText et = new TextInputEditText(requireContext());
        String current = user.getDisplayName();
        et.setText((current != null && !current.isEmpty()) ? current : "");
        til.addView(et);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(til);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("שנה שם תצוגה")
                .setView(container)
                .setPositiveButton("שמור", (d, w) -> {
                    String newName = et.getText() != null
                            ? et.getText().toString().trim() : "";
                    if (!newName.isEmpty()) updateDisplayName(newName);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * updateDisplayName — מעדכן את שם התצוגה בשני מקומות:
     *   1. Firebase Auth — שם המשתמש הרשמי (getDisplayName())
     *   2. Firestore users/{uid} — שמירה נוספת לצורכי עתיד (למשל Leaderboard)
     *
     * UserProfileChangeRequest = Builder Pattern — בונים בקשת עדכון פרופיל בשלבים.
     *   setDisplayName(name) — מגדיר את השם החדש
     *   build()              — בונה את הבקשה המוגמרת
     *
     * updateProfile() — פעולה async (לא חוסמת את ה-UI):
     *   addOnSuccessListener — קוד שירוץ אחרי הצלחה
     *   addOnFailureListener — קוד שירוץ אחרי כישלון
     *
     * SetOptions.merge() ב-Firestore — כותב רק את השדות שציינו,
     * לא מוחק שדות אחרים במסמך (כמו totalXp, streak וכו').
     *
     * @param name השם החדש שהמשתמש הקליד
     */
    private void updateDisplayName(String name) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // בניית בקשת עדכון פרופיל
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(request)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return; // Fragment כבר לא מחובר לActivity
                    binding.tvSettingsName.setText(name); // עדכון מיידי ב-UI

                    // שמירת שם גם ב-Firestore (merge — לא מוחק שדות אחרים)
                    Map<String, Object> data = new HashMap<>();
                    data.put("displayName", name);
                    FirebaseFirestore.getInstance()
                            .collection("users").document(user.getUid())
                            .set(data, SetOptions.merge()); // .set עם merge = כמו update אבל יוצר מסמך אם לא קיים

                    Toast.makeText(requireContext(),
                            "השם עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "שגיאה בעדכון השם", Toast.LENGTH_SHORT).show());
    }

    // ── הגדרות לימוד ─────────────────────────────────────────────────────────

    /**
     * loadLearningSettings — טוען ומגדיר את 3 בקרות הלימוד:
     *
     * 1. Slider "יעד יומי":
     *    Slider = פס גלילה — המשתמש גורר לבחור מספר מילים לתרגול ביום.
     *    addOnChangeListener = מגיב לכל שינוי בזמן אמת → שומר מיד ל-SharedPreferences.
     *    updateDailyGoalChip() = מציג את המספר הנבחר כ-Chip (תגית) ב-UI.
     *
     * 2. Switch TTS (Text-To-Speech):
     *    Switch = מתג דו-מצבי.
     *    ברירת מחדל = true (TTS מופעל).
     *    setOnCheckedChangeListener = מגיב לשינוי מצב → שומר ל-SharedPreferences.
     *    SpellingFragment קורא מפתח זה לפני כל קריאת TTS.
     *
     * 3. Switch "סריקה אישית בלבד":
     *    אם true — מילים שנסרקות ב-OCR יישמרו רק לרשימה האישית (isFavorite=true).
     *    אם false — יישמרו לרשימה הכללית.
     *    ScanFragment קורא מפתח זה לפני שמירת מילה.
     */
    private void loadLearningSettings() {
        // ── Slider: יעד יומי ──────────────────────────────────────────────────
        int savedGoal = settingsPrefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL);
        binding.sliderDailyGoal.setValue(savedGoal);
        updateDailyGoalChip(savedGoal); // עדכון תגית הציג עם הערך הנוכחי

        // מגיב לשינוי הסליידר בזמן אמת — שומר מיידית ל-SharedPreferences
        binding.sliderDailyGoal.addOnChangeListener((slider, value, fromUser) -> {
            int goal = (int) value;
            updateDailyGoalChip(goal);
            settingsPrefs.edit().putInt(KEY_DAILY_GOAL, goal).apply(); // apply() = async, לא חוסם UI
        });

        // ── Switch: TTS ───────────────────────────────────────────────────────
        boolean ttsEnabled = settingsPrefs.getBoolean(KEY_TTS_ENABLED, true);
        binding.switchTts.setChecked(ttsEnabled);
        binding.switchTts.setOnCheckedChangeListener((btn, isChecked) ->
                settingsPrefs.edit().putBoolean(KEY_TTS_ENABLED, isChecked).apply());

    }

    /**
     * updateDailyGoalChip — מעדכן את תגית היעד היומי עם הערך הנוכחי.
     * Chip = רכיב Material3 שנראה כמו תגית/פיל — מציג ערך קומפקטי.
     *
     * @param goal מספר המילים שנבחר ב-Slider
     */
    private void updateDailyGoalChip(int goal) {
        binding.chipDailyGoal.setText(goal + " מילים");
    }

    // ── פעולות נתונים ─────────────────────────────────────────────────────────

    /**
     * setupDataActions — מגדיר 3 כפתורי פעולה על נתונים:
     *
     * 1. "סנכרן עכשיו":
     *    - מסנכרן XP/רמה/רצף מ-Firestore (GamificationEngine.syncFromFirestore)
     *    - מסנכרן את רשימת המילים הגלובלית מ-Firestore (insert-only — לא מוחק נתוני תרגול)
     *    - מבטל את הכפתור בזמן הסנכרון (מונע לחיצה כפולה)
     *    - מחזיר את הכפתור ב-runOnUiThread (הסנכרון רץ בthread רקע)
     *
     *    runOnUiThread() — כל עדכון UI חייב לרוץ ב-Main Thread (UI Thread).
     *    Firestore callbacks רצים ב-background thread — לכן צריך לחזור ל-UI thread.
     *
     * 2. "נקה רשימה אישית":
     *    - מציג אזהרה לפני מחיקה (פעולה לא הפיכה!)
     *    - deletePersonalWords() מוחק מ-Room את כל המילים עם isFavorite=true
     *
     * 3. "אפס התקדמות":
     *    - מציג אזהרה לפני איפוס (פעולה לא הפיכה!)
     *    - resetProgress() מאפס XP/רמה/רצף ב-SharedPreferences וב-Firestore
     *
     * MaterialAlertDialogBuilder — בונה דיאלוג Material3 עם כפתורי "אשר"/"בטל".
     * setNegativeButton("ביטול", null) — null = ה-listener הוא null → הדיאלוג פשוט נסגר.
     */
    private void setupDataActions() {
        // ── כפתור: סנכרן עכשיו ───────────────────────────────────────────────
        binding.btnSyncNow.setOnClickListener(v -> {
            binding.btnSyncNow.setEnabled(false); // מניעת לחיצה כפולה
            Toast.makeText(requireContext(), "מסנכרן...", Toast.LENGTH_SHORT).show();

            // סנכרון נתוני Gamification (XP, Level, Streak) מ-Firestore
            GamificationEngine.getInstance(requireContext()).syncFromFirestore(null);

            // סנכרון רשימת המילים הגלובלית — insert-only, לא פוגע בנתוני תרגול
            new WordRepository(requireActivity().getApplication())
                    .syncGlobalWordsFromFirestore(() -> {
                        if (getActivity() == null) return; // Fragment כבר לא פעיל
                        // חזרה ל-UI Thread לעדכון הכפתור (הסנכרון רץ בThread רקע)
                        requireActivity().runOnUiThread(() -> {
                            if (binding == null) return;
                            binding.btnSyncNow.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    "הנתונים סונכרנו בהצלחה", Toast.LENGTH_SHORT).show();
                        });
                    });
        });

        // ── כפתור: נקה רשימה אישית ──────────────────────────────────────────
        binding.btnClearPersonalWords.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("נקה רשימה אישית")
                        .setMessage("האם אתה בטוח שברצונך למחוק את כל המילים שהוספת?\nפעולה זו אינה ניתנת לביטול.")
                        .setNegativeButton("ביטול", null) // null = סוגר את הדיאלוג בלבד
                        .setPositiveButton("מחק", (dialog, which) -> {
                            // מוחק מ-Room את כל המילים האישיות (isFavorite=true)
                            new WordRepository(requireActivity().getApplication())
                                    .deletePersonalWords();
                            Toast.makeText(requireContext(),
                                    "הרשימה האישית נוקתה", Toast.LENGTH_SHORT).show();
                        })
                        .show()
        );

        // ── כפתור: אפס התקדמות ───────────────────────────────────────────────
        binding.btnResetProgress.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("איפוס התקדמות")
                        .setMessage("האם אתה בטוח שברצונך לאפס את כל נקודות ה-XP, הרמה והרצף שלך?\nפעולה זו אינה ניתנת לביטול.")
                        .setNegativeButton("ביטול", null)
                        .setPositiveButton("אפס", (dialog, which) -> resetProgress())
                        .show()
        );
    }

    /**
     * resetProgress — מאפס את כל נתוני ה-Gamification בשני מקומות:
     *
     * 1. SharedPreferences ("gamification_prefs"):
     *    - מסיר: total_xp, current_level, streak, last_streak_date
     *    - remove() = מסיר מפתח לחלוטין; בקריאה הבאה יוחזר ערך ברירת המחדל (0)
     *    - apply() = שמירה async (לא חוסמת UI)
     *
     * 2. Firestore — users/{uid}:
     *    - מעדכן: totalXp=0, level=1, streak=0
     *    - update() = עדכון שדות ספציפיים בלבד (לא מוחק שדות אחרים)
     *
     * מדוע שני מקומות?
     * -----------------
     * GamificationEngine כותב ל-SharedPreferences (מהיר, מקומי)
     * + ל-Firestore (גיבוי בענן). לכן האיפוס חייב לכסות שניהם.
     * אחרת המשתמש יאפס מקומית אך בסנכרון הבא — הנתונים הישנים יחזרו מהענן!
     */
    private void resetProgress() {
        // מחיקת נתוני Gamification מ-SharedPreferences
        requireContext()
                .getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("total_xp")
                .remove("current_level")
                .remove("streak")
                .remove("last_streak_date")
                .remove("daily_quiz_date")     // מאפס Badge ציון מבחן יומי
                .remove("daily_quiz_score")    // מאפס ניקוד מבחן יומי
                .remove("daily_correct_count") // מאפס מונה תשובות נכונות יומיות
                .remove("daily_correct_date")
                .apply();

        // עדכון Firestore — XP/רמה/רצף
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> reset = new HashMap<>();
            reset.put("totalXp", 0L);
            reset.put("level",   1L);
            reset.put("streak",  0L);
            FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .update(reset);
        }

        // איפוס correctAttempts / totalAttempts / שגיאות של כל המילים ב-Room
        new com.example.easylex.data.WordRepository(requireActivity().getApplication())
                .resetAllWordProgress(null);

        Toast.makeText(requireContext(),
                "כל ההתקדמות אופסה בהצלחה", Toast.LENGTH_SHORT).show();
    }

    // ── אודות ─────────────────────────────────────────────────────────────────

    /**
     * setupAbout — מציג את גרסת האפליקציה.
     *
     * BuildConfig.VERSION_NAME — קבוע שנוצר אוטומטית מ-build.gradle בזמן קומפילציה.
     * הוא מייצג את מחרוזת הגרסה (למשל "1.1").
     * כך אנחנו לא צריכים לעדכן את ה-String ידנית בכל שחרור — הכל אוטומטי.
     */
    private void setupAbout() {
        binding.tvSettingsVersion.setText(BuildConfig.VERSION_NAME);
    }
}
