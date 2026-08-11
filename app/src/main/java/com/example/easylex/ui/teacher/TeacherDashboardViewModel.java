package com.example.easylex.ui.teacher;

/**
 * =====================================================================
 * TeacherDashboardViewModel — משימה 0.9 (שלב 0ב, איפיון הפלטפורמה)
 * =====================================================================
 *
 * טוען את שם המוסד ואת הכיתות של המורה המחובר, ומאפשר יצירת כיתה חדשה
 * דרך Cloud Function (createClass, functions/index.js). ללא Room — נתונים
 * מוסדיים הם online-only (ר' SchoolClass.java).
 */

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.easylex.data.SchoolClass;
import com.example.easylex.data.UserRoleManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherDashboardViewModel extends AndroidViewModel {

    /** ה-region שבו נפרסה createClass (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<String> institutionName = new MutableLiveData<>();
    private final MutableLiveData<List<SchoolClass>> classes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    @androidx.annotation.Nullable
    private String institutionId;

    public TeacherDashboardViewModel(@NonNull Application application) {
        super(application);
        loadDashboard();
    }

    public LiveData<String> getInstitutionName() { return institutionName; }
    public LiveData<List<SchoolClass>> getClasses() { return classes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    private void loadDashboard() {
        institutionId = UserRoleManager.getInstance().getInstitutionId();
        if (institutionId == null) {
            toastMessage.setValue("החשבון שלך אינו משויך למוסד.");
            classes.setValue(new ArrayList<>());
            return;
        }

        db.collection("institutions").document(institutionId).get()
                .addOnSuccessListener(doc -> institutionName.setValue(doc.getString("name")))
                .addOnFailureListener(e -> toastMessage.setValue("טעינת שם המוסד נכשלה."));

        loadClasses();
    }

    /** רענון רשימת הכיתות — נקרא גם אחרי יצירת כיתה חדשה. */
    public void loadClasses() {
        if (institutionId == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        isLoading.setValue(true);
        db.collection("institutions").document(institutionId)
                .collection("classes")
                .whereEqualTo("teacherId", user.getUid())
                .whereEqualTo("archived", false)
                .get()
                .addOnSuccessListener(snap -> {
                    List<SchoolClass> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        list.add(SchoolClass.fromDocument(d));
                    }
                    classes.setValue(list);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("טעינת הכיתות נכשלה.");
                    isLoading.setValue(false);
                });
    }

    /** יוצר כיתה חדשה דרך createClass (Cloud Function), ומרענן את הרשימה בהצלחה. */
    public void createClass(String className, String grade) {
        isLoading.setValue(true);

        Map<String, Object> data = new HashMap<>();
        data.put("className", className);
        data.put("grade", grade);

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("createClass")
                .call(data)
                .addOnSuccessListener(result -> {
                    toastMessage.setValue("הכיתה נוצרה בהצלחה!");
                    loadClasses(); // isLoading יתאפס כשהטעינה תסתיים
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue(describeError(e));
                });
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            switch (code) {
                case PERMISSION_DENIED:
                    return "אין לך הרשאה ליצור כיתה.";
                case FAILED_PRECONDITION:
                    return "החשבון שלך אינו משויך למוסד.";
                case INVALID_ARGUMENT:
                    return "חסר שם כיתה.";
                case UNAUTHENTICATED:
                    return "יש להתחבר מחדש.";
                default:
                    return "אירעה שגיאה. נסה שוב.";
            }
        }
        return "אירעה שגיאה. נסה שוב.";
    }
}
