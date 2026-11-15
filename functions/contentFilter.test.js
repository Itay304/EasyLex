// בדיקות לסינון המילים הרגישות בצד השרת — npm test (node --test) מתיקיית functions.
// אותם מקרים כמו src/lib/contentFilter.test.js באפליקציות התלמיד והמורה.
const test = require("node:test");
const assert = require("node:assert/strict");
const { isSensitive, hidesSensitive, filterStagePlan, applyToAssignment } = require("./contentFilter");
const { buildStageSizes } = require("./stages");

test("sensitive tag: comma-separated string or array, case-insensitive", () => {
  assert.equal(isSensitive({ tags: "General, sensitive" }), true);
  assert.equal(isSensitive({ tags: "Sensitive" }), true);
  assert.equal(isSensitive({ tags: ["School", "sensitive"] }), true);
  assert.equal(isSensitive({ tags: "General" }), false);
  assert.equal(isSensitive({ tags: "insensitive-ish" }), false);
  assert.equal(isSensitive({}), false);
});

test("institution switch defaults to OFF", () => {
  assert.equal(hidesSensitive({}), false);
  assert.equal(hidesSensitive({ hideSensitiveWords: false }), false);
  assert.equal(hidesSensitive({ hideSensitiveWords: "yes" }), false);
  assert.equal(hidesSensitive({ hideSensitiveWords: true }), true);
});

// משימה קיימת: 30 מילים בשני שלבים של 15, מילה רגישה אחת בשלב 1 ואחת בשלב 2
const ids = Array.from({ length: 30 }, (_, i) => `w${i}`);
const assignment = { assignmentId: "a1", wordIds: ids, stagePlan: { sizes: buildStageSizes(30) } };

test("switch OFF: existing assignment unchanged", () => {
  assert.deepEqual(applyToAssignment(assignment, new Set(["w3", "w20"]), false), assignment);
});

test("switch ON: hidden words leave wordIds and their own stage; other words keep their stage", () => {
  const out = applyToAssignment(assignment, new Set(["w3", "w20"]), true);
  assert.equal(out.wordIds.length, 28);
  assert.ok(!out.wordIds.includes("w3") && !out.wordIds.includes("w20"));
  assert.deepEqual(out.stagePlan.sizes, [14, 14]);
  // w15 was the first word of stage 2 and stays the first word of stage 2
  assert.equal(out.wordIds[out.stagePlan.sizes[0]], "w15");
  assert.equal(out.stagePlan.sizes.reduce((a, b) => a + b, 0), out.wordIds.length);
});

test("a stage made only of hidden words disappears (nobody gets stuck on it)", () => {
  const small = { wordIds: ["a", "b", "c", "d"], stagePlan: { sizes: [2, 2] } };
  const out = filterStagePlan(small.wordIds, small.stagePlan, new Set(["a", "b"]));
  assert.deepEqual(out, { wordIds: ["c", "d"], stagePlan: { sizes: [2] } });
});

test("old assignment without a valid stagePlan uses the default split before filtering", () => {
  const out = filterStagePlan(ids, null, new Set(["w0"]));
  assert.deepEqual(out.stagePlan.sizes, [14, 15]);
});

test("no hidden words: same ids and plan", () => {
  const out = filterStagePlan(ids, assignment.stagePlan, new Set());
  assert.deepEqual(out, { wordIds: ids, stagePlan: assignment.stagePlan });
});
