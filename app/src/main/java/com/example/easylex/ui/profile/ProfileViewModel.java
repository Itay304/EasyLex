package com.example.easylex.ui.profile;

/**
 * =====================================================================
 * ProfileViewModel — נתוני-עזר תלויי-role למסך הפרופיל (שלב 2, חלק 4)
 * =====================================================================
 *
 * ProfileFragment עצמו לא נמחק/נכתב מחדש — רק הוסיף תנאים לפי role
 * (ר' ProfileFragment.java). כל נתון שדורש קריאת Firestore נוספת (מעבר
 * לפרופיל הבסיסי הקיים) מרוכז כאן, כדי לא להעמיס לוגיקת רשת על ה-Fragment.
 *
 * תלמיד מוסדי — דירוג בטבלת המובילים: אותה שיטת חישוב בדיוק כמו
 * InstitutionalHomeViewModel (count() aggregate על totalXp גבוה יותר),
 * רק כשליפה חד-פעמית (לא real-time listener — הפרופיל לא זקוק לזה).
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.List;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {

    /** ה-region שבו נפרסה getPrincipalStats (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";

    /** תלמיד מוסדי — שם הכיתה + דירוג בטבלת המובילים (null/-1 = עוד לא ידוע). */
    public static class StudentExtras {
        @Nullable public final String className;
        public final int leaderboardRank;
        StudentExtras(@Nullable String className, int leaderboardRank) {
            this.className = className;
            this.leaderboardRank = leaderboardRank;
        }
    }

    /** מנהל מוסד בלבד — אותה סכמה בדיוק כמו PrincipalDashboardViewModel.PrincipalStats
     *  (לא משותפת בין החבילות, כדי לא ליצור תלות ui.profile↔ui.teacher על כלי-עזר קטן כזה). */
    public static class PrincipalStats {
        public final int teacherCount;
        public final int classCount;
        public final int activeStudentsThisWeek;
        public final int activeAssignmentCount;
        PrincipalStats(int teacherCount, int classCount, int activeStudentsThisWeek, int activeAssignmentCount) {
            this.teacherCount = teacherCount;
            this.classCount = classCount;
            this.activeStudentsThisWeek = activeStudentsThisWeek;
            this.activeAssignmentCount = activeAssignmentCount;
        }
    }

    private final MutableLiveData<StudentExtras> studentExtras = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeAssignmentCount = new MutableLiveData<>();
    private final MutableLiveData<PrincipalStats> principalStats = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) { super(application); }

    public LiveData<StudentExtras> getStudentExtras() { return studentExtras; }
    public LiveData<Integer> getActiveAssignmentCount() { return activeAssignmentCount; }
    public LiveData<PrincipalStats> getPrincipalStats() { return principalStats; }

    // ── תלמיד מוסדי ──────────────────────────────────────────────────────────

    public void loadStudentExtras() {
        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        List<String> classIds = UserRoleManager.getInstance().getClassIds();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (institutionId == null || user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(myDoc -> {
                    Long xp = myDoc.getLong("totalXp");
                    long myXp = xp != null ? xp : 0;

                    db.collection("users")
                            .whereEqualTo("institutionId", institutionId)
                            .whereGreaterThan("totalXp", myXp)
                            .count().get(AggregateSource.SERVER)
                            .addOnSuccessListener(agg -> {
                                int rank = (int) agg.getCount() + 1;
                                if (classIds.isEmpty()) {
                                    studentExtras.setValue(new StudentExtras(null, rank));
                                } else {
                                    db.collection("institutions").document(institutionId)
                                            .collection("classes").document(classIds.get(0)).get()
                                            .addOnSuccessListener(classDoc ->
                                                    studentExtras.setValue(
                                                            new StudentExtras(classDoc.getString("name"), rank)))
                                            .addOnFailureListener(e ->
                                                    studentExtras.setValue(new StudentExtras(null, rank)));
                                }
                            })
                            .addOnFailureListener(e -> studentExtras.setValue(new StudentExtras(null, -1)));
                });
    }

    // ── מורה/מנהל ────────────────────────────────────────────────────────────

    /** כמה משימות פעילות המורה המחובר יצר (institutions/{instId}/assignments, createdBy=uid, status=active). */
    public void loadTeacherActiveAssignmentCount() {
        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (institutionId == null || user == null) return;

        FirebaseFirestore.getInstance()
                .collection("institutions").document(institutionId).collection("assignments")
                .whereEqualTo("createdBy", user.getUid())
                .whereEqualTo("status", "active")
                .count().get(AggregateSource.SERVER)
                .addOnSuccessListener(agg -> activeAssignmentCount.setValue((int) agg.getCount()))
                .addOnFailureListener(e -> activeAssignmentCount.setValue(0));
    }

    /** נתוני מוסד שלם (מורים/כיתות/תלמידים פעילים/משימות) — getPrincipalStats, מנהל בלבד. */
    public void loadPrincipalStats() {
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("getPrincipalStats")
                .call()
                .addOnSuccessListener(result -> {
                    Object data = result.getData();
                    if (!(data instanceof Map)) return;
                    Map<?, ?> map = (Map<?, ?>) data;
                    principalStats.setValue(new PrincipalStats(
                            intValue(map.get("teacherCount")),
                            intValue(map.get("classCount")),
                            intValue(map.get("activeStudentsThisWeek")),
                            intValue(map.get("activeAssignmentCount"))
                    ));
                });
    }

    private static int intValue(Object raw) {
        return raw instanceof Number ? ((Number) raw).intValue() : 0;
    }
}
