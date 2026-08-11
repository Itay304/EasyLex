package com.example.easylex.ui.teacher;

/**
 * StudentProgressAdapter — טבלת "תלמידים" ב-ClassDetailFragment (מעקב
 * התקדמות תלמידים בכיתה). שם תצוגה מפורמט "שם פרטי + אות משם משפחה"
 * (InstitutionalHomeViewModel.formatName, שימוש חוזר — אותו פורמט בדיוק
 * כמו בטבלת המובילים של התלמיד המוסדי).
 */

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.StudentProgress;
import com.example.easylex.ui.institutional.InstitutionalHomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class StudentProgressAdapter extends RecyclerView.Adapter<StudentProgressAdapter.VH> {

    private List<StudentProgress> items = new ArrayList<>();

    public void setItems(List<StudentProgress> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_progress, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StudentProgress item = items.get(position);
        holder.tvName.setText(InstitutionalHomeViewModel.formatName(item.getDisplayName()));
        holder.tvMastered.setText(String.valueOf(item.getMasteredWords()));
        holder.tvXp.setText(String.valueOf(item.getTotalXp()));

        boolean active = item.isWeeklyActivity();
        holder.tvWeeklyActive.setText(active ? "✓" : "✗");
        holder.tvWeeklyActive.setTextColor(active
                ? holder.itemView.getResources().getColor(R.color.green_primary)
                : android.graphics.Color.parseColor("#D32F2F"));

        holder.tvLastActive.setText(formatLastActive(item.getLastActiveDateMs()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private static String formatLastActive(Long lastActiveDateMs) {
        if (lastActiveDateMs == null) return "מעולם לא תרגל";
        return DateUtils.getRelativeTimeSpanString(
                lastActiveDateMs, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvMastered, tvXp, tvWeeklyActive, tvLastActive;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvStudentName);
            tvMastered = v.findViewById(R.id.tvStudentMastered);
            tvXp = v.findViewById(R.id.tvStudentXp);
            tvWeeklyActive = v.findViewById(R.id.tvStudentWeeklyActive);
            tvLastActive = v.findViewById(R.id.tvStudentLastActive);
        }
    }
}
