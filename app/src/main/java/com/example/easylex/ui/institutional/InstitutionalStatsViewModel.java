package com.example.easylex.ui.institutional;

/**
 * =====================================================================
 * InstitutionalStatsViewModel — סטטיסטיקות תלמיד מוסדי (שלב 2, חלק 3)
 * =====================================================================
 *
 * מקור נתונים יחיד לכל 4 התרשימים: users/{uid}/progress/ (נכתב ע"י
 * ProgressSyncManager, משימה 0.11) + getMyAssignments (Cloud Function,
 * משימה 0.15) לכותרות/wordIds/listId של המשימות. נטען פעם אחת ב-load();
 * "מילה נכבשה" מוגדר במקום אחד בלבד — Word.isMastered() (חלק 2) —
 * לא משוכפל כאן.
 * =====================================================================
 */

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.easylex.data.Assignment;
import com.example.easylex.data.Word;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class InstitutionalStatsViewModel extends AndroidViewModel {

    private static final String FUNCTIONS_REGION = "europe-west1";
    private static final int WEEKS = 8;

    /** גרף 2 — "המשימות שלי": מילים שנכבשו מתוך סה"כ המילים במשימה. */
    public static class AssignmentMastery {
        public final String title;
        public final int mastered;
        public final int total;
        AssignmentMastery(String title, int mastered, int total) {
            this.title = title; this.mastered = mastered; this.total = total;
        }
    }

    /** גרף 4 — "חוזקות וחולשות": מילה בודדת עם נתוני הניסיונות שלה. */
    public static class WordStat {
        public final String englishWord;
        public final int correctAttempts;
        public final int totalAttempts;
        WordStat(String englishWord, int correctAttempts, int totalAttempts) {
            this.englishWord = englishWord; this.correctAttempts = correctAttempts; this.totalAttempts = totalAttempts;
        }
    }

    private final MutableLiveData<List<Integer>> journeyValues = new MutableLiveData<>();
    private final MutableLiveData<List<String>> journeyLabels = new MutableLiveData<>();
    private final MutableLiveData<List<AssignmentMastery>> assignmentMastery = new MutableLiveData<>();
    private final MutableLiveData<int[]> weeklyActivity = new MutableLiveData<>();
    private final MutableLiveData<List<WordStat>> strengths = new MutableLiveData<>();
    private final MutableLiveData<List<WordStat>> weaknesses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public InstitutionalStatsViewModel(@NonNull Application application) { super(application); }

    public LiveData<List<Integer>> getJourneyValues() { return journeyValues; }
    public LiveData<List<String>> getJourneyLabels() { return journeyLabels; }
    public LiveData<List<AssignmentMastery>> getAssignmentMastery() { return assignmentMastery; }
    public LiveData<int[]> getWeeklyActivity() { return weeklyActivity; }
    public LiveData<List<WordStat>> getStrengths() { return strengths; }
    public LiveData<List<WordStat>> getWeaknesses() { return weaknesses; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    /** load — טוען הכל מחדש. נקרא מ-onResume, בדיוק כמו StudentAssignmentsViewModel. */
    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        isLoading.setValue(true);
        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("progress")
                .get()
                .addOnSuccessListener(progressSnap -> {
                    List<ProgressEntry> entries = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : progressSnap) entries.add(ProgressEntry.fromDocument(doc));
                    computeJourney(entries);
                    computeStrengthsWeaknesses(entries);
                    loadAssignmentsAndCompute(entries);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("טעינת הנתונים נכשלה.");
                });
    }

    // ── מסמך progress גולמי ──────────────────────────────────────────────────

    private static class ProgressEntry {
        String englishWord;
        String sourceListId;
        int correctAttempts;
        int totalAttempts;
        @Nullable Timestamp lastPracticed;

        static ProgressEntry fromDocument(DocumentSnapshot doc) {
            ProgressEntry e = new ProgressEntry();
            e.englishWord = doc.getString("englishWord");
            e.sourceListId = doc.getString("sourceListId");
            Long correct = doc.getLong("correctAttempts");
            Long total = doc.getLong("totalAttempts");
            e.correctAttempts = correct != null ? correct.intValue() : 0;
            e.totalAttempts = total != null ? total.intValue() : 0;
            e.lastPracticed = doc.getTimestamp("lastPracticed");
            return e;
        }

        boolean isMastered() {
            Word w = new Word();
            w.setCorrectAttempts(correctAttempts);
            w.setTotalAttempts(totalAttempts);
            return w.isMastered();
        }
    }

    // ── גרף 1: "המסע שלי" — נכבשו מצטבר, 8 שבועות אחרונים ────────────────────

    private void computeJourney(List<ProgressEntry> entries) {
        long now = System.currentTimeMillis();
        long weekMs = 7L * 24 * 60 * 60 * 1000;
        long windowStart = now - (long) WEEKS * weekMs;

        int[] perWeekNewMastered = new int[WEEKS];
        for (ProgressEntry e : entries) {
            if (e.lastPracticed == null || !e.isMastered()) continue;
            long practicedMs = e.lastPracticed.toDate().getTime();
            int weekIdx = practicedMs <= windowStart
                    ? 0
                    : (int) Math.min(WEEKS - 1, (practicedMs - windowStart) / weekMs);
            perWeekNewMastered[weekIdx]++;
        }

        List<Integer> cumulative = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int running = 0;
        for (int i = 0; i < WEEKS; i++) {
            running += perWeekNewMastered[i];
            cumulative.add(running);
            labels.add("ש" + (i + 1));
        }
        journeyValues.postValue(cumulative);
        journeyLabels.postValue(labels);
    }

    // ── גרף 4: "חוזקות וחולשות" — 5 חזקות ביותר + 5 חלשות ביותר ───────────────

    private void computeStrengthsWeaknesses(List<ProgressEntry> entries) {
        List<ProgressEntry> practiced = new ArrayList<>();
        for (ProgressEntry e : entries) if (e.totalAttempts > 0 && e.englishWord != null) practiced.add(e);

        List<ProgressEntry> byStrength = new ArrayList<>(practiced);
        Collections.sort(byStrength, (a, b) -> Integer.compare(b.correctAttempts, a.correctAttempts));
        strengths.postValue(toWordStats(byStrength));

        List<ProgressEntry> byWeakness = new ArrayList<>(practiced);
        Collections.sort(byWeakness, (a, b) ->
                Integer.compare(b.totalAttempts - b.correctAttempts, a.totalAttempts - a.correctAttempts));
        weaknesses.postValue(toWordStats(byWeakness));
    }

    private static List<WordStat> toWordStats(List<ProgressEntry> sorted) {
        List<WordStat> result = new ArrayList<>();
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            ProgressEntry e = sorted.get(i);
            result.add(new WordStat(e.englishWord, e.correctAttempts, e.totalAttempts));
        }
        return result;
    }

    // ── גרפים 2+3: תלויים ברשימת המשימות (getMyAssignments) ──────────────────

    private void loadAssignmentsAndCompute(List<ProgressEntry> entries) {
        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("getMyAssignments")
                .call()
                .addOnSuccessListener(result -> {
                    List<Assignment> list = parseAssignments(result.getData());
                    fetchListWordCounts(list, counts -> {
                        computeAssignmentMastery(list, entries, counts);
                        computeWeeklyActivity(entries, list);
                        isLoading.setValue(false);
                    });
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("טעינת המשימות נכשלה.");
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

    private interface ListWordCountsCallback { void onReady(Map<String, Integer> counts); }

    /** שולף wordCount לכל listId ייחודי בין משימות עם wordIds ריק (= כל הרשימה) — זהה ל-StudentAssignmentsViewModel. */
    private void fetchListWordCounts(List<Assignment> list, ListWordCountsCallback callback) {
        Set<String> distinctListIds = new HashSet<>();
        for (Assignment a : list) {
            if (a.getWordIds().isEmpty() && a.getListId() != null) distinctListIds.add(a.getListId());
        }
        if (distinctListIds.isEmpty()) { callback.onReady(new HashMap<>()); return; }

        List<String> ids = new ArrayList<>(distinctListIds);
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : ids) tasks.add(FirebaseFirestore.getInstance().collection("word_lists").document(id).get());

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

    // ── גרף 2: "המשימות שלי" — נכבשו/סה"כ לכל משימה פעילה ─────────────────────

    private void computeAssignmentMastery(List<Assignment> list, List<ProgressEntry> entries,
                                           Map<String, Integer> listWordCounts) {
        List<AssignmentMastery> result = new ArrayList<>();
        for (Assignment a : list) {
            if (a.getListId() == null || !"active".equals(a.getStatus())) continue;

            int mastered = 0;
            for (ProgressEntry e : entries) {
                if (!a.getListId().equals(e.sourceListId)) continue;
                boolean inScope = a.getWordIds().isEmpty() || a.getWordIds().contains(e.englishWord);
                if (inScope && e.isMastered()) mastered++;
            }
            int total = !a.getWordIds().isEmpty()
                    ? a.getWordIds().size()
                    : (listWordCounts.containsKey(a.getListId()) ? listWordCounts.get(a.getListId()) : 0);

            result.add(new AssignmentMastery(
                    a.getTitle() != null ? a.getTitle() : "משימה", mastered, total));
        }
        assignmentMastery.postValue(result);
    }

    // ── גרף 3: "פעילות שבועית" — כמו הגרף הקיים ב-DashboardFragment, מסונן
    // רק ל-sourceListId-ים ששייכים למשימות של התלמיד ──────────────────────────

    private void computeWeeklyActivity(List<ProgressEntry> entries, List<Assignment> assignments) {
        Set<String> assignmentListIds = new HashSet<>();
        for (Assignment a : assignments) if (a.getListId() != null) assignmentListIds.add(a.getListId());

        int[] counts = new int[7];
        if (!assignmentListIds.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            String[] dayKeys = new String[7];
            for (int i = 0; i < 7; i++) {
                Calendar day = (Calendar) cal.clone();
                day.add(Calendar.DAY_OF_YEAR, -(6 - i));
                dayKeys[i] = sdf.format(day.getTime());
            }
            for (ProgressEntry e : entries) {
                if (e.lastPracticed == null || !assignmentListIds.contains(e.sourceListId)) continue;
                String dayKey = sdf.format(e.lastPracticed.toDate());
                for (int i = 0; i < 7; i++) {
                    if (dayKeys[i].equals(dayKey)) { counts[i]++; break; }
                }
            }
        }
        weeklyActivity.postValue(counts);
    }
}
