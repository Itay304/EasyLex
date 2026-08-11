שם בית הספר: ישיבת הרב עמיאל — הישוב החדש

קוד מוסד: 540476

שם הפרויקט: EasyLex — אפליקציית לימוד מילות אנגלית

שם התלמיד: איתי

שם המנחה: זאב פריימן

שם החלופה: תכנון ותכנות מערכות טלפונים ניידים תחת מערכת ההפעלה Android

שנת לימודים: תשפ"ה (2025–2026)

תאריך הגשה: 2026

---

> *"The limits of my language mean the limits of my world."*
> — Ludwig Wittgenstein

---

# ~ תוכן עניינים ~

~ מבוא ~...............................................................................................................................5
★ רקע לפרויקט :....................................................................................................................5
★ תהליך המחקר :...................................................................................................................6
★ אתגרים מרכזיים :................................................................................................................7
★ חידושים, התאמות ועדכונים :................................................................................................8

~ תיאור תחום הידע — פרק מילולי ~........................................................................................10
★ אובייקטים נחוצים לאפליקציה :............................................................................................10
★ סוגי נתונים :......................................................................................................................11
★ ייצוג מידע :........................................................................................................................14
★ תיאור פעולות על המידע :.....................................................................................................17

~ מבנה / ארכיטקטורה של הפרויקט ~.......................................................................................20
★ שלב תכנון ותיעוד מסכי הפרויקט :........................................................................................20
★ תרשים זרימה — UML :.......................................................................................................32

~ מימוש הפרויקט ~..................................................................................................................33
★ תיאור מחלקות :..................................................................................................................33
★ בסיס נתונים :......................................................................................................................62

~ מדריך למשתמש ~..................................................................................................................65

~ רפלקציה ~.............................................................................................................................79

~ ביבליוגרפיה ~........................................................................................................................81

~ נספחים ~...............................................................................................................................82

---

# ~ מבוא ~

## ★ רקע לפרויקט :

○ שם הפרויקט: EasyLex

○ תיאור קצר של הפרויקט:

■ EasyLex היא אפליקציית אנדרואיד ללימוד מילות אנגלית, שנבנתה כדי לפתור בעיה מאוד ספציפית שהכרתי מהחיים עצמם: הייתי צריך ללמוד מילות אנגלית לבגרות, אבל לא רציתי לשבת ולחפש כל מילה ידנית במילון. חשבתי לעצמי — למה שהאפליקציה לא תסרוק את ספר הלימוד ישירות ותבנה לי את רשימת המילים אוטומטית?

■ הרעיון המרכזי פשוט: המשתמש מצלם עמוד מספר לימוד עם המצלמה של הטלפון. האפליקציה מזהה את המילים האנגליות בתמונה בעזרת טכנולוגיית OCR (זיהוי תווים אופטי), מתרגמת כל מילה לעברית אוטומטית, ומוסיפה אותה ישירות לרשימת הלמידה האישית של המשתמש. משם — לתרגול.

■ מעבר לסריקה, EasyLex מציעה ארבעה מצבי תרגול: שאלון בחירה מרובה, כרטיסיות הפיכות, אלוף האיות, ותרגול לפי קטגוריות. כל מצב מותאם לסגנון לימוד שונה. בנוסף, יש מערכת גמיפיקציה מלאה — נקודות XP, רמות, ורצף יומי — כדי לשמור על המוטיבציה לאורך זמן.

○ קהל היעד:

■ האפליקציה מיועדת בעיקר לתלמידי תיכון הלומדים אנגלית לבגרות. עם זאת, כל מי שרוצה להרחיב את אוצר המילים האנגלי שלו — בין אם לצרכי עבודה, נסיעות, או סקרנות — יכול ליהנות ממנה.

○ הסיבות לבחירת הנושא:

■ הנושא נבחר מתוך צורך אמיתי. כשהייתי צריך ללמוד 1,902 מילות Band 2 לאנגלית, לא היה כלי שיתאים לי. Duolingo לא עוסקת במילות הבגרות הישראליות. Anki מסורבלת. Google Translate לא שומרת היסטוריה. ראיתי פער ברור בשוק — ומלאתי אותו.

---

## ★ תהליך המחקר :

○ לפני שהתחלתי לקודד, בדקתי מה קיים:

■ **Duolingo** — האפליקציה הגדולה ביותר בתחום. יתרונותיה: ממשק נהדר, גמיפיקציה מוצלחת. חסרונותיה: המסלולים קבועים מראש, לא ניתן לייבא תוכן לימודי ספציפי, ואין תמיכה במילות הבגרות הישראליות.

■ **Anki** — תוכנת כרטיסיות עם אלגוריתם חזרה מרווחת (Spaced Repetition). יתרון: מאוד יעילה מבחינה פדגוגית. חסרון: ממשק מיושן, לא אינטואיטיבי, ואין OCR.

■ **Google Translate** — תרגום מצוין. אך אינה אפליקציית לימוד, אין שמירת רשימות, ואין מעקב התקדמות.

■ **Quizlet** — פלטפורמת כרטיסיות. יתרון: ניתן ליצור רשימות מותאמות אישית. חסרון: אין OCR, אין גמיפיקציה מובנית, ואין תמיכה ביעד בגרות ישראלי.

○ מסקנת המחקר:

■ אין אפליקציה אחת שמשלבת OCR + ייבוא מסריקה + תרגול מותאם אישית + גמיפיקציה + תמיכה בעברית + מאגר מילות בגרות ישראלי. EasyLex בנתה בדיוק את הפתרון הזה.

○ מחקר טכנולוגי:

■ לאחר הגדרת הצרכים, חקרתי אילו טכנולוגיות מתאימות. למדתי על CameraX — ספריית המצלמה המודרנית של Google לאנדרואיד. חקרתי את ML Kit של Google, שמספקת זיהוי טקסט ותרגום על המכשיר עצמו, ללא צורך בחיבור לאינטרנט לאחר ההורדה הראשונה. למדתי על Room — מסד הנתונים המקומי של אנדרואיד — ועל Firebase Firestore לסנכרון ענן.

■ בחרתי ב-**Java** ולא ב-Kotlin כי Java נלמדת בבית הספר, הקוד ברור ומובן, ויש לה תיעוד עשיר. בחרתי ב-**MVVM** כי הוא מפריד אחריות בצורה נקייה — Fragment לא יודע מאיפה מגיעים הנתונים, ViewModel לא יודע מה מוצג. בחרתי ב-**Firebase** כי הוא מאפשר בניית Backend מלא בימים — Auth, DB, ועדכונים בזמן אמת.

---

## ★ אתגרים מרכזיים :

○ האתגרים שנתקלתי בהם במהלך הפיתוח:

■ **ניהול Threads:** Java אינה Kotlin — אין coroutines. כל פעולת כתיבה לבסיס הנתונים חייבת לרוץ על Background Thread, ועדכון ממשק המשתמש חייב לרוץ על Main Thread. נדרשתי להבין לעומק את ExecutorService ואת postValue() של LiveData כדי שהכל יעבוד בצורה חלקה.

■ **סנכרון דו-כיווני עם Firestore:** הקושי לא היה להוסיף מילים חדשות מ-Firestore — זה פשוט. הקושי היה למחוק מילים שהמנהל הסיר, מבלי לאבד את נתוני התרגול האישיים של המשתמש (correctAttempts, errorFlags). הפתרון: השוואת Sets ומחיקה סלקטיבית רק של מילים שנעלמו מה-Firestore.

■ **מחיקה בזמן אמת:** כשמנהל מוחק מילה ב-Firestore, כל המכשירים המחוברים צריכים לזהות זאת מיידית — ללא חכייה ל-24 שעות. הפתרון: addSnapshotListener עם MetadataChanges.EXCLUDE, שמקשיב לשינויי שרת בלבד ומגיב בפחות משנייה.

■ **כיוון RTL ו-LTR בכרטיס אחד:** כרטיס המנהל הציג גם עברית וגם אנגלית. בסביבת RTL, Android "הפך" את הכיוון והאנגלית הופיעה מימין במקום משמאל. הפתרון: הוספת layoutDirection="ltr" על ה-LinearLayout של הכרטיס, שמבטיחה שאנגלית תמיד שמאל ועברית תמיד ימין.

■ **דיוק ה-OCR:** ML Kit מחזיר תיבות בוסם (Bounding Boxes) עבור כל מילה, שורה, ובלוק. צריך לאחד, לסנן, ולהתאים לפי מצב הסריקה — מצב ספר (Word-level) לעומת מצב טקסט חופשי (Line-level). כמו כן, לאחר הצילום יש לתקן את כיוון התמונה לפי נתוני EXIF, כדי ש-OCR יקבל תמונה ישרה.

■ **אנימציות ו-ViewModel:** ה-ViewModel שורד סיבובי מסך, אך אנימציות ה-XP ("+10 XP" צפה) מתבטלות בסיבוב. נדרש ניהול state קפדני כדי לא להציג אנימציה ישנה.

○ הפתרון הכולל:

■ האפליקציה פותרת בדיוק את הבעיה שתוארה: ממשק פשוט, זרימה אוטומטית מסריקה ועד לתרגול, ומערכת שמשמרת את המוטיבציה דרך גמיפיקציה.

---

## ★ חידושים, התאמות ועדכונים של אלמנטים טכנולוגיים, עיצוביים ואחרים בפרויקט :

○ חידושים מרכזיים ב-EasyLex:

■ **זרימת OCR לימוד:** הרעיון של "לסרוק עמוד ספר ולקבל רשימת מילים" הוא הייחוד המרכזי. הזרימה: צילום — OCR — תרגום — בחירת מילים — שמירה, כולה ללא הקלדה ידנית.

■ **מחיקה בזמן אמת דרך Firestore Snapshot Listener:** כשמנהל מוחק מילה מהמאגר הגלובלי, כל המכשירים המחוברים מקבלים את השינוי בפחות משנייה — ללא polling ידני.

■ **אלגוריתם 60/20/20 אדפטיבי:** בחירת שאלות בשאלון אינה אקראית. 60% מהשאלות מגיעות ממילים קשות (Mastery 0-2), 20% ממילים חדשות (0 ניסיונות), ו-20% ממילים חזקות (Mastery 3-5). כך המשתמש תמיד מתאמן על מה שצריך.

■ **נוסחת Mastery שמונעת דירוג מנופח:** M = min(5, floor(correctAttempts / (totalAttempts + 1) × 5)). ה-(totalAttempts+1) מונע חלוקה באפס, ומבטיח שמילה עם ניסיון אחד נכון לא תקבל Mastery=5 מיד.

■ **גמיפיקציה Dual-Write:** XP, רמה, ורצף נשמרים גם ב-SharedPreferences (גישה מהירה) וגם ב-Firestore (גיבוי ענן). כך גם אם המשתמש מחליף מכשיר — ההתקדמות שמורה.

■ **מעבר מ-ViewPager2 ל-RecyclerView בממשק המנהל:** ViewPager2 הוביל לניהול מסורבל של אינדקסים. המעבר ל-RecyclerView עם LinearLayoutManager שיפר ביצועים, פישט את קוד ה-Adapter, ואפשר ItemTouchHelper לגרירה ומחיקה נוחה.

---

# ~ תיאור תחום הידע — פרק מילולי ~

## ★ אובייקטים נחוצים לאפליקציה :

○ האובייקטים המרכזיים ב-EasyLex:

■ האובייקט הראשי והמרכזי ביותר הוא **Word (מילה)**. הוא מייצג מילה אנגלית אחת עם כל המידע הנלווה לה — התרגום, חלק הדיבר, משפטי דוגמה, וכל היסטוריית הלמידה שלה. מחלקה זו היא הבסיס של כל המערכת: היא נשמרת ב-Room (מסד הנתונים המקומי), מורדת מ-Firestore (הענן), ומועברת בין כל מסכי האפליקציה.

■ האובייקט השני הוא **WordList (רשימת מילים)**. הוא מודל פשוט המשמש לייצוג קולקציה של מילים כאשר מורידים אותן מ-Firestore. הוא אינו נשמר ב-Room.

■ האובייקט השלישי הוא **נתוני גמיפיקציה** — XP כולל, רמה, רצף יומי, ותאריך פעילות אחרון. נתונים אלו אינם מאוחסנים כאובייקט Java עצמאי, אלא נשמרים ב-SharedPreferences (גישה מהירה) ומסונכרנים ל-Firestore (גיבוי).

---

## ★ סוגי נתונים :

○ מחלקת Word — סוגי הנתונים של כל שדה:

■ שדה **id** — סוג: int (מספר שלם). מזהה ייחודי שנוצר אוטומטית על ידי Room (AUTOINCREMENT). לא נדרש לאתחל אותו ידנית — Room מקצה אותו.

■ שדה **englishWord** — סוג: String (מחרוזת). המילה האנגלית, לדוגמה: "abandon". זהו המזהה הלוגי של המילה — שאר המערכת מחפשת לפיו.

■ שדה **hebrewTranslation** — סוג: String (מחרוזת). התרגום לעברית, לדוגמה: "לנטוש". נוצר אוטומטית על ידי ML Kit Translate בזרימת הסריקה.

■ שדה **partOfSpeech** — סוג: String (מחרוזת). חלק הדיבר — n (שם עצם), v (פועל), adj (תואר שם), adv (תואר פועל), prep (מילת יחס), pron (כינוי גוף).

■ שדה **exampleSentence** — סוג: String (מחרוזת). משפט דוגמה באנגלית המראה כיצד להשתמש במילה בהקשר.

■ שדה **hebrewExample** — סוג: String (מחרוזת). תרגום משפט הדוגמה לעברית.

■ שדה **tags** — סוג: String (מחרוזת). קטגוריה של המילה, לדוגמה: "Learning & Education" או "Abstract Actions". משמש לסינון בתרגול לפי נושא.

■ שדה **creationTimestamp** — סוג: long (מספר שלם ארוך). זמן הוספת המילה, בפורמט Unix Timestamp (מילישניות מאז 1970). הסיבה ל-long ולא int: ערכי timestamp גדולים מדי לאחסון ב-int רגיל.

■ שדה **correctAttempts** — סוג: int (מספר שלם). מונה את מספר התשובות הנכונות של המשתמש על מילה זו, בכל מצבי התרגול.

■ שדה **totalAttempts** — סוג: int (מספר שלם). מונה את סך כל הניסיונות (נכונות + שגויות). משמש לחישוב Mastery.

■ שדה **spellingCorrect** — סוג: int (מספר שלם). מונה תשובות נכונות ספציפית במצב "אלוף האיות".

■ שדה **isFavorite** — סוג: boolean (לוגי). true = מילה אישית של המשתמש (הוסיפה דרך סריקה/ידנית). false = מילה גלובלית שלא הוסמנה. Room שומר boolean כ-INTEGER (0 או 1).

■ שדה **isVerified** — סוג: boolean (לוגי). true = מילה שהגיעה מהמאגר הגלובלי (מהמנהל דרך Firestore). false = מילה שהמשתמש הוסיף בעצמו.

■ שדה **errorInQuiz** — סוג: boolean (לוגי). true = המשתמש טעה על מילה זו בשאלון האחרון. משמש לסינון מצב "שאלון טעויות".

■ שדה **errorInSpelling** — סוג: boolean (לוגי). true = המשתמש טעה באיות מילה זו.

■ שדה **errorInFlashcards** — סוג: boolean (לוגי). true = המשתמש לא ידע מילה זו בכרטיסיות.

---

## ★ ייצוג מידע :

○ מידע ב-EasyLex מאוחסן בשלושה מקומות שונים:

■ **Room (SQLite מקומי):** זהו מסד הנתונים המקומי שיושב על המכשיר. הוא מכיל טבלה אחת בשם words_table, גרסה 3. כל מילה = שורה אחת בטבלה. הנתונים זמינים גם ללא חיבור לאינטרנט.

■ **Firestore (ענן Google):** מאגר המילים הגלובלי — 1,902 מילות Band 2 לבגרות. גם נתוני הגמיפיקציה (XP, רמה, רצף) שמורים כאן לגיבוי ולסנכרון בין מכשירים.

■ **SharedPreferences (מקומי, key-value):** XP, רמה, רצף יומי — לגישה מהירה ללא שאילתת DB. גם הגדרות המשתמש (יעד יומי, TTS) ו-timestamp הסנכרון האחרון.

○ טבלת words_table — שם השדה ותיאורו:

| שם השדה | סוג נתון | תיאור |
|---------|---------|--------|
| id | INTEGER PRIMARY KEY AUTOINCREMENT | מזהה ייחודי — נוצר אוטומטית |
| englishWord | TEXT | המילה באנגלית |
| hebrewTranslation | TEXT | תרגום לעברית |
| partOfSpeech | TEXT | חלק הדיבר (n/v/adj/...) |
| exampleSentence | TEXT | משפט דוגמה באנגלית |
| hebrewExample | TEXT | משפט דוגמה בעברית |
| tags | TEXT | קטגוריה לסינון |
| creationTimestamp | INTEGER | זמן הוספה (Unix milliseconds) |
| correctAttempts | INTEGER DEFAULT 0 | תשובות נכונות כולל |
| totalAttempts | INTEGER DEFAULT 0 | ניסיונות כולל |
| spellingCorrect | INTEGER DEFAULT 0 | נכונות ב-Spelling |
| isFavorite | INTEGER DEFAULT 0 | 1=אישי, 0=גלובלי |
| isVerified | INTEGER DEFAULT 0 | 1=מנהל, 0=משתמש |
| errorInQuiz | INTEGER DEFAULT 0 | דגל שגיאה בשאלון |
| errorInSpelling | INTEGER DEFAULT 0 | דגל שגיאה באיות |
| errorInFlashcards | INTEGER DEFAULT 0 | דגל שגיאה בכרטיסיות |

○ מבנה Firestore:

■ word_lists/band_2_full_list/words/{docId} — כל מסמך מייצג מילה אחת עם השדות: englishWord, hebrewTranslation, partOfSpeech, exampleSentence, hebrewExample, tags.

■ users/{uid} — נתוני המשתמש: totalXp, level, streak, lastActiveDate, displayName.

■ config/app_settings — גרסה מינימלית לאכיפת עדכון כפוי (min_version).

○ כיצד המידע זורם:

■ כל שינוי ב-Room מתעדכן אוטומטית בממשק דרך LiveData. כל Fragment שצופה ב-LiveData<List<Word>> מקבל עדכון מיידי כשמילה נוספת, עודכנה, או נמחקה — בלי שה-Fragment צריך לשאול מחדש.

---

## ★ תיאור פעולות על המידע :

○ יצירה והוספה לבסיס הנתונים:

■ **הוספה ידנית:** המשתמש מקליד מילה חדשה בתיבת הדיאלוג של MyWordsFragment. MyWordsViewModel.insert(word) נקרא — WordRepository.insert(word) על ExecutorService Thread — WordDao.insert(word) — INSERT OR IGNORE ל-SQLite.

■ **סריקת OCR:** ScanFragment מצלם תמונה דרך CameraX — ML Kit OCR מזהה תיבות טקסט — ML Kit Translate מתרגם כל מילה EN לעברית — המשתמש בוחר מילים — לחיצת "שמור הכל" — insert עם isFavorite=true לכל מילה נבחרת.

■ **סנכרון ממנהל:** WordRepository.syncGlobalWordsFromFirestore() מוריד את כל 1,902 המילים מ-Firestore. לכל מילה חדשה שאינה ב-Room — insert עם isVerified=true. מילים קיימות מדולגות כדי לשמור נתוני תרגול.

○ קריאה מבסיס הנתונים:

■ WordDao.getAllWords() — מחזיר LiveData<List<Word>> בסדר יצירה הפוך. Room מאזין לשינויים ומעדכן אוטומטית.

■ WordDao.getPersonalWords() — מחזיר רק מילים אישיות (isVerified=false) ל-LiveData.

■ WordDao.getVerifiedEnglishWords() — מחזיר List<String> של שמות מילים גלובליות. משמש בסנכרון לבדיקת כפילויות.

○ עדכון בבסיס הנתונים:

■ לאחר כל תשובה בשאלון, Spelling, או Flashcards: Word.correctAttempts++, Word.totalAttempts++, errorInQuiz/Spelling/Flashcards מתעדכן. WordDao.update(word) נקרא על ExecutorService Thread.

■ עדכון שם משתמש: SettingsFragment.updateDisplayName(name) — Firebase Auth + Firestore users/{uid}/displayName.

○ מחיקה מבסיס הנתונים:

■ **מחיקת מילה אישית:** המשתמש מוחק מהרשימה — WordDao.delete(word) לפי ID.

■ **מחיקת מנהל (24 שעות):** syncGlobalWordsFromFirestore() משווה Sets — deleteVerifiedWordByEnglish(eng) לכל מילה שנעלמה מ-Firestore.

■ **מחיקת מנהל (זמן אמת):** addSnapshotListener מזהה DocumentChange.Type.REMOVED — deleteVerifiedWordByEnglish() מיידית — Room מוחק — LiveData מעדכן — UI מגיב.

■ **ניקוי כללי:** SettingsFragment — "נקה רשימה אישית" — deletePersonalWords(). "אפס הכל" — deleteAllWords() + resetAllWordProgress().

---

# ~ מבנה / ארכיטקטורה של הפרויקט ~

## ★ שלב תכנון ותיעוד מסכי הפרויקט :

○ SplashActivity : שם המסך

■ זהו מסך הפתיחה של האפליקציה — הדבר הראשון שהמשתמש רואה כשפותח את EasyLex. מוצג לוגו האפליקציה עם אנימציית כניסה (Scale + Fade), ומתחת לו שם האפליקציה.

■ בזמן שהאנימציה מתנגנת, מתבצעות בו-זמנית שתי בדיקות ברקע: בדיקת גרסה (Firestore config/app_settings — min_version) ובדיקת מצב התחברות (FirebaseAuth.getCurrentUser()).

■ אם גרסת האפליקציה ישנה מדי — נפתח דיאלוג עדכון כפוי שאינו ניתן לסגירה. המשתמש חייב לעדכן לפני שיוכל להיכנס.

■ לאחר סיום האנימציה ושתי הבדיקות — אם המשתמש מחובר: מעבר אוטומטי ל-MainActivity. אם לא מחובר: מעבר ל-LoginActivity.

---

○ LoginActivity : שם המסך

■ מסך הכניסה לאפליקציה. מכיל שדה אימייל, שדה סיסמה, כפתור "כניסה", ואפשרות "כניסה עם Google".

■ בלחיצה על "כניסה" — ולידציה מקומית (שדות לא ריקים, פורמט אימייל תקין) — Firebase Auth signInWithEmailAndPassword(). הצלחה: מעבר ל-MainActivity. כישלון: הודעת שגיאה ב-Toast.

■ בלחיצה על "כניסה עם Google" — Google Sign-In Intent נפתח, המשתמש בוחר חשבון — Firebase Auth מקבל את ה-Token — מעבר ל-MainActivity.

■ קישור "עדיין אין לך חשבון?" מוביל ל-RegisterActivity.

---

○ RegisterActivity : שם המסך

■ מסך ההרשמה. מכיל שדות: שם מלא, כתובת אימייל, סיסמה, ואישור סיסמה.

■ לחיצה על "הירשם" — ולידציה מקומית (אימייל תקין, סיסמה באורך מינימלי 6 תווים, סיסמאות תואמות) — Firebase Auth createUserWithEmailAndPassword() — Firebase שומר את המשתמש — מעבר ל-MainActivity.

■ Firebase Auth מנהל את הסיסמה מוצפנת — האפליקציה אינה שומרת סיסמאות בשום מקום.

■ קישור "כבר יש לך חשבון?" מוביל חזרה ל-LoginActivity.

---

○ MainActivity : שם המסך

■ ה-Activity הראשי שמכיל את כל שאר המסכים. הוא אינו מסך בפני עצמו — הוא "קליפה" שמכילה NavHostFragment עם Navigation Component.

■ בתחתית המסך יש BottomNavigationView עם 5 כרטיסיות: בית, מילים, תרגול, סריקה, פרופיל.

■ Navigation Component מנהל את המעבר בין ה-Fragments אוטומטית לפי לחיצה על הכרטיסיות.

■ ה-ActionBar מוסתר (NoActionBar Theme) — כל Fragment מנהל את ה-Toolbar שלו בנפרד.

---

○ DashboardFragment : שם המסך — מסך הבית

■ מסך הפתיחה לאחר כניסה. מכיל כותרת פתיח עם שם המשתמש (לדוגמה: "שלום, איתי!"), תאריך, ומוטיבציה יומית.

■ בחלק העליון מוצג כרטיס XP: הרמה הנוכחית, פס התקדמות לרמה הבאה, וסמל הרצף היומי.

■ כפתור "מבחן יומי" — שאלון של 20 מילים אחת לכל יום. המילות נבחרות לפי Daily Seed (אותן 20 מילות לאורך כל היום).

■ "מילה של היום" — מילה אחת שנבחרה אקראית מהמאגר, עם התרגום ומשפט דוגמה. לחיצה על כפתור הרמקול — האפליקציה מקריאה את המילה.

■ כפתורי ניווט מהיר: "לרשימת המילים", "לתרגול", "לסריקה".

---

○ MyWordsFragment : שם המסך — המילים שלי

■ מסך זה מציג את כל המילים של המשתמש כרשימה. בחלק העליון יש שדה חיפוש (SearchView), כפתור סינון, וכפתור הוספת מילה ידנית (FAB).

■ הרשימה בנויה מ-RecyclerView עם WordListAdapter. כל פריט מציג: המילה באנגלית, חלק הדיבר, התרגום בעברית, ופס Mastery.

■ לחיצה על מילה — פתיחת דיאלוג מפורט עם: משפטי דוגמה, כפתור TTS (הקראה), כפתור עריכה, וכפתור מחיקה.

■ ניתן לסנן לפי: כל המילים, מילים אישיות, מילים גלובליות, ולפי קטגוריות. ניתן גם למיין.

■ FAB — דיאלוג הוספה ידנית: שדות EN, HE, חלק דיבר, תגיות — לחיצת שמור — viewModel.insert().

---

○ PracticeFragment : שם המסך — תרגול

■ מסך בחירת מצב התרגול. בחלק העליון — כרטיס XP (רמה + פס התקדמות + רצף יומי). מתחת — ארבעה כרטיסי תרגול.

■ **שאלון** — 20 שאלות בחירה מרובה עם אלגוריתם 60/20/20.

■ **כרטיסיות** — Swipe ימין (ידעתי) / Swipe שמאל (לא ידעתי) / הקראה ב-TTS.

■ **אלוף האיות** — הקלדת המילה האנגלית לפי התרגום העברי.

■ **לפי נושא** — בחירת קטגוריה — שאלון ממוקד.

■ יש גם "מצב טעויות" — שאלון על מילים שטעו בהן בשאלון האחרון.

---

○ QuizFragment : שם המסך — שאלון

■ מסך השאלון. מוצגת מילה באנגלית בכרטיס, ומתחתיה 4 כפתורי תרגום. המשתמש צריך לבחור את התרגום הנכון.

■ בחלק העליון: מספר שאלה מתוך 20 ופס התקדמות.

■ בחירת תשובה נכונה: הכפתור מתעדכן לירוק, מופיע "+10 XP" צף עם אנימציה, הכרטיס מתחלף לאחר שנייה לשאלה הבאה.

■ בחירת תשובה שגויה: הכפתור מתעדכן לאדום, הכפתור הנכון מתעדכן לירוק. Word.errorInQuiz מסומן true.

■ לחיצה על שם המילה — TTS מקריא את ההגייה (אם TTS מופעל בהגדרות).

■ לאחר 20 שאלות — דיאלוג תוצאה: ציון, אנימציית גביע/שריר, XP שנצברו, כפתורי "נסה שוב" / "סיום".

---

○ SpellingFragment : שם המסך — אלוף האיות

■ מסך האיות. מוצג התרגום העברי של המילה, ועל המשתמש להקליד את המילה באנגלית.

■ כפתור "הקשב" מפעיל TTS שמקריא את המילה בקול (כרמז אם קשה).

■ שדה הקלט בו המשתמש כותב. לחיצת "בדוק":
  - נכון: הצגת ✓ ירוק + "+10 XP"
  - שגוי: הצגת ✗ אדום + המילה הנכונה

■ לאחר סיום כל מילות הסשן — דיאלוג תוצאה.

---

○ FlashcardsFragment : שם המסך — כרטיסיות

■ מסך הכרטיסיות. מוצגת מילה אנגלית בצד אחד של הכרטיסייה. לחיצה הופכת את הכרטיסייה (Flip animation) ומציגה את התרגום העברי ומשפט הדוגמה.

■ Swipe ימינה — "ידעתי": correctAttempts++ ו-FlashcardCorrect++. הכרטיסייה נעלמת ימינה.

■ Swipe שמאלה — "לא ידעתי": errorInFlashcards = true. הכרטיסייה נעלמת שמאלה.

■ כפתור רמקול — TTS מקריא את המילה.

■ לאחר סיום כל המילות — הצגת תוצאת הסשן.

---

○ ScanFragment : שם המסך — סריקה

■ המסך המורכב ביותר ב-EasyLex. יש לו שני מצבים: **מצב סריקה** ו**מצב סקירה**.

■ במצב סריקה: תצוגה חיה של המצלמה (CameraX Preview). כפתור "ספר" / "טקסט חופשי" לבחירת מצב OCR. כפתור שלח (Shutter) לצילום. כפתור גלריה לבחירת תמונה קיימת.

■ לאחר הצילום — מעבר למצב סקירה: התמונה מוצגת עם תיבות מסגרת צבעוניות על כל מילה מזוהה. תיבה ירוקה = מילה שכבר קיימת ב-DB. תיבה צהובה = מילה חדשה.

■ לחיצה על תיבה ירוקה — TTS מקריא + דיאלוג "הוסף לרשימה האישית?".

■ לחיצה על תיבה צהובה — ML Kit Translate מתרגם — דיאלוג הוספה עם שדות EN + HE מולאו מראש — "שמור".

■ כפתור "שמור הכל" — שומר את כל המילות שנבחרו בבת-אחת עם הצגת "נשמרו X מילים".

■ כפתור "X" — חזרה למצב סריקה.

---

○ StatisticsFragment : שם המסך — סטטיסטיקות

■ מסך הסטטיסטיקות. מוצג מידע גמיפיקציה: רמה נוכחית, XP כולל, XP לרמה הבאה, רצף ימים רצופים.

■ גרף עמודות של פעילות 7 ימים אחרונים — כמה תשובות נכונות ביום.

■ רשימת "הכי טובים" — המילים עם Mastery הנמוך ביותר (הכי צריכות תרגול).

■ הנתונים נסנכרנים מ-Firestore ב-onResume, כך שאם המשתמש השתמש במכשיר אחר — הנתונים מעודכנים.

---

○ SettingsFragment : שם המסך — הגדרות

■ מסך ניהול האפליקציה. מחולק לכמה קטעים: פרטי חשבון, הגדרות לימוד, ניהול נתונים, אודות.

■ **פרטי חשבון:** שם המשתמש (ניתן לעריכה), כתובת אימייל, כפתור "שנה שם".

■ **הגדרות לימוד:** Slider לבחירת יעד יומי (5-50 מילים), מתג TTS (הקראת מילים בקול).

■ **ניהול נתונים:** "סנכרן עכשיו" (סנכרון מ-Firestore), "נקה רשימה אישית" (מחיקת מילים אישיות עם אזהרה), "אפס התקדמות" (איפוס XP + תרגול עם אזהרה כפולה).

■ **אודות:** גרסת האפליקציה.

---

○ ProfileFragment : שם המסך — פרופיל

■ מסך הפרופיל של המשתמש. מוצג: שם, XP, רמה, רצף יומי.

■ כרטיס ההישגים: מדליות על אבני דרך (100 מילים, 500 מילים, Mastery 5 ראשון וכו').

■ כפתור "התנתק" — Firebase Auth signOut() + מעבר ל-LoginActivity.

---

○ AdminEditFragment : שם המסך — ממשק מנהל

■ מסך זמין רק למשתמשי admin. מוצגת רשימת כל 1,902 המילים הגלובליות כ-RecyclerView.

■ **גלילה ומחיקה:** ItemTouchHelper מאפשר Swipe שמאלה על כרטיס — רקע אדום עם אייקון מחיקה — מחיקה מ-Firestore — Snackbar "נמחקה — בטל" שמאפשר שחזור.

■ **הוספת מילה:** FAB (+) — דיאלוג עם שדות: מילה*, תרגום*, חלק דיבר, משפט EN, משפט HE, תגיות — "שמור" — הוספה ל-Firestore — הרשימה מתרענת.

■ **חיפוש:** SearchView בחלק העליון — הרשימה מסתננת ב-real-time — גלילה לתוצאה הראשונה.

■ כרטיס כל מילה מציג: אנגלית [חלק דיבר] | עברית, ומתחת משפטי דוגמה.

---

## ★ תרשים זרימה — UML :

○ תרשימי ה-UML של הפרויקט נמצאים בתיקיית uml/ בפרויקט. הם נוצרו בפורמט PlantUML:

■ 01_data_layer.puml — מחלקות שכבת הנתונים (Word, WordDao, WordRoomDatabase, WordRepository)
■ 02_auth_navigation.puml — זרימת אימות + MainActivity
■ 03_mywords_viewmodel.puml — MyWords MVVM מלא
■ 04_practice_modules.puml — מודולי תרגול (Quiz, Spelling, Flashcards, Practice)
■ 05_scan_gamification.puml — ScanFragment + GamificationEngine + StreakManager
■ 06_admin_stats_settings.puml — Admin, Statistics, Settings
■ 07_seq_sync.puml — זרימת סנכרון Firestore (Sequence Diagram)
■ 08_seq_realtime_delete.puml — זרימת מחיקה בזמן אמת (Sequence Diagram)
■ 09_seq_quiz.puml — זרימת השאלון + Gamification (Sequence Diagram)
■ 10_seq_ocr.puml — זרימת OCR + תרגום + שמירה (Sequence Diagram)
■ 11_seq_auth.puml — זרימת אימות משתמש (Sequence Diagram)

---

# ~ מימוש הפרויקט ~

## ★ תיאור מחלקות :

---

○ Word.java — שם המחלקה

■ תפקיד המחלקה: מחלקה זו מייצגת מילה אנגלית אחת — הישות המרכזית של כל המערכת. היא מוגדרת כ-Entity של Room (@Entity), כלומר כל אובייקט Word הוא שורה בטבלת words_table. בנוסף, Firestore יודע לקרוא ולכתוב אותה ישירות (POJO עם constructor ריק + Getters/Setters).

■ מחלקות אחרות שמשתמשות במחלקה זו: WordDao בשביל שאילתות, WordRepository בשביל לוגיקת סנכרון, MyWordsViewModel, QuizFragment, SpellingFragment, FlashcardsFragment, ScanFragment, GamificationEngine.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private int id | מזהה ייחודי, נוצר אוטומטית על ידי Room (PrimaryKey autoGenerate) |
| private String englishWord | המילה באנגלית |
| private String hebrewTranslation | התרגום לעברית |
| private String partOfSpeech | חלק הדיבר (n/v/adj/adv/prep/pron) |
| private String exampleSentence | משפט דוגמה באנגלית |
| private String hebrewExample | משפט דוגמה בעברית |
| private String tags | קטגוריה לסינון (Learning, Actions, וכו') |
| private long creationTimestamp | זמן הוספה — Unix milliseconds |
| private int correctAttempts | מספר תשובות נכונות כולל |
| private int totalAttempts | מספר כולל של ניסיונות |
| private int spellingCorrect | תשובות נכונות ספציפית ב-Spelling |
| private boolean isFavorite | true = מילה אישית של המשתמש |
| private boolean isVerified | true = מילה גלובלית מהמנהל |
| private boolean errorInQuiz | המשתמש טעה בשאלון האחרון |
| private boolean errorInSpelling | המשתמש טעה באיות |
| private boolean errorInFlashcards | המשתמש לא ידע בכרטיסיות |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| public Word() | קונסטרקטור ריק — נדרש ל-Room ול-Firestore |
| public Word(String eng, String heb, ...) | קונסטרקטור מלא (@Ignore) — להוספה ידנית בקוד |
| public int getMasteryLevel() | מחשב רמת שליטה: min(5, floor(correct/(total+1)×5)) |
| get/set לכל שדה | Getter/Setter עבור כל 16 השדות |

---

○ WordDao.java — שם המחלקה

■ תפקיד המחלקה: זהו ממשק ה-DAO (Data Access Object) של Room. הוא מגדיר את כל פעולות הקריאה והכתיבה לטבלת words_table. Room יוצר את המימוש אוטומטית בזמן קומפילציה מתוך האנוטציות.

■ מחלקות אחרות שמשתמשות במחלקה זו: WordRepository היא הכניסה הבלעדית ל-DAO — אף Fragment לא ניגש ישירות ל-DAO.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void insert(Word word) | הוספת מילה — OnConflictStrategy.IGNORE (לא דורס קיימות) |
| void update(Word word) | עדכון מילה לפי ID |
| void delete(Word word) | מחיקת מילה לפי ID |
| LiveData<List<Word>> getAllWords() | כל המילים בסדר יצירה הפוך — מתעדכן אוטומטית |
| LiveData<List<Word>> getPersonalWords() | רק מילים אישיות (isVerified=false) |
| List<String> getVerifiedEnglishWords() | רשימת שמות מילים גלובליות — לסנכרון |
| void deleteUnmarkedGlobalWords() | מחיקת מילים שאינן מסומנות — Migration חד-פעמי |
| void deletePersonalWords() | מחיקת כל המילים האישיות |
| void deleteVerifiedWordByEnglish(String eng) | מחיקת מילה גלובלית לפי שם — נקרא ע"י Firestore Listener |
| void deleteVerifiedWords() | מחיקת כל המילים הגלובליות |
| void deleteAllWords() | מחיקה מוחלטת של הטבלה |
| void resetAllWordProgress() | איפוס כל נתוני התרגול — correctAttempts, totalAttempts, errorFlags |

---

○ WordRoomDatabase.java — שם המחלקה

■ תפקיד המחלקה: ניהול החיבור למסד הנתונים SQLite. מחלקה זו מגדירה את מסד הנתונים: שם (word_database), גרסה (3), וטבלות ({Word.class}). היא ממומשת כ-Singleton כדי למנוע יצירת חיבורים מרובים.

■ מחלקות אחרות שמשתמשות במחלקה זו: WordRepository — מקבלת מכאן את ה-DAO.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private static volatile WordRoomDatabase INSTANCE | המופע היחיד — volatile לThread-safety |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| public abstract WordDao wordDao() | מחזיר את ה-DAO — Room מממש אוטומטית |
| public static WordRoomDatabase getDatabase(Context context) | מחזיר/יוצר את המופע היחיד עם Double-Checked Locking |

---

○ WordRepository.java — שם המחלקה

■ תפקיד המחלקה: שכבת המתווך בין ה-ViewModels לשכבת הנתונים (Room + Firestore). מסתירה מהViewModels מאיפה מגיעים הנתונים — האם מה-DB המקומי או מהענן. כל פעולת DB מתבצעת ב-Background Thread דרך ExecutorService.

■ מחלקות אחרות שמשתמשות במחלקה זו: MyWordsViewModel, StatisticsViewModel, SettingsFragment.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private final WordDao mWordDao | גישה לשאילתות SQLite |
| private final LiveData<List<Word>> mAllWords | ערוץ חי לכל המילים |
| static final ExecutorService databaseWriteExecutor | מאגר 4 Threads לפעולות ברקע |
| private ListenerRegistration mDeleteListener | מזהה ה-Listener לניהול מחזור חיים |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| WordRepository(Application application) | אתחול — מקבל את ה-DAO מה-Database Singleton |
| LiveData<List<Word>> getAllWords() | מחזיר LiveData לכל המילים |
| LiveData<List<Word>> getPersonalWords() | מחזיר LiveData למילים אישיות |
| void insert(Word word) | הוספה על ExecutorService Thread |
| void update(Word word) | עדכון על ExecutorService Thread |
| void delete(Word word) | מחיקה על ExecutorService Thread |
| void deletePersonalWords() | מחיקת מילים אישיות ברקע |
| void resetAllWordProgress(Runnable onDone) | איפוס התקדמות — callback ל-Main Thread עם onDone |
| void syncGlobalWordsFromFirestore(Runnable onComplete) | סנכרון דו-כיווני: מוסיף חדשות, מוחק שנמחקו מ-Firestore |
| void startGlobalDeleteListener() | מפעיל Snapshot Listener למחיקות מנהל בזמן אמת |
| void stopGlobalDeleteListener() | מסיר את ה-Listener — נקרא ב-ViewModel.onCleared() |

---

○ SplashActivity.java — שם המחלקה

■ תפקיד המחלקה: נקודת הכניסה לאפליקציה. מציגה אנימציה ועושה בו-זמנית בדיקת גרסה ובדיקת מצב התחברות. לאחר שתיהן סיימו, מנתבת למסך הנכון.

■ מחלקות אחרות שמשתמשות במחלקה זו: לא נקראת ממחלקות אחרות — היא ה-Launcher של האפליקציה.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private boolean authReady | האם בדיקת Auth הסתיימה |
| private boolean animDone | האם אנימציית הלוגו הסתיימה |
| private boolean goToMain | true = MainActivity, false = LoginActivity |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void onCreate() | אתחול — מפעיל אנימציה ושתי בדיקות במקביל |
| void checkAppVersion() | קוראת מ-Firestore את min_version, מציגה ForceUpdate אם נדרש |
| void resolveAuth() | בודקת FirebaseAuth.getCurrentUser(), מגדירה goToMain |
| void tryNavigate() | מנתבת למסך הנכון — רק אחרי שאנימציה + Auth שניהם סיימו |
| void showForceUpdateDialog() | דיאלוג שאינו ניתן לסגירה — המשתמש חייב לעדכן |

---

○ LoginActivity.java — שם המחלקה

■ תפקיד המחלקה: מסך הכניסה. מנהלת כניסה עם אימייל/סיסמה וכניסה עם Google.

■ מחלקות אחרות שמשתמשות במחלקה זו: SplashActivity מנתבת אליה. RegisterActivity מקושרת אליה.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private ActivityLoginBinding binding | ViewBinding לממשק |
| private FirebaseAuth mAuth | מנגנון האימות של Firebase |
| private ActivityResultLauncher googleSignInLauncher | Callback לתוצאת Google Sign-In |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void onCreate() | אתחול UI + Listeners |
| void loginWithEmail(String email, String password) | כניסה עם Firebase Auth, ניווט ל-MainActivity בהצלחה |
| void loginWithGoogle() | פתיחת Google Sign-In Intent |
| void handleGoogleResult(Intent data) | עיבוד תוצאת Google + Firebase Auth signInWithCredential() |

---

○ RegisterActivity.java — שם המחלקה

■ תפקיד המחלקה: מסך ההרשמה. מנהלת יצירת חשבון חדש עם Firebase Auth.

■ מחלקות אחרות שמשתמשות במחלקה זו: LoginActivity מקושרת אליה.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private ActivityRegisterBinding binding | ViewBinding לממשק |
| private FirebaseAuth mAuth | מנגנון האימות |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void onCreate() | אתחול UI + Listeners |
| void validateAndRegister() | ולידציה מקומית + createUserWithEmailAndPassword() |
| void navigateToMain() | מעבר ל-MainActivity לאחר הרשמה מוצלחת |

---

○ MainActivity.java — שם המחלקה

■ תפקיד המחלקה: ה-Activity הראשי שמחזיק את NavHostFragment ואת BottomNavigationView. אינו מכיל לוגיקה עסקית — תפקידו להחזיק את ה"מסגרת" של האפליקציה.

■ מחלקות אחרות שמשתמשות במחלקה זו: כל ה-Fragments רצים בתוכו.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void onCreate() | חיבור NavController ל-BottomNavigationView + AppBarConfiguration |
| boolean onSupportNavigateUp() | טיפול בכפתור Back |

---

○ GamificationEngine.java — שם המחלקה

■ תפקיד המחלקה: מנוע הגמיפיקציה המרכזי. Singleton שמנהל XP, רמות, ורצף יומי. כותב לשני מקומות: SharedPreferences (גישה מהירה) ו-Firestore (גיבוי ענן). מחשב רמה לפי נוסחה: L = floor(sqrt(XP/100)) + 1.

■ מחלקות אחרות שמשתמשות במחלקה זו: QuizFragment, SpellingFragment, FlashcardsFragment, StatisticsFragment, PracticeFragment, SettingsFragment.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private static volatile GamificationEngine instance | המופע היחיד (Singleton Thread-safe) |
| private final SharedPreferences prefs | גישה מהירה ל-XP, Level, Streak |
| private final FirebaseFirestore db | גיבוי לענן |
| KEY_TOTAL_XP, KEY_LEVEL, KEY_STREAK | מפתחות SharedPreferences |
| XP_CORRECT_ANSWER = 10 | XP על תשובה נכונה |
| XP_MODULE_COMPLETE = 50 | XP על סיום סשן |
| XP_WORD_MASTERED = 100 | בונוס XP על Mastery 4 ל-5 |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| GamificationEngine getInstance(Context ctx) | מחזיר/יוצר את המופע היחיד (Double-Checked Locking) |
| void onCorrectAnswer(Word before, Word after) | +10 XP, +100 אם Mastery עלה ל-5, עדכון רצף יומי |
| void onModuleComplete() | +50 XP על סיום סשן |
| int getTotalXp() | סך ה-XP מ-SharedPreferences |
| int getCurrentLevel() | הרמה הנוכחית מ-SharedPreferences |
| int getStreak() | הרצף הנוכחי |
| int getDailyCorrectCount() | כמה תשובות נכונות היום |
| int[] getWeeklyActivity() | מערך של 7 ימים אחרונים |
| int getXpForLevelStart(int level) | XP בתחילת הרמה: (level-1)² × 100 |
| int getXpForNextLevel() | XP לסיום הרמה הנוכחית |
| int getXpWithinLevel() | XP שנצבר בתוך הרמה הנוכחית |
| void syncFromFirestore(Runnable onComplete) | טוען XP/Level/Streak מ-Firestore |
| private void addXp(int amount) | מוסיף XP + מחשב רמה + שומר SharedPrefs + Firestore |
| private int computeLevel(int xp) | נוסחת הרמה: floor(sqrt(xp/100)) + 1 |
| private void checkStreak() | בדיקת ועדכון רצף דרך StreakManager |
| private void recordDailyActivity() | מגדיל מונה תשובות נכונות יומי |

---

○ StreakManager.java — שם המחלקה

■ תפקיד המחלקה: ניהול לוגיקת הרצף היומי (Streak). בודקת כמה ימים רצופים המשתמש היה פעיל. אם הפעילות הייתה אתמול — Streak++. אם עבר יותר מיום — מאפסת ל-1. שומרת את הרצף ב-Firestore.

■ מחלקות אחרות שמשתמשות במחלקה זו: GamificationEngine.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void checkAndUpdate(int currentStreak, FirebaseUser user, ...) | בודקת lastActiveDate מ-Firestore, מחשבת הפרש ימים, מעדכנת Streak |

---

○ MyWordsViewModel.java — שם המחלקה

■ תפקיד המחלקה: ה-ViewModel של מסך "המילים שלי". שורד סיבובי מסך ומנהל את ה-Repository. אחראי על: Migration חד-פעמי בהתקנה ראשונה, throttle של סנכרון Firestore (פעם ב-24 שעות), והפעלת ה-Snapshot Listener.

■ מחלקות אחרות שמשתמשות במחלקה זו: MyWordsFragment, QuizFragment, SpellingFragment, FlashcardsFragment, ScanFragment.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private final WordRepository mRepository | גישה לשכבת הנתונים |
| private final LiveData<List<Word>> mAllWords | ערוץ חי לכל המילים |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| MyWordsViewModel(Application application) | אתחול + Migration + startGlobalDeleteListener() + Throttle sync |
| LiveData<List<Word>> getAllWords() | מחזיר את כל המילים |
| LiveData<List<Word>> getPersonalWords() | מחזיר מילים אישיות בלבד |
| void insert(Word word) | הוספת מילה חדשה |
| void update(Word word) | עדכון מילה קיימת |
| void deletePersonalWords() | מחיקת כל המילים האישיות |
| void syncFromCloud() | סנכרון מיידי מ-Firestore ללא throttle |
| protected void onCleared() | עצירת ה-Snapshot Listener למניעת memory leak |

---

○ WordListAdapter.java — שם המחלקה

■ תפקיד המחלקה: Adapter לרשימת המילים ב-MyWordsFragment. ממפה רשימת Word לרשימה ויזואלית. מממש DiffUtil לעדכון יעיל — רק שורות שהשתנו מתחדשות.

■ מחלקות אחרות שמשתמשות במחלקה זו: MyWordsFragment.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| WordListAdapter(OnWordClickListener listener) | קבלת callback ללחיצה על מילה |
| void submitList(List<Word> words) | עדכון הרשימה עם DiffUtil |
| onCreateViewHolder / onBindViewHolder | יצירת ViewHolder + מיפוי נתונים לפריט |
| void filter(String query, String mode) | סינון לפי חיפוש + מצב (הכל/אישי/גלובלי) |

---

○ QuizFragment.java — שם המחלקה

■ תפקיד המחלקה: מסך השאלון. מציג 20 שאלות עם 4 אפשרויות, מנהל ציון, מפעיל XP, ומציג דיאלוג תוצאה.

■ מחלקות אחרות שמשתמשות במחלקה זו: PracticeFragment, DashboardFragment.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private MyWordsViewModel viewModel | ViewModel לגישה לנתונים |
| private List<Word> quizWords | 20 המילות הנבחרות לסשן |
| private List<Word> allMasterWords | כל המילים מה-DB (לבניית distractors) |
| private int currentIndex, score | אינדקס שאלה נוכחית וציון |
| private String quizType | "DAILY" / "MISTAKES" / "PERSONAL" / "CATEGORY" |
| private TextToSpeech tts | הקראת מילים בקול |
| private Handler handler | תזמון המעבר לשאלה הבאה (שנייה אחת) |
| private MaterialButton[] optButtons | 4 כפתורי אפשרויות |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + אתחול ViewBinding + Observers |
| prepareQuiz(String type, String category) | בניית pool לפי type + אלגוריתם 60/20/20 |
| addUpTo(List dest, List src, int max) | הוספת עד max פריטים מ-src ל-dest |
| showNextQuestion() | הצגת שאלה עם 4 אפשרויות (1 נכון + 3 Distractors) |
| checkAnswer(MaterialButton btn, Word word) | בדיקת תשובה + עדכון DB + XP + השהייה |
| showXpAnimation(String text) | אנימציית "+10 XP" צפה עם Fade |
| finishQuiz() | שמירת ציון יומי + onModuleComplete() |
| showResultDialog(int score, int total) | דיאלוג עם אנימציה + "נסה שוב" / "סיום" |
| speakCurrentWord() | TTS הקראה של המילה האנגלית |
| onDestroyView() | ניקוי Handler + TTS + binding = null |

---

○ SpellingFragment.java — שם המחלקה

■ תפקיד המחלקה: מסך אלוף האיות. מציג תרגום עברי, המשתמש מקליד את המילה האנגלית. תומך ב-TTS כרמז.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + אתחול + Observer |
| loadSpellingWords() | בחירת מילים לסשן מה-DB |
| showCurrentWord() | הצגת תרגום + הסתרת מילה |
| checkSpelling() | השוואה case-insensitive + עדכון DB + XP |
| onCorrect() | +10 XP, correctAttempts++, spellingCorrect++ |
| onWrong() | הצגת מילה נכונה, errorInSpelling = true |
| speakWord() | TTS הקראה |
| onDestroyView() | binding = null, TTS shutdown |

---

○ FlashcardsFragment.java — שם המחלקה

■ תפקיד המחלקה: מסך הכרטיסיות. מציג מילים בפורמט Swipe, עם Flip animation להצגת תרגום.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + אתחול + TouchListener |
| setupCard(Word word) | הצגת מילה אנגלית על הכרטיסייה |
| flipCard() | אנימציית הפיכה + הצגת תרגום |
| handleSwipe(boolean correct) | Swipe ימין (ידעתי) / שמאל (לא ידעתי) + עדכון DB |
| onDestroyView() | binding = null, TTS shutdown |

---

○ ScanFragment.java — שם המחלקה

■ תפקיד המחלקה: מסך הסריקה — המסך המורכב ביותר. מנהל CameraX Preview, צילום תמונות, קריאת EXIF, OCR, תרגום, ושמירה.

■ מחלקות אחרות שמשתמשות במחלקה זו: GraphicOverlay בשביל ציור תיבות OCR.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private ProcessCameraProvider cameraProvider | ניהול מצלמה |
| private ImageCapture imageCapture | צילום תמונות |
| private TextRecognizer textRecognizer | ML Kit OCR |
| private Translator translator | ML Kit Translate |
| private GraphicOverlay graphicOverlay | שכבת תיבות OCR |
| private MyWordsViewModel viewModel | שמירה ל-DB |
| private List<OcrResult> ocrResults | תוצאות OCR לפני שמירה |
| private boolean isReviewMode | מצב סריקה vs מצב סקירה |
| private boolean isBookMode | true=ספר (Word-level), false=טקסט (Line-level) |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + CameraX Permissions |
| startCamera() | CameraX bindToLifecycle (Preview + ImageCapture) |
| takePicture() | CameraX takePicture() — savedUri |
| loadRotatedBitmap(Uri uri) | קריאת EXIF + Matrix.postRotate() |
| runOcr(Bitmap bitmap) | ML Kit OCR.process(InputImage) |
| extractWordBoxes(Text result) | Element-level — מילים בודדות (מצב ספר) |
| extractFreeTextBoxes(Text result) | Line-level — שורות שלמות (מצב טקסט) |
| translateAll(List<String> words) | ML Kit Translate — לולאת לכל מילה |
| enterReviewMode() | מציג תמונה + GraphicOverlay + תיבות |
| saveSelectedWords() | שמירת כל המילות המסומנות ל-DB |
| onDestroyView() | CameraX unbind + Translator.close() + binding = null |

---

○ GraphicOverlay.java — שם המחלקה

■ תפקיד המחלקה: Custom View שמצייר תיבות מסגרת צבעוניות על תמונת ה-OCR. מאזינה ללחיצות המשתמש על תיבות בודדות ומחזירה callback לScanFragment.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| setWordBoxes(List<WordBox> boxes) | הגדרת תיבות לציור — ירוק (קיים) / צהוב (חדש) |
| onDraw(Canvas canvas) | ציור תיבות על ה-Canvas |
| onTouchEvent(MotionEvent event) | זיהוי לחיצה על תיבה ספציפית — callback לScanFragment |

---

○ StatisticsFragment.java — שם המחלקה

■ תפקיד המחלקה: הצגת סטטיסטיקות לימוד: XP, רמה, רצף, גרף שבועי, ומילות ה"מאתגרות" ביותר.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + Observer על LiveData |
| onResume() | GamificationEngine.syncFromFirestore() + updateUI() |
| updateXpCard() | עדכון כרטיס XP/Level/Streak |
| drawWeeklyChart() | גרף עמודות של 7 ימים אחרונים |
| showHardestWords(List<Word> words) | 5 המילות עם Mastery נמוך ביותר |
| onDestroyView() | binding = null |

---

○ SettingsFragment.java — שם המחלקה

■ תפקיד המחלקה: ניהול חשבון, הגדרות לימוד, ופעולות נתונים. מציגה ומאפשרת לשנות: שם משתמש, יעד יומי, TTS, ניקוי/איפוס נתונים.

■ קבועים ציבוריים:

| שם הקבוע | ערך | שמש ב |
|---------|-----|--------|
| PREFS_SETTINGS | "settings_prefs" | כל Fragments |
| KEY_DAILY_GOAL | "daily_goal_words" | PracticeFragment, QuizFragment |
| KEY_TTS_ENABLED | "tts_enabled" | Quiz, Spelling, Flashcards, MyWords |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + טעינת כל הסקציות |
| loadAccountInfo() | Firebase Auth — שם + אימייל |
| showEditNameDialog() | MaterialAlertDialog + TextInputLayout |
| updateDisplayName(String name) | Firebase Auth + Firestore merge |
| loadLearningSettings() | Slider (יעד יומי) + Switch (TTS) |
| setupDataActions() | 3 כפתורי נתונים |
| onDestroyView() | binding = null |

---

○ ProfileFragment.java — שם המחלקה

■ תפקיד המחלקה: הצגת פרופיל המשתמש: שם, XP, רמה, רצף, הישגים, וכפתור התנתקות.

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + טעינת נתוני משתמש |
| onResume() | GamificationEngine sync |
| loadProfile() | Firebase Auth — displayName + Firestore — XP/Level |
| setupAchievements() | הצגת מדליות לפי סף הישגים |
| signOut() | Firebase Auth.signOut() + ניווט ל-LoginActivity |
| onDestroyView() | binding = null |

---

○ AdminEditFragment.java — שם המחלקה

■ תפקיד המחלקה: ממשק המנהל לעריכת מאגר המילים הגלובלי. מאפשר הוספה, מחיקה (עם Undo), וחיפוש בין 1,902 המילים.

■ מחלקות אחרות שמשתמשות במחלקה זו: AdminWordAdapter בשביל ה-RecyclerView.

■ תכונות המחלקה:

| שם השדה | הסבר |
|---------|-------|
| private FragmentAdminEditBinding binding | ViewBinding |
| private AdminWordAdapter adapter | Adapter לרשימה |
| private ItemTouchHelper touchHelper | גרירה ומחיקה |
| private FirebaseFirestore db | גישה ישירה ל-Firestore לפעולות מנהל |
| private List<DocumentSnapshot> docs | כל המסמכים מ-Firestore |

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| onCreateView() | inflate + טעינת Firestore |
| loadWordsFromFirestore() | שליפת כל המסמכים + עדכון Adapter |
| setupItemTouchHelper() | Swipe — מחיקה + Snackbar "בטל" |
| deleteWord(int pos) | מחיקה מ-Firestore + adapter.removeItem() |
| undoDelete(int pos, DocumentSnapshot doc) | שחזור מסמך ל-Firestore + adapter.insertItem() |
| showAddWordDialog() | פתיחת dialog_admin_add_word.xml |
| saveNewWord(Map<String,Object> data) | כתיבה ל-Firestore |
| setupSearch() | SearchView — סינון + scrollToPosition |
| onDestroyView() | binding = null |

---

○ AdminWordAdapter.java — שם המחלקה

■ תפקיד המחלקה: Adapter לרשימת המנהל. כרטיסי קריאה-בלבד — ללא EditText. מציג עברית מימין ואנגלית משמאל (layoutDirection="ltr" לתיקון RTL/LTR).

■ פעולות המחלקה:

| שם הפעולה | הסבר |
|-----------|-------|
| void setWords(List<DocumentSnapshot> docs) | עדכון רשימה |
| void removeItem(int pos) | מחיקה ויזואלית מהרשימה |
| void insertItem(int pos, DocumentSnapshot doc) | הכנסה חזרה (Undo) |
| void filter(String query) | סינון לפי חיפוש |
| onCreateViewHolder / onBindViewHolder | יצירת ViewHolder + מיפוי |

---

## ★ בסיס נתונים :

○ סקירת בסיס הנתונים:

■ בסיס הנתונים של EasyLex מבוסס על Room — ספריית Google שעוטפת SQLite. Room מספק type-safety (בדיקת שאילתות SQL בזמן קומפילציה) ותמיכה ב-LiveData (עדכון UI אוטומטי).

■ שם הDB: word_database | גרסה: 3 | Migration: fallbackToDestructiveMigration (אם גרסה ישנה — מחיקה ובנייה מחדש).

■ טבלה אחת בלבד: words_table. אין Join Tables, אין relational structure. כל מידע על מילה — בשורה אחת.

○ פעולות על/עם בסיס הנתונים:

■ כל הפעולות ל-DB מתבצעות אך ורק דרך WordRepository, על ExecutorService Thread — לעולם לא על ה-Main Thread. שאילתות המחזירות LiveData מורצות ב-Background Thread אוטומטית.

■ לפי התכנון המתואר בפרק תיאור פעולות על המידע — כל פעולות CRUD (Create, Read, Update, Delete) מתבצעות דרך WordDao שמחלקת WordRepository היא הגישה הבלעדית אליו.

■ Singleton Pattern עם Double-Checked Locking:

```java
if (INSTANCE == null) {
    synchronized (WordRoomDatabase.class) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context,
                WordRoomDatabase.class, "word_database")
                .fallbackToDestructiveMigration()
                .build();
        }
    }
}
```

■ OnConflictStrategy.IGNORE ב-insert(): אם מנסים להכניס מילה שכבר קיימת (לפי ה-PRIMARY KEY) — Room מתעלם מהפעולה ושומר את הנתונים הקיימים. זה קריטי בסנכרון — נתוני תרגול (correctAttempts) לא נאבדים.

---

# ~ מדריך למשתמש ~

○ הסבר על האפליקציה:

■ האפליקציה מיועדת לאנדרואיד מגרסה 7.0 (API 24) ומעלה. ניתן להתקינה דרך Android Studio בחיבור USB עם Developer Options מופעל, או דרך קובץ APK. האפליקציה נבדקה על מכשיר פיזי ועל אמולטור Pixel 7 API 34.

---

■ מסך פתיחה:

○ כשפותחים את EasyLex בפעם הראשונה — מופיע מסך הפתיחה עם לוגו האפליקציה ואנימציה. מתחת ללוגו מופיע שם האפליקציה.

○ ברקע מתבצעות שתי בדיקות: בדיקת גרסה ובדיקת מצב כניסה. לאחר שהאנימציה מסיימת — האפליקציה מנתבת לאחד משני מסכים: מסך הכניסה (אם לא מחובר) או מסך הבית (אם כבר מחובר).

---

■ מסך הרשמה:

○ בלחיצה על "הירשם" ממסך הכניסה — מופיע מסך ההרשמה.

○ יש למלא: שם מלא, כתובת אימייל, סיסמה (מינימום 6 תווים), ואישור סיסמה. חייב למלא את כל השדות — אם לא ממלאים, מופיעה הודעת שגיאה מתחת לשדה הריק.

○ לחיצה על "הירשם" — Firebase יוצר חשבון חדש. אם האימייל כבר רשום — תופיע הודעת שגיאה. אם הצלחנו — עוברים ישירות למסך הבית.

---

■ מסך כניסה:

○ כשפותחים את האפליקציה ואין חשבון מחובר — מופיע מסך הכניסה.

○ יש להזין אימייל וסיסמה. לחיצה על "כניסה" — Firebase מאמת. אם פרטים שגויים — מופיעה הודעת שגיאה.

○ כניסה עם Google: לחיצה על "כניסה עם Google" — בחירת חשבון Google — כניסה אוטומטית — מסך הבית.

○ אם אין חשבון — לחיצה על "הירשם" מובילה למסך ההרשמה.

---

■ מסך הבית:

○ לאחר כניסה מוצלחת מופיע מסך הבית. בחלק העליון ברכה עם שם המשתמש ותאריך.

○ כרטיס XP מציג: "רמה X", פס התקדמות ל-"XP הבאים", וסמל אש עם מספר הרצף היומי.

○ לחיצה על "מבחן יומי" — פותח שאלון יומי של 20 מילות. שאלון זה משתנה פעם ביום (אותן 20 מילות לכל אורך היום).

○ "מילה של היום" — מילה שנבחרה אקראית עם התרגום ומשפט דוגמה. לחיצה על כפתור הרמקול — האפליקציה מקריאה את המילה.

---

■ מסך המילים שלי:

○ לחיצה על "מילים" ב-BottomNavigation — מסך המילים. הרשימה מציגה את כל המילות בסדר כרונולוגי הפוך (החדשות למעלה).

○ **חיפוש:** הקלדה בשדה החיפוש מסננת את הרשימה בזמן אמת לפי כל שדה (EN, HE, תגיות).

○ **סינון:** לחיצה על כפתור הסינון — בחירה: "הכל" / "אישיות" / "גלובליות" / לפי קטגוריה.

○ **הוספה ידנית:** לחיצה על FAB (כפתור הפלוס) — דיאלוג הוספה עם שדות: מילה באנגלית, תרגום, חלק דיבר, תגיות — "שמור".

○ **לחיצה על מילה:** נפתח דיאלוג מפורט עם:
● המילה ב-EN גדול + [חלק דיבר]
● תרגום בעברית
● משפטי דוגמה (EN + HE)
● כפתור "הקשב" (TTS)
● כפתור "ערוך" — עריכת פרטי המילה
● כפתור "מחק" + אישור מחיקה

---

■ מסך התרגול:

○ לחיצה על "תרגול" ב-BottomNavigation. מוצג כרטיס XP עם מצב הגמיפיקציה הנוכחי.

○ 4 כרטיסי תרגול:
● שאלון — 20 שאלות אדפטיביות
● כרטיסיות — Flip cards
● אלוף האיות — הקלדת מילה
● לפי נושא — בחירת קטגוריה

○ יש גם "שאלון טעויות" — מופיע רק אם יש מילות שטעו בהן בשאלון האחרון.

---

■ שאלון:

○ מוצגת מילה בכרטיס, ומתחתיה 4 כפתורים עם תרגומים אפשריים.

○ לחיצה על תשובה נכונה — הכפתור מתעדכן לירוק — "+10 XP" צף מעלה — לאחר שנייה — שאלה הבאה.

○ לחיצה על תשובה שגויה — הכפתור מתעדכן לאדום — הכפתור הנכון מתעדכן לירוק — השתהייה שנייה — שאלה הבאה.

○ בפינה שמאל-עליון: מספר שאלה (1/20) ופס התקדמות.

○ לאחר 20 שאלות — דיאלוג תוצאה:
● ציון (X מתוך 20)
● XP שנצברו
● אנימציית גביע (אם 60%+) או שריר (אם פחות)
● כפתורי "נסה שוב" / "סיום"

---

■ אלוף האיות:

○ מוצג התרגום העברי. על המשתמש להקליד את המילה האנגלית.

○ לחיצה על "הקשב" — TTS מקריא את המילה (רמז).

○ הקלדה + לחיצת "בדוק":
● נכון: הצגת V ירוק + "+10 XP"
● שגוי: הצגת X אדום + המילה הנכונה

○ לאחר כל המילות — דיאלוג תוצאה.

---

■ כרטיסיות:

○ מוצגת מילה אנגלית בכרטיסייה.

○ Swipe שמאל (x) — "לא ידעתי": הכרטיסייה עפה שמאלה, word.errorInFlashcards=true.

○ Swipe ימין (V) — "ידעתי": הכרטיסייה עפה ימינה, correctAttempts++.

○ לחיצה על הכרטיסייה — Flip: הפיכה לתרגום עברי + משפט דוגמה.

○ לחיצת רמקול — TTS.

---

■ מסך הסריקה:

○ שלב 1 — מצב סריקה:
● בחירת מצב: "ספר" (OCR מדויק למילים בודדות) / "טקסט חופשי" (OCR לשורות)
● מרכז המסך: תצוגה חיה של המצלמה
● לחיצת כפתור שלח — צילום תמונה
● לחיצת גלריה — בחירת תמונה קיימת

○ שלב 2 — מצב סקירה:
● התמונה שצולמה מוצגת עם תיבות מסגרת על כל מילה
● תיבה ירוקה = המילה כבר ב-DB
● תיבה צהובה = מילה חדשה
● לחיצה על תיבה ירוקה — TTS + "הוסף לרשימה האישית?"
● לחיצה על תיבה צהובה — תרגום + דיאלוג הוספה עם שדות מולאו
● "שמור הכל" — שמירת כל המילות + חזרה למצב סריקה

---

■ מסך הסטטיסטיקות:

○ מוצג כרטיס XP: רמה, XP כולל, XP לרמה הבאה, רצף ימים.

○ גרף עמודות ל-7 ימים אחרונים (כמה תשובות נכונות ביום).

○ רשימת "המילות שכדאי לתרגל" — 5 המילות עם Mastery הנמוך ביותר.

---

■ מסך ההגדרות:

○ פרטי חשבון:
● שם המשתמש (לחיצה על עריכה — דיאלוג שינוי שם)
● כתובת אימייל

○ הגדרות לימוד:
● Slider "יעד יומי" — גרירה לבחירת מספר מילות ביום (5-50)
● מתג "הקראה בקול (TTS)" — הפעלה/כיבוי הקראת מילים

○ ניהול נתונים:
● "סנכרן עכשיו" — סנכרון מידי מ-Firestore (ללא המתנה ל-24h)
● "נקה רשימה אישית" — מחיקת כל המילות האישיות (עם אזהרה)
● "אפס התקדמות" — איפוס XP, רמה, רצף, ונתוני תרגול (עם אזהרה כפולה)

○ אודות: גרסת האפליקציה (1.1).

---

■ פרופיל:

○ מוצג: שם המשתמש, XP, רמה, ורצף ימים.

○ הישגים: מדליות על אבני דרך כגון "100 מילים", "500 מילים", "רצף שבוע" וכו'.

○ לחיצת "התנתק" — אישור — Firebase signOut — LoginActivity.

---

■ ממשק המנהל:

○ נגיש רק למשתמש עם הרשאת admin. ניתן לגשת אליו מה-Settings.

○ מוצגת רשימה של כל 1,902 המילות הגלובליות.

○ מחיקת מילה: גרור כרטיס שמאלה — רקע אדום עם אייקון — שחרור — המילה נמחקת מ-Firestore — Snackbar "נמחקה — בטל" (7 שניות לבטל).

○ הוספת מילה: לחיצת FAB — מילוי טופס (מילה*, תרגום*, חלק דיבר, משפטים, תגיות) — "שמור" — המילה נוספת ל-Firestore ולרשימה.

○ חיפוש: הקלדה ב-SearchView — הרשימה מסתננת — גלילה לתוצאה הראשונה.

---

# ~ רפלקציה ~

פרויקט EasyLex היה עבורי חוויה שהשאירה חותם. ניגשתי אליו עם רעיון פשוט — אפליקציה שתחסוך לי זמן בלימוד אנגלית לבגרות — ויצאתי ממנו עם מערכת מלאה ומורכבת שלא ציפיתי שאבנה.

הדבר הראשון שלמדתי הוא שהפלטפורמה של Android היא מורכבת בהרבה ממה שנראה. כשהתחלתי, חשבתי שתוכנה = כתיבת קוד. מהר מאוד גיליתי שרוב הזמן עוסקים בדברים אחרים: ניהול Threads, מחזורי חיים, ניהול זיכרון, ובאגים שמופיעים רק בתנאים ספציפיים. הפעם הראשונה ש-ExecutorService שלי "שבר" את ה-Main Thread — הבנתי שאני צריך לחשוב אחרת.

אחת ההצלחות הגדולות ביותר שלי הייתה מימוש זרימת ה-OCR מקצה לקצה: מצילום תמונה, דרך זיהוי מילים, תרגום, ועד שמירה — כולה ללא הקלדה ידנית. כשזה עבד לראשונה — הרגשתי שכל ההשקעה שווה. עוד הצלחה שגאה בה היא מסך השאלון עם האנימציית ה-XP — הרגשה שהאפליקציה "חיה" ומגיבה.

אתגר שהיה קשה במיוחד היה מחיקת מילות מנהל בזמן אמת. התחלתי עם סנכרון כל 24 שעות, אבל זה לא הרגיש נכון — משתמש שפותח את האפליקציה שעה לאחר מחיקה יראה עדיין את המילה. הפתרון עם addSnapshotListener היה אלגנטי, אבל לקח לי זמן להבין את MetadataChanges.EXCLUDE ומדוע צריך אותו.

אתגר נוסף היה ה-RTL/LTR בכרטיס המנהל. נשמע פשוט — אנגלית שמאל, עברית ימין. בפועל: Android ב-RTL Mode הפך את הכיוון ואנגלית הופיעה מימין. לקח זמן להבין שפתרון layoutDirection="ltr" על ה-LinearLayout הוא הנכון.

אם הייתי מתחיל מחדש, הייתי:
● מתכנן את ה-Schema של Room מראש ומוסיף Migration תקין במקום fallbackToDestructiveMigration.
● מוסיף גיבוי ל-Firestore עבור נתוני תרגול (correctAttempts, errorFlags) — כיום הם אובדים אם המשתמש מחליף מכשיר.
● כותב יותר תיעוד שוטף לקוד — כשחזרתי לקוד שכתבתי חודש קודם, לא תמיד ידעתי מה ולמה.

מה שאני הכי גאה בו הוא שהאפליקציה פועלת כראוי ופותרת בעיה אמיתית. חברים בכיתה ביקשו להשתמש בה לפני הבגרות. זו האינדיקציה הטובה ביותר שמצאתי כיוון נכון.

---

# ~ ביבליוגרפיה ~

## ★ מקורות :

○ תיעוד רשמי:
● Android Developers — https://developer.android.com
● Firebase Documentation — https://firebase.google.com/docs
● ML Kit — https://developers.google.com/ml-kit
● Room Persistence Library — https://developer.android.com/training/data-storage/room
● CameraX — https://developer.android.com/training/camerax
● Navigation Component — https://developer.android.com/guide/navigation

○ מאמרים ומקורות נוספים:
● Stack Overflow — פתרונות לבעיות ספציפיות ב-Threading, OCR Bounding Boxes, ExifInterface
● Medium (@florina.muntenescu) — MVVM Architecture in Android
● Android Developer Blog — Best Practices for Room Database

## ★ כלים :

○ Android Studio Meerkat (2024.3.1) — https://developer.android.com/studio
○ Firebase Console — https://console.firebase.google.com
○ PlantUML — https://plantuml.com
○ draw.io — https://app.diagrams.net

---

# ~ נספחים ~

## נספח א — רשימת קבצים מלאה

### קבצי Java

| קובץ | Package | תפקיד |
|------|---------|--------|
| Word.java | data | Entity + Mastery logic |
| WordDao.java | data | Room queries |
| WordRoomDatabase.java | data | DB Singleton |
| WordRepository.java | data | Repository + Firestore sync |
| WordList.java | data | Firestore model |
| SplashActivity.java | ui.auth | Launcher + version check |
| LoginActivity.java | ui.auth | כניסה |
| RegisterActivity.java | ui.auth | הרשמה |
| MainActivity.java | — | NavHost shell |
| DashboardFragment.java | ui.home | מסך בית |
| MyWordsFragment.java | ui.mywords | מילון |
| MyWordsViewModel.java | ui.mywords | ViewModel + migration + throttle |
| WordListAdapter.java | ui.mywords | RecyclerView Adapter |
| PracticeFragment.java | ui.practice | תפריט תרגול |
| QuizFragment.java | ui.practice | שאלון |
| SpellingFragment.java | ui.practice | איות |
| FlashcardsFragment.java | ui.practice | כרטיסיות |
| PracticeOption.java | ui.practice | Data object |
| ScanFragment.java | ui.scan | OCR + Translate |
| GraphicOverlay.java | ui.scan | Custom View |
| StatisticsFragment.java | ui.statistics | סטטיסטיקות |
| StatisticsViewModel.java | ui.statistics | ViewModel |
| SettingsFragment.java | ui.settings | הגדרות |
| ProfileFragment.java | ui.profile | פרופיל |
| WordListsFragment.java | ui.wordlists | רשימות מילים |
| WordListsViewModel.java | ui.wordlists | ViewModel |
| WordListsAdapter.java | ui.wordlists | Adapter |
| AdminEditFragment.java | ui.admin | עריכת מילים (Admin) |
| AdminWordAdapter.java | ui.admin | Adapter Admin |
| GamificationEngine.java | ui.gamification | Singleton XP/Level/Streak |
| StreakManager.java | ui.gamification | Streak logic |

### קבצי Layout (XML)

| קובץ | תיאור |
|------|--------|
| activity_splash.xml | מסך פתיחה |
| activity_login.xml | כניסה |
| activity_register.xml | הרשמה |
| activity_main.xml | NavHost + BottomNav |
| fragment_dashboard.xml | בית |
| fragment_my_words.xml | מילון |
| fragment_practice.xml | תפריט תרגול |
| fragment_quiz.xml | שאלון |
| fragment_spelling.xml | איות |
| fragment_flashcards.xml | כרטיסיות |
| fragment_scan.xml | סריקה |
| fragment_statistics.xml | סטטיסטיקות |
| fragment_settings.xml | הגדרות |
| fragment_profile.xml | פרופיל |
| fragment_admin_edit.xml | עריכת מנהל |
| list_item_word.xml | פריט מילה ברשימה |
| item_practice_type.xml | כרטיס תרגול |
| item_admin_word_card.xml | כרטיס מנהל |
| dialog_result.xml | דיאלוג תוצאה |
| dialog_add_word.xml | הוספת מילה |
| dialog_admin_add_word.xml | הוספת מילה (מנהל) |
| nav_graph.xml | גרף ניווט |

---

## נספח ב — מילון מושגים טכניים

| מושג | הסבר |
|------|-------|
| MVVM | Model-View-ViewModel — ארכיטקטורת אפליקציה |
| ViewModel | שכבת לוגיקה שורדת סיבובי מסך |
| Repository | Pattern שמסתיר מקורות נתונים שונים |
| Room | ספריית Google שעוטפת SQLite |
| Firestore | NoSQL cloud database של Google |
| Firebase Auth | שירות אימות משתמשים |
| OCR | Optical Character Recognition — זיהוי תווים מתמונה |
| ML Kit | ספריית AI של Google לאנדרואיד |
| CameraX | ספריית מצלמה מודרנית של Jetpack |
| ViewBinding | יצירת קלאסי Java מ-XML (null-safe) |
| Singleton | Pattern המבטיח מופע יחיד של אובייקט |
| ExecutorService | מאגר Threads לפעולות ברקע |
| LiveData | מנגנון Observable עם lifecycle awareness |
| ListenerRegistration | מזהה Firestore Snapshot Listener |
| Snapshot Listener | Firestore real-time listener לשינויים |
| MetadataChanges.EXCLUDE | מסנן — מקבל רק שינויי שרת (לא מקומיים) |
| DiffUtil | כלי Room לעדכון רשימה יעיל |
| ItemTouchHelper | Android helper לגרירה ומחיקה ב-RecyclerView |
| Navigation Component | ניהול ניווט בין Fragments |
| BottomNavigationView | סרגל ניווט תחתון |
| SharedPreferences | אחסון מקומי key-value |
| TTS | Text-To-Speech — הקראת טקסט בקול |
| Mastery | רמת שליטה במילה (0-5) |
| Gamification | שילוב מנגנוני משחק (XP, רמות, רצף) בלמידה |
| ExifInterface | קריאת מטא-דאטה מתמונות (כיוון צילום) |
| Double-Checked Locking | Pattern ליצירת Singleton Thread-safe |
| fallbackToDestructiveMigration | מדיניות Migration — מחק והרץ מחדש |
| Bounding Box | תיבת המסגרת של מילה מזוהה ב-OCR |
| Daily Seed | זרע אקראי יומי — אותו ערבוב לכל השאלונים של יום |

---

## נספח ג — נוסחאות מפתח

### נוסחת Mastery (רמת שליטה במילה)
```
M = min(5, floor(correctAttempts / (totalAttempts + 1) × 5))
```
דוגמה: 4 נכון מתוך 5 ניסיונות — floor(4/6 × 5) = floor(3.33) = 3

### נוסחת רמה (Level)
```
L = floor(sqrt(XP / 100)) + 1
XP לתחילת רמה L: (L-1)² × 100
```

| רמה | XP נדרש לתחילה | XP עד סיום הרמה |
|-----|----------------|-----------------|
| 1 | 0 XP | 100 XP |
| 2 | 100 XP | 300 XP |
| 3 | 400 XP | 500 XP |
| 4 | 900 XP | 700 XP |
| 5 | 1,600 XP | 900 XP |

### אלגוריתם 60/20/20 (בחירת שאלות אדפטיבית)
```
Pool מסונן לפי mode:
    hard  (Mastery 0-2) : 60% מ-N  (12 מתוך 20)
    new   (0 ניסיונות) : 20% מ-N  ( 4 מתוך 20)
    solid (Mastery 3-5) : 20% מ-N  ( 4 מתוך 20)
אם קבוצה קטנה — ממלאים מהנותרים
shuffle סופי
```

### Daily Seed (אותן שאלות לכל היום)
```java
long seed = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
Collections.shuffle(pool, new Random(seed));
// אותה תוצאה לכל אורך היום
```

---

*ספר פרויקט זה נכתב לצורכי הגשה לבגרות במסלול הנדסת תוכנה וסייבר,*
*ישיבת הרב עמיאל — הישוב החדש, תשפ"ה 2026.*
