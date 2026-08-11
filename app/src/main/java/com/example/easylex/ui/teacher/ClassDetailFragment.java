package com.example.easylex.ui.teacher;

/**
 * =====================================================================
 * ClassDetailFragment — מסך כיתה בודדת (משימה 0.14, שלב 2 — מערכת משימות)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * נפתח בלחיצה על כרטיס כיתה ב-TeacherDashboardFragment (ר' navigation_class_detail
 * ב-teacher_nav_graph.xml). שתי לשוניות:
 *   • "תלמידים" — מעקב התקדמות (getClassProgress, Cloud Function): כרטיס סיכום
 *     + טבלה (שם/מילים שנכבשו/XP/פעיל השבוע/פעילות אחרונה), מיון וסינון
 *     בצד לקוח (ClassDetailViewModel.applySortAndFilter), swipe-to-refresh.
 *   • "משימות" — רשימת המשימות הפעילות של הכיתה + כפתור "משימה חדשה" שפותח
 *     דיאלוג יצירה (שם, רשימת מילים מתוך word_lists הציבוריות, דד-ליין
 *     אופציונלי). קורא ל-createAssignment (Cloud Function, functions/index.js).
 *
 * TODO (0.14): "האם לכלול את כל המילים או לבחור ספציפיות" — כרגע "הכל" בלבד
 * (wordIds ריק תמיד). בחירה ספציפית מתוכננת לעתיד.
 */

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.easylex.R;
import com.example.easylex.data.Assignment;
import com.example.easylex.data.StudentProgress;
import com.example.easylex.data.WordList;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ClassDetailFragment extends Fragment {

    public static final String ARG_CLASS_ID = "classId";
    public static final String ARG_INSTITUTION_ID = "institutionId";
    public static final String ARG_CLASS_NAME = "className";
    public static final String ARG_STUDENT_COUNT = "studentCount";

    private ClassDetailViewModel viewModel;
    private AssignmentAdapter adapter;
    private StudentProgressAdapter studentAdapter;

    private View layoutStudentsTab;
    private View layoutAssignmentsTab;
    private RecyclerView recyclerViewAssignments;
    private View layoutAssignmentsEmpty;
    private LinearProgressIndicator progressBar;

    // ── לשונית "תלמידים" ─────────────────────────────────────────────────────
    private TextView tvSummaryActiveThisWeek, tvSummaryAvgMastered, tvSummaryInactive;
    private LinearProgressIndicator studentsProgress;
    private SwipeRefreshLayout swipeRefreshStudents;
    private RecyclerView recyclerViewStudents;
    private View layoutStudentsEmpty;
    private ChipGroup chipGroupSort;
    private Chip chipFilterInactive;
    private View btnSendAnnouncementTab;

    /** נבחר ע"י המשתמש בדיאלוג "משימה חדשה" — null = לא נבחר דד-ליין. */
    @Nullable
    private Long selectedDueDateMs;
    @Nullable
    private String selectedListId;

    /** נדרש ע"י כפתור "שלח הודעה" בToolbar (showSendAnnouncementDialog). */
    @Nullable
    private String classId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_class_detail, container, false);

        classId = getArguments() != null ? getArguments().getString(ARG_CLASS_ID) : null;
        String institutionId = getArguments() != null ? getArguments().getString(ARG_INSTITUTION_ID) : null;
        String className = getArguments() != null ? getArguments().getString(ARG_CLASS_NAME) : null;
        long studentCount = getArguments() != null ? getArguments().getLong(ARG_STUDENT_COUNT, 0) : 0;

        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(className != null && !className.isEmpty() ? className : "כיתה");
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_class_detail);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_send_announcement) {
                showSendAnnouncementDialog();
                return true;
            }
            return false;
        });

        TextView tvStudentCountBig = root.findViewById(R.id.tvStudentCountBig);
        tvStudentCountBig.setText(String.valueOf(studentCount));

        layoutStudentsTab = root.findViewById(R.id.layoutStudentsTab);
        layoutAssignmentsTab = root.findViewById(R.id.layoutAssignmentsTab);
        recyclerViewAssignments = root.findViewById(R.id.recyclerViewAssignments);
        layoutAssignmentsEmpty = root.findViewById(R.id.layoutAssignmentsEmpty);
        progressBar = root.findViewById(R.id.classDetailProgress);
        ExtendedFloatingActionButton fabCreateAssignment = root.findViewById(R.id.fabCreateAssignment);

        adapter = new AssignmentAdapter();
        recyclerViewAssignments.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAssignments.setAdapter(adapter);

        fabCreateAssignment.setOnClickListener(v -> showCreateAssignmentDialog());

        // ── לשונית "תלמידים" ─────────────────────────────────────────────────
        tvSummaryActiveThisWeek = root.findViewById(R.id.tvSummaryActiveThisWeek);
        tvSummaryAvgMastered = root.findViewById(R.id.tvSummaryAvgMastered);
        tvSummaryInactive = root.findViewById(R.id.tvSummaryInactive);
        studentsProgress = root.findViewById(R.id.studentsProgress);
        swipeRefreshStudents = root.findViewById(R.id.swipeRefreshStudents);
        recyclerViewStudents = root.findViewById(R.id.recyclerViewStudents);
        layoutStudentsEmpty = root.findViewById(R.id.layoutStudentsEmpty);
        chipGroupSort = root.findViewById(R.id.chipGroupSort);
        chipFilterInactive = root.findViewById(R.id.chipFilterInactive);
        btnSendAnnouncementTab = root.findViewById(R.id.btnSendAnnouncementTab);
        btnSendAnnouncementTab.setOnClickListener(v -> showSendAnnouncementDialog());

        studentAdapter = new StudentProgressAdapter();
        recyclerViewStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewStudents.setAdapter(studentAdapter);

        setupStudentsTabControls();

        TabLayout tabLayout = root.findViewById(R.id.tabLayoutClassDetail);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean studentsTab = tab.getPosition() == 0;
                layoutStudentsTab.setVisibility(studentsTab ? View.VISIBLE : View.GONE);
                layoutAssignmentsTab.setVisibility(studentsTab ? View.GONE : View.VISIBLE);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        viewModel = new ViewModelProvider(this).get(ClassDetailViewModel.class);
        if (institutionId != null && classId != null) {
            viewModel.init(institutionId, classId);
        }
        observeViewModel();

        return root;
    }

    private void observeViewModel() {
        viewModel.getAssignments().observe(getViewLifecycleOwner(), this::renderAssignments);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getAnnouncementSent().observe(getViewLifecycleOwner(), sent -> {
            if (Boolean.TRUE.equals(sent) && isAdded()) {
                Toast.makeText(requireContext(), "ההודעה נשלחה לכיתה", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getDisplayedStudents().observe(getViewLifecycleOwner(), this::renderStudents);
        viewModel.getProgressSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getIsLoadingStudents().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            studentsProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) swipeRefreshStudents.setRefreshing(false);
        });
    }

    private void renderAssignments(List<Assignment> assignments) {
        boolean empty = assignments == null || assignments.isEmpty();
        layoutAssignmentsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewAssignments.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.setItems(assignments);
    }

    // ── לשונית "תלמידים" ─────────────────────────────────────────────────────

    private void setupStudentsTabControls() {
        swipeRefreshStudents.setOnRefreshListener(() -> viewModel.loadClassProgress());

        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipSortMastered) {
                viewModel.setSortMode(ClassDetailViewModel.SortMode.MASTERED);
            } else if (id == R.id.chipSortActivity) {
                viewModel.setSortMode(ClassDetailViewModel.SortMode.ACTIVITY);
            } else if (id == R.id.chipSortXp) {
                viewModel.setSortMode(ClassDetailViewModel.SortMode.XP);
            }
        });

        chipFilterInactive.setOnClickListener(v -> viewModel.toggleInactiveFilter());
    }

    private void renderStudents(List<StudentProgress> students) {
        boolean empty = students == null || students.isEmpty();
        layoutStudentsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewStudents.setVisibility(empty ? View.GONE : View.VISIBLE);
        studentAdapter.setItems(students);
    }

    private void renderSummary(@Nullable ClassDetailViewModel.ClassProgressSummary summary) {
        if (summary == null) return;
        tvSummaryActiveThisWeek.setText(String.format(Locale.getDefault(),
                "%d מתוך %d תלמידים פעילים השבוע", summary.activeThisWeek, summary.totalStudents));
        tvSummaryAvgMastered.setText(String.format(Locale.getDefault(),
                "ממוצע %.1f מילים נכבשות בכיתה", summary.avgMasteredWords));

        if (summary.inactive3PlusDays > 0) {
            tvSummaryInactive.setText(String.format(Locale.getDefault(),
                    "%d תלמידים לא תרגלו 3+ ימים", summary.inactive3PlusDays));
            tvSummaryInactive.setTextColor(0xFFD32F2F);
        } else {
            tvSummaryInactive.setText("כל התלמידים פעילים!");
            tvSummaryInactive.setTextColor(0xFF2E7D32);
        }
    }

    // ── דיאלוג "משימה חדשה" ──────────────────────────────────────────────────

    private void showCreateAssignmentDialog() {
        selectedDueDateMs = null;
        selectedListId = null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_assignment, null);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etAssignmentTitle);
        AutoCompleteTextView actvWordList = dialogView.findViewById(R.id.actvWordList);
        Button btnPickDueDate = dialogView.findViewById(R.id.btnPickDueDate);

        setupWordListDropdown(actvWordList);
        setupDueDatePicker(btnPickDueDate);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("משימה חדשה")
                .setView(dialogView)
                .setPositiveButton("שלח משימה", null) // מוגדר מחדש למטה כדי למנוע סגירה אוטומטית בקלט לא תקין
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";

                if (TextUtils.isEmpty(title)) {
                    etTitle.setError("שם המשימה הוא שדה חובה");
                    return;
                }
                if (TextUtils.isEmpty(selectedListId)) {
                    Toast.makeText(requireContext(), "יש לבחור רשימת מילים", Toast.LENGTH_SHORT).show();
                    return;
                }

                viewModel.createAssignment(title, selectedListId, selectedDueDateMs);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    /** ממלא את הדרופדאון מ-word_lists (נטען פעם אחת, ר' ClassDetailViewModel.loadWordLists). */
    private void setupWordListDropdown(AutoCompleteTextView actvWordList) {
        viewModel.getWordLists().observe(getViewLifecycleOwner(), lists -> {
            if (lists == null) return;
            List<String> names = new ArrayList<>();
            for (WordList list : lists) {
                names.add(list.getName() != null ? list.getName() : list.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, names);
            actvWordList.setAdapter(adapter);
            actvWordList.setOnItemClickListener((parent, view, position, id) ->
                    selectedListId = lists.get(position).getId());
        });
        viewModel.loadWordLists();
    }

    /** DatePicker לדד-ליין האופציונלי — מעדכן את טקסט הכפתור עם התאריך שנבחר. */
    private void setupDueDatePicker(Button btnPickDueDate) {
        btnPickDueDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth, 23, 59, 59);
                selectedDueDateMs = picked.getTimeInMillis();
                btnPickDueDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });
    }

    // ── דיאלוג "שלח הודעה" (כפתור בToolbar) ──────────────────────────────────

    private void showSendAnnouncementDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_send_announcement, null);
        TextInputEditText etMessage = dialogView.findViewById(R.id.etAnnouncementMessage);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("שלח הודעה לכיתה")
                .setView(dialogView)
                .setPositiveButton("שלח", null) // מוגדר מחדש למטה כדי למנוע סגירה אוטומטית בקלט לא תקין
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

                if (TextUtils.isEmpty(message)) {
                    etMessage.setError("הודעה היא שדה חובה");
                    return;
                }
                if (message.length() > 200) {
                    etMessage.setError("ההודעה ארוכה מדי (מקסימום 200 תווים)");
                    return;
                }
                viewModel.sendAnnouncement(message);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
