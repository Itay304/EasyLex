// node --test assignmentWords.test.js — המגבלות, הסרת כפילויות ומקורות מרובים של משימה (assignments-v2)
const { test } = require("node:test");
const assert = require("node:assert/strict");
const aw = require("./assignmentWords");

const many = (n, listId = "Band_2", listSource = "global") =>
  Array.from({ length: n }, (_, i) => ({ id: `w${i}`, listId, listSource, englishWord: `word${i}` }));

test("300 מילים — מותר בלי אישור", () => {
  assert.equal(aw.checkLimits({ count: 300, sourceCount: 1 }), null);
});

test("301 מילים בלי אישור — נדחה; עם אישור — מותר", () => {
  const e = aw.checkLimits({ count: 301, sourceCount: 1 });
  assert.equal(e.code, "failed-precondition");
  assert.match(e.message, /יותר מ-300/);
  assert.equal(aw.checkLimits({ count: 301, sourceCount: 1, acknowledgedLarge: true }), null);
});

test("1,000 מילים עם אישור — מותר; 1,001 — נדחה גם עם אישור, בהודעה ברורה", () => {
  assert.equal(aw.checkLimits({ count: 1000, sourceCount: 1, acknowledgedLarge: true }), null);
  const e = aw.checkLimits({ count: 1001, sourceCount: 1, acknowledgedLarge: true, acknowledgedMixed: true });
  assert.equal(e.code, "invalid-argument");
  assert.match(e.message, /עד 1,000 מילים/);
  assert.match(e.message, /1,001/);
});

test("כמה מקורות בלי אישור — נדחה; עם אישור — מותר", () => {
  const e = aw.checkLimits({ count: 40, sourceCount: 2 });
  assert.equal(e.code, "failed-precondition");
  assert.match(e.message, /כמה רשימות/);
  assert.equal(aw.checkLimits({ count: 40, sourceCount: 2, acknowledgedMixed: true }), null);
});

test("0 מילים — נדחה", () => {
  assert.equal(aw.checkLimits({ count: 0, sourceCount: 0 }).code, "invalid-argument");
});

test("אותה מילה באנגלית משתי רשימות — נכנסת פעם אחת (הראשונה), בלי תלות ברישיות/רווחים", () => {
  const words = [
    { id: "a1", listId: "Band_1", listSource: "global", englishWord: "Happy" },
    { id: "b1", listId: "band_2", listSource: "global", englishWord: " happy " },
    { id: "b2", listId: "band_2", listSource: "global", englishWord: "sad" },
    { id: "c1", listId: "myList", listSource: "custom", englishWord: "SAD" },
  ];
  const { kept, duplicates } = aw.dedupeByEnglish(words);
  assert.deepEqual(kept.map((w) => w.id), ["a1", "b2"]);
  assert.equal(duplicates, 2);
});

test("אותו מזהה פעמיים — פעם אחת", () => {
  const w = { id: "a1", listId: "Band_1", listSource: "global", englishWord: "x" };
  assert.equal(aw.dedupeByEnglish([w, { ...w }]).kept.length, 1);
});

test("wordsFromRequest: פורמט חדש (words) ופורמט ישן (listId + wordIds)", () => {
  assert.deepEqual(aw.wordsFromRequest({ words: [{ id: "a", listId: "L", listSource: "custom" }, { id: "", listId: "L" }] }), [
    { id: "a", listId: "L", listSource: "custom" },
  ]);
  assert.deepEqual(aw.wordsFromRequest({ listId: "Band_1", wordIds: ["x", "y"] }), [
    { id: "x", listId: "Band_1", listSource: "global" },
    { id: "y", listId: "Band_1", listSource: "global" },
  ]);
  assert.deepEqual(aw.wordsFromRequest({ listId: "Band_1", wordIds: [] }), []);
});

test("uniqueSources: Band + רשימה מוסדית = 2 מקורות, לפי סדר הופעה", () => {
  const words = [...many(2, "Band_1"), ...many(2, "inst-list", "custom"), ...many(1, "Band_1")];
  assert.deepEqual(aw.uniqueSources(words), [
    { listId: "Band_1", listSource: "global" },
    { listId: "inst-list", listSource: "custom" },
  ]);
});

test("wordGroups: משימה עם wordListIds מתחלקת לפי רשימה; משימה ישנה = קבוצה אחת", () => {
  const groups = aw.wordGroups({
    listId: "Band_1",
    listSource: "global",
    sources: [{ listId: "Band_1", listSource: "global" }, { listId: "inst-list", listSource: "custom" }],
    wordIds: ["a", "b", "c"],
    wordListIds: ["Band_1", "inst-list", "Band_1"],
  });
  assert.deepEqual(groups, [
    { listId: "Band_1", listSource: "global", wordIds: ["a", "c"] },
    { listId: "inst-list", listSource: "custom", wordIds: ["b"] },
  ]);
  assert.deepEqual(aw.wordGroups({ listId: "Band_2", listSource: "global", wordIds: ["x"] }), [
    { listId: "Band_2", listSource: "global", wordIds: ["x"] },
  ]);
});
