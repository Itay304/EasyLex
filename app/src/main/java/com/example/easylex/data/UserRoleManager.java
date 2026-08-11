package com.example.easylex.data;

/**
 * =====================================================================
 * UserRoleManager — רזולוור תפקיד מרכזי (משימה 0.2 באיפיון הפלטפורמה)
 * =====================================================================
 *
 * מה זה?
 * -------
 * מקור אמת יחיד, בזיכרון, לתפקיד המשתמש הנוכחי (role), למוסד שלו
 * (institutionId) ולכיתות שלו (classIds). טוען את users/{uid} מ-Firestore
 * פעם אחת לפי דרישה (refresh), ומאותו רגע חושף את הערכים המטומנים
 * לכל מסך באפליקציה — במקום שכל מסך יבצע קריאת Firestore אד-הוקית
 * משלו (כפי שעשה ProfileFragment.checkIfAdmin() עד כה).
 *
 * ברירת מחדל בטוחה:
 * -------------------
 * כל עוד הטעינה לא הסתיימה (או נכשלה, או אין משתמש מחובר) — role
 * מחזיר "student". זו נקודת הזהירות המפורשת מהלקח התיעודי ב-
 * MainActivity.java (בדיקת תפקיד מוקדמת מדי ב-lifecycle גרמה לקריסה
 * בעבר) — קורא לא-זהיר שישאל getRole() לפני שהטעינה הסתיימה מקבל
 * את התפקיד הכי פחות מורשה, לא null/קריסה.
 *
 * Pattern: Singleton (זהה ל-GamificationEngine).
 * =====================================================================
 */

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserRoleManager {

    // ── ערכי role אפשריים ────────────────────────────────────────────────────
    public static final String ROLE_STUDENT    = "student";
    public static final String ROLE_TEACHER    = "teacher";
    public static final String ROLE_PRINCIPAL  = "principal";
    public static final String ROLE_SUPERADMIN = "superadmin";

    private static volatile UserRoleManager instance;

    private final FirebaseFirestore db;

    /** LiveData לצרכנים ריאקטיביים — מתעדכן כל פעם ש-refresh() מסתיים בהצלחה. */
    private final MutableLiveData<String> roleLiveData = new MutableLiveData<>(ROLE_STUDENT);

    // ערכים מטומנים בזיכרון — ברירת מחדל בטוחה עד שנטען מידע אמיתי.
    private volatile String role          = ROLE_STUDENT;
    private volatile String institutionId = null;
    private volatile List<String> classIds = Collections.emptyList();
    private volatile boolean loaded  = false;
    private volatile boolean loading = false;

    private UserRoleManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static UserRoleManager getInstance() {
        if (instance == null) {
            synchronized (UserRoleManager.class) {
                if (instance == null) instance = new UserRoleManager();
            }
        }
        return instance;
    }

    // ── קריאה מהירה (מטומנת) — בטוחה לקריאה מכל שלב ב-lifecycle ─────────────

    /** התפקיד הנוכחי, או "student" אם עוד לא נטען / אין משתמש מחובר. */
    public String getRole() { return role; }

    /** מזהה המוסד, או null אם עוד לא נטען / המשתמש עצמאי. */
    @Nullable
    public String getInstitutionId() { return institutionId; }

    /** רשימת מזהי הכיתות של המשתמש (ריקה אם עוד לא נטען / אין כיתות). */
    public List<String> getClassIds() { return classIds; }

    /** true אם refresh() כבר הסתיים לפחות פעם אחת (בהצלחה או לא). */
    public boolean isLoaded() { return loaded; }

    /** LiveData לצרכנים שרוצים להאזין לעדכוני תפקיד (למשל אחרי refresh()). */
    public LiveData<String> getRoleLiveData() { return roleLiveData; }

    // ── טעינה מ-Firestore ────────────────────────────────────────────────────

    /**
     * refresh — טוענת את users/{uid} של המשתמש המחובר כרגע ומעדכנת את המטמון.
     * בטוחה לקריאה חוזרת (כל קריאה פשוט טוענת מחדש). Fire-and-forget בסגנון
     * GamificationEngine.syncFromFirestore(): לא זורקת, לא חוסמת.
     *
     * תיקון באג: לפני קריאת המסמך, מרעננים בכפייה את ה-ID token
     * (getIdToken(true)). role/institutionId עצמם כאן נקראים מהמסמך ב-
     * Firestore, לא מה-token — אבל Security Rules אחרות באפליקציה
     * (firestore.rules, החל ממשימה 0.6) כן נשענות על request.auth.token.role
     * /institutionId. בלי רענון מפורש, הטוקן המקומי יכול להישאר עם Custom
     * Claims ישנים עד שעה (עד לרענון האוטומטי הבא של Firebase) — כך שגם
     * אם UserRoleManager עצמו "ידע" נכון שהמשתמש הוא מורה, קריאות
     * Firestore אחרות שגייטד לפי role יכולות עדיין להיכשל ב-PERMISSION_DENIED
     * עד שהטוקן יתעדכן. refresh() הוא המקום הטבעי לוודא את זה — הוא רץ
     * בכל כניסה לאפליקציה (MainActivity) ולפני שמשתמשים בתפקיד בכל מסך.
     *
     * @param onComplete Runnable שיופעל בסיום (הצלחה, כישלון, או אין משתמש) —
     *                   מופעל על אותו Thread שבו Firestore מפעיל את ה-listener
     *                   (ברירת המחדל של Firebase Android SDK: ה-Main Thread).
     */
    public void refresh(@Nullable Runnable onComplete) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            resetToDefaults();
            loaded = true;
            if (onComplete != null) onComplete.run();
            return;
        }

        loading = true;
        // force refresh (true) — לא מסתפקים בטוקן המקומי המטומן, גם אם
        // הוא עדיין "לא פג תוקף" טכנית. addOnCompleteListener (לא
        // addOnSuccessListener) — ממשיכים לקרוא את המסמך גם אם רענון
        // הטוקן עצמו נכשל (למשל בעיית רשת חולפת), כדי לא להישאר תקועים.
        user.getIdToken(true)
                .addOnCompleteListener(tokenTask -> loadUserDocument(user, onComplete));
    }

    private void loadUserDocument(FirebaseUser user, @Nullable Runnable onComplete) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String r = doc.getString("role");
                        role = (r != null && !r.isEmpty()) ? r : ROLE_STUDENT;
                        institutionId = doc.getString("institutionId");
                        classIds = readStringList(doc.get("classIds"));
                    } else {
                        resetToDefaults();
                    }
                    loaded  = true;
                    loading = false;
                    roleLiveData.postValue(role);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    // כישלון רשת — משאירים את הערכים הבטוחים האחרונים שהיו במטמון,
                    // לא קורסים ולא מסמנים loaded=true (כדי שניסיון הבא ינסה שוב).
                    loading = false;
                    if (onComplete != null) onComplete.run();
                });
    }

    // ── עזר ──────────────────────────────────────────────────────────────────

    private void resetToDefaults() {
        role = ROLE_STUDENT;
        institutionId = null;
        classIds = Collections.emptyList();
    }

    /** ממיר Object שהוחזר מ-DocumentSnapshot.get() לרשימת String בטוחה. */
    private static List<String> readStringList(Object raw) {
        if (!(raw instanceof List)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object o : (List<?>) raw) {
            if (o instanceof String) result.add((String) o);
        }
        return result;
    }
}
