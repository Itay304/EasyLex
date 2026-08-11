package com.example.easylex.ui.mywords;

/**
 * =====================================================================
 * MyWordsViewModel — שכבת ה-ViewModel של רשימת המילים
 * =====================================================================
 *
 * מה זה ViewModel?
 * -----------------
 * ViewModel הוא שכבת הלוגיקה שעומדת בין ה-UI (Fragment) לנתונים
 * (Repository). הוא שורד סיבובי מסך (Rotation) — Fragment נהרס
 * ונוצר מחדש, אבל ה-ViewModel נשמר.
 *
 * למה AndroidViewModel ולא ViewModel רגיל?
 * -----------------------------------------
 * AndroidViewModel מקבל Application context — נדרש לגישה ל-Room DB
 * ול-SharedPreferences. Application context בטוח יותר מ-Activity context
 * כי הוא לא תלוי במחזור-חיי ה-Activity.
 *
 * מה ה-ViewModel הזה עושה?
 * -------------------------
 * 1. פותח חיבור ל-Repository (מקור הנתונים)
 * 2. מבצע Migration חד-פעמי (ניקוי מילים ישנות שנשמרו בשגיאה)
 * 3. מסנכרן מ-Firestore פעם אחת ב-24 שעות (throttle)
 * 4. חושף LiveData לכל Fragment שמאזין לו
 *
 * שימוש בכמה Fragments:
 * ----------------------
 * MyWordsFragment, QuizFragment, SpellingFragment, FlashcardsFragment,
 * PracticeFragment, StatisticsFragment — כולם משתמשים ב-ViewModel הזה.
 * כל אחד מקבל מופע נפרד (scoped לFragment שלו), אבל כולם
 * מאזינים לאותו LiveData מה-Repository.
 * =====================================================================
 */

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.easylex.data.Word;
import com.example.easylex.data.WordRepository;

import java.util.List;

public class MyWordsViewModel extends AndroidViewModel {

    private final WordRepository mRepository;

    /**
     * LiveData של כל המילים — "ערוץ חי" שה-Fragments מאזינים לו.
     * כל שינוי ב-DB (הוספה, עדכון, מחיקה) מגיע אוטומטית לכל Observer.
     */
    private final LiveData<List<Word>> mAllWords;

    /**
     * קונסטרקטור — נקרא ע"י Android פעם אחת ליצירת ה-ViewModel.
     * @param application context האפליקציה — לא Activity (בטוח לאורך זמן)
     */
    public MyWordsViewModel(@NonNull Application application) {
        super(application);
        mRepository = new WordRepository(application);
        mAllWords   = mRepository.getAllWords();

        // ── Migration חד-פעמי ─────────────────────────────────────────────
        // לפני תיקון הבאג, מילים גלובליות ישנות נשמרו עם isVerified=false.
        // אנחנו מזהים אותן לפי: isVerified=0 AND isFavorite=0
        // (מילים אישיות אמיתיות תמיד יש להן isFavorite=1)
        // הדגל "personal_words_migration_v1" מונע הרצת המחיקה יותר מפעם אחת.
        SharedPreferences appPrefs = application
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        if (!appPrefs.getBoolean("personal_words_migration_v1", false)) {
            appPrefs.edit().putBoolean("personal_words_migration_v1", true).apply();
            mRepository.deleteUnmarkedGlobalWords(); // ניקוי חד-פעמי של מילים ישנות
        }

        // ── Real-time listener למחיקות מנהל ──────────────────────────────
        // מאזין לאירועי REMOVED מ-Firestore — כשמנהל מוחק מילה, היא
        // נמחקת מ-Room מיד על כל מכשיר מחובר, ללא המתנה ל-24h.
        mRepository.startGlobalDeleteListener();

        // ── Throttle סנכרון — פעם אחת ב-24 שעות ─────────────────────────
        // בעבר הסנכרון רץ בכל יצירת ViewModel (=כל ניווט בין מסכים).
        // הסנכרון מחק ושחזר 1902 מילים — ומחק את כל נתוני התרגול!
        // הפתרון: שמירת זמן הסנכרון האחרון ב-SharedPreferences,
        // ובדיקה שעברו לפחות 24 שעות לפני הסנכרון הבא.
        // Sync global words from Firestore at most once every 24 hours.
        // The repository only inserts NEW words; existing words keep all their practice data.
        long lastSync = appPrefs.getLong("last_global_sync_ms", 0);
        if (System.currentTimeMillis() - lastSync > 24L * 60 * 60 * 1000) {
            // עברו יותר מ-24 שעות — מסנכרן ועדכן את זמן הסנכרון
            appPrefs.edit().putLong("last_global_sync_ms", System.currentTimeMillis()).apply();
            mRepository.syncGlobalWordsFromFirestore(null);
        }
    }

    // ── API ציבורי — מה שה-Fragments רשאים לקרוא ─────────────────────────────

    /**
     * מחזיר LiveData של כל המילים (גלובליות + אישיות).
     * Fragment קורא ל-observe() על זה וה-RecyclerView מתעדכן אוטומטית.
     */
    public LiveData<List<Word>> getAllWords()      { return mAllWords; }

    /**
     * מחזיר LiveData של מילים אישיות בלבד (isVerified=false).
     * נקרא כשמסננים ל"המילים שלי".
     */
    public LiveData<List<Word>> getPersonalWords() { return mRepository.getPersonalWords(); }

    /** הוספת מילה חדשה — נקרא כשהמשתמש מוסיף מילה ידנית או מ-OCR. */
    public void insert(Word word) { mRepository.insert(word); }

    /**
     * עדכון מילה קיימת — נקרא בכל מודולי התרגול לאחר כל תשובה.
     * מעדכן correctAttempts, totalAttempts, errorFlags וכו' ב-DB.
     */
    public void update(Word word) { mRepository.update(word); }

    /** מחיקת כל המילים האישיות — נקרא מ-Settings → "נקה רשימה אישית". */
    public void deletePersonalWords() { mRepository.deletePersonalWords(); }

    /**
     * סנכרון ידני מ-Firestore — נקרא מ-Settings → "סנכרן עכשיו".
     * לא בודק את ה-throttle — מסנכרן תמיד.
     */
    public void syncFromCloud() {
        mRepository.syncGlobalWordsFromFirestore(null);
    }

    /**
     * resetSyncThrottle — מאפס את שעון הסנכרון האחרון (last_global_sync_ms).
     *
     * למה זה נחוץ?
     * -------------
     * ה-Throttle של 24 שעות (למעלה) הוא ברמת המכשיר (SharedPreferences
     * "app_prefs"), לא ברמת המשתמש המחובר. logout כבר מוחק את כל Room
     * (ר' ProfileFragment.btnLogout) — בלי לאפס גם את שעון הסנכרון,
     * משתמש הבא שמתחבר על אותו מכשיר תוך פחות מ-24 שעות מקבל Room ריק
     * שלא מתמלא מחדש עד שהחלון חולף (בפועל: לא רואה מילים בכלל).
     * נקרא מ-ProfileFragment.btnLogout, לצד ניקוי ה-Room הקיים.
     */
    public static void resetSyncThrottle(Context context) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().remove("last_global_sync_ms").apply();
    }

    /**
     * onCleared — נקרא ע"י Android כשה-ViewModel כבר לא נדרש.
     * חייבים להסיר את ה-Firestore listener כאן למניעת Memory Leak:
     * listener מחזיק reference ל-Repository → Repository מחזיק reference ל-WordDao
     * → אם לא מוסרים, GC לא יכול לשחרר את הזיכרון.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        mRepository.stopGlobalDeleteListener();
    }
}
