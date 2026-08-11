package com.example.easylex.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easylex.MainActivity;
import com.example.easylex.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * =====================================================================
 * LoginActivity — מסך הכניסה לאפליקציה
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * מאפשר למשתמש קיים להיכנס לאפליקציה עם אימייל וסיסמה.
 *
 * זרימת הכניסה:
 * -------------
 *   1. המשתמש מזין אימייל + סיסמה
 *   2. לחיצה על "כניסה" → loginUser()
 *   3. בדיקה שהשדות לא ריקים
 *   4. mAuth.signInWithEmailAndPassword() → Firebase מאמת
 *   5. הצלחה → MainActivity (+ finish() לסגירת מסך הכניסה)
 *   6. כישלון → Toast עם הודעת שגיאה
 *
 * אין חשבון?
 * ----------
 * לחיצה על "הירשם" → RegisterActivity.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * SplashActivity — מנווטת אליה כשהמשתמש לא מחובר.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;

    /**
     * true אם המשתמש לחץ "אני מורה — יש לי קוד מוסד" (משימה 0.8) —
     * אחרי התחברות מוצלחת ננווט למסך ההצטרפות במקום למסך הבית הרגיל.
     */
    private boolean pendingJoinAsTeacher = false;

    /**
     * true אם המשתמש לחץ "יש לי קוד כיתה" (משימה 0.10) — אחרי התחברות
     * מוצלחת ננווט למסך ההצטרפות לכיתה במקום למסך הבית הרגיל.
     */
    private boolean pendingJoinClass = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> loginUser());

        findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // משימה 0.8: אותה זרימת התחברות קיימת בדיוק — רק מסמן כוונה להצטרף כמורה.
        findViewById(R.id.tvJoinAsTeacher).setOnClickListener(v -> {
            pendingJoinAsTeacher = true;
            loginUser();
        });

        // משימה 0.10: אותה זרימת התחברות קיימת בדיוק — רק מסמן כוונה להצטרף לכיתה.
        findViewById(R.id.tvJoinClass).setOnClickListener(v -> {
            pendingJoinClass = true;
            loginUser();
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        if (pendingJoinAsTeacher) {
                            intent.putExtra(MainActivity.EXTRA_OPEN_JOIN_INSTITUTION, true);
                        } else if (pendingJoinClass) {
                            intent.putExtra(MainActivity.EXTRA_OPEN_JOIN_CLASS, true);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "התחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}