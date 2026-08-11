package com.example.easylex.ui.institutional;

/**
 * =====================================================================
 * InstitutionalHomeFragment — מסך הבית של תלמיד מוסדי (שינוי ארכיטקטוני, שלב 2)
 * =====================================================================
 *
 * מסך הבית (start destination) של institutional_nav_graph.xml — שלושה חלקים:
 *   א. הודעה מהמורה — נטענת פעם אחת, מוצגת רק אם לא נקראה עדיין (SharedPreferences).
 *   ב. טבלת מובילים — real-time, כולל שורת התלמיד עצמו גם אם מחוץ לעשירייה.
 *   ג. משימות פעילות (עד 3) — שימוש חוזר מלא ב-StudentAssignmentsViewModel/
 *      StudentAssignmentAdapter הקיימים (משימה 0.15), רק חותכים ל-3 ומציגים
 *      כפתור "ראה הכל" למסך המלא. לא משוכפל שום קוד טעינה/חישוב התקדמות.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.Assignment;
import com.example.easylex.ui.assignments.PracticeTypeBottomSheet;
import com.example.easylex.ui.assignments.StudentAssignmentAdapter;
import com.example.easylex.ui.assignments.StudentAssignmentsViewModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class InstitutionalHomeFragment extends Fragment {

    private static final String PREFS_NAME = "institutional_prefs";
    private static final int ASSIGNMENTS_PREVIEW_LIMIT = 3;

    private InstitutionalHomeViewModel viewModel;
    private StudentAssignmentsViewModel assignmentsViewModel;

    private LeaderboardAdapter leaderboardAdapter;
    private StudentAssignmentAdapter assignmentsAdapter;

    private MaterialCardView cardAnnouncement;
    private android.widget.TextView tvAnnouncementMessage;
    private android.widget.TextView tvAnnouncementMeta;
    private RecyclerView recyclerViewLeaderboard;
    private View tvLeaderboardEmpty;
    private RecyclerView recyclerViewAssignmentsPreview;
    private View tvAssignmentsPreviewEmpty;

    @Nullable
    private String pendingAnnouncementId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_institutional_home, container, false);

        cardAnnouncement = root.findViewById(R.id.cardAnnouncement);
        tvAnnouncementMessage = root.findViewById(R.id.tvAnnouncementMessage);
        tvAnnouncementMeta = root.findViewById(R.id.tvAnnouncementMeta);
        recyclerViewLeaderboard = root.findViewById(R.id.recyclerViewLeaderboard);
        tvLeaderboardEmpty = root.findViewById(R.id.tvLeaderboardEmpty);
        recyclerViewAssignmentsPreview = root.findViewById(R.id.recyclerViewAssignmentsPreview);
        tvAssignmentsPreviewEmpty = root.findViewById(R.id.tvAssignmentsPreviewEmpty);
        View tvSeeAllAssignments = root.findViewById(R.id.tvSeeAllAssignments);

        leaderboardAdapter = new LeaderboardAdapter();
        recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewLeaderboard.setAdapter(leaderboardAdapter);

        assignmentsAdapter = new StudentAssignmentAdapter();
        assignmentsAdapter.setOnStartPracticeListener(this::startPractice);
        recyclerViewAssignmentsPreview.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAssignmentsPreview.setAdapter(assignmentsAdapter);

        cardAnnouncement.setOnClickListener(v -> dismissAnnouncement());
        tvSeeAllAssignments.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_navigation_institutional_home_to_navigation_assignments));

        viewModel = new ViewModelProvider(this).get(InstitutionalHomeViewModel.class);
        assignmentsViewModel = new ViewModelProvider(this).get(StudentAssignmentsViewModel.class);
        observeViewModels();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadLatestAnnouncement();
        viewModel.startLeaderboardListener();
        assignmentsViewModel.loadAssignments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.stopLeaderboardListener();
    }

    private void observeViewModels() {
        viewModel.getAnnouncement().observe(getViewLifecycleOwner(), this::renderAnnouncement);
        viewModel.getLeaderboard().observe(getViewLifecycleOwner(), this::renderLeaderboard);
        assignmentsViewModel.getAssignments().observe(getViewLifecycleOwner(), this::renderAssignmentsPreview);
    }

    // ── א. הודעה מהמורה ──────────────────────────────────────────────────────

    private void renderAnnouncement(@Nullable InstitutionalHomeViewModel.Announcement announcement) {
        if (announcement == null || isAlreadyRead(announcement.id)) {
            cardAnnouncement.setVisibility(View.GONE);
            return;
        }
        pendingAnnouncementId = announcement.id;
        tvAnnouncementMessage.setText(announcement.message);

        String teacherName = announcement.createdByName != null && !announcement.createdByName.isEmpty()
                ? announcement.createdByName : "המורה";
        String date = new java.text.SimpleDateFormat("d.M", java.util.Locale.getDefault())
                .format(new java.util.Date(announcement.createdAtMs));
        tvAnnouncementMeta.setText(teacherName + " · " + date);

        cardAnnouncement.setVisibility(View.VISIBLE);
    }

    private void dismissAnnouncement() {
        if (pendingAnnouncementId != null) {
            prefs().edit().putBoolean(readKey(pendingAnnouncementId), true).apply();
        }
        cardAnnouncement.setVisibility(View.GONE);
    }

    private boolean isAlreadyRead(String announcementId) {
        return prefs().getBoolean(readKey(announcementId), false);
    }

    private static String readKey(String announcementId) {
        return "announcement_read_" + announcementId;
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── ב. טבלת מובילים ──────────────────────────────────────────────────────

    private void renderLeaderboard(@Nullable List<InstitutionalHomeViewModel.LeaderboardRow> rows) {
        boolean empty = rows == null || rows.isEmpty();
        tvLeaderboardEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewLeaderboard.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) leaderboardAdapter.setItems(rows);
    }

    // ── ג. משימות פעילות (עד 3) ──────────────────────────────────────────────

    private void renderAssignmentsPreview(
            @Nullable List<StudentAssignmentsViewModel.AssignmentWithProgress> items) {
        boolean empty = items == null || items.isEmpty();
        tvAssignmentsPreviewEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewAssignmentsPreview.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) return;

        List<StudentAssignmentsViewModel.AssignmentWithProgress> preview =
                new ArrayList<>(items.subList(0, Math.min(ASSIGNMENTS_PREVIEW_LIMIT, items.size())));
        assignmentsAdapter.setItems(preview);
    }

    /** "התחל תרגול" מתוך התצוגה המקדימה — אותה בחירת מודול כמו StudentAssignmentsFragment.startPractice. */
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
            case "FLASHCARDS": actionId = R.id.action_navigation_institutional_home_to_navigation_flashcards; break;
            case "SPELLING":   actionId = R.id.action_navigation_institutional_home_to_navigation_spelling; break;
            default:           actionId = R.id.action_navigation_institutional_home_to_navigation_quiz; break;
        }
        Navigation.findNavController(requireView()).navigate(actionId, args);
    }
}
