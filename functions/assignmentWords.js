// מילים של משימה (assignments-v2): מילים מכמה מקורות (כמה רשימות Band, או Band + רשימה מוסדית), הסרת
// כפילויות לפי המילה באנגלית, מגבלות גודל (מעל 300 — באישור, עד 1,000), וסינון מילים רגישות לפי מקור.
//
// מודל הנתונים של משימה (תואם לאחור — אפליקציית האנדרואיד ולקוחות ישנים קוראים listId + wordIds):
//   wordIds      — מזהי המילים, בסדר המשימה (אחרי הסרת כפילויות).
//   wordListIds  — מקביל ל-wordIds: מאיזו רשימה הגיעה כל מילה.
//   sources      — [{ listId, listSource }] — המקורות השונים, לפי סדר הופעה.
//   listId/listSource — המקור הראשון (תאימות).
// משימה ישנה בלי wordListIds = מקור אחד (listId).

const stages = require("./stages");
const contentFilter = require("./contentFilter");

const normSource = (s) => (s === "custom" ? "custom" : "global");
const normEnglish = (w) => String(w || "").trim().toLowerCase();

/** בקשת יצירה → [{ id, listId, listSource }]. תומך בפורמט החדש (words) ובישן (listId + wordIds). */
function wordsFromRequest(data) {
  const d = data || {};
  if (Array.isArray(d.words) && d.words.length > 0) {
    return d.words
      .map((w) => ({ id: String((w && w.id) || ""), listId: String((w && w.listId) || ""), listSource: normSource(w && w.listSource) }))
      .filter((w) => w.id && w.listId);
  }
  const ids = Array.isArray(d.wordIds) ? d.wordIds : [];
  if (!d.listId) return [];
  return ids.map((id) => ({ id: String(id), listId: String(d.listId), listSource: normSource(d.listSource) }));
}

/** המקורות השונים, לפי סדר הופעה */
function uniqueSources(words) {
  const seen = new Map();
  for (const w of words) {
    const key = `${w.listSource}:${w.listId}`;
    if (!seen.has(key)) seen.set(key, { listId: w.listId, listSource: w.listSource });
  }
  return [...seen.values()];
}

/** הסרת כפילויות: אותה מילה באנגלית (בלי רישיות/רווחים) — פעם אחת, המופע הראשון נשמר. גם אותו מזהה פעמיים. */
function dedupeByEnglish(words) {
  const seenEn = new Set();
  const seenId = new Set();
  const kept = [];
  for (const w of words) {
    const en = normEnglish(w.englishWord);
    const idKey = `${w.listId}/${w.id}`;
    if (seenId.has(idKey) || (en && seenEn.has(en))) continue;
    seenId.add(idKey);
    if (en) seenEn.add(en);
    kept.push(w);
  }
  return { kept, duplicates: words.length - kept.length };
}

/**
 * מגבלות יצירה (אחרי הסרת כפילויות). מחזיר null אם תקין, אחרת { code, message } ל-HttpsError.
 *   0 מילים — שגיאה; מעל 1,000 — נדחה; מעל 300 — רק עם acknowledgedLarge; יותר ממקור אחד — רק עם acknowledgedMixed.
 */
function checkLimits({ count, sourceCount, acknowledgedLarge, acknowledgedMixed }) {
  if (!count) return { code: "invalid-argument", message: "לא נבחרו מילים למשימה." };
  if (count > stages.MAX_ASSIGNMENT_WORDS) {
    return {
      code: "invalid-argument",
      message: `משימה יכולה לכלול עד ${stages.MAX_ASSIGNMENT_WORDS.toLocaleString("en-US")} מילים (נבחרו ${count.toLocaleString("en-US")}). צמצמו בסינון או חלקו לכמה משימות.`,
    };
  }
  if (count > stages.LARGE_ASSIGNMENT_WORDS && acknowledgedLarge !== true) {
    return {
      code: "failed-precondition",
      message: `משימה של יותר מ-${stages.LARGE_ASSIGNMENT_WORDS} מילים דורשת אישור: יש לסמן שאת/ה מודע/ת שזו משימה גדולה מאוד.`,
    };
  }
  if (sourceCount > 1 && acknowledgedMixed !== true) {
    return {
      code: "failed-precondition",
      message: "משימה שמשלבת מילים מכמה רשימות דורשת אישור: יש לסמן שאת/ה מודע/ת שהרשימות ברמות שונות.",
    };
  }
  return null;
}

/** המשימה → קבוצות מילים לפי רשימה: [{ listId, listSource, wordIds }] */
function wordGroups(assignment) {
  const ids = Array.isArray(assignment.wordIds) ? assignment.wordIds : [];
  const lists = Array.isArray(assignment.wordListIds) && assignment.wordListIds.length === ids.length ? assignment.wordListIds : null;
  if (!lists || ids.length === 0) {
    return [{ listId: assignment.listId, listSource: assignment.listSource, wordIds: ids }];
  }
  const sourceOf = new Map((assignment.sources || []).map((s) => [s.listId, s.listSource]));
  const groups = new Map();
  ids.forEach((id, i) => {
    const listId = lists[i];
    if (!groups.has(listId)) {
      groups.set(listId, { listId, listSource: sourceOf.get(listId) || (listId === assignment.listId ? assignment.listSource : undefined), wordIds: [] });
    }
    groups.get(listId).wordIds.push(id);
  });
  return [...groups.values()];
}

/** מזהי המילים הרגישות במשימה — מכל המקורות שלה */
async function sensitiveIdsForAssignment(db, institutionId, assignment) {
  const sets = await Promise.all(
    wordGroups(assignment).map((g) => contentFilter.sensitiveIdsAmong(db, institutionId, g.listId, g.listSource, g.wordIds)),
  );
  return new Set(sets.flatMap((s) => [...s]));
}

/** טעינת מסמכי המילים של בקשת יצירה (לפי רשימה), לשם englishWord ותיוג רגיש. מילים שלא קיימות — מושמטות. */
async function loadRequestedWords(db, institutionId, words) {
  const byList = new Map();
  for (const w of words) {
    const key = `${w.listSource}:${w.listId}`;
    if (!byList.has(key)) byList.set(key, { listId: w.listId, listSource: w.listSource, ids: [] });
    byList.get(key).ids.push(w.id);
  }
  const found = new Map(); // "listId/id" -> { englishWord, sensitive }
  for (const g of byList.values()) {
    const col = g.listSource === "custom"
      ? db.collection("institutions").doc(institutionId).collection("customLists").doc(g.listId).collection("words")
      : db.collection("word_lists").doc(g.listId).collection("words");
    for (let i = 0; i < g.ids.length; i += 300) {
      const docs = await db.getAll(...g.ids.slice(i, i + 300).map((id) => col.doc(id)));
      for (const d of docs) {
        if (d.exists) found.set(`${g.listId}/${d.id}`, { englishWord: d.data().englishWord, sensitive: contentFilter.isSensitive(d.data()) });
      }
    }
  }
  return words.filter((w) => found.has(`${w.listId}/${w.id}`)).map((w) => ({ ...w, ...found.get(`${w.listId}/${w.id}`) }));
}

module.exports = {
  wordsFromRequest,
  uniqueSources,
  dedupeByEnglish,
  checkLimits,
  wordGroups,
  sensitiveIdsForAssignment,
  loadRequestedWords,
};
