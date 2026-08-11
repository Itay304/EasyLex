package com.example.easylex.ui.teacher;

/** AssignmentAdapter — משימה 0.14: מציג כרטיס לכל משימה (שם, רשימה, דד-ליין). */

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.Assignment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private List<Assignment> items = new ArrayList<>();

    public void setItems(List<Assignment> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvListName;
        private final TextView tvDueDate;

        AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvListName = itemView.findViewById(R.id.tvAssignmentListName);
            tvDueDate = itemView.findViewById(R.id.tvAssignmentDueDate);
        }

        void bind(Assignment item) {
            tvTitle.setText(item.getTitle());
            tvListName.setText("רשימה: " + item.getListId());

            Long dueDateMs = item.getDueDateMs();
            if (dueDateMs != null) {
                String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.US)
                        .format(new Date(dueDateMs));
                tvDueDate.setText("דד-ליין: " + formatted);
                tvDueDate.setVisibility(View.VISIBLE);
            } else {
                tvDueDate.setVisibility(View.GONE);
            }
        }
    }
}
