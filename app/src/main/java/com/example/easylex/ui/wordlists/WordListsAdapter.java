package com.example.easylex.ui.wordlists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easylex.R;
import com.example.easylex.data.WordList;
import java.util.ArrayList;
import java.util.List;

public class WordListsAdapter extends RecyclerView.Adapter<WordListsAdapter.WordListViewHolder> {

    private List<WordList> wordLists = new ArrayList<>();
    private OnDownloadClickListener listener;

    public interface OnDownloadClickListener {
        void onDownloadClick(WordList wordList);
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WordListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_wordlist, parent, false);
        return new WordListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordListViewHolder holder, int position) {
        WordList currentList = wordLists.get(position);
        holder.bind(currentList, listener);
    }

    @Override
    public int getItemCount() {
        return wordLists.size();
    }

    public void setWordLists(List<WordList> lists) {
        this.wordLists = lists;
        notifyDataSetChanged();
    }

    static class WordListViewHolder extends RecyclerView.ViewHolder {
        private final TextView listNameTextView;
        private final TextView listDescriptionTextView;
        private final Button downloadButton;

        public WordListViewHolder(@NonNull View itemView) {
            super(itemView);
            listNameTextView = itemView.findViewById(R.id.textViewListName);
            listDescriptionTextView = itemView.findViewById(R.id.textViewListDescription);
            downloadButton = itemView.findViewById(R.id.buttonDownloadList);
        }

        public void bind(final WordList wordList, final OnDownloadClickListener listener) {
            if (wordList != null) {
                listNameTextView.setText(wordList.getName());
                listDescriptionTextView.setText(wordList.getWordCount() + " words");
                downloadButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDownloadClick(wordList);
                    }
                });
            }
        }
    }
}