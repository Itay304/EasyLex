package com.example.easylex.data;

/**
 * =====================================================================
 * Word — מודל הנתונים המרכזי של האפליקציה
 * =====================================================================
 *
 * מה זה?
 * -------
 * Word הוא "תבנית" המייצגת מילה אחת במערכת.
 * כל מה שהאפליקציה יודעת על מילה — שמור כאן:
 * התרגום, הדוגמה, ההתקדמות, הדגלים, ועוד.
 *
 * איפה זה נשמר?
 * --------------
 * @Entity(tableName = "words_table") — Room (SQLite) שומר כל אובייקט Word
 * כ-שורה אחת בטבלה "words_table" במסד הנתונים המקומי.
 * כלומר: כל שדה בקלאס הזה = עמודה אחת בטבלה.
 *
 * שני סוגי מילים:
 * ----------------
 *   • isVerified = true  → מילה גלובלית מה-Curriculum (מגיעה מ-Firestore)
 *   • isVerified = false + isFavorite = true → מילה אישית שהמשתמש הוסיף
 *
 * נוסחת השליטה (Mastery):
 * ------------------------
 * getMasteryLevel() מחשב ציון 0–5 לכל מילה:
 *   M = min(5, floor(correctAttempts / (totalAttempts + 1) × 5))
 * מילה עם M=5 = "נלמדה היטב" — תופיע פחות בתרגול.
 * =====================================================================
 */

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "words_table")
public class Word {

    // ── מפתח ראשי ────────────────────────────────────────────────────────────
    /**
     * מזהה ייחודי אוטומטי לכל מילה.
     * autoGenerate=true → Room מקצה מספר עולה בכל הוספה.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    // ── תוכן המילה ───────────────────────────────────────────────────────────
    /** המילה באנגלית — מה שמוצג בחזית הכרטיסייה ונשאל בשאלון. */
    private String englishWord;

    /** תרגום המילה לעברית — מה שמוצג בגב הכרטיסייה ומשמש כתשובה נכונה בשאלון. */
    private String hebrewTranslation;

    /** חלק הדיבר — Noun / Verb / Adjective וכו'. */
    private String partOfSpeech;

    /** משפט דוגמה באנגלית — מוצג בגב הכרטיסייה. */
    private String exampleSentence;

    /** משפט דוגמה בעברית (אופציונלי). */
    private String hebrewExample;

    /**
     * תגיות קטגוריה — לדוגמה: "Learning & Education", "Daily Life".
     * משמשות לסינון ב"תרגול לפי נושאים".
     */
    private String tags;

    /**
     * מזהה הרשימה המקורית ב-Firestore שממנה הגיעה המילה
     * (מסמך תחת word_lists, למשל "band_2_full_list", "pre_band_1").
     * רלוונטי רק למילים גלובליות (isVerified=true); null עבור מילים אישיות.
     * נוסף כדי לתמוך בכמה רשימות מילים במקביל (ר' syncGlobalWordsFromFirestore).
     */
    private String sourceListId;

    /** חותמת זמן יצירת המילה (מילישניות מ-1970) — לסדר לפי תאריך הוספה. */
    private long creationTimestamp;

    // ── נתוני התקדמות ────────────────────────────────────────────────────────
    /**
     * כמה פעמים המשתמש ענה נכון על מילה זו (בכל המודולים).
     * מתעדכן ב: QuizFragment, SpellingFragment, FlashcardsFragment.
     */
    private int correctAttempts;

    /**
     * כמה פעמים בסך הכל הוצגה המילה למשתמש (נכון + שגוי).
     * correctAttempts / totalAttempts = שיעור הדיוק לחישוב ה-Mastery.
     */
    private int totalAttempts;

    /**
     * כמה פעמים הוקלד שם המילה נכון במודול "אלוף האיות" ספציפית.
     * שדה נפרד — לסטטיסטיקת כתיב בלבד.
     */
    private int spellingCorrect;

    // ── דגלים (flags) ────────────────────────────────────────────────────────
    /**
     * האם המילה היא "מועדפת" / אישית?
     * true = הוספה ע"י המשתמש (OCR או ידנית).
     * משמש גם להבחנה בין מילה אישית לגלובלית כשisVerified=false.
     */
    private boolean isFavorite;

    /**
     * האם המילה אומתה כחלק מהקורס הגלובלי?
     * true  = מגיעה מ-Firestore (הוספה ע"י מנהל המערכת)
     * false = הוספה ע"י המשתמש עצמו
     *
     * כלל חשוב: בסנכרון Firestore — נמחקות ומוחלפות רק מילים עם isVerified=true.
     * מילים אישיות (false) לעולם אינן נמחקות ע"י הסנכרון.
     */
    private boolean isVerified;

    /**
     * דגלי שגיאה — האם המשתמש טעה לאחרונה בכל מודול.
     * משמשים את מסך "תיקון טעויות" (MISTAKES) שמציג רק מילים עם שגיאה אחת לפחות.
     */
    private boolean errorInQuiz      = false; // שגה במבחן
    private boolean errorInSpelling  = false; // שגה בכתיב
    private boolean errorInFlashcards= false; // לא ידע בכרטיסיות (החליק שמאלה)

    // ── קונסטרקטורים ─────────────────────────────────────────────────────────

    /**
     * קונסטרקטור ריק — נדרש ע"י Room ו-Firestore ליצירת אובייקט ריק
     * לפני מילוי השדות (deserialization).
     */
    public Word() {}

    /**
     * קונסטרקטור מלא להוספת מילה חדשה ידנית.
     * @Ignore — Room מתעלם ממנו; משמש רק בקוד Java.
     */
    @Ignore
    public Word(String englishWord, String hebrewTranslation, String partOfSpeech,
                String exampleSentence, String hebrewExample,
                int difficulty, long creationTimestamp) {
        this.englishWord       = englishWord;
        this.hebrewTranslation = hebrewTranslation;
        this.partOfSpeech      = partOfSpeech;
        this.exampleSentence   = exampleSentence;
        this.hebrewExample     = hebrewExample;
        this.creationTimestamp = creationTimestamp;
        this.correctAttempts   = 0; // מילה חדשה — אפס ניסיונות
        this.totalAttempts     = 0;
        this.spellingCorrect   = 0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    // Room דורש getter ו-setter לכל שדה — כך הוא קורא וכותב נתונים לטבלה.

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEnglishWord() { return englishWord; }
    public void setEnglishWord(String englishWord) { this.englishWord = englishWord; }

    public String getHebrewTranslation() { return hebrewTranslation; }
    public void setHebrewTranslation(String hebrewTranslation) { this.hebrewTranslation = hebrewTranslation; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getExampleSentence() { return exampleSentence; }
    public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }

    public String getHebrewExample() { return hebrewExample; }
    public void setHebrewExample(String hebrewExample) { this.hebrewExample = hebrewExample; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getSourceListId() { return sourceListId; }
    public void setSourceListId(String sourceListId) { this.sourceListId = sourceListId; }

    public long getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(long creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    public int getCorrectAttempts() { return correctAttempts; }
    public void setCorrectAttempts(int correctAttempts) { this.correctAttempts = correctAttempts; }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }

    public int getSpellingCorrect() { return spellingCorrect; }
    public void setSpellingCorrect(int spellingCorrect) { this.spellingCorrect = spellingCorrect; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isErrorInQuiz() { return errorInQuiz; }
    public void setErrorInQuiz(boolean errorInQuiz) { this.errorInQuiz = errorInQuiz; }

    public boolean isErrorInSpelling() { return errorInSpelling; }
    public void setErrorInSpelling(boolean errorInSpelling) { this.errorInSpelling = errorInSpelling; }

    public boolean isErrorInFlashcards() { return errorInFlashcards; }
    public void setErrorInFlashcards(boolean errorInFlashcards) { this.errorInFlashcards = errorInFlashcards; }

    // ── לוגיקת שליטה ─────────────────────────────────────────────────────────

    /**
     * getMasteryLevel — מחשב את רמת השליטה במילה זו (0 עד 5).
     *
     * הנוסחה: M = min(5,  floor( correctAttempts / (totalAttempts + 1) × 5 ))
     *
     * דוגמאות:
     *   • 0 ניסיונות כלל      → M = 0  (מילה חדשה)
     *   • 3 נכון מתוך 10      → M = floor(3/11 × 5) = floor(1.36) = 1  (קשה)
     *   • 8 נכון מתוך 10      → M = floor(8/11 × 5) = floor(3.6)  = 3  (טוב)
     *   • 10 נכון מתוך 10     → M = floor(10/11 × 5) = floor(4.5) = 4  (מצוין)
     *
     * ה-+1 במכנה מונע חלוקה באפס כשtotalAttempts=0.
     *
     * שימוש:
     *   • M ≤ 2 → "מילה קשה" (מוצגת ראשונה בתרגול האדפטיבי)
     *   • M = 5 → "נלמדה היטב" (מוצגת אחרונה + בונוס XP)
     */
    public int getMasteryLevel() {
        return Math.min(5, (int) Math.floor((double) correctAttempts / (totalAttempts + 1) * 5));
    }

    /**
     * isMastered — האם המילה "נכבשה" (הגדרה חדשה, שלב 2 — סטטיסטיקות מוסדיות).
     * שונה במכוון מ-getMasteryLevel(): זהו סף בוליאני (כן/לא) לתצוגה
     * (StatisticsFragment, InstitutionalStatsFragment) — לא נוגע בנוסחת
     * ה-0–5 שמניעה את הבחירה האדפטיבית ב-Quiz/Flashcards/Spelling.
     *
     * M = correctAttempts >= 3 AND correctAttempts / totalAttempts >= 0.7
     */
    public boolean isMastered() {
        return correctAttempts >= 3 && totalAttempts > 0
                && (double) correctAttempts / totalAttempts >= 0.7;
    }
}
