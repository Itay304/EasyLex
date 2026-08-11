package com.example.easylex.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.easylex.MainActivity;
import com.example.easylex.R;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * =====================================================================
 * SplashActivity — מסך הפתיחה (Splash Screen)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * זהו המסך הראשון שנפתח עם האפליקציה. הוא עושה שלושה דברים במקביל:
 *   1. מציג אנימציית לוגו (scale + fade, 700ms)
 *   2. בודק אם יש עדכון חובה (Firestore: config/app_settings.min_version)
 *   3. בודק אם המשתמש כבר מחובר (Firebase Auth)
 *
 * מנגנון הניווט:
 * ---------------
 * שני דגלים: animDone ו-authReady.
 * tryProceed() נקרא בכל פעם שאחד מהם מתעדכן.
 * ניווט מתבצע רק כשגם animDone=true וגם authReady=true.
 * כך האנימציה תמיד מסתיימת לפני המעבר — גם אם ה-Auth מהיר יותר.
 *
 * לאן מנווטים?
 * ------------
 *   goToMain=true  → MainActivity (המשתמש מחובר)
 *   goToMain=false → LoginActivity (המשתמש לא מחובר)
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * AndroidManifest: LAUNCHER Activity — נקודת הכניסה לאפליקציה.
 * ProfileFragment — קורא ל-SplashActivity בהתנתקות.
 */
public class SplashActivity extends AppCompatActivity {

    // Both checks run in parallel; we navigate only when BOTH are ready
    private boolean authReady = false;
    private boolean animDone  = false;
    private boolean goToMain  = false;  // true → MainActivity, false → LoginActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startSplashAnimation();
        checkAppVersion();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Animation
    // ──────────────────────────────────────────────────────────────────────────

    private void startSplashAnimation() {
        ImageView ivLogo   = findViewById(R.id.ivLogo);
        TextView  tvName   = findViewById(R.id.tvAppName);

        // Logo: scale 0.4→1 + fade in, 700ms
        ivLogo.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(700)
            .start();

        // App name: fade in with 400ms delay, 600ms duration
        tvName.animate()
            .alpha(1f)
            .setStartDelay(400)
            .setDuration(600)
            .withEndAction(() -> {
                animDone = true;
                tryProceed();
            })
            .start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Version + auth checks (unchanged logic, refactored into tryProceed)
    // ──────────────────────────────────────────────────────────────────────────

    private void checkAppVersion() {
        int currentVersion = com.example.easylex.BuildConfig.VERSION_CODE;
        FirebaseFirestore.getInstance().collection("config").document("app_settings")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    long minVersion = doc.getLong("min_version");
                    String updateUrl = doc.getString("update_url");
                    if (currentVersion < minVersion) {
                        showForceUpdateDialog(updateUrl);
                        return;
                    }
                }
                resolveAuth();
            })
            .addOnFailureListener(e -> resolveAuth());
    }

    private void resolveAuth() {
        goToMain  = (FirebaseAuth.getInstance().getCurrentUser() != null);
        authReady = true;
        tryProceed();
    }

    private void tryProceed() {
        if (!authReady || !animDone) return;

        if (goToMain) {
            // Fire-and-forget gamification sync so data is warm when app opens
            GamificationEngine.getInstance(this).syncFromFirestore(null);
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private void showForceUpdateDialog(String url) {
        new AlertDialog.Builder(this)
            .setTitle("נדרש עדכון גרסה")
            .setMessage("אתה משתמש בגרסה ישנה. חובה לעדכן כדי להמשיך.")
            .setCancelable(false)
            .setPositiveButton("עדכן עכשיו", (dialog, which) -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                finish();
            })
            .show();
    }
}
