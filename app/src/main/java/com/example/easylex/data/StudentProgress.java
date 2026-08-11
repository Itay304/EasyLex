package com.example.easylex.data;

/**
 * StudentProgress — מודל קריאה-בלבד לשורת תלמיד בלשונית "תלמידים" של
 * ClassDetailFragment (מעקב התקדמות תלמידים בכיתה, שלב 1 — דשבורד מורה).
 * נוצר אך ורק מתוצאת getClassProgress (Cloud Function, functions/index.js) —
 * לא נקרא ישירות מ-Firestore בצד לקוח (מורה לא אמור לגשת ל-progress גולמי
 * של תלמידים אחרים ישירות, ר' הערה ב-firestore.rules).
 */

import androidx.annotation.Nullable;

import java.util.Map;

public class StudentProgress {

    private final String uid;
    private final String displayName;
    private final long totalXp;
    @Nullable
    private final Long lastActiveDateMs;
    private final int masteredWords;
    private final boolean weeklyActivity;

    private StudentProgress(String uid, String displayName, long totalXp,
                             @Nullable Long lastActiveDateMs, int masteredWords, boolean weeklyActivity) {
        this.uid = uid;
        this.displayName = displayName;
        this.totalXp = totalXp;
        this.lastActiveDateMs = lastActiveDateMs;
        this.masteredWords = masteredWords;
        this.weeklyActivity = weeklyActivity;
    }

    public static StudentProgress fromMap(Map<String, Object> map) {
        Object uidObj = map.get("uid");
        Object xpObj = map.get("totalXp");
        Object lastActiveObj = map.get("lastActiveDate");
        Object masteredObj = map.get("masteredWords");
        Object weeklyObj = map.get("weeklyActivity");

        return new StudentProgress(
                uidObj != null ? uidObj.toString() : "",
                (String) map.get("displayName"),
                xpObj instanceof Number ? ((Number) xpObj).longValue() : 0,
                lastActiveObj instanceof Number ? ((Number) lastActiveObj).longValue() : null,
                masteredObj instanceof Number ? ((Number) masteredObj).intValue() : 0,
                Boolean.TRUE.equals(weeklyObj)
        );
    }

    public String getUid() { return uid; }
    @Nullable public String getDisplayName() { return displayName; }
    public long getTotalXp() { return totalXp; }
    @Nullable public Long getLastActiveDateMs() { return lastActiveDateMs; }
    public int getMasteredWords() { return masteredWords; }
    public boolean isWeeklyActivity() { return weeklyActivity; }
}
