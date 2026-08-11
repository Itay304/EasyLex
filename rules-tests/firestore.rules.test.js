// בדיקות אוטומטיות ל-firestore.rules (משימה 0.4, עודכן במשימה 0.6).
//
// מאז 0.6, role/institutionId נקראים ב-Rules מ-request.auth.token (Custom
// Claims), לא מ-get() על מסמך users/{uid}. לכן הבדיקות כאן מעבירות אותם
// כ-tokenClaims דרך testEnv.authenticatedContext(uid, claims) — לא עוד
// כשדות במסמך users/{uid} מדומה.
//
// classIds *נשאר* חריג: myClassIds() ב-Rules עדיין קורא get() על המסמך
// (ר' firestore.rules), כי הוא לא עבר ל-Custom Claims. הבדיקות שתלויות
// בזה (assignments, תרחישים 8-9) עדיין מזריעות מסמך users/{uid} עם שדה
// classIds בפועל.
//
// רצות אך ורק מול Firebase Emulator מקומי — לעולם לא נוגעות ב-DB האמיתי.
// יש להריץ קודם `firebase emulators:start --only firestore --project demo-easylex`
// (בטרמינל נפרד, ר' README.md באותה תיקייה) ורק אז `npm test`.

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const fs = require('fs');
const path = require('path');

const PROJECT_ID = 'easylex-rules-test';
const FIRESTORE_EMULATOR_PORT = 8080;

let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: FIRESTORE_EMULATOR_PORT,
    },
  });
});

afterAll(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

/** כותב נתוני בדיקה תוך עקיפה מלאה של ה-Rules (context מנהלי של הבדיקה עצמה). */
async function seed(setupFn) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setupFn(context.firestore());
  });
}

/** משתמש מחובר עם Custom Claims מדומים (role/institutionId) — לא מסמך users/{uid}. */
function authedContext(uid, claims) {
  return testEnv.authenticatedContext(uid, claims).firestore();
}

// ── 1-2. users/{uid} — מבוסס בעלות (isOwner), לא תלוי ב-role בכלל ───────

describe('users/{uid}', () => {
  test('תלמיד קורא את המסמך שלו בלבד', async () => {
    await seed(async (db) => {
      await db.doc('users/student_a').set({ displayName: 'סתיו' });
    });

    const studentA = authedContext('student_a', { role: 'student' });
    await assertSucceeds(studentA.doc('users/student_a').get());
  });

  test('תלמיד לא יכול לקרוא מסמך של תלמיד אחר', async () => {
    await seed(async (db) => {
      await db.doc('users/student_a').set({ displayName: 'סתיו' });
      await db.doc('users/student_b').set({ displayName: 'נועה' });
    });

    const studentA = authedContext('student_a', { role: 'student' });
    await assertFails(studentA.doc('users/student_b').get());
  });
});

// ── 3. word_lists — הבאנדים הציבוריים ────────────────────────────────────

describe('word_lists (הבאנדים הציבוריים)', () => {
  test('תלמיד לא יכול לכתוב ל-word_lists', async () => {
    const studentA = authedContext('student_a', { role: 'student' });
    await assertFails(
      studentA.doc('word_lists/band_2_full_list/words/w1').set({ englishWord: 'test' })
    );
  });

  test('תלמיד יכול לקרוא מ-word_lists (רשימה ציבורית, לכל משתמש מחובר)', async () => {
    await seed(async (db) => {
      await db.doc('word_lists/band_2_full_list/words/w1').set({ englishWord: 'test' });
    });

    const studentA = authedContext('student_a', { role: 'student' });
    await assertSucceeds(studentA.doc('word_lists/band_2_full_list/words/w1').get());
  });
});

// ── 4. institutions/classes — הבידוד הכי קריטי ───────────────────────────
// אין יותר צורך לזרוע users/{uid} — role/institutionId מגיעים מהטוקן.

describe('institutions/classes — בידוד בין מוסדות', () => {
  test('מוסד A לא יכול לקרוא כיתה של מוסד B', async () => {
    await seed(async (db) => {
      await db.doc('institutions/inst_b/classes/class_b').set({
        name: 'class b',
        teacherId: 'teacher_b',
      });
    });

    const teacherA = authedContext('teacher_a', { role: 'teacher', institutionId: 'inst_a' });
    await assertFails(teacherA.doc('institutions/inst_b/classes/class_b').get());
  });

  test('מורה יכול לקרוא כיתה של המוסד שלו', async () => {
    await seed(async (db) => {
      await db.doc('institutions/inst_a/classes/class_a').set({
        name: 'class a',
        teacherId: 'teacher_a',
      });
    });

    const teacherA = authedContext('teacher_a', { role: 'teacher', institutionId: 'inst_a' });
    await assertSucceeds(teacherA.doc('institutions/inst_a/classes/class_a').get());
  });
});

// ── 5. assignments — כאן myClassIds() עדיין מבוסס get() (חריג מכוון) ────

describe('institutions/{instId}/assignments', () => {
  test('מורה יכול לכתוב assignment לכיתה שלו', async () => {
    await seed(async (db) => {
      await db.doc('institutions/inst_a/classes/class_a').set({
        name: 'class a',
        teacherId: 'teacher_a',
      });
    });

    const teacherA = authedContext('teacher_a', { role: 'teacher', institutionId: 'inst_a' });
    await assertSucceeds(
      teacherA.doc('institutions/inst_a/assignments/assignment_1').set({
        listId: 'band_2_full_list',
        classId: 'class_a',
        wordIds: [],
        dueDate: null,
        createdBy: 'teacher_a',
      })
    );
  });

  test('תלמיד שבכיתה של המשימה יכול לקרוא אותה', async () => {
    await seed(async (db) => {
      // classIds עדיין נקרא מהמסמך (לא מה-token) — ר' הערה בראש הקובץ.
      await db.doc('users/student_a').set({ classIds: ['class_a'] });
      await db.doc('institutions/inst_a/assignments/assignment_1').set({
        listId: 'band_2_full_list',
        classId: 'class_a',
        wordIds: [],
        dueDate: null,
        createdBy: 'teacher_a',
      });
    });

    const studentA = authedContext('student_a', { role: 'student', institutionId: 'inst_a' });
    await assertSucceeds(studentA.doc('institutions/inst_a/assignments/assignment_1').get());
  });

  test('תלמיד שלא בכיתה של המשימה לא יכול לקרוא אותה', async () => {
    await seed(async (db) => {
      await db.doc('users/student_a').set({ classIds: ['some_other_class'] });
      await db.doc('institutions/inst_a/assignments/assignment_1').set({
        listId: 'band_2_full_list',
        classId: 'class_a',
        wordIds: [],
        dueDate: null,
        createdBy: 'teacher_a',
      });
    });

    const studentA = authedContext('student_a', { role: 'student', institutionId: 'inst_a' });
    await assertFails(studentA.doc('institutions/inst_a/assignments/assignment_1').get());
  });
});

// ── 7. progress — פרטי לגמרי, מבוסס בעלות בלבד ────────────────────────────

describe('users/{uid}/progress', () => {
  test('תלמיד לא יכול לקרוא progress של תלמיד אחר', async () => {
    await seed(async (db) => {
      await db.doc('users/student_b/progress/test').set({ englishWord: 'test' });
    });

    const studentA = authedContext('student_a', { role: 'student' });
    await assertFails(studentA.doc('users/student_b/progress/test').get());
  });
});

// ── 6. גישה לא-מאומתת ─────────────────────────────────────────────────────

describe('משתמש לא מחובר', () => {
  test('לא יכול לגשת לשום דבר', async () => {
    await seed(async (db) => {
      await db.doc('users/student_a').set({ displayName: 'סתיו' });
      await db.doc('word_lists/band_2_full_list/words/w1').set({ englishWord: 'test' });
      await db.doc('institutions/inst_a').set({ name: 'inst a' });
    });

    const anon = testEnv.unauthenticatedContext().firestore();
    await assertFails(anon.doc('users/student_a').get());
    await assertFails(anon.doc('word_lists/band_2_full_list/words/w1').get());
    await assertFails(anon.doc('institutions/inst_a').get());
  });
});
