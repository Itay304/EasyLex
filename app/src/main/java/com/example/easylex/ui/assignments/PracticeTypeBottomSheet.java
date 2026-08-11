package com.example.easylex.ui.assignments;

/**
 * =====================================================================
 * PracticeTypeBottomSheet — בחירת מודול תרגול למשימה (שלב 2, המשך משימה 0.15)
 * =====================================================================
 *
 * מוצג בלחיצה על "התחל תרגול" ב-StudentAssignmentsFragment/InstitutionalHomeFragment,
 * במקום ניווט ישיר ל-Quiz. מציע 3 אפשרויות (כרטיסיות/מבחן/איות) ומחזיר את
 * הבחירה דרך listener — לא יודע כלום על Bundle/Navigation; זה נשאר באחריות
 * ה-Fragment הקורא, כדי לא לשכפל את בניית ה-Bundle בשתי מחלקות.
 */

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.easylex.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PracticeTypeBottomSheet extends BottomSheetDialogFragment {

    public interface OnPracticeTypeSelected {
        /** type: "FLASHCARDS" | "QUIZ" | "SPELLING" */
        void onPracticeTypeSelected(String type);
    }

    @Nullable
    private OnPracticeTypeSelected listener;

    public static PracticeTypeBottomSheet create(OnPracticeTypeSelected listener) {
        PracticeTypeBottomSheet sheet = new PracticeTypeBottomSheet();
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_practice_type_picker, container, false);

        root.findViewById(R.id.optionFlashcards).setOnClickListener(v -> select("FLASHCARDS"));
        root.findViewById(R.id.optionQuiz).setOnClickListener(v -> select("QUIZ"));
        root.findViewById(R.id.optionSpelling).setOnClickListener(v -> select("SPELLING"));

        return root;
    }

    private void select(String type) {
        if (listener != null) listener.onPracticeTypeSelected(type);
        dismiss();
    }
}
