package com.example.easylex.ui.institution;

/**
 * =====================================================================
 * JoinClassFragment — הצטרפות תלמיד לכיתה עם קוד כיתה (משימה 0.10)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * תלמיד שקיבל קוד כיתה (joinCode, נוצר על ידי מורה — משימה 0.9) מזין
 * אותו כאן. הפורמט (XX-XXXXX) מתעצב אוטומטית תוך כדי הקלדה. הכפתור
 * קורא ל-Cloud Function joinClass (functions/index.js, משימה 0.10) דרך
 * Firebase Callable Functions SDK. בהצלחה — מוסיף את הכיתה למשתמש בשרת
 * (role נשאר student, לא משתנה) ומנווט לדשבורד. בכישלון — מציג הודעת
 * שגיאה רלוונטית.
 *
 * איך מגיעים למסך הזה?
 * ----------------------
 * מ-LoginActivity/RegisterActivity ("יש לי קוד כיתה") → אחרי
 * login/register מוצלח → MainActivity עם EXTRA_OPEN_JOIN_CLASS → ניווט
 * לכאן. לא דרך ה-Bottom Navigation. זהה במבנה ל-JoinInstitutionFragment
 * (משימה 0.8), רק עם פורמט קוד שונה (XX-XXXXX במקום XXXX-XXXX) ו-Cloud
 * Function שונה.
 *
 * למה checkExistingRole כאן ולא ב-LoginActivity?
 * --------------------------------------------------
 * בדיוק כמו ב-JoinInstitutionFragment: role לא ניתן לדעת לפני שהמשתמש
 * כבר מחובר, אז הקישור ב-LoginActivity/RegisterActivity תמיד גלוי,
 * וההגבלה בפועל (מורה/מנהל/סופר-אדמין לא צריך להצטרף כתלמיד) נאכפת כאן.
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JoinClassFragment extends Fragment {

    /** ה-region שבו נפרסה joinClass (ר' functions/index.js) — חייב להתאים. */
    private static final String FUNCTIONS_REGION = "europe-west1";

    private TextView tvInstructions;
    private TextInputLayout classCodeInputLayout;
    private TextInputEditText etClassCode;
    private MaterialButton btnJoin;

    /** מונע לולאה אינסופית בין ה-TextWatcher לעיצוב האוטומטי שהוא עצמו מבצע. */
    private boolean isFormattingCode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_join_class, container, false);

        ((MaterialToolbar) root.findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tvInstructions = root.findViewById(R.id.tvJoinClassInstructions);
        classCodeInputLayout = root.findViewById(R.id.classCodeInputLayout);
        etClassCode = root.findViewById(R.id.etClassCode);
        btnJoin = root.findViewById(R.id.btnJoinClass);

        setupAutoDashFormatting();
        btnJoin.setOnClickListener(v -> attemptJoin());

        checkExistingRole();

        return root;
    }

    /**
     * checkExistingRole — מורה/מנהל/סופר-אדמין לא אמור להצטרף לכיתה
     * כתלמיד. מסתיר את הטופס ומציג הודעה במקום.
     */
    private void checkExistingRole() {
        UserRoleManager.getInstance().refresh(() -> {
            if (!isAdded()) return;
            String role = UserRoleManager.getInstance().getRole();
            boolean isStaff = UserRoleManager.ROLE_TEACHER.equals(role)
                    || UserRoleManager.ROLE_PRINCIPAL.equals(role)
                    || UserRoleManager.ROLE_SUPERADMIN.equals(role);
            if (isStaff) {
                tvInstructions.setText("החשבון שלך משויך כמורה/מנהל. לא ניתן להצטרף לכיתה כתלמיד.");
                classCodeInputLayout.setVisibility(View.GONE);
                btnJoin.setVisibility(View.GONE);
            }
        });
    }

    /** מעצב את הקלט אוטומטית לפורמט XX-XXXXX תוך כדי הקלדה. */
    private void setupAutoDashFormatting() {
        etClassCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingCode) return;
                isFormattingCode = true;

                String raw = s.toString().toUpperCase(Locale.US).replace("-", "");
                if (raw.length() > 7) raw = raw.substring(0, 7);

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    if (i == 2) formatted.append('-');
                    formatted.append(raw.charAt(i));
                }

                s.replace(0, s.length(), formatted.toString());
                etClassCode.setSelection(formatted.length());
                isFormattingCode = false;
            }
        });
    }

    private void attemptJoin() {
        String code = etClassCode.getText() != null ? etClassCode.getText().toString().trim() : "";
        if (code.length() != 8) { // XX-XXXXX
            classCodeInputLayout.setError("קוד לא תקין — צריך להיות בפורמט XX-XXXXX");
            return;
        }
        classCodeInputLayout.setError(null);

        btnJoin.setEnabled(false);
        btnJoin.setText("מצטרף...");

        Map<String, Object> data = new HashMap<>();
        data.put("joinCode", code);

        FirebaseFunctions.getInstance(FUNCTIONS_REGION)
                .getHttpsCallable("joinClass")
                .call(data)
                .addOnSuccessListener(result -> {
                    if (!isAdded()) return;
                    String className = extractClassName(result.getData());

                    Toast.makeText(requireContext(),
                            className.isEmpty()
                                    ? "ברוך הבא לכיתה!"
                                    : "ברוך הבא לכיתה " + className + "!",
                            Toast.LENGTH_LONG).show();

                    // מפעילים מחדש את MainActivity כדי לחזור לדשבורד התלמיד —
                    // אותו דפוס בדיוק כמו ב-JoinInstitutionFragment.
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnJoin.setEnabled(true);
                    btnJoin.setText("הצטרף לכיתה");
                    classCodeInputLayout.setError(describeError(e));
                });
    }

    private static String extractClassName(Object resultData) {
        if (resultData instanceof Map) {
            Object name = ((Map<?, ?>) resultData).get("className");
            if (name != null) return name.toString();
        }
        return "";
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
            switch (code) {
                case NOT_FOUND:
                    return "קוד הכיתה לא נמצא. ודא/י שהקלדת אותו נכון.";
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
