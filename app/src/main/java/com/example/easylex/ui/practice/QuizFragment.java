package com.example.easylex.ui.practice;

/**
 * =====================================================================
 * QuizFragment — מסך השאלון (Multiple Choice Quiz)
 * =====================================================================
 *
 * מה עושה המסך הזה?
 * ------------------
 * מציג שאלון בחירה מרובה: כל שאלה מציגה מילה באנגלית ו-4 אפשרויות
 * תרגום לעברית — רק אחת נכונה. המשתמש מקבל XP על כל תשובה נכונה.
 *
 * מצבי שאלון (quizType):
 * -----------------------
 *   "DAILY"    → מבחן יומי — 20 מילים אדפטיביות מכל המאגר
 *   "MISTAKES" → תיקון טעויות — רק מילים שנכשלו בהן בעבר
 *   "PERSONAL" → המילים שלי — רק מילים שהמשתמש הוסיף
 *   "CATEGORY" → לפי נושא — מילים מקטגוריה ספציפית (תג)
 *
 * אלגוריתם 60/20/20 (בחירה אדפטיבית):
 * --------------------------------------
 * מתוך ה-pool של מילים מסוננות, נבחרות 20 מילים:
 *   60% מילים קשות (Mastery 0–2)
 *   20% מילים חדשות (אף ניסיון)
 *   20% מילים חזקות (Mastery 3–5)
 * כך המשתמש מתרגל בעיקר את מה שהוא לא יודע.
 *
 * שמירת ציון יומי:
 * -----------------
 * בסיום מבחן יומי, הציון נשמר ב-SharedPreferences ("gamification_prefs")
 * כדי שPracticeFragment יוכל להציג אותו כ-Badge ("X/20").
 * =====================================================================
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QuizFragment extends Fragment {

    // ── שדות מחלקה ───────────────────────────────────────────────────────────

    private MyWordsViewModel viewModel;

    /** 20 המילים שנבחרו לשאלון הנוכחי (אחרי הסינון האדפטיבי). */
    private List<Word> quizWords = new ArrayList<>();

    /** כל המילים מה-DB — נדרשות לבניית תשובות הסחת הדעת (Distractors). */
    private List<Word> allMasterWords = new ArrayList<>();

    /** אינדקס השאלה הנוכחית | מספר תשובות נכונות בסשן. */
    private int currentIndex = 0, score = 0;

    /**
     * סוג השאלון שהועבר מ-PracticeFragment דרך Bundle.
     * ערכים אפשריים: "DAILY", "MISTAKES", "PERSONAL", "CATEGORY".
     */
    private String quizType = "DAILY";

    /** tvQuestion = המילה באנגלית | tvCount = "שאלה 3 מתוך 20" | tvXpGain = אנימציית "+10 XP" */
    private TextView tvQuestion, tvCount, tvXpGain;

    /** מנוע Text-To-Speech — השמעת המילה האנגלית. */
    private TextToSpeech tts;

    /** סרגל ההתקדמות הירוק בראש המסך. */
    private LinearProgressIndicator progressBar;

    /** מערך של 4 כפתורי תשובה. */
    private MaterialButton[] optButtons = new MaterialButton[4];

    /**
     * Handler — מאפשר להפעיל קוד אחרי עיכוב.
     * שימוש: 1 שניה אחרי תשובה → מציג שאלה הבאה.
     * Looper.getMainLooper() = הקוד ירוץ על ה-Main Thread (UI Thread).
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ── אתחול ────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_quiz, container, false);

        // חיבור כפתור חזרה
        ((MaterialToolbar) root.findViewById(R.id.toolbar))
            .setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // חיבור אלמנטי UI
        tvQuestion  = root.findViewById(R.id.tvTargetWord);
        tvCount     = root.findViewById(R.id.tvQuestionCount);
        progressBar = root.findViewById(R.id.quizProgress);
        tvXpGain    = root.findViewById(R.id.tvXpGain);
        optButtons[0] = root.findViewById(R.id.btnOpt1);
        optButtons[1] = root.findViewById(R.id.btnOpt2);
        optButtons[2] = root.findViewById(R.id.btnOpt3);
        optButtons[3] = root.findViewById(R.id.btnOpt4);

        // אתחול TTS וכפתור רמקול
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
        root.findViewById(R.id.btnQuizSpeak).setOnClickListener(v -> speakCurrentWord());

        // קריאת סוג השאלון והקטגוריה מה-Bundle שהועבר מ-PracticeFragment
        quizType = getArguments() != null ? getArguments().getString("practiceType", "DAILY") : "DAILY";
        String category = getArguments() != null ? getArguments().getString("selectedCategory", "") : "";

        // טעינת מילים מה-DB וקריאה ל-prepareQuiz פעם אחת
        viewModel = new ViewModelProvider(this).get(MyWordsViewModel.class);
        viewModel.getAllWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null && !words.isEmpty() && quizWords.isEmpty()) {
                allMasterWords = words;
                prepareQuiz(quizType, category); // בחירת 20 המילים
                showNextQuestion();              // הצגת שאלה ראשונה
            }
        });
        return root;
    }

    // ── הכנת השאלון ──────────────────────────────────────────────────────────

    /**
     * prepareQuiz — מסנן ובוחר את 20 המילים לשאלון.
     *
     * שלב 1 — סינון לפי מצב:
     *   DAILY    → כל המילים, בסדר אקראי קבוע ליום (Seed מבוסס תאריך)
     *   MISTAKES → רק מילים עם errorInQuiz=true או errorInFlashcards=true
     *   PERSONAL → רק מילים שהמשתמש הוסיף (isFavorite=true)
     *   CATEGORY → רק מילים שה-tags שלהן מכיל את הקטגוריה הנבחרת
     *
     * שלב 2 — בחירה אדפטיבית 60/20/20:
     *   מחלק את ה-pool לשלוש קבוצות ובוחר לפי יחס.
     *   אם קבוצה קטנה מהנדרש — ממלא מהנותרים.
     */
    private void prepareQuiz(String type, String category) {
        // ── Step 1: mode-based filter ──────────────────────────────────────────
        List<Word> pool = new ArrayList<>();
        if (type.equals("DAILY")) {
            pool.addAll(allMasterWords);
            // Seed = מספר הימים מאז 1.1.1970 → אותה תוצאה לכל השאלונים של אותו יום
            long seed = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
            Collections.shuffle(pool, new Random(seed));
        } else if (type.equals("MISTAKES")) {
            for (Word w : allMasterWords) if (w.isErrorInQuiz() || w.isErrorInFlashcards()) pool.add(w);
        } else if (type.equals("PERSONAL")) {
            for (Word w : allMasterWords) if (w.isFavorite()) pool.add(w);
        } else if (type.equals("CATEGORY")) {
            for (Word w : allMasterWords) if (w.getTags() != null && w.getTags().contains(category)) pool.add(w);
        } else if (type.equals("ASSIGNMENT")) {
            // משימה 0.15: תרגול ממוקד למשימה שהמורה הקצה (StudentAssignmentsFragment).
            // listId/wordIds מגיעים ב-Bundle, לא כפרמטר — כדי לא לשנות את החתימה הקיימת
            // של prepareQuiz (נקראת גם מ-showResultDialog בכפתור "נסה שוב").
            // wordIds ריק = כל המילים ברשימה (המצב היחיד הנתמך כרגע — ר' 0.14).
            String assignmentListId = getArguments() != null
                    ? getArguments().getString("assignmentListId", "") : "";
            List<String> assignmentWordIds = getArguments() != null
                    ? getArguments().getStringArrayList("assignmentWordIds") : null;
            for (Word w : allMasterWords) {
                boolean matchesList = assignmentListId.equals(w.getSourceListId());
                boolean matchesWordIds = assignmentWordIds == null || assignmentWordIds.isEmpty()
                        || assignmentWordIds.contains(w.getEnglishWord());
                if (matchesList && matchesWordIds) pool.add(w);
            }
        }

        // אם אין מילים בכלל לאחר הסינון — יוצאים
        if (pool.isEmpty()) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "אין מילים לתרגול זה", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
            return;
        }

        // ── Step 2: Adaptive 60 / 20 / 20 selection ───────────────────────────
        List<Word> hardWords  = new ArrayList<>();  // Mastery 0-2 — לא יודע טוב
        List<Word> newWords   = new ArrayList<>();  // totalAttempts == 0 — מעולם לא ראה
        List<Word> solidWords = new ArrayList<>();  // Mastery 3-5 — יודע היטב

        for (Word w : pool) {
            if (w.getTotalAttempts() == 0) {
                newWords.add(w);
            } else if (w.getMasteryLevel() <= 2) {
                hardWords.add(w);
            } else {
                solidWords.add(w);
            }
        }

        // ערבוב כל קבוצה בנפרד
        Collections.shuffle(hardWords);
        Collections.shuffle(newWords);
        Collections.shuffle(solidWords);

        // חישוב כמה לקחת מכל קבוצה — DAILY קורא את היעד היומי מ-Settings, שאר המצבים: מקסימום 20
        int maxWords = 20;
        if (type.equals("DAILY") && isAdded()) {
            maxWords = requireContext()
                    .getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE)
                    .getInt(SettingsFragment.KEY_DAILY_GOAL, 20);
        }
        int total     = Math.min(pool.size(), maxWords);
        int wantHard  = (int) Math.round(total * 0.60); // 12 מתוך 20
        int wantNew   = (int) Math.round(total * 0.20); // 4 מתוך 20
        int wantSolid = total - wantHard - wantNew;     // 4 מתוך 20

        List<Word> selected = new ArrayList<>();
        addUpTo(selected, hardWords,  wantHard);
        addUpTo(selected, newWords,   wantNew);
        addUpTo(selected, solidWords, wantSolid);

        // Fill remaining slots if any bucket is smaller than target
        // אם קבוצה הייתה קטנה מהנדרש — ממלאים מהנותרים
        if (selected.size() < total) {
            List<Word> remaining = new ArrayList<>(pool);
            remaining.removeAll(selected);
            Collections.shuffle(remaining);
            addUpTo(selected, remaining, total - selected.size());
        }

        Collections.shuffle(selected); // ערבוב סופי — מניעת דפוס קבוע
        quizWords = selected;
        progressBar.setMax(quizWords.size());
    }

    /**
     * addUpTo — מוסיף עד max פריטים מ-src אל dest.
     * עוזר למנוע IndexOutOfBoundsException כש-src קטן מ-max.
     */
    private static void addUpTo(List<Word> dest, List<Word> src, int max) {
        for (int i = 0; i < Math.min(src.size(), max); i++) dest.add(src.get(i));
    }

    // ── ניקוי ────────────────────────────────────────────────────────────────

    /**
     * onDestroyView — נקרא כש-Fragment נהרס (יציאה מהמסך).
     * חשוב: מנקים את ה-Handler כדי למנוע Memory Leak — אחרת הפעולות
     * המתוזמנות (handler.postDelayed) ימשיכו לרוץ על Fragment שכבר לא קיים.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null); // ביטול כל ה-postDelayed הממתינים
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        // מאפסים refs לאלמנטי UI — מניעת Memory Leak
        tvQuestion  = null;
        tvCount     = null;
        tvXpGain    = null;
        progressBar = null;
        optButtons  = new MaterialButton[4];
    }

    // ── הצגת שאלה ────────────────────────────────────────────────────────────

    /**
     * showNextQuestion — בונה ומציגה את השאלה הנוכחית.
     *
     * בניית אפשרויות (Distractors):
     *   • תשובה נכונה: התרגום של המילה הנוכחית
     *   • 3 הסחות דעת: תרגומים אקראיים של מילים אחרות מה-DB
     *   • כל 4 האפשרויות מעורבבות → הנכונה לא תמיד בקביעות במקום אחד
     */
    private void showNextQuestion() {
        if (!isAdded() || tvQuestion == null) return; // Fragment כבר לא פעיל
        if (currentIndex >= quizWords.size()) { finishQuiz(); return; } // סיימנו

        Word current = quizWords.get(currentIndex);
        tvQuestion.setText(current.getEnglishWord());
        tvCount.setText("שאלה " + (currentIndex + 1) + " מתוך " + quizWords.size());
        progressBar.setProgress(currentIndex + 1);

        // בניית רשימת 4 אפשרויות: 1 נכונה + 3 הסחות דעת
        List<String> options = new ArrayList<>();
        options.add(current.getHebrewTranslation()); // תשובה נכונה — ראשונה לפני הערבוב

        List<Word> distractors = new ArrayList<>(allMasterWords);
        Collections.shuffle(distractors); // ערבוב המאגר לבחירת הסחות דעת אקראיות
        for (Word w : distractors) {
            if (options.size() < 4 && w.getHebrewTranslation() != null
                    && !w.getHebrewTranslation().equals(current.getHebrewTranslation()))
                options.add(w.getHebrewTranslation()); // הסחת דעת תקינה
        }
        Collections.shuffle(options); // ערבוב — הנכונה לא תמיד ראשונה

        for (int i = 0; i < 4; i++) {
            optButtons[i].setText(options.get(i));
            optButtons[i].setStrokeColor(ColorStateList.valueOf(Color.parseColor("#DDDDDD"))); // אפור ניטרלי
            optButtons[i].setOnClickListener(v -> checkAnswer((MaterialButton) v, current));
            optButtons[i].setEnabled(true); // מאפשרים לחיצה (מושבת מהשאלה הקודמת)
        }
    }

    // ── בדיקת תשובה ──────────────────────────────────────────────────────────

    /**
     * checkAnswer — מטפל בלחיצה על אחד מ-4 הכפתורים.
     *
     * מה קורה:
     *   1. מנטרל את כל הכפתורים (מניעת לחיצה כפולה)
     *   2. בודק נכון/שגוי
     *   3. מעדכן correctAttempts ו-totalAttempts ב-DB
     *   4. מוסר XP דרך GamificationEngine
     *   5. מציג אנימציית "+10 XP"
     *   6. אחרי שנייה → שאלה הבאה (handler.postDelayed)
     *
     * Snapshot לפני עדכון:
     *   שומרים את prevCorrect ו-prevTotal לפני השינוי,
     *   כי GamificationEngine צריך להשוות לפני/אחרי כדי לזהות
     *   מעבר ל-Mastery 5 ולתת בונוס +100 XP.
     */
    private void checkAnswer(MaterialButton btn, Word word) {
        for (MaterialButton b : optButtons) b.setEnabled(false); // ניטרול כל הכפתורים
        boolean correct = btn.getText().toString().equals(word.getHebrewTranslation());

        // Snapshot mastery BEFORE updating for bonus detection
        // שמירת מצב לפני העדכון — נדרש לחישוב בונוס XP
        int prevCorrect = word.getCorrectAttempts();
        int prevTotal   = word.getTotalAttempts();
        int prevMastery = word.getMasteryLevel();

        word.setTotalAttempts(word.getTotalAttempts() + 1); // תמיד — גם בשגיאה
        if (correct) {
            score++;
            word.setCorrectAttempts(word.getCorrectAttempts() + 1);
            word.setErrorInQuiz(false);                              // ניקוי דגל השגיאה
            btn.setStrokeColor(ColorStateList.valueOf(Color.GREEN)); // ירוק = נכון
        } else {
            word.setErrorInQuiz(true);                              // סימון לתיקון טעויות
            btn.setStrokeColor(ColorStateList.valueOf(Color.RED));  // אדום = שגוי
            // מציג גם את הנכונה בירוק — ככה המשתמש לומד
            for (MaterialButton b : optButtons) {
                if (b.getText().toString().equals(word.getHebrewTranslation()))
                    b.setStrokeColor(ColorStateList.valueOf(Color.GREEN));
            }
        }
        viewModel.update(word); // שמירת השינוי ב-Room ברקע

        if (correct && isAdded()) {
            // Build minimal "before" snapshot for mastery crossing check
            Word wordBefore = new Word();
            wordBefore.setCorrectAttempts(prevCorrect);
            wordBefore.setTotalAttempts(prevTotal);

            GamificationEngine.getInstance(requireContext()).onCorrectAnswer(wordBefore, word);

            // אם עלינו ל-Mastery 5 בדיוק — בונוס +100 XP נוסף על ה-+10 הרגיל
            boolean masteryUnlocked = (prevMastery < 5 && word.getMasteryLevel() == 5);
            showXpAnimation(masteryUnlocked ? "+110 XP ⭐" : "+10 XP");
        }

        currentIndex++;
        handler.postDelayed(this::showNextQuestion, 1000); // המתנה של שנייה לפני שאלה הבאה
    }

    // ── אנימציית XP ──────────────────────────────────────────────────────────

    /**
     * showXpAnimation — מציג טקסט "+10 XP" שעולה ומתפוגג.
     * אנימציה: TranslationY (עולה 80 פיקסל) + Alpha (נעלם) במשך 900ms.
     */
    private void showXpAnimation(String text) {
        if (tvXpGain == null) return;
        tvXpGain.setText(text);
        tvXpGain.setVisibility(View.VISIBLE);
        tvXpGain.setAlpha(1f);       // מלא
        tvXpGain.setTranslationY(0f); // מיקום התחלתי
        tvXpGain.animate()
            .translationY(-80f)  // עולה למעלה
            .alpha(0f)           // נעלם
            .setDuration(900)
            .withEndAction(() -> {
                if (tvXpGain != null) tvXpGain.setVisibility(View.INVISIBLE);
            })
            .start();
    }

    // ── סיום שאלון ───────────────────────────────────────────────────────────

    /**
     * finishQuiz — נקרא כשעברנו את כל 20 השאלות.
     *
     * מה קורה בסיום:
     *   1. Toast עם הציון הסופי
     *   2. אם ציון >= 60% → בונוס +50 XP על השלמת מודול
     *   3. אם שאלון יומי → שומר ציון ב-SharedPreferences לטובת badge המסך
     *   4. חוזר למסך התרגול
     */
    private void finishQuiz() {
        if (!isAdded()) return;

        // משימה 0.11: כתיבת progress לענן פר-סשן (fire-and-forget, רק לתלמיד מוסדי)
        ProgressSyncManager.syncSession(quizWords);

        // +50 XP module bonus only when score >= 60%
        if (!quizWords.isEmpty() && score * 100 / quizWords.size() >= 60) {
            GamificationEngine.getInstance(requireContext()).onModuleComplete();
        }

        // Persist daily quiz result so PracticeFragment badge stays up-to-date
        if ("DAILY".equals(quizType)) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE);
            int prev = today.equals(prefs.getString("daily_quiz_date", "")) ?
                    prefs.getInt("daily_quiz_score", 0) : 0;
            prefs.edit()
                    .putString("daily_quiz_date", today)
                    .putInt("daily_quiz_score", prev + score)
                    .apply();
        }
        showResultDialog(score, quizWords.size());
    }

    /**
     * showResultDialog — מציג דיאלוג גדול עם תוצאת הסשן:
     * ניצחון (>=60%): אנימציית אמוג'י ניצחון + הודעה מעודדת
     * הפסד (<60%): הודעה שמזכירה להמשיך להתאמן
     */
    private void showResultDialog(int finalScore, int total) {
        if (!isAdded()) return;
        int pct = total > 0 ? finalScore * 100 / total : 0;
        boolean win = pct >= 60;

        View v = getLayoutInflater().inflate(R.layout.dialog_result, null);
        MaterialCardView card = v.findViewById(R.id.resultCard);
        TextView tvEmoji = v.findViewById(R.id.tvResultEmoji);
        TextView tvTitle = v.findViewById(R.id.tvResultTitle);
        TextView tvScore = v.findViewById(R.id.tvResultScore);
        TextView tvMsg   = v.findViewById(R.id.tvResultMsg);
        MaterialButton btnRetry = v.findViewById(R.id.btnResultRetry);
        MaterialButton btnDone  = v.findViewById(R.id.btnResultDone);

        card.setCardBackgroundColor(win ? 0xFF2E7D32 : 0xFFBF360C);
        tvEmoji.setText(win ? "🏆" : "💪");
        tvTitle.setText(win ? "כל הכבוד!" : "אל תוותר!");
        tvScore.setText(finalScore + "/" + total);
        tvMsg.setText(win
                ? "ענית נכון על " + pct + "% מהשאלות — מצוין!"
                : "ענית נכון על " + pct + "%\nעוד קצת תרגול ותגיע לשם!");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v).setCancelable(false).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnDone.setOnClickListener(vv -> { dialog.dismiss(); requireActivity().onBackPressed(); });
        btnRetry.setOnClickListener(vv -> {
            dialog.dismiss();
            currentIndex = 0; score = 0; quizWords.clear();
            prepareQuiz(quizType,
                    getArguments() != null ? getArguments().getString("selectedCategory", "") : "");
            showNextQuestion();
        });

        dialog.show();
        // Entrance animation
        v.setScaleX(0.75f); v.setScaleY(0.75f); v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350)
                .setInterpolator(new OvershootInterpolator()).start();
        tvEmoji.setScaleX(0f); tvEmoji.setScaleY(0f);
        tvEmoji.animate().scaleX(1f).scaleY(1f).setDuration(450).setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    /** speakCurrentWord — מקריא את המילה הנוכחית בקול (בכפוף להגדרת TTS). */
    private void speakCurrentWord() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsFragment.KEY_TTS_ENABLED, true)) return;
        if (tts != null && currentIndex < quizWords.size()) {
            tts.speak(quizWords.get(currentIndex).getEnglishWord(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

}
