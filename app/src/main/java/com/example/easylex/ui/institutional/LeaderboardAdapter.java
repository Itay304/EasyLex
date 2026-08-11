package com.example.easylex.ui.institutional;

/** LeaderboardAdapter — שורת מקום/שם/XP בטבלת המובילים (מסך הבית המוסדי). */

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.RowViewHolder> {

    private List<InstitutionalHomeViewModel.LeaderboardRow> items = new ArrayList<>();

    public void setItems(List<InstitutionalHomeViewModel.LeaderboardRow> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_row, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRank;
        private final TextView tvName;
        private final TextView tvXp;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvLeaderboardRank);
            tvName = itemView.findViewById(R.id.tvLeaderboardName);
            tvXp = itemView.findViewById(R.id.tvLeaderboardXp);
        }

        void bind(InstitutionalHomeViewModel.LeaderboardRow row) {
            tvRank.setText(String.valueOf(row.rank));
            tvName.setText(row.isCurrentUser ? row.displayName + " (אתה)" : row.displayName);
            tvXp.setText(row.totalXp + " XP");

            int style = row.isCurrentUser ? Typeface.BOLD : Typeface.NORMAL;
            tvName.setTypeface(null, style);
            itemView.setBackgroundColor(row.isCurrentUser ? 0xFFE8F5E9 : 0x00000000);
        }
    }
}
