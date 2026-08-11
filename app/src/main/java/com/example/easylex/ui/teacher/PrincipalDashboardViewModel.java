package com.example.easylex.ui.teacher;

/**
 * =====================================================================
 * PrincipalDashboardViewModel — דשבורד מינימלי למנהל מוסד (שלב 2)
 * =====================================================================
 *
 * שני מקורות נתונים עצמאיים:
 *   1. getPrincipalStats (Cloud Function) — 4 ספירות ברמת המוסד כולו.
 *   2. institutions/{instId}/classes — *כל* כיתות המוסד (לא מסונן לפי
 *      teacherId, בניגוד ל-TeacherDashboardViewModel.loadClasses) — משתמש
 *      חוזר מלא ב-SchoolClass/ClassAdapter הקיימים.
 */

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.easylex.data.SchoolClass;
import com.example.easylex.data.UserRoleManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrincipalDashboardViewModel extends AndroidViewModel {

    /** ה-region שבו נפרסה getPrincipalStats (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";

    /** 4 כרטיסי הסיכום בראש המסך. */
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

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<PrincipalStats> stats = new MutableLiveData<>();
    private final MutableLiveData<List<SchoolClass>> allClasses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public PrincipalDashboardViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<PrincipalStats> getStats() { return stats; }
    public LiveData<List<SchoolClass>> getAllClasses() { return allClasses; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    /** טוען הכל מחדש — נקרא מ-onResume וגם ברענון ידני. */
    public void loadAll() {
        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        if (institutionId == null) {
            toastMessage.setValue("החשבון שלך אינו משויך למוסד.");
            allClasses.setValue(new ArrayList<>());
            return;
        }

        loadStats();
        loadAllClasses(institutionId);
    }

    private void loadStats() {
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("getPrincipalStats")
                .call()
                .addOnSuccessListener(result -> {
                    Object data = result.getData();
                    if (!(data instanceof Map)) return;
                    Map<?, ?> map = (Map<?, ?>) data;
                    stats.setValue(new PrincipalStats(
                            intValue(map.get("teacherCount")),
                            intValue(map.get("classCount")),
                            intValue(map.get("activeStudentsThisWeek")),
                            intValue(map.get("activeAssignmentCount"))
                    ));
                })
                .addOnFailureListener(e -> toastMessage.setValue(describeError(e)));
    }

    private static int intValue(Object raw) {
        return raw instanceof Number ? ((Number) raw).intValue() : 0;
    }

    /** כל כיתות המוסד (לא רק כיתות המורה המחובר) — ר' תיעוד למעלה. */
    private void loadAllClasses(String institutionId) {
        isLoading.setValue(true);
        db.collection("institutions").document(institutionId)
                .collection("classes")
                .whereEqualTo("archived", false)
                .get()
                .addOnSuccessListener(snap -> {
                    List<SchoolClass> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        list.add(SchoolClass.fromDocument(d));
                    }
                    allClasses.setValue(list);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("טעינת כיתות המוסד נכשלה.");
                    isLoading.setValue(false);
                });
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            switch (code) {
                case PERMISSION_DENIED:
                    return "אין לך הרשאה לצפות בנתוני המוסד.";
                case FAILED_PRECONDITION:
                    return "החשבון שלך אינו משויך למוסד.";
                case UNAUTHENTICATED:
                    return "יש להתחבר מחדש.";
                default:
                    return "טעינת נתוני המוסד נכשלה.";
            }
        }
        return "טעינת נתוני המוסד נכשלה.";
    }
}
