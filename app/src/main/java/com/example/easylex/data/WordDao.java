package com.example.easylex.data;

/**
 * =====================================================================
 * WordDao — ממשק הגישה למסד הנתונים (Data Access Object)
 * =====================================================================
 *
 * מה זה DAO?
 * -----------
 * DAO = Data Access Object. זהו הממשק שמגדיר את כל פעולות
 * הקריאה והכתיבה אל מסד הנתונים המקומי (Room / SQLite).
 * Room מייצר את הקוד בפועל בזמן קומפילציה — אנחנו רק מגדירים
 * מה אנחנו רוצים לעשות, ו-Room יודע איך לתרגם זאת ל-SQL.
 *
 * ארכיטקטורה:
 * -----------
 * Fragment / ViewModel → WordRepository → WordDao → Room (SQLite)
 *
 * חשוב: כל פעולות ה-DAO מבוצעות על Thread ברקע (ExecutorService)
 * ולא על ה-Main Thread — כי פעולות DB הן איטיות ויחסמו את ה-UI.
 * החריג: LiveData<> — Room מחזיר תוצאה אסינכרונית שמתעדכנת אוטומטית.
 * =====================================================================
 */

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WordDao {

    // ── פעולות בסיסיות ───────────────────────────────────────────────────────

    /**
     * הוספת מילה חדשה לטבלה.
     * OnConflictStrategy.IGNORE = אם המילה כבר קיימת עם אותו id — מתעלמים ולא מוסיפים.
     * (מונע כפילויות בסנכרון Firestore)
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Word word);

    /**
     * עדכון מילה קיימת — Room מזהה את המילה לפי ה-id שלה ומעדכן את כל שאר השדות.
     * נקרא בכל פעם שמשתמש עונה על שאלה (correctAttempts++, totalAttempts++ וכו').
     */
    @Update
    void update(Word word);

    /**
     * מחיקת מילה בודדת לפי ה-id שלה.
     * נקרא רק מה-Admin fragment למחיקת מילה ספציפית.
     */
    @Delete
    void delete(Word word);

    // ── שאילתות קריאה ────────────────────────────────────────────────────────

    /**
     * מחזיר את כל המילים בטבלה, ממוינות לפי תאריך הוספה (החדשות ראשון).
     * LiveData = "ערוץ חי" — כל שינוי בטבלה מעדכן אוטומטית את כל ה-Observers
     * (למשל, RecyclerView ברשימת המילים יתרענן אוטומטית).
     */
    @Query("SELECT * FROM words_table ORDER BY creationTimestamp DESC")
    LiveData<List<Word>> getAllWords();

    /**
     * מחזיר רק מילים אישיות של המשתמש (isVerified = 0 = false).
     * "0" ב-SQLite = false; "1" = true.
     * נקרא מ-MyWordsFragment כשמסנן לתצוגת "המילים שלי".
     */
    @Query("SELECT * FROM words_table WHERE isVerified = 0 ORDER BY creationTimestamp DESC")
    LiveData<List<Word>> getPersonalWords();

    /**
     * VerifiedWordKey — זיהוי מילה גלובלית לפי (englishWord, sourceListId).
     * דרוש כי אותה מילה (englishWord) יכולה להופיע במספר רשימות שונות עם
     * תרגומים שונים — הזיהוי הכפול מונע דילוג/מחיקה שגויים בין רשימות.
     */
    class VerifiedWordKey {
        public String englishWord;
        public String sourceListId;
    }

    /**
     * מחזיר (englishWord, sourceListId) של כל המילים הגלובליות.
     * שימוש: לפני סנכרון Firestore — בונים Set מהרשימה הזו
     * כדי לדעת אילו מילים כבר קיימות בכל רשימה ולא להוסיף כפילויות.
     * (List רגיל — לא LiveData — כי זו שאילתה חד-פעמית ברקע)
     */
    @Query("SELECT englishWord, sourceListId FROM words_table WHERE isVerified = 1")
    List<VerifiedWordKey> getVerifiedWordKeys();

    // ── פעולות מחיקה ─────────────────────────────────────────────────────────

    /**
     * מחיקת מילים גלובליות ישנות שנשמרו לפני תיקון הבאג (לפני הוספת isVerified).
     * מזוהות לפי: isVerified=0 AND isFavorite=0
     * (מילים אישיות תמיד יש להן isFavorite=1, לכן הן בטוחות)
     * נקרא פעם אחת בלבד כ-Migration (ראה MyWordsViewModel).
     */
    @Query("DELETE FROM words_table WHERE isVerified = 0 AND isFavorite = 0")
    void deleteUnmarkedGlobalWords();

    /**
     * מחיקת כל המילים האישיות של המשתמש (isVerified=0).
     * נקרא מ-Settings כשהמשתמש לוחץ "נקה רשימה אישית".
     * מילים גלובליות (isVerified=1) לא נמחקות.
     */
    @Query("DELETE FROM words_table WHERE isVerified = 0")
    void deletePersonalWords();

    /**
     * מחיקת מילה גלובלית ספציפית לפי שמה האנגלי + רשימת המקור שלה.
     * נקרא בסנכרון Firestore — כשמילה נמחקה מרשימה ב-Firestore ע"י מנהל
     * ויש למחוק אותה גם מ-Room כדי שתיעלם מכל המכשירים.
     * הסינון לפי sourceListId מונע מחיקה שגויה כשאותה מילה קיימת גם ברשימה אחרת.
     */
    @Query("DELETE FROM words_table WHERE englishWord = :englishWord " +
           "AND sourceListId = :sourceListId AND isVerified = 1")
    void deleteVerifiedWordByEnglishAndList(String englishWord, String sourceListId);

    /**
     * מחיקת כל המילים הגלובליות (isVerified=1).
     * שמור בקוד לצורך שימוש עתידי — כרגע לא בשימוש פעיל
     * מכיוון שהסנכרון שונה ל-"insert only" (ראה WordRepository).
     */
    @Query("DELETE FROM words_table WHERE isVerified = 1")
    void deleteVerifiedWords();

    /**
     * מחיקת כל המילים בטבלה — גלובליות ואישיות כאחד.
     * פעולה קיצונית — משמשת רק ב-replaceAllWords() ב-Repository (Legacy).
     */
    @Query("DELETE FROM words_table")
    void deleteAllWords();

    /**
     * איפוס נתוני התרגול של כל המילים — correctAttempts, totalAttempts,
     * spellingCorrect, ודגלי השגיאה.
     * נקרא מ-SettingsFragment כשהמשתמש לוחץ "אפס התקדמות".
     * המילים עצמן (תוכן) נשמרות — רק ההתקדמות מאופסת.
     */
    @Query("UPDATE words_table SET " +
           "correctAttempts = 0, totalAttempts = 0, spellingCorrect = 0, " +
           "errorInQuiz = 0, errorInSpelling = 0, errorInFlashcards = 0")
    void resetAllWordProgress();
}
