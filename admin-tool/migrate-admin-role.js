// שימוש חד-פעמי (משימה 0.1 באיפיון הפלטפורמה): ממיר כל מסמך users/{uid} עם
// role="admin" (המודל הישן) ל-role="superadmin" (המודל החדש בן 4 הערכים:
// student/teacher/principal/superadmin). לא נוגע במסמכים עם role אחר או ללא role.
//
// הרצה מתוך תיקיית admin-tool:
//   node migrate-admin-role.js
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

admin.initializeApp({
  credential: admin.credential.cert(require(SERVICE_ACCOUNT_PATH)),
});

const db = admin.firestore();
db.settings({ preferRest: true });

async function migrateAdminToSuperadmin() {
  const snapshot = await db.collection('users').where('role', '==', 'admin').get();

  if (snapshot.empty) {
    console.log('לא נמצאו מסמכי משתמש עם role="admin". לא בוצע שינוי.');
    return;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.update(doc.ref, { role: 'superadmin' });
  });
  await batch.commit();

  console.log(`הומרו ${snapshot.size} מסמכי משתמש מ-role="admin" ל-role="superadmin":`);
  snapshot.docs.forEach((doc) => console.log(`  - users/${doc.id}`));
}

migrateAdminToSuperadmin()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('המיגרציה נכשלה:', err);
    process.exit(1);
  });
