package com.example.easylex.ui.teacher;

/**
 * =====================================================================
 * TeacherDashboardFragment — משימה 0.9 (שלב 0ב, איפיון הפלטפורמה)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * "כלי כיתה" יומיומי ומהיר למורה — לא placeholder. מציג את הכיתות של
 * המורה עם קוד ההצטרפות שלהן (גדול, מוכן להקרנה/שיתוף) ומספר תלמידים,
 * ומאפשר יצירת כיתה חדשה. ניתוח עמוק (מפת חום, גרפים, הקצאות) יגיע
 * בעתיד מאתר ווב נפרד — במכוון לא כאן.
 *
 * TODO (0.9): כאן, ליד כותרת המוסד, יתווסף בעתיד כפתור לפתיחת דשבורד
 * ווב מלא (מפת חום מילים קשות, גרפי פעילות, הקצאת משימות). ראו גם TODO
 * מקביל ב-fragment_teacher_dashboard.xml.
 *
 * איך מגיעים למסך הזה?
 * ----------------------
 * MainActivity מנתב לכאן אוטומטית אחרי טעינת UserRoleManager, אם
 * role == "teacher" או "principal" — ר' MainActivity.java. לא דרך
 * Bottom Navigation (מוחלף לגמרי עבור המשתמשים האלה, לא נוסף עליו).
 */

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.SchoolClass;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class TeacherDashboardFragment extends Fragment {

    private TeacherDashboardViewModel viewModel;
    private ClassAdapter adapter;

    private TextView tvInstitutionName;
    private RecyclerView recyclerViewClasses;
    private View layoutEmptyState;
    private LinearProgressIndicator progressBar;
    private ExtendedFloatingActionButton fabCreateClass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);

        tvInstitutionName = root.findViewById(R.id.tvInstitutionName);
        recyclerViewClasses = root.findViewById(R.id.recyclerViewClasses);
        layoutEmptyState = root.findViewById(R.id.layoutEmptyState);
        progressBar = root.findViewById(R.id.teacherDashboardProgress);
        fabCreateClass = root.findViewById(R.id.fabCreateClass);

        adapter = new ClassAdapter();
        adapter.setOnShareClickListener(this::shareJoinCode);
        adapter.setOnClassClickListener(this::openClassDetail);
        recyclerViewClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewClasses.setAdapter(adapter);

        fabCreateClass.setOnClickListener(v -> showCreateClassDialog());

        viewModel = new ViewModelProvider(this).get(TeacherDashboardViewModel.class);
        observeViewModel();

        return root;
    }

    /**
     * onResume — מרענן את רשימת הכיתות בכל פעם שהמסך חוזר להיות גלוי.
     *
     * הבאג שתוקן: TeacherDashboardViewModel טען כיתות פעם אחת בלבד,
     * בקונסטרוקטור (ר' loadDashboard()/loadClasses() שם). כשה-Fragment/
     * ViewModel נוצרים מחדש (למשל אחרי שה-Activity נהרס ונבנה מחדש
     * ברקע — התנהגות רגילה של אנדרואיד, במיוחד באמולטור) בלי שאף אחד
     * מבקש טעינה מחדש, המסך נשאר תלוי בתוצאה הישנה/החסרה במקום לשלוף
     * מצב עדכני מהשרת. הקריאה כאן הופכת את הרענון לבלתי-תלוי בשאלה אם
     * זהו אותו מופע ViewModel או מופע חדש — בכל מקרה, "המסך גלוי" = "שולפים
     * מהשרת עכשיו".
     */
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadClasses();
        }
    }

    private void observeViewModel() {
        viewModel.getInstitutionName().observe(getViewLifecycleOwner(), name ->
                tvInstitutionName.setText(name != null && !name.isEmpty() ? name : "המוסד שלי"));

        viewModel.getClasses().observe(getViewLifecycleOwner(), this::renderClasses);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderClasses(List<SchoolClass> classes) {
        boolean empty = classes == null || classes.isEmpty();
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewClasses.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.setItems(classes);
    }

    /**
     * openClassDetail — משימה 0.14: לחיצה על כרטיס כיתה (לא כפתור השיתוף)
     * פותחת את מסך הכיתה (ClassDetailFragment, לשוניות תלמידים/משימות).
     * institutionId מגיע מ-UserRoleManager — כבר נטען לפני שהמורה הגיע
     * לדשבורד הזה בכלל (ר' MainActivity switchToTeacherNavigation).
     */
    private void openClassDetail(SchoolClass schoolClass) {
        Bundle args = new Bundle();
        args.putString(ClassDetailFragment.ARG_CLASS_ID, schoolClass.getId());
        args.putString(ClassDetailFragment.ARG_INSTITUTION_ID,
                com.example.easylex.data.UserRoleManager.getInstance().getInstitutionId());
        args.putString(ClassDetailFragment.ARG_CLASS_NAME, schoolClass.getName());
        args.putLong(ClassDetailFragment.ARG_STUDENT_COUNT, schoolClass.getStudentCount());

        Navigation.findNavController(requireView())
                .navigate(R.id.action_navigation_teacher_dashboard_to_navigation_class_detail, args);
    }

    /** כפתור "שתף קוד" — פותח את ה-share sheet הרגיל של אנדרואיד עם קוד הכיתה. */
    private void shareJoinCode(SchoolClass schoolClass) {
        String className = schoolClass.getName() != null ? schoolClass.getName() : "";
        String joinCode = schoolClass.getJoinCode() != null ? schoolClass.getJoinCode() : "";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "הצטרפו לכיתה \"" + className + "\" באפליקציית EasyLex עם הקוד: " + joinCode);
        startActivity(Intent.createChooser(shareIntent, "שתף קוד הצטרפות"));
    }

    private void showCreateClassDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_class, null);
        TextInputEditText etClassName = dialogView.findViewById(R.id.etClassName);
        TextInputEditText etClassGrade = dialogView.findViewById(R.id.etClassGrade);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("כיתה חדשה")
                .setView(dialogView)
                .setPositiveButton("צור", null) // מוגדר מחדש למטה כדי למנוע סגירה אוטומטית בקלט לא תקין
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String className = etClassName.getText() != null
                        ? etClassName.getText().toString().trim() : "";
                String grade = etClassGrade.getText() != null
                        ? etClassGrade.getText().toString().trim() : "";

                if (TextUtils.isEmpty(className)) {
                    etClassName.setError("שם הכיתה הוא שדה חובה");
                    return;
                }

                viewModel.createClass(className, grade);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
