// בדיקות למודל השלבים בצד השרת — מריצים מתיקיית functions: npm test (node --test).
// אותם מקרים כמו the-easy-way-app-student/src/lib/stages.test.js, כדי ששני העותקים יישארו זהים.
const test = require("node:test");
const assert = require("node:assert/strict");
const { buildStageSizes, isKnown, dayKey, paceStatus } = require("./stages");

test("stage sizes stay within 10–15 (one stage under 20 words)", () => {
  assert.deepEqual(buildStageSizes(150), Array(10).fill(15));
  assert.deepEqual(buildStageSizes(7), [7]);
  assert.deepEqual(buildStageSizes(17), [17]);
  assert.deepEqual(buildStageSizes(20), [10, 10]);
  for (let n = 20; n <= 300; n++) {
    const sizes = buildStageSizes(n);
    assert.equal(sizes.reduce((a, b) => a + b, 0), n);
    assert.ok(sizes.every((s) => s >= 10 && s <= 15), `n=${n}`);
  }
});

test("known = 2 different days; lapse and recovery", () => {
  assert.equal(isKnown({ correctDays: ["2026-10-22"] }), false);
  assert.equal(isKnown({ correctDays: ["2026-10-22", "2026-10-23"] }), true);
  assert.equal(isKnown({ correctDays: ["2026-10-22", "2026-10-23"], lapsedDay: "2026-10-24" }), false);
  assert.equal(isKnown({ correctDays: ["2026-10-22", "2026-10-23", "2026-10-25"], lapsedDay: "2026-10-24" }), true);
});

test("Israel calendar day", () => {
  assert.equal(dayKey(Date.parse("2026-10-21T22:30:00Z")), "2026-10-22");
});

test("pace status", () => {
  const created = Date.parse("2026-10-20T08:00:00Z");
  const now = Date.parse("2026-10-24T08:00:00Z"); // יום 5
  // בלי תאריך יעד: צפוי 5 × 8 = 40
  assert.equal(paceStatus({ totalWords: 150, seenWords: 0, createdAtMs: created, now }).status, "not-started");
  assert.equal(paceStatus({ totalWords: 150, seenWords: 30, createdAtMs: created, now }).status, "on-pace");
  assert.equal(paceStatus({ totalWords: 150, seenWords: 20, createdAtMs: created, now }).status, "behind");
  // עם תאריך יעד בעוד 10 ימים מהיצירה: צפוי 150 × 5/10 = 75
  const due = Date.parse("2026-10-29T08:00:00Z");
  assert.equal(paceStatus({ totalWords: 150, seenWords: 60, createdAtMs: created, dueDateMs: due, now }).status, "on-pace");
  assert.equal(paceStatus({ totalWords: 150, seenWords: 45, createdAtMs: created, dueDateMs: due, now }).status, "behind");
});
