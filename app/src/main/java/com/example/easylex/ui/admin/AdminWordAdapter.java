package com.example.easylex.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easylex.R;
import com.example.easylex.data.Word;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * AdminWordAdapter — מתאם רשימת מילים לממשק המנהל
 * =====================================================================
 *
 * מה זה מתאם (Adapter)?
 * ----------------------
 * ב-RecyclerView, ה-Adapter הוא הגשר בין הנתונים (רשימת Word)
 * לבין הכרטיסים הוויזואליים (item_admin_word_card.xml).
 * לכל פריט ברשימה — ה-Adapter יוצר כרטיס ומאכלס אותו בנתונים.
 *
 * מה מוצג בכל כרטיס?
 * --------------------
 * ● שורה עליונה: מילה באנגלית [חלק הדיבר] | תרגום לעברית
 * ● שורה תחתונה: משפט דוגמה באנגלית + משפט דוגמה בעברית
 * ● כיוון כרטיס: layoutDirection=ltr — עברית ואנגלית מוצגות נכון ביחד
 *
 * מחיקה ושחזור (Undo):
 * ----------------------
 * המחיקה עצמה מתבצעת ב-AdminEditFragment דרך ItemTouchHelper.
 * removeItem() + insertItem() מאפשרים Optimistic UI Update:
 * המסך מתעדכן מיידית, לפני אישור ה-Firestore.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * AdminEditFragment — יוצר, מאכלס, ומנהל את ה-Adapter.
 */
public class AdminWordAdapter extends RecyclerView.Adapter<AdminWordAdapter.AdminViewHolder> {

    private final List<Word>   words  = new ArrayList<>();
    private final List<String> docIds = new ArrayList<>();

    public void setData(List<Word> newWords, List<String> newDocIds) {
        words.clear();
        docIds.clear();
        words.addAll(newWords);
        docIds.addAll(newDocIds);
        notifyDataSetChanged();
    }

    /** Returns the word at position — used by ItemTouchHelper before removal. */
    public Word getWord(int position) {
        return words.get(position);
    }

    /** Returns the Firestore document ID at position. */
    public String getDocId(int position) {
        return docIds.get(position);
    }

    /** Optimistic removal — call after initiating Firestore delete. */
    public void removeItem(int position) {
        words.remove(position);
        docIds.remove(position);
        notifyItemRemoved(position);
    }

    /** Re-insert a word (used by the Undo Snackbar action). */
    public void insertItem(int position, Word word, String docId) {
        words.add(position, word);
        docIds.add(position, docId);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_word_card, parent, false);
        return new AdminViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        Word w = words.get(position);

        // English word + part of speech badge
        String pos = w.getPartOfSpeech();
        if (pos != null && !pos.isEmpty()) {
            holder.tvEnglish.setText(w.getEnglishWord() + "  [" + pos + "]");
        } else {
            holder.tvEnglish.setText(w.getEnglishWord());
        }

        holder.tvHebrew.setText(nullToEmpty(w.getHebrewTranslation()));
        holder.tvExEng.setText(nullToEmpty(w.getExampleSentence()));
        holder.tvExHeb.setText(nullToEmpty(w.getHebrewExample()));

        // Hide sentence rows if both are empty (keeps cards compact)
        boolean hasSentences = !nullToEmpty(w.getExampleSentence()).isEmpty()
                            || !nullToEmpty(w.getHebrewExample()).isEmpty();
        holder.tvExEng.setVisibility(hasSentences ? View.VISIBLE : View.GONE);
        holder.tvExHeb.setVisibility(hasSentences ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView tvEnglish, tvHebrew, tvExEng, tvExHeb;

        public AdminViewHolder(@NonNull View v) {
            super(v);
            tvEnglish = v.findViewById(R.id.tvAdminEnglish);
            tvHebrew  = v.findViewById(R.id.tvAdminHebrew);
            tvExEng   = v.findViewById(R.id.tvAdminExEng);
            tvExHeb   = v.findViewById(R.id.tvAdminExHeb);
        }
    }
}
