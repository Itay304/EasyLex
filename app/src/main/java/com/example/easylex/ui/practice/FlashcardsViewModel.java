package com.example.easylex.ui.practice;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.easylex.data.Word;
import com.example.easylex.data.WordRepository;
import java.util.List;

public class FlashcardsViewModel extends AndroidViewModel {

    private WordRepository repository;
    private LiveData<List<Word>> allWords;

    public FlashcardsViewModel(@NonNull Application application) {
        super(application);
        // כאן אנחנו קוראים מהמאגר המקומי (Room), כי המשתמש אמור לתרגל
        // את המילים שהוא כבר הוריד/סרק.
        repository = new WordRepository(application);
        allWords = repository.getAllWords();
    }

    public LiveData<List<Word>> getAllWords() {
        return allWords;
    }
}