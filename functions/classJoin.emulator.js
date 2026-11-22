// בדיקת קצה-לקצה להצטרפות לכיתה בקוד (checkClassCode / joinClass) מול האמולטורים — פרויקט דמו מקומי בלבד.
//   npx -y firebase-tools@15.24.0 emulators:exec --only firestore,auth,functions --project demo-easylex "node functions/classJoin.emulator.js"
const assert = require("node:assert/strict");
const admin = require("firebase-admin");

const PROJECT = process.env.GCLOUD_PROJECT || "demo-easylex";
const FN = `http://${process.env.FUNCTIONS_EMULATOR_HOST || "127.0.0.1:5001"}/${PROJECT}/europe-west1`;
const AUTH = `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099"}`;
admin.initializeApp({ projectId: PROJECT });
const db = admin.firestore();
const results = [];
const check = (name, fn) =>
  Promise.resolve()
    .then(fn)
    .then(() => results.push(`PASS  ${name}`))
    .catch((e) => results.push(`FAIL  ${name} — ${e.message}`));

async function tokenFor(uid) {
  await admin.auth().createUser({ uid }).catch(() => {});
  const custom = await admin.auth().createCustomToken(uid);
  const res = await fetch(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=fake`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token: custom, returnSecureToken: true }),
  });
  return (await res.json()).idToken;
}
async function call(name, token, data) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${FN}/${name}`, { method: "POST", headers, body: JSON.stringify({ data }) });
  const body = await res.json();
  if (body.error) {
    const err = new Error(body.error.message);
    err.status = body.error.status;
    err.reason = body.error.details && body.error.details.reason;
    throw err;
  }
  return body.result;
}
async function expectReason(promise, status, reason, re) {
  try {
    await promise;
  } catch (e) {
    assert.equal(e.status, status, `${e.status}: ${e.message}`);
    assert.equal(e.reason, reason);
    if (re) assert.match(e.message, re);
    return;
  }
  throw new Error("expected an error");
}

(async () => {
  const batch = db.batch();
  batch.set(db.doc("institutions/A"), { name: "A" });
  batch.set(db.doc("institutions/A/classes/open"), { name: "ז1", joinCode: "AB-12345", archived: false, teacherId: "t" });
  batch.set(db.doc("institutions/A/classes/closed"), { name: "ח1", joinCode: "CD-23456", archived: true, teacherId: "t" });
  batch.set(db.doc("institutions/B"), { name: "B", disabled: true });
  batch.set(db.doc("institutions/B/classes/k"), { name: "ט1", joinCode: "EF-34567", archived: false, teacherId: "t2" });
  await batch.commit();

  // ---- checkClassCode (no account) ----
  await check("checkClassCode: valid code, without login → ok", async () => assert.deepEqual(await call("checkClassCode", null, { joinCode: "AB-12345" }), { ok: true }));
  await check("checkClassCode: lowercase, no hyphen → normalized, ok", async () => assert.equal((await call("checkClassCode", null, { joinCode: "ab12345" })).ok, true));
  await check("checkClassCode: unknown code → not-found + the requested message", () =>
    expectReason(call("checkClassCode", null, { joinCode: "ZZ-99999" }), "NOT_FOUND", "not-found", /^קוד הכיתה לא נמצא\. בדקו את הקוד עם המורה ונסו שוב\.$/));
  await check("checkClassCode: closed (archived) class → class-closed", () =>
    expectReason(call("checkClassCode", null, { joinCode: "CD-23456" }), "FAILED_PRECONDITION", "class-closed", /סגורה/));
  await check("checkClassCode: disabled institution → institution-closed", () =>
    expectReason(call("checkClassCode", null, { joinCode: "EF-34567" }), "FAILED_PRECONDITION", "institution-closed", /לא מקבל כרגע תלמידים חדשים/));
  await check("checkClassCode: empty → missing", () => expectReason(call("checkClassCode", null, { joinCode: "  " }), "INVALID_ARGUMENT", "missing"));
  await check("checkClassCode returns nothing about the class (no name/institution)", async () => {
    const r = await call("checkClassCode", null, { joinCode: "AB-12345" });
    assert.deepEqual(Object.keys(r), ["ok"]);
  });

  // ---- joinClass (after login) ----
  const S1 = await tokenFor("s1");
  await check("joinClass: unknown code → not-found, student not attached", async () => {
    await expectReason(call("joinClass", S1, { joinCode: "ZZ-99999" }), "NOT_FOUND", "not-found");
    assert.equal(((await db.doc("users/s1").get()).data() || {}).classIds, undefined);
  });
  await check("joinClass: closed class → class-closed", () => expectReason(call("joinClass", S1, { joinCode: "CD-23456" }), "FAILED_PRECONDITION", "class-closed"));
  await check("joinClass: disabled institution → institution-closed", () => expectReason(call("joinClass", S1, { joinCode: "EF-34567" }), "FAILED_PRECONDITION", "institution-closed"));
  await check("joinClass: valid code (no hyphen) → joined class + institution, role student", async () => {
    const r = await call("joinClass", S1, { joinCode: "ab12345" });
    assert.equal(r.classId, "open");
    const u = (await db.doc("users/s1").get()).data();
    assert.deepEqual(u.classIds, ["open"]);
    assert.equal(u.institutionId, "A");
    assert.equal(u.role, "student");
  });
  await check("joinClass: rate limit (5 attempts / 10 min) → rate-limited", async () => {
    const S2 = await tokenFor("s2");
    for (let i = 0; i < 5; i++) await call("joinClass", S2, { joinCode: "ZZ-00000" }).catch(() => {});
    await expectReason(call("joinClass", S2, { joinCode: "AB-12345" }), "RESOURCE_EXHAUSTED", "rate-limited");
  });
  await check("checkClassCode: rate limit per IP (20 / 10 min) → rate-limited", async () => {
    // 7 calls so far from this IP above; fill up to 20, then the 21st is blocked
    let blocked = null;
    for (let i = 0; i < 25 && !blocked; i++) {
      await call("checkClassCode", null, { joinCode: "AB-12345" }).catch((e) => (blocked = e));
    }
    assert.ok(blocked, "expected to be rate-limited");
    assert.equal(blocked.reason, "rate-limited");
  });

  console.log(results.join("\n"));
  const failed = results.filter((r) => r.startsWith("FAIL")).length;
  console.log(`\n${results.length - failed}/${results.length} passed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => {
  console.error("FATAL", e);
  process.exit(1);
});
