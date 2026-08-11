package com.example.easylex.ui.institution;

/**
 * =====================================================================
 * JoinInstitutionFragment — הצטרפות מורה למוסד עם קוד הצטרפות (משימה 0.8)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * מורה שקיבל קוד מוסד (teacherJoinCode, נוצר במסך admin-tool — משימה 0.7)
 * מזין אותו כאן. הפורמט (XXXX-XXXX) מתעצב אוטומטית תוך כדי הקלדה.
 * הכפתור קורא ל-Cloud Function joinAsTeacher (functions/index.js, משימה
 * 0.8) דרך Firebase Callable Functions SDK. בהצלחה — מקדם את המשתמש
 * ל-role: "teacher" בשרת (הפונקציה כותבת ל-Firestore, לא המסך הזה),
 * ומנווט הביתה. בכישלון — מציג את הודעת השגיאה הרלוונטית.
 *
 * איך מגיעים למסך הזה?
 * ----------------------
 * מ-LoginActivity/RegisterActivity ("אני מורה — יש לי קוד מוסד?") →
 * אחרי login/register מוצלח → MainActivity עם EXTRA_OPEN_JOIN_INSTITUTION
 * → ניווט לכאן. לא דרך ה-Bottom Navigation.
 *
 * למה הבדיקה "כבר משויך למוסד" נעשית כאן ולא ב-LoginActivity?
 * --------------------------------------------------------------
 * role לא ניתן לדעת לפני שהמשתמש כבר מחובר — LoginActivity, מטבעה, רצה
 * *לפני* שיש session פעיל. לכן הקישור ב-LoginActivity/RegisterActivity
 * תמיד גלוי, וההגבלה בפועל ("מורה/מנהל שכבר משויך לא צריך את זה")
 * נאכפת כאן, ברגע שה-role כבר ניתן לפתרון דרך UserRoleManager.
 */

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.easylex.MainActivity;
import com.example.easylex.R;
import com.example.easylex.data.UserRoleManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JoinInstitutionFragment extends Fragment {

    /** ה-region שבו נפרסה joinAsTeacher (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";

    private TextView tvInstructions;
    private TextInputLayout joinCodeInputLayout;
    private TextInputEditText etJoinCode;
    private MaterialButton btnJoin;

    /** מונע לולאה אינסופית בין ה-TextWatcher לעיצוב האוטומטי שהוא עצמו מבצע. */
    private boolean isFormattingCode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_join_institution, container, false);

        ((MaterialToolbar) root.findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tvInstructions = root.findViewById(R.id.tvJoinInstructions);
        joinCodeInputLayout = root.findViewById(R.id.joinCodeInputLayout);
        etJoinCode = root.findViewById(R.id.etJoinCode);
        btnJoin = root.findViewById(R.id.btnJoinInstitution);

        setupAutoDashFormatting();
        btnJoin.setOnClickListener(v -> attemptJoin());

        checkExistingRole();

        return root;
    }

    /**
     * checkExistingRole — מורה/מנהל/סופר-אדמין שכבר משויך למוסד לא צריך
     * להצטרף שוב. מסתיר את הטופס ומציג הודעה במקום.
     */
    private void checkExistingRole() {
        UserRoleManager.getInstance().refresh(() -> {
            if (!isAdded()) return;
            String role = UserRoleManager.getInstance().getRole();
            boolean alreadyAssigned = UserRoleManager.ROLE_TEACHER.equals(role)
                    || UserRoleManager.ROLE_PRINCIPAL.equals(role)
                    || UserRoleManager.ROLE_SUPERADMIN.equals(role);
            if (alreadyAssigned) {
                tvInstructions.setText("החשבון שלך כבר משויך למוסד. אין צורך להצטרף שוב.");
                joinCodeInputLayout.setVisibility(View.GONE);
                btnJoin.setVisibility(View.GONE);
            }
        });
    }

    /** מעצב את הקלט אוטומטית לפורמט XXXX-XXXX תוך כדי הקלדה. */
    private void setupAutoDashFormatting() {
        etJoinCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingCode) return;
                isFormattingCode = true;

                String raw = s.toString().toUpperCase(Locale.US).replace("-", "");
                if (raw.length() > 8) raw = raw.substring(0, 8);

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    if (i == 4) formatted.append('-');
                    formatted.append(raw.charAt(i));
                }

                s.replace(0, s.length(), formatted.toString());
                etJoinCode.setSelection(formatted.length());
                isFormattingCode = false;
            }
        });
    }

    private void attemptJoin() {
        String code = etJoinCode.getText() != null ? etJoinCode.getText().toString().trim() : "";
        if (code.length() != 9) { // XXXX-XXXX
            joinCodeInputLayout.setError("קוד לא תקין — צריך להיות בפורמט XXXX-XXXX");
            return;
        }
        joinCodeInputLayout.setError(null);

        btnJoin.setEnabled(false);
        btnJoin.setText("מצטרף...");

        Map<String, Object> data = new HashMap<>();
        data.put("teacherJoinCode", code);

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("joinAsTeacher")
                .call(data)
                .addOnSuccessListener(result -> {
                    if (!isAdded()) return;
                    String institutionName = extractInstitutionName(result.getData());

                    // מרענן את ה-ID token כדי שה-Custom Claims החדשים (role=teacher,
                    // ר' functions/index.js) ייכנסו לתוקף בהקדם האפשרי בצד לקוח.
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) user.getIdToken(true);

                    Toast.makeText(requireContext(),
                            institutionName.isEmpty()
                                    ? "ברוך הבא למוסד!"
                                    : "ברוך הבא ל" + institutionName + "!",
                            Toast.LENGTH_LONG).show();

                    // מפעילים מחדש את MainActivity במקום לנווט ידנית — כך אותה
                    // זרימת "role נטען, ואז נבחר גרף/bottom-nav מתאים" (ר'
                    // MainActivity.onCreate) מטפלת גם כאן, בלי לשכפל את לוגיקת
                    // ההחלפה לגרף המורה (teacher_nav_graph.xml) פעמיים בקוד.
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnJoin.setEnabled(true);
                    btnJoin.setText("הצטרף");
                    joinCodeInputLayout.setError(describeError(e));
                });
    }

    private static String extractInstitutionName(Object resultData) {
        if (resultData instanceof Map) {
            Object name = ((Map<?, ?>) resultData).get("institutionName");
            if (name != null) return name.toString();
        }
        return "";
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            switch (code) {
                case NOT_FOUND:
                    return "קוד ההצטרפות לא נמצא. ודא/י שהקלדת אותו נכון.";
                case RESOURCE_EXHAUSTED:
                    return "יותר מדי ניסיונות הצטרפות. נסה/י שוב בעוד כמה דקות.";
                case UNAUTHENTICATED:
                    return "יש להתחבר מחדש כדי להצטרף.";
                default:
                    return "אירעה שגיאה. נסה/י שוב.";
            }
        }
        return "אירעה שגיאה. נסה/י שוב.";
    }
}
