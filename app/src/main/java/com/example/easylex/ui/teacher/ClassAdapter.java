package com.example.easylex.ui.teacher;

/** ClassAdapter — משימה 0.9: מציג כרטיס לכל כיתה (שם, שכבה, קוד, מספר תלמידים). */

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.SchoolClass;

import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    public interface OnShareClickListener {
        void onShareClick(SchoolClass schoolClass);
    }

    /** onClassClick — משימה 0.14: לחיצה על הכרטיס (לא כפתור השיתוף) פותחת את מסך הכיתה. */
    public interface OnClassClickListener {
        void onClassClick(SchoolClass schoolClass);
    }

    private List<SchoolClass> items = new ArrayList<>();
    private OnShareClickListener listener;
    private OnClassClickListener classClickListener;

    public void setOnShareClickListener(OnShareClickListener listener) {
        this.listener = listener;
    }

    public void setOnClassClickListener(OnClassClickListener classClickListener) {
        this.classClickListener = classClickListener;
    }

    public void setItems(List<SchoolClass> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        holder.bind(items.get(position), listener, classClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvClassName;
        private final TextView tvClassGrade;
        private final TextView tvStudentCount;
        private final TextView tvJoinCode;
        private final ImageButton btnShareCode;

        ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvClassGrade = itemView.findViewById(R.id.tvClassGrade);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            tvJoinCode = itemView.findViewById(R.id.tvJoinCode);
            btnShareCode = itemView.findViewById(R.id.btnShareCode);
        }

        void bind(SchoolClass item, OnShareClickListener listener, OnClassClickListener classClickListener) {
            tvClassName.setText(item.getName());
            String grade = item.getGrade();
            tvClassGrade.setText(grade != null && !grade.isEmpty() ? "שכבה " + grade : "");
            tvStudentCount.setText(String.valueOf(item.getStudentCount()));
            tvJoinCode.setText(item.getJoinCode());
            btnShareCode.setOnClickListener(v -> {
                if (listener != null) listener.onShareClick(item);
            });
            itemView.setOnClickListener(v -> {
                if (classClickListener != null) classClickListener.onClassClick(item);
            });
        }
    }
}
