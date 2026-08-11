package com.example.easylex.ui.mywords;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easylex.R;
import com.example.easylex.data.Word;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * =====================================================================
 * WordListAdapter — מתאם רשימת המילים האישית
 * =====================================================================
 *
 * מה זה מתאם (Adapter)?
 * ----------------------
 * ב-RecyclerView, ה-Adapter הוא הגשר בין רשימת המילים (List<Word>)
 * לבין הכרטיסים הוויזואליים (list_item_word.xml).
 * לכל Word ברשימה — ה-Adapter יוצר כרטיס ומאכלס אותו בנתונים.
 *
 * מה מוצג בכל כרטיס?
 * --------------------
 * ● מילה באנגלית + [חלק הדיבר] (הופך לאפור קטן יותר — SpannableString)
 * ● תרגום לעברית
 * ● משפט דוגמה באנגלית
 * ● משפט דוגמה בעברית
 * ● כפתור הרמקול — שולח אירוע ל-Fragment להקראה ב-TTS
 *
 * סינון משולש:
 * ------------
 * ה-Adapter שומר שלושה פילטרים בו-זמנית:
 *   currentSearchQuery — חיפוש טקסט חופשי (מחרוזת)
 *   currentPOSFilter  — סינון לפי חלק הדיבר (n/v/adj/...)
 *   currentTagFilter  — סינון לפי קטגוריה (תגית)
 * applyCurrentFilters() מפעיל את כל השלושה יחד בכל שינוי.
 *
 * אינדקס צד (Side Index):
 * -------------------------
 * getPositionForLetter() — מחזיר את המיקום הראשון של מילה שמתחילה
 * באות מסוימת. נקרא ע"י MyWordsFragment לגלילה מהירה לפי א-ב.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * MyWordsFragment — יוצר את ה-Adapter ומפעיל את הפילטרים.
 */
public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.WordViewHolder> implements Filterable {

    public interface OnPronounceClickListener {
        void onPronounceClick(String textToSpeak);
    }

    private OnPronounceClickListener pronounceClickListener;
    private List<Word> wordListFull = new ArrayList<>();
    private List<Word> wordListFiltered = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentPOSFilter = "All";
    private String currentTagFilter = "All";

    public void setOnPronounceClickListener(OnPronounceClickListener listener) {
        this.pronounceClickListener = listener;
    }

    public void setWords(List<Word> words) {
        if (words != null) {
            this.wordListFull = new ArrayList<>(words);
            applyCurrentFilters();
        }
    }

    public void filterByText(String query) {
        this.currentSearchQuery = query.toLowerCase(Locale.ROOT).trim();
        applyCurrentFilters();
    }

    public void filterByPOSAndTag(String pos, String tag) {
        this.currentPOSFilter = pos;
        this.currentTagFilter = tag;
        applyCurrentFilters();
    }

    private void applyCurrentFilters() {
        getFilter().filter(currentSearchQuery);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Word> filteredList = new ArrayList<>();
                for (Word item : wordListFull) {
                    if (item.getEnglishWord() == null) continue;

                    String eng = item.getEnglishWord().toLowerCase(Locale.ROOT);
                    boolean matchesSearch = eng.contains(currentSearchQuery);

                    // בדיקת POS - תומך בקיצורים ובשמות מלאים
                    boolean matchesPOS = currentPOSFilter.equals("All") ||
                            (item.getPartOfSpeech() != null && item.getPartOfSpeech().toLowerCase().contains(currentPOSFilter.toLowerCase()));

                    // בדיקת תגית
                    boolean matchesTag = currentTagFilter.equals("All") ||
                            (item.getTags() != null && item.getTags().contains(currentTagFilter));

                    if (matchesSearch && matchesPOS && matchesTag) {
                        filteredList.add(item);
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                wordListFiltered = (List<Word>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public int getPositionForLetter(String letter) {
        for (int i = 0; i < wordListFiltered.size(); i++) {
            String word = wordListFiltered.get(i).getEnglishWord();
            if (word != null && !word.trim().isEmpty() &&
                    word.trim().toUpperCase(Locale.ROOT).startsWith(letter.toUpperCase(Locale.ROOT))) {
                return i;
            }
        }
        return -1;
    }

    public String getLetterForPosition(int position) {
        if (position >= 0 && position < wordListFiltered.size()) {
            Word word = wordListFiltered.get(position);
            if (word != null && word.getEnglishWord() != null && !word.getEnglishWord().trim().isEmpty()) {
                return word.getEnglishWord().trim().substring(0, 1).toUpperCase(Locale.ROOT);
            }
        }
        return "";
    }

    @Override
    public int getItemCount() { return wordListFiltered.size(); }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        holder.bind(wordListFiltered.get(position), pronounceClickListener);
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEnglish, tvHebrew, tvExEng, tvExHeb;
        private final ImageButton btnSpeak;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEnglish = itemView.findViewById(R.id.textViewEnglishWord);
            tvHebrew = itemView.findViewById(R.id.textViewHebrewTranslation);
            tvExEng = itemView.findViewById(R.id.textViewExampleSentence);
            tvExHeb = itemView.findViewById(R.id.textViewHebrewExample);
            btnSpeak = itemView.findViewById(R.id.imageButtonPronounce);
        }

        public void bind(final Word word, final OnPronounceClickListener listener) {
            String eng = word.getEnglishWord();
            String pos = (word.getPartOfSpeech() != null && !word.getPartOfSpeech().isEmpty()) ? " (" + word.getPartOfSpeech() + ")" : "";

            SpannableString spannable = new SpannableString(eng + pos);
            if (!pos.isEmpty()) {
                int start = eng.length();
                spannable.setSpan(new RelativeSizeSpan(0.7f), start, spannable.length(), 0);
                spannable.setSpan(new ForegroundColorSpan(Color.GRAY), start, spannable.length(), 0);
            }

            tvEnglish.setText(spannable);
            tvHebrew.setText(word.getHebrewTranslation());
            tvExEng.setText(word.getExampleSentence());
            tvExHeb.setText(word.getHebrewExample());

            btnSpeak.setOnClickListener(v -> {
                if (listener != null && word.getEnglishWord() != null) listener.onPronounceClick(word.getEnglishWord());
            });
        }
    }
}