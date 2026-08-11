package com.example.easylex.ui.assignments;

/** StudentAssignmentAdapter — משימה 0.15: כרטיס משימה + פס התקדמות + "התחל תרגול". */

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.Assignment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StudentAssignmentAdapter
        extends RecyclerView.Adapter<StudentAssignmentAdapter.AssignmentViewHolder> {

    /** יום בחודש בעברית (א'=ראשון..ש'=שבת) — אותה קונבנציה כמו DashboardFragment.buildDayLabels(). */
    private static final String[] DAY_LETTERS = {"א", "ב", "ג", "ד", "ה", "ו", "ש"};

    public interface OnStartPracticeListener {
        void onStartPractice(Assignment assignment);
    }

    private List<StudentAssignmentsViewModel.AssignmentWithProgress> items = new ArrayList<>();
    private OnStartPracticeListener listener;

    public void setOnStartPracticeListener(OnStartPracticeListener listener) {
        this.listener = listener;
    }

    public void setItems(List<StudentAssignmentsViewModel.AssignmentWithProgress> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String formatDueDate(long dueDateMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dueDateMs);
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=ראשון
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        return "עד יום " + DAY_LETTERS[dow] + "' " + day + "." + month;
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvListName;
        private final TextView tvDueDate;
        private final TextView tvProgress;
        private final LinearProgressIndicator progressBar;
        private final MaterialButton btnStartPractice;

        AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvListName = itemView.findViewById(R.id.tvAssignmentListName);
            tvDueDate = itemView.findViewById(R.id.tvAssignmentDueDate);
            tvProgress = itemView.findViewById(R.id.tvAssignmentProgress);
            progressBar = itemView.findViewById(R.id.progressAssignment);
            btnStartPractice = itemView.findViewById(R.id.btnStartPractice);
        }

        void bind(StudentAssignmentsViewModel.AssignmentWithProgress item, OnStartPracticeListener listener) {
            Assignment a = item.assignment;
            tvTitle.setText(a.getTitle());
            tvListName.setText("רשימה: " + a.getListId());

            Long dueDateMs = a.getDueDateMs();
            if (dueDateMs != null) {
                tvDueDate.setText(formatDueDate(dueDateMs));
                tvDueDate.setVisibility(View.VISIBLE);
            } else {
                tvDueDate.setVisibility(View.GONE);
            }

            int total = Math.max(item.total, 0);
            int completed = Math.min(Math.max(item.completed, 0), total);
            tvProgress.setText(completed + "/" + total);
            progressBar.setMax(Math.max(total, 1));
            progressBar.setProgress(completed);

            btnStartPractice.setOnClickListener(v -> {
                if (listener != null) listener.onStartPractice(a);
            });
        }
    }
}
