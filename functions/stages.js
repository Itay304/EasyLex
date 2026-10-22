// מודל השלבים — צד שרת. מקביל ל-the-easy-way-app-student/src/lib/stages.js (אותם קבועים ואותן
// הגדרות; הלקוח ESM והפונקציות CommonJS, ולכן שני עותקים קטנים). כל שינוי בהגדרת "ידועה" או
// בחלוקת השלבים — בשני המקומות. נבדק ב-stages.test.js (node --test).

const STAGE_MIN = 10;
const STAGE_MAX = 15;
const MAX_ASSIGNMENT_WORDS = 300;
const SESSION_BASE = 16;
const NEW_PER_SESSION = Math.ceil(SESSION_BASE / 2); // קצב ברירת מחדל: סבב ביום ≈ 8 מילים חדשות
const DAY_MS = 24 * 60 * 60 * 1000;

function dayKey(ms = Date.now()) {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Jerusalem" }).format(new Date(ms));
}

function dayDiff(a, b) {
  return Math.round((Date.parse(`${b}T00:00:00Z`) - Date.parse(`${a}T00:00:00Z`)) / DAY_MS);
}

function buildStageSizes(wordCount) {
  if (wordCount <= 0) return [];
  const count = Math.max(1, Math.min(Math.ceil(wordCount / STAGE_MAX), Math.floor(wordCount / STAGE_MIN)));
  const base = Math.floor(wordCount / count);
  const extra = wordCount % count;
  return Array.from({ length: count }, (_, i) => base + (i < extra ? 1 : 0));
}

function isKnown(entry) {
  if (!entry || !Array.isArray(entry.correctDays)) return false;
  const distinct = new Set(entry.correctDays);
  if (distinct.size < 2) return false;
  const last = [...entry.correctDays].sort().at(-1);
  return !entry.lapsedDay || last > entry.lapsedDay;
}

/**
 * מצב תלמיד במשימה למורה: "not-started" | "behind" | "on-pace".
 * הקצב הצפוי = כמה מילים התלמיד היה אמור "לראות" עד היום:
 *   עם תאריך יעד — חלק יחסי מהמשימה לפי הזמן שעבר מיצירתה עד היעד;
 *   בלי תאריך יעד — סבב ביום (NEW_PER_SESSION מילים חדשות ליום) מיום היצירה.
 * "מאחור" = ראה פחות מהצפוי בפער של יותר משלב אחד (STAGE_MAX), כדי לא לסמן על יום חופש בודד.
 */
function paceStatus({ totalWords, seenWords, createdAtMs, dueDateMs, now = Date.now() }) {
  if (!seenWords) return { status: "not-started", expectedSeen: expectedSeen() };
  const exp = expectedSeen();
  return { status: seenWords + STAGE_MAX < exp ? "behind" : "on-pace", expectedSeen: exp };

  function expectedSeen() {
    const days = Math.max(0, dayDiff(dayKey(createdAtMs || now), dayKey(now))) + 1;
    if (dueDateMs) {
      const span = Math.max(1, dayDiff(dayKey(createdAtMs || now), dayKey(dueDateMs)) + 1);
      return Math.min(totalWords, Math.ceil((totalWords * days) / span));
    }
    return Math.min(totalWords, days * NEW_PER_SESSION);
  }
}

module.exports = {
  STAGE_MIN,
  STAGE_MAX,
  MAX_ASSIGNMENT_WORDS,
  NEW_PER_SESSION,
  dayKey,
  dayDiff,
  buildStageSizes,
  isKnown,
  paceStatus,
};
