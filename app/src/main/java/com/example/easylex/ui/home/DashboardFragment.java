package com.example.easylex.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easylex.R;
import com.example.easylex.data.Word;
import com.example.easylex.databinding.FragmentDashboardBinding;
import com.example.easylex.ui.gamification.GamificationEngine;
import com.example.easylex.ui.mywords.MyWordsViewModel;
import com.example.easylex.ui.settings.SettingsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private GamificationEngine gamification;
    private List<Word> masterWords = new ArrayList<>();

    private static final String[] QUOTES = {
        "\"The limits of my language mean the limits of my world.\"",
        "\"Learning is a treasure that will follow its owner everywhere.\"",
        "\"A different language is a different vision of life.\"",
        "\"Do something today that your future self will thank you for.\""
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding      = FragmentDashboardBinding.inflate(inflater, container, false);
        gamification = GamificationEngine.getInstance(requireContext());

        setupGreeting();
        binding.tvQuote.setText(getDailyQuote());

        binding.cardDictionary.setOnClickListener(v ->
            Navigation.findNavController(v)
                .navigate(R.id.action_navigation_home_to_navigation_word_list));

        binding.cardWordLists.setOnClickListener(v ->
            Navigation.findNavController(v)
                .navigate(R.id.action_home_to_word_lists));

        binding.cardQuickQuiz.setOnClickListener(v -> navigateToQuiz("DAILY"));
        binding.cardQuickMistakes.setOnClickListener(v -> navigateToQuiz("MISTAKES"));
        binding.cardQuickFlashcards.setOnClickListener(v ->
            Navigation.findNavController(binding.getRoot())
                .navigate(R.id.action_home_to_flashcards));
        binding.cardQuickSpelling.setOnClickListener(v ->
            Navigation.findNavController(binding.getRoot())
                .navigate(R.id.action_home_to_spelling));

        new ViewModelProvider(this).get(MyWordsViewModel.class)
            .getAllWords()
            .observe(getViewLifecycleOwner(), this::onWordsLoaded);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        gamification.syncFromFirestore(this::refreshStats);
        refreshStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data-driven sections
    // ──────────────────────────────────────────────────────────────────────────

    private void onWordsLoaded(List<Word> words) {
        if (words == null) return;
        masterWords = words;
        setupMistakesRow(words);
        setupWordOfDay(words);
        setupDailyChallenges(words);
    }

    private void setupWordOfDay(List<Word> words) {
        if (binding == null || words == null || words.isEmpty()) return;
        int idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % words.size();
        Word w = words.get(idx);
        String pos = w.getPartOfSpeech();
        binding.tvWodPos.setText(pos != null && !pos.isEmpty() ? pos : "word");
        binding.tvWodEnglish.setText(w.getEnglishWord());
        binding.tvWodHebrew.setText(w.getHebrewTranslation() != null ? w.getHebrewTranslation() : "");
        String ex = w.getExampleSentence();
        if (ex != null && !ex.isEmpty()) {
            binding.tvWodExample.setText(ex);
            binding.tvWodExample.setVisibility(View.VISIBLE);
        } else {
            binding.tvWodExample.setVisibility(View.GONE);
        }
    }

    private void setupDailyChallenges(List<Word> words) {
        if (binding == null) return;
        binding.progressChallenge1.setMax(100);
        binding.progressChallenge2.setMax(100);

        // Challenge 1: correct answers today — goal from Settings (daily goal)
        int dailyGoal = requireContext()
                .getSharedPreferences(SettingsFragment.PREFS_SETTINGS, Context.MODE_PRIVATE)
                .getInt(SettingsFragment.KEY_DAILY_GOAL, 10);
        int dailyCorrect = gamification.getDailyCorrectCount();
        binding.tvChallenge1Label.setText("תרגל " + dailyGoal + " מילים היום 🎯");
        binding.tvChallenge1Progress.setText(dailyCorrect + "/" + dailyGoal);
        binding.progressChallenge1.setProgress(dailyGoal > 0 ? Math.min(100, dailyCorrect * 100 / dailyGoal) : 0);

        // Challenge 2: words with at least 1 correct attempt vs total
        if (words == null || words.isEmpty()) return;
        int learned = 0;
        for (Word w : words) if (w.getCorrectAttempts() >= 1) learned++;
        int total = words.size();
        binding.tvChallenge2Progress.setText(learned + "/" + total);
        binding.progressChallenge2.setProgress(total == 0 ? 0 : (int)(learned * 100L / total));
    }

    private void setupWeeklyActivity() {
        if (binding == null) return;
        int[] weekly = gamification.getWeeklyActivity();
        int maxVal = 1;
        for (int v : weekly) if (v > maxVal) maxVal = v;

        binding.llWeeklyBars.removeAllViews();
        String[] dayLabels = buildDayLabels();

        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, dpToPx(80), 1f);
            col.setLayoutParams(colLp);

            View bar = new View(requireContext());
            int barH = weekly[i] == 0 ? dpToPx(5)
                : Math.max(dpToPx(5), (int)(weekly[i] * 1f / maxVal * dpToPx(56)));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dpToPx(16), barH);
            barLp.bottomMargin = dpToPx(4);
            bar.setLayoutParams(barLp);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setColor(i == 6 ? Color.parseColor("#00BFA5") : Color.parseColor("#B2DFDB"));
            barBg.setCornerRadius(dpToPx(4));
            bar.setBackground(barBg);
            col.addView(bar);

            TextView lbl = new TextView(requireContext());
            lbl.setText(dayLabels[i]);
            lbl.setTextSize(9f);
            lbl.setTextColor(Color.parseColor("#AAAAAA"));
            lbl.setGravity(Gravity.CENTER);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            col.addView(lbl);

            binding.llWeeklyBars.addView(col);
        }
    }

    private String[] buildDayLabels() {
        String[] all = {"א'", "ב'", "ג'", "ד'", "ה'", "ו'", "ש'"};
        String[] result = new String[7];
        int todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        for (int i = 0; i < 7; i++) result[i] = all[(todayDow - (6 - i) + 7) % 7];
        return result;
    }

    private void setupMistakesRow(List<Word> words) {
        if (binding == null || words == null) return;
        List<Word> mistakes = new ArrayList<>();
        for (Word w : words) {
            if (w.isErrorInQuiz()) mistakes.add(w);
            if (mistakes.size() == 5) break;
        }
        if (mistakes.isEmpty()) {
            binding.rvMistakes.setVisibility(View.GONE);
            binding.tvNoMistakes.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoMistakes.setVisibility(View.GONE);
        binding.rvMistakes.setVisibility(View.VISIBLE);
        binding.rvMistakes.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvMistakes.setAdapter(new MistakeAdapter(mistakes));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────────────────────────────────

    private void navigateToQuiz(String type) {
        if (binding == null) return;
        Bundle b = new Bundle();
        b.putString("practiceType", type);
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_home_to_quiz, b);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void setupGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && user.getDisplayName() != null
                && !user.getDisplayName().isEmpty())
            ? user.getDisplayName().split(" ")[0] : "משתמש";
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String period = hour >= 6 && hour < 12 ? "בוקר טוב"
            : hour < 18 ? "צהריים טובים" : "ערב טוב";
        binding.tvGreeting.setText(period + ", " + name + "!");
    }

    private void refreshStats() {
        if (binding == null) return;
        binding.tvDashLevel.setText(String.valueOf(gamification.getCurrentLevel()));
        binding.tvDashXp.setText(String.valueOf(gamification.getTotalXp()));
        binding.tvDashStreak.setText(String.valueOf(gamification.getStreak()));
        setupWeeklyActivity();
        setupDailyChallenges(masterWords);
    }

    private String getDailyQuote() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        SharedPreferences prefs = requireContext()
            .getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE);
        if (!today.equals(prefs.getString("daily_quote_date", ""))) {
            int idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % QUOTES.length;
            prefs.edit()
                .putInt("daily_quote_index", idx)
                .putString("daily_quote_date", today)
                .apply();
            return QUOTES[idx];
        }
        return QUOTES[prefs.getInt("daily_quote_index", 0)];
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inline MistakeAdapter
    // ──────────────────────────────────────────────────────────────────────────

    private static class MistakeAdapter extends RecyclerView.Adapter<MistakeAdapter.VH> {
        private final List<Word> items;
        MistakeAdapter(List<Word> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mistake_word, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Word w = items.get(pos);
            h.tvEn.setText(w.getEnglishWord());
            h.tvHe.setText(w.getHebrewTranslation() != null ? w.getHebrewTranslation() : "");
            h.itemView.setOnClickListener(v ->
                Toast.makeText(v.getContext(),
                    w.getEnglishWord() + " = " + w.getHebrewTranslation(),
                    Toast.LENGTH_SHORT).show());
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvEn, tvHe;
            VH(View v) {
                super(v);
                tvEn = v.findViewById(R.id.tvMistakeEn);
                tvHe = v.findViewById(R.id.tvMistakeHe);
            }
        }
    }
}
