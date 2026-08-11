package com.example.easylex.ui.statistics;

/**
 * =====================================================================
 * StatisticsFragment — מסך הסטטיסטיקות
 * =====================================================================
 *
 * מה עושה המסך הזה?
 * ------------------
 * מציג למשתמש סיכום מלא של התקדמותו:
 *   • XP כולל, רמה נוכחית, רצף יומי
 *   • סרגל התקדמות לרמה הבאה
 *   • אחוז שליטה כולל (Mastery %)
 *   • מספר מילים שנכבשו (correctAttempts >= 3)
 *   • דיוק כללי בתרגול (correctAttempts / totalAttempts)
 *
 * שני מקורות נתונים:
 * -------------------
 *   1. GamificationEngine (SharedPreferences) → XP, Level, Streak
 *      מסונכרן מ-Firestore ב-onResume()
 *   2. Room DB (LiveData<List<Word>>) → חישוב Mastery ודיוק
 *
 * מתי מתרעננים הנתונים?
 * -----------------------
 *   • Room: LiveData — מתעדכן אוטומטית עם כל שינוי ב-DB
 *   • GamificationEngine: onResume() — כל כניסה למסך מסנכרנת מ-Firestore
 *
 * הגדרת "מילה שנכבשה":
 * ----------------------
 * Word.isMastered() — correctAttempts >= 3 וגם דיוק >= 70% (ר' Word.java).
 * =====================================================================
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.easylex.data.Word;
import com.example.easylex.databinding.FragmentStatisticsBinding;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.example.easylex.ui.mywords.MyWordsViewModel;
import java.util.List;

public class StatisticsFragment extends Fragment {

    /**
     * ViewBinding — גישה בטוחה לאלמנטי ה-XML ללא צורך ב-findViewById.
     * מוגדר כ-null ב-onDestroyView למניעת Memory Leak.
     */
    private FragmentStatisticsBinding binding;

    /** GamificationEngine — מקור נתוני XP/רמה/רצף. */
    private GamificationEngine gamification;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding      = FragmentStatisticsBinding.inflate(inflater, container, false);
        gamification = GamificationEngine.getInstance(requireContext());

        // מאזינים לשינויים ברשימת המילים — calculateMastery מחשב את כל האחוזים
        new ViewModelProvider(this).get(MyWordsViewModel.class)
            .getAllWords()
            .observe(getViewLifecycleOwner(), words -> {
                if (words != null && !words.isEmpty()) calculateMastery(words);
            });

        return binding.getRoot();
    }

    /**
     * onResume — נקרא בכל כניסה למסך (גם מרקע, גם מNavigate back).
     * מסנכרן XP/רמה/רצף מ-Firestore ומרענן את ה-UI.
     * כך אם המשתמש צבר XP במכשיר אחר — הנתונים יתעדכנו.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Sync server-side XP/level/streak then refresh UI
        gamification.syncFromFirestore(this::updateGamificationUi);
    }

    /**
     * onDestroyView — ניקוי binding למניעת Memory Leak.
     * Fragment ממשיך לחיות אחרי שה-View נהרס (למשל במעבר בין Fragments).
     * אם binding לא מאופס, הוא ימנע מה-View להיאסף ע"י ה-GC.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── עדכון UI ──────────────────────────────────────────────────────────────

    /**
     * updateGamificationUi — מרענן את כרטיסיית XP/Level/Streak.
     * נקרא אחרי סנכרון Firestore (כשהנתונים העדכניים מוכנים).
     * קריאות getXxxx() הן synchronous מ-SharedPreferences — מהירות.
     *
     * getXpWithinLevel() = XP שנצבר בתוך הרמה הנוכחית (לסרגל התקדמות)
     * getXpRangeOfLevel() = כמה XP שוות הרמה הנוכחית בסך הכל (מקסימום הסרגל)
     */
    private void updateGamificationUi() {
        if (binding == null) return; // Fragment כבר לא פעיל

        int xp     = gamification.getTotalXp();
        int level  = gamification.getCurrentLevel();
        int streak = gamification.getStreak();
        int within = gamification.getXpWithinLevel();  // XP מתוך הרמה
        int range  = gamification.getXpRangeOfLevel(); // גודל הרמה
        int toNext = gamification.getXpForNextLevel() - xp; // כמה עד הרמה הבאה

        binding.tvXPPoints.setText(xp + " XP");
        binding.tvLevel.setText("רמה " + level);
        binding.tvStreak.setText(streak + " ימים ברצף 🔥");
        binding.progressLevel.setMax(Math.max(range, 1)); // מינימום 1 — מניעת חלוקה ב-0
        binding.progressLevel.setProgress(within);
        binding.tvXpToNext.setText(toNext + " XP לרמה הבאה");
    }

    /**
     * calculateMastery — מחשב סטטיסטיקות שליטה ודיוק מרשימת המילים.
     *
     * חישובים:
     *   mastered   = מילים עם Word.isMastered() == true (הגדרת "נכבשה")
     *   masteryPct = mastered / total × 100  → "X% שליטה"
     *   accuracy   = correctAttempts_כולל / totalAttempts_כולל × 100 → "X% דיוק"
     *
     * שני סוגי מילים:
     *   Long totalAttempts/correctAttempts — long ולא int לגיבוש על 1902 מילים
     *   (int מגיע עד ~2M — מספיק, אבל long בטוח יותר)
     *
     * @param words רשימת כל המילים מה-DB
     */
    private void calculateMastery(List<Word> words) {
        if (binding == null) return;

        int total = words.size(); // סה"כ מילים במאגר
        int mastered = 0;
        long totalAttempts = 0, correctAttempts = 0;

        for (Word w : words) {
            if (w.isMastered()) mastered++; // "נכבשה" = Word.isMastered() (ר' תיעוד שם)
            totalAttempts   += w.getTotalAttempts();     // סך כל הניסיונות
            correctAttempts += w.getCorrectAttempts();   // סך כל הנכונים
        }

        // חישוב אחוז שליטה (כמה מילים "נכבשו") — עשרוני לדיוק
        float masteryPct = total > 0 ? (mastered * 100.0f) / total : 0f;
        // חישוב דיוק כללי (נכון מסך כל הניסיונות) — עשרוני לדיוק
        float accuracy   = totalAttempts > 0 ? (correctAttempts * 100.0f) / totalAttempts : 0f;

        // עדכון ה-UI עם התוצאות
        binding.tvMasteryScore.setText(String.format("%.1f%%", masteryPct));
        binding.tvTotalWordsCount.setText(mastered + " מתוך " + total + " מילים נכבשו");
        binding.tvAccuracyPercent.setText(String.format("%.1f%%", accuracy));
        binding.progressMasteryCircular.setProgress(Math.round(masteryPct));

        // Also refresh gamification row with current SharedPrefs values
        updateGamificationUi();

        // עדכון כרטיס דירוג לפי אחוז שליטה
        updateRank(masteryPct, mastered);
    }

    /**
     * updateRank — בוחר תואר לפי אחוז השליטה וכמות מילים שנכבשו.
     *
     * סולם הדירוג:
     *   < 5%          🌱 מתחיל    "המסע זה עתה החל!"
     *   5-19%         📖 תלמיד    "אתה לומד, המשך!"
     *   20-39%        ⭐ מתקדם    "ביצועים מרשימים!"
     *   40-59%        🎖 אלוף     "הכרת מילים רבות!"
     *   60-79%        👑 נסיך     "בקיאות מרשימה!"
     *   80-94%        🏆 מלך      "שליטה מעולה!"
     *   95-100%       💎 אגדה     "אין לך מתחרים!"
     */
    private void updateRank(float pct, int mastered) {
        if (binding == null) return;
        String emoji, title, desc;
        if      (pct < 5)  { emoji = "🌱"; title = "מתחיל";  desc = "המסע זה עתה החל!"; }
        else if (pct < 20) { emoji = "📖"; title = "תלמיד";  desc = "אתה לומד, המשך כך!"; }
        else if (pct < 40) { emoji = "⭐"; title = "מתקדם";  desc = "ביצועים מרשימים! " + mastered + " מילים נכבשו."; }
        else if (pct < 60) { emoji = "🎖"; title = "אלוף";   desc = "הכרת מילים רבות! כמעט נסיך!"; }
        else if (pct < 80) { emoji = "👑"; title = "נסיך";   desc = "בקיאות מרשימה! דרך לכתר!"; }
        else if (pct < 95) { emoji = "🏆"; title = "מלך";    desc = "שליטה מעולה! כמעט שלמות!"; }
        else               { emoji = "💎"; title = "אגדה";   desc = "שלמות מוחלטת! אין לך מתחרים!"; }

        binding.tvRankEmoji.setText(emoji);
        binding.tvRankTitle.setText(title);
        binding.tvRankDesc.setText(desc);
    }
}
