// הצטרפות לכיתה בקוד (joinClass / checkClassCode): נרמול הקוד, ומציאת הכיתה עם סיבה מדויקת כשאי אפשר
// להצטרף — כדי שהאפליקציה תציג הודעה ברורה לכל מקרה. נבדק ב-classJoin.test.js וב-classJoin.emulator.js.
//
// סיבות (details.reason ב-HttpsError):
//   "not-found"          — אין כיתה עם הקוד הזה
//   "class-closed"       — הכיתה סגורה (המורה העביר אותה לארכיון: archived = true)
//   "institution-closed" — המוסד הושבת (disabled = true, "השבת" בלוח הבקרה) — לא מקבל תלמידים חדשים
//   "rate-limited"       — יותר מדי ניסיונות

const CODE_RE = /^[A-Z]{2}-[A-Z0-9]{5}$/;

/** "ab 12345" / "AB12345" / " ab-12345 " → "AB-12345". קוד בפורמט אחר — רק trim + אותיות גדולות. */
function normalizeJoinCode(raw) {
  const s = String(raw || "").trim().toUpperCase().replace(/\s+/g, "");
  if (/^[A-Z]{2}[A-Z0-9]{5}$/.test(s)) return `${s.slice(0, 2)}-${s.slice(2)}`;
  return s;
}

const looksLikeJoinCode = (code) => CODE_RE.test(code);

/**
 * מחפש כיתה לפי קוד. אותו אינדקס של השאילתה הקיימת (joinCode + archived) — קודם כיתה פעילה,
 * ואם אין, בודק אם יש כיתה סגורה עם הקוד (כדי להבדיל "סגורה" מ"לא קיימת").
 * @returns {{ ok: true, classDoc, institutionId } | { ok: false, reason }}
 */
async function findClassForJoin(db, joinCode) {
  if (!joinCode) return { ok: false, reason: "not-found" };
  const active = await db.collectionGroup("classes").where("joinCode", "==", joinCode).where("archived", "==", false).limit(1).get();
  if (active.empty) {
    const closed = await db.collectionGroup("classes").where("joinCode", "==", joinCode).where("archived", "==", true).limit(1).get();
    return { ok: false, reason: closed.empty ? "not-found" : "class-closed" };
  }
  const classDoc = active.docs[0];
  const institutionRef = classDoc.ref.parent.parent;
  const institution = await institutionRef.get();
  if (institution.exists && institution.data().disabled === true) {
    return { ok: false, reason: "institution-closed" };
  }
  return { ok: true, classDoc, institutionId: institutionRef.id };
}

const REASON_MESSAGES = {
  "not-found": "קוד הכיתה לא נמצא. בדקו את הקוד עם המורה ונסו שוב.",
  "class-closed": "הכיתה הזו סגורה ולא מקבלת תלמידים חדשים. בקשו מהמורה קוד של כיתה פעילה.",
  "institution-closed": "בית הספר לא מקבל כרגע תלמידים חדשים באפליקציה. פנו למורה.",
  "rate-limited": "יותר מדי ניסיונות. חכו כמה דקות ונסו שוב.",
};
const REASON_CODES = {
  "not-found": "not-found",
  "class-closed": "failed-precondition",
  "institution-closed": "failed-precondition",
  "rate-limited": "resource-exhausted",
};

module.exports = { normalizeJoinCode, looksLikeJoinCode, findClassForJoin, REASON_MESSAGES, REASON_CODES };
