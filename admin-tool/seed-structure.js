// שימוש חד-פעמי/חוזר (משימה 0.3 באיפיון הפלטפורמה): יוצר מסמכי בדיקה במבנה
// הנתונים המוסדי החדש (institutions/classes/assignments/customLists/progress),
// כדי לאמת בקונסולת Firestore שהצורה תואמת לאיפיון, ולתת נתוני בדיקה אמיתיים
// לפני שנכתבים Security Rules (משימה 0.4) שאמורים להגן על המבנה הזה.
//
// בטוח להרצה חוזרת: בודק אם כל מסמך כבר קיים לפני יצירה, ולא דורס מסמך קיים.
//
// הרצה מתוך תיקיית admin-tool:
//   node seed-structure.js <uid>
//
// <uid> הוא ה-UID האמיתי שלך מ-Firebase Auth (למשל מ-Firebase Console > Authentication),
// כי מסמך ה-progress נכתב תחת users/{uid}/progress.
//
// דורש serviceAccountKey.json באותה תיקייה (בדיוק כמו server.js).

const path = require('path');
const fs = require('fs');
const admin = require('firebase-admin');

const SERVICE_ACCOUNT_PATH = path.join(__dirname, 'serviceAccountKey.json');

if (!fs.existsSync(SERVICE_ACCOUNT_PATH)) {
  console.error(
    '\nחסר קובץ serviceAccountKey.json בתיקיית admin-tool.\n' +
    'הורד אותו מ- Firebase Console > Project Settings > Service Accounts > Generate new private key\n'
  );
  process.exit(1);
}

const uid = process.argv[2];
if (!uid) {
  console.error('שימוש: node seed-structure.js <uid>');
  console.error('  <uid> = ה-UID שלך מ-Firebase Console > Authentication > Users');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(SERVICE_ACCOUNT_PATH)),
});

const db = admin.firestore();
db.settings({ preferRest: true });

const FieldValue = admin.firestore.FieldValue;

// מזהים קבועים (לא אקראיים) — כדי שההרצה תהיה אידמפוטנטית וניתנת לאיתור בקלות.
const INST_ID = 'test_institution';
const CLASS_ID = 'test_class';
const ASSIGNMENT_ID = 'test_assignment';
const CUSTOM_LIST_ID = 'test_custom_list';
const PROGRESS_WORD_KEY = 'test';

/**
 * יוצר מסמך רק אם עוד לא קיים. לא דורס נתונים קיימים — כך אפשר להריץ
 * את הסקריפט שוב ושוב (למשל אחרי מחיקה ידנית חלקית) בלי סיכון.
 */
async function createIfMissing(ref, data, label) {
  const snap = await ref.get();
  if (snap.exists) {
    console.log(`  (כבר קיים, דילוג) ${label}: ${ref.path}`);
    return;
  }
  await ref.set(data);
  console.log(`  נוצר ${label}: ${ref.path}`);
}

async function seed() {
  console.log(`מתחיל שתילת מבנה בדיקה עבור uid=${uid}...\n`);

  const instRef = db.collection('institutions').doc(INST_ID);
  await createIfMissing(instRef, {
    name: 'בית ספר בדיקה',
    teacherJoinCode: 'TEST-INST-001',
    status: 'active',
    plan: 'free',
    createdAt: FieldValue.serverTimestamp(),
  }, 'institution');

  const classRef = instRef.collection('classes').doc(CLASS_ID);
  await createIfMissing(classRef, {
    name: "כיתה ז'1 בדיקה",
    grade: '7',
    joinCode: '7A-TEST',
    teacherId: '',
    studentCount: 0,
    archived: false,
    createdAt: FieldValue.serverTimestamp(),
  }, 'class');

  // הערה: הוספתי שדה classId שלא היה ברשימת השדות המקורית שקיבלתי —
  // ראו הסבר מפורש בסיכום שנשלח בסוף המשימה (נדרש כדי שכלל ה-Rules של
  // assignments יהיה בר-מימוש בפועל).
  const assignmentRef = instRef.collection('assignments').doc(ASSIGNMENT_ID);
  await createIfMissing(assignmentRef, {
    listId: 'band_2_full_list',
    classId: CLASS_ID,
    wordIds: [],
    dueDate: null,
    createdBy: '',
    createdAt: FieldValue.serverTimestamp(),
  }, 'assignment');

  const customListRef = instRef.collection('customLists').doc(CUSTOM_LIST_ID);
  await createIfMissing(customListRef, {
    name: 'רשימה מוסדית בדיקה',
    wordCount: 0,
    createdBy: '',
    createdAt: FieldValue.serverTimestamp(),
  }, 'customList');

  const progressRef = db.collection('users').doc(uid)
    .collection('progress').doc(PROGRESS_WORD_KEY);
  await createIfMissing(progressRef, {
    englishWord: 'test',
    sourceListId: 'band_2_full_list',
    correctAttempts: 0,
    totalAttempts: 0,
    lastPracticed: FieldValue.serverTimestamp(),
  }, 'progress');

  console.log('\nסיכום מזהים לאימות בקונסולת Firestore:');
  console.log(`  institutions/${INST_ID}`);
  console.log(`  institutions/${INST_ID}/classes/${CLASS_ID}`);
  console.log(`  institutions/${INST_ID}/assignments/${ASSIGNMENT_ID}`);
  console.log(`  institutions/${INST_ID}/customLists/${CUSTOM_LIST_ID}`);
  console.log(`  users/${uid}/progress/${PROGRESS_WORD_KEY}`);
}

seed()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('השתילה נכשלה:', err);
    process.exit(1);
  });
