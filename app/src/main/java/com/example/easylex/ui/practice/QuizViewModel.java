package com.example.easylex.ui.practice;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.easylex.data.Word;
import com.example.easylex.data.WordRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuizViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final LiveData<List<Word>> allWords;

    // LiveData שיחזיק את השאלה הנוכחית
    private final MutableLiveData<QuizQuestion> currentQuestion = new MutableLiveData<>();

    private List<Word> wordList;
    private int currentQuestionIndex = 0;
    private final int totalQuestions = 10; // נתחיל עם מבחן של 10 שאלות

    public QuizViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);
        allWords = repository.getAllWords();

        // מאזינים לרשימת המילים. כשהיא מגיעה, מתחילים את המבחן.
        allWords.observeForever(words -> {
            if (words != null && !words.isEmpty()) {
                wordList = new ArrayList<>(words);
                startQuiz();
            }
        });
    }

    // ה-Fragment יאזין ל-LiveData הזה כדי לקבל שאלות חדשות
    public LiveData<QuizQuestion> getCurrentQuestion() {
        return currentQuestion;
    }

    // מתחיל את המבחן (או מאתחל אותו)
    public void startQuiz() {
        if (wordList != null && wordList.size() >= 4) {
            // מערבבים את רשימת המילים כדי שהשאלות יהיו אקראיות
            Collections.shuffle(wordList);
            currentQuestionIndex = 0;
            generateNextQuestion();
        }
    }

    // המתודה המרכזית: יוצרת שאלה חדשה
    private void generateNextQuestion() {
        if (wordList == null || currentQuestionIndex >= totalQuestions || currentQuestionIndex >= wordList.size()) {
            // סוף המבחן
            currentQuestion.setValue(null); // נסמן ל-Fragment שהמבחן נגמר
            return;
        }

        // 1. בחירת המילה לשאלה והתשובה הנכונה
        Word correctWord = wordList.get(currentQuestionIndex);
        String questionText = correctWord.getEnglishWord();
        String correctAnswer = correctWord.getHebrewTranslation();

        // 2. יצירת רשימת מסיחים
        ArrayList<String> options = new ArrayList<>();
        options.add(correctAnswer);

        // יצירת עותק של הרשימה ללא המילה הנכונה
        List<Word> tempWordList = new ArrayList<>(wordList);
        tempWordList.remove(correctWord);

        Random random = new Random();
        while (options.size() < 4 && !tempWordList.isEmpty()) {
            int randomIndex = random.nextInt(tempWordList.size());
            Word distractorWord = tempWordList.get(randomIndex);
            // ודא שהתרגום לא זהה לתשובה הנכונה או למסיח שכבר הוספנו
            if (!options.contains(distractorWord.getHebrewTranslation())) {
                options.add(distractorWord.getHebrewTranslation());
            }
            // הסר את המילה שבה השתמשנו כדי למנוע כפילויות
            tempWordList.remove(randomIndex);
        }

        // 3. ערבוב התשובות
        Collections.shuffle(options);

        // יצירת אובייקט שאלה חדש ועדכון ה-LiveData
        QuizQuestion newQuestion = new QuizQuestion(questionText, options, correctAnswer, currentQuestionIndex + 1, totalQuestions);
        currentQuestion.setValue(newQuestion);
    }

    // מתודה שה-Fragment יקרא לה כדי לעבור לשאלה הבאה
    public void moveToNextQuestion() {
        currentQuestionIndex++;
        generateNextQuestion();
    }

    // מחלקה פנימית פשוטה שמייצגת שאלה בודדת
    public static class QuizQuestion {
        public final String questionText;
        public final List<String> options;
        public final String correctAnswer;
        public final int questionNumber;
        public final int totalQuestions;

        public QuizQuestion(String questionText, List<String> options, String correctAnswer, int questionNumber, int totalQuestions) {
            this.questionText = questionText;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.questionNumber = questionNumber;
            this.totalQuestions = totalQuestions;
        }
    }
}