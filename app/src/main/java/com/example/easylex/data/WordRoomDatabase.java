package com.example.easylex.data;

/**
 * =====================================================================
 * WordRoomDatabase — מחלקת מסד הנתונים המקומי
 * =====================================================================
 *
 * מה זה Room?
 * -----------
 * Room היא ספרייה של Google שעוטפת את SQLite (מסד נתונים מובנה
 * באנדרואיד) ומקלה על השימוש בו.
 * במקום לכתוב SQL ידנית וקוד Java מסורבל, Room מאפשר לנו
 * להגדיר Entity, DAO, ו-Database בצורה פשוטה.
 *
 * מבנה שלוש השכבות של Room:
 * --------------------------
 *   Entity (Word.java)   → טבלה במסד הנתונים
 *   DAO (WordDao.java)   → שאילתות ופעולות על הטבלה
 *   Database (זה הקובץ) → מנהל החיבור עצמו
 *
 * Pattern: Singleton
 * ------------------
 * יש רק מופע אחד של מסד הנתונים בכל הרצת האפליקציה.
 * volatile + synchronized מבטיחים שגם אם שני Threads יגיעו
 * בו-זמנית, ייוצר רק מופע אחד (Thread-safe Singleton).
 *
 * גרסת DB: 4
 * -----------
 * כל פעם שמוסיפים עמודה חדשה ל-Word (Entity), גרסת ה-DB עולה.
 * fallbackToDestructiveMigration() = אם גרסת ה-DB ישנה ממה שמותקן וללא
 * Migration מפורש לנתיב הזה — מחק הכל ובנה מחדש (פשוט, אבל מוחק נתונים ישנים).
 * גרסה 3→4 (הוספת sourceListId) מוגדרת במפורש ב-MIGRATION_3_4 כדי לא
 * למחוק נתוני תרגול קיימים של משתמשים.
 * =====================================================================
 */

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// הגדרת מסד הנתונים: טבלה אחת (Word), גרסה 4, ללא ייצוא סכמה
// --- 🔥 עדכון גרסה ל-4: הוספת sourceListId (תמיכה בכמה רשימות מילים) ---
@Database(entities = {Word.class}, version = 4, exportSchema = false)
public abstract class WordRoomDatabase extends RoomDatabase {

    /**
     * MIGRATION_3_4 — מוסיפה את עמודת sourceListId מבלי למחוק נתונים.
     * מילים גלובליות קיימות (isVerified=1) — כולן הגיעו עד כה מ-band_2_full_list
     * בלבד — מקבלות sourceListId="band_2_full_list" כברירת מחדל, כך שהסנכרון
     * הדינמי החדש (ר' WordRepository) ימשיך לזהות אותן נכון ולא ינסה
     * להוסיף אותן שוב או למחוק אותן.
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE words_table ADD COLUMN sourceListId TEXT");
            database.execSQL(
                "UPDATE words_table SET sourceListId = 'band_2_full_list' WHERE isVerified = 1");
        }
    };

    /**
     * מחזיר את ה-DAO — שכבת הגישה לטבלה.
     * Room מייצר את המימוש אוטומטית בזמן קומפילציה.
     */
    public abstract WordDao wordDao();

    /**
     * המופע היחיד (Singleton) של מסד הנתונים.
     * volatile = שינויים בשדה זה גלויים מיד לכל ה-Threads.
     */
    private static volatile WordRoomDatabase INSTANCE;

    /**
     * getDatabase — מחזיר את המופע היחיד של מסד הנתונים.
     * אם עדיין לא נוצר — יוצר אותו (Double-Checked Locking).
     *
     * Double-Checked Locking:
     *   בדיקה ראשונה (ללא נעילה) — אם קיים, מחזיר מיד (מהיר).
     *   נעילה (synchronized) — רק אם צריך ליצור.
     *   בדיקה שנייה (עם נעילה) — מונע שניים ייצרו בו-זמנית.
     *
     * @param context Context של האפליקציה (לא Activity — כדי למנוע memory leak)
     */
    public static WordRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WordRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),  // ApplicationContext — בטוח
                                    WordRoomDatabase.class,
                                    "word_database")                  // שם קובץ ה-DB בדיסק
                            .addMigrations(MIGRATION_3_4)             // 3→4: הוספת sourceListId, ללא מחיקת נתונים
                            .fallbackToDestructiveMigration()         // לכל נתיב אחר שלא הוגדר לו Migration — מחק ובנה מחדש
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
