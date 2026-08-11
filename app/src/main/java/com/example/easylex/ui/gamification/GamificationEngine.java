package com.example.easylex.ui.gamification;

/**
 * =====================================================================
 * GamificationEngine — מנוע הגמיפיקציה של האפליקציה
 * =====================================================================
 *
 * מה זה גמיפיקציה?
 * -----------------
 * גמיפיקציה = שילוב מנגנוני משחק בתוך אפליקציה לימודית.
 * בEasyLex: XP (נקודות ניסיון), רמות (Level), ורצף יומי (Streak).
 * המטרה: לשמור על מוטיבציה — המשתמש "מרוויח" על כל תשובה נכונה.
 *
 * Pattern: Singleton (מופע יחיד)
 * --------------------------------
 * ישנו מופע אחד בלבד של GamificationEngine בכל הרצת האפליקציה.
 * כל Fragment שרוצה לתת XP או לבדוק רמה — קורא ל-getInstance().
 * Double-Checked Locking עם volatile מבטיח Thread-safety.
 *
 * שמירת נתונים — כפולה:
 * -----------------------
 * • SharedPreferences ("gamification_prefs") — קריאה מהירה ומקומית (synchronous)
 * • Firestore users/{uid} — גיבוי ענן (async) — שומר totalXp, level, streak
 * הכתיבה לFirestore היא fire-and-forget (לא מחכים לתשובה).
 *
 * XP Events:
 *   +10  XP — תשובה נכונה בכל מודול
 *   +100 XP — מילה הגיעה ל-Mastery 5 בפעם הראשונה (Bonus)
 *   +50  XP — השלמת מודול עם ציון >= 60%
 *
 * נוסחת הרמה:
 *   L = floor(sqrt(XP / 100)) + 1
 *   לדוגמה: 400 XP → sqrt(4)+1 = 3 → רמה 3
 *
 * XP לרמה L מתחיל ב: (L-1)² × 100
 *   רמה 1: 0 XP | רמה 2: 100 XP | רמה 3: 400 XP | רמה 4: 900 XP
 * =====================================================================
 */

import android.content.Context;
import android.content.SharedPreferences;

import com.example.easylex.data.Word;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Singleton that manages XP, Level, and Streak.
 * Dual-writes to SharedPreferences (fast local reads) and Firestore (cloud persistence).
 *
 * XP events:
 *   - Correct answer:     +10 XP
 *   - Mastery 4→5 bonus:  +100 XP (once per word crossing)
 *   - Module complete:    +50 XP (if caller's score >= 60%)
 *
 * Level formula:  L = floor(sqrt(XP / 100)) + 1
 * XP for level L: (L-1)^2 * 100
 */
public class GamificationEngine {

    // ── מפתחות SharedPreferences ──────────────────────────────────────────────
    private static final String PREFS_NAME      = "gamification_prefs";
    private static final String KEY_TOTAL_XP    = "total_xp";
    private static final String KEY_LEVEL       = "current_level";
    private static final String KEY_STREAK      = "streak";
    private static final String KEY_STREAK_DATE        = "last_streak_date";  // yyyy-MM-dd
    private static final String KEY_DAILY_CORRECT_PREFIX = "daily_correct_";   // + yyyy-MM-dd

    // ── ערכי XP ──────────────────────────────────────────────────────────────
    private static final int XP_CORRECT_ANSWER  = 10;  // תשובה נכונה
    private static final int XP_MODULE_COMPLETE = 50;  // השלמת מודול
    private static final int XP_WORD_MASTERED   = 100; // בונוס מעבר ל-Mastery 5

    /**
     * volatile — מבטיח שכל Thread רואה את הערך העדכני של instance.
     * בלי volatile, ייתכן שThread אחד יראה גרסה ישנה מה-Cache שלו.
     */
    private static volatile GamificationEngine instance;

    private final SharedPreferences prefs; // קריאה/כתיבה מהירה מקומית
    private final FirebaseFirestore db;    // חיבור ל-Firestore לגיבוי ענן

    /** קונסטרקטור פרטי — מונע יצירת מופעים ישירה מבחוץ (חלק מ-Singleton Pattern). */
    private GamificationEngine(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db    = FirebaseFirestore.getInstance();
    }

    /**
     * getInstance — מחזיר את המופע היחיד של GamificationEngine.
     * יוצר אותו בפעם הראשונה (Lazy Initialization).
     *
     * Double-Checked Locking:
     *   בדיקה ראשונה בלי נעילה (מהיר אחרי האתחול).
     *   בדיקה שנייה עם נעילה (בפעם הראשונה בלבד).
     */
    public static GamificationEngine getInstance(Context ctx) {
        if (instance == null) {
            synchronized (GamificationEngine.class) {
                if (instance == null) instance = new GamificationEngine(ctx);
            }
        }
        return instance;
    }

    // ── אירועי XP ────────────────────────────────────────────────────────────

    /**
     * onCorrectAnswer — נקרא מכל מודול לאחר תשובה נכונה.
     *
     * @param wordBefore מצב המילה לפני עדכון הנתונים (snapshot)
     * @param wordAfter  מצב המילה אחרי עדכון הנתונים
     *
     * למה צריך "לפני ואחרי"?
     * כדי לזהות אם המילה עלתה ל-Mastery 5 בדיוק בניסיון הזה,
     * ולא לתת בונוס שוב אם כבר הייתה ב-5.
     *
     * Call after a correct answer in Quiz or Spelling. Pass word state before and after update.
     */
    public void onCorrectAnswer(Word wordBefore, Word wordAfter) {
        int gained = XP_CORRECT_ANSWER; // בסיס: 10 XP
        // בונוס: רק כשחוצים מ-<5 ל-5 (לא כל פעם שהמילה ב-5)
        // Word mastery bonus: only when crossing from <5 to 5
        if (wordAfter.getMasteryLevel() == 5 && wordBefore.getMasteryLevel() < 5) {
            gained += XP_WORD_MASTERED; // +100 XP בונוס
        }
        recordDailyActivity(); // תיעוד לסטטיסטיקת פעילות יומית
        addXp(gained);
        checkStreak();         // בדיקת עדכון רצף יומי
    }

    /**
     * onModuleComplete — נקרא בסיום שאלון עם ציון >= 60%.
     * Call at quiz end when score >= 60%.
     */
    public void onModuleComplete() {
        addXp(XP_MODULE_COMPLETE);
    }

    // ── Getters — קריאה מהירה מ-SharedPreferences ────────────────────────────

    /** סך כל נקודות ה-XP שנצברו. */
    public int getTotalXp()      { return prefs.getInt(KEY_TOTAL_XP, 0); }

    /** הרמה הנוכחית (מחושבת לפי נוסחת sqrt). */
    public int getCurrentLevel() { return prefs.getInt(KEY_LEVEL, 1); }

    /** מספר ימי הרצף הנוכחי. */
    public int getStreak()       { return prefs.getInt(KEY_STREAK, 0); }

    /**
     * getDailyCorrectCount — כמה תשובות נכונות נענו היום.
     * מאוחסן לפי מפתח "daily_correct_yyyy-MM-dd".
     * Correct answers recorded today.
     */
    public int getDailyCorrectCount() {
        return prefs.getInt(KEY_DAILY_CORRECT_PREFIX + todayString(), 0);
    }

    /**
     * getWeeklyActivity — מספר תשובות נכונות ל-7 הימים האחרונים.
     * Index 0 = לפני 6 ימים, Index 6 = היום.
     * משמש לגרף הפעילות השבועית במסך הסטטיסטיקות.
     * Returns correct answer counts for the last 7 days.
     */
    public int[] getWeeklyActivity() {
        int[] result = new int[7];
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        for (int i = 0; i < 7; i++) {
            java.util.Calendar day = (java.util.Calendar) cal.clone();
            day.add(java.util.Calendar.DAY_OF_YEAR, -(6 - i));
            result[i] = prefs.getInt(KEY_DAILY_CORRECT_PREFIX + sdf.format(day.getTime()), 0);
        }
        return result;
    }

    /**
     * XP at which level L starts: (L-1)^2 * 100
     * XP שרמה L מתחילה בו.
     * רמה 1: 0 | רמה 2: 100 | רמה 3: 400 | רמה 4: 900
     */
    public int getXpForLevelStart(int level) {
        int l = level - 1;
        return l * l * 100;
    }

    /**
     * XP at which the next level starts: level^2 * 100
     * XP שהרמה הבאה מתחילה בו.
     */
    public int getXpForNextLevel() {
        int level = getCurrentLevel();
        return level * level * 100;
    }

    /**
     * XP earned within the current level (for progress bar).
     * כמה XP צברנו מתוך הרמה הנוכחית — לסרגל ההתקדמות.
     */
    public int getXpWithinLevel() {
        return getTotalXp() - getXpForLevelStart(getCurrentLevel());
    }

    /**
     * XP range of the current level (progress bar max).
     * כמה XP כוללת הרמה הנוכחית מהתחלה לסוף — מגדיר מקסימום הסרגל.
     */
    public int getXpRangeOfLevel() {
        int level = getCurrentLevel();
        return getXpForNextLevel() - getXpForLevelStart(level);
    }

    // ── סנכרון Firestore ──────────────────────────────────────────────────────

    /**
     * syncFromFirestore — טוען XP/רמה/רצף מה-Firestore אל SharedPreferences.
     *
     * מתי קוראים לזה?
     *   onResume() של מסכי Statistics ו-Practice (בכל כניסה למסך).
     *   כך אם המשתמש התרגל במכשיר אחר, הנתונים יתעדכנו.
     *
     * @param onComplete Runnable שיופעל לאחר קבלת נתונים — לרענון ה-UI.
     *
     * Loads server-side XP/level/streak into SharedPreferences.
     * Call in onResume() of Statistics/Practice fragments (safe to call repeatedly).
     */
    public void syncFromFirestore(Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { if (onComplete != null) onComplete.run(); return; }

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // קריאת ערכים מה-Firestore ושמירתם מקומית
                    Long xp      = doc.getLong("totalXp");
                    Long level   = doc.getLong("level");
                    Long streak  = doc.getLong("streak");
                    String date  = doc.getString("lastActiveDate");
                    SharedPreferences.Editor ed = prefs.edit();
                    if (xp     != null) ed.putInt(KEY_TOTAL_XP, xp.intValue());
                    if (level  != null) ed.putInt(KEY_LEVEL,    level.intValue());
                    if (streak != null) ed.putInt(KEY_STREAK,   streak.intValue());
                    if (date   != null) ed.putString(KEY_STREAK_DATE, date);
                    ed.apply();
                }
                if (onComplete != null) onComplete.run(); // עדכון ה-UI
            })
            .addOnFailureListener(e -> { if (onComplete != null) onComplete.run(); });
    }

    /**
     * clearLocalData — מנקה את כל נתוני הגמיפיקציה המקומיים (XP, רמה, רצף,
     * lastActiveDate, וספירות daily_correct_* לגרף הפעילות השבועית).
     * נקרא ב-logout (ר' ProfileFragment), לצד WordRepository.deleteAll() —
     * אחרת משתמש הבא שמתחבר על אותו מכשיר רואה XP/רמה/רצף/גרף פעילות של
     * המשתמש הקודם. prefs.edit().clear() מוחק את כל הקובץ "gamification_prefs"
     * (ייעודי לגמיפיקציה בלבד — לא משותף עם settings_prefs/app_prefs).
     */
    public void clearLocalData() {
        prefs.edit().clear().apply();
    }

    // ── פונקציות פנימיות (private) ───────────────────────────────────────────

    /**
     * addXp — מוסיף XP, מחשב רמה חדשה, שומר מקומית ומסנכרן לענן.
     */
    private void addXp(int amount) {
        int newXp    = getTotalXp() + amount;
        int newLevel = computeLevel(newXp); // חישוב רמה לפי הנוסחה
        prefs.edit()
            .putInt(KEY_TOTAL_XP, newXp)
            .putInt(KEY_LEVEL,    newLevel)
            .apply(); // שמירה מקומית מיידית
        persistToFirestore(newXp, newLevel); // סנכרון ענן ברקע
    }

    /**
     * computeLevel — נוסחת חישוב הרמה.
     * L = floor(sqrt(XP / 100)) + 1
     * @param xp סך כל נקודות ה-XP
     * @return רמה (מינימום 1)
     */
    private static int computeLevel(int xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0)) + 1;
    }

    /**
     * checkStreak — בודק ומעדכן את הרצף היומי.
     * מופעל לאחר כל תשובה נכונה.
     * אם כבר בדקנו היום — מדלגים (מניעת עדכון כפול).
     * הלוגיקה בפועל מבוצעת ב-StreakManager (בודק כמה ימים עברו מהפעם האחרונה).
     */
    private void checkStreak() {
        String today       = todayString();
        String lastChecked = prefs.getString(KEY_STREAK_DATE, "");
        if (today.equals(lastChecked)) return;  // already checked today

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // StreakManager בודק ב-Firestore את תאריך הפעילות האחרונה
        // ומחשב: +1 אם פעיל אתמול, reset ל-1 אם דילג יום
        StreakManager.checkAndUpdate(user.getUid(), getStreak(), db, newStreak -> {
            prefs.edit()
                .putInt(KEY_STREAK, newStreak)
                .putString(KEY_STREAK_DATE, today)
                .apply();
        });
    }

    /**
     * persistToFirestore — שומר XP ורמה ב-Firestore.
     * אם המסמך לא קיים — יוצר אותו (set במקום update).
     * Fire-and-forget: לא מחכים לתשובה, לא מטפלים בשגיאות קריטיות.
     */
    private void persistToFirestore(int xp, int level) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalXp", (long) xp);
        updates.put("level",   (long) level);

        db.collection("users").document(user.getUid())
            .update(updates)
            .addOnFailureListener(e ->
                // אם update נכשל (מסמך לא קיים) — create עם set
                db.collection("users").document(user.getUid()).set(updates)
            );
    }

    /**
     * recordDailyActivity — מוסיף 1 לספירת התשובות הנכונות של היום.
     * מפתח הוא: "daily_correct_yyyy-MM-dd"
     * נשמר ב-SharedPreferences לצורך גרף הפעילות השבועית.
     */
    private void recordDailyActivity() {
        String key = KEY_DAILY_CORRECT_PREFIX + todayString();
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply();
    }

    /**
     * todayString — מחזיר את התאריך היום בפורמט "yyyy-MM-dd".
     * משמש כמפתח ב-SharedPreferences ולהשוואת תאריכים.
     */
    private static String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
