package com.example.easylex.data;

/**
 * SchoolClass — מודל קריאה-בלבד לכיתה (משימה 0.9).
 * לא נשמר ב-Room במכוון — נתונים מוסדיים הם online-only (ר' דוח ההיתכנות,
 * חלק ב' סעיף 4): שיתופיים מטבעם, לא נדרש cache אופליין כמו מילות תרגול.
 */

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

public class SchoolClass {

    private final String id;
    private final String name;
    private final String grade;
    private final String joinCode;
    private final String teacherId;
    private final long studentCount;
    private final boolean archived;

    private SchoolClass(String id, String name, String grade, String joinCode,
                         String teacherId, long studentCount, boolean archived) {
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.joinCode = joinCode;
        this.teacherId = teacherId;
        this.studentCount = studentCount;
        this.archived = archived;
    }

    public static SchoolClass fromDocument(DocumentSnapshot doc) {
        Long count = doc.getLong("studentCount");
        Boolean archived = doc.getBoolean("archived");
        return new SchoolClass(
                doc.getId(),
                doc.getString("name"),
                doc.getString("grade"),
                doc.getString("joinCode"),
                doc.getString("teacherId"),
                count != null ? count : 0,
                archived != null && archived
        );
    }

    public String getId() { return id; }
    @Nullable public String getName() { return name; }
    @Nullable public String getGrade() { return grade; }
    @Nullable public String getJoinCode() { return joinCode; }
    @Nullable public String getTeacherId() { return teacherId; }
    public long getStudentCount() { return studentCount; }
    public boolean isArchived() { return archived; }
}
