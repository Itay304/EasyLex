package com.example.easylex.ui.assignments;

/**
 * =====================================================================
 * StudentAssignmentsFragment — "המשימות שלי" (משימה 0.15, שלב 2)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * לא טאב ב-bottom-nav — BottomNavigationView תומך במקסימום 5 פריטים
 * (מגבלה קשיחה של Android), וכבר יש 5. נגיש רק דרך כרטיס "המשימות שלי"
 * בדשבורד (DashboardFragment.cardMyAssignments), אותו דפוס בדיוק כמו
 * navigation_word_list. תלמיד מוסדי רואה את רשימת המשימות שהמורה הקצה
 * לכיתה שלו (StudentAssignmentsViewModel, getMyAssignments); תלמיד עצמאי
 * (אין institutionId) רואה מסך "הצטרף לכיתה" במקום, בלי שום קריאת רשת.
 *
 * "התחל תרגול" על משימה:
 * ------------------------
 * מציג PracticeTypeBottomSheet (כרטיסיות/מבחן/איות), ואז מנווט למודול
 * שנבחר עם practiceType="ASSIGNMENT" + listId/wordIds של המשימה (Bundle) —
 * שלושתם (QuizFragment/FlashcardsFragment/SpellingFragment) מקבלים ענף
 * סינון "ASSIGNMENT" נוסף (לא נוגע בשום ענף/מנגנון קיים).
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.Assignment;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class StudentAssignmentsFragment extends Fragment {

    private StudentAssignmentsViewModel viewModel;
    private StudentAssignmentAdapter adapter;

    private RecyclerView recyclerViewAssignments;
    private View layoutEmptyState;
    private View layoutNotInInstitution;
    private LinearProgressIndicator progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_student_assignments, container, false);

        recyclerViewAssignments = root.findViewById(R.id.recyclerViewAssignments);
        layoutEmptyState = root.findViewById(R.id.layoutEmptyState);
        layoutNotInInstitution = root.findViewById(R.id.layoutNotInInstitution);
        progressBar = root.findViewById(R.id.assignmentsProgress);

        adapter = new StudentAssignmentAdapter();
        adapter.setOnStartPracticeListener(this::startPractice);
        recyclerViewAssignments.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAssignments.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(StudentAssignmentsViewModel.class);
        observeViewModel();

        return root;
    }

    /** onResume — כמו TeacherDashboardFragment: המסך תמיד שולף מצב עדכני כשהוא נהיה גלוי. */
    @Override
    public void onResume() {
        super.onResume();
        if (!viewModel.hasInstitution()) {
            showNotInInstitution();
            return;
        }
        viewModel.loadAssignments();
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
    }

    private void showNotInInstitution() {
        layoutNotInInstitution.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        recyclerViewAssignments.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void renderAssignments(List<StudentAssignmentsViewModel.AssignmentWithProgress> items) {
        if (!viewModel.hasInstitution()) return; // כבר מוצג מסך "הצטרף לכיתה" — לא דורס אותו

        layoutNotInInstitution.setVisibility(View.GONE);
        boolean empty = items == null || items.isEmpty();
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewAssignments.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.setItems(items);
    }

    /** "התחל תרגול" — מציג בחירת מודול (כרטיסיות/מבחן/איות), ואז מנווט עם המילים של המשימה בלבד. */
    private void startPractice(Assignment assignment) {
        PracticeTypeBottomSheet.create(type -> navigateToPractice(type, assignment))
                .show(getChildFragmentManager(), "practice_type_picker");
    }

    private void navigateToPractice(String type, Assignment assignment) {
        Bundle args = new Bundle();
        args.putString("practiceType", "ASSIGNMENT");
        args.putString("assignmentListId", assignment.getListId());
        args.putStringArrayList("assignmentWordIds", new ArrayList<>(assignment.getWordIds()));

        int actionId;
        switch (type) {
            case "FLASHCARDS": actionId = R.id.action_navigation_assignments_to_navigation_flashcards; break;
            case "SPELLING":   actionId = R.id.action_navigation_assignments_to_navigation_spelling; break;
            default:           actionId = R.id.action_navigation_assignments_to_navigation_quiz; break;
        }
        Navigation.findNavController(requireView()).navigate(actionId, args);
    }
}
