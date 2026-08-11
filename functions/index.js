/**
 * =====================================================================
 * syncUserClaims — משימה 0.5 (שלב 0א, איפיון הפלטפורמה)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * מופעלת אוטומטית בכל כתיבה למסמך users/{uid} (יצירה/עדכון/מחיקה).
 * קוראת את role ואת institutionId (אם קיים) מהמסמך אחרי הכתיבה, ומסנכרנת
 * אותם ל-Custom Claims של המשתמש ב-Firebase Auth. כך ה-Security Rules
 * (firestore.rules) יוכלו בהמשך (משימה 0.6) לבדוק request.auth.token.role
 * במקום לבצע get() על מסמך users/{uid} בכל בקשה — זול יותר, בלי תלות
 * במסמך Firestore נוסף.
 *
 * מתי claims מתאפסים ל-{}?
 * --------------------------
 *   - המסמך נמחק (event.data.after לא קיים).
 *   - למסמך אין לא role ולא institutionId.
 * במקרים האלה ברירת המחדל הבטוחה — "student" — ממשיכה לחול בצד לקוח
 * דרך UserRoleManager (ר' data/UserRoleManager.java), לא כאן.
 *
 * תיקון: role ו-institutionId מסונכרנים בנפרד, לא תלויים זה בזה
 * ------------------------------------------------------------------
 * גרסה קודמת סנכרנה institutionId רק אם role היה קיים על המסמך. זה שבר
 * תלמיד שמצטרף לכיתה דרך joinClass (functions/index.js) — הפונקציה ההיא
 * כותבת institutionId/classIds אבל לא נגעה ב-role בכלל, אז הטריגר הזה
 * ראה "אין role" והשאיר את ה-Claims ריקים, כולל institutionId שכן היה
 * על המסמך. כל Cloud Function שבודקת request.auth.token.institutionId
 * (כמו getMyAssignments) נכשלה בשקט בשביל חשבונות כאלה, ללא תלות בזמן —
 * לא בעיית תזמון, הערך פשוט לא היה שם. עכשיו כל שדה מסונכרן בנפרד.
 *
 * הערה על עדכון הטוקן בצד לקוח:
 * -------------------------------
 * Custom Claims לא נכנסים לתוקף מיידית ב-ID token הקיים אצל המשתמש —
 * צריך getIdToken(true) (force refresh) בצד לקוח כדי לראות אותם.
 * =====================================================================
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * =====================================================================
 * sendPushToTokens — עזר משותף ל-Push (אפליקציית תלמיד)
 * =====================================================================
 * שולחת notification ל-FCM tokens נתונים, בצ'אנקים של עד 500 (מגבלת
 * sendEachForMulticast). tokens לא-תקינים/פגי-תוקף פשוט נכשלים בשקט
 * בתוך ה-response (successCount/failureCount) — לא נשלף מה-Firestore
 * שדה fcmToken בעל תוקף שגוי אוטומטית כאן, לא בתחום v1.
 */
async function sendPushToTokens(tokens, notification) {
  logger.info(`sendPushToTokens: ${tokens ? tokens.length : 0} tokens נמצאו לשליחה`, { notification });
  if (!tokens || tokens.length === 0) return { successCount: 0, failureCount: 0 };
  const CHUNK_SIZE = 500;
  let successCount = 0;
  let failureCount = 0;
  for (let i = 0; i < tokens.length; i += CHUNK_SIZE) {
    const chunk = tokens.slice(i, i + CHUNK_SIZE);
    const response = await admin.messaging().sendEachForMulticast({ notification, tokens: chunk });
    successCount += response.successCount;
    failureCount += response.failureCount;
    // FCM response המלא לצ'אנק — כולל error.code/message לכל token שנכשל
    // (למשל invalid-registration-token / registration-token-not-registered),
    // קריטי לאבחון פעם ראשונה שיש טוקנים בפועל אבל ה-push עדיין לא מגיע.
    response.responses.forEach((r, idx) => {
      if (!r.success) {
        logger.warn(`sendPushToTokens: token ${chunk[idx].substring(0, 15)}... נכשל`, {
          code: r.error && r.error.code,
          message: r.error && r.error.message,
        });
      }
    });
  }
  return { successCount, failureCount };
}

/**
 * =====================================================================
 * getTokensForUids — עזר משותף ל-Push, מולטי-מכשיר
 * =====================================================================
 * מחליף את הקריאה הישנה לשדה היחיד users/{uid}.fcmToken (נדרס בכל מכשיר
 * חדש). קורא את users/{uid}/tokens/{tokenId} לכל uid נתון ומחזיר מערך
 * שטוח של כל הטוקנים (כל המכשירים, כל המשתמשים).
 */
async function getTokensForUids(uids) {
  const tokensPerUser = await Promise.all(
    uids.map(async (uid) => {
      const snap = await admin.firestore().collection("users").doc(uid).collection("tokens").get();
      return snap.docs.map((d) => d.data().token).filter(Boolean);
    })
  );
  return tokensPerUser.flat();
}

function todayKeyIsrael() {
  // en-CA מפרמט כ-YYYY-MM-DD — אותו מבנה בדיוק כמו toDateKey() בצד לקוח
  // (src/lib/dateUtils.js), רק עם timeZone מפורש כדי שה"יום" יחושב לפי
  // השעון בישראל ולא UTC (הפונקציה רצה על שרתי Firebase, לאו דווקא UTC+2/3).
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Jerusalem" }).format(new Date());
}

exports.syncUserClaims = onDocumentWritten(
  {
    document: "users/{uid}",
    region: "europe-west1",
  },
  async (event) => {
    const uid = event.params.uid;

    try {
      const afterSnap = event.data && event.data.after;
      const afterExists = !!(afterSnap && afterSnap.exists);

      let claims = {};

      if (afterExists) {
        const data = afterSnap.data() || {};
        if (data.role) {
          claims.role = data.role;
        }
        if (data.institutionId) {
          claims.institutionId = data.institutionId;
        }
        // אין role ואין institutionId על המסמך → claims נשארים {} (אותחל למעלה).
      }
      // afterExists === false (המסמך נמחק) → claims נשארים {} (אותחל למעלה).

      await admin.auth().setCustomUserClaims(uid, claims);
      logger.info(`syncUserClaims: עודכנו claims עבור ${uid}`, claims);
    } catch (err) {
      // לא זורקים הלאה — כשל בסנכרון claims (למשל uid לא קיים ב-Auth)
      // לא אמור לגרום ל-retry אינסופי או להשפיע על שאר האפליקציה.
      // ה-Rules הישנות (get() על המסמך, לפני משימה 0.6) ממשיכות לעבוד
      // כ-fallback עד שהסנכרון יצליח בפעם הבאה שהמסמך ייכתב.
      logger.error(`syncUserClaims: נכשל עבור ${uid}`, err);
    }
  }
);

/**
 * =====================================================================
 * joinAsTeacher — משימה 0.8 (שלב 0ב, איפיון הפלטפורמה)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * Callable Function שמורה קורא לה מהאפליקציה עם קוד מורים (teacherJoinCode)
 * שקיבל מהמנהל (נוצר במשימה 0.7, POST /create-institution). מחפשת מוסד עם
 * הקוד, ואם נמצא — מקדמת את המשתמש הקורא ל-role: "teacher" + institutionId.
 * הכתיבה ל-users/{uid} מפעילה אוטומטית את syncUserClaims (0.5) — ה-Custom
 * Claims מתעדכנים בלי צורך בקוד נוסף כאן.
 *
 * הגבלת קצב:
 * -----------
 * עד 5 ניסיונות ב-10 דקות, פר-משתמש (uid). נשמר בשדה teacherJoinAttempts
 * על מסמך users/{uid} עצמו (מערך של timestamps במילישניות) — מסונן לחלון
 * הזמן הרלוונטי בכל קריאה, כך שהוא "מתנקה" מעצמו ולא גדל בלי גבול.
 * זו הגנה בסיסית מפני ניחוש קודים בלולאה, לא הגנה קריפטוגרפית — לכן בלי
 * טרנזקציה (לא קריטי ל-race condition בין קריאות בו-זמניות של אותו uid).
 * =====================================================================
 */

const TEACHER_JOIN_RATE_LIMIT_MAX_ATTEMPTS = 5;
const TEACHER_JOIN_RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 דקות

exports.joinAsTeacher = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר לפני הצטרפות למוסד.");
    }
    const uid = request.auth.uid;

    const rawCode = (request.data && request.data.teacherJoinCode) || "";
    const teacherJoinCode = String(rawCode).trim().toUpperCase();
    if (!teacherJoinCode) {
      throw new HttpsError("invalid-argument", "חסר קוד הצטרפות.");
    }

    const userRef = admin.firestore().collection("users").doc(uid);
    const userSnap = await userRef.get();

    // הגבלת קצב
    const now = Date.now();
    const prevAttempts = (userSnap.exists && userSnap.data().teacherJoinAttempts) || [];
    const recentAttempts = prevAttempts.filter(
      (ts) => now - ts < TEACHER_JOIN_RATE_LIMIT_WINDOW_MS
    );

    if (recentAttempts.length >= TEACHER_JOIN_RATE_LIMIT_MAX_ATTEMPTS) {
      logger.warn(`joinAsTeacher: ${uid} חסום זמנית — יותר מדי ניסיונות`);
      throw new HttpsError(
        "resource-exhausted",
        "יותר מדי ניסיונות הצטרפות. נסה שוב בעוד כמה דקות."
      );
    }
    recentAttempts.push(now);

    // חיפוש מוסד עם הקוד המבוקש.
    const instQuery = await admin
      .firestore()
      .collection("institutions")
      .where("teacherJoinCode", "==", teacherJoinCode)
      .limit(1)
      .get();

    if (instQuery.empty) {
      // רושמים את הניסיון גם כשהקוד שגוי — נספר לצורך הגבלת הקצב.
      await userRef.set({ teacherJoinAttempts: recentAttempts }, { merge: true });
      throw new HttpsError("not-found", "קוד ההצטרפות לא נמצא. ודא שהקלדת אותו נכון.");
    }

    const instDoc = instQuery.docs[0];
    const institutionId = instDoc.id;
    const institutionName = instDoc.data().name || "";

    await userRef.set(
      {
        teacherJoinAttempts: recentAttempts,
        role: "teacher",
        institutionId,
      },
      { merge: true }
    );

    logger.info(`joinAsTeacher: ${uid} הצטרף למוסד ${institutionId} כמורה`);

    return { institutionId, institutionName };
  }
);

/**
 * =====================================================================
 * createClass — משימה 0.9 (שלב 0ב, איפיון הפלטפורמה)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * Callable Function שמורה/מנהל קורא לה כדי ליצור כיתה חדשה במוסד שלו.
 * ההרשאה נבדקת דרך Custom Claims (role, institutionId) — לא get() על
 * המסמך — עקבי עם גישת 0.6. חשוב: יש כאן תלות בעקיפין ב-0.5 (הפצת
 * claims) — מורה שהצטרף הרגע (joinAsTeacher, 0.8) וקורא ל-createClass
 * מיד אחרי, בלי getIdToken(true) קודם, עלול לקבל role ישן מהטוקן.
 * JoinInstitutionFragment כבר מרענן טוקן אחרי הצטרפות מוצלחת בדיוק בגלל
 * זה — ר' הערה שם.
 *
 * ייחודיות joinCode:
 * -------------------
 * נבדקת רק בתוך המוסד (לא גלובלית) — לפי הדרישה המפורשת של המשימה.
 * הערה קדימה: כשתלמיד יצטרף עם קוד כיתה (משימה עתידית), החיפוש יצטרך
 * להיות מוגבל למוסד ידוע מראש, כי קודי כיתה לא ייחודיים בין מוסדות.
 * =====================================================================
 */

const CLASS_JOIN_CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // בלי I, O
const CLASS_JOIN_CODE_ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // בלי 0,1,O,I,L

function generateClassJoinCode() {
  let part1 = "";
  for (let i = 0; i < 2; i++) {
    part1 += CLASS_JOIN_CODE_LETTERS[Math.floor(Math.random() * CLASS_JOIN_CODE_LETTERS.length)];
  }
  let part2 = "";
  for (let i = 0; i < 5; i++) {
    part2 += CLASS_JOIN_CODE_ALNUM[Math.floor(Math.random() * CLASS_JOIN_CODE_ALNUM.length)];
  }
  return `${part1}-${part2}`;
}

async function generateUniqueClassJoinCode(institutionId) {
  const MAX_ATTEMPTS = 5;
  const classesRef = admin
    .firestore()
    .collection("institutions")
    .doc(institutionId)
    .collection("classes");

  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    const code = generateClassJoinCode();
    const existing = await classesRef.where("joinCode", "==", code).limit(1).get();
    if (existing.empty) return code;
  }
  throw new HttpsError("internal", "לא הצלחנו להפיק קוד כיתה ייחודי — נסה שוב.");
}

exports.createClass = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר כדי ליצור כיתה.");
    }

    const role = request.auth.token.role;
    if (role !== "teacher" && role !== "principal") {
      throw new HttpsError("permission-denied", "רק מורה או מנהל מוסד יכולים ליצור כיתה.");
    }

    const institutionId = request.auth.token.institutionId;
    if (!institutionId) {
      throw new HttpsError("failed-precondition", "החשבון שלך אינו משויך למוסד.");
    }

    const className = String((request.data && request.data.className) || "").trim();
    const grade = String((request.data && request.data.grade) || "").trim();
    if (!className) {
      throw new HttpsError("invalid-argument", "חסר שם כיתה.");
    }

    const joinCode = await generateUniqueClassJoinCode(institutionId);

    const classRef = admin
      .firestore()
      .collection("institutions")
      .doc(institutionId)
      .collection("classes")
      .doc();

    await classRef.set({
      name: className,
      grade,
      teacherId: request.auth.uid,
      studentCount: 0,
      archived: false,
      joinCode,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    logger.info(`createClass: ${request.auth.uid} יצר כיתה ${classRef.id} במוסד ${institutionId}`);

    return { classId: classRef.id, joinCode };
  }
);

/**
 * =====================================================================
 * joinClass — משימה 0.10 (סיום שלב 0, איפיון הפלטפורמה)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * Callable Function שתלמיד קורא לה עם קוד כיתה (joinCode, נוצר ב-
 * createClass — משימה 0.9). מוצא את הכיתה בכל המוסדות (collectionGroup
 * query — ר' הערה למטה), מוסיף את classId ל-classIds[] של המשתמש
 * (arrayUnion, לא דורס כיתות קודמות), וקובע institutionId רק אם עוד
 * אין למשתמש אחד (תלמיד יכול להיות בכמה כיתות, אבל institutionId הוא
 * שדה יחיד — ר' דוח ההיתכנות למגבלה הידועה במקרה-קצה של מוסדות שונים).
 * role לא נוגע בכלל — נשאר מה שהיה (student כברירת מחדל בטוחה בצד לקוח,
 * UserRoleManager).
 *
 * למה collectionGroup ולא get() ישיר?
 * -------------------------------------
 * joinCode ייחודי רק בתוך מוסד (לא גלובלית — בניגוד ל-teacherJoinCode,
 * שכן ייחודי גלובלית ב-0.7). תלמיד שמזין קוד לא יודע (ולא צריך לדעת)
 * לאיזה מוסד הכיתה שייכת, אז אין דרך לבנות path ישיר כמו
 * institutions/{instId}/classes/{classId} מראש. collectionGroup("classes")
 * מחפש בכל תת-אוספי "classes" תחת כל מוסד בבת אחת. דורש אינדקס ייעודי
 * (firestore.indexes.json, נפרס בנפרד מ-Rules/Functions).
 *
 * הגנה מפני ניפוח studentCount:
 * -------------------------------
 * אם התלמיד כבר חבר בכיתה הזו (למשל לחיצה כפולה על הכפתור, או הזנת
 * אותו קוד פעמיים בטעות) — לא מגדילים את studentCount שוב. arrayUnion
 * עצמו כבר לא-יוצר-כפילות ל-classIds, אבל studentCount הוא contler
 * נפרד שדורש בדיקה מפורשת לפני ההגדלה.
 * =====================================================================
 */

const CLASS_JOIN_RATE_LIMIT_MAX_ATTEMPTS = 5;
const CLASS_JOIN_RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 דקות

exports.joinClass = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר לפני הצטרפות לכיתה.");
    }
    const uid = request.auth.uid;

    const rawCode = (request.data && request.data.joinCode) || "";
    const joinCode = String(rawCode).trim().toUpperCase();
    if (!joinCode) {
      throw new HttpsError("invalid-argument", "חסר קוד כיתה.");
    }

    const userRef = admin.firestore().collection("users").doc(uid);
    const userSnap = await userRef.get();
    const userData = userSnap.exists ? userSnap.data() : {};

    // הגבלת קצב — שדה נפרד מ-teacherJoinAttempts (0.8), כדי שניסיונות
    // הצטרפות לכיתה וניסיונות הצטרפות כמורה לא "יצרכו" זה מהמכסה של זה.
    const now = Date.now();
    const prevAttempts = userData.classJoinAttempts || [];
    const recentAttempts = prevAttempts.filter(
      (ts) => now - ts < CLASS_JOIN_RATE_LIMIT_WINDOW_MS
    );

    if (recentAttempts.length >= CLASS_JOIN_RATE_LIMIT_MAX_ATTEMPTS) {
      logger.warn(`joinClass: ${uid} חסום זמנית — יותר מדי ניסיונות`);
      throw new HttpsError(
        "resource-exhausted",
        "יותר מדי ניסיונות הצטרפות. נסה שוב בעוד כמה דקות."
      );
    }
    recentAttempts.push(now);

    // חיפוש הכיתה בכל המוסדות.
    const classQuery = await admin
      .firestore()
      .collectionGroup("classes")
      .where("joinCode", "==", joinCode)
      .where("archived", "==", false)
      .limit(1)
      .get();

    if (classQuery.empty) {
      // רושמים את הניסיון גם כשהקוד שגוי — נספר לצורך הגבלת הקצב.
      await userRef.set({ classJoinAttempts: recentAttempts }, { merge: true });
      throw new HttpsError("not-found", "קוד הכיתה לא נמצא. ודא שהקלדת אותו נכון.");
    }

    const classDoc = classQuery.docs[0];
    const classId = classDoc.id;
    const className = classDoc.data().name || "";
    // institutions/{instId}/classes/{classId} — ה-ref.parent.parent הוא מסמך המוסד.
    const institutionId = classDoc.ref.parent.parent.id;

    const existingClassIds = userData.classIds || [];
    const alreadyMember = existingClassIds.includes(classId);

    const userUpdate = {
      classJoinAttempts: recentAttempts,
      classIds: admin.firestore.FieldValue.arrayUnion(classId),
    };
    if (!userData.institutionId) {
      userUpdate.institutionId = institutionId;
    }
    // אם אין למשתמש role בכלל (למשל הצטרפות ראשונה מיד אחרי הרשמה) — קובעים
    // "student" במפורש. לא דורסים role קיים (teacher/principal/superadmin
    // וכו') — התנאי שומר על כך. הכתיבה עצמה מבטיחה שלמסמך תמיד יהיה role
    // אחרי הצטרפות לכיתה, כך שגם institutionId יסונכרן ל-Custom Claims
    // (ר' syncUserClaims למעלה בקובץ).
    if (!userData.role) {
      userUpdate.role = "student";
    }

    await userRef.set(userUpdate, { merge: true });

    if (!alreadyMember) {
      await classDoc.ref.update({
        studentCount: admin.firestore.FieldValue.increment(1),
      });
    }

    logger.info(`joinClass: ${uid} הצטרף לכיתה ${classId} (מוסד ${institutionId})`);

    return { classId, className, institutionId };
  }
);

/**
 * =====================================================================
 * createAssignment — משימה 0.14 (שלב 2, מערכת משימות)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * מורה/מנהל יוצרים משימת תרגול לכיתה: אילו מילים (רשימה שלמה, כרגע —
 * בחירה ספציפית מתוכננת לעתיד, wordIds ריק = כל הרשימה), שם, ודד-ליין
 * אופציונלי. נכתב ל-institutions/{institutionId}/assignments/{id} —
 * מבנה "שטוח" (לא מקונן תחת classes/{classId}), עם שדה classId על
 * המסמך עצמו לסינון. זה בכוונה: firestore.rules (משימה 0.6) וה-
 * emulator tests (rules-tests/firestore.rules.test.js, תרחישים 8-9)
 * כבר בנויים בדיוק על המבנה השטוח הזה — שינוי המבנה היה דורש לשנות
 * גם את הכללים וגם את הבדיקות הקיימות, בניגוד לעקרון "שינויים מינימליים".
 *
 * למה classId על המסמך ולא רק ב-path?
 * -------------------------------------
 * כי המבנה שטוח (לא מקונן תחת הכיתה) — התלמיד/המורה מסננים לפי classId
 * כשדה שאילתה רגיל. גם ה-Rule הקיים (`resource.data.classId in
 * myClassIds()`) תלוי בשדה הזה קיים על כל מסמך.
 */
exports.createAssignment = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר כדי ליצור משימה.");
    }

    const role = request.auth.token.role;
    if (role !== "teacher" && role !== "principal") {
      throw new HttpsError("permission-denied", "רק מורה או מנהל יכולים ליצור משימה.");
    }

    const institutionId = request.auth.token.institutionId;
    if (!institutionId) {
      throw new HttpsError("failed-precondition", "החשבון שלך אינו משויך למוסד.");
    }

    const data = request.data || {};
    const classId = data.classId;
    const listId = data.listId;
    const title = typeof data.title === "string" ? data.title.trim() : "";
    const wordIds = Array.isArray(data.wordIds) ? data.wordIds : [];
    const dueDateMs = typeof data.dueDateMs === "number" ? data.dueDateMs : null;
    // practiceMode: "varied" — מורה בחר "מגוון אוטומטי" בשלב 3 של האשף
    // (StepFinalize.jsx). לא אוכף חלוקה בפועל בין מודולים — רק דגל
    // תצוגה, ר' getMyAssignments/PracticePicker.
    const practiceMode = data.practiceMode === "varied" ? "varied" : null;

    if (!classId || !listId || !title) {
      throw new HttpsError(
        "invalid-argument",
        "חסרים שדות חובה: כיתה, רשימת מילים ושם משימה."
      );
    }

    // ודא שהכיתה קיימת במוסד הזה, ושמורה (לא מנהל) יוצר משימה רק לכיתה שלו.
    const classRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("classes").doc(classId);
    const classDoc = await classRef.get();

    if (!classDoc.exists) {
      throw new HttpsError("not-found", "הכיתה לא נמצאה.");
    }
    if (role === "teacher" && classDoc.data().teacherId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "זו לא הכיתה שלך.");
    }

    const assignmentData = {
      title,
      classId,
      listId,
      wordIds,
      createdBy: request.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      status: "active",
    };
    if (dueDateMs !== null) {
      assignmentData.dueDateMs = dueDateMs;
    }
    if (practiceMode !== null) {
      assignmentData.practiceMode = practiceMode;
    }

    const assignmentRef = await admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("assignments")
      .add(assignmentData);

    logger.info(
      `createAssignment: ${request.auth.uid} יצר משימה ${assignmentRef.id} לכיתה ${classId} (מוסד ${institutionId})`
    );

    return { assignmentId: assignmentRef.id };
  }
);

/**
 * =====================================================================
 * getMyAssignments — משימה 0.15 (שלב 2, מערכת משימות — צד תלמיד)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * מחזירה לתלמיד המחובר את כל המשימות (institutions/{institutionId}
 * /assignments) שה-classId שלהן נמצא באחת הכיתות של התלמיד. ממוינות:
 * פעילות (status="active") קודם, ואז לפי dueDateMs עולה (בלי דד-ליין —
 * לסוף).
 *
 * classIds מגיע ממסמך users/{uid}, לא מ-Custom Claims:
 * -----------------------------------------------------
 * ה-Claims (syncUserClaims, למעלה בקובץ זה) מסנכרנים בכוונה רק role
 * ו-institutionId — לא classIds (מערך באורך משתנה, לא מתאים למגבלת
 * הגודל של Custom Claims). לכן classIds נקרא כאן ישירות מהמסמך, בדיוק
 * כמו ש-myClassIds() ב-firestore.rules עושה בצד ה-Rules.
 *
 * תלמיד עצמאי / בלי כיתות:
 * --------------------------
 * לא שגיאה — מחזירה { assignments: [], hasInstitution } כדי שהלקוח
 * יוכל להבדיל "אין מוסד בכלל" (מציג "הצטרף לכיתה") מ"יש מוסד אבל אין
 * עדיין משימות פעילות" (מציג "אין משימות פעילות כרגע"). בפועל, הלקוח
 * (StudentAssignmentsViewModel) בודק את זה כבר לפני הקריאה דרך
 * UserRoleManager המקומי — hasInstitution כאן הוא הגנה כפולה בלבד.
 */
exports.getMyAssignments = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const institutionId = request.auth.token.institutionId;
    if (!institutionId) {
      return { assignments: [], hasInstitution: false };
    }

    const userDoc = await admin.firestore().collection("users").doc(request.auth.uid).get();
    const classIds = userDoc.exists && Array.isArray(userDoc.data().classIds)
      ? userDoc.data().classIds
      : [];

    if (classIds.length === 0) {
      return { assignments: [], hasInstitution: true };
    }

    // Firestore 'in' תומך עד 30 ערכים; תלמיד לא אמור להיות ביותר מכמה כיתות בודדות בפועל.
    const snap = await admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("assignments")
      .where("classId", "in", classIds.slice(0, 30))
      .get();

    const assignments = snap.docs.map((doc) => {
      const data = doc.data();
      return {
        assignmentId: doc.id,
        title: data.title || "",
        classId: data.classId || "",
        listId: data.listId || "",
        wordIds: Array.isArray(data.wordIds) ? data.wordIds : [],
        dueDateMs: typeof data.dueDateMs === "number" ? data.dueDateMs : null,
        status: data.status || "active",
        practiceMode: data.practiceMode === "varied" ? "varied" : null,
      };
    });

    assignments.sort((a, b) => {
      const aActive = a.status === "active" ? 0 : 1;
      const bActive = b.status === "active" ? 0 : 1;
      if (aActive !== bActive) return aActive - bActive;
      const aDue = a.dueDateMs === null ? Infinity : a.dueDateMs;
      const bDue = b.dueDateMs === null ? Infinity : b.dueDateMs;
      return aDue - bDue;
    });

    return { assignments, hasInstitution: true };
  }
);

/**
 * =====================================================================
 * getClassProgress — מעקב התקדמות תלמידים בכיתה (שלב 1, דשבורד מורה)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * למורה/מנהל בלבד. מקבלת { classId, institutionId }, מוודאת שהכיתה שייכת
 * למוסד של המבקש (ושמורה — לא מנהל — מבקש רק את הכיתה שלו), ומחזירה לכל
 * תלמיד עם classId ברשימת ה-classIds שלו: displayName, totalXp,
 * lastActiveDate (ms, null אם מעולם לא תרגל), masteredWords, weeklyActivity.
 *
 * "מילה נכבשה" — אותה נוסחה בדיוק כמו Word.isMastered() בצד לקוח
 * (data/Word.java, המשימה הקודמת): correctAttempts>=3 וגם דיוק>=70%.
 * מטבע הדברים משוכפלת כאן כי Cloud Functions הוא סביבת JS נפרדת — אין
 * דרך לשתף קוד Java/JS ישירות בפרויקט הזה.
 *
 * שאילתת users: array-contains בלבד (לא משולב עם equality על institutionId,
 * במכוון) — כדי לא לדרוש אינדקס מורכב חדש; array-contains בודד נתמך
 * באינדקס האוטומטי הרגיל. הבידוד המוסדי עדיין נאכף: classDoc נבדק שקיים
 * תחת institutions/{institutionId}/classes/{classId} לפני השאילתה, ולכל
 * מסמך תוצאה נבדק גם institutionId === המוסד המבוקש (הגנה כפולה בזיכרון).
 *
 * N קריאות progress (אחת לכל תלמיד): מקביל לדפוס הקיים ב-
 * StudentAssignmentsViewModel/InstitutionalStatsViewModel (קריאה עצמית),
 * רק שכאן זה בצד שרת עבור כל תלמידי הכיתה בבת אחת — זו בדיוק "0.14 Cloud
 * Function אגרגציה מינימלית" מהאיפיון (docs/EasyLex_Platform_Spec_v1.1.md),
 * שהמומשה הישירה שלה נדחתה עד עכשיו. גודל כיתה טיפוסי (עשרות תלמידים)
 * הופך את זה לזול; "עדכני עד דקה" ולא real-time, כמו שהאיפיון מגדיר.
 */
exports.getClassProgress = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const role = request.auth.token.role;
    if (role !== "teacher" && role !== "principal") {
      throw new HttpsError("permission-denied", "רק מורה או מנהל יכולים לצפות בהתקדמות תלמידים.");
    }

    const data = request.data || {};
    const classId = data.classId;
    const institutionId = data.institutionId;
    if (!classId || !institutionId) {
      throw new HttpsError("invalid-argument", "חסרים שדות חובה: classId, institutionId.");
    }
    if (institutionId !== request.auth.token.institutionId) {
      throw new HttpsError("permission-denied", "אין לך הרשאה למוסד זה.");
    }

    const classRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("classes").doc(classId);
    const classDoc = await classRef.get();

    if (!classDoc.exists) {
      throw new HttpsError("not-found", "הכיתה לא נמצאה.");
    }
    if (role === "teacher" && classDoc.data().teacherId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "זו לא הכיתה שלך.");
    }

    const usersSnap = await admin
      .firestore()
      .collection("users")
      .where("classIds", "array-contains", classId)
      .get();

    const nowMs = Date.now();
    const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;

    const students = await Promise.all(
      usersSnap.docs
        .filter((userDoc) => userDoc.data().institutionId === institutionId)
        .map(async (userDoc) => {
          const u = userDoc.data();
          const progressSnap = await admin
            .firestore()
            .collection("users").doc(userDoc.id)
            .collection("progress")
            .get();

          let masteredWords = 0;
          let lastActiveDate = null;
          progressSnap.forEach((doc) => {
            const p = doc.data();
            const correct = typeof p.correctAttempts === "number" ? p.correctAttempts : 0;
            const total = typeof p.totalAttempts === "number" ? p.totalAttempts : 0;
            if (correct >= 3 && total > 0 && correct / total >= 0.7) {
              masteredWords++;
            }
            if (p.lastPracticed && typeof p.lastPracticed.toMillis === "function") {
              const ms = p.lastPracticed.toMillis();
              if (lastActiveDate === null || ms > lastActiveDate) lastActiveDate = ms;
            }
          });

          return {
            uid: userDoc.id,
            displayName: u.displayName || "",
            totalXp: typeof u.totalXp === "number" ? u.totalXp : 0,
            lastActiveDate,
            masteredWords,
            weeklyActivity: lastActiveDate !== null && (nowMs - lastActiveDate) <= sevenDaysMs,
          };
        })
    );

    students.sort((a, b) => b.totalXp - a.totalXp);

    logger.info(
      `getClassProgress: ${request.auth.uid} טען התקדמות ל-${students.length} תלמידים בכיתה ${classId}`
    );

    return { students };
  }
);

/**
 * =====================================================================
 * getClassHardWords — מפת חום: כל המילים הקשות בכיתה (דשבורד מורה, אתר ווב)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * למורה/מנהל בלבד, אותה בדיקת הרשאות בדיוק כמו getClassProgress (role,
 * institutionId, בעלות המורה על הכיתה) — כי אותה סיבה בדיוק: קריאת
 * users/{uid}/progress של תלמידים אחרים חסומה ל-owner-בלבד ב-Rules,
 * כך שרק Cloud Function עם Admin SDK יכולה לצבור את זה בין תלמידים.
 *
 * צובר לפי englishWord על פני progress של כל תלמידי הכיתה (attempts+errors
 * מצטברים, לא ממוצע של ממוצעים), ומחזירה את *כל* המילים עם אחוז השגיאה
 * הגבוה ביותר קודם (לא רק top 5 — המסך שמצרך את זה מציג מפת חום לכל
 * הכיתה). מילים עם totalAttempts=0 לא נכללות (אין מה לחשב).
 *
 * studentsAttempted/studentsFailed — לכל מילה, כמה תלמידים ניסו אותה
 * בכלל וכמה מהם עדיין לא "כבשו" אותה (אותה הגדרת isMastered בדיוק כמו
 * getClassProgress: correctAttempts>=3 && total>0 && correct/total>=0.7).
 * כל תלמיד תורם לכל היותר progress doc אחד למילה נתונה (doc ID = המילה),
 * כך שאין צורך ב-Set לדה-דופ — ספירה ישירה מספיקה.
 */
exports.getClassHardWords = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const role = request.auth.token.role;
    if (role !== "teacher" && role !== "principal") {
      throw new HttpsError("permission-denied", "רק מורה או מנהל יכולים לצפות בהתקדמות תלמידים.");
    }

    const data = request.data || {};
    const classId = data.classId;
    const institutionId = data.institutionId;
    if (!classId || !institutionId) {
      throw new HttpsError("invalid-argument", "חסרים שדות חובה: classId, institutionId.");
    }
    if (institutionId !== request.auth.token.institutionId) {
      throw new HttpsError("permission-denied", "אין לך הרשאה למוסד זה.");
    }

    const classRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("classes").doc(classId);
    const classDoc = await classRef.get();

    if (!classDoc.exists) {
      throw new HttpsError("not-found", "הכיתה לא נמצאה.");
    }
    if (role === "teacher" && classDoc.data().teacherId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "זו לא הכיתה שלך.");
    }

    const usersSnap = await admin
      .firestore()
      .collection("users")
      .where("classIds", "array-contains", classId)
      .get();

    const wordStats = new Map(); // englishWord -> { attempts, errors, studentsAttempted, studentsFailed }

    await Promise.all(
      usersSnap.docs
        .filter((userDoc) => userDoc.data().institutionId === institutionId)
        .map(async (userDoc) => {
          const progressSnap = await admin
            .firestore()
            .collection("users").doc(userDoc.id)
            .collection("progress")
            .get();

          progressSnap.forEach((doc) => {
            const p = doc.data();
            const word = p.englishWord;
            const total = typeof p.totalAttempts === "number" ? p.totalAttempts : 0;
            const correct = typeof p.correctAttempts === "number" ? p.correctAttempts : 0;
            if (!word || total <= 0) return;

            const errors = Math.max(0, total - correct);
            const mastered = correct >= 3 && correct / total >= 0.7;
            const entry = wordStats.get(word) ||
              { attempts: 0, errors: 0, studentsAttempted: 0, studentsFailed: 0 };
            entry.attempts += total;
            entry.errors += errors;
            entry.studentsAttempted += 1;
            if (!mastered) entry.studentsFailed += 1;
            wordStats.set(word, entry);
          });
        })
    );

    const words = Array.from(wordStats.entries())
      .map(([englishWord, stats]) => ({
        englishWord,
        totalAttempts: stats.attempts,
        errorRate: stats.errors / stats.attempts,
        studentsAttempted: stats.studentsAttempted,
        studentsFailed: stats.studentsFailed,
      }))
      .sort((a, b) => b.errorRate - a.errorRate);

    logger.info(
      `getClassHardWords: ${request.auth.uid} טען ${words.length} מילים קשות לכיתה ${classId}`
    );

    return { words };
  }
);

/**
 * =====================================================================
 * getAssignmentProgress — % השלמה למשימה (דשבורד מורה, אתר ווב)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * למורה/מנהל בלבד, אותה בדיקת הרשאות בדיוק כמו getClassProgress/
 * getClassHardWords — ואותה סיבה: קריאת progress של תלמידים אחרים
 * חסומה ל-owner-בלבד ב-Rules.
 *
 * קובעת את קבוצת המילים של המשימה: אם wordIds לא ריק — אלה המילים
 * הספציפיות (נטענות מ-word_lists/{listId}/words לפי ID כדי לקבל את
 * englishWord); אם ריק — כל המילים ברשימה listId (התנהגות "הכל" הקיימת).
 * לכל תלמיד בכיתה, % השלמה אישי = (מילים "נכבשות" מתוך מילות המשימה
 * בלבד) / (סה"כ מילות המשימה) — "נכבשת" באותה הגדרה בדיוק כמו
 * getClassProgress (correctAttempts>=3 && total>0 && correct/total>=0.7).
 * % ההשלמה של המשימה כולה = ממוצע האחוזים האישיים.
 */
exports.getAssignmentProgress = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const role = request.auth.token.role;
    if (role !== "teacher" && role !== "principal") {
      throw new HttpsError("permission-denied", "רק מורה או מנהל יכולים לצפות בהתקדמות תלמידים.");
    }

    const data = request.data || {};
    const assignmentId = data.assignmentId;
    const institutionId = data.institutionId;
    if (!assignmentId || !institutionId) {
      throw new HttpsError("invalid-argument", "חסרים שדות חובה: assignmentId, institutionId.");
    }
    if (institutionId !== request.auth.token.institutionId) {
      throw new HttpsError("permission-denied", "אין לך הרשאה למוסד זה.");
    }

    const assignmentRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("assignments").doc(assignmentId);
    const assignmentDoc = await assignmentRef.get();
    if (!assignmentDoc.exists) {
      throw new HttpsError("not-found", "המשימה לא נמצאה.");
    }
    const assignment = assignmentDoc.data();
    const classId = assignment.classId;
    const listId = assignment.listId;
    const wordIds = Array.isArray(assignment.wordIds) ? assignment.wordIds : [];

    const classRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("classes").doc(classId);
    const classDoc = await classRef.get();
    if (!classDoc.exists) {
      throw new HttpsError("not-found", "הכיתה לא נמצאה.");
    }
    if (role === "teacher" && classDoc.data().teacherId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "זו לא הכיתה שלך.");
    }

    // קביעת קבוצת המילים של המשימה (englishWord strings).
    let targetWords;
    if (wordIds.length > 0) {
      const wordDocs = await Promise.all(
        wordIds.map((wordId) =>
          admin.firestore().collection("word_lists").doc(listId)
            .collection("words").doc(wordId).get()
        )
      );
      targetWords = new Set(
        wordDocs.filter((d) => d.exists).map((d) => d.data().englishWord).filter(Boolean)
      );
    } else {
      const listWordsSnap = await admin
        .firestore()
        .collection("word_lists").doc(listId)
        .collection("words")
        .get();
      targetWords = new Set(
        listWordsSnap.docs.map((d) => d.data().englishWord).filter(Boolean)
      );
    }

    if (targetWords.size === 0) {
      return { completionPct: 0, studentCount: 0 };
    }

    const usersSnap = await admin
      .firestore()
      .collection("users")
      .where("classIds", "array-contains", classId)
      .get();

    const relevantStudents = usersSnap.docs.filter(
      (userDoc) => userDoc.data().institutionId === institutionId
    );

    const perStudentPct = await Promise.all(
      relevantStudents.map(async (userDoc) => {
        const progressSnap = await admin
          .firestore()
          .collection("users").doc(userDoc.id)
          .collection("progress")
          .where("sourceListId", "==", listId)
          .get();

        let mastered = 0;
        progressSnap.forEach((doc) => {
          const p = doc.data();
          if (!targetWords.has(p.englishWord)) return;
          const correct = typeof p.correctAttempts === "number" ? p.correctAttempts : 0;
          const total = typeof p.totalAttempts === "number" ? p.totalAttempts : 0;
          if (correct >= 3 && total > 0 && correct / total >= 0.7) mastered++;
        });

        return mastered / targetWords.size;
      })
    );

    const completionPct =
      relevantStudents.length === 0
        ? 0
        : Math.round(
            (perStudentPct.reduce((sum, pct) => sum + pct, 0) / relevantStudents.length) * 100
          );

    logger.info(
      `getAssignmentProgress: ${request.auth.uid} — משימה ${assignmentId}: ${completionPct}% (${relevantStudents.length} תלמידים)`
    );

    return { completionPct, studentCount: relevantStudents.length };
  }
);

/**
 * =====================================================================
 * sendAnnouncement — הודעת מורה לכיתה, חד-כיוונית (שלב 2)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * למורה/מנהל בלבד. מקבלת { classId, message }, מוודאת שהכיתה שייכת למוסד
 * של המבקש (ושמורה — לא מנהל — שולח רק לכיתה שלו, אותו תבנית בדיקה בדיוק
 * כמו createAssignment), ומגבילה את ההודעה ל-200 תווים. כותבת מסמך חדש
 * ב-institutions/{institutionId}/classes/{classId}/announcements/ —
 * ה-schema (message/createdAt/createdBy/createdByName/isRead) כבר צפוי
 * ע"י InstitutionalHomeViewModel.loadLatestAnnouncement (שינוי ארכיטקטוני,
 * שלב 2). isRead נכתב false לשלמות הסכמה בלבד — ה"נקרא" בפועל הוא מקומי
 * (SharedPreferences בצד תלמיד, InstitutionalHomeFragment.dismissAnnouncement),
 * לא מסונכרן חזרה ל-Firestore.
 */
exports.sendAnnouncement = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const role = request.auth.token.role;
    if (role !== "teacher" && role !== "principal") {
      throw new HttpsError("permission-denied", "רק מורה או מנהל יכולים לשלוח הודעה לכיתה.");
    }

    const institutionId = request.auth.token.institutionId;
    if (!institutionId) {
      throw new HttpsError("failed-precondition", "החשבון שלך אינו משויך למוסד.");
    }

    const data = request.data || {};
    const classId = data.classId;
    const message = typeof data.message === "string" ? data.message.trim() : "";

    if (!classId || !message) {
      throw new HttpsError("invalid-argument", "חסרים שדות חובה: כיתה והודעה.");
    }
    if (message.length > 200) {
      throw new HttpsError("invalid-argument", "ההודעה ארוכה מדי (מקסימום 200 תווים).");
    }

    const classRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("classes").doc(classId);
    const classDoc = await classRef.get();

    if (!classDoc.exists) {
      throw new HttpsError("not-found", "הכיתה לא נמצאה.");
    }
    if (role === "teacher" && classDoc.data().teacherId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "זו לא הכיתה שלך.");
    }

    const senderDoc = await admin.firestore().collection("users").doc(request.auth.uid).get();
    const createdByName = (senderDoc.exists && senderDoc.data().displayName) || "";

    const announcementRef = await classRef.collection("announcements").add({
      message,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      createdBy: request.auth.uid,
      createdByName,
      isRead: false,
    });

    // Push לתלמידי הכיתה — best-effort: ההודעה כבר נכתבה ל-Firestore
    // (זה מה שקובע הצלחה), כשל בשליחת ה-push לא אמור לחסום את התשובה
    // למורה. אותו דפוס where("classIds","array-contains",classId) +
    // סינון institutionId ב-JS כמו getClassHardWords למעלה.
    try {
      const studentsSnap = await admin
        .firestore()
        .collection("users")
        .where("classIds", "array-contains", classId)
        .get();
      const studentUids = studentsSnap.docs
        .filter((d) => d.data().institutionId === institutionId)
        .map((d) => d.id);
      const tokens = await getTokensForUids(studentUids);
      logger.info(
        `sendAnnouncement: ${studentUids.length} תלמידים בכיתה ${classId}, ${tokens.length} tokens נמצאו ב-users/*/tokens`
      );
      const { successCount, failureCount } = await sendPushToTokens(tokens, {
        title: "📢 הודעה חדשה מהמורה",
        body: message,
      });
      logger.info(
        `sendAnnouncement: push ל-${studentUids.length} תלמידים (${tokens.length} מכשירים) — ${successCount} הצליחו, ${failureCount} נכשלו`
      );
    } catch (err) {
      logger.error(`sendAnnouncement: שליחת push נכשלה: ${err.message}`);
    }

    logger.info(
      `sendAnnouncement: ${request.auth.uid} שלח הודעה ${announcementRef.id} לכיתה ${classId} (מוסד ${institutionId})`
    );

    return { announcementId: announcementRef.id };
  }
);

/**
 * =====================================================================
 * sendStreakReminders — תזכורת רצף יומית (Push, אפליקציית תלמיד)
 * =====================================================================
 * רצה כל יום ב-20:00 שעון ישראל. עוברת על כל התלמידים שלא תרגלו היום
 * (lastActiveDate != todayKey) ושולחת push לכל המכשירים הרשומים שלהם
 * (users/{uid}/tokens/*, ר' getTokensForUids).
 * scheduled function, לא onCall — אין request.auth, לא צריך בדיקת
 * הרשאות.
 */
exports.sendStreakReminders = onSchedule(
  {
    schedule: "0 20 * * *",
    timeZone: "Asia/Jerusalem",
    region: "europe-west1",
  },
  async () => {
    const todayKey = todayKeyIsrael();
    const usersSnap = await admin.firestore().collection("users").where("role", "==", "student").get();

    const eligibleUids = [];
    usersSnap.forEach((doc) => {
      const data = doc.data();
      if (data.lastActiveDate === todayKey) return; // כבר תרגל היום
      eligibleUids.push(doc.id);
    });

    const tokens = await getTokensForUids(eligibleUids);
    const { successCount, failureCount } = await sendPushToTokens(tokens, {
      title: "🔥 הרצף שלך בסכנה!",
      body: "תרגל עכשיו כדי לשמור על הרצף שלך.",
    });

    logger.info(
      `sendStreakReminders: ${eligibleUids.length} תלמידים לא תרגלו היום (${todayKey}, ${tokens.length} מכשירים) — ${successCount} התראות נשלחו בהצלחה, ${failureCount} נכשלו`
    );
  }
);

/**
 * =====================================================================
 * getPrincipalStats — דשבורד מינימלי למנהל מוסד (שלב 2)
 * =====================================================================
 *
 * למנהל (role="principal") בלבד. מחזירה 4 ספירות ברמת המוסד כולו (לא
 * כיתה בודדת, בניגוד ל-getClassProgress):
 *   • teacherCount — users עם institutionId==X ו-role=="teacher"
 *   • classCount — institutions/{id}/classes לא-מאורכבות
 *   • activeStudentsThisWeek — users עם institutionId==X ו-lastActiveDate
 *     (מחרוזת "yyyy-MM-dd", ר' StreakManager.java) בטווח 7 הימים האחרונים.
 *     דורש אינדקס מורכב (institutionId + lastActiveDate) — ר' firestore.indexes.json.
 *   • activeAssignmentCount — institutions/{id}/assignments עם status=="active"
 *     (כל הכיתות במוסד, לא רק כיתת מורה בודד — בניגוד ל-
 *     ProfileViewModel.loadTeacherActiveAssignmentCount).
 *
 * כל 4 הספירות נשלפות במקביל דרך count() aggregate queries — זול, בלי
 * לקרוא את כל המסמכים עצמם.
 */
exports.getPrincipalStats = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const role = request.auth.token.role;
    if (role !== "principal") {
      throw new HttpsError("permission-denied", "רק מנהל מוסד יכול לצפות בנתוני המוסד.");
    }

    const institutionId = request.auth.token.institutionId;
    if (!institutionId) {
      throw new HttpsError("failed-precondition", "החשבון שלך אינו משויך למוסד.");
    }

    const db = admin.firestore();
    const usersRef = db.collection("users");
    const institutionRef = db.collection("institutions").doc(institutionId);

    const sevenDaysAgoStr = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
      .toISOString()
      .slice(0, 10); // "yyyy-MM-dd" — אותו פורמט כמו StreakManager

    const [teacherCountSnap, classCountSnap, activeStudentsSnap, assignmentCountSnap] =
      await Promise.all([
        usersRef
          .where("institutionId", "==", institutionId)
          .where("role", "==", "teacher")
          .count()
          .get(),
        institutionRef.collection("classes").where("archived", "==", false).count().get(),
        usersRef
          .where("institutionId", "==", institutionId)
          .where("lastActiveDate", ">=", sevenDaysAgoStr)
          .count()
          .get(),
        institutionRef.collection("assignments").where("status", "==", "active").count().get(),
      ]);

    logger.info(`getPrincipalStats: ${request.auth.uid} טען נתוני מוסד ${institutionId}`);

    return {
      teacherCount: teacherCountSnap.data().count,
      classCount: classCountSnap.data().count,
      activeStudentsThisWeek: activeStudentsSnap.data().count,
      activeAssignmentCount: assignmentCountSnap.data().count,
    };
  }
);

/**
 * =====================================================================
 * transferClass — העברת כיתה בין מורים (דשבורד מנהל)
 * =====================================================================
 *
 * מה זה עושה?
 * ------------
 * למנהל מוסד (role="principal") בלבד — לא מורה. firestore.rules מגביל
 * כתיבה ל-institutions/{instId}/classes/{classId} ל-isTeacher() בלבד,
 * כך שמנהל לא יכול לעדכן teacherId ישירות מה-client SDK; זו הסיבה
 * שהפעולה הזו חייבת Cloud Function עם Admin SDK, בדיוק כמו
 * createAssignment/sendAnnouncement למעלה.
 *
 * מאמתת: (1) role==principal, (2) הכיתה שייכת למוסד של המנהל,
 * (3) המורה החדש שייך לאותו מוסד ובעל role=="teacher" — אימות נוסף
 * מעבר למפורש בדרישה, כדי שלא ניתן יהיה להעביר כיתה לחשבון שגוי/של
 * מוסד אחר דרך תפירת data.newTeacherId.
 */
exports.transferClass = onCall(
  {
    region: "europe-west1",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "יש להתחבר.");
    }

    const role = request.auth.token.role;
    if (role !== "principal") {
      throw new HttpsError("permission-denied", "רק מנהל מוסד יכול להעביר כיתה בין מורים.");
    }

    const institutionId = request.auth.token.institutionId;
    if (!institutionId) {
      throw new HttpsError("failed-precondition", "החשבון שלך אינו משויך למוסד.");
    }

    const data = request.data || {};
    const classId = data.classId;
    const newTeacherId = data.newTeacherId;
    if (!classId || !newTeacherId) {
      throw new HttpsError("invalid-argument", "חסרים שדות חובה: classId, newTeacherId.");
    }

    const classRef = admin
      .firestore()
      .collection("institutions").doc(institutionId)
      .collection("classes").doc(classId);
    const classDoc = await classRef.get();

    if (!classDoc.exists) {
      throw new HttpsError("not-found", "הכיתה לא נמצאה.");
    }

    const teacherDoc = await admin.firestore().collection("users").doc(newTeacherId).get();
    const teacherData = teacherDoc.exists ? teacherDoc.data() : null;
    if (!teacherData || teacherData.institutionId !== institutionId || teacherData.role !== "teacher") {
      throw new HttpsError("invalid-argument", "המורה שנבחר אינו מורה פעיל במוסד הזה.");
    }

    await classRef.update({ teacherId: newTeacherId });

    logger.info(
      `transferClass: ${request.auth.uid} העביר כיתה ${classId} למורה ${newTeacherId} (מוסד ${institutionId})`
    );

    return { success: true };
  }
);
