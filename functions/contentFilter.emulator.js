// בדיקת קצה-לקצה ל"תוכן מותאם לבתי ספר דתיים" מול האמולטורים (Firestore + Auth + Functions), בפרויקט
// דמו מקומי — לא נוגעת בפרויקט האמיתי. מריצים מתיקיית השורש של EasyLex:
//   npx -y firebase-tools@15.24.0 emulators:exec --only firestore,auth,functions --project demo-easylex "node functions/contentFilter.emulator.js"
// מוסד A: ההגדרה פעילה; מוסד B: כבויה. בכל אחד משימה קיימת עם 30 מילים, מתוכן 2 מתויגות sensitive.
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

async function seed() {
  const ids = Array.from({ length: 30 }, (_, i) => `w${i}`);
  const batch = db.batch();
  batch.set(db.doc("word_lists/L"), { wordCount: 30 });
  ids.forEach((id, i) => batch.set(db.doc(`word_lists/L/words/${id}`), {
    englishWord: `word${i}`,
    hebrewTranslation: `מילה${i}`,
    tags: i === 3 || i === 20 ? "General, sensitive" : "General",
  }));
  for (const [inst, hide] of [["A", true], ["B", false]]) {
    batch.set(db.doc(`institutions/${inst}`), hide ? { name: inst, hideSensitiveWords: true } : { name: inst });
    batch.set(db.doc(`institutions/${inst}/classes/c${inst}`), { name: `class ${inst}`, teacherId: `t${inst}` });
    batch.set(db.doc(`institutions/${inst}/assignments/a${inst}`), {
      title: "existing", classId: `c${inst}`, listId: "L", listSource: "global", wordIds: ids,
      stagePlan: { sizes: [15, 15] }, status: "active", createdAt: admin.firestore.Timestamp.now(),
    });
    for (const s of [`s${inst}1`, `s${inst}2`]) {
      batch.set(db.doc(`users/${s}`), { role: "student", institutionId: inst, classIds: [`c${inst}`], displayName: s });
      // התקדמות במשימה: התלמיד ראה את כל המילים כולל המילה הרגישה w3
      batch.set(db.doc(`institutions/${inst}/assignments/a${inst}/students/${s}`), {
        words: Object.fromEntries(ids.map((id) => [id, { seen: true, n: 4, wrong: 2, correctDays: ["2026-11-10"], en: `word${id.slice(1)}` }])),
      });
      // progress גלובלי (מפת המילים הקשות): word3 (רגישה) ו-word5 (רגילה) עם הרבה טעויות
      for (const w of ["word3", "word5"]) {
        batch.set(db.doc(`users/${s}/progress/${w}`), { englishWord: w, sourceListId: "L", totalAttempts: 6, correctAttempts: 1 });
      }
    }
    batch.set(db.doc(`users/t${inst}`), { role: "teacher", institutionId: inst, displayName: `t${inst}` });
    batch.set(db.doc(`users/p${inst}`), { role: "principal", institutionId: inst, displayName: `p${inst}` });
  }
  await batch.commit();
}

(async () => {
  await seed();
  const tok = {};
  for (const inst of ["A", "B"]) {
    tok[`s${inst}`] = await tokenFor(`s${inst}1`, { role: "student", institutionId: inst });
    tok[`t${inst}`] = await tokenFor(`t${inst}`, { role: "teacher", institutionId: inst });
    tok[`p${inst}`] = await tokenFor(`p${inst}`, { role: "principal", institutionId: inst });
  }

  await check("ON — existing assignment: student gets 28 words, no sensitive ids, stages [14,14]", async () => {
    const a = (await call("getMyAssignments", tok.sA)).assignments[0];
    assert.equal(a.wordIds.length, 28);
    assert.ok(!a.wordIds.includes("w3") && !a.wordIds.includes("w20"));
    assert.deepEqual(a.stagePlan.sizes, [14, 14]);
    assert.equal(a.wordIds[14], "w15"); // שלב 2 עדיין מתחיל ב-w15 — אף מילה לא זזה שלב
  });
  await check("OFF — existing assignment unchanged (30 words, stages [15,15])", async () => {
    const a = (await call("getMyAssignments", tok.sB)).assignments[0];
    assert.equal(a.wordIds.length, 30);
    assert.deepEqual(a.stagePlan.sizes, [15, 15]);
  });
  await check("ON — createAssignment with a sensitive word is refused (modified client)", async () => {
    await assert.rejects(
      call("createAssignment", tok.tA, { classId: "cA", listId: "L", title: "x", wordIds: ["w1", "w3"] }),
      (e) => e.status === "FAILED_PRECONDITION",
    );
  });
  await check("ON — createAssignment without sensitive words works", async () => {
    const r = await call("createAssignment", tok.tA, { classId: "cA", listId: "L", title: "ok", wordIds: ["w1", "w2"] });
    assert.ok(r.assignmentId);
  });
  await check("OFF — createAssignment with a sensitive word works", async () => {
    const r = await call("createAssignment", tok.tB, { classId: "cB", listId: "L", title: "ok", wordIds: ["w1", "w3"] });
    assert.ok(r.assignmentId);
  });
  await check("ON — teacher summary counts 28 words / 2 stages and ignores progress on hidden words", async () => {
    const s = await call("getAssignmentSummary", tok.tA, { assignmentId: "aA", institutionId: "A" });
    assert.equal(s.totalWords, 28);
    assert.equal(s.stageCount, 2);
    assert.ok(s.students.every((st) => st.seen === 28));
    assert.ok(!s.hardWords.some((w) => w.englishWord === "word3" || w.englishWord === "word20"));
  });
  await check("OFF — teacher summary counts all 30 words", async () => {
    const s = await call("getAssignmentSummary", tok.tB, { assignmentId: "aB", institutionId: "B" });
    assert.equal(s.totalWords, 30);
  });
  await check("ON — class hard words hide the sensitive word; OFF — it is shown", async () => {
    const on = (await call("getClassHardWords", tok.tA, { classId: "cA", institutionId: "A" })).words.map((w) => w.englishWord);
    const off = (await call("getClassHardWords", tok.tB, { classId: "cB", institutionId: "B" })).words.map((w) => w.englishWord);
    assert.deepEqual(on.sort(), ["word5"]);
    assert.deepEqual(off.sort(), ["word3", "word5"]);
  });
  await check("teacher (not principal) cannot change the institution setting", async () => {
    await assert.rejects(call("setInstitutionContentSettings", tok.tB, { hideSensitiveWords: true }), (e) => e.status === "PERMISSION_DENIED");
  });
  await check("principal turns the setting ON for institution B → its students stop seeing sensitive words", async () => {
    await call("setInstitutionContentSettings", tok.pB, { hideSensitiveWords: true });
    assert.equal((await db.doc("institutions/B").get()).get("hideSensitiveWords"), true);
    const a = (await call("getMyAssignments", tok.sB)).assignments.find((x) => x.assignmentId === "aB");
    assert.equal(a.wordIds.length, 28);
  });
  await check("principal turns it OFF again → back to 30 words", async () => {
    await call("setInstitutionContentSettings", tok.pB, { hideSensitiveWords: false });
    const a = (await call("getMyAssignments", tok.sB)).assignments.find((x) => x.assignmentId === "aB");
    assert.equal(a.wordIds.length, 30);
  });

  console.log(results.join("\n"));
  const failed = results.filter((r) => r.startsWith("FAIL")).length;
  console.log(`\n${results.length - failed}/${results.length} passed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => {
  console.error("FAILED:", e);
  process.exit(1);
});
