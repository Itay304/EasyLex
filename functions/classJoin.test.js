// node --test classJoin.test.js — נרמול קוד כיתה
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { normalizeJoinCode, looksLikeJoinCode, REASON_MESSAGES } = require("./classJoin");

test("normalizeJoinCode: missing hyphen, lowercase, spaces", () => {
  assert.equal(normalizeJoinCode("ab12345"), "AB-12345");
  assert.equal(normalizeJoinCode(" gj-472c4 "), "GJ-472C4");
  assert.equal(normalizeJoinCode("GJ 472C4"), "GJ-472C4");
  assert.equal(normalizeJoinCode(""), "");
  assert.equal(normalizeJoinCode(undefined), "");
});

test("other formats are kept (only trimmed/uppercased)", () => {
  assert.equal(normalizeJoinCode("demo01"), "DEMO01");
  assert.equal(looksLikeJoinCode("AB-12345"), true);
  assert.equal(looksLikeJoinCode("DEMO01"), false);
});

test("the not-found message is the requested Hebrew text", () => {
  assert.equal(REASON_MESSAGES["not-found"], "קוד הכיתה לא נמצא. בדקו את הקוד עם המורה ונסו שוב.");
});
