package com.example.easylex.data;

/**
 * =====================================================================
 * ProgressSyncManager — כתיבת progress לענן פר-סשן (משימה 0.11)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * Room הוא ותמיד יישאר מקור האמת במכשיר — correctAttempts/totalAttempts
 * מתעדכנים שם בכל תשובה, כרגיל, בלי שום שינוי. המחלקה הזו רק *מוסיפה*
 * כתיבה מקבילה לענן, אחרי שסשן תרגול שלם הסתיים (לא פר-תשובה — פר-סשן,
 * batch אחד), כדי שמורה/מנהל יוכלו לראות התקדמות תלמיד ב-Firestore.
 *
 * מבנה היעד ב-Firestore:
 *   users/{uid}/progress/{englishWord}
 *     englishWord, sourceListId, correctAttempts, totalAttempts, lastPracticed
 *
 * מתי לא כותבים בכלל (יציאה שקטה, בלי טוסט/שגיאה):
 * ---------------------------------------------------
 *   • אין משתמש מחובר (התנתק/מצב אורח).
 *   • המשתמש עצמאי — אין לו institutionId (UserRoleManager, נטען כבר
 *     ב-MainActivity לפני שהמשתמש בכלל מגיע למודול תרגול). תלמיד עצמאי
 *     לא צריך שאף אחד יראה את ההתקדמות שלו בענן.
 *   • רשימת המילים של הסשן ריקה.
 *
 * Fire-and-forget:
 * -----------------
 * batch.commit() לא חוסם, ואין addOnFailureListener שמציג הודעה —
 * כישלון רשת פשוט אומר שהפעם לא הגיע עדכון לענן; Room המקומי כבר שמור
 * ותקין בכל מקרה, אז אין צורך ב-retry/queue.
 * =====================================================================
 */

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressSyncManager {

    private ProgressSyncManager() { } // מחלקת עזר סטטית בלבד — אין צורך במופע

    /**
     * syncSession — נקראת מנקודת סיום הסשן בכל אחד משלושת מודולי התרגול
     * (QuizFragment.finishQuiz, FlashcardsFragment.showFlashResult,
     * SpellingFragment.finish) עם רשימת המילים שתורגלו בסשן זה.
     *
     * @param sessionWords מילים שתורגלו בסשן — Room כבר עודכן עבורן
     *                     (viewModel.update() נקרא לכל מילה תוך כדי הסשן);
     *                     כאן רק קוראים את הערכים הנוכחיים וכותבים אותם לענן.
     */
    public static void syncSession(@Nullable List<Word> sessionWords) {
        if (sessionWords == null || sessionWords.isEmpty()) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        if (institutionId == null) return; // תלמיד עצמאי — לא כותבים progress לענן

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CollectionReference progressRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("progress");

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        boolean hasWrites = false;

        for (Word w : sessionWords) {
            if (w == null || w.getEnglishWord() == null || w.getEnglishWord().isEmpty()) continue;

            Map<String, Object> data = new HashMap<>();
            data.put("englishWord", w.getEnglishWord()); // ערך מקורי, לא מסונן — רק ה-ID למסמך מסונן
            data.put("sourceListId", w.getSourceListId());
            data.put("correctAttempts", w.getCorrectAttempts());
            data.put("totalAttempts", w.getTotalAttempts());
            data.put("lastPracticed", FieldValue.serverTimestamp());

            batch.set(progressRef.document(sanitizeDocumentId(w.getEnglishWord())), data);
            hasWrites = true;
        }

        if (hasWrites) {
            batch.commit(); // fire-and-forget — בכוונה בלי listener על הצלחה/כישלון
        }
    }

    /**
     * sanitizeDocumentId — מונע קריסה (docs/error): englishWord כמו "a/an" מכיל "/",
     * שב-Firestore הוא מפריד path בתוך ID של מסמך ("progress/a" + "/an" = 5
     * מקטעים במקום 4 → IllegalArgumentException). מחליף ל-"-", שלא פוגע בזיהוי
     * המילה (הערך המקורי, לא המסונן, עדיין נשמר בשדה englishWord של המסמך עצמו).
     */
    private static String sanitizeDocumentId(String englishWord) {
        return englishWord.replace("/", "-");
    }
}
