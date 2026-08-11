package com.example.easylex.ui.institutional;

/**
 * =====================================================================
 * InstitutionalStatsFragment — סטטיסטיקות תלמיד מוסדי (שלב 2, חלק 3)
 * =====================================================================
 *
 * מחליף את ה-placeholder (ComingSoonFragment) שהיה עד כה על
 * navigation_institutional_stats ב-institutional_nav_graph.xml.
 * ארבעה חלקים, כל הנתונים דרך InstitutionalStatsViewModel:
 *   1. "המסע שלי"          — LineChartView (8 שבועות, נכבשו מצטבר)
 *   2. "המשימות שלי"        — DonutChartView לכל משימה פעילה (נכבשו/סה"כ)
 *   3. "פעילות שבועית"      — עמודות בנייה דינמית (זהה בסגנון ל-
 *                             DashboardFragment.setupWeeklyActivity, מסונן למשימות)
 *   4. "חוזקות וחולשות"     — רשימת טקסט פשוטה (לא גרף) — top-5/bottom-5
 * =====================================================================
 */

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easylex.R;
import com.example.easylex.ui.charts.DonutChartView;
import com.example.easylex.ui.charts.LineChartView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Calendar;
import java.util.List;

public class InstitutionalStatsFragment extends Fragment {

    private InstitutionalStatsViewModel viewModel;

    private LineChartView lineChartJourney;
    private LinearLayout llAssignmentMastery;
    private View tvAssignmentMasteryEmpty;
    private LinearLayout llStatsWeeklyBars;
    private LinearLayout llStrengths;
    private LinearLayout llWeaknesses;
    private View tvStrengthsWeaknessesEmpty;
    private LinearProgressIndicator progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_institutional_stats, container, false);

        lineChartJourney = root.findViewById(R.id.lineChartJourney);
        llAssignmentMastery = root.findViewById(R.id.llAssignmentMastery);
        tvAssignmentMasteryEmpty = root.findViewById(R.id.tvAssignmentMasteryEmpty);
        llStatsWeeklyBars = root.findViewById(R.id.llStatsWeeklyBars);
        llStrengths = root.findViewById(R.id.llStrengths);
        llWeaknesses = root.findViewById(R.id.llWeaknesses);
        tvStrengthsWeaknessesEmpty = root.findViewById(R.id.tvStrengthsWeaknessesEmpty);
        progressBar = root.findViewById(R.id.statsProgress);

        viewModel = new ViewModelProvider(this).get(InstitutionalStatsViewModel.class);
        observeViewModel();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getJourneyValues().observe(getViewLifecycleOwner(), values ->
                lineChartJourney.setData(values, viewModel.getJourneyLabels().getValue()));
        viewModel.getJourneyLabels().observe(getViewLifecycleOwner(), labels ->
                lineChartJourney.setData(viewModel.getJourneyValues().getValue(), labels));

        viewModel.getAssignmentMastery().observe(getViewLifecycleOwner(), this::renderAssignmentMastery);
        viewModel.getWeeklyActivity().observe(getViewLifecycleOwner(), this::renderWeeklyActivity);
        viewModel.getStrengths().observe(getViewLifecycleOwner(), s -> renderWordStats(llStrengths, s, true));
        viewModel.getWeaknesses().observe(getViewLifecycleOwner(), w -> {
            renderWordStats(llWeaknesses, w, false);
            boolean bothEmpty = isEmpty(viewModel.getStrengths().getValue()) && isEmpty(w);
            tvStrengthsWeaknessesEmpty.setVisibility(bothEmpty ? View.VISIBLE : View.GONE);
        });
    }

    private static boolean isEmpty(@Nullable List<?> list) {
        return list == null || list.isEmpty();
    }

    // ── 2. המשימות שלי — טבעת לכל משימה פעילה ────────────────────────────────

    private void renderAssignmentMastery(@Nullable List<InstitutionalStatsViewModel.AssignmentMastery> items) {
        llAssignmentMastery.removeAllViews();
        boolean empty = items == null || items.isEmpty();
        tvAssignmentMasteryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty || !isAdded()) return;

        for (InstitutionalStatsViewModel.AssignmentMastery item : items) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            DonutChartView donut = new DonutChartView(requireContext());
            LinearLayout.LayoutParams donutLp = new LinearLayout.LayoutParams(dp(56), dp(56));
            donut.setLayoutParams(donutLp);
            donut.setProgress(item.mastered, item.total);
            row.addView(donut);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(item.title);
            tvTitle.setTextSize(14f);
            tvTitle.setTextColor(Color.parseColor("#333333"));
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleLp.setMarginStart(dp(12));
            tvTitle.setLayoutParams(titleLp);
            row.addView(tvTitle);

            llAssignmentMastery.addView(row);
        }
    }

    // ── 3. פעילות שבועית — עמודות, אותו סגנון כמו ב-DashboardFragment ────────

    private void renderWeeklyActivity(@Nullable int[] weekly) {
        if (weekly == null || !isAdded()) return;
        llStatsWeeklyBars.removeAllViews();

        int maxVal = 1;
        for (int v : weekly) if (v > maxVal) maxVal = v;
        String[] dayLabels = buildDayLabels();

        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, dp(80), 1f));

            View bar = new View(requireContext());
            int barH = weekly[i] == 0 ? dp(5) : Math.max(dp(5), (int) (weekly[i] * 1f / maxVal * dp(56)));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(16), barH);
            barLp.bottomMargin = dp(4);
            bar.setLayoutParams(barLp);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setColor(i == 6 ? Color.parseColor("#00BFA5") : Color.parseColor("#B2DFDB"));
            barBg.setCornerRadius(dp(4));
            bar.setBackground(barBg);
            col.addView(bar);

            TextView lbl = new TextView(requireContext());
            lbl.setText(dayLabels[i]);
            lbl.setTextSize(9f);
            lbl.setTextColor(Color.parseColor("#AAAAAA"));
            lbl.setGravity(Gravity.CENTER);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            col.addView(lbl);

            llStatsWeeklyBars.addView(col);
        }
    }

    private String[] buildDayLabels() {
        String[] all = {"א'", "ב'", "ג'", "ד'", "ה'", "ו'", "ש'"};
        String[] result = new String[7];
        int todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        for (int i = 0; i < 7; i++) result[i] = all[(todayDow - (6 - i) + 7) % 7];
        return result;
    }

    // ── 4. חוזקות וחולשות — רשימת טקסט פשוטה ──────────────────────────────────

    private void renderWordStats(LinearLayout container, @Nullable List<InstitutionalStatsViewModel.WordStat> items,
                                  boolean strengths) {
        container.removeAllViews();
        if (items == null || !isAdded()) return;

        for (InstitutionalStatsViewModel.WordStat item : items) {
            TextView row = new TextView(requireContext());
            String detail = strengths
                    ? item.correctAttempts + " תשובות נכונות"
                    : (item.totalAttempts - item.correctAttempts) + " תשובות שגויות";
            row.setText(item.englishWord + "  —  " + detail);
            row.setTextSize(13f);
            row.setTextColor(Color.parseColor("#555555"));
            row.setPadding(0, dp(3), 0, dp(3));
            container.addView(row);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
