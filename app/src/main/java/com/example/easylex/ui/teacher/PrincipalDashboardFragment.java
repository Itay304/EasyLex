package com.example.easylex.ui.teacher;

/**
 * =====================================================================
 * PrincipalDashboardFragment — דשבורד מינימלי למנהל מוסד (שלב 2)
 * =====================================================================
 *
 * מחליף את ComingSoonFragment בטאב "עוד" של teacher_nav_graph.xml
 * (navigation_more_placeholder — אותו id/label, לא שונה). בפועל שני
 * מצבים לפי role, שניהם ב-fragment_principal_dashboard.xml:
 *   • role=="principal" — 4 כרטיסי סיכום (getPrincipalStats) + כל כיתות
 *     המוסד (לא רק כיתות המורה, בניגוד ל-TeacherDashboardFragment) + רענון.
 *   • כל role אחר (teacher) — אותו placeholder "בקרוב" בדיוק כמו קודם.
 *
 * הבדיקה נעשית פעם אחת ב-onCreateView דרך UserRoleManager (כבר טעון —
 * MainActivity מנתב לגרף הזה רק אחרי שה-role נפתר, אותו דפוס בדיוק כמו
 * TeacherDashboardFragment).
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.easylex.R;
import com.example.easylex.data.SchoolClass;
import com.example.easylex.data.UserRoleManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class PrincipalDashboardFragment extends Fragment {

    private boolean isPrincipal;

    private PrincipalDashboardViewModel viewModel;
    private ClassAdapter adapter;

    private TextView tvInstitutionName;
    private TextView tvStatTeacherCount, tvStatClassCount, tvStatActiveStudents, tvStatActiveAssignments;
    private LinearProgressIndicator progressBar;
    private SwipeRefreshLayout swipeRefreshPrincipal;
    private RecyclerView recyclerViewAllClasses;
    private View layoutAllClassesEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_principal_dashboard, container, false);

        View layoutComingSoon = root.findViewById(R.id.layoutComingSoon);
        View layoutPrincipalDashboard = root.findViewById(R.id.layoutPrincipalDashboard);

        isPrincipal = UserRoleManager.ROLE_PRINCIPAL.equals(UserRoleManager.getInstance().getRole());
        layoutComingSoon.setVisibility(isPrincipal ? View.GONE : View.VISIBLE);
        layoutPrincipalDashboard.setVisibility(isPrincipal ? View.VISIBLE : View.GONE);

        if (!isPrincipal) {
            return root; // מורה רגיל — placeholder בלבד, אין מה לחווט יותר
        }

        tvInstitutionName = root.findViewById(R.id.tvPrincipalInstitutionName);
        tvStatTeacherCount = root.findViewById(R.id.tvStatTeacherCount);
        tvStatClassCount = root.findViewById(R.id.tvStatClassCount);
        tvStatActiveStudents = root.findViewById(R.id.tvStatActiveStudents);
        tvStatActiveAssignments = root.findViewById(R.id.tvStatActiveAssignments);
        progressBar = root.findViewById(R.id.principalDashboardProgress);
        swipeRefreshPrincipal = root.findViewById(R.id.swipeRefreshPrincipal);
        recyclerViewAllClasses = root.findViewById(R.id.recyclerViewAllClasses);
        layoutAllClassesEmpty = root.findViewById(R.id.layoutAllClassesEmpty);

        adapter = new ClassAdapter();
        adapter.setOnClassClickListener(this::openClassDetail);
        recyclerViewAllClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAllClasses.setAdapter(adapter);

        swipeRefreshPrincipal.setOnRefreshListener(() -> viewModel.loadAll());

        viewModel = new ViewModelProvider(this).get(PrincipalDashboardViewModel.class);
        observeViewModel();

        loadInstitutionName();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPrincipal && viewModel != null) {
            viewModel.loadAll();
        }
    }

    private void observeViewModel() {
        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) return;
            tvStatTeacherCount.setText(String.valueOf(stats.teacherCount));
            tvStatClassCount.setText(String.valueOf(stats.classCount));
            tvStatActiveStudents.setText(String.valueOf(stats.activeStudentsThisWeek));
            tvStatActiveAssignments.setText(String.valueOf(stats.activeAssignmentCount));
        });

        viewModel.getAllClasses().observe(getViewLifecycleOwner(), this::renderClasses);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) swipeRefreshPrincipal.setRefreshing(false);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** שם המוסד — אותה קריאה חד-פעמית פשוטה כמו TeacherDashboardViewModel.loadDashboard. */
    private void loadInstitutionName() {
        String institutionId = UserRoleManager.getInstance().getInstitutionId();
        if (institutionId == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("institutions").document(institutionId).get()
                .addOnSuccessListener(doc -> {
                    if (isAdded()) tvInstitutionName.setText(doc.getString("name"));
                });
    }

    private void renderClasses(List<SchoolClass> classes) {
        boolean empty = classes == null || classes.isEmpty();
        layoutAllClassesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewAllClasses.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.setItems(classes);
    }

    /** לחיצה על כיתה — פותחת ClassDetailFragment, אותו מסך בדיוק כמו למורה. */
    private void openClassDetail(SchoolClass schoolClass) {
        Bundle args = new Bundle();
        args.putString(ClassDetailFragment.ARG_CLASS_ID, schoolClass.getId());
        args.putString(ClassDetailFragment.ARG_INSTITUTION_ID, UserRoleManager.getInstance().getInstitutionId());
        args.putString(ClassDetailFragment.ARG_CLASS_NAME, schoolClass.getName());
        args.putLong(ClassDetailFragment.ARG_STUDENT_COUNT, schoolClass.getStudentCount());

        androidx.navigation.Navigation.findNavController(requireView())
                .navigate(R.id.action_navigation_more_placeholder_to_navigation_class_detail, args);
    }
}
