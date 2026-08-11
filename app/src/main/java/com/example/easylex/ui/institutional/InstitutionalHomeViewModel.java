package com.example.easylex.ui.institutional;

/**
 * =====================================================================
 * InstitutionalHomeViewModel — מסך בית לתלמיד מוסדי (שינוי ארכיטקטוני, שלב 2)
 * =====================================================================
 *
 * שלושה מקורות נתונים עצמאיים:
 *   1. הודעה מהמורה — קריאה חד-פעמית, ממוינת לפי createdAt desc, limit 1.
 *   2. טבלת מובילים — Real-time listener על users/ בתוך אותו מוסד, ממוין
 *      לפי totalXp desc, limit 10. אם התלמיד לא בעשירייה — מצורפת שורה
 *      נוספת עבורו עם הדירוג האמיתי שלו (via count() aggregation query).
 *   3. משימות פעילות (עד 3) — *לא* מיושם כאן. ר' InstitutionalHomeFragment:
 *      נעשה שימוש חוזר ישיר ב-StudentAssignmentsViewModel הקיים (משימה 0.15)
 *      במקום לשכפל את לוגיקת הטעינה/חישוב ההתקדמות.
 *
 * שם תצוגה בטבלת המובילים ("יוסי כ'"):
 * -------------------------------------
 * displayName על המסמך הוא עדיין טקסט חופשי (SettingsFragment) — פורמט
 * "שם פרטי + אות משם משפחה" (איפיון גרסה 1.1) מיושם כאן כתצוגה בלבד,
 * ב-LeaderboardAdapter, בלי לשנות את המודל/את SettingsFragment (מחוץ לתחום).
 */

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.easylex.data.UserRoleManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class InstitutionalHomeViewModel extends AndroidViewModel {

    private static final int LEADERBOARD_LIMIT = 10;
    /** הודעה ישנה יותר מ-7 ימים — לא מוצגת (שלב 2, "הודעות מורה לכיתה"). */
    private static final long ANNOUNCEMENT_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    /** שורה בטבלת המובילים — נבנית כאן, מוצגת ב-LeaderboardAdapter. */
    public static class LeaderboardRow {
        public final int rank;
        public final String displayName;
        public final long totalXp;
        public final boolean isCurrentUser;

        LeaderboardRow(int rank, String displayName, long totalXp, boolean isCurrentUser) {
            this.rank = rank;
            this.displayName = displayName;
            this.totalXp = totalXp;
            this.isCurrentUser = isCurrentUser;
        }
    }

    /** הודעה מהמורה — עטיפה כדי להבדיל "עוד לא נטען" מ"אין הודעות בכלל". */
    public static class Announcement {
        public final String id;
        public final String message;
        @Nullable public final String createdByName;
        public final long createdAtMs;

        Announcement(String id, String message, @Nullable String createdByName, long createdAtMs) {
            this.id = id;
            this.message = message;
            this.createdByName = createdByName;
            this.createdAtMs = createdAtMs;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<LeaderboardRow>> leaderboard = new MutableLiveData<>();
    private final MutableLiveData<Announcement> announcement = new MutableLiveData<>();

    @Nullable
    private ListenerRegistration leaderboardListener;

    public InstitutionalHomeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<LeaderboardRow>> getLeaderboard() { return leaderboard; }
    public LiveData<Announcement> getAnnouncement() { return announcement; }

    // ── הודעה מהמורה ─────────────────────────────────────────────────────────

    /**
     * נקרא פעם אחת מה-Fragment ב-onResume. תלמיד ללא כיתה — פשוט לא מציג הודעה.
     * הודעה ישנה יותר מ-ANNOUNCEMENT_MAX_AGE_MS (7 ימים) — לא מוצגת בכלל
     * (לא נקבעת ל-announcement LiveData, אז renderAnnouncement רואה null
     * ומסתיר את ה-banner, בדיוק כמו "אין הודעות בכלל").
     */
    public void loadLatestAnnouncement() {
        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        List<String> classIds = UserRoleManager.getInstance().getClassIds();
        if (institutionId == null || classIds.isEmpty()) return;

        // תלמיד יכול להיות בכמה כיתות (classIds[]) — לוקחים את הראשונה כרגע.
        // מיזוג הודעות מכמה כיתות במקביל מחוץ לתחום המשימה הזו.
        String classId = classIds.get(0);

        db.collection("institutions").document(institutionId)
                .collection("classes").document(classId)
                .collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String message = doc.getString("message");
                    com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                    if (message == null || message.isEmpty() || createdAt == null) return;

                    long createdAtMs = createdAt.toDate().getTime();
                    long ageMs = System.currentTimeMillis() - createdAtMs;
                    if (ageMs > ANNOUNCEMENT_MAX_AGE_MS) return; // ישנה מדי — לא מוצגת

                    String createdByName = doc.getString("createdByName");
                    announcement.setValue(new Announcement(doc.getId(), message, createdByName, createdAtMs));
                });
        // כישלון (למשל אין הרשאה/רשת) — פשוט לא מציגים באנר, בלי הודעת שגיאה.
    }

    // ── טבלת מובילים ─────────────────────────────────────────────────────────

    /** מתחיל להאזין לטבלת המובילים בזמן אמת. נקרא מ-onResume. */
    public void startLeaderboardListener() {
        if (leaderboardListener != null) return; // כבר מאזין

        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        if (institutionId == null) return;

        leaderboardListener = db.collection("users")
                .whereEqualTo("institutionId", institutionId)
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(LEADERBOARD_LIMIT)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null) return;
                    onLeaderboardSnapshot(snap, institutionId);
                });
    }

    /** מפסיק להאזין — חובה לקרוא ב-onDestroyView כדי למנוע דליפת זיכרון. */
    public void stopLeaderboardListener() {
        if (leaderboardListener != null) {
            leaderboardListener.remove();
            leaderboardListener = null;
        }
    }

    private void onLeaderboardSnapshot(QuerySnapshot snap, String institutionId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = user != null ? user.getUid() : null;

        List<LeaderboardRow> rows = new ArrayList<>();
        boolean currentUserInTop = false;
        int rank = 0;
        for (DocumentSnapshot doc : snap.getDocuments()) {
            rank++;
            boolean isMe = doc.getId().equals(myUid);
            if (isMe) currentUserInTop = true;
            rows.add(new LeaderboardRow(rank, formatName(doc.getString("displayName")),
                    readXp(doc), isMe));
        }

        if (myUid == null || currentUserInTop) {
            leaderboard.setValue(rows);
            return;
        }

        // התלמיד לא בעשירייה — מוסיפים את השורה שלו בסוף, עם הדירוג האמיתי
        // (via count() aggregation: כמה תלמידים במוסד עם totalXp גבוה יותר, +1).
        db.collection("users").document(myUid).get()
                .addOnSuccessListener(myDoc -> {
                    if (!myDoc.exists()) {
                        leaderboard.setValue(rows);
                        return;
                    }
                    long myXp = readXp(myDoc);
                    db.collection("users")
                            .whereEqualTo("institutionId", institutionId)
                            .whereGreaterThan("totalXp", myXp)
                            .count()
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener(agg -> {
                                List<LeaderboardRow> withMe = new ArrayList<>(rows);
                                int myRank = (int) agg.getCount() + 1;
                                withMe.add(new LeaderboardRow(myRank,
                                        formatName(myDoc.getString("displayName")), myXp, true));
                                leaderboard.setValue(withMe);
                            })
                            .addOnFailureListener(e -> leaderboard.setValue(rows));
                })
                .addOnFailureListener(e -> leaderboard.setValue(rows));
    }

    private static long readXp(DocumentSnapshot doc) {
        Long xp = doc.getLong("totalXp");
        return xp != null ? xp : 0;
    }

    /**
     * formatName — "שם פרטי + אות משם משפחה" (איפיון גרסה 1.1, "יוסי כ'").
     * displayName עדיין טקסט חופשי במקור (SettingsFragment) — זו רק תצוגה.
     */
    public static String formatName(@Nullable String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) return "משתמש";
        String[] parts = rawName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[1].substring(0, 1) + "'";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopLeaderboardListener();
    }
}
