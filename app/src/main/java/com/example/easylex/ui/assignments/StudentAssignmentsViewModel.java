package com.example.easylex.ui.assignments;

/**
 * =====================================================================
 * StudentAssignmentsViewModel — משימה 0.15 (שלב 2, מערכת משימות — צד תלמיד)
 * =====================================================================
 *
 * טוען את משימות התלמיד (getMyAssignments, Cloud Function) ומצרף לכל אחת
 * פס התקדמות: כמה מהמילים שלה כבר יש לתלמיד ב-users/{uid}/progress עם
 * correctAttempts > 0 (ר' 0.11 — הכתיבה לשם). progress נשלף פעם אחת
 * לכל טעינה (לא לכל משימה בנפרד), וממופה לפי sourceListId.
 *
 * wordIds ריק במשימה (מצב "כל הרשימה", היחיד הנתמך כרגע — ר' 0.14) →
 * ה"סך הכל" של המשימה הוא wordCount של הרשימה כולה (word_lists/{listId}).
 */

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.easylex.data.Assignment;
import com.example.easylex.data.UserRoleManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentAssignmentsViewModel extends AndroidViewModel {

    /** ה-region שבו נפרסה getMyAssignments (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";

    /** משימה + פס התקדמות מחושב — מה שה-Fragment/Adapter בפועל מציגים. */
    public static class AssignmentWithProgress {
        public final Assignment assignment;
        public final int completed;
        public final int total;

        AssignmentWithProgress(Assignment assignment, int completed, int total) {
            this.assignment = assignment;
            this.completed = completed;
            this.total = total;
        }
    }

    private final MutableLiveData<List<AssignmentWithProgress>> assignments = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public StudentAssignmentsViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<AssignmentWithProgress>> getAssignments() { return assignments; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    /**
     * true אם למשתמש יש institutionId מקומי (UserRoleManager, כבר נטען
     * ב-MainActivity לפני שהתלמיד בכלל מגיע למסך הזה). תלמיד עצמאי — false,
     * ולא מתבצעת שום קריאת רשת בכלל (ר' loadAssignments).
     */
    public boolean hasInstitution() {
        return UserRoleManager.getInstance().getInstitutionId() != null;
    }

    /** טוען את המשימות — נקרא מ-onResume, בדיוק כמו TeacherDashboardFragment. */
    public void loadAssignments() {
        if (!hasInstitution()) {
            assignments.setValue(new ArrayList<>()); // תלמיד עצמאי — ה-Fragment מציג מסך "הצטרף לכיתה" בנפרד
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        isLoading.setValue(true);
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("getMyAssignments")
                .call()
                .addOnSuccessListener(result -> attachProgress(parseAssignments(result.getData()), user.getUid()))
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue(describeError(e));
                });
    }

    @SuppressWarnings("unchecked")
    private static List<Assignment> parseAssignments(Object data) {
        List<Assignment> list = new ArrayList<>();
        if (!(data instanceof Map)) return list;
        Object raw = ((Map<?, ?>) data).get("assignments");
        if (!(raw instanceof List)) return list;
        for (Object o : (List<?>) raw) {
            if (o instanceof Map) list.add(Assignment.fromMap((Map<String, Object>) o));
        }
        return list;
    }

    /** שולף את כל ה-progress של התלמיד פעם אחת, ובונה מפה sourceListId → מילים עם correctAttempts&gt;0. */
    private void attachProgress(List<Assignment> rawList, String uid) {
        if (rawList.isEmpty()) {
            assignments.setValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("progress")
                .get()
                .addOnSuccessListener(progressSnap -> {
                    Map<String, Set<String>> practicedByList = new HashMap<>();
                    for (DocumentSnapshot doc : progressSnap) {
                        Long correct = doc.getLong("correctAttempts");
                        String listId = doc.getString("sourceListId");
                        String englishWord = doc.getString("englishWord");
                        if (correct == null || correct <= 0 || listId == null || englishWord == null) continue;
                        practicedByList.computeIfAbsent(listId, k -> new HashSet<>()).add(englishWord);
                    }
                    fetchListWordCounts(rawList, counts -> buildAndPublish(rawList, practicedByList, counts));
                })
                // כישלון בשליפת progress — עדיין מציגים את המשימות, רק בלי פס התקדמות מדויק.
                .addOnFailureListener(e -> buildAndPublish(rawList, new HashMap<>(), new HashMap<>()));
    }

    private interface ListWordCountsCallback {
        void onReady(Map<String, Integer> counts);
    }

    /** שולף wordCount לכל listId ייחודי בין המשימות שיש להן wordIds ריק (=כל הרשימה). */
    private void fetchListWordCounts(List<Assignment> rawList, ListWordCountsCallback callback) {
        Set<String> distinctListIds = new HashSet<>();
        for (Assignment a : rawList) {
            if (a.getWordIds().isEmpty() && a.getListId() != null) distinctListIds.add(a.getListId());
        }
        if (distinctListIds.isEmpty()) {
            callback.onReady(new HashMap<>());
            return;
        }

        List<String> ids = new ArrayList<>(distinctListIds);
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : ids) {
            tasks.add(FirebaseFirestore.getInstance().collection("word_lists").document(id).get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(ignored -> {
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                Task<DocumentSnapshot> t = tasks.get(i);
                if (t.isSuccessful() && t.getResult() != null) {
                    Long wordCount = t.getResult().getLong("wordCount");
                    counts.put(ids.get(i), wordCount != null ? wordCount.intValue() : 0);
                }
            }
            callback.onReady(counts);
        });
    }

    private void buildAndPublish(List<Assignment> rawList, Map<String, Set<String>> practicedByList,
                                  Map<String, Integer> listWordCounts) {
        List<AssignmentWithProgress> result = new ArrayList<>();
        for (Assignment a : rawList) {
            Set<String> practicedWords = practicedByList.get(a.getListId());
            int completed;
            int total;
            if (!a.getWordIds().isEmpty()) {
                total = a.getWordIds().size();
                completed = 0;
                if (practicedWords != null) {
                    for (String w : a.getWordIds()) {
                        if (practicedWords.contains(w)) completed++;
                    }
                }
            } else {
                Integer wordCount = listWordCounts.get(a.getListId());
                total = wordCount != null ? wordCount : 0;
                completed = practicedWords != null ? practicedWords.size() : 0;
            }
            result.add(new AssignmentWithProgress(a, completed, total));
        }
        assignments.setValue(result);
        isLoading.setValue(false);
    }

    @Nullable
    private static String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            if (code == FirebaseFunctionsException.Code.UNAUTHENTICATED) {
                return "יש להתחבר מחדש.";
            }
        }
        return "טעינת המשימות נכשלה.";
    }
}
