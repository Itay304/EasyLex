package com.example.easylex.ui.admin;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easylex.R;
import com.example.easylex.data.Word;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================
 * AdminEditFragment — ממשק עריכת מאגר המילים הגלובלי (למנהל בלבד)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * מאפשר למנהל לנהל את מאגר מילות הבגרות הגלובלי ב-Firestore.
 * כל שינוי כאן מגיע לכל משתמשי האפליקציה (דרך addSnapshotListener).
 *
 * פעולות עיקריות:
 * ----------------
 * ● בחירת רשימה מתוך תפריט נפתח (adminListSpinner) — נטען דינמית מ-word_lists,
 *   ברירת מחדל band_2_full_list לתאימות לאחור
 * ● טעינת כל המילים מ-Firestore (collection: word_lists/{currentListId}/words)
 * ● FAB (כפתור +) → פתיחת דיאלוג הוספת מילה חדשה ל-Firestore, לרשימה הנבחרת
 * ● החלקת כרטיס שמאלה/ימינה → מחיקה מ-Firestore, מהרשימה הנבחרת
 * ● Snackbar "בטל" — שחזור המילה המחוקה מ-Firestore
 * ● חיפוש בשדה → גלילה לפוזיציה המתאימה ב-RecyclerView
 *
 * ארכיטקטורה:
 * -----------
 * AdminEditFragment ↔ Firestore ישירות (לא דרך Room/Repository)
 * כי הנתונים כאן הם גלובליים, לא אישיים.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * AdminWordAdapter — מציג את הרשימה
 * nav_graph.xml — navigation_admin
 */
public class AdminEditFragment extends Fragment {

    // Firestore path
    private static final String COL  = "word_lists";
    private static final String DEFAULT_LIST_ID = "band_2_full_list";
    private static final String SUB  = "words";

    /** הרשימה הנבחרת כרגע ב-Spinner. ברירת מחדל = הרשימה הישנה, לתאימות לאחור. */
    private String currentListId = DEFAULT_LIST_ID;

    private Spinner listSpinner;
    private final List<String> listIds = new ArrayList<>();
    /** מונע הפעלת onItemSelected כתוצאה מבחירה פרוגרמטית (לא מבוצעת ע"י המשתמש). */
    private boolean isProgrammaticSelection = false;

    private View rootView;
    private RecyclerView recyclerView;
    private AdminWordAdapter adapter;
    private TextView tvProgress;
    private LinearProgressIndicator progressBar;

    // Full lists — kept in sync with the adapter for search scrolling
    private final List<Word>   allWords  = new ArrayList<>();
    private final List<String> allDocIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_admin_edit, container, false);

        tvProgress   = rootView.findViewById(R.id.tvAdminProgress);
        progressBar  = rootView.findViewById(R.id.adminProgressBar);
        recyclerView = rootView.findViewById(R.id.adminRecyclerView);
        listSpinner  = rootView.findViewById(R.id.adminListSpinner);

        adapter = new AdminWordAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        setupSwipeToDelete();
        setupSearch();
        setupFab();
        setupListSpinner();
        loadWords(); // טוען מיד את currentListId (ברירת מחדל) — לא ממתין לטעינת ה-Spinner

        return rootView;
    }

    // ── List selector ─────────────────────────────────────────────────────────

    /**
     * setupListSpinner — טוען דינמית את כל מזהי הרשימות מ-word_lists (אין רשימה קשיחה),
     * ומאפשר למנהל לעבור בין רשימות. הבחירה הראשונית נשארת band_2_full_list
     * (loadWords() כבר רץ מעליה ב-onCreateView) — כשהרשימות מגיעות, רק מסמנים
     * את הפריט הנכון ב-Spinner בלי לגרום לטעינה כפולה.
     */
    private void setupListSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, listIds);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listSpinner.setAdapter(spinnerAdapter);

        FirebaseFirestore.getInstance().collection(COL).get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    listIds.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) listIds.add(d.getId());
                    if (listIds.isEmpty()) return;
                    if (!listIds.contains(currentListId)) currentListId = listIds.get(0);
                    spinnerAdapter.notifyDataSetChanged();
                    isProgrammaticSelection = true;
                    listSpinner.setSelection(listIds.indexOf(currentListId));
                });

        listSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isProgrammaticSelection) { isProgrammaticSelection = false; return; }
                String selected = listIds.get(position);
                if (selected.equals(currentListId)) return;
                currentListId = selected;
                loadWords();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadWords() {
        FirebaseFirestore.getInstance()
                .collection(COL).document(currentListId)
                .collection(SUB).orderBy("englishWord").get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;

                    allWords.clear();
                    allDocIds.clear();
                    int verified = 0;

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Word w = d.toObject(Word.class);
                        if (w != null) {
                            allWords.add(w);
                            allDocIds.add(d.getId());
                            if (w.isVerified()) verified++;
                        }
                    }

                    tvProgress.setText("מילות הבנק: " + allWords.size()
                            + "  (מאומתות: " + verified + ")");
                    progressBar.setMax(allWords.size() > 0 ? allWords.size() : 1);
                    progressBar.setProgress(verified);

                    adapter.setData(allWords, allDocIds);
                });
    }

    // ── Swipe-to-delete ───────────────────────────────────────────────────────

    private void setupSwipeToDelete() {
        ColorDrawable redBg = new ColorDrawable(Color.parseColor("#E53935"));

        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // no drag-and-drop
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int state, boolean active) {
                View item = vh.itemView;
                if (dX > 0) {
                    redBg.setBounds(item.getLeft(), item.getTop(),
                            item.getLeft() + (int) dX, item.getBottom());
                } else {
                    redBg.setBounds(item.getRight() + (int) dX, item.getTop(),
                            item.getRight(), item.getBottom());
                }
                redBg.draw(c);
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int     pos     = vh.getAdapterPosition();
                Word    removed = adapter.getWord(pos);
                String  docId   = adapter.getDocId(pos);

                // 1. Remove from UI immediately (optimistic)
                adapter.removeItem(pos);

                // 2. Delete from Firestore
                FirebaseFirestore.getInstance()
                        .collection(COL).document(currentListId)
                        .collection(SUB).document(docId)
                        .delete();

                // 3. Offer undo — re-adds document with original data
                Snackbar.make(rootView,
                                "\"" + removed.getEnglishWord() + "\" נמחקה",
                                Snackbar.LENGTH_LONG)
                        .setAction("בטל", v ->
                                FirebaseFirestore.getInstance()
                                        .collection(COL).document(currentListId)
                                        .collection(SUB).document(docId)
                                        .set(wordToMap(removed))
                                        .addOnSuccessListener(a -> {
                                            adapter.insertItem(pos, removed, docId);
                                            recyclerView.scrollToPosition(pos);
                                        })
                        )
                        .show();
            }
        };

        new ItemTouchHelper(cb).attachToRecyclerView(recyclerView);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        SearchView sv = rootView.findViewById(R.id.adminSearchView);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) {
                String lower = q.toLowerCase();
                for (int i = 0; i < allWords.size(); i++) {
                    String eng = allWords.get(i).getEnglishWord();
                    if (eng != null && eng.toLowerCase().startsWith(lower)) {
                        recyclerView.scrollToPosition(i);
                        break;
                    }
                }
                return true;
            }
        });
    }

    // ── FAB — Add word ────────────────────────────────────────────────────────

    private void setupFab() {
        FloatingActionButton fab = rootView.findViewById(R.id.fabAddWord);
        fab.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        View dv = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_admin_add_word, null);

        TextInputEditText etEng   = dv.findViewById(R.id.etAdminEng);
        TextInputEditText etHeb   = dv.findViewById(R.id.etAdminHeb);
        TextInputEditText etPOS   = dv.findViewById(R.id.etAdminPOS);
        TextInputEditText etExEng = dv.findViewById(R.id.etAdminExEng);
        TextInputEditText etExHeb = dv.findViewById(R.id.etAdminExHeb);
        TextInputEditText etTags  = dv.findViewById(R.id.etAdminTags);

        new AlertDialog.Builder(requireContext())
                .setTitle("הוספת מילה חדשה")
                .setView(dv)
                .setPositiveButton("הוסף", (dialog, which) -> {
                    String eng = text(etEng);
                    String heb = text(etHeb);
                    if (TextUtils.isEmpty(eng) || TextUtils.isEmpty(heb)) {
                        Toast.makeText(requireContext(),
                                "מילה ותרגום הם שדות חובה", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addToFirestore(eng, heb, text(etPOS), text(etExEng),
                                   text(etExHeb), text(etTags));
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void addToFirestore(String eng, String heb, String pos,
                                String exEng, String exHeb, String tags) {
        Map<String, Object> data = new HashMap<>();
        data.put("englishWord",       eng);
        data.put("hebrewTranslation", heb);
        data.put("partOfSpeech",      pos);
        data.put("exampleSentence",   exEng);
        data.put("hebrewExample",     exHeb);
        data.put("tags",              tags);
        data.put("isVerified",        true);
        data.put("creationTimestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection(COL).document(currentListId)
                .collection(SUB)
                .add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(requireContext(),
                            "\"" + eng + "\" נוספה בהצלחה!", Toast.LENGTH_SHORT).show();
                    loadWords(); // reload full list so new word appears in sorted position
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static Map<String, Object> wordToMap(Word w) {
        Map<String, Object> m = new HashMap<>();
        m.put("englishWord",       w.getEnglishWord());
        m.put("hebrewTranslation", w.getHebrewTranslation());
        m.put("partOfSpeech",      w.getPartOfSpeech());
        m.put("exampleSentence",   w.getExampleSentence());
        m.put("hebrewExample",     w.getHebrewExample());
        m.put("tags",              w.getTags());
        m.put("isVerified",        w.isVerified());
        m.put("creationTimestamp", w.getCreationTimestamp());
        return m;
    }
}
