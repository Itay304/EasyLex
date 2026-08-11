# בדיקות Security Rules — הפעלה (משימה 0.4)

בדיקות אלה רצות **רק** מול Firebase Emulator מקומי. הן לא נוגעות ב-Firestore
האמיתי, לא דורשות חשבון Blaze, ולא דורשות אפילו התחברות (`firebase login`) —
כי אנחנו משתמשות ב-project id מזויף (`demo-easylex`), מוסכמה מוכרת של Firebase
שמפעילה את ה-Emulator באופן מקומי לחלוטין בלי גישה לפרויקט אמיתי.

בדקתי את הסביבה שלך: **Java כבר מותקן** (OpenJDK 21 — נדרש להרצת ה-Firestore
Emulator), **Node/npm כבר מותקנים**. **Firebase CLI לא מותקן עדיין** — שלב א'
למטה.

## א. התקנת Firebase CLI (חד-פעמי)

מכל תיקייה:

```bash
npm install -g firebase-tools
firebase --version   # ודא שמוצג מספר גרסה
```

## ב. הפעלת ה-Emulator

**לא צריך `firebase init emulators`** — כבר יצרתי עבורך את
[firebase.json](../firebase.json) בשורש הפרויקט עם קונפיגורציית ה-Emulator
המוכנה (Firestore בפורט 8080, UI בפורט **4001** — לא 4000 ברירת המחדל, כדי
לא להתנגש עם `admin-tool/server.js` שכבר תופס את פורט 4000).

מתוך **שורש הפרויקט** (`EasyLex/`, לא `rules-tests/`), בטרמינל נפרד שנשאר
פתוח לאורך כל הבדיקות:

```bash
firebase emulators:start --only firestore --project demo-easylex
```

בפעם הראשונה ה-CLI עשוי להוריד את בינארי ה-Emulator (חד-פעמי, כמה עשרות MB).
כשתראה בפלט שורה כמו `✔  firestore: Emulator started` — ההמתלר מוכן.
אופציונלי: אפשר לפתוח את ה-UI הגרפי בדפדפן בכתובת `http://127.0.0.1:4001`
כדי לראות בזמן אמת אילו מסמכים הבדיקות יוצרות/קוראות.

**השאירו את הטרמינל הזה פתוח** — כל הרצת בדיקות דורשת שה-Emulator ירוץ ברקע.

## ג. הרצת הבדיקות

בטרמינל **שני** (הראשון תפוס ע"י ה-Emulator):

```bash
cd rules-tests
npm install   # פעם ראשונה בלבד — מתקין @firebase/rules-unit-testing + jest
npm test
```

## ד. איך לדעת שהכול עבר

פלט Jest מסודר לפי `describe`/`test`. הצלחה נראית כך (כל השורות עם ✓ ירוק):

```
 PASS  ./firestore.rules.test.js
  users/{uid}
    ✓ תלמיד קורא את המסמך שלו בלבד
    ✓ תלמיד לא יכול לקרוא מסמך של תלמיד אחר
  word_lists (הבאנדים הציבוריים)
    ✓ תלמיד לא יכול לכתוב ל-word_lists
    ✓ תלמיד יכול לקרוא מ-word_lists (רשימה ציבורית, לכל משתמש מחובר)
  institutions/classes — בידוד בין מוסדות
    ✓ מוסד A לא יכול לקרוא כיתה של מוסד B
    ✓ מורה יכול לקרוא כיתה של המוסד שלו
  institutions/{instId}/assignments
    ✓ מורה יכול לכתוב assignment לכיתה שלו
    ✓ תלמיד שבכיתה של המשימה יכול לקרוא אותה
    ✓ תלמיד שלא בכיתה של המשימה לא יכול לקרוא אותה
  users/{uid}/progress
    ✓ תלמיד לא יכול לקרוא progress של תלמיד אחר
  משתמש לא מחובר
    ✓ לא יכול לגשת לשום דבר

Test Suites: 1 passed, 1 total
Tests:       10 passed, 10 total
```

אם `Test Suites: 1 failed` — Jest מדפיס בדיוק איזו בדיקה נכשלה ועם איזו
ציפייה (`assertSucceeds`/`assertFails`) מול מה שקרה בפועל. **firestore.rules
לא אמור להיפרס ל-production כל עוד יש כאן כישלון.**

## הרצה חוזרת

`beforeEach` בקובץ הבדיקות מנקה את כל ה-Emulator (`clearFirestore()`) לפני כל
בדיקה בנפרד — אין תלות בין בדיקות, ואין צורך לאתחל את ה-Emulator בין הרצות.
אפשר להשאיר את הטרמינל של שלב ב' פתוח ולהריץ `npm test` שוב ושוב.
