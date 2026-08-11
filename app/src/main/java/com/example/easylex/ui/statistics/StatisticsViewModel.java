package com.example.easylex.ui.statistics;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.easylex.data.Word;
import com.example.easylex.data.WordRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatisticsViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final LiveData<List<Word>> allWords;

    // LiveData לנתונים המחושבים
    private final MutableLiveData<Integer> totalWordsCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> successRate = new MutableLiveData<>();
    private final MediatorLiveData<List<Word>> difficultWords = new MediatorLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);
        allWords = repository.getAllWords();

        // מאזינים לרשימת המילים המלאה, ובכל פעם שהיא משתנה, מחשבים מחדש את הסטטיסטיקות
        difficultWords.addSource(allWords, words -> {
            if (words != null) {
                calculateStatistics(words);
            }
        });
    }

    // מתודות שה-Fragment ישתמש בהן
    public LiveData<Integer> getTotalWordsCount() { return totalWordsCount; }
    public LiveData<Integer> getSuccessRate() { return successRate; }
    public LiveData<List<Word>> getDifficultWords() { return difficultWords; }

    private void calculateStatistics(List<Word> words) {
        // 1. סך הכל מילים
        totalWordsCount.setValue(words.size());

        // 2. חישוב אחוז הצלחה
        long totalAttempts = 0;
        long correctAttempts = 0;
        for (Word word : words) {
            totalAttempts += word.getTotalAttempts();
            correctAttempts += word.getCorrectAttempts();
        }
        if (totalAttempts > 0) {
            int rate = (int) ((correctAttempts * 100) / totalAttempts);
            successRate.setValue(rate);
        } else {
            successRate.setValue(0); // למניעת חלוקה באפס
        }

        // 3. מציאת המילים הקשות ביותר
        List<Word> practicedWords = new ArrayList<>();
        for (Word word : words) {
            if (word.getTotalAttempts() > 0) {
                practicedWords.add(word);
            }
        }

        // מיון המילים: הקשות ביותר (אחוז הצלחה נמוך) יופיעו ראשונות
        Collections.sort(practicedWords, Comparator.comparingDouble(word -> (double) word.getCorrectAttempts() / word.getTotalAttempts()));

        // לוקחים עד 5 המילים הקשות ביותר להצגה
        List<Word> topDifficultWords = new ArrayList<>();
        for (int i = 0; i < Math.min(5, practicedWords.size()); i++) {
            topDifficultWords.add(practicedWords.get(i));
        }
        difficultWords.setValue(topDifficultWords);
    }
}