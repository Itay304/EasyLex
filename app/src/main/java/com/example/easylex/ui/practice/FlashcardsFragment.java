package com.example.easylex.ui.practice;

/**
 * =====================================================================
 * FlashcardsFragment — מסך הכרטיסיות (Flashcards)
 * =====================================================================
 *
 * מה עושה המסך הזה?
 * ------------------
 * מציג למשתמש כרטיסיות לימוד דיגיטליות — בדומה לכרטיסיות נייר.
 * כל כרטיסייה מציגה מילה באנגלית בחזית, ולאחר הקשה — את התרגום
 * לעברית ומשפט דוגמה בצידה האחורי.
 *
 * איך משתמשים בה?
 * ----------------
 * • הקשה על הכרטיסייה  ← הפיכה (חזית / אחורית)
 * • החלקה ימינה        ← "ידעתי" — ניקוד ו-XP
 * • החלקה שמאלה        ← "לא ידעתי" — מסומן לתיקון
 * • כפתור "המילה הבאה" ← דילוג ללא ניקוד
 *
 * סדר המילים — אדפטיבי:
 * -----------------------
 * מילים קשות (רמת שליטה 0–2) מוצגות ראשונות,
 * מילים חדשות (אף ניסיון) — אחריהן,
 * מילים שנלמדו טוב (3–5) — אחרונות.
 * כך המשתמש מתמודד עם החומר הקשה כשהוא עדיין ממוקד.
 *
 * שמירת נתונים:
 * --------------
 * בכל החלקה ("ידעתי"/"לא ידעתי") מתעדכנים שדות correctAttempts
 * ו-totalAttempts בטבלת Room — נוסחת השליטה (Mastery) מתחשבת בהם.
 * =====================================================================
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
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
import android.widget.Toast;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FlashcardsFragment extends Fragment {

    // ── שדות מחלקה (משתנים) ──────────────────────────────────────────────────

    /** ViewModel — שכבת הגישה לנתונים. מנוהל ע"י Android ולא נהרס בסיבוב מסך. */
    private MyWordsViewModel viewModel;

    /** רשימת כל המילים לסשן הנוכחי, ממוינת לפי עדיפות לימוד. */
    private List<Word> words = new ArrayList<>();

    /** אינדקס המילה הנוכחית ברשימה (מתחיל מ-0). */
    private int currentIndex = 0;

    /** מונה מילים שהמשתמש השיב "ידעתי" עליהן — מוצג בסוף הסשן. */
    private int score = 0;

    /** האם כרגע מוצג צד חזית הכרטיסייה (אנגלית)? false = מוצג הצד האחורי (עברית). */
    private boolean isShowingFront = true;

    /** הכרטיסייה עצמה — האלמנט הויזואלי שמוחלק ומוהפך. */
    private MaterialCardView flashCard;

    /** מנוע Text-To-Speech — קריאת המילה הנוכחית בקול. */
    private TextToSpeech tts;

    /** מיקום X המקורי של הכרטיסייה (אחרי layout) — נשמר כדי לחזור למרכז אחרי כל החלקה. */
    private float cardRestingX = -1f;

    /** layoutFront = ה-View של צד האנגלית | layoutBack = ה-View של צד העברית. */
    private View layoutFront, layoutBack;

    /** tvFront = מילה באנגלית | tvBack = תרגום לעברית | tvEx = משפט דוגמה | tvCount = "3 / 1902" */
    private TextView tvFront, tvBack, tvEx, tvCount;

    /**
     * משתנים לחישוב תנועת המגע:
     * dX       = מרחק בין אצבע לקצה הכרטיסייה (להזזה חלקה)
     * startRawX/Y = מיקום האצבע ברגע הנגיעה הראשונה (לזיהוי טאפ מול החלקה)
     */
    private float dX, startRawX, startRawY;

    // ── אתחול המסך ───────────────────────────────────────────────────────────

    /**
     * onCreateView — נקרא ע"י Android בעת יצירת המסך.
     * כאן מחברים את כל אלמנטי ה-XML לקוד ומגדירים את ההתנהגויות.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // מנפחים (inflate) את קובץ ה-XML של המסך לאובייקט View שניתן לעבוד איתו
        View root = inflater.inflate(R.layout.fragment_flashcards, container, false);

        // חיבור כפתור חזרה בסרגל העליון
        ((MaterialToolbar) root.findViewById(R.id.toolbar))
            .setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // חיבור כל אלמנטי ה-UI לפי ה-ID שלהם מה-XML
        flashCard   = root.findViewById(R.id.flashCard);
        layoutFront = root.findViewById(R.id.layoutFront);
        layoutBack  = root.findViewById(R.id.layoutBack);
        tvFront     = root.findViewById(R.id.tvFront);
        tvBack      = root.findViewById(R.id.tvBack);
        tvEx        = root.findViewById(R.id.tvEx);
        tvCount     = root.findViewById(R.id.tvFlashCount);

        // אתחול TTS
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        // כפתור רמקול בכרטיסיית האנגלית
        ImageButton btnFlashSpeak = root.findViewById(R.id.btnFlashSpeak);
        btnFlashSpeak.setOnClickListener(v -> speakCurrentWord());

        // שומרים את X הטבעי של הכרטיסייה אחרי Layout — כדי לחזור למרכז בכל reset
        flashCard.post(() -> { if (cardRestingX < 0) cardRestingX = flashCard.getX(); });

        // ── טעינת מילים מה-DB ────────────────────────────────────────────────
        // ViewModelProvider מחזיר את ה-ViewModel הקיים (או יוצר אחד חדש).
        // observe() = "הקשב לשינויים ברשימת המילים וקרא לי כשיש עדכון".
        // הרשימה מגיעה מ-Room (SQLite מקומי) בצורה אסינכרונית.
        viewModel = new ViewModelProvider(this).get(MyWordsViewModel.class);
        viewModel.getAllWords().observe(getViewLifecycleOwner(), w -> {
            // טוענים את המילים פעם אחת בלבד (words.isEmpty() מונע טעינה חוזרת)
            if (w != null && words.isEmpty()) {
                // משימה 0.15 (המשך) — אם הגענו ממשימה שהמורה הקצה (PracticeTypeBottomSheet),
                // מסננים ל-wordIds/listId של המשימה בלבד לפני המיון האדפטיבי.
                List<Word> pool = filterForAssignment(new ArrayList<>(w));
                if (pool.isEmpty() && isAdded()) {
                    Toast.makeText(requireContext(), "אין מילים לתרגול זה", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                    return;
                }
                // Order: hard words (mastery 0-2) first, new words second, solid (3-5) last.
                // This means high-mastery words naturally appear LATER in the session,
                // giving harder words more exposure time.
                words = orderByPriority(pool);
                showWord(); // מציגים את הכרטיסייה הראשונה
            }
        });

        // ── כפתור "המילה הבאה" — דילוג ללא ניקוד ────────────────────────────
        MaterialButton btnNext = root.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> advanceCard());

        // ── ערכי סף למגע — בדפ (dp = יחידת מסך עצמאית-צפיפות) ───────────────
        // density הופך dp לפיקסלים אמיתיים בהתאם לצפיפות המסך הספציפי
        float density     = getResources().getDisplayMetrics().density;
        int tapSlop       = (int) (10 * density);  // 10dp — movement within this = tap
        int swipeMinDist  = (int) (80 * density);  // 80dp — minimum horizontal swipe distance

        // ── מאזין מגע יחיד לכרטיסייה — מטפל גם בטאפ וגם בהחלקה ────────────────
        // Single touch listener handles both tap (flip) and swipe (advance).
        // Returns true to consume the event so the click listener never fires accidentally.
        flashCard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                // ACTION_DOWN — האצבע נגעה במסך
                case MotionEvent.ACTION_DOWN:
                    startRawX = event.getRawX(); // שומרים מיקום התחלתי לחישוב מרחק ההחלקה
                    startRawY = event.getRawY();
                    dX = v.getX() - event.getRawX(); // offset בין מיקום הכרטיסייה לאצבע
                    break;

                // ACTION_MOVE — האצבע נעה (גרירה)
                case MotionEvent.ACTION_MOVE: {
                    float newX = event.getRawX() + dX;
                    v.setX(newX); // הכרטיסייה עוקבת אחרי האצבע
                    v.setRotation(newX / 20f); // natural tilt while dragging — הטיה טבעית בגרירה

                    // שינוי צבע מסגרת הכרטיסייה כאות חזותי למשתמש
                    if      (newX >  60) flashCard.setStrokeColor(Color.GREEN);  // ימינה = ירוק
                    else if (newX < -60) flashCard.setStrokeColor(Color.RED);    // שמאלה = אדום
                    else                 flashCard.setStrokeColor(Color.TRANSPARENT); // אמצע = שקוף
                    break;
                }

                // ACTION_UP — האצבע הורמה מהמסך — כאן מחליטים מה קרה
                case MotionEvent.ACTION_UP: {
                    float dx = event.getRawX() - startRawX; // כמה זזנו אופקית
                    float dy = event.getRawY() - startRawY; // כמה זזנו אנכית

                    if (Math.abs(dx) < tapSlop && Math.abs(dy) < tapSlop) {
                        // תנועה קטנה מאוד = טאפ רגיל → הפיכת כרטיסייה
                        // Tap: snap back and flip
                        v.setX(cardRestingX);
                        v.setRotation(0);
                        flashCard.setStrokeColor(Color.TRANSPARENT);
                        flipCard();

                    } else if (Math.abs(dx) >= swipeMinDist && Math.abs(dx) > Math.abs(dy)) {
                        // תנועה אופקית גדולה מספיק = החלקה → ידעתי/לא ידעתי
                        // Horizontal swipe: right = known ✓, left = unknown ✗
                        handleSwipe(dx > 0); // dx חיובי = ימינה = ידעתי

                    } else {
                        // תנועה שאינה לא טאפ ולא החלקה → חזרת הכרטיסייה למקומה
                        // Not enough movement — spring back
                        v.animate().x(cardRestingX).rotation(0).setDuration(200).start();
                        flashCard.setStrokeColor(Color.TRANSPARENT);
                    }
                    break;
                }
            }
            return true; // consume — prevents double-firing with click listener
            // "true" אומר: "טיפלתי באירוע, אל תעביר אותו לאף מאזין אחר"
        });

        return root;
    }

    // ── סינון למשימה (ASSIGNMENT) ───────────────────────────────────────────

    /**
     * filterForAssignment — אם practiceType="ASSIGNMENT" ב-Bundle (הועבר מ-
     * PracticeTypeBottomSheet, ר' StudentAssignmentsFragment/InstitutionalHomeFragment),
     * מצמצם את ה-pool ל-listId/wordIds של המשימה בלבד. אחרת מחזיר את כל המילים
     * ללא שינוי — זהה להתנהגות הקודמת. אותה לוגיקת סינון בדיוק כמו
     * QuizFragment.prepareQuiz's ASSIGNMENT branch.
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

    // ── מיון אדפטיבי — מילים קשות ראשון ────────────────────────────────────

    /**
     * orderByPriority — ממיינת את רשימת המילים לפי עדיפות לימוד:
     *   1. מילים קשות  (mastery 0–2) — ראשונות
     *   2. מילים חדשות (אף ניסיון)  — שניות
     *   3. מילים חזקות (mastery 3–5) — אחרונות
     *
     * בתוך כל קבוצה הסדר אקראי (shuffle) — למניעת שינון מיכני לפי סדר.
     * כך המשתמש מתמודד עם החומר הקשה בתחילת הסשן כשקשב מרבי.
     *
     * @param pool רשימת כל המילים לפני המיון
     * @return רשימה ממוינת לפי עדיפות
     */
    private List<Word> orderByPriority(List<Word> pool) {
        List<Word> hard  = new ArrayList<>(); // מילים קשות
        List<Word> newW  = new ArrayList<>(); // מילים חדשות לגמרי
        List<Word> solid = new ArrayList<>(); // מילים שנלמדו היטב

        // מסווגים כל מילה לאחת משלוש הקבוצות
        for (Word w : pool) {
            if (w.getTotalAttempts() == 0)     newW.add(w);  // מעולם לא נוסתה
            else if (w.getMasteryLevel() <= 2) hard.add(w);  // שליטה נמוכה
            else                               solid.add(w); // שליטה טובה
        }

        // מערבבים כל קבוצה בנפרד — שינוי הסדר בתוך הקבוצה בלי לשבור את העדיפות
        Collections.shuffle(hard);
        Collections.shuffle(newW);
        Collections.shuffle(solid);

        // מרכיבים את הרשימה הסופית: קשות → חדשות → חזקות
        List<Word> ordered = new ArrayList<>();
        ordered.addAll(hard);
        ordered.addAll(newW);
        ordered.addAll(solid);
        return ordered;
    }

    // ── תצוגה ────────────────────────────────────────────────────────────────

    /**
     * showWord — מציגה את המילה הנוכחית לפי currentIndex.
     * אם עברנו את כל המילים — מציגה הודעת סיום וחוזרת למסך הקודם.
     */
    private void showWord() {
        if (currentIndex < words.size()) {
            // עדיין יש מילים — טוענים ומציגים
            Word w = words.get(currentIndex);
            tvFront.setText(w.getEnglishWord());        // צד אנגלית
            tvBack.setText(w.getHebrewTranslation());   // צד עברית (מוסתר עד הפיכה)
            tvEx.setText(w.getExampleSentence());       // משפט דוגמה (מוסתר עד הפיכה)
            tvCount.setText((currentIndex + 1) + " / " + words.size()); // "3 / 1902"
        } else {
            // סיימנו את כל המילים — מציגים דיאלוג תוצאה
            if (isAdded()) showFlashResult();
        }
    }

    /**
     * advanceCard — מעביר לכרטיסייה הבאה ללא ניקוד.
     * מופעל ע"י כפתור "המילה הבאה".
     * מבצע אנימציית יציאה (הכרטיסייה עפה ימינה ונעלמת) לפני הטעינה הבאה.
     */
    private void advanceCard() {
        flashCard.animate().cancel(); // מפסיק כל אנימציה קודמת שעדיין רצה
        float exitX = cardRestingX >= 0 ? cardRestingX + 1200 : 1200;
        flashCard.animate()
                .x(exitX)       // הכרטיסייה עפה ימינה מחוץ למסך
                .rotation(15f)  // מסתובבת מעט בזמן היציאה — אפקט טבעי
                .alpha(0)       // הופכת שקופה
                .setDuration(250) // 250 אלפיות שנייה — מהיר ונקי
                .withEndAction(() -> { currentIndex++; resetCard(); showWord(); }) // אחרי האנימציה
                .start();
    }

    /**
     * handleSwipe — מטפל בהחלקה ימינה ("ידעתי") או שמאלה ("לא ידעתי").
     *
     * מה קורה כשהמשתמש "ידע":
     *   • correctAttempts++ ו-totalAttempts++ → משפיע על נוסחת ה-Mastery
     *   • GamificationEngine מקבל עדכון → מחלק XP (+10, ואם עלה ל-Mastery 5: +100)
     *   • score++ → מוצג בסוף הסשן
     *
     * מה קורה כשלא "ידע":
     *   • totalAttempts++ בלבד → נוסחת ה-Mastery תרד
     *   • errorInFlashcards = true → המילה תופיע ב"תיקון טעויות"
     *
     * @param known true = ימינה = ידעתי | false = שמאלה = לא ידעתי
     */
    private void handleSwipe(boolean known) {
        Word w = words.get(currentIndex);

        // Snapshot BEFORE updating — required for mastery-crossing detection in GamificationEngine
        // שומרים את המצב לפני העדכון — ה-GamificationEngine צריך להשוות "לפני" ו"אחרי"
        // כדי לזהות אם הגענו לרמת שליטה חדשה ולתת בונוס XP בהתאם
        Word wordBefore = new Word();
        wordBefore.setCorrectAttempts(w.getCorrectAttempts());
        wordBefore.setTotalAttempts(w.getTotalAttempts());

        // מעדכנים את נתוני המילה
        w.setTotalAttempts(w.getTotalAttempts() + 1); // תמיד — גם אם טעה
        w.setErrorInFlashcards(!known); // true אם לא ידע → יסומן בתיקון טעויות
        if (known) {
            w.setCorrectAttempts(w.getCorrectAttempts() + 1); // רק אם ידע
            GamificationEngine.getInstance(requireContext()).onCorrectAnswer(wordBefore, w);
            score++;
        }
        viewModel.update(w); // שומרים את השינוי ב-Room (DB מקומי) ברקע

        // אנימציית יציאה — כיוון ועיקול בהתאם לתשובה
        flashCard.animate().cancel();
        float base = cardRestingX >= 0 ? cardRestingX : 0;
        flashCard.animate()
                .x(known ? base + 1200 : base - 1200) // ימינה אם ידע, שמאלה אם לא
                .rotation(known ? 20f : -20f)          // עיקול בכיוון היציאה
                .alpha(0)
                .setDuration(280)
                .withEndAction(() -> { currentIndex++; resetCard(); showWord(); })
                .start();
    }

    /**
     * resetCard — מאפס את הכרטיסייה למצב ברירת מחדל לפני הצגת המילה הבאה.
     *
     * סדר הפעולות קריטי:
     * 1. מסתירים את הצד האחורי ראשון — מונע "בזק" (flash) של תוכן מוסתר
     * 2. רק אחר כך מחזירים את הכרטיסייה למיקום ולאטימות המקוריים
     *
     * בלי הסדר הזה: Android מציג פריים אחד שבו הכרטיסייה חזרה למיקום אבל
     * הצד האחורי עדיין גלוי — מה שגורם לאנגלית להיראות הפוכה כמו מראה.
     */
    private void resetCard() {
        // Cancel any in-progress animations before resetting state
        flashCard.animate().cancel();

        // Hide and un-rotate the back face FIRST, so it can never flash while the
        // card snaps back to its resting position
        layoutBack.setRotationY(0);                   // מבטלים את הסיבוב הנגדי של הצד האחורי
        layoutBack.setVisibility(View.INVISIBLE);      // מסתירים את הצד האחורי
        layoutFront.setRotationY(0);
        layoutFront.setVisibility(View.VISIBLE);       // מציגים את הצד הקדמי

        // Restore card position, tilt, and opacity — רק עכשיו בטוח לאפס את הכרטיסייה
        flashCard.setX(cardRestingX >= 0 ? cardRestingX : 0); // חזרה למרכז (מיקום הטבעי)
        flashCard.setRotation(0);   // ישר — ללא הטיה
        flashCard.setAlpha(1);      // גלוי לגמרי
        flashCard.setRotationY(0);  // לא מסובב על ציר Y
        flashCard.setStrokeColor(Color.TRANSPARENT); // אין מסגרת צבעונית
        isShowingFront = true;      // חוזרים לצד הקדמי
    }

    /**
     * flipCard — מבצע אנימציית הפיכה (כמו הפיכת כרטיסיית נייר).
     *
     * איך זה עובד טכנית:
     *   • הכרטיסייה מסתובבת 180° סביב ציר Y (ציר אנכי) — כמו הפיכת ספר
     *   • באמצע הסיבוב (כשהכרטיסייה "פונה הצידה") מחליפים בין layoutFront ו-layoutBack
     *   • layoutBack מקבל rotationY=180° נגדי — כך התוכן נקרא בכיוון הנכון ולא מתהפך
     *
     * לדוגמה: אם הכרטיסייה סובבת ל-180°, הצד האחורי סובב ל-180° נגד הכיוון →
     * 180° + 180° = 360° = נראה ישר.
     */
    private void flipCard() {
        // Cancel any ongoing animation (e.g. a previous incomplete flip)
        flashCard.animate().cancel();

        // אם מציגים חזית → נסובב ל-180°; אם מציגים גב → נסובב חזרה ל-0°
        float rotation = isShowingFront ? 180f : 0f;

        flashCard.animate().rotationY(rotation).setDuration(300).withEndAction(() -> {
            // הקוד הזה רץ בסיום האנימציה — כשהכרטיסייה כבר "הפוכה"
            if (isShowingFront) {
                // עברנו מחזית לאחורית
                layoutFront.setVisibility(View.INVISIBLE);
                layoutBack.setVisibility(View.VISIBLE);
                layoutBack.setRotationY(180f); // counter-rotate so content reads normally
            } else {
                // עברנו מאחורית לחזית
                layoutBack.setRotationY(0);
                layoutBack.setVisibility(View.INVISIBLE);
                layoutFront.setVisibility(View.VISIBLE);
            }
            isShowingFront = !isShowingFront; // מחליפים מצב
        }).start();
    }

    /** speakCurrentWord — קורא את המילה הנוכחית באנגלית דרך TTS (בכפוף להגדרת TTS). */
    private void speakCurrentWord() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsFragment.KEY_TTS_ENABLED, true)) return;
        if (tts != null && currentIndex < words.size()) {
            tts.speak(words.get(currentIndex).getEnglishWord(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /** showFlashResult — דיאלוג תוצאה יפה בסיום סשן הכרטיסיות. */
    private void showFlashResult() {
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

        card.setCardBackgroundColor(win ? 0xFF1565C0 : 0xFFBF360C); // blue win, orange loss
        tvEmoji.setText(win ? "🎉" : "💪");
        tvTitle.setText(win ? "כרטיסיות כבושות!" : "אל תוותר!");
        tvScore.setText(score + "/" + total);
        tvMsg.setText(win
                ? "ידעת " + pct + "% מהמילים — מרשים!"
                : "ידעת " + pct + "%\nחזור על המילים שלא זכרת!");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v).setCancelable(false).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnDone.setOnClickListener(vv -> { dialog.dismiss(); requireActivity().onBackPressed(); });
        btnRetry.setOnClickListener(vv -> {
            dialog.dismiss();
            currentIndex = 0; score = 0;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
