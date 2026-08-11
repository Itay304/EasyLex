package com.example.easylex.ui.gamification;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================================
 * StreakManager — מחלקת ניהול הרצף היומי (Daily Streak)
 * =====================================================================
 *
 * מה זה Streak (רצף)?
 * --------------------
 * Streak = מספר הימים הרצופים שבהם המשתמש השתמש באפליקציה.
 * לדוגמה: אם המשתמש נכנס שלושה ימים ברציפות — הרצף הוא 3.
 * אם פספס יום — הרצף מתאפס ל-1.
 *
 * מאיפה המידע?
 * -------------
 * Firestore: users/{uid}.lastActiveDate (מחרוזת "yyyy-MM-dd")
 * Firestore: users/{uid}.streak (מספר)
 *
 * חוקי הרצף:
 * -----------
 *   diff == 0 ימים → אותו יום, הרצף לא משתנה
 *   diff == 1 יום  → יום עוקב, streak++
 *   diff > 1 ימים  → הרצף נשבר, מאפסים ל-1
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * GamificationEngine — קוראת ל-checkAndUpdate() בכל פתיחת אפליקציה.
 */
public class StreakManager {

    public interface StreakCallback {
        void onResult(int newStreak);
    }

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * Reads lastActiveDate from Firestore, computes new streak, writes back, and calls callback.
     *
     * @param uid           Firebase user UID
     * @param currentStreak streak value currently cached in SharedPreferences
     * @param db            Firestore instance
     * @param callback      called on main thread with the updated streak value
     */
    public static void checkAndUpdate(String uid, int currentStreak,
                                      FirebaseFirestore db, StreakCallback callback) {
        String today = DATE_FMT.format(new Date());

        db.collection("users").document(uid).get()
            .addOnSuccessListener((DocumentSnapshot doc) -> {
                String lastDate = doc.exists() ? doc.getString("lastActiveDate") : null;
                int newStreak   = computeStreak(lastDate, currentStreak, today);

                Map<String, Object> updates = new HashMap<>();
                updates.put("streak",         (long) newStreak);
                updates.put("lastActiveDate", today);

                // Update or create the user document
                if (doc.exists()) {
                    db.collection("users").document(uid).update(updates);
                } else {
                    db.collection("users").document(uid).set(updates);
                }

                if (callback != null) callback.onResult(newStreak);
            })
            .addOnFailureListener(e -> {
                // On failure, keep the current streak value
                if (callback != null) callback.onResult(currentStreak);
            });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private static int computeStreak(String lastDateStr, int currentStreak, String today) {
        if (lastDateStr == null || lastDateStr.isEmpty()) {
            return 1;  // first ever activity
        }
        long diffDays = daysDiff(lastDateStr, today);
        if (diffDays == 0) return currentStreak;  // same day
        if (diffDays == 1) return currentStreak + 1;  // consecutive
        return 1;  // broken — reset
    }

    /** Returns absolute difference in days between two yyyy-MM-dd strings. */
    private static long daysDiff(String dateA, String dateB) {
        try {
            Date a = DATE_FMT.parse(dateA);
            Date b = DATE_FMT.parse(dateB);
            if (a == null || b == null) return 0;
            long diffMs = Math.abs(b.getTime() - a.getTime());
            return TimeUnit.MILLISECONDS.toDays(diffMs);
        } catch (ParseException e) {
            return 0;
        }
    }
}
