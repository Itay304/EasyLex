# EasyLex — תרשימי UML

## תרשימי מחלקות (Class Diagrams)

| קובץ | תוכן | A4 |
|------|------|----|
| `01_data_layer.puml` | Word, WordDao, WordRoomDatabase, WordRepository | ✓ |
| `02_auth_navigation.puml` | SplashActivity, LoginActivity, RegisterActivity, MainActivity | ✓ |
| `03_mywords_viewmodel.puml` | MyWordsFragment, MyWordsViewModel, Repository, Room, Firestore | ✓ |
| `04_practice_modules.puml` | PracticeFragment, QuizFragment, SpellingFragment, FlashcardsFragment | ✓ |
| `05_scan_gamification.puml` | ScanFragment, GraphicOverlay, GamificationEngine, StreakManager | ✓ |
| `06_admin_stats_settings.puml` | AdminEditFragment, AdminWordAdapter, StatisticsFragment, SettingsFragment | ✓ |

## תרשימי רצף (Sequence Diagrams)

| קובץ | תוכן | A4 |
|------|------|----|
| `07_seq_sync.puml` | סנכרון Firestore — Throttle 24h + מחיקה דו-כיוונית | ✓ |
| `08_seq_realtime_delete.puml` | מחיקת מנהל → Listener → Room → UI בזמן אמת | ✓ |
| `09_seq_quiz.puml` | חידון: 60/20/20 + XP + GamificationEngine | ✓ |
| `10_seq_ocr.puml` | OCR: CameraX → ExifInterface → ML Kit → Translate → Room | ✓ |
| `11_seq_auth.puml` | אימות: Splash → Firebase → Login/Register → MainActivity | ✓ |

## איך לרנדר ל-PNG

### אפשרות 1 — Android Studio Plugin (מומלץ)
`File → Settings → Plugins → חפש "PlantUML Integration"` → Install → Restart
פתח קובץ `.puml` → לשונית Preview מימין.

### אפשרות 2 — אתר אינטרנט (ללא התקנה)
העתק את תוכן הקובץ לאתר:
**https://www.plantuml.com/plantuml/uml/**

### אפשרות 3 — שורת פקודה
```bash
java -jar plantuml.jar uml/*.puml
```
יוצר PNG לכל קובץ בתיקיית `uml/`.
