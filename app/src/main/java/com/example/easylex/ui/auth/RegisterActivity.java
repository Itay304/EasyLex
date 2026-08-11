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
 * RegisterActivity — מסך ההרשמה לאפליקציה
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * מאפשר למשתמש חדש ליצור חשבון ב-Firebase Authentication.
 *
 * זרימת ההרשמה:
 * -------------
 *   1. המשתמש מזין אימייל + סיסמה
 *   2. לחיצה על "הירשם" → registerUser()
 *   3. בדיקות ולידציה:
 *      א. שדות לא ריקים
 *      ב. פורמט אימייל תקין (Patterns.EMAIL_ADDRESS)
 *      ג. סיסמה לפחות 6 תווים
 *   4. mAuth.createUserWithEmailAndPassword() → Firebase יוצר חשבון
 *   5. הצלחה → MainActivity + finishAffinity() (מנקה את כל ה-Back Stack)
 *   6. כישלון → Toast עם הודעת שגיאה
 *
 * finishAffinity() — למה חשוב?
 * ------------------------------
 * מסגר את כל המסכים הקודמים (Splash + Login + Register) ממחסנית הניווט.
 * כך המשתמש לא יכול ללחוץ "חזרה" ולחזור למסך הכניסה.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * LoginActivity — מנווטת אליה כשהמשתמש לוחץ "הירשם".
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;

    /**
     * true אם המשתמש לחץ "אני מורה — יש לי קוד מוסד" (משימה 0.8) —
     * אחרי הרשמה מוצלחת ננווט למסך ההצטרפות במקום למסך הבית הרגיל.
     */
    private boolean pendingJoinAsTeacher = false;

    /**
     * true אם המשתמש לחץ "יש לי קוד כיתה" (משימה 0.10) — אחרי הרשמה
     * מוצלחת ננווט למסך ההצטרפות לכיתה במקום למסך הבית הרגיל.
     */
    private boolean pendingJoinClass = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerUser());

        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> finish());

        // משימה 0.8: אותה זרימת הרשמה קיימת בדיוק — רק מסמן כוונה להצטרף כמורה.
        findViewById(R.id.tvJoinAsTeacherReg).setOnClickListener(v -> {
            pendingJoinAsTeacher = true;
            registerUser();
        });

        // משימה 0.10: אותה זרימת הרשמה קיימת בדיוק — רק מסמן כוונה להצטרף לכיתה.
        findViewById(R.id.tvJoinClassReg).setOnClickListener(v -> {
            pendingJoinClass = true;
            registerUser();
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. בדיקה שהשדות לא ריקים
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 🔥 בדיקה שהאימייל בפורמט תקין (חדש)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "נא להזין כתובת אימייל תקינה (למשל name@email.com)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. בדיקת אורך סיסמה
        if (password.length() < 6) {
            Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
            return;
        }

        // אם הכל תקין - שולחים ל-Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "הרשמה הצליחה!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        if (pendingJoinAsTeacher) {
                            intent.putExtra(MainActivity.EXTRA_OPEN_JOIN_INSTITUTION, true);
                        } else if (pendingJoinClass) {
                            intent.putExtra(MainActivity.EXTRA_OPEN_JOIN_CLASS, true);
                        }
                        startActivity(intent);
                        finishAffinity();
                    } else {
                        Toast.makeText(RegisterActivity.this, "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
