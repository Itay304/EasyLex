package com.example.easylex.ui.practice;

/**
 * =====================================================================
 * PracticeFragment — מסך תפריט התרגול
 * =====================================================================
 *
 * מה עושה המסך הזה?
 * ------------------
 * מסך הבית של אזור התרגול — מציג רשת (Grid) של 6 כרטיסי תרגול:
 *   • מבחן יומי   — 20 מילים אדפטיביות
 *   • לפי נושאים  — בחירת קטגוריה
 *   • המילים שלי  — רק מילים שהמשתמש הוסיף
 *   • תיקון טעויות — מילים שנכשל בהן
 *   • אלוף האיות  — תרגול כתיב
 *   • כרטיסיות    — Flashcards
 *
 * Badge (תגית) על כל כרטיס:
 * ---------------------------
 * כל כרטיס מציג תגית עם מספר — מראה למשתמש את ההתקדמות שלו:
 *   מבחן יומי   → "X/20"  (מ-SharedPreferences — ציון של היום)
 *   תיקון טעויות → "X"     (מספר מילים עם errorFlag=true)
 *   המילים שלי  → "X"     (מספר מילים אישיות)
 *   אלוף האיות  → "X/Y"   (ספקלינג נכון / סה"כ)
 *   כרטיסיות   → "X/Y"   (נראו ונכונות / סה"כ)
 *
 * ניווט:
 * ------
 * Navigation Component — לחיצה על כרטיס מנווטת ל-Fragment המתאים
 * עם Bundle (פרמטרים). QuizFragment מקבל practiceType ו-selectedCategory.
 *
 * כותרת XP:
 * ----------
 * בראש המסך — שורת Level + Streak + סרגל XP.
 * מסונכרנת מ-Firestore בכל onResume().
 * =====================================================================
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easylex.R;
import com.example.easylex.data.Word;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.example.easylex.ui.mywords.MyWordsViewModel;
import com.example.easylex.ui.settings.SettingsFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PracticeFragment extends Fragment {

    private MyWordsViewModel viewModel;

    /** כל המילים מה-DB — משמשות לחישוב ה-Badge של כל כרטיס. */
    private List<Word> masterWords = new ArrayList<>();

    /** Adapter של ה-RecyclerView — מציג את 6 הכרטיסים. */
    private PracticeAdapter adapter;

    // ── שדות כותרת XP ────────────────────────────────────────────────────────
    // XP header views
    private TextView tvPracticeLevel, tvStreakBadge;
    private LinearProgressIndicator progressXp;

    // ── אתחול ────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_practice, container, false);

        // Bind XP header — חיבור שורת XP
        tvPracticeLevel = root.findViewById(R.id.tvPracticeLevel);
        tvStreakBadge   = root.findViewById(R.id.tvStreakBadge);
        progressXp      = root.findViewById(R.id.progressXp);

        // הגדרת RecyclerView כ-Grid בעמודתיים
        RecyclerView rv = root.findViewById(R.id.rvPracticeOptions);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new PracticeAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        // האזנה לשינויים ב-DB — מעדכנים Badges בכל שינוי
        viewModel = new ViewModelProvider(this).get(MyWordsViewModel.class);
        viewModel.getAllWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null) {
                masterWords = words;
                updateMenu(); // חישוב מחדש של כל ה-Badges
            }
        });
        return root;
    }

    /**
     * onResume — נקרא בכל כניסה למסך.
     * קודם מציג את הנתונים המקומיים (מהיר), אחר כך מסנכרן מ-Firestore ומרענן.
     * Sync Firestore then refresh header (synchronous SharedPrefs read is instant before sync)
     */
    @Override
    public void onResume() {
        super.onResume();
        GamificationEngine ge = GamificationEngine.getInstance(requireContext());
        updateXpHeader(ge);                           // מיידי — מ-SharedPrefs
        ge.syncFromFirestore(() -> updateXpHeader(ge)); // אחרי Firestore
    }

    /**
     * updateXpHeader — מרענן את שורת Level + Streak + XP Progress.
     * @param ge מופע GamificationEngine עם הנתונים המעודכנים
     */
    private void updateXpHeader(GamificationEngine ge) {
        if (tvPracticeLevel == null) return;
        tvPracticeLevel.setText("רמה " + ge.getCurrentLevel());
        tvStreakBadge.setText("🔥 " + ge.getStreak());
        int range  = ge.getXpRangeOfLevel();   // גודל הרמה הנוכחית
        int within = ge.getXpWithinLevel();    // כמה XP כבר צברנו ברמה זו
        progressXp.setMax(Math.max(range, 1)); // מקסימום הסרגל (מינימום 1)
        progressXp.setProgress(within);
    }

    // ── בניית התפריט ─────────────────────────────────────────────────────────

    /**
     * updateMenu — בונה את רשימת 6 הכרטיסים עם ה-Badges המעודכנים.
     * PracticeOption = אובייקט נתונים: שם, תיאור, אייקון, צבע, type, badge.
     */
    private void updateMenu() {
        int dailyGoal = requireContext()
                .getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getInt(SettingsFragment.KEY_DAILY_GOAL, 20);
        List<PracticeOption> options = new ArrayList<>();
        options.add(new PracticeOption("מבחן יומי",    dailyGoal + " מילים ליום", R.drawable.ic_daily_quiz,  Color.parseColor("#FF9800"), "DAILY",      getDailyProgress(dailyGoal)));
        options.add(new PracticeOption("לפי נושאים",   "לפי קטגוריות AI",         R.drawable.ic_categories,  Color.parseColor("#4CAF50"), "CATEGORY",   ""));
        options.add(new PracticeOption("המילים שלי",   "רק מה שסימנת",            R.drawable.ic_favorites,   Color.parseColor("#E91E63"), "PERSONAL",   getPersonalCount()));
        options.add(new PracticeOption("תיקון טעויות", "שגיאות שנותרו",           R.drawable.ic_mistakes,    Color.parseColor("#F44336"), "MISTAKES",   getMistakeCount()));
        options.add(new PracticeOption("אלוף האיות",   "כתיבה נכונה",             R.drawable.ic_spelling,    Color.parseColor("#2196F3"), "SPELLING",   getSpellingCount()));
        options.add(new PracticeOption("כרטיסיות",    "שינון מהיר",              R.drawable.ic_flashcards,  Color.parseColor("#9C27B0"), "FLASHCARDS", getFlashCount()));
        adapter.setData(options);
    }

    // ── חישובי Badge ──────────────────────────────────────────────────────────

    /**
     * getDailyProgress — מחזיר ציון המבחן היומי כ-"X/Goal".
     * קורא מ-SharedPreferences: QuizFragment שומר את הציון שם בסיום.
     * אם לא בוצע מבחן היום — מחזיר "0/Goal".
     */
    private String getDailyProgress(int goal) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE);
        if (today.equals(prefs.getString("daily_quiz_date", ""))) {
            int score = prefs.getInt("daily_quiz_score", 0);
            return Math.min(score, goal) + "/" + goal;
        }
        return "0/" + goal;
    }

    /**
     * getMistakeCount — מונה מילים שיש להן דגל שגיאה לפחות באחד המודולים.
     * errorInQuiz + errorInSpelling + errorInFlashcards — כולן מתעדכנות בזמן תרגול.
     */
    private String getMistakeCount() {
        int count = 0;
        for (Word w : masterWords) if (w.isErrorInQuiz() || w.isErrorInSpelling() || w.isErrorInFlashcards()) count++;
        return String.valueOf(count);
    }

    /**
     * getSpellingCount — מחזיר "X/Y" — כמה מילים נאותו נכון לפחות פעם אחת.
     * spellingCorrect > 0 = נאות נכון לפחות פעם אחת ב"אלוף האיות".
     */
    private String getSpellingCount() {
        int count = 0;
        for (Word w : masterWords) if (w.getSpellingCorrect() > 0) count++;
        return count + "/" + masterWords.size();
    }

    /**
     * getPersonalCount — מחזיר את מספר המילים האישיות (isFavorite=true).
     */
    private String getPersonalCount() {
        int count = 0;
        for (Word w : masterWords) if (w.isFavorite()) count++;
        return String.valueOf(count);
    }

    /**
     * getFlashCount — מחזיר "X/Y" — כמה מילים "נוצחו" בכרטיסיות.
     * "נוצחה" = totalAttempts > 0 (נראתה לפחות פעם) AND !errorInFlashcards (לא הופיע שמאל לאחרונה).
     */
    private String getFlashCount() {
        int count = 0;
        for (Word w : masterWords) if (w.getTotalAttempts() > 0 && !w.isErrorInFlashcards()) count++;
        return count + "/" + masterWords.size();
    }

    // ── Adapter פנימי ─────────────────────────────────────────────────────────

    /**
     * PracticeAdapter — מחלקה פנימית של RecyclerView.Adapter.
     *
     * RecyclerView עובד כך:
     *   onCreateViewHolder — יוצר View חדש (item_practice_type.xml) — קורה מעט פעמים
     *   onBindViewHolder   — "ממלא" View קיים בנתונים של פריט ספציפי — קורה לכל פריט
     *   getItemCount       — כמה פריטים יש (= 6)
     *
     * VH = ViewHolder — מחזיק refs לאלמנטי ה-UI של פריט אחד.
     * מונע קריאות חוזרות ל-findViewById (ביצועים טובים יותר).
     */
    class PracticeAdapter extends RecyclerView.Adapter<PracticeAdapter.VH> {
        List<PracticeOption> data;
        PracticeAdapter(List<PracticeOption> data) { this.data = data; }

        /** setData — מחליף את רשימת הנתונים ומרענן את התצוגה. */
        public void setData(List<PracticeOption> d) { this.data = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            // מנפחים את ה-XML של פריט אחד
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_practice_type, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            PracticeOption opt = data.get(p);
            h.title.setText(opt.getTitle());
            h.badge.setText(opt.getBadgeText());
            // מסתירים Badge אם הערך "0" או ריק — אין טעם להציג "0 שגיאות"
            h.badge.setVisibility(opt.getBadgeText().equals("0") || opt.getBadgeText().isEmpty()
                    ? View.GONE : View.VISIBLE);
            h.bg.setBackgroundColor(opt.getColor()); // צבע ייחודי לכל כרטיס
            h.icon.setImageResource(opt.getIconRes());
            h.itemView.setOnClickListener(v -> handleNav(v, opt)); // לחיצה → ניווט
        }

        @Override public int getItemCount() { return data.size(); }

        /** VH — מחזיק refs לאלמנטי UI של כרטיס אחד. */
        class VH extends RecyclerView.ViewHolder {
            TextView title, badge;
            ImageView icon;
            View bg;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.practiceTitle);
                badge = v.findViewById(R.id.tvBadge);
                icon  = v.findViewById(R.id.practiceIcon);
                bg    = v.findViewById(R.id.cardBackground);
            }
        }
    }

    // ── ניווט ────────────────────────────────────────────────────────────────

    /**
     * handleNav — מנווט למסך המתאים לפי ה-type של הכרטיס שנלחץ.
     * משתמש ב-Navigation Component — בטוח וקל לתחזוקה.
     * Bundle = כמו "מעטפה" שמעבירה פרמטרים בין Fragments.
     */
    private void handleNav(View v, PracticeOption opt) {
        Bundle b = new Bundle();
        b.putString("practiceType", opt.getType());
        if (opt.getType().equals("CATEGORY")) {
            showCategorySelection(v); // דיאלוג בחירת קטגוריה לפני הניווט
        } else if (opt.getType().equals("FLASHCARDS")) {
            Navigation.findNavController(v).navigate(R.id.action_navigation_practice_to_flashcardsFragment);
        } else if (opt.getType().equals("SPELLING")) {
            Navigation.findNavController(v).navigate(R.id.action_navigation_practice_to_spellingFragment);
        } else {
            // DAILY, MISTAKES, PERSONAL — כולם ל-QuizFragment עם Bundle
            Navigation.findNavController(v).navigate(R.id.action_navigation_practice_to_quizFragment, b);
        }
    }

    /**
     * showCategorySelection — מציג דיאלוג בחירת קטגוריה.
     * לאחר בחירה, מנווט ל-QuizFragment עם practiceType="CATEGORY" + selectedCategory.
     */
    private void showCategorySelection(View v) {
        // מפתחות Firestore (tags) — אל תשנה ללא עדכון ה-DB
        String[] catKeys = {
            "Learning & Education",
            "Daily Life & Environment",
            "Emotions & Relationships",
            "Society & Culture",
            "Health & Science",
            "Abstract Actions & Concepts"
        };
        // תוויות לתצוגה בעברית
        String[] catLabels = {
            "לימוד וחינוך",
            "חיי יומיום וסביבה",
            "רגשות ומערכות יחסים",
            "חברה ותרבות",
            "בריאות ומדע",
            "מושגים ופעולות מופשטות"
        };
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("בחר נושא")
                .setItems(catLabels, (d, which) -> {
                    Bundle b = new Bundle();
                    b.putString("practiceType", "CATEGORY");
                    b.putString("selectedCategory", catKeys[which]); // מפתח Firestore
                    Navigation.findNavController(v).navigate(R.id.action_navigation_practice_to_quizFragment, b);
                }).show();
    }
}
