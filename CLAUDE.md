# EasyLex — Claude Code Conventions

This file defines the architecture and coding standards for the EasyLex project.
Claude must follow these rules strictly in every response.

---

## Project Identity

- **App Name:** EasyLex
- **Application ID:** `com.example.easylex`
- **Version:** 1.1 (versionCode 3)
- **Primary Language:** **Java** (NOT Kotlin — do not suggest or write Kotlin)
- **UI Paradigm:** **XML layouts with ViewBinding** (NOT Jetpack Compose)
- **Module Structure:** Single-module (`:app` only)

---

## SDK & Build Targets

| Setting         | Value              |
|-----------------|--------------------|
| compileSdk      | 34 (Android 14)    |
| targetSdk       | 34                 |
| minSdk          | 24 (Android 7.0)   |
| AGP             | 8.13.0             |
| Gradle Wrapper  | 8.13               |
| Java Source/Target | Java 17         |

---

## Architecture

**Pattern: MVVM + Repository**

```
Activity/Fragment (View)
    ↓ observes LiveData
ViewModel (AndroidViewModel)
    ↓ calls
Repository
    ↓ reads/writes
Room DAO (local) + Firestore (cloud)
```

- Fragments use `ViewModelProvider` (no Hilt/DI framework)
- Repository is the single source of truth
- Room DB ↔ Firestore two-way sync
- Database operations run on `ExecutorService` (4-thread pool)
- **No Hilt, Dagger, Koin, or any DI framework**
- **No Retrofit** — Firebase is the sole backend

---

## Package Structure

```
com.example.easylex/
├── data/
│   ├── Word.java            (Room @Entity)
│   ├── WordDao.java         (Room @Dao)
│   ├── WordList.java        (Firestore model)
│   ├── WordRepository.java  (Repository)
│   └── WordRoomDatabase.java
└── ui/
    ├── admin/               (AdminEditFragment, AdminWordAdapter)
    ├── auth/                (SplashActivity, LoginActivity, RegisterActivity)
    ├── mywords/             (MyWordsFragment, MyWordsViewModel, WordListAdapter)
    ├── practice/            (FlashcardsFragment, PracticeFragment, QuizFragment, SpellingFragment + ViewModels)
    ├── profile/             (ProfileFragment)
    ├── scan/                (ScanFragment, GraphicOverlay)
    ├── statistics/          (StatisticsFragment, StatisticsViewModel)
    └── wordlists/           (WordListsFragment, WordListsViewModel, WordListsAdapter)
```

- Place new features as a sub-package under `ui/`
- Data models and DAOs belong in `data/`

---

## Key Dependencies (exact versions)

### AndroidX & UI
- AppCompat: `1.6.1`
- Material Design (Material3): `1.11.0`
- ConstraintLayout: `2.1.4`
- Activity: `1.8.0`
- Navigation Fragment/UI: `2.7.7`

### Architecture Components
- ViewModel / LiveData: `2.7.0`

### Room (Local DB)
- Room Runtime + Compiler: `2.6.1`
- DB name: `word_database`, version `3`
- Migration strategy: `fallbackToDestructiveMigration()`

### Firebase
- Firebase BOM: `33.0.0`
- Firebase Auth, Firestore (via BOM)
- Google Play Services Auth: `21.0.0`

### Camera & ML
- CameraX (core/camera2/lifecycle/view): `1.3.1`
- ML Kit Text Recognition: `16.0.0`
- ML Kit Translate: `17.0.1`

### Image Loading
- Glide: `4.16.0`

---

## Coding Standards

### Java Style
- Use standard Android Java conventions (camelCase, PascalCase for classes)
- All source files are `.java` — never create `.kt` files
- Annotations: `@Override`, `@NonNull`, `@Nullable` from `androidx.annotation`
- Use `ExecutorService` for background/DB threads, `postValue()` on LiveData
- Use `ViewBinding` (never `findViewById` in new code)

### ViewBinding
- Binding field: `private FragmentXxxBinding binding;`
- Inflate in `onCreateView`: `binding = FragmentXxxBinding.inflate(inflater, container, false); return binding.getRoot();`
- Release in `onDestroyView`: `binding = null;`

### ViewModel
- Extend `AndroidViewModel` (requires `Application` context for Room)
- Expose data via `LiveData<T>` (never expose `MutableLiveData` publicly)
- Constructor: `public XxxViewModel(@NonNull Application application) { super(application); }`

### Room / Database
- Entity: annotate with `@Entity(tableName = "words_table")`
- DAO: `@Dao` interface with `@Query`, `@Insert`, `@Update`, `@Delete`
- Database ops must run off the main thread via `ExecutorService`

### Navigation
- Use Navigation Component (nav_graph.xml)
- Navigate with `NavController` via `Navigation.findNavController(view)`
- Pass data using Safe Args or `Bundle`

### Fragments vs Activities
- Prefer Fragments for screens (Activities only for auth flow and MainActivity shell)
- Auth screens: `SplashActivity`, `LoginActivity`, `RegisterActivity`
- Main app: `MainActivity` hosts `NavHostFragment`

### Naming Conventions
- Layouts: `fragment_xxx.xml`, `activity_xxx.xml`, `item_xxx.xml`, `dialog_xxx.xml`
- ViewModels: `XxxViewModel.java`
- Adapters: `XxxAdapter.java`
- DAOs: `XxxDao.java`

---

## Theming

- Theme: `Theme.Material3.DayNight.NoActionBar`
- Primary: green (`green_primary`)
- Accent: turquoise (`turquoise_accent`)
- Background: light grey (`grey_light`)
- Dark mode: supported via `values-night/`
- RTL: enabled (`supportsRtl="true"`)
- Hebrew language support is core to the app

---

## What NOT to Do

- Do NOT write Kotlin code
- Do NOT use Jetpack Compose
- Do NOT introduce Hilt, Dagger, or any DI framework
- Do NOT use Retrofit or OkHttp (Firebase is the backend)
- Do NOT use DataBinding (ViewBinding only)
- Do NOT create new modules without explicit request
- Do NOT use coroutines (use ExecutorService + LiveData postValue)
- Do NOT use Flow (use LiveData)
- Do NOT skip `binding = null` in `onDestroyView`
