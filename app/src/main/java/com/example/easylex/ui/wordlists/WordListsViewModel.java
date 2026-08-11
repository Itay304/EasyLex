package com.example.easylex.ui.wordlists;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.easylex.data.Word;
import com.example.easylex.data.WordList;
import com.example.easylex.data.WordRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class WordListsViewModel extends AndroidViewModel {

    private static final String TAG = "WordListsViewModel";

    private final MutableLiveData<List<WordList>> wordLists = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private final FirebaseFirestore db;
    private final WordRepository wordRepository;

    public WordListsViewModel(@NonNull Application application) {
        super(application);

        // --- הפעלת מצב Offline Persistence ---
        // פעולה זו מאפשרת ל-Firestore לשמור עותק של הנתונים על המכשיר,
        // מה שמשפר את חווית המשתמש במצב לא מקוון או ברשת איטית.
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);
        // ------------------------------------

        wordRepository = new WordRepository(application);
        fetchWordLists();
    }

    // --- Getters עבור ה-Fragment ---
    public LiveData<List<WordList>> getWordLists() { return wordLists; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    /**
     * פונה ל-Firestore, מביא את רשימת ה-'word_lists' ומעדכן את ה-LiveData.
     */
    private void fetchWordLists() {
        Log.d(TAG, "fetchWordLists: מתחיל שליפה מ-Firestore...");
        isLoading.setValue(true);
        db.collection("word_lists")
                .get()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "fetchWordLists: onComplete הופעל.");
                    if (task.isSuccessful()) {
                        Log.d(TAG, "fetchWordLists: המשימה הצליחה.");
                        ArrayList<WordList> lists = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // המרת כל מסמך לאובייקט WordList
                            String id = document.getId();
                            String name = document.getString("name");
                            long wordCountLong = document.getLong("wordCount") != null ? document.getLong("wordCount") : 0;
                            int wordCount = (int) wordCountLong;
                            lists.add(new WordList(id, name, wordCount));
                        }
                        Log.d(TAG, "fetchWordLists: נמצאו " + lists.size() + " רשימות.");
                        wordLists.setValue(lists); // עדכון ה-LiveData עבור ה-UI
                    } else {
                        Log.e(TAG, "fetchWordLists: המשימה נכשלה.", task.getException());
                        toastMessage.setValue("טעינת הרשימות נכשלה.");
                    }
                    isLoading.setValue(false);
                });
    }

    /**
     * מוריד את כל המילים מתת-אוסף של רשימה ספציפית מהענן,
     * ומכניס אותן לבסיס הנתונים המקומי (Room).
     * @param listToDownload הרשימה שנבחרה להורדה.
     */
    public void downloadWordList(WordList listToDownload) {
        isLoading.setValue(true);
        toastMessage.setValue("מתחיל הורדה...");
        Log.d(TAG, "downloadWordList: מתחיל הורדה של רשימה: " + listToDownload.getName());

        db.collection("word_lists").document(listToDownload.getId()).collection("words")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "downloadWordList: הצליח לשלוף " + queryDocumentSnapshots.size() + " מילים מהענן.");

                    if (queryDocumentSnapshots.isEmpty()) {
                        toastMessage.setValue("לא נמצאו מילים להורדה.");
                        isLoading.setValue(false);
                        return;
                    }

                    List<Word> wordsToInsert = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // המרת כל מסמך מהענן לאובייקט Word
                        Word word = doc.toObject(Word.class);
                        // קביעת חותמת זמן כדי שהמילה תופיע בראש הרשימה
                        word.setCreationTimestamp(System.currentTimeMillis());
                        // מילה גלובלית מרשימה מאומתת — לא מילה אישית (ר' isVerified ב-Word.java).
                        // בלי זה המילה נחשבת "אישית" ועלולה להימחק ב-migration החד-פעמי.
                        word.setVerified(true);
                        word.setSourceListId(listToDownload.getId());
                        wordsToInsert.add(word);
                    }
                    Log.d(TAG, "downloadWordList: מתחיל הכנסה של " + wordsToInsert.size() + " מילים לבסיס הנתונים המקומי.");

                    // ה-Repository כבר מנהל תהליכוני רקע, לכן אפשר לקרוא לו ישירות.
                    for(Word w : wordsToInsert) {
                        wordRepository.insert(w);
                    }

                    Log.d(TAG, "downloadWordList: הסתיימה ההכנסה.");
                    toastMessage.setValue("ההורדה הושלמה! " + wordsToInsert.size() + " מילים נוספו.");
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "downloadWordList: נכשל בהורדת המילים.", e);
                    toastMessage.setValue("ההורדה נכשלה.");
                    isLoading.setValue(false);
                });
    }
}