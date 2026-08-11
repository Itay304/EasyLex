package com.example.easylex.data;

/**
 * =====================================================================
 * WordRepository — מתווך הנתונים (Repository Pattern)
 * =====================================================================
 *
 * מה זה Repository Pattern?
 * --------------------------
 * ה-Repository הוא "שכבת האמצע" בארכיטקטורה MVVM.
 * הוא מסתיר מה-ViewModel ומה-Fragment מאיפה מגיעים הנתונים —
 * Room (מקומי) או Firestore (ענן) — ומספק ממשק אחיד לכולם.
 *
 * ArchitectureFlow:
 *   Fragment → ViewModel → Repository → Room (מקומי)
 *                                     → Firestore (ענן)
 *
 * Threading — ניהול Threads:
 * ---------------------------
 * כל פעולת DB (insert, update, delete, query) מתבצעת על Thread
 * ברקע דרך ExecutorService — כי פעולות DB הן איטיות ויחסמו את ה-UI.
 * ExecutorService עם 4 Threads = מאגר של 4 עובדים מוכנים לפעולה.
 *
 * הכלל החשוב: אסור לגעת ב-DB מה-Main Thread!
 *
 * סנכרון Firestore — הגנה על נתוני תרגול:
 * -----------------------------------------
 * הסנכרון משתמש ב-"insert only" ולא ב-"delete & replace".
 * כך נתוני התרגול (correctAttempts, totalAttempts וכו') לעולם
 * אינם נמחקים ע"י סנכרון — רק מילים חדשות מתווספות.
 * =====================================================================
 */

import android.app.Application;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordRepository {

    /** אוסף הרשימות ב-Firestore ותת-אוסף המילים בכל רשימה — נקרא דינמית, אין שם רשימה קשיח. */
    private static final String LISTS_COLLECTION = "word_lists";
    private static final String WORDS_SUBCOLLECTION = "words";

    private final WordDao mWordDao;

    /** Real-time listeners למחיקות מנהל — אחד לכל רשימה ב-word_lists (נבנה דינמית). */
    private final List<ListenerRegistration> mDeleteListeners = new ArrayList<>();

    /** "ערוץ חי" לרשימת כל המילים — מתעדכן אוטומטית כשיש שינוי ב-DB. */
    private final LiveData<List<Word>> mAllWords;

    /**
     * ExecutorService — מאגר של 4 Threads לפעולות ברקע.
     * static = משותף לכל מופעי ה-Repository (חיסכון במשאבים).
     */
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * קונסטרקטור — מחבר ל-Room ומאתחל את ה-LiveData.
     * @param application נדרש כדי לקבל את מסד הנתונים (לא Activity — בטוח יותר)
     */
    public WordRepository(Application application) {
        WordRoomDatabase db = WordRoomDatabase.getDatabase(application);
        mWordDao  = db.wordDao();
        mAllWords = mWordDao.getAllWords(); // LiveData נוצר פעם אחת ופעיל כל הזמן
    }

    // ── קריאה ────────────────────────────────────────────────────────────────

    /** מחזיר LiveData של כל המילים — Fragment/ViewModel מאזינים לזה. */
    public LiveData<List<Word>> getAllWords() {
        return mAllWords;
    }

    /** מחזיר LiveData של מילים אישיות בלבד (isVerified=false). */
    public LiveData<List<Word>> getPersonalWords() {
        return mWordDao.getPersonalWords();
    }

    // ── כתיבה (מבוצעת על Thread ברקע) ────────────────────────────────────────

    /**
     * הוספת מילה חדשה ל-DB.
     * execute() = שולח את הפעולה ל-Thread ברקע מה-ExecutorService.
     */
    public void insert(Word word) {
        databaseWriteExecutor.execute(() -> mWordDao.insert(word));
    }

    /**
     * עדכון מילה קיימת ב-DB (לפי id).
     * נקרא בכל תשובה נכונה/שגויה בכל מודולי התרגול.
     */
    public void update(Word word) {
        databaseWriteExecutor.execute(() -> mWordDao.update(word));
    }

    /** מחיקת מילה בודדת ב-DB. */
    public void delete(Word word) {
        databaseWriteExecutor.execute(() -> mWordDao.delete(word));
    }

    /** מחיקת כל המילים (Legacy — זהיר בשימוש!). */
    public void deleteAll() {
        databaseWriteExecutor.execute(mWordDao::deleteAllWords);
    }

    /**
     * מחיקת מילים גלובליות ישנות שנשמרו לפני תיקון הבאג.
     * (isVerified=0 AND isFavorite=0 — אינן מילים אישיות ואינן מסומנות)
     */
    public void deleteUnmarkedGlobalWords() {
        databaseWriteExecutor.execute(mWordDao::deleteUnmarkedGlobalWords);
    }

    /**
     * מחיקת כל המילים האישיות של המשתמש (isVerified=false).
     * נקרא מ-Settings → "נקה רשימה אישית".
     */
    public void deletePersonalWords() {
        databaseWriteExecutor.execute(mWordDao::deletePersonalWords);
    }

    /**
     * איפוס נתוני התרגול של כל המילים (correctAttempts, totalAttempts וכו').
     * נקרא מ-Settings → "אפס התקדמות".
     * @param onDone callback ב-UI Thread אחרי סיום (אופציונלי)
     */
    public void resetAllWordProgress(@Nullable Runnable onDone) {
        databaseWriteExecutor.execute(() -> {
            mWordDao.resetAllWordProgress();
            if (onDone != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onDone);
            }
        });
    }

    // ── סנכרון Firestore ──────────────────────────────────────────────────────

    /**
     * fetchListIds — שולפת דינמית את כל מזהי הרשימות תחת word_lists (אין שם רשימה קשיח).
     * בכישלון (למשל אין רשת) — מחזירה רשימה ריקה, כדי שהקריאה לא תיפול.
     */
    private void fetchListIds(ListIdsCallback callback) {
        FirebaseFirestore.getInstance()
                .collection(LISTS_COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ids.add(doc.getId());
                    }
                    callback.onReady(ids);
                })
                .addOnFailureListener(e -> callback.onReady(new ArrayList<>()));
    }

    private interface ListIdsCallback {
        void onReady(List<String> listIds);
    }

    /** מפתח ייחודי (englishWord + sourceListId) לזיהוי מילה בין רשימות שונות. */
    private static String wordKey(String englishWord, String sourceListId) {
        return englishWord + "" + (sourceListId == null ? "" : sourceListId);
    }

    /**
     * syncGlobalWordsFromFirestore — סנכרון דו-כיווני של כל רשימות המילים הגלובליות מהענן.
     *
     * איך עובד הסנכרון (מרובה-רשימות, דינמי):
     * ------------------------------------------
     * שלב 0: שליפת רשימת מזהי הרשימות (fetchListIds) — לא קשיח בקוד, נגזר מ-word_lists.
     * שלב 1: שליפת כל המילים מכל רשימה (אסינכרוני, במקביל)
     * שלב 2 (על Thread ברקע של ExecutorService):
     *   א. בונים לכל רשימה את קבוצת המילים שלה מ-Firestore
     *   ב. שולפים מ-Room את כל המילים הגלובליות הקיימות (englishWord + sourceListId)
     *   ג. מוסיפים מילים חדשות (קיימות ב-Firestore, לא קיימות ב-Room, לפי הרשימה שלהן)
     *   ד. מוחקים מילים שהוסרו מרשימה שהובאה בהצלחה (קיימות ב-Room, לא קיימות עוד
     *      ב-Firestore באותה רשימה) → כך מחיקה של מנהל מ-Firestore נמחקת מכל המכשירים.
     *      רשימה שהשליפה שלה נכשלה (כישלון רשת חלקי) פשוט לא נבדקת בסבב הזה —
     *      כדי לא למחוק בטעות מילים בגלל כישלון זמני.
     *
     * שמירת נתוני תרגול:
     * -------------------
     * מילים קיימות אינן נמחקות ונכתבות מחדש — correctAttempts וכו' נשמרים (insert-only).
     * רק מילים חדשות מתווספות ורק מילים שהוסרו בוודאות מ-Firestore נמחקות.
     *
     * @param onComplete Runnable שיופעל לאחר סיום (גם אם נכשל) — null = לא צריך.
     *                   מופעל על Thread הרקע של הסנכרון (כמו קודם) — קריאה מ-UI
     *                   חייבת לחזור ל-Main Thread בעצמה (ר' שימוש ב-SettingsFragment).
     */
    public void syncGlobalWordsFromFirestore(@Nullable Runnable onComplete) {
        fetchListIds(listIds -> {
            if (listIds.isEmpty()) {
                if (onComplete != null) onComplete.run();
                return;
            }

            List<Task<QuerySnapshot>> tasks = new ArrayList<>();
            for (String listId : listIds) {
                tasks.add(FirebaseFirestore.getInstance()
                        .collection(LISTS_COLLECTION).document(listId)
                        .collection(WORDS_SUBCOLLECTION)
                        .orderBy("englishWord", Query.Direction.ASCENDING)
                        .get());
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener(ignored -> databaseWriteExecutor.execute(() -> {
                // שלב א: בנה, לכל רשימה שנשלפה בהצלחה, את רשימת המילים שלה (מתויגות ב-sourceListId)
                Map<String, List<Word>> wordsByList = new HashMap<>();
                for (int i = 0; i < listIds.size(); i++) {
                    Task<QuerySnapshot> t = tasks.get(i);
                    if (!t.isSuccessful() || t.getResult() == null) continue; // כישלון חלקי — דלג על הרשימה הזו הסבב

                    String listId = listIds.get(i);
                    List<Word> listWords = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : t.getResult()) {
                        Word w = doc.toObject(Word.class);
                        if (w != null && w.getEnglishWord() != null) {
                            w.setSourceListId(listId);
                            listWords.add(w);
                        }
                    }
                    wordsByList.put(listId, listWords);
                }

                // שלב ב: שלוף את כל המילים הגלובליות הקיימות ב-Room, לפי (englishWord, sourceListId)
                List<WordDao.VerifiedWordKey> roomKeys = mWordDao.getVerifiedWordKeys();
                Set<String> existingSet = new HashSet<>();
                for (WordDao.VerifiedWordKey k : roomKeys) {
                    existingSet.add(wordKey(k.englishWord, k.sourceListId));
                }

                // שלב ג: הוסף מילים חדשות שאינן ב-Room עדיין (ברשימה שלהן)
                for (List<Word> listWords : wordsByList.values()) {
                    for (Word w : listWords) {
                        if (!existingSet.contains(wordKey(w.getEnglishWord(), w.getSourceListId()))) {
                            w.setVerified(true);
                            mWordDao.insert(w);
                        }
                    }
                }

                // שלב ד: מחק מילים שהוסרו מרשימה שנשלפה בהצלחה (נמחקו ע"י מנהל)
                for (WordDao.VerifiedWordKey k : roomKeys) {
                    List<Word> currentListWords = wordsByList.get(k.sourceListId);
                    if (currentListWords == null) continue; // הרשימה לא נשלפה בהצלחה הסבב — לא נוגעים בה

                    boolean stillPresent = false;
                    for (Word w : currentListWords) {
                        if (w.getEnglishWord().equals(k.englishWord)) { stillPresent = true; break; }
                    }
                    if (!stillPresent) {
                        mWordDao.deleteVerifiedWordByEnglishAndList(k.englishWord, k.sourceListId);
                    }
                }

                if (onComplete != null) onComplete.run();
            }));
        });
    }

    /**
     * startGlobalDeleteListener — מאזין בזמן אמת למחיקות מנהל, בכל רשימות word_lists.
     *
     * כיצד עובד:
     * -----------
     * מזהי הרשימות נשלפים דינמית (fetchListIds), ומוצמד listener נפרד לכל רשימה —
     * Firestore שולח אירוע REMOVED מיד כשמסמך נמחק, בכל אחת מהרשימות.
     * EXCLUDE = מתעלמים מאירועי cache מקומיים; רק שינויי שרת.
     *
     * אירועים מטופלים:
     * ----------------
     * REMOVED → מחק מ-Room מיד (המילה נעלמת מכל המסכים בזמן אמת)
     * ADDED   → מתעלמים (הסנכרון הרגיל מטפל בהוספות)
     * MODIFIED → מתעלמים (לא מחליפים נתוני תרגול)
     *
     * הערה: רשימה חדשה שנוספה ל-word_lists אחרי startGlobalDeleteListener() לא
     * תקבל listener עד ההפעלה הבאה של המסך — עקבי עם ההתנהגות הקודמת (חד-רשימתית).
     *
     * קוראים ל-stopGlobalDeleteListener() ב-onCleared() של ה-ViewModel.
     */
    public void startGlobalDeleteListener() {
        if (!mDeleteListeners.isEmpty()) return; // כבר מאזין

        fetchListIds(listIds -> {
            for (String listId : listIds) {
                ListenerRegistration reg = FirebaseFirestore.getInstance()
                        .collection(LISTS_COLLECTION).document(listId)
                        .collection(WORDS_SUBCOLLECTION)
                        .addSnapshotListener(MetadataChanges.EXCLUDE, (snapshots, error) -> {
                            if (error != null || snapshots == null) return;

                            databaseWriteExecutor.execute(() -> {
                                for (DocumentChange change : snapshots.getDocumentChanges()) {
                                    if (change.getType() == DocumentChange.Type.REMOVED) {
                                        Word w = change.getDocument().toObject(Word.class);
                                        if (w != null && w.getEnglishWord() != null) {
                                            mWordDao.deleteVerifiedWordByEnglishAndList(w.getEnglishWord(), listId);
                                        }
                                    }
                                }
                            });
                        });
                mDeleteListeners.add(reg);
            }
        });
    }

    /**
     * stopGlobalDeleteListener — מסיר את כל ה-listeners.
     * חובה לקרוא ב-onCleared() של ה-ViewModel למניעת Memory Leak.
     */
    public void stopGlobalDeleteListener() {
        for (ListenerRegistration reg : mDeleteListeners) {
            if (reg != null) reg.remove();
        }
        mDeleteListeners.clear();
    }

    /**
     * replaceAllWords — Legacy: מחליף את כל המילים בבת-אחת.
     * נשמר לצורך תאימות עם קוד ישן — לא בשימוש פעיל.
     * שימוש זה מסוכן: מוחק גם מילים אישיות!
     */
    public void replaceAllWords(List<Word> words) {
        databaseWriteExecutor.execute(() -> {
            mWordDao.deleteAllWords();
            for (Word w : words) mWordDao.insert(w);
        });
    }
}
