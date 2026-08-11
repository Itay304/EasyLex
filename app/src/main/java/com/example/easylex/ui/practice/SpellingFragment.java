package com.example.easylex.ui.practice;

/**
 * =====================================================================
 * SpellingFragment — מסך "אלוף האיות"
 * =====================================================================
 *
 * מה עושה המסך הזה?
 * ------------------
 * מציג תרגול כתיב: המשתמש רואה תרגום עברי, שומע את המילה באנגלית
 * (Text-To-Speech), ומקליד את הכתיב הנכון באנגלית.
 * בשונה מהשאלון — כאן אין אפשרויות בחירה, צריך לדעת לאיית.
 *
 * TTS — Text To Speech:
 * ----------------------
 * Android מכיל מנוע TTS מובנה שיכול לקרוא טקסט בקול.
 * אנחנו מאתחלים אותו עם שפה = US English, ומשתמשים בו
 * לקריאת המילה בכל שאלה (0.5 שניה אחרי הטעינה).
 *
 * אלגוריתם בחירה אדפטיבי (60/20/20):
 * -------------------------------------
 * זהה ל-QuizFragment — 60% קשות, 20% חדשות, 20% חזקות.
 * גודל הסשן: 15 מילים (SESSION_SIZE).
 *
 * ספירת שגיאות — פעם אחת בלבד:
 * --------------------------------
 * currentAttempted = האם כבר ניסה פעם אחת על המילה הנוכחית?
 * רק ניסיון כושל ראשון מעלה totalAttempts — ניסיונות חוזרים
 * לאחר הצגת הרמז לא נספרים שוב (מניעת ענישה כפולה).
 * =====================================================================
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.easylex.R;
import com.example.easylex.data.ProgressSyncManager;
import com.example.easylex.data.Word;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.example.easylex.ui.mywords.MyWordsViewModel;
import com.example.easylex.ui.settings.SettingsFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SpellingFragment extends Fragment {

    // ── קבועים ───────────────────────────────────────────────────────────────

    /** מספר המילים בסשן אחד של "אלוף האיות". */
    private static final int SESSION_SIZE = 15;

    // ── שדות מחלקה ───────────────────────────────────────────────────────────

    private MyWordsViewModel viewModel;

    /** 15 המילים שנבחרו לסשן הנוכחי (אדפטיבי). */
    private List<Word> words = new ArrayList<>();

    /** אינדקס המילה הנוכחית | ציון כמה נכתבו נכון בסשן זה. */
    private int currentIndex = 0;
    private int score = 0;

    /**
     * האם המשתמש כבר ניסה לענות על המילה הנוכחית?
     * מונע ספירת שגיאה כפולה: רק הניסיון הכושל הראשון מעלה totalAttempts.
     */
    private boolean currentAttempted = false; // true after the first attempt on current word

    /** מנוע Text-To-Speech — קריאת המילה בקול. */
    private TextToSpeech tts;

    /**
     * Handler לתזמון קריאת TTS: 500ms אחרי טעינת מילה חדשה.
     * מאפשר ל-UI להתייצב לפני שמתחיל הקול.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** tvHebrew = התרגום העברי | tvCount = "3 / 15" | tvError = הודעת שגיאה גדולה */
    private TextView tvHebrew, tvCount, tvError;

    /** שדה הקלט שבו המשתמש מקליד את הכתיב האנגלי. */
    private TextInputEditText etInput;

    /** עוטף שדה הקלט — מאפשר הצגת הודעת שגיאה מתחת לשדה. */
    private TextInputLayout inputLayout;

    /** סרגל ההתקדמות הירוק. */
    private LinearProgressIndicator progressBar;

    // ── אתחול ────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_spelling, container, false);

        // חיבור כפתור חזרה
        ((MaterialToolbar) root.findViewById(R.id.toolbar))
            .setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // חיבור אלמנטי UI
        tvHebrew    = root.findViewById(R.id.tvHebrewPrompt);
        tvCount     = root.findViewById(R.id.tvSpellingCount);
        etInput     = root.findViewById(R.id.etEnglishInput);
        inputLayout = root.findViewById(R.id.inputLayout);
        progressBar = root.findViewById(R.id.spellingProgress);
        tvError     = root.findViewById(R.id.tvSpellingError);

        // אתחול TTS — מבקשים שפה US English לקריאת מילים
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        // טעינת מילים מה-DB ובחירה אדפטיבית של 15
        viewModel = new ViewModelProvider(this).get(MyWordsViewModel.class);
        viewModel.getAllWords().observe(getViewLifecycleOwner(), w -> {
            if (w != null && words.isEmpty()) {
                // מדלגים על ביטויים (partOfSpeech="phrase") — איות מילולי של ביטוי שלם
                // כולל placeholders (sth/sb) הוא תרגיל לא מתאים למודול הזה.
                List<Word> eligible = new ArrayList<>();
                for (Word word : w) {
                    if (!"phrase".equalsIgnoreCase(word.getPartOfSpeech())) {
                        eligible.add(word);
                    }
                }
                // משימה 0.15 (המשך) — אם הגענו ממשימה שהמורה הקצה, מסננים ל-wordIds/listId שלה בלבד.
                eligible = filterForAssignment(eligible);
                if (eligible.isEmpty() && isAdded()) {
                    Toast.makeText(requireContext(), "אין מילים לתרגול זה", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                    return;
                }
                words = selectAdaptiveWords(eligible, SESSION_SIZE);
                progressBar.setMax(words.size()); // מגדירים את מקסימום הסרגל
                showWord();
            }
        });

        root.findViewById(R.id.btnCheckSpelling).setOnClickListener(v -> check());
        root.findViewById(R.id.btnRepeatAudio).setOnClickListener(v -> speak()); // שוב קריאת TTS
        return root;
    }

    // ── ניקוי ────────────────────────────────────────────────────────────────

    /**
     * onDestroyView — נקרא ביציאה מהמסך.
     * חובה לכבות את TTS ולנקות את ה-Handler — אחרת ממשיכים לפעול ברקע.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null); // ביטול תזמוני TTS ממתינים
        if (tts != null) {
            tts.stop();     // עצור קריאה נוכחית
            tts.shutdown(); // שחרר משאבי מנוע TTS
            tts = null;
        }
    }

    // ── סינון למשימה (ASSIGNMENT) ───────────────────────────────────────────

    /**
     * filterForAssignment — זהה ל-FlashcardsFragment.filterForAssignment ול-
     * QuizFragment.prepareQuiz's ASSIGNMENT branch: אם practiceType="ASSIGNMENT",
     * מצמצם ל-listId/wordIds של המשימה בלבד. אחרת מחזיר ללא שינוי.
     */
    private List<Word> filterForAssignment(List<Word> pool) {
        String practiceType = getArguments() != null ? getArguments().getString("practiceType", "") : "";
        if (!"ASSIGNMENT".equals(practiceType)) return pool;

        String assignmentListId = getArguments().getString("assignmentListId", "");
        List<String> assignmentWordIds = getArguments().getStringArrayList("assignmentWordIds");

        List<Word> filtered = new ArrayList<>();
        for (Word w : pool) {
            boolean matchesList = assignmentListId.equals(w.getSourceListId());
            boolean matchesWordIds = assignmentWordIds == null || assignmentWordIds.isEmpty()
                    || assignmentWordIds.contains(w.getEnglishWord());
            if (matchesList && matchesWordIds) filtered.add(w);
        }
        return filtered;
    }

    // ── בחירה אדפטיבית ───────────────────────────────────────────────────────

    /**
     * selectAdaptiveWords — בוחרת עד total מילים מה-pool לפי יחס 60/20/20.
     *
     * הגיון:
     *   60% מילים שה-Mastery שלהן נמוך (0–2) → צריך תרגול דחוף
     *   20% מילים חדשות (אף ניסיון)           → חשיפה ראשונה
     *   20% מילים חזקות (Mastery 3–5)          → חזרה לשמירה
     *
     * אם קבוצה קטנה מהנדרש — ממלאים את החסר מהנותרים.
     *
     * @param pool  רשימת כל המילים מה-DB
     * @param total מספר המילים המקסימלי לסשן (= SESSION_SIZE = 15)
     */
    private List<Word> selectAdaptiveWords(List<Word> pool, int total) {
        List<Word> hard  = new ArrayList<>();
        List<Word> newW  = new ArrayList<>();
        List<Word> solid = new ArrayList<>();

        for (Word w : pool) {
            if (w.getTotalAttempts() == 0)     newW.add(w);
            else if (w.getMasteryLevel() <= 2) hard.add(w);
            else                               solid.add(w);
        }
        Collections.shuffle(hard);
        Collections.shuffle(newW);
        Collections.shuffle(solid);

        int count     = Math.min(pool.size(), total);
        int wantHard  = (int) Math.round(count * 0.60);
        int wantNew   = (int) Math.round(count * 0.20);
        int wantSolid = count - wantHard - wantNew;

        List<Word> selected = new ArrayList<>();
        addUpTo(selected, hard,  wantHard);
        addUpTo(selected, newW,  wantNew);
        addUpTo(selected, solid, wantSolid);

        // Fill any remaining slots if a bucket was smaller than its target
        if (selected.size() < count) {
            List<Word> remaining = new ArrayList<>(pool);
            remaining.removeAll(selected);
            Collections.shuffle(remaining);
            addUpTo(selected, remaining, count - selected.size());
        }
        Collections.shuffle(selected);
        return selected;
    }

    /** עוזר: מוסיף עד max פריטים מ-src אל dest. */
    private static void addUpTo(List<Word> dest, List<Word> src, int max) {
        for (int i = 0; i < Math.min(src.size(), max); i++) dest.add(src.get(i));
    }

    // ── תצוגה ────────────────────────────────────────────────────────────────

    /**
     * showWord — מציגה את המילה הנוכחית וממתינה 500ms לפני קריאת TTS.
     * אם סיימנו את כל המילים — קוראת finish().
     */
    private void showWord() {
        if (currentIndex >= words.size()) {
            finish();
            return;
        }
        currentAttempted = false; // מילה חדשה — מאפסים דגל הניסיון
        Word current = words.get(currentIndex);
        tvHebrew.setText(current.getHebrewTranslation()); // הצגת הרמז בעברית
        tvCount.setText((currentIndex + 1) + " / " + words.size());
        progressBar.setProgress(currentIndex + 1);
        etInput.setText("");         // ניקוי שדה הקלט
        inputLayout.setError(null);  // הסרת הודעת שגיאה מהשאלה הקודמת
        tvError.setVisibility(android.view.View.GONE); // הסתרת הודעת השגיאה הגדולה
        handler.postDelayed(this::speak, 500); // קריאת TTS אחרי חצי שנייה
    }

    // ── בדיקת תשובה ──────────────────────────────────────────────────────────

    /**
     * check — בודק את מה שהמשתמש הקליד מול הכתיב הנכון.
     *
     * תשובה נכונה:
     *   • spellingCorrect++ (ספציפי לאיות)
     *   • correctAttempts++ ו-totalAttempts++ (לנוסחת ה-Mastery)
     *   • GamificationEngine → +10 XP
     *   • score++ → מוצג בסוף הסשן
     *   • מעבר למילה הבאה
     *
     * תשובה שגויה (פעם ראשונה בלבד):
     *   • totalAttempts++ (מוריד את ה-Mastery)
     *   • currentAttempted = true (שגיאות נוספות לא יספרו)
     *   • הצגת התשובה הנכונה כ-Error hint
     *
     * equalsIgnoreCase — מתעלם מאותיות גדולות/קטנות (Hello = hello).
     */
    private void check() {
        String input = etInput.getText().toString().trim();
        Word word = words.get(currentIndex);

        if (input.equalsIgnoreCase(word.getEnglishWord().trim())) {
            // ── תשובה נכונה ──────────────────────────────────────────────────

            // Snapshot BEFORE updating — required for mastery-crossing detection
            // שמירת מצב לפני עדכון — GamificationEngine צריך להשוות לפני/אחרי
            Word wordBefore = new Word();
            wordBefore.setCorrectAttempts(word.getCorrectAttempts());
            wordBefore.setTotalAttempts(word.getTotalAttempts());

            word.setErrorInSpelling(false);
            word.setSpellingCorrect(word.getSpellingCorrect() + 1);           // ספירת כתיב נכון
            word.setCorrectAttempts(word.getCorrectAttempts() + 1); // feeds mastery formula
            word.setTotalAttempts(word.getTotalAttempts() + 1);     // feeds mastery formula
            viewModel.update(word);

            GamificationEngine.getInstance(requireContext()).onCorrectAnswer(wordBefore, word);
            score++;
            currentIndex++;
            showWord();

        } else {
            // ── תשובה שגויה — ספירה רק בניסיון הראשון ───────────────────────
            if (!currentAttempted) {
                word.setErrorInSpelling(true);
                word.setTotalAttempts(word.getTotalAttempts() + 1); // lowers mastery
                viewModel.update(word);
                currentAttempted = true; // לא נספור שגיאות נוספות על אותה מילה
            }
            // הודעת שגיאה גדולה + עדכון inputLayout לנגישות
            tvError.setText("✗  " + word.getEnglishWord());
            tvError.setVisibility(android.view.View.VISIBLE);
            inputLayout.setError(""); // מסמן את שדה הקלט כשגוי
        }
    }

    // ── סיום ─────────────────────────────────────────────────────────────────

    /**
     * finish — נקרא לאחר כל 15 המילים.
     * מציג הודעת סיום עם הציון וחוזר למסך התרגול.
     */
    private void finish() {
        if (!isAdded()) return;

        // משימה 0.11: כתיבת progress לענן פר-סשן (fire-and-forget, רק לתלמיד מוסדי)
        ProgressSyncManager.syncSession(words);

        int total = words.size();
        int pct   = total > 0 ? score * 100 / total : 0;
        boolean win = pct >= 60;

        View v = getLayoutInflater().inflate(R.layout.dialog_result, null);
        MaterialCardView card   = v.findViewById(R.id.resultCard);
        TextView tvEmoji        = v.findViewById(R.id.tvResultEmoji);
        TextView tvTitle        = v.findViewById(R.id.tvResultTitle);
        TextView tvScore        = v.findViewById(R.id.tvResultScore);
        TextView tvMsg          = v.findViewById(R.id.tvResultMsg);
        MaterialButton btnRetry = v.findViewById(R.id.btnResultRetry);
        MaterialButton btnDone  = v.findViewById(R.id.btnResultDone);

        card.setCardBackgroundColor(win ? 0xFF1B5E20 : 0xFFBF360C);
        tvEmoji.setText(win ? "🏆" : "💪");
        tvTitle.setText(win ? "אלוף האיות!" : "אל תוותר!");
        tvScore.setText(score + "/" + total);
        tvMsg.setText(win
                ? "איתת נכון " + pct + "% ✓ — איות מצוין!"
                : "כיוונת " + pct + "%\nאיות דורש תרגול — המשך!");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v).setCancelable(false).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnDone.setOnClickListener(vv -> { dialog.dismiss(); requireActivity().onBackPressed(); });
        btnRetry.setOnClickListener(vv -> {
            dialog.dismiss();
            currentIndex = 0; score = 0; currentAttempted = false;
            showWord();
        });

        dialog.show();
        v.setScaleX(0.75f); v.setScaleY(0.75f); v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350)
                .setInterpolator(new OvershootInterpolator()).start();
        tvEmoji.setScaleX(0f); tvEmoji.setScaleY(0f);
        tvEmoji.animate().scaleX(1f).scaleY(1f).setDuration(450).setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    /**
     * speak — קורא את המילה הנוכחית בקול דרך TTS.
     * QUEUE_FLUSH = עצור כל קריאה קיימת ותתחיל מהתחלה (לא מחכה בתור).
     */
    private void speak() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsFragment.KEY_TTS_ENABLED, true)) return;
        if (tts != null && currentIndex < words.size())
            tts.speak(words.get(currentIndex).getEnglishWord(), TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
