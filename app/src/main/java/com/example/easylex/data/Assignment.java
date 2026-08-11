package com.example.easylex.data;

/**
 * Assignment — מודל קריאה-בלבד למשימת תרגול (משימה 0.14/0.15, שלב 2).
 * לא נשמר ב-Room, כמו SchoolClass — נתון מוסדי, online-only.
 * נוצר ע"י Cloud Function createAssignment בלבד; המחלקה הזו רק קוראת אותו —
 * בצד מורה ישירות מ-Firestore (fromDocument), בצד תלמיד דרך התוצאה של
 * getMyAssignments (fromMap — ר' functions/index.js, משימה 0.15).
 */

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Assignment {

    private final String id;
    private final String title;
    private final String classId;
    private final String listId;
    private final List<String> wordIds;
    @Nullable
    private final Long dueDateMs;
    private final String status;

    private Assignment(String id, String title, String classId, String listId, List<String> wordIds,
                        @Nullable Long dueDateMs, String status) {
        this.id = id;
        this.title = title;
        this.classId = classId;
        this.listId = listId;
        this.wordIds = wordIds;
        this.dueDateMs = dueDateMs;
        this.status = status;
    }

    /** בצד מורה — קריאה ישירה מ-Firestore (ClassDetailViewModel). */
    public static Assignment fromDocument(DocumentSnapshot doc) {
        return new Assignment(
                doc.getId(),
                doc.getString("title"),
                doc.getString("classId"),
                doc.getString("listId"),
                readStringList(doc.get("wordIds")),
                doc.getLong("dueDateMs"),
                doc.getString("status")
        );
    }

    /** בצד תלמיד — מתוך תוצאת getMyAssignments (Cloud Function, לא Firestore ישיר). */
    public static Assignment fromMap(Map<String, Object> map) {
        Object idObj = map.get("assignmentId");
        Object dueObj = map.get("dueDateMs");
        return new Assignment(
                idObj != null ? idObj.toString() : "",
                (String) map.get("title"),
                (String) map.get("classId"),
                (String) map.get("listId"),
                readStringList(map.get("wordIds")),
                dueObj instanceof Number ? ((Number) dueObj).longValue() : null,
                (String) map.get("status")
        );
    }

    private static List<String> readStringList(Object raw) {
        if (!(raw instanceof List)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object o : (List<?>) raw) {
            if (o instanceof String) result.add((String) o);
        }
        return result;
    }

    public String getId() { return id; }
    @Nullable public String getTitle() { return title; }
    @Nullable public String getClassId() { return classId; }
    @Nullable public String getListId() { return listId; }
    public List<String> getWordIds() { return wordIds; }
    @Nullable public Long getDueDateMs() { return dueDateMs; }
    @Nullable public String getStatus() { return status; }
}
