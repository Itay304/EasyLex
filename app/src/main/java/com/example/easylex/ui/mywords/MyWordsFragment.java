package com.example.easylex.ui.mywords;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easylex.R;
import com.example.easylex.data.Word;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MyWordsFragment extends Fragment implements WordListAdapter.OnPronounceClickListener {

    private MyWordsViewModel viewModel;
    private WordListAdapter adapter;
    private RecyclerView recyclerView;
    private TextToSpeech tts;
    private LinearLayout sideIndex;
    private AutoCompleteTextView spinnerPOS, spinnerTags;
    private PopupWindow bubblePopup;
    private TextView bubbleTextView, lastSelectedLetter = null;

    private List<Word> masterList = new ArrayList<>();
    private String selectedPOS = "All", selectedTag = "All";
    private boolean showPersonalOnly = true; // default: show personal words only

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_words, container, false);
        ((MaterialToolbar) root.findViewById(R.id.toolbar))
            .setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        sideIndex  = root.findViewById(R.id.side_index);
        spinnerPOS = root.findViewById(R.id.spinnerPOS);
        spinnerTags = root.findViewById(R.id.spinnerTags);

        initializeTts();
        setupRecyclerView(root);
        setupViewModel();
        setupSearch(root);
        createBubblePopup();
        setupSideIndex();
        setupFilterChips(root);
        setupFab(root);

        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter chips: All words / My words
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFilterChips(View root) {
        Chip chipAll      = root.findViewById(R.id.chipFilterAll);
        Chip chipPersonal = root.findViewById(R.id.chipFilterPersonal);

        // Reflect default selection in the UI (personal chip starts checked)
        chipAll.setChecked(false);
        chipPersonal.setChecked(true);

        chipAll.setOnClickListener(v -> {
            showPersonalOnly = false;
            applyPersonalFilter();
        });
        chipPersonal.setOnClickListener(v -> {
            showPersonalOnly = true;
            applyPersonalFilter();
        });
    }

    private void applyPersonalFilter() {
        List<Word> source;
        if (showPersonalOnly) {
            source = new ArrayList<>();
            // Personal words are always isVerified=false AND isFavorite=true
            for (Word w : masterList) if (!w.isVerified() && w.isFavorite()) source.add(w);
        } else {
            source = new ArrayList<>(masterList);
        }
        adapter.setWords(source);
        adapter.filterByPOSAndTag(selectedPOS, selectedTag);
        setupDropdowns();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FAB — Manual word entry
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFab(View root) {
        FloatingActionButton fab = root.findViewById(R.id.fabAddWord);
        fab.setOnClickListener(v -> showAddWordDialog());
    }

    private void showAddWordDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_word, null);
        TextInputEditText etEnglish  = v.findViewById(R.id.etEnglish);
        TextInputEditText etHeb      = v.findViewById(R.id.etHebrew);
        AutoCompleteTextView actvTag = v.findViewById(R.id.actvTag);
        AutoCompleteTextView actvPos = v.findViewById(R.id.actvPos);

        // Populate tag suggestions from existing words
        Set<String> tagSet = new HashSet<>();
        for (Word w : masterList) {
            if (w.getTags() != null && !w.getTags().isEmpty()) tagSet.add(w.getTags());
        }
        actvTag.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(tagSet)));
        actvTag.setOnClickListener(vv -> actvTag.showDropDown());

        final String[] posDisplay = {
                "שם עצם (n)", "פועל (v)", "תואר (adj)", "תואר פועל (adv)",
                "מילת יחס (prep)", "כינוי (pron)", "אחר"
        };
        final String[] posCodes = {"n", "v", "adj", "adv", "prep", "pron", ""};
        actvPos.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, posDisplay));
        actvPos.setOnClickListener(vv -> actvPos.showDropDown());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("הוסף מילה ידנית")
                .setView(v)
                .setPositiveButton("שמור", (d, which) -> {
                    String eng  = etEnglish.getText() != null ? etEnglish.getText().toString().trim() : "";
                    String heb  = etHeb.getText()     != null ? etHeb.getText().toString().trim()     : "";
                    String tag  = actvTag.getText()   != null ? actvTag.getText().toString().trim()   : "";
                    String posT = actvPos.getText()   != null ? actvPos.getText().toString().trim()   : "";

                    if (eng.isEmpty()) {
                        Toast.makeText(requireContext(), "יש להזין מילה באנגלית", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String posCode = "";
                    for (int i = 0; i < posDisplay.length; i++) {
                        if (posDisplay[i].equals(posT)) { posCode = posCodes[i]; break; }
                    }
                    Word newWord = new Word(eng, heb, posCode, "", "", 1, System.currentTimeMillis());
                    newWord.setTags(tag);
                    newWord.setFavorite(true);
                    newWord.setVerified(false); // personal word — never global
                    viewModel.insert(newWord);
                    Toast.makeText(requireContext(), "נשמר!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ViewModel
    // ─────────────────────────────────────────────────────────────────────────

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MyWordsViewModel.class);
        viewModel.getAllWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null) {
                List<Word> cleanList = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (Word w : words) {
                    if (w.getEnglishWord() != null) {
                        String key = w.getEnglishWord().toLowerCase().trim();
                        if (seen.add(key)) cleanList.add(w);
                    }
                }
                Collections.sort(cleanList, (w1, w2) ->
                        w1.getEnglishWord().compareToIgnoreCase(w2.getEnglishWord()));

                masterList = cleanList;
                applyPersonalFilter();
            }
        });
    }

    private void setupDropdowns() {
        final String[] posDisplay = {
                "הכל (All)", "שם עצם (n)", "פועל (v)", "שם תואר (adj)",
                "תואר הפועל (adv)", "כינוי גוף (pron)", "מילת יחס (prep)",
                "מילת קישור (conj)", "מילת קריאה (interj)"
        };
        final String[] posValues = {"All", "n", "v", "adj", "adv", "pron", "prep", "conj", "interj"};

        ArrayAdapter<String> pAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, posDisplay);
        spinnerPOS.setAdapter(pAdapter);
        spinnerPOS.setOnItemClickListener((p, vv, position, id) -> {
            selectedPOS = posValues[position];
            adapter.filterByPOSAndTag(selectedPOS, selectedTag);
        });

        Set<String> tagSet = new HashSet<>();
        tagSet.add("All");
        for (Word w : masterList) {
            if (w.getTags() != null) tagSet.add(w.getTags().split(",")[0].trim());
        }
        List<String> sortedTags = new ArrayList<>(tagSet);
        Collections.sort(sortedTags);

        ArrayAdapter<String> tAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, sortedTags);
        spinnerTags.setAdapter(tAdapter);
        spinnerTags.setOnItemClickListener((p, vv, position, id) -> {
            selectedTag = sortedTags.get(position);
            adapter.filterByPOSAndTag(selectedPOS, selectedTag);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Side index
    // ─────────────────────────────────────────────────────────────────────────

    private void setupSideIndex() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        sideIndex.removeAllViews();
        for (char letter : alphabet.toCharArray()) {
            TextView tv = new TextView(requireContext());
            tv.setText(String.valueOf(letter));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(10f);
            tv.setTextColor(getResources().getColor(R.color.turquoise_accent, null));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            tv.setLayoutParams(p);
            sideIndex.addView(tv);
        }
        sideIndex.setOnTouchListener((v, event) -> {
            float y = event.getY();
            int index = (int) (y / (v.getHeight() / 26f));
            if (index >= 0 && index < 26) {
                String letter = String.valueOf(alphabet.charAt(index));
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN) {
                    showBubble(letter, y, v);
                    scrollToLetter(letter);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (bubblePopup.isShowing()) bubblePopup.dismiss();
                }
            }
            return true;
        });
    }

    private void highlightLetter(String letter) {
        if (lastSelectedLetter != null) {
            lastSelectedLetter.setBackgroundResource(0);
            lastSelectedLetter.setTypeface(null, Typeface.NORMAL);
        }
        for (int i = 0; i < sideIndex.getChildCount(); i++) {
            TextView tv = (TextView) sideIndex.getChildAt(i);
            if (tv.getText().toString().equalsIgnoreCase(letter)) {
                tv.setBackgroundResource(R.drawable.letter_selected_background);
                tv.setTypeface(null, Typeface.BOLD);
                lastSelectedLetter = tv;
                break;
            }
        }
    }

    private void scrollToLetter(String letter) {
        int pos = adapter.getPositionForLetter(letter);
        if (pos != -1)
            ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .scrollToPositionWithOffset(pos, 0);
    }

    private void showBubble(String letter, float y, View anchor) {
        bubbleTextView.setText(letter);
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int x = loc[0] - 160;
        int yPos = (int) (loc[1] + y - 100);
        if (!bubblePopup.isShowing()) bubblePopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, yPos);
        else bubblePopup.update(x, yPos, -1, -1);
    }

    private void createBubblePopup() {
        View b = LayoutInflater.from(getContext()).inflate(R.layout.bubble_layout, null);
        bubbleTextView = b.findViewById(R.id.bubble_textview);
        bubblePopup = new PopupWindow(b, 160, 160);
        bubblePopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView + Search + TTS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupRecyclerView(View root) {
        recyclerView = root.findViewById(R.id.recyclerViewWords);
        adapter = new WordListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.setOnPronounceClickListener(this);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int first = lm.findFirstVisibleItemPosition();
                    String letter = adapter.getLetterForPosition(first);
                    if (!letter.isEmpty()) highlightLetter(letter);
                }
            }
        });
    }

    private void setupSearch(View root) {
        SearchView sv = root.findViewById(R.id.search_view);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) {
                adapter.filterByText(q);
                return true;
            }
        });
    }

    private void initializeTts() {
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
    }

    @Override
    public void onPronounceClick(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (bubblePopup != null && bubblePopup.isShowing()) bubblePopup.dismiss();
    }
}
