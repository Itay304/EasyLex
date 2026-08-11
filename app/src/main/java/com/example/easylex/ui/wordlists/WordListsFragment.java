package com.example.easylex.ui.wordlists;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.WordList;

// שינינו את שם הממשק כך שיכלול את הקובץ הנכון
public class WordListsFragment extends Fragment implements WordListsAdapter.OnDownloadClickListener {

    private WordListsViewModel viewModel;
    private WordListsAdapter adapter;

    private static final String TAG = "WordListsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_word_lists, container, false);
        Log.d(TAG, "onCreateView: Started.");

        // אתחול הרכיבים הנכונים מה-Layout הנכון
        RecyclerView recyclerView = root.findViewById(R.id.recyclerViewWordLists);
        adapter = new WordListsAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // הגדרת המאזין ללחיצות על כפתור ההורדה
        adapter.setOnDownloadClickListener(this);

        // אתחול ה-ViewModel
        viewModel = new ViewModelProvider(this).get(WordListsViewModel.class);
        Log.d(TAG, "onCreateView: ViewModel initialized.");

        // האזנה לשינויים
        observeViewModel();

        return root;
    }

    // מימוש הלחיצה על כפתור ההורדה
    @Override
    public void onDownloadClick(WordList wordList) {
        Log.d(TAG, "Download button clicked for list: " + wordList.getName());
        viewModel.downloadWordList(wordList);
    }

    private void observeViewModel() {
        // האזנה לרשימות המילים מהענן
        viewModel.getWordLists().observe(getViewLifecycleOwner(), wordLists -> {
            if (wordLists != null) {
                Log.d(TAG, "Observer: Received " + wordLists.size() + " lists.");
                adapter.setWordLists(wordLists);
            }
        });

        // האזנה להודעות Toast
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // האזנה למצב הטעינה (כדי להציג ProgressBar בעתיד אם נרצה)
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // כרגע לא עושים כלום, אבל התשתית קיימת
            Log.d(TAG, "isLoading state changed: " + isLoading);
        });
    }
}