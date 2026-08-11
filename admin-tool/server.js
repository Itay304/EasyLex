const path = require('path');
const fs = require('fs');
const express = require('express');
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

const app = express();
app.use(express.json({ limit: '10mb' }));
app.use(express.static(path.join(__dirname, 'public')));

function toDocData(word) {
  const difficulty = Number(word.difficulty);
  return {
    englishWord: String(word.englishWord || '').trim(),
    hebrewTranslation: String(word.hebrewTranslation || '').trim(),
    partOfSpeech: String(word.partOfSpeech || '').trim(),
    exampleSentence: String(word.exampleSentence || '').trim(),
    hebrewExample: String(word.hebrewExample || '').trim(),
    tags: String(word.tags || '').trim(),
    difficulty: Number.isFinite(difficulty) && difficulty > 0 ? difficulty : 1,
    correctAttempts: 0,
    totalAttempts: 0,
  };
}

app.get('/lists', async (req, res) => {
  try {
    const snapshot = await db.collection('word_lists').select('wordCount').get();
    const lists = snapshot.docs.map((doc) => ({
      id: doc.id,
      wordCount: doc.get('wordCount') || 0,
    }));
    res.json({ lists });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

app.get('/existing-words', async (req, res) => {
  const listId = req.query.listId;
  if (!listId) {
    return res.status(400).json({ error: 'חסר פרמטר listId' });
  }
  try {
    const snapshot = await db
      .collection('word_lists')
      .doc(listId)
      .collection('words')
      .select('englishWord')
      .get();
    const words = snapshot.docs
      .map((doc) => doc.get('englishWord'))
      .filter((w) => typeof w === 'string' && w.length > 0);
    res.json({ words });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

app.post('/upload', async (req, res) => {
  const listId = req.query.listId;
  if (!listId) {
    return res.status(400).json({ error: 'חסר פרמטר listId' });
  }
  const words = req.body;
  if (!Array.isArray(words) || words.length === 0) {
    return res.status(400).json({ error: 'לא נשלחו מילים להעלאה' });
  }

  const results = { uploaded: 0, failed: 0, errors: [] };
  const listRef = db.collection('word_lists').doc(listId);
  const wordsRef = listRef.collection('words');

  // Firestore batches are capped at 500 writes.
  const BATCH_SIZE = 400;
  for (let i = 0; i < words.length; i += BATCH_SIZE) {
    const chunk = words.slice(i, i + BATCH_SIZE);
    const batch = db.batch();
    chunk.forEach((word) => {
      const docRef = wordsRef.doc();
      batch.set(docRef, toDocData(word));
    });
    try {
      await batch.commit();
      results.uploaded += chunk.length;
    } catch (err) {
      console.error(err);
      results.failed += chunk.length;
      results.errors.push(err.message);
    }
  }

  try {
    const countSnapshot = await wordsRef.count().get();
    const wordCount = countSnapshot.data().count;
    const listSnap = await listRef.get();
    const updateData = { wordCount };
    // כותבים name רק אם עדיין אין לרשימה שם (לא דורסים שם ידני שהוגדר בעבר).
    if (!listSnap.exists || !listSnap.get('name')) {
      updateData.name = listId;
    }
    await listRef.set(updateData, { merge: true });
    results.wordCount = wordCount;
  } catch (err) {
    console.error(err);
    results.errors.push('עדכון wordCount נכשל: ' + err.message);
  }

  res.json(results);
});

// שימוש חד-פעמי: משלים שדה 'name' לרשימות קיימות שהועלו לפני שהתחלנו לכתוב אותו.
// אפשר גם להעביר שמות מותאמים אישית: POST body { "names": { "pre_band_1": "Pre-Band I", ... } }
// רשימה שכבר יש לה name לא נדרסת, אלא אם היא מופיעה מפורשות ב-names שבבקשה.
app.post('/backfill-list-names', async (req, res) => {
  const overrides = (req.body && req.body.names) || {};
  try {
    const snapshot = await db.collection('word_lists').get();
    const results = { updated: [], skipped: [] };
    for (const doc of snapshot.docs) {
      const listId = doc.id;
      const existingName = doc.get('name');
      const overrideName = overrides[listId];
      if (overrideName) {
        await doc.ref.set({ name: overrideName }, { merge: true });
        results.updated.push({ id: listId, name: overrideName });
      } else if (!existingName) {
        await doc.ref.set({ name: listId }, { merge: true });
        results.updated.push({ id: listId, name: listId });
      } else {
        results.skipped.push({ id: listId, name: existingName });
      }
    }
    res.json(results);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// יצירת מוסד חדש (משימה 0.7, שלב 0ב). למנהל-על בלבד — כלי מקומי, לא חשוף
// לאפליקציה. יוצר institutions/{instId} + מקדם את המשתמש שהועבר ל-role: "principal".
function generateJoinCode() {
  // בלי תווים מבלבלים לקריאה/הקלדה: 0/O, 1/I/L.
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let part1 = '';
  let part2 = '';
  for (let i = 0; i < 4; i++) part1 += chars[Math.floor(Math.random() * chars.length)];
  for (let i = 0; i < 4; i++) part2 += chars[Math.floor(Math.random() * chars.length)];
  return `${part1}-${part2}`;
}

async function generateUniqueJoinCode() {
  const MAX_ATTEMPTS = 5;
  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    const code = generateJoinCode();
    const existing = await db
      .collection('institutions')
      .where('teacherJoinCode', '==', code)
      .limit(1)
      .get();
    if (existing.empty) return code;
  }
  throw new Error('לא הצלחנו להפיק קוד הצטרפות ייחודי אחרי כמה ניסיונות — נסה שוב');
}

app.post('/create-institution', async (req, res) => {
  const { name, adminUid } = req.body || {};
  if (!name || typeof name !== 'string' || !name.trim()) {
    return res.status(400).json({ error: 'חסר שדה name' });
  }
  if (!adminUid || typeof adminUid !== 'string' || !adminUid.trim()) {
    return res.status(400).json({ error: 'חסר שדה adminUid' });
  }

  try {
    const userRef = db.collection('users').doc(adminUid);
    const userSnap = await userRef.get();
    if (!userSnap.exists) {
      return res.status(400).json({
        error:
          `המשתמש ${adminUid} לא נמצא ב-users/. ` +
          'צריך להירשם לפחות פעם אחת דרך האפליקציה לפני יצירת מוסד.',
      });
    }

    const teacherJoinCode = await generateUniqueJoinCode();

    const instRef = db.collection('institutions').doc();
    const batch = db.batch();
    batch.set(instRef, {
      name: name.trim(),
      status: 'active',
      plan: 'free',
      teacherJoinCode,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    // role: "principal" — מקדם את המשתמש שהועבר להיות מנהל המוסד החדש.
    // הכתיבה הזו גם מפעילה את syncUserClaims (משימה 0.5) — ה-Custom Claims
    // של המשתמש יתעדכנו אוטומטית (עם עיכוב הפצה, כמו כל כתיבה ל-users/{uid}).
    batch.set(userRef, { institutionId: instRef.id, role: 'principal' }, { merge: true });
    await batch.commit();

    res.json({ institutionId: instRef.id, teacherJoinCode });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`EasyLex admin tool listening on http://localhost:${PORT}`);
});
