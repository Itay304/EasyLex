package com.example.easylex.ui.teacher;

/**
 * =====================================================================
 * ClassDetailViewModel — משימה 0.14 (שלב 2, מערכת משימות)
 * =====================================================================
 *
 * טוען את רשימת המשימות הפעילות של כיתה ספציפית (institutions/{institutionId}
 * /assignments, מסונן לפי classId — ר' הערה ב-functions/index.js על המבנה
 * השטוח) ואת רשימת ה-word_lists הציבוריות (לדרופדאון בדיאלוג "משימה חדשה").
 * יצירת משימה עצמה מתבצעת דרך Cloud Function createAssignment.
 */

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.easylex.data.Assignment;
import com.example.easylex.data.StudentProgress;
import com.example.easylex.data.WordList;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDetailViewModel extends AndroidViewModel {

    /** ה-region שבו נפרסו createAssignment/getClassProgress (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";
    private static final long THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000;

    /** מצב מיון טבלת "תלמידים" — ר' applySortAndFilter(). ברירת מחדל: XP. */
    public enum SortMode { XP, MASTERED, ACTIVITY }

    /** כרטיס הסיכום בראש לשונית "תלמידים". */
    public static class ClassProgressSummary {
        public final int totalStudents;
        public final int activeThisWeek;
        public final double avgMasteredWords;
        public final int inactive3PlusDays;

        ClassProgressSummary(int totalStudents, int activeThisWeek, double avgMasteredWords, int inactive3PlusDays) {
            this.totalStudents = totalStudents;
            this.activeThisWeek = activeThisWeek;
            this.avgMasteredWords = avgMasteredWords;
            this.inactive3PlusDays = inactive3PlusDays;
        }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Assignment>> assignments = new MutableLiveData<>();
    private final MutableLiveData<List<WordList>> wordLists = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> assignmentCreated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> announcementSent = new MutableLiveData<>();

    private final MutableLiveData<List<StudentProgress>> allStudents = new MutableLiveData<>();
    private final MutableLiveData<List<StudentProgress>> displayedStudents = new MutableLiveData<>();
    private final MutableLiveData<ClassProgressSummary> progressSummary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingStudents = new MutableLiveData<>();

    private SortMode sortMode = SortMode.XP;
    private boolean inactiveFilterOn = false;

    private String institutionId;
    private String classId;

    public ClassDetailViewModel(@NonNull Application application) {
        super(application);
    }

    /** נקרא פעם אחת מה-Fragment עם הפרמטרים שהתקבלו ב-Bundle (ר' ClassDetailFragment). */
    public void init(String institutionId, String classId) {
        if (this.classId != null) return; // כבר אותחל — מונע טעינה כפולה בסיבוב מסך
        this.institutionId = institutionId;
        this.classId = classId;
        loadAssignments();
        loadClassProgress();
    }

    public LiveData<List<Assignment>> getAssignments() { return assignments; }
    public LiveData<List<WordList>> getWordLists() { return wordLists; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getAssignmentCreated() { return assignmentCreated; }
    public LiveData<Boolean> getAnnouncementSent() { return announcementSent; }

    public LiveData<List<StudentProgress>> getDisplayedStudents() { return displayedStudents; }
    public LiveData<ClassProgressSummary> getProgressSummary() { return progressSummary; }
    public LiveData<Boolean> getIsLoadingStudents() { return isLoadingStudents; }
    public SortMode getSortMode() { return sortMode; }
    public boolean isInactiveFilterOn() { return inactiveFilterOn; }

    /** טוען את המשימות הפעילות של הכיתה — נקרא גם אחרי יצירת משימה חדשה. */
    public void loadAssignments() {
        if (institutionId == null || classId == null) return;

        isLoading.setValue(true);
        db.collection("institutions").document(institutionId)
                .collection("assignments")
                .whereEqualTo("classId", classId)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Assignment> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        list.add(Assignment.fromDocument(d));
                    }
                    assignments.setValue(list);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("טעינת המשימות נכשלה.");
                    isLoading.setValue(false);
                });
    }

    /** טוען את רשימות המילים הציבוריות (word_lists) — לדרופדאון בדיאלוג "משימה חדשה". */
    public void loadWordLists() {
        if (wordLists.getValue() != null) return; // כבר נטען — לא צריך לשלוף שוב בכל פתיחת דיאלוג

        db.collection("word_lists").get()
                .addOnSuccessListener(snap -> {
                    List<WordList> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Long wordCount = d.getLong("wordCount");
                        list.add(new WordList(d.getId(), d.getString("name"),
                                wordCount != null ? wordCount.intValue() : 0));
                    }
                    wordLists.setValue(list);
                })
                .addOnFailureListener(e -> toastMessage.setValue("טעינת רשימות המילים נכשלה."));
    }

    /** יוצר משימה חדשה דרך createAssignment (Cloud Function), ומרענן את הרשימה בהצלחה. */
    public void createAssignment(String title, String listId, @androidx.annotation.Nullable Long dueDateMs) {
        if (institutionId == null || classId == null) return;

        isLoading.setValue(true);

        Map<String, Object> data = new HashMap<>();
        data.put("classId", classId);
        data.put("listId", listId);
        data.put("title", title);
        data.put("wordIds", new ArrayList<String>()); // ריק = כל המילים ברשימה (שלב זה: "הכל" בלבד)
        if (dueDateMs != null) {
            data.put("dueDateMs", dueDateMs);
        }

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("createAssignment")
                .call(data)
                .addOnSuccessListener(result -> {
                    toastMessage.setValue("המשימה נוצרה בהצלחה!");
                    assignmentCreated.setValue(true);
                    loadAssignments(); // isLoading יתאפס כשהטעינה תסתיים
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue(describeError(e));
                });
    }

    /** שולח הודעה חד-כיוונית לכיתה דרך sendAnnouncement (Cloud Function). */
    public void sendAnnouncement(String message) {
        if (institutionId == null || classId == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("classId", classId);
        data.put("message", message);

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("sendAnnouncement")
                .call(data)
                .addOnSuccessListener(result -> announcementSent.setValue(true))
                .addOnFailureListener(e -> toastMessage.setValue(describeError(e)));
    }

    // ── לשונית "תלמידים" — getClassProgress (Cloud Function) ─────────────────

    /** טוען/מרענן את התקדמות התלמידים — נקרא מ-init() וגם מ-swipe-to-refresh. */
    public void loadClassProgress() {
        if (institutionId == null || classId == null) return;

        isLoadingStudents.setValue(true);

        Map<String, Object> data = new HashMap<>();
        data.put("classId", classId);
        data.put("institutionId", institutionId);

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("getClassProgress")
                .call(data)
                .addOnSuccessListener(result -> {
                    List<StudentProgress> list = parseStudents(result.getData());
                    allStudents.setValue(list);
                    computeSummary(list);
                    applySortAndFilter();
                    isLoadingStudents.setValue(false);
                })
                .addOnFailureListener(e -> {
                    isLoadingStudents.setValue(false);
                    toastMessage.setValue(describeError(e));
                });
    }

    @SuppressWarnings("unchecked")
    private static List<StudentProgress> parseStudents(Object data) {
        List<StudentProgress> list = new ArrayList<>();
        if (!(data instanceof Map)) return list;
        Object raw = ((Map<?, ?>) data).get("students");
        if (!(raw instanceof List)) return list;
        for (Object o : (List<?>) raw) {
            if (o instanceof Map) list.add(StudentProgress.fromMap((Map<String, Object>) o));
        }
        return list;
    }

    private void computeSummary(List<StudentProgress> list) {
        int total = list.size();
        int activeThisWeek = 0;
        int inactive3Plus = 0;
        long masteredSum = 0;
        long now = System.currentTimeMillis();

        for (StudentProgress s : list) {
            if (s.isWeeklyActivity()) activeThisWeek++;
            masteredSum += s.getMasteredWords();
            if (isInactive3Plus(s, now)) inactive3Plus++;
        }
        double avgMastered = total > 0 ? masteredSum / (double) total : 0;
        progressSummary.setValue(new ClassProgressSummary(total, activeThisWeek, avgMastered, inactive3Plus));
    }

    private static boolean isInactive3Plus(StudentProgress s, long now) {
        Long last = s.getLastActiveDateMs();
        return last == null || (now - last) >= THREE_DAYS_MS;
    }

    /** setSortMode — נקרא מה-Fragment כשהמורה בוחר סדר מיון אחר בטבלה. */
    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        applySortAndFilter();
    }

    /** toggleInactiveFilter — כפתור ה-toggle "לא פעילים 3+ ימים". */
    public void toggleInactiveFilter() {
        this.inactiveFilterOn = !this.inactiveFilterOn;
        applySortAndFilter();
    }

    /** applySortAndFilter — מפיק מחדש את displayedStudents מתוך allStudents לפי המצב הנוכחי. */
    private void applySortAndFilter() {
        List<StudentProgress> source = allStudents.getValue();
        if (source == null) {
            displayedStudents.setValue(new ArrayList<>());
            return;
        }

        long now = System.currentTimeMillis();
        List<StudentProgress> filtered = new ArrayList<>();
        for (StudentProgress s : source) {
            if (!inactiveFilterOn || isInactive3Plus(s, now)) filtered.add(s);
        }

        Comparator<StudentProgress> comparator;
        switch (sortMode) {
            case MASTERED:
                comparator = (a, b) -> Integer.compare(b.getMasteredWords(), a.getMasteredWords());
                break;
            case ACTIVITY:
                comparator = (a, b) -> {
                    long aLast = a.getLastActiveDateMs() != null ? a.getLastActiveDateMs() : -1;
                    long bLast = b.getLastActiveDateMs() != null ? b.getLastActiveDateMs() : -1;
                    return Long.compare(bLast, aLast);
                };
                break;
            default: // XP
                comparator = (a, b) -> Long.compare(b.getTotalXp(), a.getTotalXp());
        }
        Collections.sort(filtered, comparator);
        displayedStudents.setValue(filtered);
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            switch (code) {
                case PERMISSION_DENIED:
                    return "אין לך הרשאה ליצור משימה לכיתה זו.";
                case FAILED_PRECONDITION:
                    return "החשבון שלך אינו משויך למוסד.";
                case INVALID_ARGUMENT:
                    return "חסרים שדות חובה — שם משימה ורשימת מילים.";
                case NOT_FOUND:
                    return "הכיתה לא נמצאה.";
                case UNAUTHENTICATED:
                    return "יש להתחבר מחדש.";
                default:
                    return "אירעה שגיאה. נסה שוב.";
            }
        }
        return "אירעה שגיאה. נסה שוב.";
    }
}
