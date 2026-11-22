// בדיקת קצה-לקצה ל-assignments-v2 מול האמולטורים (Firestore + Auth + Functions), בפרויקט דמו מקומי —
// לא נוגעת בפרויקט האמיתי. מריצים מתיקיית השורש של EasyLex:
//   npx -y firebase-tools@15.24.0 emulators:exec --only firestore,auth,functions --project demo-easylex "node functions/assignmentsV2.emulator.js"
// מקרים: ארכיון/שחזור (הרשאות, הסתרה מתלמיד, התקדמות נשמרת, לא בסיכום), 300/301/1,000/1,001 מילים,
// מקורות מעורבים עם/בלי אישור, מילה כפולה בין רשימות, סינון מילים רגישות ממקור מוסדי.
const assert = require("node:assert/strict");
const admin = require("firebase-admin");

const PROJECT = process.env.GCLOUD_PROJECT || "demo-easylex";
const FN = `http://${process.env.FUNCTIONS_EMULATOR_HOST || "127.0.0.1:5001"}/${PROJECT}/europe-west1`;
const AUTH = `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099"}`;

admin.initializeApp({ projectId: PROJECT });
const db = admin.firestore();
const results = [];

function check(name, fn) {
  return Promise.resolve()
    .then(fn)
    .then(() => results.push(`PASS  ${name}`))
    .catch((e) => results.push(`FAIL  ${name} — ${e.message}`));
}

async function tokenFor(uid, claims) {
  await admin.auth().createUser({ uid }).catch(() => {});
  await admin.auth().setCustomUserClaims(uid, claims);
  const custom = await admin.auth().createCustomToken(uid, claims);
  const res = await fetch(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=fake`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token: custom, returnSecureToken: true }),
  });
  return (await res.json()).idToken;
}

async function call(name, token, data = {}) {
  const res = await fetch(`${FN}/${name}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ data }),
  });
  const body = await res.json();
  if (body.error) {
    const err = new Error(body.error.message);
    err.status = body.error.status;
    throw err;
  }
  return body.result;
}

async function expectError(promise, status, re) {
  try {
    await promise;
  } catch (e) {
    assert.equal(e.status, status, `status ${e.status}: ${e.message}`);
    if (re) assert.match(e.message, re);
    return e;
  }
  throw new Error(`expected ${status}, but the call succeeded`);
}

const INST = "X";
const words = (listId, n, listSource = "global", from = 0) =>
  Array.from({ length: n }, (_, i) => ({ id: `${listId}_w${from + i}`, listId, listSource }));

async function seed() {
  const writes = [];
  // B1: 400 מילים, B2: 800 מילים — 5 הראשונות שלה הן אותן מילים באנגלית כמו 5 הראשונות של B1 (רישיות שונה)
  writes.push(["word_lists/B1", { name: "Band 1" }], ["word_lists/B2", { name: "Band 2" }]);
  for (let i = 0; i < 400; i++) writes.push([`word_lists/B1/words/B1_w${i}`, { englishWord: `bone${i}`, hebrewTranslation: `א${i}`, tags: "General" }]);
  for (let i = 0; i < 800; i++) {
    const englishWord = i < 5 ? `BONE${i}` : `btwo${i}`;
    writes.push([`word_lists/B2/words/B2_w${i}`, { englishWord, hebrewTranslation: `ב${i}`, tags: "General" }]);
  }
  // רשימה מוסדית C1: 20 מילים, C1_w7 מתויגת רגישה
  writes.push([`institutions/${INST}`, { name: "בית ספר בדיקה", hideSensitiveWords: true }]);
  writes.push([`institutions/${INST}/customLists/C1`, { name: "רשימה מוסדית" }]);
  for (let i = 0; i < 20; i++) {
    writes.push([`institutions/${INST}/customLists/C1/words/C1_w${i}`, { englishWord: `inst${i}`, hebrewTranslation: `ג${i}`, tags: i === 7 ? "General, sensitive" : "General" }]);
  }
  writes.push([`institutions/${INST}/classes/K`, { name: "ז1", teacherId: "T1" }]);
  writes.push(["users/S", { role: "student", institutionId: INST, classIds: ["K"], displayName: "תלמיד" }]);
  for (let i = 0; i < writes.length; i += 450) {
    const batch = db.batch();
    writes.slice(i, i + 450).forEach(([path, data]) => batch.set(db.doc(path), data));
    await batch.commit();
  }
}

(async () => {
  await seed();
  const T1 = await tokenFor("T1", { role: "teacher", institutionId: INST });
  const T2 = await tokenFor("T2", { role: "teacher", institutionId: INST });
  const P = await tokenFor("P", { role: "principal", institutionId: INST });
  const S = await tokenFor("S", { role: "student", institutionId: INST });
  const base = { classId: "K", title: "משימה" };
  const doc = async (id) => (await db.doc(`institutions/${INST}/assignments/${id}`).get()).data();
  let mixedId;

  await check("300 מילים בלי אישור — נוצרת", async () => {
    const r = await call("createAssignment", T1, { ...base, words: words("B1", 300) });
    assert.equal(r.wordCount, 300);
    assert.equal((await doc(r.assignmentId)).acknowledgedLarge, undefined);
  });

  await check("301 מילים בלי אישור — נדחית", () =>
    expectError(call("createAssignment", T1, { ...base, words: words("B1", 301) }), "FAILED_PRECONDITION", /יותר מ-300/));

  await check("301 מילים עם אישור — נוצרת, שלבים מכסים 301", async () => {
    const r = await call("createAssignment", T1, { ...base, words: words("B1", 301), acknowledgedLarge: true });
    const d = await doc(r.assignmentId);
    assert.equal(d.wordIds.length, 301);
    assert.equal(d.acknowledgedLarge, true);
    assert.equal(d.stagePlan.sizes.reduce((a, b) => a + b, 0), 301);
    assert.ok(d.stagePlan.sizes.every((n) => n >= 10 && n <= 15));
  });

  await check("1,000 מילים (שתי רשימות, עם שני האישורים) — נוצרת", async () => {
    const r = await call("createAssignment", T1, {
      ...base,
      words: [...words("B1", 400), ...words("B2", 605)], // 5 כפולות → 1,000
      acknowledgedLarge: true,
      acknowledgedMixed: true,
    });
    assert.equal(r.wordCount, 1000);
    assert.equal(r.duplicatesRemoved, 5);
  });

  await check("1,001 מילים — נדחית עם הודעה ברורה (גם עם אישורים)", () =>
    expectError(
      call("createAssignment", T1, { ...base, words: [...words("B1", 400), ...words("B2", 606)], acknowledgedLarge: true, acknowledgedMixed: true }),
      "INVALID_ARGUMENT",
      /עד 1,000 מילים \(נבחרו 1,001\)/,
    ));

  await check("Band + רשימה מוסדית בלי אישור — נדחית", () =>
    expectError(call("createAssignment", T1, { ...base, words: [...words("B1", 10), ...words("C1", 5, "custom")] }), "FAILED_PRECONDITION", /כמה רשימות/));

  await check("Band + רשימה מוסדית עם אישור — נוצרת, מקור לכל מילה", async () => {
    const r = await call("createAssignment", T1, {
      ...base,
      title: "משולבת",
      words: [...words("B1", 10), ...words("C1", 5, "custom")],
      acknowledgedMixed: true,
    });
    mixedId = r.assignmentId;
    const d = await doc(mixedId);
    assert.equal(d.acknowledgedMixed, true);
    assert.deepEqual(d.sources, [{ listId: "B1", listSource: "global" }, { listId: "C1", listSource: "custom" }]);
    assert.equal(d.wordListIds.length, 15);
    assert.deepEqual(d.wordListIds.slice(8, 12), ["B1", "B1", "C1", "C1"]);
    assert.equal(d.listId, "B1");
  });

  await check("שתי רשימות Band (כמה Bands) בלי אישור — נדחית", () =>
    expectError(call("createAssignment", T1, { ...base, words: [...words("B1", 3), ...words("B2", 20, "global", 10)] }), "FAILED_PRECONDITION"));

  await check("אותה מילה בשתי רשימות — נכנסת פעם אחת, מהרשימה הראשונה", async () => {
    const r = await call("createAssignment", T1, {
      ...base,
      words: [...words("B1", 3), ...words("B2", 5)], // B2_w0..2 = BONE0..2 = כפולות של bone0..2
      acknowledgedMixed: true,
    });
    const d = await doc(r.assignmentId);
    assert.equal(r.duplicatesRemoved, 3);
    assert.deepEqual(d.wordIds, ["B1_w0", "B1_w1", "B1_w2", "B2_w3", "B2_w4"]);
    assert.deepEqual(d.wordListIds, ["B1", "B1", "B1", "B2", "B2"]);
  });

  await check("כפילות בתוך אותה בקשה (B1 + B2 כולה כפולה) = מקור אחד בלבד → בלי צורך באישור", async () => {
    const r = await call("createAssignment", T1, { ...base, words: [...words("B1", 5), ...words("B2", 5)] });
    assert.equal(r.wordCount, 5);
    assert.deepEqual((await doc(r.assignmentId)).sources, [{ listId: "B1", listSource: "global" }]);
  });

  await check("מוסד שמסנן: מילה רגישה מהרשימה המוסדית — נדחית", () =>
    expectError(call("createAssignment", T1, { ...base, words: [...words("B1", 10), ...words("C1", 10, "custom")], acknowledgedMixed: true }), "FAILED_PRECONDITION", /תוכן מותאם/));

  await check("פורמט ישן (listId + wordIds) עדיין עובד", async () => {
    const r = await call("createAssignment", T1, { ...base, listId: "B1", listSource: "global", wordIds: ["B1_w1", "B1_w2"] });
    const d = await doc(r.assignmentId);
    assert.deepEqual(d.sources, [{ listId: "B1", listSource: "global" }]);
  });

  await check("שם משימה חסר — ההודעה מציינת בדיוק מה חסר", () =>
    expectError(call("createAssignment", T1, { classId: "K", title: "  ", words: words("B1", 3) }), "INVALID_ARGUMENT", /^חסרים שדות חובה: שם המשימה.$/));

  await check("בקשה כפולת-פורמט (words + listId/wordIds) — נוצרת פעם אחת, לפי words", async () => {
    const r = await call("createAssignment", T1, { ...base, words: words("B1", 4), listId: "B1", listSource: "global", wordIds: ["B1_w0", "B1_w1", "B1_w2", "B1_w3"] });
    assert.equal(r.wordCount, 4);
  });

  // ---- ארכיון ----
  await db.doc(`institutions/${INST}/assignments/${mixedId}/students/S`).set({ uid: "S", words: { B1_w0: { en: "bone0", seen: true, n: 1 } } });

  await check("התלמיד רואה את המשימה המשולבת, עם מקורות ומקור לכל מילה", async () => {
    const r = await call("getMyAssignments", S);
    const a = r.assignments.find((x) => x.assignmentId === mixedId);
    assert.ok(a);
    assert.equal(a.sources.length, 2);
    assert.equal(a.wordListIds.length, a.wordIds.length);
  });

  await check("מורה אחר לא יכול להעביר לארכיון", () => expectError(call("archiveAssignment", T2, { assignmentId: mixedId }), "PERMISSION_DENIED"));
  await check("תלמיד לא יכול להעביר לארכיון", () => expectError(call("archiveAssignment", S, { assignmentId: mixedId }), "PERMISSION_DENIED"));

  await check("המורה שיצר מעביר לארכיון: status/archivedAt/archivedBy, ההתקדמות נשמרת", async () => {
    await call("archiveAssignment", T1, { assignmentId: mixedId });
    const d = await doc(mixedId);
    assert.equal(d.status, "archived");
    assert.equal(d.archivedBy, "T1");
    assert.ok(d.archivedAt);
    assert.ok((await db.doc(`institutions/${INST}/assignments/${mixedId}/students/S`).get()).exists);
  });

  await check("משימה בארכיון — מוסתרת מהתלמיד", async () => {
    const r = await call("getMyAssignments", S);
    assert.equal(r.assignments.some((x) => x.assignmentId === mixedId), false);
  });

  await check("משימה בארכיון — לא בסיכום המורה", () =>
    expectError(call("getAssignmentSummary", T1, { assignmentId: mixedId, institutionId: INST }), "FAILED_PRECONDITION", /בארכיון/));

  await check("מנהל המוסד משחזר — חוזרת לתלמיד, ההתקדמות במקום", async () => {
    await call("restoreAssignment", P, { assignmentId: mixedId });
    const d = await doc(mixedId);
    assert.equal(d.status, "active");
    assert.equal(d.archivedAt, undefined);
    assert.equal(d.restoredBy, "P");
    const r = await call("getMyAssignments", S);
    assert.ok(r.assignments.some((x) => x.assignmentId === mixedId));
    const summary = await call("getAssignmentSummary", T1, { assignmentId: mixedId, institutionId: INST });
    assert.equal(summary.students.find((s) => s.uid === "S").seen, 1);
  });

  await check("מנהל יכול גם להעביר לארכיון (משימה של מורה אחר)", async () => {
    await call("archiveAssignment", P, { assignmentId: mixedId });
    assert.equal((await doc(mixedId)).status, "archived");
  });

  // ---- מחיקה לצמיתות ----
  // mixedId בארכיון עכשיו (המנהל העביר). התקדמות גלובלית של התלמיד — לוודא שלא נמחקת.
  await db.doc("users/S/progress/bone0").set({ englishWord: "bone0", sourceListId: "B1", correctAttempts: 2, totalAttempts: 3 });
  const active = await call("createAssignment", T1, { ...base, title: "פעילה", words: words("B1", 12) });

  await check("מחיקה לצמיתות של משימה פעילה (לא בארכיון) — נדחית", () =>
    expectError(call("deleteAssignmentPermanently", T1, { assignmentId: active.assignmentId }), "FAILED_PRECONDITION", /רק משימה שנמצאת בארכיון/));
  await check("מורה אחר — לא יכול למחוק לצמיתות", () =>
    expectError(call("deleteAssignmentPermanently", T2, { assignmentId: mixedId }), "PERMISSION_DENIED"));
  await check("תלמיד — לא יכול למחוק לצמיתות", () =>
    expectError(call("deleteAssignmentPermanently", S, { assignmentId: mixedId }), "PERMISSION_DENIED"));

  await check("המורה שיצר מוחק לצמיתות משימה בארכיון: משימה + students/* נמחקים, users/*/progress נשאר, רישום נכתב", async () => {
    const before = await doc(mixedId);
    const r = await call("deleteAssignmentPermanently", T1, { assignmentId: mixedId });
    assert.equal(r.deleted, true);
    assert.equal(r.studentProgressDocs, 1);
    assert.equal((await db.doc(`institutions/${INST}/assignments/${mixedId}`).get()).exists, false);
    assert.equal((await db.collection(`institutions/${INST}/assignments/${mixedId}/students`).get()).size, 0);
    assert.equal((await db.doc("users/S/progress/bone0").get()).data().correctAttempts, 2);
    const log = (await db.doc(`institutions/${INST}/deletionLog/${r.logId}`).get()).data();
    assert.equal(log.assignmentId, mixedId);
    assert.equal(log.title, before.title);
    assert.equal(log.wordCount, before.wordIds.length);
    assert.equal(log.deletedBy, "T1");
    assert.ok(log.deletedAt);
  });

  await check("מנהל המוסד יכול למחוק לצמיתות משימה בארכיון של מורה אחר", async () => {
    await call("archiveAssignment", T1, { assignmentId: active.assignmentId });
    const r = await call("deleteAssignmentPermanently", P, { assignmentId: active.assignmentId });
    assert.equal(r.deleted, true);
    assert.equal((await db.doc(`institutions/${INST}/assignments/${active.assignmentId}`).get()).exists, false);
  });

  await check("משימה שכבר נמחקה — not-found", () =>
    expectError(call("deleteAssignmentPermanently", T1, { assignmentId: mixedId }), "NOT_FOUND"));

  console.log(results.join("\n"));
  const failed = results.filter((r) => r.startsWith("FAIL")).length;
  console.log(`\n${results.length - failed}/${results.length} passed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => {
  console.error("FATAL", e);
  process.exit(1);
});
