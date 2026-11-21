// סינון "תוכן מותאם לבתי ספר דתיים" — צד שרת. מוסד עם hideSensitiveWords=true לא רואה מילים שמתויגות
// "sensitive" (שדה tags, מחרוזת מופרדת בפסיקים): לא ביצירת משימה, לא במשימות קיימות, לא בספירות ההתקדמות.
// המקבילות בלקוח: src/lib/contentFilter.js באפליקציית התלמיד ובאפליקציית המורה (אותם כללים בדיוק).
// נבדק ב-contentFilter.test.js (node --test).

const stages = require("./stages");

const SENSITIVE_TAG = "sensitive";
const INSTITUTION_FIELD = "hideSensitiveWords";

/** מילה מתויגת "sensitive"? tags היא מחרוזת ("General, sensitive") — ותומכים גם במערך. */
function isSensitive(word) {
  const tags = word && word.tags;
  const list = Array.isArray(tags) ? tags : typeof tags === "string" ? tags.split(",") : [];
  return list.some((t) => String(t).trim().toLowerCase() === SENSITIVE_TAG);
}

/** ההגדרה במסמך המוסד (ברירת מחדל: כבוי). */
function hidesSensitive(institutionData) {
  return Boolean(institutionData && institutionData[INSTITUTION_FIELD] === true);
}

/**
 * משימה קיימת עם מילים מוסתרות: מוציאים אותן מ-wordIds ומכל שלב, בלי להזיז מילים בין שלבים
 * (תלמיד שכבר באמצע שלב 3 נשאר בשלב 3). שלב שהתרוקן לגמרי נמחק.
 * stagePlan לא תקין (סכום שונה מ-wordIds) — אותה חלוקה שהלקוח היה מחשב (buildStageSizes).
 */
function filterStagePlan(wordIds, stagePlan, hiddenIds) {
  const ids = Array.isArray(wordIds) ? wordIds : [];
  const hidden = hiddenIds instanceof Set ? hiddenIds : new Set(hiddenIds || []);
  const planSizes = stagePlan && Array.isArray(stagePlan.sizes) ? stagePlan.sizes : null;
  const sizes = planSizes && planSizes.reduce((a, b) => a + b, 0) === ids.length
    ? planSizes
    : stages.buildStageSizes(ids.length);
  if (hidden.size === 0) return { wordIds: ids, stagePlan: { sizes } };
  const keptIds = [];
  const keptSizes = [];
  let i = 0;
  for (const size of sizes) {
    const kept = ids.slice(i, i + size).filter((id) => !hidden.has(id));
    i += size;
    if (kept.length === 0) continue;
    keptIds.push(...kept);
    keptSizes.push(kept.length);
  }
  return { wordIds: keptIds, stagePlan: { sizes: keptSizes } };
}

/** משימה כפי שמוחזרת ללקוח, אחרי הסינון (רק אם המוסד מסנן). */
function applyToAssignment(assignment, hiddenIds, enabled) {
  if (!enabled) return assignment;
  const { wordIds, stagePlan } = filterStagePlan(assignment.wordIds, assignment.stagePlan, hiddenIds);
  return { ...assignment, wordIds, stagePlan };
}

// ── קריאות Firestore ─────────────────────────────────────────────────────

async function institutionHidesSensitive(db, institutionId) {
  if (!institutionId) return false;
  const snap = await db.collection("institutions").doc(institutionId).get();
  return snap.exists && hidesSensitive(snap.data());
}

/** אוסף המילים של משימה (גלובלי או מוסדי) — אותה לוגיקה כמו loadAssignmentWords ב-index.js. */
async function wordsCollection(db, institutionId, listId, listSource) {
  let source = listSource;
  if (source !== "custom" && source !== "global") {
    source = (await db.collection("word_lists").doc(listId).get()).exists ? "global" : "custom";
  }
  return source === "custom"
    ? db.collection("institutions").doc(institutionId).collection("customLists").doc(listId).collection("words")
    : db.collection("word_lists").doc(listId).collection("words");
}

/** מזהי המילים הרגישות מתוך wordIds (getAll בלבד — לא כל הרשימה). */
async function sensitiveIdsAmong(db, institutionId, listId, listSource, wordIds) {
  if (!Array.isArray(wordIds) || wordIds.length === 0) return new Set();
  const col = await wordsCollection(db, institutionId, listId, listSource);
  const docs = await db.getAll(...wordIds.map((id) => col.doc(id)));
  return new Set(docs.filter((d) => d.exists && isSensitive(d.data())).map((d) => d.id));
}

// מילים קשות לפי englishWord (getClassHardWords — progress גלובלי בלי מזהה מילה): קבוצת כל המילים
// הרגישות בכל הרשימות. נשמרת בזיכרון של מופע הפונקציה ל-10 דקות (קריאה אחת של collectionGroup לכל
// היותר פעם ב-10 דקות, ורק כשמוסד מסנן מבקש מילים קשות).
const SENSITIVE_CACHE_MS = 10 * 60 * 1000;
let sensitiveEnglishCache = null;

async function sensitiveEnglishWords(db, now = Date.now()) {
  if (sensitiveEnglishCache && now - sensitiveEnglishCache.at < SENSITIVE_CACHE_MS) return sensitiveEnglishCache.words;
  const snap = await db.collectionGroup("words").get();
  const words = new Set(
    snap.docs.filter((d) => isSensitive(d.data())).map((d) => String(d.data().englishWord || "").trim().toLowerCase())
  );
  sensitiveEnglishCache = { at: now, words };
  return words;
}

module.exports = {
  SENSITIVE_TAG,
  INSTITUTION_FIELD,
  isSensitive,
  hidesSensitive,
  filterStagePlan,
  applyToAssignment,
  institutionHidesSensitive,
  sensitiveIdsAmong,
  sensitiveEnglishWords,
};
