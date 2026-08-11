# EasyLex — רשימת כל הפונקציות בפרויקט

> **סה"כ: ~235 פונקציות | 33 קבצים | עדכון: 2026-06-18**

---

## data/Word.java
`app/src/main/java/com/example/easylex/data/Word.java`

| פונקציה | תיאור |
|---|---|
| `Word()` | קונסטרוקטור ריק — נדרש ע"י Room לדה-סריאליזציה |
| `Word(String english, String hebrew, String pos, String exEn, String exHe, int diff, long ts)` | קונסטרוקטור מלא להוספת מילה חדשה |
| `int getId()` | מחזיר את ה-ID הייחודי האוטומטי (PrimaryKey) |
| `void setId(int id)` | מגדיר את ה-ID |
| `String getEnglishWord()` | מחזיר את המילה האנגלית — משמש כמזהה לוגי ב-Firestore |
| `void setEnglishWord(String)` | מגדיר את המילה האנגלית |
| `String getHebrewTranslation()` | מחזיר את התרגום העברי |
| `void setHebrewTranslation(String)` | מגדיר את התרגום העברי |
| `String getPartOfSpeech()` | מחזיר חלק הדיבור (noun, verb, adj...) |
| `void setPartOfSpeech(String)` | מגדיר חלק הדיבור |
| `String getExampleSentence()` | מחזיר משפט דוגמה באנגלית |
| `void setExampleSentence(String)` | מגדיר משפט דוגמה באנגלית |
| `String getHebrewExample()` | מחזיר תרגום משפט הדוגמה לעברית |
| `void setHebrewExample(String)` | מגדיר תרגום משפט הדוגמה לעברית |
| `String getTags()` | מחזיר תגיות מופרדות בפסיק |
| `void setTags(String)` | מגדיר תגיות |
| `long getCreationTimestamp()` | מחזיר חותמת זמן יצירה (ms) |
| `void setCreationTimestamp(long)` | מגדיר חותמת זמן יצירה |
| `int getCorrectAttempts()` | מחזיר מספר תשובות נכונות בחידון |
| `void setCorrectAttempts(int)` | מגדיר מספר תשובות נכונות |
| `int getTotalAttempts()` | מחזיר סה"כ ניסיונות בחידון |
| `void setTotalAttempts(int)` | מגדיר סה"כ ניסיונות |
| `int getSpellingCorrect()` | מחזיר מספר איותים נכונים |
| `void setSpellingCorrect(int)` | מגדיר מספר איותים נכונים |
| `boolean isFavorite()` | האם המילה אישית (הוספה ידנית/OCR) |
| `void setFavorite(boolean)` | מגדיר דגל "אישי/מועדף" |
| `boolean isVerified()` | האם המילה גלובלית מאומתת (מ-Firestore) |
| `void setVerified(boolean)` | מגדיר דגל אימות גלובלי |
| `boolean isErrorInQuiz()` | האם נטעה בחידון האחרון |
| `void setErrorInQuiz(boolean)` | מגדיר דגל שגיאה בחידון |
| `boolean isErrorInSpelling()` | האם נטעה באיות |
| `void setErrorInSpelling(boolean)` | מגדיר דגל שגיאה באיות |
| `boolean isErrorInFlashcards()` | האם סומנה "לא יודע" בפלאשקארד |
| `void setErrorInFlashcards(boolean)` | מגדיר דגל שגיאה בפלאשקארד |
| `int getMasteryLevel()` | מחשב רמת שליטה 0–5: `min(5, floor(correct/(total+1)×5))` |

---

## data/WordDao.java
`app/src/main/java/com/example/easylex/data/WordDao.java`

| פונקציה | תיאור |
|---|---|
| `void insert(Word)` | מוסיף מילה; conflict = IGNORE |
| `void update(Word)` | מעדכן שורה לפי ID |
| `void delete(Word)` | מוחק שורה לפי ID |
| `LiveData<List<Word>> getAllWords()` | כל המילים ממויינות לפי חותמת יצירה |
| `LiveData<List<Word>> getPersonalWords()` | מילים אישיות בלבד (isVerified=false) |
| `List<String> getVerifiedEnglishWords()` | רשימת englishWord של מילים גלובליות — לחישוב diff |
| `void deleteUnmarkedGlobalWords()` | מוחק גלובליות שאינן מסומנות כמועדפות |
| `void deletePersonalWords()` | מוחק את כל המילים האישיות |
| `void deleteVerifiedWordByEnglish(String)` | מוחק מילה גלובלית ספציפית — מ-Firestore listener |
| `void deleteVerifiedWords()` | מוחק את כל המילים הגלובליות |
| `void deleteAllWords()` | מוחק הכל — איפוס מלא |
| `void resetAllWordProgress()` | מאפס correctAttempts, totalAttempts, spellingCorrect וכל דגלי שגיאה |

---

## data/WordRoomDatabase.java
`app/src/main/java/com/example/easylex/data/WordRoomDatabase.java`

| פונקציה | תיאור |
|---|---|
| `abstract WordDao wordDao()` | Room מייצר את המימוש — מחזיר ה-DAO |
| `static WordRoomDatabase getDatabase(Context)` | Singleton עם Double-Checked Locking |

---

## data/WordRepository.java
`app/src/main/java/com/example/easylex/data/WordRepository.java`

| פונקציה | תיאור |
|---|---|
| `WordRepository(Application)` | מאתחל Room, DAO, ExecutorService (4 threads), LiveData |
| `LiveData<List<Word>> getAllWords()` | מחזיר LiveData לצפייה מה-UI |
| `LiveData<List<Word>> getPersonalWords()` | מחזיר LiveData של מילים אישיות |
| `void insert(Word)` | insert על background thread |
| `void update(Word)` | update על background thread |
| `void delete(Word)` | delete על background thread |
| `void deleteAll()` | מוחק הכל על background thread |
| `void deleteUnmarkedGlobalWords()` | מוחק גלובליות לא-מסומנות |
| `void deletePersonalWords()` | מוחק מילים אישיות |
| `void resetAllWordProgress(Runnable)` | מאפס התקדמות; קורא callback בסיום |
| `void syncGlobalWordsFromFirestore(Runnable)` | שולף Firestore, מחשב diff, מוסיף/מוחק, callback בסיום |
| `void startGlobalDeleteListener()` | Firestore listener (MetadataChanges.EXCLUDE) למחיקות |
| `void stopGlobalDeleteListener()` | מנתק listener — למניעת memory leak |
| `void replaceAllWords(List<Word>)` | מוחק הכל ומכניס רשימה חדשה |

---

## data/WordList.java
`app/src/main/java/com/example/easylex/data/WordList.java`

| פונקציה | תיאור |
|---|---|
| `WordList()` | קונסטרוקטור ריק — לדה-סריאליזציה מ-Firestore |
| `WordList(String id, String name, int wordCount)` | קונסטרוקטור מלא |
| `String getId()` | מחזיר ID של הרשימה |
| `String getName()` | מחזיר שם הרשימה |
| `int getWordCount()` | מחזיר מספר מילים ברשימה |

---

## MainActivity.java
`app/src/main/java/com/example/easylex/MainActivity.java`

| פונקציה | תיאור |
|---|---|
| `void onCreate(Bundle)` | ViewBinding, NavController, BottomNavigationView (5 טאבים) |
| `boolean onSupportNavigateUp()` | ניווט אחורה דרך NavController |

---

## ui/auth/SplashActivity.java
`app/src/main/java/com/example/easylex/ui/auth/SplashActivity.java`

| פונקציה | תיאור |
|---|---|
| `void onCreate(Bundle)` | מפעיל אנימציה ובדיקת auth במקביל |
| `void startSplashAnimation()` | scale + fade על לוגו; בסיום: animDone=true → tryProceed() |
| `void checkAppVersion()` | בודק מ-Firestore אם יש גרסת חובה חדשה |
| `void resolveAuth()` | FirebaseAuth.getCurrentUser(); authReady=true → tryProceed() |
| `void tryProceed()` | מנווט רק אם animDone AND authReady — מונע race condition |
| `void showForceUpdateDialog(String url)` | AlertDialog לא-בטיל לעדכון חובה |

---

## ui/auth/LoginActivity.java
`app/src/main/java/com/example/easylex/ui/auth/LoginActivity.java`

| פונקציה | תיאור |
|---|---|
| `void onCreate(Bundle)` | אתחול UI: email/password + כפתורי כניסה/הרשמה |
| `void loginUser()` | signInWithEmailAndPassword; הצלחה → startActivity(Main) + finish() |

---

## ui/auth/RegisterActivity.java
`app/src/main/java/com/example/easylex/ui/auth/RegisterActivity.java`

| פונקציה | תיאור |
|---|---|
| `void onCreate(Bundle)` | אתחול UI: שדות email/password |
| `void registerUser()` | createUserWithEmailAndPassword; הצלחה → startActivity(Main) + finishAffinity() |

---

## ui/home/DashboardFragment.java
`app/src/main/java/com/example/easylex/ui/home/DashboardFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | מנפח layout, click listeners, מתחיל צפייה ב-LiveData |
| `void onResume()` | מסנכרן XP/Streak מ-Firestore ומרענן סטטיסטיקות |
| `void onDestroyView()` | מאפס binding |
| `void onWordsLoaded(List<Word>)` | callback LiveData — מעדכן את כל חלקי ה-Dashboard |
| `void setupWordOfDay(List<Word>)` | מילה יומית לפי `DAY_OF_YEAR % size` |
| `void setupDailyChallenges(List<Word>)` | progress bars של dailyCorrect/dailyGoal לכל מודול |
| `void setupWeeklyActivity()` | גרף עמודות ידני ל-7 ימים (ללא ספרייה) |
| `String[] buildDayLabels()` | שמות ימים עבריים מקוצרים — 7 ימים אחורה |
| `void setupMistakesRow(List<Word>)` | RecyclerView אופקי של מילות שגיאה (errorInQuiz=true) |
| `void navigateToQuiz(String type)` | ניווט ל-QuizFragment עם type argument |
| `void setupGreeting()` | ברכה לפי שעה: בוקר / צהריים / ערב |
| `void refreshStats()` | מעדכן XP, Level, Streak, challenge progress |
| `String getDailyQuote()` | ציטוט יומי לפי DAY_OF_YEAR |
| `int dpToPx(int dp)` | המרת dp לפיקסלים לבנייה ידנית של גרף |

---

## ui/mywords/MyWordsFragment.java
`app/src/main/java/com/example/easylex/ui/mywords/MyWordsFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | RecyclerView, אינדקס צדדי, חיפוש, FAB, TTS |
| `void setupFilterChips(View)` | Chips לסינון "כל המילים" / "המילים שלי" |
| `void applyPersonalFilter()` | מחליף בין כל/אישיות ומפעיל פילטרים |
| `void setupFab(View)` | FAB (+) לפתיחת dialog הוספת מילה |
| `void showAddWordDialog()` | AlertDialog: EN, HE, POS, example, tags + תיקוף |
| `void setupViewModel()` | ViewModel observer — מעדכן adapter ו-dropdowns |
| `void setupDropdowns()` | AutoCompleteTextView לחלקי דיבור ותגיות |
| `void setupSideIndex()` | עמודת A-Z עם onTouch לגלילה מהירה |
| `void highlightLetter(String)` | מדגיש אות נבחרת באינדקס |
| `void scrollToLetter(String)` | scroll לפוזיציה הראשונה של האות |
| `void showBubble(String, float, View)` | PopupWindow עם האות ליד האצבע |
| `void createBubblePopup()` | יוצר PopupWindow (bubble_layout.xml) |
| `void setupRecyclerView(View)` | RecyclerView + scroll listener להסתרת FAB |
| `void setupSearch(View)` | TextWatcher לסינון חי |
| `void initializeTts()` | TextToSpeech, locale=Locale.US |
| `void onPronounceClick(String)` | מנגן הגייה דרך TTS |
| `void onDestroyView()` | ניקוי TTS, PopupWindow, binding |

---

## ui/mywords/MyWordsViewModel.java
`app/src/main/java/com/example/easylex/ui/mywords/MyWordsViewModel.java`

| פונקציה | תיאור |
|---|---|
| `MyWordsViewModel(Application)` | Repository + migration חד-פעמי + listener + sync throttle 24h |
| `LiveData<List<Word>> getAllWords()` | LiveData של כל המילים |
| `LiveData<List<Word>> getPersonalWords()` | LiveData של מילים אישיות |
| `void insert(Word)` | מעביר ל-Repository |
| `void update(Word)` | מעביר ל-Repository |
| `void deletePersonalWords()` | מוחק מילים אישיות |
| `void syncFromCloud()` | מאלץ סנכרון מיידי (ללא throttle) |
| `void onCleared()` | stopGlobalDeleteListener() — מונע memory leak |

---

## ui/mywords/WordListAdapter.java
`app/src/main/java/com/example/easylex/ui/mywords/WordListAdapter.java`

| פונקציה | תיאור |
|---|---|
| `void setOnPronounceClickListener(listener)` | callback לכפתור הגייה |
| `void setWords(List<Word>)` | מעדכן רשימה ומפעיל פילטרים |
| `void filterByText(String)` | פילטר חיפוש טקסטואלי |
| `void filterByPOSAndTag(String, String)` | פילטר POS + תגית |
| `void applyCurrentFilters()` | מפעיל את כל הפילטרים יחד |
| `Filter getFilter()` | Filter לסינון מרובה-קריטריונים |
| `int getPositionForLetter(String)` | מיקום ראשון של אות — לאינדקס צדדי |
| `String getLetterForPosition(int)` | אות ראשונה של מילה במיקום נתון |
| `int getItemCount()` | מספר מילים לאחר סינון |
| `WordViewHolder onCreateViewHolder(...)` | יוצר ViewHolder מ-list_item_word.xml |
| `void onBindViewHolder(...)` | קושר מילה + SpannableString לסימון חיפוש |
| `WordViewHolder(View)` | קונסטרוקטור ViewHolder |
| `void WordViewHolder.bind(Word, listener)` | קושר מילה עם פורמט + הגייה |

---

## ui/practice/PracticeFragment.java
`app/src/main/java/com/example/easylex/ui/practice/PracticeFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | תפריט 6 מצבי תרגול + XP Header |
| `void onResume()` | sync gamification + עדכון XP header |
| `void updateXpHeader(GamificationEngine)` | רמה, progress bar תוך-רמה, streak |
| `void updateMenu()` | מחשב badges לכל כרטיס |
| `String getDailyProgress(int goal)` | "X/Goal" של תשובות היום |
| `String getMistakeCount()` | ספירת מילים עם דגלי שגיאה |
| `String getSpellingCount()` | התקדמות איות "X/Y" |
| `String getPersonalCount()` | מספר מילים אישיות |
| `String getFlashCount()` | התקדמות פלאשקארד "X/Y" |
| `void handleNav(View, PracticeOption)` | ניווט לפרגמנט המתאים |
| `void showCategorySelection(View)` | dialog בחירת קטגוריה (POS) |

---

## ui/practice/QuizFragment.java
`app/src/main/java/com/example/easylex/ui/practice/QuizFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | UI חידון: שאלה + 4 כפתורי תשובה |
| `void prepareQuiz(String type, String category)` | בחירת 20 מילים אדפטיבית: 60% Mastery 0-2, 20% חדשות, 20% Mastery 3-5 |
| `void addUpTo(List, List, int)` | static helper: מוסיף עד max פריטים |
| `void onDestroyView()` | ניקוי Handler + TTS |
| `void showNextQuestion()` | מציג שאלה + 3 distractors מגורלים; TTS |
| `void checkAnswer(MaterialButton, Word)` | בודק, צובע ירוק/אדום, מעדכן DB, XP, אנימציה |
| `void showXpAnimation(String)` | "+X XP" עולה ונעלם |
| `void finishQuiz()` | onModuleComplete() +50 XP, שומר ציון, dialog תוצאות |
| `void showResultDialog(int, int)` | dialog עם ציון + OvershootInterpolator animation |
| `void speakCurrentWord()` | TTS להגיית המילה הנוכחית |

---

## ui/practice/SpellingFragment.java
`app/src/main/java/com/example/easylex/ui/practice/SpellingFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | UI איות: prompt עברי + שדה קלט אנגלי (SESSION=15) |
| `void onDestroyView()` | ניקוי Handler + TTS |
| `List<Word> selectAdaptiveWords(List<Word>, int)` | 60/20/20 אדפטיבי: קשות/חדשות/מוצקות |
| `void addUpTo(List, List, int)` | static helper |
| `void showWord()` | prompt עברי + TTS אחרי 500ms |
| `void check()` | השוואה case-insensitive; currentAttempted מונע ספירה כפולה |
| `void finish()` | dialog סיכום + ניקוי דגלי שגיאה |
| `void speak()` | בשאלה=עברית, בתשובה=אנגלית |

---

## ui/practice/FlashcardsFragment.java
`app/src/main/java/com/example/easylex/ui/practice/FlashcardsFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | UI פלאשקארד: swipe/tap + TTS |
| `List<Word> orderByPriority(List<Word>)` | Mastery 0-2 → חדשות → Mastery 3-5, כל קבוצה מעורבבת |
| `void showWord()` | מציג כרטיס או dialog סיכום |
| `void advanceCard()` | אנימציית יציאה + כרטיס הבא |
| `void handleSwipe(boolean known)` | ימינה=יודע (+XP) | שמאלה=לא יודע (errorInFlashcards=true) |
| `void resetCard()` | מחזיר כרטיס למיקום התחלתי |
| `void flipCard()` | rotationY 0→90° (flip1), החלפת layout HE↔EN, 90°→0° (flip2) |
| `void speakCurrentWord()` | TTS |
| `void showFlashResult()` | dialog: ידוע/לא ידוע + חזרה |
| `void onDestroyView()` | ניקוי TTS |

---

## ui/practice/QuizViewModel.java
`app/src/main/java/com/example/easylex/ui/practice/QuizViewModel.java`

| פונקציה | תיאור |
|---|---|
| `QuizViewModel(Application)` | Repository + LiveData |
| `LiveData<QuizQuestion> getCurrentQuestion()` | השאלה הנוכחית |
| `void startQuiz()` | מאתחל חידון עם מילים מעורבבות |
| `void generateNextQuestion()` | מייצר שאלה + 4 אפשרויות |
| `void moveToNextQuestion()` | מקדם אינדקס |
| `QuizQuestion(String, List, String, int, int)` | קונסטרוקטור data class שאלת חידון |

---

## ui/practice/FlashcardsViewModel.java
`app/src/main/java/com/example/easylex/ui/practice/FlashcardsViewModel.java`

| פונקציה | תיאור |
|---|---|
| `FlashcardsViewModel(Application)` | Repository + LiveData |
| `LiveData<List<Word>> getAllWords()` | כל המילים |

---

## ui/practice/PracticeOption.java
`app/src/main/java/com/example/easylex/ui/practice/PracticeOption.java`

| פונקציה | תיאור |
|---|---|
| `PracticeOption(String title, String desc, int icon, int color, String type, String badge)` | קונסטרוקטור של כרטיס תרגול |
| `String getTitle()` | כותרת האפשרות |
| `String getDescription()` | תיאור האפשרות |
| `int getIconRes()` | resource ID אייקון |
| `int getColor()` | צבע רקע הכרטיס |
| `String getType()` | סוג (DAILY, CATEGORY, MISTAKES...) |
| `String getBadgeText()` | טקסט badge (ספירה/התקדמות) |

---

## ui/gamification/GamificationEngine.java
`app/src/main/java/com/example/easylex/ui/gamification/GamificationEngine.java`

| פונקציה | תיאור |
|---|---|
| `GamificationEngine(Context)` | קונסטרוקטור פרטי — Singleton; SharedPrefs + Firestore |
| `static getInstance(Context)` | Double-Checked Locking Singleton |
| `void onCorrectAnswer(Word before, Word after)` | +10 XP; אם Mastery עלה ל-5: +100 XP בונוס |
| `void onModuleComplete()` | +50 XP + recordDailyActivity() |
| `int getTotalXp()` | XP כולל מ-SharedPrefs |
| `int getCurrentLevel()` | רמה נוכחית מ-SharedPrefs |
| `int getStreak()` | streak יומי |
| `int getDailyCorrectCount()` | תשובות נכונות של היום |
| `int[] getWeeklyActivity()` | int[7] תשובות ל-7 ימים — לגרף |
| `int getXpForLevelStart(int level)` | XP בתחילת רמה L: `(L-1)² × 100` |
| `int getXpForNextLevel()` | XP לרמה הבאה: `L² × 100` |
| `int getXpWithinLevel()` | XP בתוך הרמה — לחישוב progress bar |
| `int getXpRangeOfLevel()` | טווח XP של הרמה (max progress bar) |
| `void syncFromFirestore(Runnable)` | טוען XP/Level/Streak מ-Firestore → SharedPrefs |
| `void addXp(int)` | מוסיף XP, מחשב רמה, שומר, persistToFirestore() |
| `int computeLevel(int xp)` | `floor(sqrt(XP/100)) + 1` |
| `void checkStreak()` | מאציל ל-StreakManager.checkAndUpdate() |
| `void persistToFirestore(int xp, int level)` | כותב ל-`users/{uid}` ב-Firestore |
| `void recordDailyActivity()` | מגדיל KEY_DAILY_CORRECT_{date} ב-SharedPrefs |
| `String todayString()` | תאריך היום "yyyy-MM-dd" |

---

## ui/gamification/StreakManager.java
`app/src/main/java/com/example/easylex/ui/gamification/StreakManager.java`

| פונקציה | תיאור |
|---|---|
| `void checkAndUpdate(String uid, int streak, Firestore, callback)` | קורא lastActiveDate, מחשב, כותב ל-Firestore, callback |
| `int computeStreak(String lastDate, int current, String today)` | אין תאריך=1 \| יום רצוף=current+1 \| פער>1=1 (reset) |
| `long daysDiff(String dateA, String dateB)` | הפרש ימים בין שני strings "yyyy-MM-dd" |
| `void StreakCallback.onResult(int newStreak)` | ממשק callback לערך streak מעודכן |

---

## ui/scan/ScanFragment.java
`app/src/main/java/com/example/easylex/ui/scan/ScanFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | מצלמה/גלריה + OCR + toggle ספר/טקסט חופשי |
| `void onDestroyView()` | ניקוי camera executor, OCR, TTS |
| `void showCaptureMode()` | UI מצלמה חיה |
| `void showReviewMode()` | UI תוצאות + GraphicOverlay |
| `void setupOverlayClick()` | touch listener → handleWordClick |
| `void handleWordClick(WordBox)` | exists=true → TTS \| false → dialog הוספה |
| `void showAddDialogWithTranslation(String, String)` | ML Kit Translate → showAddDialog |
| `void showAddDialog(String, String, String)` | dialog: EN, HE, POS (pre-filled) |
| `void checkCameraPermission()` | בדיקת/בקשת הרשאת CAMERA |
| `void startCamera()` | CameraX: Preview + ImageCapture |
| `void takePhoto()` | ImageCapture.takePicture() → קובץ זמני |
| `void recognizeText(InputImage)` | ML Kit OCR → WordBoxes → GraphicOverlay |
| `boolean blockHasBulletLine(TextBlock)` | מסנן blocks עם bullet characters |
| `List<WordBox> extractWordBoxes(Text)` | מצב ספר: Element level — מילה בודדת |
| `List<WordBox> extractFreeTextBoxes(Text)` | מצב חופשי: Line level — שורה שלמה |
| `Bitmap loadRotatedBitmap(File)` | EXIF orientation → Matrix.postRotate() |
| `Rect extractTargetBounds(Line, String)` | Bounding Box של phrase בתוך Line |

---

## ui/scan/GraphicOverlay.java
`app/src/main/java/com/example/easylex/ui/scan/GraphicOverlay.java`

| פונקציה | תיאור |
|---|---|
| `GraphicOverlay(Context, AttributeSet)` | אתחול Paint + anti-aliasing |
| `void setImageSourceInfo(int w, int h)` | מימדי תמונה לחישוב scale |
| `void setWordBoxes(List<WordBox>)` | מעדכן מסגרות + invalidate() |
| `List<WordBox> getWordBoxes()` | מחזיר רשימת WordBox |
| `RectF getScaledRect(Rect)` | קוא' תמונה → קוא' מסך |
| `void onDraw(Canvas)` | ירוק (exists) / כחול (חדש) לכל WordBox |
| `WordBox(Rect, boolean, String, String)` | inner class: rect, existsInDb, text, pos |

---

## ui/admin/AdminEditFragment.java
`app/src/main/java/com/example/easylex/ui/admin/AdminEditFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | RecyclerView + swipe-delete + חיפוש + FAB |
| `void loadWords()` | שולף collection "words" מ-Firestore |
| `void setupSwipeToDelete()` | ItemTouchHelper: אדום + פח → מחיקה + Snackbar "בטל" |
| `void setupSearch()` | TextWatcher → scrollToPosition |
| `void setupFab()` | FAB לפתיחת dialog הוספה |
| `void showAddDialog()` | טופס: EN*, HE*, POS, exEN, exHE, tags (* = חובה) |
| `void addToFirestore(String...)` | מכניס מסמך ל-Firestore + אדפטר אופטימיסטי |
| `String text(TextInputEditText)` | static: trim + null-safe |
| `Map<String,Object> wordToMap(Word)` | static: Word → Firestore map |

---

## ui/admin/AdminWordAdapter.java
`app/src/main/java/com/example/easylex/ui/admin/AdminWordAdapter.java`

| פונקציה | תיאור |
|---|---|
| `void setData(List<Word>, List<String>)` | מעדכן מילים + docIds |
| `Word getWord(int)` | מילה לפי מיקום — ל-ItemTouchHelper |
| `String getDocId(int)` | Firestore docId לפי מיקום |
| `void removeItem(int)` | הסרה מיידית מה-UI (Optimistic) |
| `void insertItem(int, Word, String)` | החזרה לפוזיציה — ל-undo |
| `AdminViewHolder onCreateViewHolder(...)` | item_admin_word_card.xml |
| `void onBindViewHolder(...)` | EN \| HE, POS badge, דוגמאות |
| `int getItemCount()` | מספר מילים |
| `String nullToEmpty(String)` | static: null → "" |
| `AdminViewHolder(View)` | קונסטרוקטור ViewHolder |

---

## ui/statistics/StatisticsFragment.java
`app/src/main/java/com/example/easylex/ui/statistics/StatisticsFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | ViewBinding + LiveData observer |
| `void onResume()` | sync Firestore + refresh |
| `void onDestroyView()` | מאפס binding |
| `void updateGamificationUi()` | XP, Level, Streak, progress bar |
| `void calculateMastery(List<Word>)` | אחוז שליטה + bar chart לפי Mastery 0-5 |
| `void updateRank(float pct, int mastered)` | <5%=🌱 \| 5-19%=📖 \| 20-39%=⭐ \| 40-59%=🎖 \| 60-79%=👑 \| 80-94%=🏆 \| ≥95%=💎 |

---

## ui/statistics/StatisticsViewModel.java
`app/src/main/java/com/example/easylex/ui/statistics/StatisticsViewModel.java`

| פונקציה | תיאור |
|---|---|
| `StatisticsViewModel(Application)` | Repository + LiveData מחושב |
| `LiveData<Integer> getTotalWordsCount()` | מספר מילים כולל |
| `LiveData<Integer> getSuccessRate()` | אחוז הצלחה כולל |
| `LiveData<List<Word>> getDifficultWords()` | 5 המילים הקשות ביותר |
| `void calculateStatistics(List<Word>)` | מחשב ומעדכן כל LiveDatas |

---

## ui/settings/SettingsFragment.java
`app/src/main/java/com/example/easylex/ui/settings/SettingsFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | טולבר, חשבון, למידה, ניהול נתונים |
| `void onDestroyView()` | מאפס binding |
| `void setupToolbar()` | כפתור Back → Activity.onBackPressed() |
| `void loadAccountInfo()` | שם + email מ-Firebase Auth |
| `void showEditNameDialog()` | AlertDialog + EditText לשינוי שם |
| `void updateDisplayName(String)` | Firestore `users/{uid}` + FirebaseAuth.updateProfile() |
| `void loadLearningSettings()` | Slider (1-50 יעד יומי) + Switch (TTS) מ-SharedPrefs |
| `void updateDailyGoalChip(int)` | מעדכן Chip טקסט לפי Slider |
| `void setupDataActions()` | כפתורי sync, מחיקת אישיות, איפוס |
| `void resetProgress()` | מאפס XP/Level/Streak + נתוני מילים |
| `void setupAbout()` | גרסת אפליקציה מ-BuildConfig |

---

## ui/profile/ProfileFragment.java
`app/src/main/java/com/example/easylex/ui/profile/ProfileFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | תמונה (Glide), שם, email, כפתורים |
| `void checkIfAdmin(String uid)` | בודק Firestore → מציג כפתור admin |

---

## ui/wordlists/WordListsFragment.java
`app/src/main/java/com/example/easylex/ui/wordlists/WordListsFragment.java`

| פונקציה | תיאור |
|---|---|
| `View onCreateView(...)` | RecyclerView + ViewModel observer |
| `void onDownloadClick(WordList)` | callback הורדה → downloadWordList() |
| `void observeViewModel()` | מצפה לרשימות, Toast, מצב טעינה |

---

## ui/wordlists/WordListsViewModel.java
`app/src/main/java/com/example/easylex/ui/wordlists/WordListsViewModel.java`

| פונקציה | תיאור |
|---|---|
| `WordListsViewModel(Application)` | Firestore (offline persistence) + fetchWordLists() |
| `LiveData<List<WordList>> getWordLists()` | רשימות מילים |
| `LiveData<Boolean> getIsLoading()` | מצב טעינה |
| `LiveData<String> getToastMessage()` | הודעות Toast |
| `void fetchWordLists()` | שולף רשימות מ-Firestore |
| `void downloadWordList(WordList)` | מוריד מילים מ-Firestore → מכניס ל-Room |

---

## ui/wordlists/WordListsAdapter.java
`app/src/main/java/com/example/easylex/ui/wordlists/WordListsAdapter.java`

| פונקציה | תיאור |
|---|---|
| `void setOnDownloadClickListener(listener)` | callback לכפתור הורדה |
| `WordListViewHolder onCreateViewHolder(...)` | list_item_wordlist.xml |
| `void onBindViewHolder(...)` | קושר נתוני רשימה |
| `int getItemCount()` | מספר רשימות |
| `void setWordLists(List<WordList>)` | מעדכן רשימה |
| `WordListViewHolder(View)` | קונסטרוקטור ViewHolder |
| `void WordListViewHolder.bind(WordList, listener)` | קושר + click handler הורדה |
| `void OnDownloadClickListener.onDownloadClick(WordList)` | ממשק callback — מימוש ב-Fragment |

---

*סה"כ: ~235 פונקציות ב-33 קבצי מקור | EasyLex v1.1*
